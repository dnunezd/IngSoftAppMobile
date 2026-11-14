package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.FavoritoDao
import com.example.vinilos.data.local.FavoritoEntity
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Favoritos confirmados guardados en el repositorio (add-cache-consultas, D4), con un DAO falso en memoria. */
class FavoriteRepositoryCacheTest {

    /** Guarda en memoria; [fallaAlLeer] y [fallaAlEscribir] simulan un almacenamiento que no responde. */
    private class FavoritoDaoFalso : FavoritoDao() {
        var filas: Set<FavoritoEntity> = emptySet()
        var fallaAlLeer: Throwable? = null
        var fallaAlEscribir: Throwable? = null

        override suspend fun existe(coleccionistaId: Int, artistaId: Int): Boolean {
            fallaAlLeer?.let { throw it }
            return FavoritoEntity(coleccionistaId, artistaId) in filas
        }

        override suspend fun deColeccionista(coleccionistaId: Int): List<Int> {
            fallaAlLeer?.let { throw it }
            return filas.filter { it.coleccionistaId == coleccionistaId }.map { it.artistaId }
        }

        override suspend fun borrarDe(coleccionistaId: Int) {
            fallaAlEscribir?.let { throw it }
            filas = filas.filterNot { it.coleccionistaId == coleccionistaId }.toSet()
        }

        override suspend fun insertar(favoritos: List<FavoritoEntity>) {
            fallaAlEscribir?.let { throw it }
            filas = filas + favoritos
        }
    }

    private lateinit var servidor: MockWebServer
    private lateinit var dao: FavoritoDaoFalso
    private lateinit var repositorio: FavoriteRepository

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        dao = FavoritoDaoFalso()
        repositorio = FavoriteRepositoryImpl(ServiceAdapter(servidor.url("/").toString()), favoritoDao = dao)
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun comprobarReemplazaLosFavoritosDeEseColeccionistaSinTocarLosDeOtro() = runTest {
        dao.filas = setOf(FavoritoEntity(100, 1), FavoritoEntity(101, 1))
        servidor.enqueue(MockResponse().setBody("""[{"id": 5, "name": "Queen"}, {"id": 8}]"""))

        assertFalse(repositorio.esFavorito(collectorId = 100, artistId = 1).getOrThrow())

        assertEquals(setOf(FavoritoEntity(100, 5), FavoritoEntity(100, 8), FavoritoEntity(101, 1)), dao.filas)
    }

    @Test
    fun unaComprobacionFallidaNoTocaLoGuardado() = runTest {
        dao.filas = setOf(FavoritoEntity(100, 5))
        servidor.enqueue(MockResponse().setResponseCode(500))

        assertTrue(repositorio.esFavorito(100, 5).exceptionOrNull() is ErrorDeCarga)
        assertEquals(setOf(FavoritoEntity(100, 5)), dao.filas)
    }

    @Test
    fun agregarConExitoGuardaElFavorito() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"id": 8}"""))

        repositorio.agregarFavorito(100, 8, ArtistType.MUSICO).getOrThrow()

        assertTrue(repositorio.esFavoritoGuardado(100, 8))
    }

    @Test
    fun agregarSinExitoNoGuardaNada() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))
        servidor.enqueue(MockResponse().setResponseCode(404))

        assertTrue(repositorio.agregarFavorito(100, 8, ArtistType.MUSICO).exceptionOrNull() is ErrorDeCarga)
        assertTrue(repositorio.agregarFavorito(100, 8, ArtistType.MUSICO).exceptionOrNull() is ArtistaNoDisponible)

        assertTrue(dao.filas.isEmpty())
    }

    @Test
    fun esFavoritoGuardadoRespondePorColeccionistaYArtista() = runTest {
        dao.filas = setOf(FavoritoEntity(100, 5))

        assertTrue(repositorio.esFavoritoGuardado(100, 5))
        assertFalse(repositorio.esFavoritoGuardado(101, 5))
        assertFalse(repositorio.esFavoritoGuardado(100, 8))
    }

    @Test
    fun sinDaoNadaEsFavoritoGuardadoYSeConsultaComoAntes() = runTest {
        val sinCache = FavoriteRepositoryImpl(ServiceAdapter(servidor.url("/").toString()))
        servidor.enqueue(MockResponse().setBody("""[{"id": 5}]"""))

        assertFalse(sinCache.esFavoritoGuardado(100, 5))
        assertTrue(sinCache.esFavorito(100, 5).getOrThrow())
    }

    @Test
    fun siNoSePuedeLeerNadaEsFavoritoGuardado() = runTest {
        dao.filas = setOf(FavoritoEntity(100, 5))
        dao.fallaAlLeer = SQLiteException("disco no disponible")

        assertFalse(repositorio.esFavoritoGuardado(100, 5))
    }

    @Test
    fun siNoSePuedeEscribirSeDevuelveIgualLaRespuestaDeRed() = runTest {
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(MockResponse().setBody("""[{"id": 5}]"""))
        servidor.enqueue(MockResponse().setBody("""{"id": 8}"""))

        assertTrue(repositorio.esFavorito(100, 5).getOrThrow())
        assertTrue(repositorio.agregarFavorito(100, 8, ArtistType.BANDA).isSuccess)
    }
}
