package com.example.vinilos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Saved collectors (add-cache-consultas, D1): the table holds the latest response, in its original order. */
@RunWith(AndroidJUnit4::class)
class CollectorDaoTest {

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: VinilosDb
    private lateinit var dao: CollectorDao

    @Before
    fun createBase() {
        db = Room.inMemoryDatabaseBuilder(contexto, VinilosDb::class.java).build()
        dao = db.coleccionistaDao()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    private fun coleccionista(id: Int, posicion: Int, nombre: String = "Coleccionista $id") =
        CollectorEntity(id = id, nombre = nombre, correo = "c$id@example.com", posicion = posicion)

    @Test
    fun unaBaseNuevaNoTieneColeccionistas() = runTest {
        assertTrue(dao.todos().isEmpty())
    }

    @Test
    fun seLeenEnElOrdenDeLaPosicionNoDelId() = runTest {
        // The backend doesn't deliver them sorted by id; the selector preserves their order.
        val enOrden = listOf(coleccionista(105, 0), coleccionista(100, 1), coleccionista(103, 2))

        dao.reemplazar(enOrden.reversed())

        assertEquals(enOrden, dao.todos())
    }

    @Test
    fun reemplazarBorraLosQueYaNoVienen() = runTest {
        dao.reemplazar(listOf(coleccionista(100, 0), coleccionista(101, 1), coleccionista(102, 2)))

        dao.reemplazar(listOf(coleccionista(101, 0, nombre = "Nombre nuevo"), coleccionista(103, 1)))

        assertEquals(listOf(coleccionista(101, 0, nombre = "Nombre nuevo"), coleccionista(103, 1)), dao.todos())
    }

    @Test
    fun reemplazarConUnaListaVaciaDejaLaTablaVacia() = runTest {
        dao.reemplazar(listOf(coleccionista(100, 0)))

        dao.reemplazar(emptyList())

        assertTrue(dao.todos().isEmpty())
    }

    // ---------- Incremental writes (optimizar-cache-e-imagenes, D6) ----------

    @Test
    fun aplicarGuardaLasFilasCambiadasYBorraLasQueNoLlegan() = runTest {
        dao.reemplazar(listOf(coleccionista(1, 0), coleccionista(2, 1), coleccionista(3, 2)))

        dao.aplicar(TableChanges.diferencias(listOf(coleccionista(3, 1), coleccionista(4, 2)), listOf(2)))

        assertEquals(listOf(coleccionista(1, 0), coleccionista(3, 1), coleccionista(4, 2)), dao.todos())
    }

    @Test
    fun aplicarConReemplazoDejaSoloLasFilasRecibidas() = runTest {
        dao.reemplazar(listOf(coleccionista(1, 0), coleccionista(2, 1)))

        dao.aplicar(TableChanges.reemplazar(listOf(coleccionista(5, 0))))

        assertEquals(listOf(coleccionista(5, 0)), dao.todos())
    }
}
