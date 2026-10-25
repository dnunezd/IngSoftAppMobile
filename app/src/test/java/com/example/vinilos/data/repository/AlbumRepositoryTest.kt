package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.remote.ServiceAdapter
import com.google.gson.JsonParseException
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
import retrofit2.HttpException
import java.io.IOException
import java.lang.reflect.Modifier
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class AlbumRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: AlbumRepository

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        repositorio = AlbumRepository(ServiceAdapter(servidor.url("/").toString()))
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun albumCompletoConsultaElIdYMapeaSusRelacionesSinAutor() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": 101, "name": "Buscando America",
                  "cover": "https://example.com/portada.jpg",
                  "description": "Un album de salsa", "genre": "Salsa",
                  "recordLabel": "Elektra", "releaseDate": "1984-08-01",
                  "tracks": [{"id": 1, "name": "Decisiones", "duration": "5:00"}],
                  "performers": [{"id": 2, "name": "Ruben Blades", "image": "https://example.com/artista.jpg"}],
                  "comments": [{"id": 3, "description": "Excelente", "rating": 5,
                                "collector": {"id": 100, "name": "Autor privado"}}]
                }
                """.trimIndent()
            )
        )
        val esperado = Album(
            id = 101,
            nombre = "Buscando America",
            portada = "https://example.com/portada.jpg",
            descripcion = "Un album de salsa",
            genero = "Salsa",
            disquera = "Elektra",
            fechaLanzamiento = "1984-08-01",
            canciones = listOf(Cancion("Decisiones", "5:00")),
            artistas = listOf(Artista("Ruben Blades", "https://example.com/artista.jpg")),
            comentarios = listOf(Comentario("Excelente", 5))
        )

        val resultado = repositorio.obtenerAlbum(101)

        assertEquals(esperado, resultado.getOrNull())
        val peticion = servidor.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Debe consultar el álbum al backend", peticion)
        assertEquals("GET", peticion!!.method)
        assertEquals("/albums/101", peticion.path)
        assertEquals(
            setOf("descripcion", "calificacion"),
            Comentario::class.java.declaredFields
                .filterNot { it.isSynthetic || Modifier.isStatic(it.modifiers) }
                .map { it.name }.toSet()
        )
    }

    @Test
    fun respuesta404SeTraduceAAlbumNoDisponible() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(404))

        val error = repositorio.obtenerAlbum(999).exceptionOrNull()

        assertTrue("El 404 debe indicar álbum no disponible, no error de red", error is AlbumNoDisponible)
        assertTrue(error!!.cause is HttpException)
        assertEquals(404, (error.cause as HttpException).code())
        assertEquals("/albums/999", servidor.takeRequest(1, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun respuesta500SeTraduceAErrorDeCargaConSuCausa() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))

        val error = repositorio.obtenerAlbum(101).exceptionOrNull()

        assertTrue("El fallo del servidor debe ser recuperable", error is ErrorDeCarga)
        assertTrue(error!!.cause is HttpException)
        assertEquals(500, (error.cause as HttpException).code())
    }

    @Test
    fun desconexionSeTraduceAErrorDeCargaConSuCausa() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))

        val error = repositorio.obtenerAlbum(101).exceptionOrNull()

        assertTrue("La desconexión debe ser un error de carga, no un 404", error is ErrorDeCarga)
        assertTrue(error!!.cause is IOException)
        assertTrue(error.cause !is HttpException)
    }

    @Test
    fun timeoutSeTraduceAErrorDeCargaConSuCausa() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val error = repositorio.obtenerAlbum(101).exceptionOrNull()

        assertTrue("El timeout debe permitir reintentar, no indicar un 404", error is ErrorDeCarga)
        assertTrue(error!!.cause is SocketTimeoutException)
    }

    @Test
    fun jsonIlegibleSeTraduceAErrorDeCargaConSuCausa() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"id":[]}"""))

        val error = repositorio.obtenerAlbum(101).exceptionOrNull()

        assertTrue("El cuerpo ilegible debe permitir recuperar la carga", error is ErrorDeCarga)
        assertTrue(error!!.cause is JsonParseException)
    }

    @Test
    fun respuestaSinCamposObligatoriosSeTraduceAErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"id":101}"""))

        val error = repositorio.obtenerAlbum(101).exceptionOrNull()

        assertTrue("Un álbum incompleto debe ser un fallo de carga, no cerrar la app", error is ErrorDeCarga)
        assertTrue(error!!.cause is JsonParseException)
    }
}
