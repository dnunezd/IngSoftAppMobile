package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.AlbumDetalleEntity
import com.example.vinilos.data.local.AlbumEntity
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.remote.ErrorDeCarga
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

/** Caché del catálogo en el repositorio (add-cache-catalogo, D4 y D7), con un DAO falso en memoria. */
class AlbumRepositoryCacheTest {

    /** Guarda en una lista; [fallaAlLeer] y [fallaAlEscribir] simulan un almacenamiento que no responde. */
    private class AlbumDaoFalso(inicial: List<AlbumEntity> = emptyList()) : AlbumDao() {
        var filas: List<AlbumEntity> = inicial
        var fallaAlLeer: Throwable? = null
        var fallaAlEscribir: Throwable? = null
        var escrituras = 0

        /** Cada escritura que recibió, en orden, con los ids que tocó (optimizar-cache-e-imagenes). */
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

        /** Detalles guardados por id (add-cache-consultas). */
        var detalles: Map<Int, AlbumDetalleEntity> = emptyMap()

        override suspend fun detalle(id: Int): AlbumDetalleEntity? {
            fallaAlLeer?.let { throw it }
            return detalles[id]
        }

        override suspend fun guardarDetalle(detalle: AlbumDetalleEntity) {
            anotar("guardarDetalle", listOf(detalle.id))
            detalles = detalles + (detalle.id to detalle)
        }

        override suspend fun borrarDetalles() {
            anotar("borrarDetalles")
            detalles = emptyMap()
        }

        override suspend fun insertarDetalles(detalles: List<AlbumDetalleEntity>) {
            anotar("insertarDetalles", detalles.map { it.id })
            this.detalles = this.detalles + detalles.associateBy { it.id }
        }

        override suspend fun borrarDelCatalogo(id: Int) {
            anotar("borrarDelCatalogo", listOf(id))
            filas = filas.filterNot { it.id == id }
        }

        override suspend fun borrarDetalle(id: Int) {
            anotar("borrarDetalle", listOf(id))
            detalles = detalles - id
        }

        override suspend fun guardarAlbumes(albumes: List<AlbumEntity>) {
            anotar("guardarAlbumes", albumes.map { it.id })
            filas = filas.filterNot { fila -> albumes.any { it.id == fila.id } } + albumes
        }

        override suspend fun guardarDetalles(detalles: List<AlbumDetalleEntity>) {
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

    private val buscandoAmerica = AlbumResumen(
        id = 100, nombre = "Buscando América", portada = "https://example.com/100.jpg",
        genero = "Salsa", artista = "Rubén Blades", anio = "1984"
    )
    private val nightAtTheOpera = AlbumResumen(
        id = 102, nombre = "A Night at the Opera", portada = null, genero = "Rock", artista = "", anio = null
    )

    private fun fila(album: AlbumResumen) =
        AlbumEntity(album.id, album.nombre, album.portada, album.genero, album.artista, album.anio)

    @Test
    fun unaConsultaExitosaReemplazaLoGuardadoConLosAlbumesValidos() = runTest {
        dao.filas = listOf(AlbumEntity(999, "Ya no existe", null, "Pop", "", null))
        servidor.enqueue(MockResponse().setBody(catalogo))

        val albumes = repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), albumes)
        assertEquals(setOf(fila(nightAtTheOpera), fila(buscandoAmerica)), dao.filas.toSet())
    }

    @Test
    fun unaRespuestaVaciaDejaLaCacheVacia() = runTest {
        dao.filas = listOf(fila(buscandoAmerica))
        servidor.enqueue(MockResponse().setBody("[]"))

        repositorio.obtenerAlbumes().getOrThrow()

        assertTrue(dao.filas.isEmpty())
    }

    @Test
    fun unaConsultaFallidaNoTocaLoGuardado() = runTest {
        dao.filas = listOf(fila(buscandoAmerica))
        servidor.enqueue(MockResponse().setResponseCode(500))

        val error = repositorio.obtenerAlbumes().exceptionOrNull()

        assertTrue(error is ErrorDeCarga)
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

        val nombres = repositorio.obtenerAlbumesGuardados().map { it.nombre }

        assertEquals(listOf("abbey Road", "Álbum blanco", "Buscando América", "Poeta del pueblo"), nombres)
    }

