package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Track
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.remote.TrackApi
import com.example.vinilos.data.remote.CrearTrackDto
import com.google.gson.JsonParser
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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class AlbumRepositoryTracksTest {
    private lateinit var server: MockWebServer
    private lateinit var adapter: ServiceAdapter
    private lateinit var repository: AlbumRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        adapter = ServiceAdapter(server.url("/").toString())
        repository = AlbumRepositoryImpl(adapter)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun agregaTrackAlAlbumConPostYSoloLosCamposPermitidos() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id": 10, "name": "Decisiones", "duration": "5:05"}"""
            )
        )

        val result = repository.addTrack(101, Track("Decisiones", "5:05"))

        assertTrue("La respuesta 200 debe ser exitosa: ${result.exceptionOrNull()}", result.isSuccess)
        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Debe enviarse una petición al backend", request)
        assertEquals("POST", request!!.method)
        assertEquals("/albums/101/tracks", request.path)
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals(setOf("name", "duration"), body.keySet())
        assertEquals("Decisiones", body.get("name").asString)
        assertEquals("5:05", body.get("duration").asString)
    }

    @Test
    fun respuesta404AlAddTrackSeTraduceAAlbumNotAvailable() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val error = repository.addTrack(999, Track("Tema", "3:15")).exceptionOrNull()

        assertTrue("El 404 debe identificar al álbum inexistente", error is AlbumNotAvailable)
        assertTrue(error!!.cause is HttpException)
        assertEquals(404, (error.cause as HttpException).code())
        assertEquals("/albums/999/tracks", server.takeRequest(1, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun respuesta500AlAddTrackPermiteReintentar() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val error = repository.addTrack(101, Track("Tema", "3:15")).exceptionOrNull()

        assertTrue("El error del servidor debe ser recuperable", error is LoadError)
        assertTrue(error!!.cause is HttpException)
        assertEquals(500, (error.cause as HttpException).code())
    }

    @Test
    fun desconexionAlAddTrackSeTraduceALoadError() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))

        val error = repository.addTrack(101, Track("Tema", "3:15")).exceptionOrNull()

        assertTrue(error is LoadError)
        assertTrue(error!!.cause is IOException)
    }

    @Test
    fun cancelarLaAsociacionPropagaLaCancelacion() = runTest {
        val trackApi = object : TrackApi {
            override suspend fun addTrack(
                albumId: Int,
                cancion: CrearTrackDto
            ): Response<Unit> = awaitCancellation()
        }
        val repository = AlbumRepositoryImpl(adapter, trackApi = trackApi)
        var devolvioResultado = false
        val request = async(start = CoroutineStart.UNDISPATCHED) {
            val result = repository.addTrack(101, Track("Tema", "3:15"))
            devolvioResultado = true
            result
        }
        val cancellation = CancellationException("Se canceló el formulario")

        request.cancel(cancellation)

        try {
            request.await()
            fail("La cancelación no debe convertirse en un resultado de error")
        } catch (error: CancellationException) {
            assertEquals(cancellation.message, error.message)
        }
        assertTrue(request.isCancelled)
        assertFalse(devolvioResultado)
    }
}
