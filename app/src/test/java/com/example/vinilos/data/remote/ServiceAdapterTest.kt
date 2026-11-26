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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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
    fun llamadaSuccesssaDevuelveLosDatos() = runTest {
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
        val resultado = adaptador.call { api.obtenerPrueba() }

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

        val resultado = adaptador.call { api.obtenerPrueba() }

        assertTrue(resultado.isFailure)
        val error = resultado.exceptionOrNull()
        assertTrue(error is LoadError)
        assertTrue(error!!.cause is HttpException)
        assertEquals(404, (error.cause as HttpException).code())
    }

    @Test
    fun respuesta404SeReportaComoResourceNotFound() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(404))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        val error = adaptador.call { api.obtenerPrueba() }.exceptionOrNull()

        // Los repositorios lo distinguen sin conocer HTTP; para los demás sigue siendo un error de carga.
        assertTrue(error is ResourceNotFound)
        assertTrue(error is LoadError)
    }

    @Test
    fun otrosErroresHttpNoSonResourceNotFound() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))
        servidor.enqueue(MockResponse().setResponseCode(400))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        repeat(2) {
            val error = adaptador.call { api.obtenerPrueba() }.exceptionOrNull()
            assertTrue(error is LoadError)
            assertTrue(error !is ResourceNotFound)
        }
    }

    @Test
    fun respuesta500ConservaElCodigoHttp() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.call { api.obtenerPrueba() }

        assertTrue(resultado.isFailure)
        val error = resultado.exceptionOrNull()
        assertTrue(error is LoadError)
        assertTrue(error!!.cause is HttpException)
        assertEquals(500, (error.cause as HttpException).code())
    }

    @Test
    fun desconexionDevuelveElErrorDeTransporte() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.call { api.obtenerPrueba() }

        assertTrue(resultado.isFailure)
        val error = resultado.exceptionOrNull()
        assertTrue(error is LoadError)
        assertTrue(error!!.cause is IOException)
        assertTrue(error.cause !is HttpException)
    }

    @Test
    fun timeoutDevuelveElErrorDeEspera() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.call { api.obtenerPrueba() }

        assertTrue(resultado.isFailure)
        val error = resultado.exceptionOrNull()
        assertTrue(error is LoadError)
        assertTrue(error!!.cause is SocketTimeoutException)
    }

    @Test
    fun jsonIlegibleDevuelveElErrorDeLectura() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"nombre":[]}"""))
        val adaptador = ServiceAdapter(servidor.url("/").toString())
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.call { api.obtenerPrueba() }

        assertTrue(resultado.isFailure)
        val error = resultado.exceptionOrNull()
        assertTrue(error is LoadError)
        assertTrue(error!!.cause is JsonParseException)
    }

    @Test
    fun cuerpoNullDevuelveElErrorDeLectura() = runTest {
        // 200 con cuerpo "null" en una función que declara un tipo no nulo: no debe cerrar la app.
        servidor.enqueue(MockResponse().setBody("null"))
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.call { api.obtenerPrueba() }

        assertTrue(resultado.exceptionOrNull() is LoadError)
    }

    @Test
    fun respuestaSinContenidoDevuelveElErrorDeLectura() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(204))
        val api = adaptador.api<ApiDePrueba>()

        val resultado = adaptador.call { api.obtenerPrueba() }

        assertTrue(resultado.exceptionOrNull() is LoadError)
    }

    @Test
    fun unNullPointerExceptionDelCodigoNoSeOculta() = runTest {
        // Solo el cuerpo vacío de Retrofit es un fallo de carga; un NPE propio es un error de programación.
        try {
            adaptador.call<DatosPrueba> { throw NullPointerException("Error de programación") }
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
            adaptador.call<DatosPrueba> { throw cancelacion }
            fail("La cancelación debe propagarse")
        } catch (error: CancellationException) {
            assertSame(cancelacion, error)
        }
    }

    @Test
    fun respuestaSuccesssaSeEntregaTalCual() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """[{"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"}]"""
            )
        )

        val resultado = adaptador.call { api.getCollectors().conVersion().valor }

        val lista = resultado.getOrThrow()
        assertEquals(1, lista.size)
        assertEquals(100, lista[0].id)
        assertEquals("Manolo Bellon", lista[0].name)
        assertEquals("/collectors", servidor.takeRequest().path)
    }

    @Test
    fun respuestaVaciaEsUnSuccessConListaVacia() = runTest {
        servidor.enqueue(MockResponse().setBody("[]"))

        assertEquals(emptyList<CollectorDto>(), adaptador.call { api.getCollectors().conVersion().valor }.getOrThrow())
    }

    @Test
    fun errorDelServidorSeTraduceALoadError() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))

        assertTrue(adaptador.call { api.getCollectors().conVersion().valor }.exceptionOrNull() is LoadError)
    }

    @Test
    fun errorDeCliente404SeTraduceALoadError() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(404))

        assertTrue(adaptador.call { api.getCollectors().conVersion().valor }.exceptionOrNull() is LoadError)
    }

    @Test
    fun sinConexionSeTraduceALoadError() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertTrue(adaptador.call { api.getCollectors().conVersion().valor }.exceptionOrNull() is LoadError)
    }

    @Test
    fun tiempoDeEsperaAgotadoSeTraduceALoadError() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        assertTrue(adaptador.call { api.getCollectors().conVersion().valor }.exceptionOrNull() is LoadError)
    }

    @Test
    fun jsonMalformadoSeTraduceALoadError() = runTest {
        servidor.enqueue(MockResponse().setBody("esto no es json"))

        assertTrue(adaptador.call { api.getCollectors().conVersion().valor }.exceptionOrNull() is LoadError)
    }

    @Test
    fun jsonConEstructuraInesperadaSeTraduceALoadError() = runTest {
        // Gson lanza IllegalStateException si llega un objeto donde se espera una lista.
        servidor.enqueue(MockResponse().setBody("""{"mensaje": "no es una lista"}"""))

        assertTrue(adaptador.call { api.getCollectors().conVersion().valor }.exceptionOrNull() is LoadError)
    }

    @Test
    fun laCancelacionSeRelanzaYNoSeConvierteEnError() = runTest {
        var relanzada = false
        try {
            adaptador.call<List<CollectorDto>> { throw CancellationException("cancelado") }
        } catch (e: CancellationException) {
            relanzada = true
        }

        assertTrue(relanzada)
    }

    @Test
    fun otrosFallosNoSeConfundenConLoadError() = runTest {
        var lanzada: Throwable? = null
        try {
            adaptador.call<List<CollectorDto>> { error("error de programación") }
        } catch (e: IllegalStateException) {
            lanzada = e
        }

        assertEquals("error de programación", lanzada?.message)
    }

    @Test
    fun repetirLaConsultaTrasUnFalloDevuelveLosDatos() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))
        servidor.enqueue(MockResponse().setBody("""[{"id": 1, "name": "A", "email": "a@a.com"}]"""))

        val primero = adaptador.call { api.getCollectors().conVersion().valor }
        val segundo = adaptador.call { api.getCollectors().conVersion().valor }

        assertTrue(primero.exceptionOrNull() is LoadError)
        assertEquals(1, segundo.getOrThrow().size)
    }

    // ---------- Caché HTTP (optimizar-cache-e-imagenes, D2) ----------

    @get:Rule
    val carpeta = TemporaryFolder()

    /** Ejecuta [bloque] con un adaptador que usa la caché HTTP en una carpeta temporal y la cierra al final. */
    private suspend fun conCache(bloque: suspend (ServiceAdapter, ApiDePrueba) -> Unit) {
        val cliente = ServiceAdapter.clientePorDefecto(carpeta.newFolder("http"))
        try {
            val adaptador = ServiceAdapter(servidor.url("/").toString(), cliente)
            bloque(adaptador, adaptador.api())
        } finally {
            cliente.cache?.close()
        }
    }

    private fun respuestaConVersion(version: String, nombre: String = "Vinilo") =
        MockResponse().setHeader("ETag", version).setBody("""{"nombre":"$nombre"}""")

    @Test
    fun conCacheLaConsultaRepetidaEnviaLaVersionYUsaLoGuardadoAnteUn304() = runTest {
        servidor.enqueue(respuestaConVersion("W/\"abc\""))
        servidor.enqueue(MockResponse().setResponseCode(304).setHeader("ETag", "W/\"abc\""))

        conCache { adaptador, api ->
            assertEquals("Vinilo", adaptador.call { api.obtenerPrueba() }.getOrThrow().nombre)
            val repetida = adaptador.call { api.obtenerPrueba() }

            assertEquals("Vinilo", repetida.getOrThrow().nombre)
        }
        assertEquals(null, servidor.takeRequest().getHeader("If-None-Match"))
        assertEquals("W/\"abc\"", servidor.takeRequest().getHeader("If-None-Match"))
    }

    @Test
    fun conCacheUnaRespuestaNuevaReemplazaLaGuardadaYSuVersion() = runTest {
        servidor.enqueue(respuestaConVersion("W/\"v1\"", "Antes"))
        servidor.enqueue(respuestaConVersion("W/\"v2\"", "Despues"))
        servidor.enqueue(MockResponse().setResponseCode(304).setHeader("ETag", "W/\"v2\""))

        conCache { adaptador, api ->
            adaptador.call { api.obtenerPrueba() }
            assertEquals("Despues", adaptador.call { api.obtenerPrueba() }.getOrThrow().nombre)
            assertEquals("Despues", adaptador.call { api.obtenerPrueba() }.getOrThrow().nombre)
        }
        servidor.takeRequest()
        servidor.takeRequest()
        assertEquals("W/\"v2\"", servidor.takeRequest().getHeader("If-None-Match"))
    }

    @Test
    fun conCacheUnMaxAgeDelBackendNoEvitaPreguntarle() = runTest {
        servidor.enqueue(respuestaConVersion("W/\"abc\"").setHeader("Cache-Control", "max-age=600"))
        servidor.enqueue(MockResponse().setResponseCode(304).setHeader("ETag", "W/\"abc\""))

        conCache { adaptador, api ->
            adaptador.call { api.obtenerPrueba() }
            assertEquals("Vinilo", adaptador.call { api.obtenerPrueba() }.getOrThrow().nombre)
        }
        assertEquals(2, servidor.requestCount)
    }

    @Test
    fun conCacheSinConexionSeReportaElLoadErrorAunqueHayaRespuestaGuardada() = runTest {
        servidor.enqueue(respuestaConVersion("W/\"abc\""))
        // OkHttp reintenta una conexión fallida: se encolan varias desconexiones.
        repeat(3) { servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }

        conCache { adaptador, api ->
            adaptador.call { api.obtenerPrueba() }

            assertTrue(adaptador.call { api.obtenerPrueba() }.exceptionOrNull() is LoadError)
        }
    }

    @Test
    fun sinCarpetaDeCacheNoSeEnviaLaVersion() = runTest {
        servidor.enqueue(respuestaConVersion("W/\"abc\""))
        servidor.enqueue(respuestaConVersion("W/\"abc\""))
        val adaptador = ServiceAdapter(servidor.url("/").toString(), ServiceAdapter.clientePorDefecto())
        val api = adaptador.api<ApiDePrueba>()

        adaptador.call { api.obtenerPrueba() }
        adaptador.call { api.obtenerPrueba() }

        servidor.takeRequest()
        assertEquals(null, servidor.takeRequest().getHeader("If-None-Match"))
    }

    // ---------- Versión de la respuesta (optimizar-cache-e-imagenes, D3) ----------

    @Test
    fun unaRespuestaSuccesssaEntregaSuVersion() = runTest {
        servidor.enqueue(respuestaConVersion("W/\"abc\""))
        val api = adaptador.api<ApiConVersion>()

        val resultado = adaptador.call { api.obtenerPrueba().conVersion() }.getOrThrow()

        assertEquals(ConVersion(DatosPrueba("Vinilo"), "W/\"abc\""), resultado)
    }

    @Test
    fun unaRespuestaSinEtagEntregaVersionNula() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"nombre":"Vinilo"}"""))
        val api = adaptador.api<ApiConVersion>()

        assertEquals(null, adaptador.call { api.obtenerPrueba().conVersion() }.getOrThrow().version)
    }

    @Test
    fun conVersionUn404SigueSiendoResourceNotFound() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(404))
        val api = adaptador.api<ApiConVersion>()

        val error = adaptador.call { api.obtenerPrueba().conVersion() }.exceptionOrNull()

        assertTrue(error is ResourceNotFound)
        assertEquals(404, (error?.cause as HttpException).code())
    }

    @Test
    fun conVersionUn500EsLoadErrorSinSerResourceNotFound() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))
        val api = adaptador.api<ApiConVersion>()

        val error = adaptador.call { api.obtenerPrueba().conVersion() }.exceptionOrNull()

        assertTrue(error is LoadError)
        assertTrue(error !is ResourceNotFound)
    }

    @Test
    fun conVersionUnCuerpoNuloEsLoadError() = runTest {
        servidor.enqueue(MockResponse().setBody("null"))
        val api = adaptador.api<ApiConVersion>()

        assertTrue(adaptador.call { api.obtenerPrueba().conVersion() }.exceptionOrNull() is LoadError)
    }

    @Test
    fun un304DeLaCacheEntregaLaVersionGuardada() = runTest {
        servidor.enqueue(respuestaConVersion("W/\"abc\""))
        servidor.enqueue(MockResponse().setResponseCode(304).setHeader("ETag", "W/\"abc\""))
        val cliente = ServiceAdapter.clientePorDefecto(carpeta.newFolder("http"))
        try {
            val adaptador = ServiceAdapter(servidor.url("/").toString(), cliente)
            val api = adaptador.api<ApiConVersion>()
            adaptador.call { api.obtenerPrueba().conVersion() }

            val repetida = adaptador.call { api.obtenerPrueba().conVersion() }.getOrThrow()

            assertEquals(ConVersion(DatosPrueba("Vinilo"), "W/\"abc\""), repetida)
        } finally {
            cliente.cache?.close()
        }
    }
}

// API falsa que devuelve la respuesta completa de Retrofit, como los GET que se guardan en Room.
interface ApiConVersion {
    @GET("prueba")
    suspend fun obtenerPrueba(): retrofit2.Response<DatosPrueba>
}
