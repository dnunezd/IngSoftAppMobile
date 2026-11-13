package com.example.vinilos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Favoritos confirmados (add-cache-consultas, D1): por coleccionista, reemplazados en cada comprobación. */
@RunWith(AndroidJUnit4::class)
class FavoritoDaoTest {

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: VinilosDb
    private lateinit var dao: FavoritoDao

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(contexto, VinilosDb::class.java).build()
        dao = db.favoritoDao()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    @Test
    fun sinNadaGuardadoNingunoEsFavorito() = runTest {
        assertFalse(dao.existe(coleccionistaId = 100, artistaId = 1))
        assertTrue(dao.deColeccionista(100).isEmpty())
    }

    @Test
    fun reemplazarDeUnColeccionistaBorraLosQueYaNoVienen() = runTest {
        dao.reemplazarDe(100, listOf(1, 2, 3))

        dao.reemplazarDe(100, listOf(2, 4))

        assertEquals(setOf(2, 4), dao.deColeccionista(100).toSet())
        assertFalse(dao.existe(100, 1))
        assertTrue(dao.existe(100, 4))
    }

    @Test
    fun reemplazarDeUnColeccionistaNoTocaLosDeOtro() = runTest {
        dao.reemplazarDe(100, listOf(1, 2))
        dao.reemplazarDe(101, listOf(1))

        dao.reemplazarDe(100, emptyList())

        assertTrue(dao.deColeccionista(100).isEmpty())
        assertTrue(dao.existe(101, 1))
        assertFalse(dao.existe(100, 1))
    }

    @Test
    fun agregarSumaUnFavoritoYRepetirloNoFalla() = runTest {
        dao.reemplazarDe(100, listOf(1))

        dao.agregar(100, 2)
        dao.agregar(100, 2)

        assertEquals(setOf(1, 2), dao.deColeccionista(100).toSet())
    }
}
