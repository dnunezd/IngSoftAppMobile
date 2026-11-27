package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.AlbumDetailEntity
import com.example.vinilos.data.local.AlbumEntity
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Performer
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.Comment
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Catalog cache in the repository (add-cache-catalogo, D4 and D7), with a fake in-memory DAO. */
class AlbumRepositoryCacheTest {

    /** Stores into a list; [fallaAlLeer] and [fallaAlEscribir] simulate storage that isn't responding. */
    private class AlbumDaoFalso(inicial: List<AlbumEntity> = emptyList()) : AlbumDao() {
        var filas: List<AlbumEntity> = inicial
        var fallaAlLeer: Throwable? = null
        var fallaAlEscribir: Throwable? = null
        var escrituras = 0

        /** Every write it received, in order, with the ids it touched (optimizar-cache-e-imagenes). */
        val operaciones = mutableListOf<String>()

        private fun anotar(operacion: String, ids: List<Int> = emptyList()) {
            fallaAlEscribir?.let { throw it }
            operaciones += if (ids.isEmpty()) operacion else "$operacion$ids"
        }

        override suspend fun todos(): List<AlbumEntity> {
            fallaAlLeer?.let { throw it }
            return filas
        }

        override suspend fun borrarTodos() {
            anotar("borrarTodos")
            filas = emptyList()
        }

        override suspend fun insertar(albumes: List<AlbumEntity>) {
            anotar("insertar", albumes.map { it.id })
            filas = filas + albumes
            escrituras++
        }

        /** Details stored by id (add-cache-consultas). */
        var detalles: Map<Int, AlbumDetailEntity> = emptyMap()

        override suspend fun detalle(id: Int): AlbumDetailEntity? {
            fallaAlLeer?.let { throw it }
            return detalles[id]
        }

        override suspend fun saveDetail(detalle: AlbumDetailEntity) {
            anotar("guardarDetalle", listOf(detalle.id))
            detalles = detalles + (detalle.id to detalle)
        }

        override suspend fun borrarDetalles() {
            anotar("borrarDetalles")
            detalles = emptyMap()
        }

        override suspend fun insertarDetalles(detalles: List<AlbumDetailEntity>) {
            anotar("insertarDetalles", detalles.map { it.id })
            this.detalles += detalles.associateBy { it.id }
        }

        override suspend fun borrarDelCatalogo(id: Int) {
            anotar("borrarDelCatalogo", listOf(id))
            filas = filas.filterNot { it.id == id }
        }

        override suspend fun borrarDetalle(id: Int) {
            anotar("borrarDetalle", listOf(id))
            detalles = detalles - id
        }

        override suspend fun saveAlbums(albumes: List<AlbumEntity>) {
            anotar("guardarAlbumes", albumes.map { it.id })
            filas = filas.filterNot { fila -> albumes.any { it.id == fila.id } } + albumes
        }

        override suspend fun saveDetails(detalles: List<AlbumDetailEntity>) {
            anotar("guardarDetalles", detalles.map { it.id })
            this.detalles = this.detalles + detalles.associateBy { it.id }
        }

        override suspend fun borrarAlbumesPorId(ids: List<Int>) {
            anotar("borrarAlbumesPorId", ids)
            filas = filas.filterNot { it.id in ids }
        }

        override suspend fun borrarDetallesPorId(ids: List<Int>) {
            anotar("borrarDetallesPorId", ids)
            detalles = detalles - ids.toSet()
        }
    }

    private lateinit var servidor: MockWebServer
    private lateinit var dao: AlbumDaoFalso
    private lateinit var repositorio: AlbumRepository

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        dao = AlbumDaoFalso()
        repositorio = AlbumRepositoryImpl(ServiceAdapter(servidor.url("/").toString()), albumDao = dao)
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    private val catalogo = """
        [
          {"name": "Sin id", "genre": "Rock"},
          {"id": 100, "name": "Buscando América", "cover": "https://example.com/100.jpg", "genre": "Salsa",
           "releaseDate": "1984-08-01T00:00:00.000Z", "performers": [{"id": 1, "name": "Rubén Blades"}]},
          {"id": 102, "name": "A Night at the Opera", "genre": "Rock"}
        ]
    """.trimIndent()

