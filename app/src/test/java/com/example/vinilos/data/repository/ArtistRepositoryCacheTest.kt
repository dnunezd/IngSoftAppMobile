package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.ArtistaDao
import com.example.vinilos.data.local.ArtistaDetalleEntity
import com.example.vinilos.data.local.ArtistaEntity
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.RecursoNoEncontrado
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Listado y detalle de artistas guardados (add-cache-consultas, D4), con un DAO falso en memoria. */
class ArtistRepositoryCacheTest {

    /** Guarda en memoria; [fallaAlLeer] y [fallaAlEscribir] simulan un almacenamiento que no responde. */
    private class ArtistaDaoFalso : ArtistaDao() {
        var filas: List<ArtistaEntity> = emptyList()
        var detalles: Map<Pair<ArtistType, Int>, ArtistaDetalleEntity> = emptyMap()
        var fallaAlLeer: Throwable? = null
        var fallaAlEscribir: Throwable? = null

        /** Cada escritura que recibió, en orden, con las claves que tocó (optimizar-cache-e-imagenes). */
        val operaciones = mutableListOf<String>()

        private fun anotar(operacion: String, claves: List<Any> = emptyList()) {
            fallaAlEscribir?.let { throw it }
            operaciones += if (claves.isEmpty()) operacion else "$operacion$claves"
        }

        override suspend fun todos(): List<ArtistaEntity> {
            fallaAlLeer?.let { throw it }
            return filas
        }

        override suspend fun borrarTodos() {
            anotar("borrarTodos")
            filas = emptyList()
        }

        override suspend fun insertar(artistas: List<ArtistaEntity>) {
            anotar("insertar", artistas.map { it.id })
            filas = filas + artistas
        }

        override suspend fun detalle(tipo: ArtistType, id: Int): ArtistaDetalleEntity? {
            fallaAlLeer?.let { throw it }
            return detalles[tipo to id]
        }

        override suspend fun guardarDetalle(detalle: ArtistaDetalleEntity) {
            anotar("guardarDetalle", listOf(detalle.id))
            detalles = detalles + ((detalle.tipo to detalle.id) to detalle)
        }

        override suspend fun borrarDetalle(tipo: ArtistType, id: Int) {
            anotar("borrarDetalle", listOf(id))
            detalles = detalles - (tipo to id)
        }

        override suspend fun guardarArtistas(artistas: List<ArtistaEntity>) {
            anotar("guardarArtistas", artistas.map { it.id })
            filas = filas.filterNot { fila -> artistas.any { it.tipo == fila.tipo && it.id == fila.id } } + artistas
        }

        override suspend fun borrarArtistas(tipo: ArtistType, ids: List<Int>) {
            anotar("borrarArtistas", ids)
            filas = filas.filterNot { it.tipo == tipo && it.id in ids }
        }
    }

    private lateinit var servidor: MockWebServer
    private lateinit var dao: ArtistaDaoFalso
    private lateinit var repositorio: ArtistRepository

