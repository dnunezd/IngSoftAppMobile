package com.example.vinilos.data.repository

import com.example.vinilos.data.model.ComentarioNuevo
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Publicar un comentario (HU09, add-comentar-album D1) contra un backend simulado. */
class ComentarioRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: ComentarioRepository

    private val comentario = ComentarioNuevo(coleccionistaId = 101, calificacion = 5, descripcion = "Gran prensado")

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        repositorio = ComentarioRepositoryImpl(ServiceAdapter(servidor.url("/").toString()))
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    private fun guardado() = MockResponse().setBody(
        """{"id": 7, "description": "Gran prensado", "rating": 5, "collector": {"id": 101}}"""
    )

    @Test
    fun publicaEnLaRutaDelAlbumConLaOpinionLaCalificacionYElColeccionista() = runTest {
        servidor.enqueue(guardado())

        val resultado = repositorio.publicarComentario(albumId = 100, comentario = comentario)

        assertTrue("Debe publicarse", resultado.isSuccess)
        val peticion = servidor.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Debe enviar el comentario", peticion)
        assertEquals("POST", peticion!!.method)
        assertEquals("/albums/100/comments", peticion.path)
        val cuerpo = JsonParser.parseString(peticion.body.readUtf8()).asJsonObject
        assertEquals("Gran prensado", cuerpo["description"].asString)
        assertEquals(5, cuerpo["rating"].asInt)
        assertEquals(101, cuerpo["collector"].asJsonObject["id"].asInt)
        assertEquals("Solo los campos que valida el backend", setOf("description", "rating", "collector"), cuerpo.keySet())
        assertEquals(setOf("id"), cuerpo["collector"].asJsonObject.keySet())
    }

    @Test
    fun unAlbumOColeccionistaInexistenteEsErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(404))

        val resultado = repositorio.publicarComentario(albumId = 999, comentario = comentario)

        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun datosInvalidosEsErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(400))

        val resultado = repositorio.publicarComentario(albumId = 100, comentario = comentario)

        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun unErrorDelServidorEsErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))

        val resultado = repositorio.publicarComentario(albumId = 100, comentario = comentario)

        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun sinConexionEsErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val resultado = repositorio.publicarComentario(albumId = 100, comentario = comentario)

        assertTrue(resultado.exceptionOrNull() is ErrorDeCarga)
    }
}