    @Test
    fun loGuardadoConservaTodosLosCamposDelAlbum() = runTest {
        dao.filas = listOf(fila(buscandoAmerica), fila(nightAtTheOpera))

        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), repositorio.obtenerAlbumesGuardados())
    }

    @Test
    fun sinDaoNoHayNadaGuardadoYElCatalogoSeConsultaComoAntes() = runTest {
        val sinCache = AlbumRepositoryImpl(ServiceAdapter(servidor.url("/").toString()))
        servidor.enqueue(MockResponse().setBody(catalogo))

        assertTrue(sinCache.obtenerAlbumesGuardados().isEmpty())
        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), sinCache.obtenerAlbumes().getOrThrow())
    }

    // ---- Fallos del almacenamiento (D7): la caché es una mejora, nunca una condición ----

    @Test
    fun siNoSePuedeLeerLaCacheNoHayNadaGuardado() = runTest {
        dao.filas = listOf(fila(buscandoAmerica))
        dao.fallaAlLeer = SQLiteException("disco no disponible")

        assertTrue(repositorio.obtenerAlbumesGuardados().isEmpty())
    }

    @Test
    fun siNoSePuedeEscribirLaCacheSeDevuelveIgualLaRespuestaDeRed() = runTest {
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(MockResponse().setBody(catalogo))

        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), repositorio.obtenerAlbumes().getOrThrow())
    }

    @Test
    fun laCancelacionAlLeerLaCacheSeRelanza() = runTest {
        dao.fallaAlLeer = CancellationException("cancelada")

        try {
            repositorio.obtenerAlbumesGuardados()
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
            repositorio.obtenerAlbumes()
            fail("La cancelación debe relanzarse")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
    }

    // ---- Detalle guardado (add-cache-consultas, D4) ----

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
        canciones = listOf(Cancion("Decisiones", "5:05")),
        artistas = listOf(Artista("Rubén Blades", null)),
        comentarios = listOf(Comentario("Excelente", 5))
    )

    private fun detalleGuardado(album: Album = albumCompleto) = AlbumDetalleEntity(
        album.id, album.nombre, album.portada, album.descripcion, album.genero, album.disquera,
        album.fechaLanzamiento, album.canciones, album.artistas, album.comentarios
    )

    @Test
    fun elCatalogoGuardaElDetalleDeLosAlbumesCompletosSinPeticionesNuevas() = runTest {
        // El 102 no tiene descripción, disquera ni fecha: queda en el catálogo, pero sin detalle.
        servidor.enqueue(MockResponse().setBody("[$albumCompletoJson, {\"id\": 102, \"name\": \"A Night at the Opera\", \"genre\": \"Rock\"}]"))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(setOf(200, 102), dao.filas.map { it.id }.toSet())
        assertEquals(mapOf(200 to detalleGuardado()), dao.detalles)
        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun elCatalogoBorraElDetalleDeLosAlbumesQueYaNoVienen() = runTest {
        dao.detalles = mapOf(999 to detalleGuardado(albumCompleto.copy(id = 999)))
        servidor.enqueue(MockResponse().setBody("[$albumCompletoJson]"))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(setOf(200), dao.detalles.keys)
    }

    @Test
    fun obtenerAlbumGuardadoDevuelveElDetalleGuardado() = runTest {
        dao.detalles = mapOf(200 to detalleGuardado())

        assertEquals(albumCompleto, repositorio.obtenerAlbumGuardado(200))
        assertNull(repositorio.obtenerAlbumGuardado(201))
    }

    @Test
    fun abrirElDetalleConConexionGuardaOReemplazaSuDetalle() = runTest {
        dao.detalles = mapOf(200 to detalleGuardado(albumCompleto.copy(nombre = "Nombre viejo")))
        servidor.enqueue(MockResponse().setBody(albumCompletoJson))

        assertEquals(albumCompleto, repositorio.obtenerAlbum(200).getOrThrow())
        assertEquals(detalleGuardado(), dao.detalles[200])
    }

    @Test
    fun un404BorraElAlbumDelCatalogoYDelDetalleGuardados() = runTest {
        dao.filas = listOf(AlbumEntity(200, "Buscando América", null, "Salsa", "", null))
        dao.detalles = mapOf(200 to detalleGuardado())
        servidor.enqueue(MockResponse().setResponseCode(404))

        val error = repositorio.obtenerAlbum(200).exceptionOrNull()

        assertTrue(error is AlbumNoDisponible)
        assertTrue(dao.filas.isEmpty())
        assertTrue(dao.detalles.isEmpty())
    }

    @Test
    fun unFalloDeRedNoTocaElDetalleGuardado() = runTest {
        dao.detalles = mapOf(200 to detalleGuardado())
        servidor.enqueue(MockResponse().setResponseCode(500))

        assertTrue(repositorio.obtenerAlbum(200).exceptionOrNull() is ErrorDeCarga)
        assertEquals(mapOf(200 to detalleGuardado()), dao.detalles)
    }

    @Test
    fun sinDaoNoHayDetalleGuardadoYElDetalleSeConsultaComoAntes() = runTest {
        val sinCache = AlbumRepositoryImpl(ServiceAdapter(servidor.url("/").toString()))
        servidor.enqueue(MockResponse().setBody(albumCompletoJson))

        assertNull(sinCache.obtenerAlbumGuardado(200))
        assertEquals(albumCompleto, sinCache.obtenerAlbum(200).getOrThrow())
    }

    @Test
    fun siNoSePuedeLeerElDetalleNoHayNadaGuardado() = runTest {
        dao.detalles = mapOf(200 to detalleGuardado())
        dao.fallaAlLeer = SQLiteException("disco no disponible")

        assertNull(repositorio.obtenerAlbumGuardado(200))
    }

    @Test
    fun siNoSePuedeEscribirElDetalleSeDevuelveIgualElAlbum() = runTest {
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(MockResponse().setBody(albumCompletoJson))
        servidor.enqueue(MockResponse().setResponseCode(404))

        assertEquals(albumCompleto, repositorio.obtenerAlbum(200).getOrThrow())
        assertTrue(repositorio.obtenerAlbum(200).exceptionOrNull() is AlbumNoDisponible)
    }

    // ---- Escribir solo lo que cambió (optimizar-cache-e-imagenes, D4 y D5) ----

    private fun albumJson(id: Int, nombre: String = "Álbum $id", descripcion: String = "Descripción $id") = """
        {"id": $id, "name": "$nombre", "cover": "https://example.com/$id.jpg", "description": "$descripcion",
         "genre": "Salsa", "recordLabel": "Elektra", "releaseDate": "1984-08-01T00:00:00.000Z",
         "tracks": [], "performers": [{"id": 1, "name": "Rubén Blades"}], "comments": []}
    """.trimIndent()

    private fun respuesta(cuerpo: String, version: String? = null) =
        MockResponse().setBody(cuerpo).apply { if (version != null) setHeader("ETag", version) }

    private fun catalogo(vararg albumes: String, version: String? = null) =
        respuesta(albumes.joinToString(prefix = "[", postfix = "]"), version)

    /** Hace una consulta del catálogo que deja la copia completa y limpia el registro de escrituras. */
    private suspend fun catalogoYaGuardado(vararg albumes: String, version: String? = null) {
        servidor.enqueue(catalogo(*albumes, version = version))
        repositorio.obtenerAlbumes().getOrThrow()
        dao.operaciones.clear()
    }

    @Test
    fun laPrimeraConsultaReemplazaTodoLoGuardado() = runTest {
        servidor.enqueue(catalogo(albumJson(200), albumJson(201), version = "W/\"c1\""))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals("borrarTodos", dao.operaciones.first())
        assertTrue("borrarDetalles" in dao.operaciones)
    }

    @Test
    fun conLaMismaVersionNoSeEscribeNada() = runTest {
        catalogoYaGuardado(albumJson(200), albumJson(201), version = "W/\"c1\"")
        servidor.enqueue(catalogo(albumJson(200), albumJson(201), version = "W/\"c1\""))

        val albumes = repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(listOf(200, 201), albumes.map { it.id })
        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun conOtraVersionYElMismoContenidoNoSeEscribeNada() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c2\""))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun sinVersionYConElMismoContenidoNoSeEscribeNada() = runTest {
        catalogoYaGuardado(albumJson(200))
        servidor.enqueue(catalogo(albumJson(200)))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun unAlbumCambiadoEscribeSoloSuFilaYSuDetalle() = runTest {
        catalogoYaGuardado(albumJson(200), albumJson(201), version = "W/\"c1\"")
        servidor.enqueue(catalogo(albumJson(200, "Nombre nuevo"), albumJson(201), version = "W/\"c2\""))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(listOf("guardarAlbumes[200]", "guardarDetalles[200]"), dao.operaciones)
        assertEquals(setOf("Nombre nuevo", "Álbum 201"), dao.filas.map { it.nombre }.toSet())
        assertEquals("Nombre nuevo", dao.detalles[200]?.nombre)
    }

    @Test
    fun unAlbumQueYaNoLlegaSeBorraSoloPorSuId() = runTest {
        catalogoYaGuardado(albumJson(200), albumJson(201), version = "W/\"c1\"")
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c2\""))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(listOf("borrarAlbumesPorId[201]", "borrarDetallesPorId[201]"), dao.operaciones)
        assertEquals(listOf(200), dao.filas.map { it.id })
        assertEquals(setOf(200), dao.detalles.keys)
    }

    @Test
    fun trasUnFalloAlGuardarLaSiguienteConsultaSinCambiosVuelveAGuardarTodo() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(catalogo(albumJson(200, "Nombre nuevo"), version = "W/\"c2\""))
        repositorio.obtenerAlbumes().getOrThrow()
        dao.fallaAlEscribir = null
        servidor.enqueue(catalogo(albumJson(200, "Nombre nuevo"), version = "W/\"c2\""))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals("borrarTodos", dao.operaciones.first())
        assertEquals(listOf("Nombre nuevo"), dao.filas.map { it.nombre })
    }

    @Test
    fun unaCancelacionAlGuardarHaceQueLaSiguienteConsultaGuardeTodo() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        dao.fallaAlEscribir = CancellationException("cancelada")
        servidor.enqueue(catalogo(albumJson(200, "Nombre nuevo"), version = "W/\"c2\""))
        try {
            repositorio.obtenerAlbumes()
            fail("La cancelación debe relanzarse")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
        dao.fallaAlEscribir = null
        // El backend vuelve a la versión anterior: sin descartar lo recordado, no se escribiría nada.
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c1\""))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals("borrarTodos", dao.operaciones.first())
    }

    @Test
    fun unDetalleIgualAlQueGuardoElCatalogoNoSeEscribe() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        servidor.enqueue(respuesta(albumJson(200), "W/\"d1\""))

        repositorio.obtenerAlbum(200).getOrThrow()

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun unDetalleConLaMismaVersionNoSeEscribeDosVeces() = runTest {
        servidor.enqueue(respuesta(albumJson(200), "W/\"d1\""))
        servidor.enqueue(respuesta(albumJson(200), "W/\"d1\""))

        repositorio.obtenerAlbum(200).getOrThrow()
        repositorio.obtenerAlbum(200).getOrThrow()

        assertEquals(listOf("guardarDetalle[200]"), dao.operaciones)
    }

    @Test
    fun escribirUnDetalleDescartaLaVersionDelCatalogo() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        servidor.enqueue(respuesta(albumJson(200, descripcion = "Descripción nueva"), "W/\"d1\""))
        repositorio.obtenerAlbum(200).getOrThrow()
        dao.operaciones.clear()
        // El backend vuelve a la versión anterior del catálogo.
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c1\""))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(listOf("guardarDetalles[200]"), dao.operaciones)
        assertEquals("Descripción 200", dao.detalles[200]?.descripcion)
    }

    @Test
    fun elCatalogoQueCambiaUnDetalleDescartaLaVersionDeEseDetalle() = runTest {
        servidor.enqueue(respuesta(albumJson(200), "W/\"d1\""))
        repositorio.obtenerAlbum(200).getOrThrow()
        servidor.enqueue(catalogo(albumJson(200, descripcion = "Descripción nueva"), version = "W/\"c1\""))
        repositorio.obtenerAlbumes().getOrThrow()
        dao.operaciones.clear()
        // El backend vuelve a la versión anterior del detalle.
        servidor.enqueue(respuesta(albumJson(200), "W/\"d1\""))

        repositorio.obtenerAlbum(200).getOrThrow()

        assertEquals(listOf("guardarDetalle[200]"), dao.operaciones)
        assertEquals("Descripción 200", dao.detalles[200]?.descripcion)
    }

    @Test
    fun trasUn404ElCatalogoConLaVersionAnteriorVuelveAGuardarElAlbum() = runTest {
        catalogoYaGuardado(albumJson(200), version = "W/\"c1\"")
        servidor.enqueue(MockResponse().setResponseCode(404))
        repositorio.obtenerAlbum(200)
        dao.operaciones.clear()
        servidor.enqueue(catalogo(albumJson(200), version = "W/\"c1\""))

        repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(listOf("guardarAlbumes[200]", "guardarDetalles[200]"), dao.operaciones)
        assertEquals(listOf(200), dao.filas.map { it.id })
    }
}
