package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
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

class ArtistRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: ArtistRepository

    // Respuesta de cada ruta; las dos consultas van en paralelo, así que se responde por ruta y no en orden.
    private var musicos: MockResponse = MockResponse().setBody("[]")
    private var bandas: MockResponse = MockResponse().setBody("[]")

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/musicians" -> musicos
                "/bands" -> bandas
                else -> MockResponse().setResponseCode(404)
            }
        }
        servidor.start()
        val cliente = OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .build()
        repositorio = ArtistRepository(ServiceAdapter(servidor.url("/").toString(), cliente))
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun uneMusicosYBandasEnUnSoloListado() = runTest {
        musicos = MockResponse().setBody(
            """
            [{"id": 100, "name": "Rubén Blades Bellido de Luna", "image": "https://img/ruben.jpg",
              "description": "Cantante", "birthDate": "1948-07-16T00:00:00.000Z",
              "albums": [], "performerPrizes": []}]
            """.trimIndent()
        )
        bandas = MockResponse().setBody(
            """
            [{"id": 101, "name": "Queen", "image": "https://img/queen.jpg",
              "description": "Banda británica", "creationDate": "1970-01-01T00:00:00.000Z",
              "albums": [], "musicians": [], "performerPrizes": []}]
            """.trimIndent()
        )

        val resultado = repositorio.obtenerArtistas().getOrThrow()

        assertEquals(
            listOf(
                Artist(101, "Queen", "https://img/queen.jpg", ArtistType.BANDA),
                Artist(100, "Rubén Blades Bellido de Luna", "https://img/ruben.jpg", ArtistType.MUSICO)
            ),
            resultado
        )
        val rutas = listOf(servidor.takeRequest().path, servidor.takeRequest().path)
        assertEquals(setOf("/musicians", "/bands"), rutas.toSet())
    }

    @Test
    fun ambasVaciasDevuelveListaVacia() = runTest {
        assertEquals(emptyList<Artist>(), repositorio.obtenerArtistas().getOrThrow())
    }

    @Test
    fun soloMusicosDevuelveLosMusicos() = runTest {
        musicos = MockResponse().setBody("""[{"id": 1, "name": "Ástor Piazzolla", "image": "x"}]""")

        assertEquals(
            listOf(Artist(1, "Ástor Piazzolla", "x", ArtistType.MUSICO)),
            repositorio.obtenerArtistas().getOrThrow()
        )
    }

    @Test
    fun mismoIdEnMusicoYBandaSonArtistasDistintos() = runTest {
        musicos = MockResponse().setBody("""[{"id": 7, "name": "Freddie Mercury", "image": "m"}]""")
        bandas = MockResponse().setBody("""[{"id": 7, "name": "Queen", "image": "b"}]""")

        val resultado = repositorio.obtenerArtistas().getOrThrow()

        assertEquals(
            listOf(
                Artist(7, "Freddie Mercury", "m", ArtistType.MUSICO),
                Artist(7, "Queen", "b", ArtistType.BANDA)
            ),
            resultado
        )
    }

    @Test
    fun ordenaPorNombreSinDistinguirMayusculasNiTildes() = runTest {
        musicos = MockResponse().setBody(
            """[{"id": 1, "name": "rubén Blades", "image": null}, {"id": 2, "name": "Ástor Piazzolla", "image": null}]"""
        )
        bandas = MockResponse().setBody(
            """[{"id": 3, "name": "Queen", "image": null}, {"id": 4, "name": "aerosmith", "image": null}]"""
        )

        val nombres = repositorio.obtenerArtistas().getOrThrow().map { it.name }

        assertEquals(listOf("aerosmith", "Ástor Piazzolla", "Queen", "rubén Blades"), nombres)
    }

    @Test
    fun descartaElementosSinIdOSinNombre() = runTest {
        musicos = MockResponse().setBody(
            """[{"name": "Sin id"}, {"id": 2, "image": "x"}, {"id": 3, "name": "Válido"}]"""
        )

        assertEquals(
            listOf(Artist(3, "Válido", null, ArtistType.MUSICO)),
            repositorio.obtenerArtistas().getOrThrow()
        )
    }

    @Test
    fun errorDelServidorEnUnaDeLasDosDevuelveError() = runTest {
        musicos = MockResponse().setBody("""[{"id": 1, "name": "Ástor Piazzolla"}]""")
        bandas = MockResponse().setResponseCode(500)

        val resultado = repositorio.obtenerArtistas()

        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun desconexionDevuelveError() = runTest {
        musicos = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)

        val resultado = repositorio.obtenerArtistas()

        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun tiempoAgotadoDevuelveError() = runTest {
        bandas = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)

        val resultado = repositorio.obtenerArtistas()

        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun jsonInvalidoDevuelveError() = runTest {
        musicos = MockResponse().setBody("""{"no": "es una lista"}""")

        val resultado = repositorio.obtenerArtistas()

        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }
}
