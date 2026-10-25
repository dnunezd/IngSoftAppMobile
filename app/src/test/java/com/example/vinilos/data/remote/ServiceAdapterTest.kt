package com.example.vinilos.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET

// Respuesta pequeña que Retrofit/Gson convertirá desde el JSON.
data class DatosPrueba(val nombre: String)

// API falsa para probar que el ServiceAdapter puede ejecutar una llamada Retrofit.
interface ApiDePrueba {
    @GET("prueba")
    suspend fun obtenerPrueba(): DatosPrueba
}

class ServiceAdapterTest {

    private lateinit var servidor: MockWebServer

    @Before
    fun preparar() {
        // Inicia un servidor local falso para no depender de Internet ni de BackVynils.
        servidor = MockWebServer()
        servidor.start()
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
}