    private val buscandoAmerica = AlbumSummary(
        id = 100, nombre = "Buscando América", portada = "https://example.com/100.jpg",
        genero = "Salsa", artista = "Rubén Blades", anio = "1984"
    )
    private val nightAtTheOpera = AlbumSummary(
        id = 102, nombre = "A Night at the Opera", portada = null, genero = "Rock", artista = "", anio = null
    )

    private fun fila(album: AlbumSummary) =
        AlbumEntity(album.id, album.nombre, album.portada, album.genero, album.artista, album.anio)

    @Test
    fun unaConsultaSuccesssaReemplazaLoGuardadoConLosAlbumesValidos() = runTest {
        dao.filas = listOf(AlbumEntity(999, "Ya no existe", null, "Pop", "", null))
        servidor.enqueue(MockResponse().setBody(catalogo))

        val albumes = repositorio.getAlbums().getOrThrow()

        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), albumes)
        assertEquals(setOf(fila(nightAtTheOpera), fila(buscandoAmerica)), dao.filas.toSet())
    }

    @Test
    fun unaRespuestaVaciaDejaLaCacheVacia() = runTest {
        dao.filas = listOf(fila(buscandoAmerica))
        servidor.enqueue(MockResponse().setBody("[]"))

        repositorio.getAlbums().getOrThrow()

        assertTrue(dao.filas.isEmpty())
    }

    @Test
    fun unaConsultaFallidaNoTocaLoGuardado() = runTest {
        dao.filas = listOf(fila(buscandoAmerica))
        servidor.enqueue(MockResponse().setResponseCode(500))

        val error = repositorio.getAlbums().exceptionOrNull()

        assertTrue(error is LoadError)
        assertEquals(listOf(fila(buscandoAmerica)), dao.filas)
        assertEquals(0, dao.escrituras)
    }

    @Test
    fun loGuardadoSeDevuelveEnOrdenAlfabeticoSinDistinguirTildes() = runTest {
        dao.filas = listOf(
            AlbumEntity(3, "Poeta del pueblo", null, "Salsa", "", null),
            AlbumEntity(1, "Álbum blanco", null, "Rock", "", null),
            AlbumEntity(2, "Buscando América", null, "Salsa", "", null),
            AlbumEntity(4, "abbey Road", null, "Rock", "", null)
        )

        val nombres = repositorio.getCachedAlbums().map { it.nombre }

        assertEquals(listOf("abbey Road", "Álbum blanco", "Buscando América", "Poeta del pueblo"), nombres)
    }

    @Test
    fun loGuardadoConservaTodosLosCamposDelAlbum() = runTest {
        dao.filas = listOf(fila(buscandoAmerica), fila(nightAtTheOpera))

        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), repositorio.getCachedAlbums())
    }

    @Test
    fun sinDaoNoHayNadaGuardadoYElCatalogoSeConsultaComoAntes() = runTest {
        val sinCache = AlbumRepositoryImpl(ServiceAdapter(servidor.url("/").toString()))
        servidor.enqueue(MockResponse().setBody(catalogo))

        assertTrue(sinCache.getCachedAlbums().isEmpty())
        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), sinCache.getAlbums().getOrThrow())
    }

    // ---- Storage failures (D7): the cache is an enhancement, never a precondition ----

    @Test
    fun siNoSePuedeLeerLaCacheNoHayNadaGuardado() = runTest {
        dao.filas = listOf(fila(buscandoAmerica))
        dao.fallaAlLeer = SQLiteException("disco no disponible")

        assertTrue(repositorio.getCachedAlbums().isEmpty())
    }

    @Test
    fun siNoSePuedeEscribirLaCacheSeDevuelveIgualLaRespuestaDeRed() = runTest {
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(MockResponse().setBody(catalogo))

        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), repositorio.getAlbums().getOrThrow())
    }

    @Test
    fun laCancelacionAlLeerLaCacheSeRelanza() = runTest {
        dao.fallaAlLeer = CancellationException("cancelada")

        try {
            repositorio.getCachedAlbums()
            fail("La cancelación debe relanzarse")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
    }

    @Test
    fun laCancelacionAlEscribirLaCacheSeRelanza() = runTest {
        dao.fallaAlEscribir = CancellationException("cancelada")
        servidor.enqueue(MockResponse().setBody(catalogo))

        try {
            repositorio.getAlbums()
            fail("La cancelación debe relanzarse")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
    }

    // ---- Stored detail (add-cache-consultas, D4) ----

    private val albumCompletoJson = """
        {"id": 200, "name": "Buscando América", "cover": "https://example.com/200.jpg",
         "description": "Salsa urbana", "genre": "Salsa", "recordLabel": "Elektra",
         "releaseDate": "1984-08-01T00:00:00.000Z",
         "tracks": [{"id": 1, "name": "Decisiones", "duration": "5:05"}],
         "performers": [{"id": 1, "name": "Rubén Blades"}],
         "comments": [{"id": 1, "description": "Excelente", "rating": 5}]}
    """.trimIndent()

    private val albumCompleto = Album(
        id = 200, nombre = "Buscando América", portada = "https://example.com/200.jpg",
        descripcion = "Salsa urbana", genero = "Salsa", disquera = "Elektra",
        fechaLanzamiento = "1984-08-01T00:00:00.000Z",
        canciones = listOf(Track("Decisiones", "5:05")),
        artistas = listOf(Performer("Rubén Blades", null)),
        comentarios = listOf(Comment("Excelente", 5))
    )

    private fun detalleGuardado(album: Album = albumCompleto) = AlbumDetailEntity(
        album.id, album.nombre, album.portada, album.descripcion, album.genero, album.disquera,
        album.fechaLanzamiento, album.canciones, album.artistas, album.comentarios
    )

    @Test
    fun elCatalogoGuardaElDetalleDeLosAlbumesCompletosSinPeticionesNuevas() = runTest {
        // 102 has no description, record label, or date: it stays in the catalog, but without a detail.
        servidor.enqueue(MockResponse().setBody("[$albumCompletoJson, {\"id\": 102, \"name\": \"A Night at the Opera\", \"genre\": \"Rock\"}]"))

        repositorio.getAlbums().getOrThrow()

        assertEquals(setOf(200, 102), dao.filas.map { it.id }.toSet())
        assertEquals(mapOf(200 to detalleGuardado()), dao.detalles)
        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun elCatalogoBorraElDetalleDeLosAlbumesQueYaNoVienen() = runTest {
        dao.detalles = mapOf(999 to detalleGuardado(albumCompleto.copy(id = 999)))
        servidor.enqueue(MockResponse().setBody("[$albumCompletoJson]"))

        repositorio.getAlbums().getOrThrow()

        assertEquals(setOf(200), dao.detalles.keys)
    }

    @Test
    fun getCachedAlbumDevuelveElDetalleGuardado() = runTest {
        dao.detalles = mapOf(200 to detalleGuardado())

        assertEquals(albumCompleto, repositorio.getCachedAlbum(200))
        assertNull(repositorio.getCachedAlbum(201))
    }

    @Test
    fun abrirElDetalleConConexionGuardaOReemplazaSuDetalle() = runTest {
        dao.detalles = mapOf(200 to detalleGuardado(albumCompleto.copy(nombre = "Nombre viejo")))
        servidor.enqueue(MockResponse().setBody(albumCompletoJson))

        assertEquals(albumCompleto, repositorio.getAlbum(200).getOrThrow())
        assertEquals(detalleGuardado(), dao.detalles[200])
    }

    @Test
    fun un404BorraElAlbumDelCatalogoYDelDetalleGuardados() = runTest {
        dao.filas = listOf(AlbumEntity(200, "Buscando América", null, "Salsa", "", null))
        dao.detalles = mapOf(200 to detalleGuardado())
        servidor.enqueue(MockResponse().setResponseCode(404))

        val error = repositorio.getAlbum(200).exceptionOrNull()

        assertTrue(error is AlbumNotAvailable)
        assertTrue(dao.filas.isEmpty())
        assertTrue(dao.detalles.isEmpty())
    }

    @Test
    fun unFalloDeRedNoTocaElDetalleGuardado() = runTest {
        dao.detalles = mapOf(200 to detalleGuardado())
        servidor.enqueue(MockResponse().setResponseCode(500))

        assertTrue(repositorio.getAlbum(200).exceptionOrNull() is LoadError)
        assertEquals(mapOf(200 to detalleGuardado()), dao.detalles)
    }

    @Test
    fun sinDaoNoHayDetalleGuardadoYElDetalleSeConsultaComoAntes() = runTest {
        val sinCache = AlbumRepositoryImpl(ServiceAdapter(servidor.url("/").toString()))
        servidor.enqueue(MockResponse().setBody(albumCompletoJson))

        assertNull(sinCache.getCachedAlbum(200))
        assertEquals(albumCompleto, sinCache.getAlbum(200).getOrThrow())
    }

    @Test
    fun siNoSePuedeLeerElDetalleNoHayNadaGuardado() = runTest {
        dao.detalles = mapOf(200 to detalleGuardado())
        dao.fallaAlLeer = SQLiteException("disco no disponible")

        assertNull(repositorio.getCachedAlbum(200))
    }

    @Test
    fun siNoSePuedeEscribirElDetalleSeDevuelveIgualElAlbum() = runTest {
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(MockResponse().setBody(albumCompletoJson))
        servidor.enqueue(MockResponse().setResponseCode(404))

        assertEquals(albumCompleto, repositorio.getAlbum(200).getOrThrow())
        assertTrue(repositorio.getAlbum(200).exceptionOrNull() is AlbumNotAvailable)
    }

    // ---- Writing only what changed (optimizar-cache-e-imagenes, D4 and D5) ----

    private fun albumJson(id: Int, nombre: String = "Álbum $id", descripcion: String = "Descripción $id") = """
        {"id": $id, "name": "$nombre", "cover": "https://example.com/$id.jpg", "description": "$descripcion",
         "genre": "Salsa", "recordLabel": "Elektra", "releaseDate": "1984-08-01T00:00:00.000Z",
         "tracks": [], "performers": [{"id": 1, "name": "Rubén Blades"}], "comments": []}
    """.trimIndent()

    private fun respuesta(cuerpo: String, version: String? = null) =
        MockResponse().setBody(cuerpo).apply { if (version != null) setHeader("ETag", version) }

    private fun catalogo(vararg albumes: String, version: String? = null) =
        respuesta(albumes.joinToString(prefix = "[", postfix = "]"), version)

    /** Performs a catalog query that leaves the full copy stored and clears the write log. */
    private suspend fun catalogoYaGuardado(vararg albumes: String, version: String? = null) {
        servidor.enqueue(catalogo(*albumes, version = version))
        repositorio.getAlbums().getOrThrow()
        dao.operaciones.clear()
    }

    @Test
    fun laPrimeraConsultaReemplazaTodoLoGuardado() = runTest {
        servidor.enqueue(catalogo(albumJson(200), albumJson(201), version = "W/\"c1\""))

        repositorio.getAlbums().getOrThrow()

        assertEquals("borrarTodos", dao.operaciones.first())
        assertTrue("borrarDetalles" in dao.operaciones)
    }

    @Test
    fun conLaMismaVersionNoSeEscribeNada() = runTest {
        catalogoYaGuardado(albumJson(200), albumJson(201), version = "W/\"c1\"")
        servidor.enqueue(catalogo(albumJson(200), albumJson(201), version = "W/\"c1\""))

        val albumes = repositorio.getAlbums().getOrThrow()

        assertEquals(listOf(200, 201), albumes.map { it.id })
        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun conOtraVersionYElMismoContenidoNoSeEscribeNada() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c2\""))

        repositorio.getAlbums().getOrThrow()

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun sinVersionYConElMismoContenidoNoSeEscribeNada() = runTest {
        catalogoYaGuardado(albumJson(200))
        servidor.enqueue(catalogo(albumJson(200)))

        repositorio.getAlbums().getOrThrow()

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun unAlbumCambiadoEscribeSoloSuFilaYSuDetalle() = runTest {
        catalogoYaGuardado(albumJson(200), albumJson(201), version = "W/\"c1\"")
        servidor.enqueue(catalogo(albumJson(200, "Nombre nuevo"), albumJson(201), version = "W/\"c2\""))

        repositorio.getAlbums().getOrThrow()

        assertEquals(listOf("guardarAlbumes[200]", "guardarDetalles[200]"), dao.operaciones)
        assertEquals(setOf("Nombre nuevo", "Álbum 201"), dao.filas.map { it.nombre }.toSet())
        assertEquals("Nombre nuevo", dao.detalles[200]?.nombre)
    }

    @Test
    fun unAlbumQueYaNoLlegaSeBorraSoloPorSuId() = runTest {
        catalogoYaGuardado(albumJson(200), albumJson(201), version = "W/\"c1\"")
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c2\""))

        repositorio.getAlbums().getOrThrow()

        assertEquals(listOf("borrarAlbumesPorId[201]", "borrarDetallesPorId[201]"), dao.operaciones)
        assertEquals(listOf(200), dao.filas.map { it.id })
        assertEquals(setOf(200), dao.detalles.keys)
    }

    @Test
    fun trasUnFalloAlGuardarLaSiguienteConsultaSinCambiosVuelveAGuardarTodo() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(catalogo(albumJson(200, "Nombre nuevo"), version = "W/\"c2\""))
        repositorio.getAlbums().getOrThrow()
        dao.fallaAlEscribir = null
        servidor.enqueue(catalogo(albumJson(200, "Nombre nuevo"), version = "W/\"c2\""))

        repositorio.getAlbums().getOrThrow()

        assertEquals("borrarTodos", dao.operaciones.first())
        assertEquals(listOf("Nombre nuevo"), dao.filas.map { it.nombre })
    }

    @Test
    fun unaCancelacionAlGuardarHaceQueLaSiguienteConsultaGuardeTodo() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        dao.fallaAlEscribir = CancellationException("cancelada")
        servidor.enqueue(catalogo(albumJson(200, "Nombre nuevo"), version = "W/\"c2\""))
        try {
            repositorio.getAlbums()
            fail("La cancelación debe relanzarse")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
        dao.fallaAlEscribir = null
        // The backend reverts to the previous version: without discarding what's remembered, nothing would be written.
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c1\""))

        repositorio.getAlbums().getOrThrow()

        assertEquals("borrarTodos", dao.operaciones.first())
    }

    @Test
    fun unDetalleIgualAlQueGuardoElCatalogoNoSeEscribe() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        servidor.enqueue(respuesta(albumJson(200), "W/\"d1\""))

        repositorio.getAlbum(200).getOrThrow()

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun unDetalleConLaMismaVersionNoSeEscribeDosVeces() = runTest {
        servidor.enqueue(respuesta(albumJson(200), "W/\"d1\""))
        servidor.enqueue(respuesta(albumJson(200), "W/\"d1\""))

        repositorio.getAlbum(200).getOrThrow()
        repositorio.getAlbum(200).getOrThrow()

        assertEquals(listOf("guardarDetalle[200]"), dao.operaciones)
    }

    @Test
    fun escribirUnDetalleDescartaLaVersionDelCatalogo() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        servidor.enqueue(respuesta(albumJson(200, descripcion = "Descripción nueva"), "W/\"d1\""))
        repositorio.getAlbum(200).getOrThrow()
        dao.operaciones.clear()
        // The backend reverts to the catalog's previous version.
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c1\""))

        repositorio.getAlbums().getOrThrow()

        assertEquals(listOf("guardarDetalles[200]"), dao.operaciones)
        assertEquals("Descripción 200", dao.detalles[200]?.descripcion)
    }

    @Test
    fun elCatalogoQueCambiaUnDetalleDescartaLaVersionDeEseDetalle() = runTest {
        servidor.enqueue(respuesta(albumJson(200), "W/\"d1\""))
        repositorio.getAlbum(200).getOrThrow()
        servidor.enqueue(catalogo(albumJson(200, descripcion = "Descripción nueva"), version = "W/\"c1\""))
        repositorio.getAlbums().getOrThrow()
        dao.operaciones.clear()
        // The backend reverts to the detail's previous version.
        servidor.enqueue(respuesta(albumJson(200), "W/\"d1\""))

        repositorio.getAlbum(200).getOrThrow()

        assertEquals(listOf("guardarDetalle[200]"), dao.operaciones)
        assertEquals("Descripción 200", dao.detalles[200]?.descripcion)
    }

    @Test
    fun trasUn404ElCatalogoConLaVersionAnteriorVuelveAGuardarElAlbum() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        servidor.enqueue(MockResponse().setResponseCode(404))
        repositorio.getAlbum(200)
        dao.operaciones.clear()
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c1\""))

        repositorio.getAlbums().getOrThrow()

        assertEquals(listOf("guardarAlbumes[200]", "guardarDetalles[200]"), dao.operaciones)
        assertEquals(listOf(200), dao.filas.map { it.id })
    }

    // ---- Created album (optimizar-cache-e-imagenes, D7) ----

    private val nuevo = com.example.vinilos.data.model.NewAlbum(
        nombre = "Abbey Road",
        portada = "https://example.com/abbey.jpg",
        fechaLanzamientoMillis = -8_380_800_000L,
        descripcion = "Undécimo álbum de estudio",
        genero = "Rock",
        disquera = "EMI"
    )

    @Test
    fun createUnAlbumGuardaLoQueDevuelveElBackend() = runTest {
        servidor.enqueue(respuesta(albumJson(300, "Abbey Road")))

        assertEquals(300, repositorio.createAlbum(nuevo).getOrThrow())

        assertEquals(listOf(300), dao.filas.map { it.id })
        assertEquals("Abbey Road", dao.detalles[300]?.nombre)
        assertEquals(listOf("Abbey Road"), repositorio.getCachedAlbums().map { it.nombre })
    }

    @Test
    fun conUnaRespuestaDeCreacionIncompletaLaCreacionEsSuccesssaSinGuardar() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"id": 300}"""))

        assertEquals(300, repositorio.createAlbum(nuevo).getOrThrow())

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun siNoSePuedeGuardarElAlbumCreadoLaCreacionIgualEsSuccesssa() = runTest {
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(respuesta(albumJson(300, "Abbey Road")))

        assertEquals(300, repositorio.createAlbum(nuevo).getOrThrow())
    }

    @Test
    fun siElBackendVuelveALaVersionPreviaALaCreacionElCatalogoSeReescribeSinElAlbum() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        servidor.enqueue(respuesta(albumJson(300, "Abbey Road")))
        repositorio.createAlbum(nuevo).getOrThrow()
        dao.operaciones.clear()
        // Someone deleted the created album on the backend: the catalog reverts to its earlier version.
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c1\""))

        repositorio.getAlbums().getOrThrow()

        assertEquals(listOf("borrarAlbumesPorId[300]", "borrarDetallesPorId[300]"), dao.operaciones)
        assertEquals(listOf(200), dao.filas.map { it.id })
    }

    // ---- Added track (HU08, D7: don't wait for the next GET to reflect a successful POST) ----

    @Test
    fun agregarUnaTrackActualizaDeInmediatoElDetalleYaGuardado() = runTest {
        servidor.enqueue(respuesta(albumJson(200)))
        repositorio.getAlbum(200).getOrThrow()
        dao.operaciones.clear()
        servidor.enqueue(MockResponse().setResponseCode(200))

        repositorio.addTrack(200, Track("Decisiones", "5:05")).getOrThrow()

        assertEquals(listOf(Track("Decisiones", "5:05")), dao.detalles[200]?.canciones)
        assertTrue("guardarDetalle[200]" in dao.operaciones)
    }

    @Test
    fun agregarUnaTrackAUnAlbumSinDetalleGuardadoNoEscribeNada() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200))

        repositorio.addTrack(999, Track("Tema", "3:15")).getOrThrow()

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun siNoSePuedeGuardarLaTrackAgregadaLaAsociacionIgualEsSuccesssa() = runTest {
        servidor.enqueue(respuesta(albumJson(200)))
        repositorio.getAlbum(200).getOrThrow()
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(MockResponse().setResponseCode(200))

        assertTrue(repositorio.addTrack(200, Track("Decisiones", "5:05")).isSuccess)
    }
}
