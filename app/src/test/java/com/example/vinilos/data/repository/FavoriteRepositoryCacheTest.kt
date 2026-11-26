package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.FavoriteDao
import com.example.vinilos.data.local.FavoriteEntity
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.LoadError
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
    private class FavoriteDaoFalso : FavoriteDao() {
        var filas: Set<FavoriteEntity> = emptySet()
        var fallaAlLeer: Throwable? = null
        var fallaAlEscribir: Throwable? = null

        /** Cada escritura que recibió, en orden, con las claves que tocó (optimizar-cache-e-imagenes). */
        val operaciones = mutableListOf<String>()

        private fun anotar(operacion: String, claves: List<Any> = emptyList()) {
            fallaAlEscribir?.let { throw it }
            operaciones += if (claves.isEmpty()) operacion else "$operacion$claves"
        }

        override suspend fun existe(coleccionistaId: Int, artistaId: Int): Boolean {
            fallaAlLeer?.let { throw it }
            return FavoriteEntity(coleccionistaId, artistaId) in filas
        }

        override suspend fun deColeccionista(coleccionistaId: Int): List<Int> {
            fallaAlLeer?.let { throw it }
            return filas.filter { it.coleccionistaId == coleccionistaId }.map { it.artistaId }
        }

        override suspend fun borrarDe(coleccionistaId: Int) {
            anotar("borrarDe", listOf(coleccionistaId))
            filas = filas.filterNot { it.coleccionistaId == coleccionistaId }.toSet()
        }

        override suspend fun insertar(favoritos: List<FavoriteEntity>) {
            anotar("insertar", favoritos.map { it.artistaId })
            filas = filas + favoritos
        }

        override suspend fun borrarDeColeccionista(coleccionistaId: Int, artistaIds: List<Int>) {
            anotar("borrarDeColeccionista", artistaIds)
            filas = filas.filterNot { it.coleccionistaId == coleccionistaId && it.artistaId in artistaIds }.toSet()
        }
    }

    private lateinit var servidor: MockWebServer
    private lateinit var dao: FavoriteDaoFalso
    private lateinit var repositorio: FavoriteRepository

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        dao = FavoriteDaoFalso()
        repositorio = FavoriteRepositoryImpl(ServiceAdapter(servidor.url("/").toString()), favoritoDao = dao)
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun comprobarReemplazaLosFavoritosDeEseColeccionistaSinTocarLosDeOtro() = runTest {
        dao.filas = setOf(FavoriteEntity(100, 1), FavoriteEntity(101, 1))
        servidor.enqueue(MockResponse().setBody("""[{"id": 5, "name": "Queen"}, {"id": 8}]"""))

        assertFalse(repositorio.esFavorito(collectorId = 100, artistId = 1).getOrThrow())

        assertEquals(setOf(FavoriteEntity(100, 5), FavoriteEntity(100, 8), FavoriteEntity(101, 1)), dao.filas)
    }

    @Test
    fun unaComprobacionFallidaNoTocaLoGuardado() = runTest {
        dao.filas = setOf(FavoriteEntity(100, 5))
        servidor.enqueue(MockResponse().setResponseCode(500))

        assertTrue(repositorio.esFavorito(100, 5).exceptionOrNull() is LoadError)
        assertEquals(setOf(FavoriteEntity(100, 5)), dao.filas)
    }

    @Test
    fun agregarConSuccessGuardaElFavorito() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"id": 8}"""))

        repositorio.addFavorite(100, 8, ArtistType.MUSICO).getOrThrow()

        assertTrue(repositorio.esFavoritoGuardado(100, 8))
    }

    @Test
    fun agregarSinSuccessNoGuardaNada() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))
        servidor.enqueue(MockResponse().setResponseCode(404))

        assertTrue(repositorio.addFavorite(100, 8, ArtistType.MUSICO).exceptionOrNull() is LoadError)
        assertTrue(repositorio.addFavorite(100, 8, ArtistType.MUSICO).exceptionOrNull() is ArtistNotAvailable)

        assertTrue(dao.filas.isEmpty())
    }

    @Test
    fun esFavoritoGuardadoRespondePorColeccionistaYArtista() = runTest {
        dao.filas = setOf(FavoriteEntity(100, 5))

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
        dao.filas = setOf(FavoriteEntity(100, 5))
        dao.fallaAlLeer = SQLiteException("disco no disponible")

        assertFalse(repositorio.esFavoritoGuardado(100, 5))
    }

    @Test
    fun siNoSePuedeEscribirSeDevuelveIgualLaRespuestaDeRed() = runTest {
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(MockResponse().setBody("""[{"id": 5}]"""))
        servidor.enqueue(MockResponse().setBody("""{"id": 8}"""))

        assertTrue(repositorio.esFavorito(100, 5).getOrThrow())
        assertTrue(repositorio.addFavorite(100, 8, ArtistType.BANDA).isSuccess)
    }

    // ---------- Escribir solo lo que cambió (optimizar-cache-e-imagenes, D4 y D5) ----------

    private fun favoritos(vararg ids: Int, version: String? = null) = MockResponse()
        .setBody(ids.joinToString(prefix = "[", postfix = "]") { """{"id": $it}""" })
        .apply { if (version != null) setHeader("ETag", version) }

    @Test
    fun laComprobacionConLaMismaVersionNoEscribe() = runTest {
        servidor.enqueue(favoritos(1, 2, version = "W/\"f1\""))
        servidor.enqueue(favoritos(1, 2, version = "W/\"f1\""))
        repositorio.esFavorito(100, 1).getOrThrow()
        dao.operaciones.clear()

        assertTrue(repositorio.esFavorito(100, 2).getOrThrow())

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun laComprobacionAgregaYQuitaSoloLasDiferencias() = runTest {
        servidor.enqueue(favoritos(1, 2, version = "W/\"f1\""))
        servidor.enqueue(favoritos(2, 3, version = "W/\"f2\""))
        repositorio.esFavorito(100, 1).getOrThrow()
        dao.operaciones.clear()

        repositorio.esFavorito(100, 1).getOrThrow()

        assertEquals(listOf("insertar[3]", "borrarDeColeccionista[1]"), dao.operaciones)
        assertEquals(setOf(2, 3), dao.filas.filter { it.coleccionistaId == 100 }.map { it.artistaId }.toSet())
    }

    @Test
    fun siElFavoritoSeQuitaEnElBackendYVuelveLaVersionAnteriorLoGuardadoSeCorrige() = runTest {
        servidor.enqueue(favoritos(version = "W/\"f0\""))
        servidor.enqueue(MockResponse().setBody("""{"id": 7}"""))
        // Alguien quitó el favorito en el backend: vuelve a responder la versión de antes de agregarlo.
        servidor.enqueue(favoritos(version = "W/\"f0\""))
        repositorio.esFavorito(100, 7).getOrThrow()
        repositorio.addFavorite(100, 7, ArtistType.BANDA).getOrThrow()
        assertTrue(repositorio.esFavoritoGuardado(100, 7))

        assertFalse(repositorio.esFavorito(100, 7).getOrThrow())

        assertFalse(repositorio.esFavoritoGuardado(100, 7))
    }

    @Test
    fun trasUnFalloAlGuardarLaSiguienteComprobacionReemplazaLosFavoritos() = runTest {
        servidor.enqueue(favoritos(1, version = "W/\"f1\""))
        servidor.enqueue(favoritos(2, version = "W/\"f2\""))
        servidor.enqueue(favoritos(2, version = "W/\"f2\""))
        repositorio.esFavorito(100, 1).getOrThrow()
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        repositorio.esFavorito(100, 2).getOrThrow()
        dao.fallaAlEscribir = null
        dao.operaciones.clear()

        repositorio.esFavorito(100, 2).getOrThrow()

        assertEquals("borrarDe[100]", dao.operaciones.first())
        assertEquals(setOf(2), dao.filas.map { it.artistaId }.toSet())
    }
}
