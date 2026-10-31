package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.remote.AlbumApi
import com.example.vinilos.data.remote.AlbumDto
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
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

    @Test
    fun fechaDeLanzamientoConFormatoInvalidoSeTraduceAErrorDeCarga() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """
                {"id":103,"name":"Fecha invalida","description":"Descripcion",
                 "genre":"Rock","recordLabel":"Sello","releaseDate":"31/12/2020"}
                """.trimIndent()
            )
        )

        val error = repositorio.obtenerAlbum(103).exceptionOrNull()

        assertTrue(
            "Una fecha con formato inesperado debe ser un fallo de carga, no cerrar la app",
            error is ErrorDeCarga
        )
        assertTrue(error!!.cause is JsonParseException)
    }

    @Test
    fun albumSinRelacionesDevuelveListasVacias() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """
                {"id":102,"name":"Sin relaciones","description":"Album sin contenido relacionado",
                 "genre":"Rock","recordLabel":"Independiente","releaseDate":"2000-01-01"}
                """.trimIndent()
            )
        )

        val album = repositorio.obtenerAlbum(102).getOrThrow()

        assertEquals(emptyList<Cancion>(), album.canciones)
        assertEquals(emptyList<Artista>(), album.artistas)
        assertEquals(emptyList<Comentario>(), album.comentarios)
        assertEquals(null, album.portada)
        assertEquals("/albums/102", servidor.takeRequest(1, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun errorInesperadoSePropagaSinDisfrazarloDeErrorDeCarga() = runTest {
        val esperado = IllegalStateException("Error de programación")
        val api = object : AlbumApi {
            override suspend fun obtenerAlbum(id: Int): Response<AlbumDto> = throw esperado
            override suspend fun obtenerAlbumes(): List<AlbumDto> = throw esperado
        }
        val repositorio = AlbumRepository(ServiceAdapter(servidor.url("/").toString()), api)

        try {
            repositorio.obtenerAlbum(101)
            fail("El error inesperado debe propagarse")
        } catch (error: IllegalStateException) {
            assertSame(esperado, error)
        }
    }

    @Test
    fun cancelarLaConsultaPropagaLaCancelacionSinErrorDeCarga() = runTest {
        val api = object : AlbumApi {
            override suspend fun obtenerAlbum(id: Int): Response<AlbumDto> = awaitCancellation()
            override suspend fun obtenerAlbumes(): List<AlbumDto> = awaitCancellation()
        }
        val repositorio = AlbumRepository(ServiceAdapter(servidor.url("/").toString()), api)
        var devolvioResultado = false
        val consulta = async(start = CoroutineStart.UNDISPATCHED) {
            val resultado = repositorio.obtenerAlbum(101)
            devolvioResultado = true
            resultado
        }
        val cancelacion = CancellationException("El usuario salió del detalle")

        consulta.cancel(cancelacion)

        try {
            consulta.await()
            fail("La consulta cancelada no debe devolver un Result de carga")
        } catch (error: CancellationException) {
            assertEquals(cancelacion.message, error.message)
        }
        assertTrue(consulta.isCancelled)
        assertFalse("La cancelación no debe convertirse en un Result", devolvioResultado)
    }

    @Test
    fun cuerpoNuloSeTraduceAErrorDeCargaSinConfundirloCon404() = runTest {
        servidor.enqueue(MockResponse().setBody("null"))

        val error = repositorio.obtenerAlbum(101).exceptionOrNull()

        assertTrue("Una respuesta sin álbum debe ser un error de carga", error is ErrorDeCarga)
        assertTrue(error!!.cause is JsonParseException)
    }

    // ---- Catálogo (HU01): GET /albums ----

    @Test
    fun catalogoConsultaGetAlbumsYConvierteCadaAlbum() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """
                [
                  {"id": 100, "name": "Buscando América", "cover": "https://example.com/100.jpg",
                   "description": "Salsa urbana", "genre": "Salsa", "recordLabel": "Elektra",
                   "releaseDate": "1984-08-01T00:00:00.000Z",
                   "tracks": [], "performers": [{"id": 1, "name": "Rubén Blades Bellido de Luna"}],
                   "comments": []},
                  {"id": 102, "name": "A Night at the Opera", "cover": "https://example.com/102.jpg",
                   "genre": "Rock", "releaseDate": "1975-11-21T00:00:00.000Z",
                   "performers": [{"id": 2, "name": "Queen"}, {"id": 3, "name": "Freddie Mercury"}]}
                ]
                """.trimIndent()
            )
        )

        val albumes = repositorio.obtenerAlbumes().getOrThrow()

        assertEquals("/albums", servidor.takeRequest(1, TimeUnit.SECONDS)?.path)
        assertEquals(
            listOf(
                AlbumResumen(
                    id = 102, nombre = "A Night at the Opera", portada = "https://example.com/102.jpg",
                    genero = "Rock", artista = "Queen, Freddie Mercury", anio = "1975"
                ),
                AlbumResumen(
                    id = 100, nombre = "Buscando América", portada = "https://example.com/100.jpg",
                    genero = "Salsa", artista = "Rubén Blades Bellido de Luna", anio = "1984"
                )
            ),
            albumes
        )
    }

    @Test
    fun catalogoDescartaLosAlbumesIncompletosSinAfectarALosDemas() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """
                [
                  {"name": "Sin id", "genre": "Rock"},
                  {"id": 2, "genre": "Rock"},
                  {"id": 3, "name": "Sin género"},
                  {"id": 4, "name": "Completo", "genre": "Folk"}
                ]
                """.trimIndent()
            )
        )

        val albumes = repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(listOf(4), albumes.map { it.id })
    }

    @Test
    fun elArtistaEsCadenaVaciaSiNoHayArtistasConNombre() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """
                [
                  {"id": 1, "name": "A sin performers", "genre": "Rock"},
                  {"id": 2, "name": "B con lista vacía", "genre": "Rock", "performers": []},
                  {"id": 3, "name": "C sin nombre", "genre": "Rock", "performers": [{"id": 9}]},
                  {"id": 4, "name": "D mixto", "genre": "Rock",
                   "performers": [{"id": 9, "name": ""}, {"id": 10, "name": "Queen"}, {"id": 11, "name": "  "}]}
                ]
                """.trimIndent()
            )
        )

        val albumes = repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(listOf("", "", "", "Queen"), albumes.map { it.artista })
    }

    @Test
    fun elAnioEsNuloSinFechaOConFechaInvalida() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """
                [
                  {"id": 1, "name": "A sin fecha", "genre": "Rock"},
                  {"id": 2, "name": "B fecha inválida", "genre": "Rock", "releaseDate": "pronto"},
                  {"id": 3, "name": "C solo día", "genre": "Rock", "releaseDate": "1976-12-10"}
                ]
                """.trimIndent()
            )
        )

        val albumes = repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(listOf(null, null, "1976"), albumes.map { it.anio })
    }

    @Test
    fun catalogoSinAlbumesEsUnExitoConListaVacia() = runTest {
        servidor.enqueue(MockResponse().setBody("[]"))

        assertEquals(emptyList<AlbumResumen>(), repositorio.obtenerAlbumes().getOrThrow())
    }

    @Test
    fun catalogoOrdenaLosAlbumesPorNombre() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """
                [
                  {"id": 100, "name": "Buscando América", "genre": "Salsa"},
                  {"id": 101, "name": "Poeta del pueblo", "genre": "Salsa"},
                  {"id": 102, "name": "A Night at the Opera", "genre": "Rock"},
                  {"id": 103, "name": "A Day at the Races", "genre": "Rock"}
                ]
                """.trimIndent()
            )
        )

        val nombres = repositorio.obtenerAlbumes().getOrThrow().map { it.nombre }

        assertEquals(
            listOf("A Day at the Races", "A Night at the Opera", "Buscando América", "Poeta del pueblo"),
            nombres
        )
    }

    @Test
    fun catalogoConError500EsErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))

        val error = repositorio.obtenerAlbumes().exceptionOrNull()

        assertTrue(error is ErrorDeCarga)
        assertEquals(500, (error!!.cause as HttpException).code())
    }

    @Test
    fun catalogoConDesconexionEsErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))

        val error = repositorio.obtenerAlbumes().exceptionOrNull()

        assertTrue(error is ErrorDeCarga)
        assertTrue(error!!.cause is IOException)
    }

    @Test
    fun catalogoConJsonIlegibleEsErrorDeCarga() = runTest {
        servidor.enqueue(MockResponse().setBody("{no es una lista"))

        assertTrue(repositorio.obtenerAlbumes().exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun cancelarElCatalogoPropagaLaCancelacion() = runTest {
        val api = object : AlbumApi {
            override suspend fun obtenerAlbum(id: Int): Response<AlbumDto> = awaitCancellation()
            override suspend fun obtenerAlbumes(): List<AlbumDto> = awaitCancellation()
        }
        val repositorio = AlbumRepository(ServiceAdapter(servidor.url("/").toString()), api)
        val consulta = async(start = CoroutineStart.UNDISPATCHED) { repositorio.obtenerAlbumes() }

        consulta.cancel(CancellationException("El usuario salió del catálogo"))

        try {
            consulta.await()
            fail("La consulta cancelada no debe devolver un Result")
        } catch (error: CancellationException) {
            assertTrue(consulta.isCancelled)
        }
    }
}
