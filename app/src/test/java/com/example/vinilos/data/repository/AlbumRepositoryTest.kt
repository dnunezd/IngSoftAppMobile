package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
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
            Comentario::class.java.declaredFields.filterNot { it.isSynthetic }.map { it.name }.toSet()
        )
    }
}
