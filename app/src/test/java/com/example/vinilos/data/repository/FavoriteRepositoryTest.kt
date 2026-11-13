package com.example.vinilos.data.repository

import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.PerformerDto
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class FavoriteRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: FavoriteRepository

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        repositorio = FavoriteRepositoryImpl(ServiceAdapter(servidor.url("/").toString()))
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun esFavoritoEncuentraElArtistaPorIdEnElListadoDelColeccionista() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """[{"id": 5, "name": "Queen"}, {"id": 8, "name": "Ruben Blades"}]"""
            )
        )

        val resultado = repositorio.esFavorito(collectorId = 100, artistId = 8)

        assertEquals(true, resultado.getOrNull())
        val peticion = servidor.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Debe consultar los favoritos del coleccionista", peticion)
        assertEquals("GET", peticion!!.method)
        assertEquals("/collectors/100/performers", peticion.path)
    }

    @Test
    fun esFavoritoDevuelveFalsoCuandoElIdNoEstaEnElListado() = runTest {
        servidor.enqueue(MockResponse().setBody("""[{"id": 5, "name": "Queen"}]"""))

        val resultado = repositorio.esFavorito(collectorId = 100, artistId = 8)

        assertEquals(false, resultado.getOrNull())
    }

    @Test
    fun esFavoritoDevuelveErrorDeCargaSiLaConsultaFalla() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))

        val resultado = repositorio.esFavorito(collectorId = 100, artistId = 8)

        assertTrue("Un fallo del servidor debe ser ErrorDeCarga", resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun agregarFavoritoUsaElEndpointDeBandaCuandoElTipoEsBanda() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"id": 8}"""))

        val resultado = repositorio.agregarFavorito(collectorId = 100, artistId = 8, tipo = ArtistType.BANDA)

        assertTrue(resultado.isSuccess)
        val peticion = servidor.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Debe guardar el favorito en el backend", peticion)
        assertEquals("POST", peticion!!.method)
        assertEquals("/collectors/100/bands/8", peticion.path)
    }

    @Test
    fun agregarFavoritoUsaElEndpointDeMusicoCuandoElTipoEsMusico() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"id": 8}"""))

        repositorio.agregarFavorito(collectorId = 100, artistId = 8, tipo = ArtistType.MUSICO)

        val peticion = servidor.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Debe guardar el favorito en el backend", peticion)
        assertEquals("/collectors/100/musicians/8", peticion!!.path)
    }

    @Test
    fun agregarFavoritoTraduceUn404AArtistaNoDisponible() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(404))

        val resultado = repositorio.agregarFavorito(collectorId = 100, artistId = 8, tipo = ArtistType.BANDA)

        val error = resultado.exceptionOrNull()
        assertTrue("El 404 debe indicar artista no disponible, no error de red", error is ArtistaNoDisponible)
    }

    @Test
    fun agregarFavoritoDevuelveErrorDeCargaSiFallaLaConexion() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))

        val resultado = repositorio.agregarFavorito(collectorId = 100, artistId = 8, tipo = ArtistType.BANDA)

        assertTrue("Un fallo del servidor debe ser ErrorDeCarga", resultado.exceptionOrNull() is ErrorDeCarga)
    }

    @Test
    fun cancelarLaComprobacionPropagaLaCancelacionSinErrorDeCarga() = runTest {
        val api = object : CollectorApi {
            override suspend fun getCollectors() = throw NotImplementedError()
            override suspend fun getFavoritePerformers(collectorId: Int): List<PerformerDto> = awaitCancellation()
            override suspend fun addFavoriteBand(collectorId: Int, bandId: Int) = awaitCancellation()
            override suspend fun addFavoriteMusician(collectorId: Int, musicianId: Int) = awaitCancellation()
        }
        val repositorio = FavoriteRepositoryImpl(ServiceAdapter(servidor.url("/").toString()), api)
        var devolvioResultado = false
        val consulta = async(start = CoroutineStart.UNDISPATCHED) {
            val resultado = repositorio.esFavorito(100, 8)
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
}
