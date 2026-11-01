package com.example.vinilos.data.remote

import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.http.GET
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

// Respuesta pequeña que Retrofit/Gson convertirá desde el JSON.
data class DatosPrueba(val nombre: String)

// API falsa para probar que el ServiceAdapter puede ejecutar una llamada Retrofit.
interface ApiDePrueba {
    @GET("prueba")
    suspend fun obtenerPrueba(): DatosPrueba
}
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
        // Cierra el servidor cuando acaba la prueba.
        servidor.shutdown()
    }

    @Test
    fun llamadaExitosaDevuelveLosDatos() = runTest {
        // Arrange: el servidor responderá 200 con este JSON.
        servidor.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"nombre":"Vinilo"}""")
        )

        // El adaptador debe apuntar al servidor falso de esta prueba.
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        // Act: la solicitud se ejecuta a través del adaptador.
        val resultado = adaptador.llamar { api.obtenerPrueba() }

        // Assert: debe ser exitosa y devolver el valor que envió el servidor.
        assertTrue(resultado.isSuccess)
        assertEquals("Vinilo", resultado.getOrThrow().nombre)

        // Además, comprobamos que Retrofit hizo la petición esperada.
        val peticion = servidor.takeRequest()
        assertEquals("GET", peticion.method)
        assertEquals("/prueba", peticion.path)
    }

    @Test
    fun respuesta404ConservaElCodigoHttp() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(404))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.llamar { api.obtenerPrueba() }

        assertTrue(resultado.isFailure)
        val error = resultado.exceptionOrNull()
        assertTrue(error is ErrorDeCarga)
        assertTrue(error!!.cause is HttpException)
        assertEquals(404, (error.cause as HttpException).code())
    }

    @Test
    fun respuesta500ConservaElCodigoHttp() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.llamar { api.obtenerPrueba() }

        assertTrue(resultado.isFailure)
        val error = resultado.exceptionOrNull()
        assertTrue(error is ErrorDeCarga)
        assertTrue(error!!.cause is HttpException)
        assertEquals(500, (error.cause as HttpException).code())
    }

    @Test
    fun desconexionDevuelveElErrorDeTransporte() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.llamar { api.obtenerPrueba() }

        assertTrue(resultado.isFailure)
        val error = resultado.exceptionOrNull()
        assertTrue(error is ErrorDeCarga)
        assertTrue(error!!.cause is IOException)
        assertTrue(error.cause !is HttpException)
    }

    @Test
    fun timeoutDevuelveElErrorDeEspera() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.llamar { api.obtenerPrueba() }

        assertTrue(resultado.isFailure)
        val error = resultado.exceptionOrNull()
        assertTrue(error is ErrorDeCarga)
        assertTrue(error!!.cause is SocketTimeoutException)
    }

    @Test
    fun jsonIlegibleDevuelveElErrorDeLectura() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"nombre":[]}"""))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.llamar { api.obtenerPrueba() }

        assertTrue(resultado.isFailure)
        val error = resultado.exceptionOrNull()
        assertTrue(error is ErrorDeCarga)
        assertTrue(error!!.cause is JsonParseException)
    }

    @Test
    fun cuerpoNullDevuelveElErrorDeLectura() = runTest {
        // 200 con cuerpo "null" en una función que declara un tipo no nulo: no debe cerrar la app.
        servidor.enqueue(MockResponse().setBody("null"))
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.llamar { api.obtenerPrueba() }

        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun respuestaSinContenidoDevuelveElErrorDeLectura() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(204))
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.llamar { api.obtenerPrueba() }

        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun unNullPointerExceptionDelCodigoNoSeOculta() = runTest {
        // Solo el cuerpo vacío de Retrofit es un fallo de carga; un NPE propio es un error de programación.
        try {
            adaptador.llamar<DatosPrueba> { throw NullPointerException("Error de programación") }
            fail("El NullPointerException debe propagarse")
        } catch (error: NullPointerException) {
            assertEquals("Error de programación", error.message)
        }
    }

    @Test
    fun cancelacionSeRelanzaSinConvertirseEnFallo() = runTest {
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val cancelacion = CancellationException("Consulta cancelada")

        try {
            adaptador.llamar<DatosPrueba> { throw cancelacion }
            fail("La cancelación debe propagarse")
        } catch (error: CancellationException) {
            assertSame(cancelacion, error)
        }
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
