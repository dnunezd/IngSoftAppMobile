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

        /** Cada escritura que recibió, en orden, con las claves que tocó (optimizar-cache-e-imagenes). */
        val operaciones = mutableListOf<String>()

        private fun anotar(operacion: String, claves: List<Any> = emptyList()) {
            fallaAlEscribir?.let { throw it }
            operaciones += if (claves.isEmpty()) operacion else "$operacion$claves"
        }

        override suspend fun todos(): List<ColeccionistaEntity> {
            fallaAlLeer?.let { throw it }
            return filas.sortedBy { it.posicion }
        }

        override suspend fun borrarTodos() {
            anotar("borrarTodos")
            filas = emptyList()
        }

        override suspend fun insertar(coleccionistas: List<ColeccionistaEntity>) {
            anotar("insertar", coleccionistas.map { it.id })
            filas = filas + coleccionistas
            escrituras++
        }

        override suspend fun guardar(coleccionistas: List<ColeccionistaEntity>) {
            anotar("guardar", coleccionistas.map { it.id })
            filas = filas.filterNot { fila -> coleccionistas.any { it.id == fila.id } } + coleccionistas
        }

        override suspend fun borrarPorId(ids: List<Int>) {
            anotar("borrarPorId", ids)
            filas = filas.filterNot { it.id in ids }
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

    // ---------- Escribir solo lo que cambió (optimizar-cache-e-imagenes, D4 y D5) ----------

    private fun coleccionistas(vararg pares: Pair<Int, String>, version: String? = null) = MockResponse()
        .setBody(pares.joinToString(prefix = "[", postfix = "]") { (id, nombre) ->
            """{"id": $id, "name": "$nombre", "email": "c$id@a.com"}"""
        })
        .apply { if (version != null) setHeader("ETag", version) }

    @Test
    fun conLaMismaVersionNoSeEscribeNada() = runTest {
        servidor.enqueue(coleccionistas(1 to "Ana", 2 to "Beto", version = "W/\"v1\""))
        servidor.enqueue(coleccionistas(1 to "Ana", 2 to "Beto", version = "W/\"v1\""))
        repositorio.obtenerColeccionistas().getOrThrow()
        dao.operaciones.clear()

        assertEquals(listOf(1, 2), repositorio.obtenerColeccionistas().getOrThrow().map { it.id })

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun conOtraVersionYElMismoContenidoNoSeEscribeNada() = runTest {
        servidor.enqueue(coleccionistas(1 to "Ana", version = "W/\"v1\""))
        servidor.enqueue(coleccionistas(1 to "Ana", version = "W/\"v2\""))
        repositorio.obtenerColeccionistas().getOrThrow()
        dao.operaciones.clear()

        repositorio.obtenerColeccionistas().getOrThrow()

        assertEquals(emptyList<String>(), dao.operaciones)
    }

    @Test
    fun unColeccionistaNuevoAlPrincipioEscribeSoloLasFilasQueCambiaronDePosicion() = runTest {
        servidor.enqueue(coleccionistas(1 to "Ana", 2 to "Beto", version = "W/\"v1\""))
        servidor.enqueue(coleccionistas(3 to "Carla", 1 to "Ana", 2 to "Beto", version = "W/\"v2\""))
        repositorio.obtenerColeccionistas().getOrThrow()
        dao.operaciones.clear()

        repositorio.obtenerColeccionistas().getOrThrow()

        assertEquals(listOf("guardar[3, 1, 2]"), dao.operaciones)
        assertEquals(listOf(3, 1, 2), dao.filas.sortedBy { it.posicion }.map { it.id })
    }

    @Test
    fun unColeccionistaQueYaNoLlegaSeBorraSoloPorSuId() = runTest {
        servidor.enqueue(coleccionistas(1 to "Ana", 2 to "Beto", version = "W/\"v1\""))
        servidor.enqueue(coleccionistas(1 to "Ana", version = "W/\"v2\""))
        repositorio.obtenerColeccionistas().getOrThrow()
        dao.operaciones.clear()

        repositorio.obtenerColeccionistas().getOrThrow()

        assertEquals(listOf("borrarPorId[2]"), dao.operaciones)
        assertEquals(listOf(1), dao.filas.map { it.id })
    }

    @Test
    fun trasUnFalloAlGuardarLaSiguienteConsultaSinCambiosVuelveAGuardarTodo() = runTest {
        servidor.enqueue(coleccionistas(1 to "Ana", version = "W/\"v1\""))
        servidor.enqueue(coleccionistas(1 to "Ana Nueva", version = "W/\"v2\""))
        servidor.enqueue(coleccionistas(1 to "Ana Nueva", version = "W/\"v2\""))
        repositorio.obtenerColeccionistas().getOrThrow()
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        repositorio.obtenerColeccionistas().getOrThrow()
        dao.fallaAlEscribir = null
        dao.operaciones.clear()

        repositorio.obtenerColeccionistas().getOrThrow()

        assertEquals("borrarTodos", dao.operaciones.first())
        assertEquals(listOf("Ana Nueva"), dao.filas.map { it.nombre })
    }

    @Test
    fun unaCancelacionAlGuardarSeRelanzaYLaSiguienteConsultaGuardaTodo() = runTest {
        servidor.enqueue(coleccionistas(1 to "Ana", version = "W/\"v1\""))
        servidor.enqueue(coleccionistas(1 to "Ana Nueva", version = "W/\"v2\""))
        servidor.enqueue(coleccionistas(1 to "Ana", version = "W/\"v1\""))
        repositorio.obtenerColeccionistas().getOrThrow()
        dao.fallaAlEscribir = CancellationException("cancelada")
        try {
            repositorio.obtenerColeccionistas()
            fail("La cancelación debe relanzarse")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
        dao.fallaAlEscribir = null
        dao.operaciones.clear()

        repositorio.obtenerColeccionistas().getOrThrow()

        assertEquals("borrarTodos", dao.operaciones.first())
    }
}
