package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Mapeo de DTO a modelo; la traducción de errores de red se prueba en `ServiceAdapterTest`. */
class CollectorRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: CollectorRepository

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        val cliente = OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .build()
        repositorio = CollectorRepository(ServiceAdapter(servidor.url("/").toString(), cliente))
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun respuestaConDatosDevuelveLosColeccionistas() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """
                [
                  {"id": 100, "name": "Manolo Bellon", "telephone": "1", "email": "manollo@caracol.com.co",
                   "comments": [], "favoritePerformers": [], "collectorAlbums": []},
                  {"id": 101, "name": "Jaime Monsalve", "email": "jmonsalve@rtvc.com.co"}
                ]
                """.trimIndent()
            )
        )

        val resultado = repositorio.obtenerColeccionistas()

        assertEquals(
            listOf(
                Collector(100, "Manolo Bellon", "manollo@caracol.com.co"),
                Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co")
            ),
            resultado.getOrThrow()
        )
        assertEquals("/collectors", servidor.takeRequest().path)
    }

    @Test
    fun respuestaVaciaDevuelveListaVacia() = runTest {
        servidor.enqueue(MockResponse().setBody("[]"))

        assertEquals(emptyList<Collector>(), repositorio.obtenerColeccionistas().getOrThrow())
    }

    @Test
    fun ignoraLosCamposDesconocidos() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """[{"id": 7, "name": "Con extras", "email": "x@a.com", "campoNuevo": {"a": 1}, "otro": [1, 2]}]"""
            )
        )

        assertEquals(
            listOf(Collector(7, "Con extras", "x@a.com")),
            repositorio.obtenerColeccionistas().getOrThrow()
        )
    }

    @Test
    fun descartaLosElementosIncompletos() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """
                [
                  {"name": "Sin id", "email": "a@a.com"},
                  {"id": 2, "email": "sin-nombre@a.com"},
                  {"id": 3, "name": "Sin correo"},
                  {"id": 4, "name": "Completo", "email": "ok@a.com"}
                ]
                """.trimIndent()
            )
        )

        assertEquals(
            listOf(Collector(4, "Completo", "ok@a.com")),
            repositorio.obtenerColeccionistas().getOrThrow()
        )
    }

    @Test
    fun elErrorDelAdaptadorSePropagaComoErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))

        assertTrue(repositorio.obtenerColeccionistas().exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun sinConexionSeTraduceAErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertTrue(repositorio.obtenerColeccionistas().exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun tiempoDeEsperaAgotadoSeTraduceAErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        assertTrue(repositorio.obtenerColeccionistas().exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun jsonMalformadoSeTraduceAErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setBody("esto no es json"))

        assertTrue(repositorio.obtenerColeccionistas().exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun laCancelacionSeRelanzaYNoSeConvierteEnError() = runTest {
        val api = object : CollectorApi {
            override suspend fun getCollectors(): List<CollectorDto> = throw CancellationException("cancelado")
        }

        var relanzada = false
        try {
            CollectorRepository(ServiceAdapter(servidor.url("/").toString()), api)
                .obtenerColeccionistas()
        } catch (e: CancellationException) {
            relanzada = true
        }

        assertTrue(relanzada)
    }
}
