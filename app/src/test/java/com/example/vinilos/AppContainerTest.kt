package com.example.vinilos

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumNoDisponible
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

private class AlbumRepositoryFalso : AlbumRepository(ServiceAdapter("http://localhost/")) {
    override suspend fun obtenerAlbum(id: Int): Result<Album> = Result.failure(AlbumNoDisponible())
}

class AppContainerTest {

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
    fun albumYColeccionistasUsanLaConfiguracionDelContenedor() = runTest {
        servidor.enqueue(MockResponse().setBody("[]"))
        servidor.enqueue(MockResponse().setResponseCode(404))
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString())

        assertEquals(
            emptyList<Collector>(),
            contenedor.collectorRepository.obtenerColeccionistas().getOrThrow()
        )
        val repositorio = contenedor.albumRepository
        assertSame(repositorio, contenedor.albumRepository)
        assertTrue(repositorio.obtenerAlbum(123).exceptionOrNull() is AlbumNoDisponible)
        assertEquals("/collectors", servidor.takeRequest(1, TimeUnit.SECONDS)?.path)
        assertEquals("/albums/123", servidor.takeRequest(1, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun albumRepositorioPuedeReemplazarseComoLosDemas() = runTest {
        val falso = AlbumRepositoryFalso()
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), albumesRepositorio = falso)

        assertSame(falso, contenedor.albumRepository)
        assertTrue(contenedor.albumRepository.obtenerAlbum(1).exceptionOrNull() is AlbumNoDisponible)
    }
}
