package com.example.vinilos.data.remote

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

class ServiceAdapterTest {

    private lateinit var servidor: MockWebServer
    private lateinit var adaptador: ServiceAdapter
    private lateinit var api: CollectorApi

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        val cliente = OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .build()
        adaptador = ServiceAdapter(servidor.url("/").toString(), cliente)
        api = adaptador.api()
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun respuestaExitosaSeEntregaTalCual() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """[{"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"}]"""
            )
        )

        val resultado = adaptador.llamar { api.getCollectors() }

        val lista = resultado.getOrThrow()
        assertEquals(1, lista.size)
        assertEquals(100, lista[0].id)
        assertEquals("Manolo Bellon", lista[0].name)
        assertEquals("/collectors", servidor.takeRequest().path)
    }

    @Test
    fun respuestaVaciaEsUnExitoConListaVacia() = runTest {
        servidor.enqueue(MockResponse().setBody("[]"))

        assertEquals(emptyList<CollectorDto>(), adaptador.llamar { api.getCollectors() }.getOrThrow())
    }

    @Test
    fun errorDelServidorSeTraduceAErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))

        assertTrue(adaptador.llamar { api.getCollectors() }.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun errorDeCliente404SeTraduceAErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(404))

        assertTrue(adaptador.llamar { api.getCollectors() }.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun sinConexionSeTraduceAErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertTrue(adaptador.llamar { api.getCollectors() }.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun tiempoDeEsperaAgotadoSeTraduceAErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        assertTrue(adaptador.llamar { api.getCollectors() }.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun jsonMalformadoSeTraduceAErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setBody("esto no es json"))

        assertTrue(adaptador.llamar { api.getCollectors() }.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun jsonConEstructuraInesperadaSeTraduceAErrorDeCarga() = runTest {
        // Gson lanza IllegalStateException si llega un objeto donde se espera una lista.
        servidor.enqueue(MockResponse().setBody("""{"mensaje": "no es una lista"}"""))

        assertTrue(adaptador.llamar { api.getCollectors() }.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun laCancelacionSeRelanzaYNoSeConvierteEnError() = runTest {
        var relanzada = false
        try {
            adaptador.llamar<List<CollectorDto>> { throw CancellationException("cancelado") }
        } catch (e: CancellationException) {
            relanzada = true
        }

        assertTrue(relanzada)
    }

    @Test
    fun otrosFallosNoSeConfundenConErrorDeCarga() = runTest {
        var lanzada: Throwable? = null
        try {
            adaptador.llamar<List<CollectorDto>> { error("error de programación") }
        } catch (e: IllegalStateException) {
            lanzada = e
        }

        assertEquals("error de programación", lanzada?.message)
    }

    @Test
    fun repetirLaConsultaTrasUnFalloDevuelveLosDatos() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))
        servidor.enqueue(MockResponse().setBody("""[{"id": 1, "name": "A", "email": "a@a.com"}]"""))

        val primero = adaptador.llamar { api.getCollectors() }
        val segundo = adaptador.llamar { api.getCollectors() }

        assertTrue(primero.exceptionOrNull() is ErrorDeCarga)
        assertEquals(1, segundo.getOrThrow().size)
    }
}
