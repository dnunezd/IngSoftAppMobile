package com.example.vinilos.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AgenteDeUsuarioInterceptorTest {

    private lateinit var servidor: MockWebServer

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun reemplazaElAgenteDeUsuarioPorDefectoDeOkHttp() {
        servidor.enqueue(MockResponse())
        val cliente = OkHttpClient.Builder()
            .addInterceptor(AgenteDeUsuarioInterceptor("Vinilos/1.0 (Android)"))
            .build()

        cliente.newCall(Request.Builder().url(servidor.url("/imagen.jpg")).build()).execute().close()

        assertEquals("Vinilos/1.0 (Android)", servidor.takeRequest().getHeader("User-Agent"))
    }
}
