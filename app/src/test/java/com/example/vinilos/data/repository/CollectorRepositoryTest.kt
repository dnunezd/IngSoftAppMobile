package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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
        val api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CollectorApi::class.java)
        repositorio = CollectorRepository(api)
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun respuestaConDatosDevuelveLosColeccionistas() = runTest {
        // Given
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

        // When
        val resultado = repositorio.obtenerColeccionistas()

        // Then
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
        // Given
        servidor.enqueue(MockResponse().setBody("[]"))

        // When
        val resultado = repositorio.obtenerColeccionistas()

        // Then
        assertEquals(emptyList<Collector>(), resultado.getOrThrow())
    }

    @Test
    fun descartaLosElementosIncompletos() = runTest {
        // Given
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

        // When
        val resultado = repositorio.obtenerColeccionistas()

        // Then
        assertEquals(listOf(Collector(4, "Completo", "ok@a.com")), resultado.getOrThrow())
    }

    @Test
    fun errorDelServidorSeTraduceAErrorDeCarga() = runTest {
        // Given
        servidor.enqueue(MockResponse().setResponseCode(500))

        // When
        val resultado = repositorio.obtenerColeccionistas()

        // Then
        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun errorDeCliente404SeTraduceAErrorDeCarga() = runTest {
        // Given
        servidor.enqueue(MockResponse().setResponseCode(404))

        // When
        val resultado = repositorio.obtenerColeccionistas()

        // Then
        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun sinConexionSeTraduceAErrorDeCarga() = runTest {
        // Given
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        // When
        val resultado = repositorio.obtenerColeccionistas()

        // Then
        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun tiempoDeEsperaAgotadoSeTraduceAErrorDeCarga() = runTest {
        // Given
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        // When
        val resultado = repositorio.obtenerColeccionistas()

        // Then
        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun jsonMalformadoSeTraduceAErrorDeCarga() = runTest {
        // Given
        servidor.enqueue(MockResponse().setBody("esto no es json"))

        // When
        val resultado = repositorio.obtenerColeccionistas()

        // Then
        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun laCancelacionSeRelanzaYNoSeConvierteEnError() = runTest {
        // Given
        val api = object : CollectorApi {
            override suspend fun getCollectors(): List<CollectorDto> = throw CancellationException("cancelado")
        }

        // When
        var relanzada = false
        try {
            CollectorRepository(api).obtenerColeccionistas()
        } catch (e: CancellationException) {
            relanzada = true
        }

        // Then
        assertTrue(relanzada)
    }
}
