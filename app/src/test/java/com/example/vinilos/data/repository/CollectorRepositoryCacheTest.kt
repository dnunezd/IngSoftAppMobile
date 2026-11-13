package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.ColeccionistaDao
import com.example.vinilos.data.local.ColeccionistaEntity
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Coleccionistas guardados en el repositorio (add-cache-consultas, D4), con un DAO falso en memoria. */
class CollectorRepositoryCacheTest {

    /** Guarda en una lista; [fallaAlLeer] y [fallaAlEscribir] simulan un almacenamiento que no responde. */
    private class ColeccionistaDaoFalso : ColeccionistaDao() {
        var filas: List<ColeccionistaEntity> = emptyList()
        var fallaAlLeer: Throwable? = null
        var fallaAlEscribir: Throwable? = null
        var escrituras = 0

        override suspend fun todos(): List<ColeccionistaEntity> {
            fallaAlLeer?.let { throw it }
            return filas.sortedBy { it.posicion }
        }

        override suspend fun borrarTodos() {
            fallaAlEscribir?.let { throw it }
            filas = emptyList()
        }

        override suspend fun insertar(coleccionistas: List<ColeccionistaEntity>) {
            fallaAlEscribir?.let { throw it }
            filas = filas + coleccionistas
            escrituras++
        }
    }

    private lateinit var servidor: MockWebServer
    private lateinit var dao: ColeccionistaDaoFalso
    private lateinit var repositorio: CollectorRepository

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        dao = ColeccionistaDaoFalso()
        repositorio = CollectorRepositoryImpl(ServiceAdapter(servidor.url("/").toString()), coleccionistaDao = dao)
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    // El backend no los entrega ordenados por id ni por nombre: el selector conserva su orden.
    private val respuesta = """
        [
          {"id": 105, "name": "Zoe Ruiz", "email": "zoe@a.com"},
          {"name": "Sin id", "email": "x@a.com"},
          {"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"}
        ]
    """.trimIndent()

    private val zoe = Collector(105, "Zoe Ruiz", "zoe@a.com")
    private val manolo = Collector(100, "Manolo Bellon", "manollo@caracol.com.co")

    @Test
    fun unaConsultaExitosaReemplazaLoGuardadoEnElOrdenDelBackend() = runTest {
        dao.filas = listOf(ColeccionistaEntity(999, "Ya no existe", "z@a.com", 0))
        servidor.enqueue(MockResponse().setBody(respuesta))

        assertEquals(listOf(zoe, manolo), repositorio.obtenerColeccionistas().getOrThrow())
        assertEquals(
            listOf(ColeccionistaEntity(105, "Zoe Ruiz", "zoe@a.com", 0), ColeccionistaEntity(100, "Manolo Bellon", "manollo@caracol.com.co", 1)),
            dao.filas
        )
    }

    @Test
    fun loGuardadoSeDevuelveEnElMismoOrden() = runTest {
        servidor.enqueue(MockResponse().setBody(respuesta))
        repositorio.obtenerColeccionistas().getOrThrow()

        assertEquals(listOf(zoe, manolo), repositorio.obtenerColeccionistasGuardados())
    }

    @Test
    fun unaRespuestaVaciaDejaLoGuardadoVacio() = runTest {
        dao.filas = listOf(ColeccionistaEntity(100, "Manolo Bellon", "m@a.com", 0))
        servidor.enqueue(MockResponse().setBody("[]"))

        repositorio.obtenerColeccionistas().getOrThrow()

        assertTrue(dao.filas.isEmpty())
    }

    @Test
    fun unaConsultaFallidaNoTocaLoGuardado() = runTest {
        val guardado = listOf(ColeccionistaEntity(100, "Manolo Bellon", "m@a.com", 0))
        dao.filas = guardado
        servidor.enqueue(MockResponse().setResponseCode(500))

        assertTrue(repositorio.obtenerColeccionistas().exceptionOrNull() is ErrorDeCarga)
        assertEquals(guardado, dao.filas)
        assertEquals(0, dao.escrituras)
    }

    @Test
    fun sinDaoNoHayNadaGuardadoYSeConsultaComoAntes() = runTest {
        val sinCache = CollectorRepositoryImpl(ServiceAdapter(servidor.url("/").toString()))
        servidor.enqueue(MockResponse().setBody(respuesta))

        assertTrue(sinCache.obtenerColeccionistasGuardados().isEmpty())
        assertEquals(listOf(zoe, manolo), sinCache.obtenerColeccionistas().getOrThrow())
    }

    @Test
    fun siNoSePuedeLeerNoHayNadaGuardado() = runTest {
        dao.filas = listOf(ColeccionistaEntity(100, "Manolo Bellon", "m@a.com", 0))
        dao.fallaAlLeer = SQLiteException("disco no disponible")

        assertTrue(repositorio.obtenerColeccionistasGuardados().isEmpty())
    }

    @Test
    fun siNoSePuedeEscribirSeDevuelveIgualLaRespuestaDeRed() = runTest {
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(MockResponse().setBody(respuesta))

        assertEquals(listOf(zoe, manolo), repositorio.obtenerColeccionistas().getOrThrow())
    }

    @Test
    fun laCancelacionAlLeerSeRelanza() = runTest {
        dao.fallaAlLeer = CancellationException("cancelada")

        try {
            repositorio.obtenerColeccionistasGuardados()
            fail("La cancelación debe relanzarse")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
    }
}
