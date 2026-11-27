package com.example.vinilos.data.repository

import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Associate an album with an artist (HU15, add-agregar-album-artista D1). The response bodies mimic those
 * from BackVynils verified on 2026-11-19: the musician's `POST` returns the album and the band's `PUT` returns the band.
 */
class ArtistRepositoryAsociarTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: ArtistRepository

    private val respuestas = mutableMapOf<String, MockResponse>()
    private val peticiones = mutableListOf<RecordedRequest>()

    private val queenConDosAlbumes = """
        {"id": 101, "name": "Queen", "image": "q", "description": "Banda", "creationDate": "1970-01-01T00:00:00.000Z",
         "albums": [
           {"id": 102, "name": "A Night at the Opera", "cover": "c", "releaseDate": "1975-11-21T00:00:00.000Z"},
           {"id": 103, "name": "A Day at the Races", "cover": "c", "releaseDate": "1976-12-10T00:00:00.000Z"}
         ],
         "musicians": [], "performerPrizes": []}
    """.trimIndent()

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(peticiones) { peticiones += request }
                return respuestas["${request.method} ${request.path}"] ?: MockResponse().setResponseCode(404)
            }
        }
        servidor.start()
        val cliente = OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .build()
        repositorio = ArtistRepositoryImpl(ServiceAdapter(servidor.url("/").toString(), cliente))
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    private fun rutas() = synchronized(peticiones) { peticiones.map { "${it.method} ${it.path}" } }

    private fun cuerpoDelPut() = synchronized(peticiones) { peticiones.single { it.method == "PUT" }.body.readUtf8() }

    // ---------- Musician ----------

    @Test
    fun aUnMusicoLoAsociaConElPostDelMusico() = runTest {
        respuestas["POST /musicians/100/albums/102"] = MockResponse().setBody("""{"id": 102, "name": "A Night at the Opera"}""")

        val resultado = repositorio.associateAlbum(ArtistType.MUSICO, 100, 102)

        assertTrue(resultado.isSuccess)
        assertEquals(listOf("POST /musicians/100/albums/102"), rutas())
    }

    @Test
    fun unErrorDelServidorAlAsociarAUnMusicoEsLoadError() = runTest {
        respuestas["POST /musicians/100/albums/102"] = MockResponse().setResponseCode(500)

        val error = repositorio.associateAlbum(ArtistType.MUSICO, 100, 102).exceptionOrNull()

        assertTrue("Fue $error", error is LoadError)
    }

    @Test
    fun unMusicoOAlbumInexistenteEsLoadError() = runTest {
        // Without a configured response, the fake server responds with 404, like BackVynils.
        val error = repositorio.associateAlbum(ArtistType.MUSICO, 999, 102).exceptionOrNull()

        assertTrue("Fue $error", error is LoadError)
    }

    @Test
    fun sinConexionAlAsociarEsLoadError() = runTest {
        respuestas["POST /musicians/100/albums/102"] = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)

        val error = repositorio.associateAlbum(ArtistType.MUSICO, 100, 102).exceptionOrNull()

        assertTrue("Fue $error", error is LoadError)
    }

    // ---------- Band ----------

    @Test
    fun aUnaBandaLaAsociaConSusAlbumesActualesMasElNuevo() = runTest {
        respuestas["GET /bands/101"] = MockResponse().setBody(queenConDosAlbumes)
        respuestas["PUT /bands/101/albums"] = MockResponse().setBody(queenConDosAlbumes)

        val resultado = repositorio.associateAlbum(ArtistType.BANDA, 101, 100)

        assertTrue(resultado.isSuccess)
        assertEquals(listOf("GET /bands/101", "PUT /bands/101/albums"), rutas())
        assertEquals("""[{"id":102},{"id":103},{"id":100}]""", cuerpoDelPut())
    }

    @Test
    fun nuncaUsaElPostDeBandaQueQuitaALosOtrosInterpretes() = runTest {
        respuestas["GET /bands/101"] = MockResponse().setBody(queenConDosAlbumes)
        respuestas["PUT /bands/101/albums"] = MockResponse().setBody(queenConDosAlbumes)

        repositorio.associateAlbum(ArtistType.BANDA, 101, 100)

        assertTrue(rutas().none { it.startsWith("POST") })
    }

    @Test
    fun siLaBandaYaTieneElAlbumNoLoRepiteEnElPut() = runTest {
        respuestas["GET /bands/101"] = MockResponse().setBody(queenConDosAlbumes)
        respuestas["PUT /bands/101/albums"] = MockResponse().setBody(queenConDosAlbumes)

        assertTrue(repositorio.associateAlbum(ArtistType.BANDA, 101, 103).isSuccess)

        assertEquals("""[{"id":102},{"id":103}]""", cuerpoDelPut())
    }

    @Test
    fun unaBandaSinAlbumesRecibeSoloElNuevo() = runTest {
        respuestas["GET /bands/101"] = MockResponse().setBody("""{"id": 101, "name": "Queen", "albums": []}""")
        respuestas["PUT /bands/101/albums"] = MockResponse().setBody("""{"id": 101, "name": "Queen", "albums": []}""")

        assertTrue(repositorio.associateAlbum(ArtistType.BANDA, 101, 100).isSuccess)

        assertEquals("""[{"id":100}]""", cuerpoDelPut())
    }

    @Test
    fun siNoPuedeLeerLosAlbumesDeLaBandaNoEnviaElPut() = runTest {
        respuestas["GET /bands/101"] = MockResponse().setResponseCode(500)

        val error = repositorio.associateAlbum(ArtistType.BANDA, 101, 100).exceptionOrNull()

        assertTrue("Fue $error", error is LoadError)
        assertEquals(listOf("GET /bands/101"), rutas())
    }

    @Test
    fun siLaBandaLlegaSinListaDeAlbumesNoEnviaElPut() = runTest {
        // Without "albums" there's no way to know what the band has: a PUT with only the new one would erase the rest.
        respuestas["GET /bands/101"] = MockResponse().setBody("""{"id": 101, "name": "Queen"}""")

        val error = repositorio.associateAlbum(ArtistType.BANDA, 101, 100).exceptionOrNull()

        assertTrue("Fue $error", error is LoadError)
        assertEquals(listOf("GET /bands/101"), rutas())
    }

    @Test
    fun siUnAlbumDeLaBandaLlegaSinIdNoEnviaElPut() = runTest {
        respuestas["GET /bands/101"] = MockResponse().setBody(
            """{"id": 101, "name": "Queen", "albums": [{"name": "Sin id"}, {"id": 103, "name": "A Day at the Races"}]}"""
        )

        val error = repositorio.associateAlbum(ArtistType.BANDA, 101, 100).exceptionOrNull()

        assertTrue("Fue $error", error is LoadError)
        assertEquals(listOf("GET /bands/101"), rutas())
    }

    @Test
    fun unErrorDelPutDeBandaEsLoadError() = runTest {
        respuestas["GET /bands/101"] = MockResponse().setBody(queenConDosAlbumes)
        respuestas["PUT /bands/101/albums"] = MockResponse().setResponseCode(500)

        val error = repositorio.associateAlbum(ArtistType.BANDA, 101, 100).exceptionOrNull()

        assertTrue("Fue $error", error is LoadError)
    }

    // ---------- Cancellation ----------

    @Test
    fun laCancelacionSePropagaSinConvertirseEnLoadError() = runTest {
        respuestas["POST /musicians/100/albums/102"] = MockResponse().setHeadersDelay(5, TimeUnit.SECONDS)
        var resultado: Result<Unit>? = null

        val tarea = launch(Dispatchers.IO) { resultado = repositorio.associateAlbum(ArtistType.MUSICO, 100, 102) }
        servidor.takeRequest(2, TimeUnit.SECONDS)
        tarea.cancelAndJoin()

        assertEquals(null, resultado)
        assertTrue(tarea.isCancelled)
    }
}
