package com.example.vinilos.data.repository

import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.AlbumEntity
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * Los repositorios hacen su trabajo en el dispatcher que reciben (en la app, `Dispatchers.IO`), no en
 * el de quien los llama, que en los ViewModel es el hilo principal.
 */
class RepositoriosDispatcherTest {

    /** Corre todo en un hilo con nombre propio y cuenta cuántas veces se le pidió trabajo. */
    private class DispatcherDePrueba : CoroutineDispatcher() {
        private val hilo = Executors.newSingleThreadExecutor { Thread(it, NOMBRE_HILO) }
        val usos = AtomicInteger()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            usos.incrementAndGet()
            hilo.execute(block)
        }

        fun cerrar() = hilo.shutdown()
    }

    /** Anota en qué hilo se lee y se escribe la caché. */
    private class DaoQueAnotaElHilo : AlbumDao() {
        val hilos: MutableList<String> = Collections.synchronizedList(mutableListOf())

        override suspend fun todos(): List<AlbumEntity> {
            hilos += Thread.currentThread().name
            return emptyList()
        }

        override suspend fun borrarTodos() {
            hilos += Thread.currentThread().name
        }

        override suspend fun insertar(albumes: List<AlbumEntity>) {
            hilos += Thread.currentThread().name
        }
    }

    private lateinit var servidor: MockWebServer
    private lateinit var dispatcher: DispatcherDePrueba
    private lateinit var adaptador: ServiceAdapter

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        dispatcher = DispatcherDePrueba()
        adaptador = ServiceAdapter(servidor.url("/").toString())
    }

    @After
    fun terminar() {
        dispatcher.cerrar()
        servidor.shutdown()
    }

    private fun responderListaVacia(veces: Int = 1) =
        repeat(veces) { servidor.enqueue(MockResponse().setBody("[]")) }

    @Test
    fun coleccionistasUsaElDispatcherRecibido() = runTest {
        responderListaVacia()

        val resultado = CollectorRepositoryImpl(adaptador, dispatcher = dispatcher).obtenerColeccionistas()

        assertTrue(resultado.isSuccess)
        assertTrue(dispatcher.usos.get() > 0)
    }

    @Test
    fun artistasUsaElDispatcherRecibido() = runTest {
        responderListaVacia(veces = 2) // músicos y bandas

        val resultado = ArtistRepositoryImpl(adaptador, dispatcher).obtenerArtistas()

        assertTrue(resultado.isSuccess)
        assertTrue(dispatcher.usos.get() > 0)
    }

    @Test
    fun favoritosUsaElDispatcherRecibido() = runTest {
        responderListaVacia()

        val resultado = FavoriteRepositoryImpl(adaptador, dispatcher = dispatcher).esFavorito(100, 1)

        assertTrue(resultado.isSuccess)
        assertTrue(dispatcher.usos.get() > 0)
    }

    @Test
    fun albumesLeenYEscribenLaCacheEnElDispatcherRecibido() = runTest {
        responderListaVacia()
        val dao = DaoQueAnotaElHilo()
        val repositorio = AlbumRepositoryImpl(adaptador, albumDao = dao, dispatcher = dispatcher)

        assertTrue(repositorio.obtenerAlbumes().isSuccess)
        repositorio.obtenerAlbumesGuardados()

        assertTrue(dispatcher.usos.get() > 0)
        assertTrue(dao.hilos.isNotEmpty())
        assertEquals(listOf(NOMBRE_HILO), dao.hilos.distinct())
    }

    private companion object {
        const val NOMBRE_HILO = "io-de-prueba"
    }
}