    // Las dos consultas del listado van en paralelo, así que se responde por ruta y no en orden.
    private var musicos: MockResponse = MockResponse().setBody("[]")
    private var bandas: MockResponse = MockResponse().setBody("[]")
    private val otrasRutas = mutableMapOf<String, MockResponse>()

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/musicians" -> musicos
                "/bands" -> bandas
                else -> otrasRutas[request.path] ?: MockResponse().setResponseCode(404)
            }
        }
        servidor.start()
        dao = ArtistaDaoFalso()
        repositorio = ArtistRepositoryImpl(ServiceAdapter(servidor.url("/").toString()), artistaDao = dao)
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    private fun fila(tipo: ArtistType, id: Int, nombre: String) = ArtistaEntity(tipo, id, nombre, null)

    // ---------- Listado (HU03) ----------

    @Test
    fun unListadoExitosoReemplazaLoGuardado() = runTest {
        dao.filas = listOf(fila(ArtistType.BANDA, 999, "Ya no existe"))
        musicos = MockResponse().setBody("""[{"id": 7, "name": "Freddie Mercury", "image": null}]""")
        bandas = MockResponse().setBody("""[{"id": 7, "name": "Queen", "image": null}]""")

        repositorio.obtenerArtistas().getOrThrow()

        assertEquals(
            setOf(fila(ArtistType.MUSICO, 7, "Freddie Mercury"), fila(ArtistType.BANDA, 7, "Queen")),
            dao.filas.toSet()
        )
    }

    @Test
    fun loGuardadoSeDevuelveEnOrdenAlfabeticoSinDistinguirTildes() = runTest {
        dao.filas = listOf(
            fila(ArtistType.BANDA, 1, "Zeta"),
            fila(ArtistType.MUSICO, 2, "Ástor Piazzolla"),
            fila(ArtistType.MUSICO, 3, "alberto Barros")
        )

        assertEquals(
            listOf("alberto Barros", "Ástor Piazzolla", "Zeta"),
            repositorio.obtenerArtistasGuardados().map { it.name }
        )
    }

    @Test
    fun loGuardadoConservaElTipoDeCadaArtista() = runTest {
        dao.filas = listOf(fila(ArtistType.BANDA, 7, "Queen"), fila(ArtistType.MUSICO, 7, "Freddie Mercury"))

        assertEquals(
            listOf(Artist(7, "Freddie Mercury", null, ArtistType.MUSICO), Artist(7, "Queen", null, ArtistType.BANDA)),
            repositorio.obtenerArtistasGuardados()
        )
    }

    @Test
    fun siFallaUnaDeLasDosConsultasLoGuardadoNoCambia() = runTest {
        val guardado = listOf(fila(ArtistType.BANDA, 1, "Queen"))
        dao.filas = guardado
        musicos = MockResponse().setBody("""[{"id": 2, "name": "Rubén Blades", "image": null}]""")
        bandas = MockResponse().setResponseCode(500)

        assertTrue(repositorio.obtenerArtistas().exceptionOrNull() is ErrorDeCarga)
        assertEquals(guardado, dao.filas)
    }

    // ---------- Detalle (HU04) ----------

    private val queenJson = """
        {"id": 101, "name": "Queen", "image": "https://img/queen.jpg", "description": "Banda británica",
         "creationDate": "1970-01-01T00:00:00.000Z", "albums": [], "musicians": [], "performerPrizes": []}
    """.trimIndent()

    private val queen = ArtistaDetalle(
        101, "Queen", "https://img/queen.jpg", "Banda británica", ArtistType.BANDA, "1 de enero de 1970",
        emptyList(), emptyList(), emptyList()
    )

    private fun guardado(detalle: ArtistaDetalle) = ArtistaDetalleEntity(
        detalle.tipo, detalle.id, detalle.nombre, detalle.imagen, detalle.descripcion, detalle.fecha,
        detalle.integrantes, detalle.albumes, detalle.premios
    )

    @Test
    fun abrirElDetalleConConexionLoGuarda() = runTest {
        otrasRutas["/bands/101"] = MockResponse().setBody(queenJson)

        assertEquals(queen, repositorio.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow())
        assertEquals(guardado(queen), dao.detalles[ArtistType.BANDA to 101])
    }

    @Test
    fun obtenerDetalleGuardadoDistingueElTipo() = runTest {
        dao.detalles = mapOf((ArtistType.BANDA to 101) to guardado(queen))

        assertEquals(queen, repositorio.obtenerDetalleGuardado(ArtistType.BANDA, 101))
        assertNull(repositorio.obtenerDetalleGuardado(ArtistType.MUSICO, 101))
    }

    @Test
    fun un404BorraElDetalleGuardadoYSigueSiendoErrorDeCarga() = runTest {
        dao.detalles = mapOf((ArtistType.BANDA to 101) to guardado(queen))
        otrasRutas["/bands/101"] = MockResponse().setResponseCode(404)

        val error = repositorio.obtenerDetalle(ArtistType.BANDA, 101).exceptionOrNull()

        assertTrue(error is RecursoNoEncontrado)
        assertTrue(error is ErrorDeCarga)
        assertTrue(dao.detalles.isEmpty())
    }

    @Test
    fun unFalloDeRedNoTocaElDetalleGuardado() = runTest {
        dao.detalles = mapOf((ArtistType.BANDA to 101) to guardado(queen))
        otrasRutas["/bands/101"] = MockResponse().setResponseCode(500)

        assertTrue(repositorio.obtenerDetalle(ArtistType.BANDA, 101).exceptionOrNull() is ErrorDeCarga)
        assertEquals(guardado(queen), dao.detalles[ArtistType.BANDA to 101])
    }

    // ---------- Sin DAO y fallos del almacenamiento ----------

    @Test
    fun sinDaoNoHayNadaGuardadoYSeConsultaComoAntes() = runTest {
        val sinCache = ArtistRepositoryImpl(ServiceAdapter(servidor.url("/").toString()))
        otrasRutas["/bands/101"] = MockResponse().setBody(queenJson)

        assertTrue(sinCache.obtenerArtistasGuardados().isEmpty())
        assertNull(sinCache.obtenerDetalleGuardado(ArtistType.BANDA, 101))
        assertEquals(queen, sinCache.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow())
    }

    @Test
    fun siNoSePuedeLeerNoHayNadaGuardado() = runTest {
        dao.filas = listOf(fila(ArtistType.BANDA, 1, "Queen"))
        dao.detalles = mapOf((ArtistType.BANDA to 101) to guardado(queen))
        dao.fallaAlLeer = SQLiteException("disco no disponible")

        assertTrue(repositorio.obtenerArtistasGuardados().isEmpty())
        assertNull(repositorio.obtenerDetalleGuardado(ArtistType.BANDA, 101))
    }

    @Test
    fun siNoSePuedeEscribirSeDevuelveIgualLaRespuestaDeRed() = runTest {
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        bandas = MockResponse().setBody("""[{"id": 101, "name": "Queen", "image": null}]""")
        otrasRutas["/bands/101"] = MockResponse().setBody(queenJson)

        assertEquals(listOf(Artist(101, "Queen", null, ArtistType.BANDA)), repositorio.obtenerArtistas().getOrThrow())
        assertEquals(queen, repositorio.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow())
    }

    // ---------- Escribir solo lo que cambió (optimizar-cache-e-imagenes, D4 y D5) ----------

    private fun conVersion(cuerpo: String, version: String? = null) =
        MockResponse().setBody(cuerpo).apply { if (version != null) setHeader("ETag", version) }

    private fun responderListado(musicosJson: String, bandasJson: String, versionMusicos: String?, versionBandas: String?) {
        musicos = conVersion(musicosJson, versionMusicos)
        bandas = conVersion(bandasJson, versionBandas)
    }

    private val freddieJson = """[{"id": 7, "name": "Freddie Mercury", "image": null}]"""
    private val queenListadoJson = """[{"id": 7, "name": "Queen", "image": null}]"""

    @Test
    fun elListadoConLasMismasVersionesNoSeEscribe() = runTest {
        responderListado(freddieJson, queenListadoJson, "W/\"m1\"", "W/\"b1\"")
        repositorio.obtenerArtistas().getOrThrow()
        dao.operaciones.clear()
        responderListado(freddieJson, queenListadoJson, "W/\"m1\"", "W/\"b1\"")

        assertEquals(2, repositorio.obtenerArtistas().getOrThrow().size)

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun elListadoSinVersionDeUnaParteYConElMismoContenidoNoSeEscribe() = runTest {
        responderListado(freddieJson, queenListadoJson, "W/\"m1\"", null)
        repositorio.obtenerArtistas().getOrThrow()
        dao.operaciones.clear()
        responderListado(freddieJson, queenListadoJson, "W/\"m1\"", null)

        repositorio.obtenerArtistas().getOrThrow()

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun unArtistaNuevoEscribeSoloSuFila() = runTest {
        responderListado(freddieJson, queenListadoJson, "W/\"m1\"", "W/\"b1\"")
        repositorio.obtenerArtistas().getOrThrow()
        dao.operaciones.clear()
        responderListado(
            freddieJson,
            """[{"id": 7, "name": "Queen", "image": null}, {"id": 8, "name": "Seis del Solar", "image": null}]""",
            "W/\"m1\"",
            "W/\"b2\""
        )

        repositorio.obtenerArtistas().getOrThrow()

        assertEquals(listOf("guardarArtistas[8]"), dao.operaciones)
        assertEquals(3, dao.filas.size)
    }

    @Test
    fun unArtistaQueYaNoLlegaSeBorraSoloConSuTipo() = runTest {
        responderListado(freddieJson, queenListadoJson, "W/\"m1\"", "W/\"b1\"")
        repositorio.obtenerArtistas().getOrThrow()
        dao.operaciones.clear()
        responderListado("[]", queenListadoJson, "W/\"m2\"", "W/\"b1\"")

        repositorio.obtenerArtistas().getOrThrow()

        assertEquals(listOf("borrarArtistas[7]"), dao.operaciones)
        assertEquals(listOf(fila(ArtistType.BANDA, 7, "Queen")), dao.filas)
    }

    @Test
    fun elDetalleConLaMismaVersionNoSeEscribeDosVeces() = runTest {
        otrasRutas["/bands/101"] = conVersion(queenJson, "W/\"q1\"")
        repositorio.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow()
        otrasRutas["/bands/101"] = conVersion(queenJson, "W/\"q1\"")

        repositorio.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow()

        assertEquals(listOf("guardarDetalle[101]"), dao.operaciones)
    }

    private val queenConPremioJson = """
        {"id": 101, "name": "Queen", "image": "https://img/queen.jpg", "description": "Banda británica",
         "creationDate": "1970-01-01T00:00:00.000Z", "albums": [], "musicians": [],
         "performerPrizes": [{"id": 100, "premiationDate": "1978-12-10T00:00:00.000Z"}]}
    """.trimIndent()

    private fun premiaciones(nombrePremio: String) =
        """[{"id": 100, "prize": {"name": "$nombrePremio", "organization": "Academia"}}]"""

    @Test
    fun elDetalleSeGuardaSiSoloCambiaronLasPremiacionesDeEseArtista() = runTest {
        otrasRutas["/bands/101"] = conVersion(queenConPremioJson, "W/\"q1\"")
        otrasRutas["/performerprizes"] = conVersion(premiaciones("Grammy"), "W/\"p1\"")
        repositorio.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow()
        dao.operaciones.clear()
        otrasRutas["/bands/101"] = conVersion(queenConPremioJson, "W/\"q1\"")
        otrasRutas["/performerprizes"] = conVersion(premiaciones("Grammy Award"), "W/\"p2\"")

        repositorio.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow()

        assertEquals(listOf("guardarDetalle[101]"), dao.operaciones)
        assertEquals(listOf("Grammy Award"), dao.detalles[ArtistType.BANDA to 101]?.premios?.map { it.nombre })
    }

    @Test
    fun elDetalleNoSeGuardaSiCambioOtraPremiacionPeroNoLaDeEseArtista() = runTest {
        otrasRutas["/bands/101"] = conVersion(queenConPremioJson, "W/\"q1\"")
        otrasRutas["/performerprizes"] = conVersion(premiaciones("Grammy"), "W/\"p1\"")
        repositorio.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow()
        dao.operaciones.clear()
        otrasRutas["/bands/101"] = conVersion(queenConPremioJson, "W/\"q1\"")
        otrasRutas["/performerprizes"] = conVersion(
            """[{"id": 100, "prize": {"name": "Grammy", "organization": "Academia"}},
                {"id": 200, "prize": {"name": "Otro premio", "organization": "Otra"}}]""",
            "W/\"p2\""
        )

        repositorio.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow()

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun trasUn404ElDetalleConLaVersionAnteriorVuelveAGuardarse() = runTest {
        otrasRutas["/bands/101"] = conVersion(queenJson, "W/\"q1\"")
        repositorio.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow()
        otrasRutas["/bands/101"] = MockResponse().setResponseCode(404)
        repositorio.obtenerDetalle(ArtistType.BANDA, 101)
        dao.operaciones.clear()
        otrasRutas["/bands/101"] = conVersion(queenJson, "W/\"q1\"")

        repositorio.obtenerDetalle(ArtistType.BANDA, 101).getOrThrow()

        assertEquals(listOf("guardarDetalle[101]"), dao.operaciones)
    }

    @Test
    fun trasUnFalloAlGuardarElListadoSinCambiosSeVuelveAGuardar() = runTest {
        responderListado(freddieJson, queenListadoJson, "W/\"m1\"", "W/\"b1\"")
        repositorio.obtenerArtistas().getOrThrow()
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        responderListado("[]", queenListadoJson, "W/\"m2\"", "W/\"b1\"")
        repositorio.obtenerArtistas().getOrThrow()
        dao.fallaAlEscribir = null
        dao.operaciones.clear()
        responderListado("[]", queenListadoJson, "W/\"m2\"", "W/\"b1\"")

        repositorio.obtenerArtistas().getOrThrow()

        assertEquals("borrarTodos", dao.operaciones.first())
        assertEquals(listOf(fila(ArtistType.BANDA, 7, "Queen")), dao.filas)
    }
}
