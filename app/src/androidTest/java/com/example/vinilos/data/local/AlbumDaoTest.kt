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

/** Caché del catálogo (add-cache-catalogo, D2): la tabla siempre es la última respuesta completa. */
@RunWith(AndroidJUnit4::class)
class AlbumDaoTest {

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: VinilosDb
    private lateinit var dao: AlbumDao

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(contexto, VinilosDb::class.java).build()
        dao = db.albumDao()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    private fun album(id: Int, nombre: String = "Álbum $id") = AlbumEntity(
        id = id,
        nombre = nombre,
        portada = "https://portadas.example/$id.jpg",
        genero = "Salsa",
        artista = "Rubén Blades",
        anio = "1984"
    )

    @Test
    fun unaBaseNuevaNoTieneAlbumes() = runTest {
        assertTrue(dao.todos().isEmpty())
    }

    @Test
    fun reemplazarGuardaLaLista() = runTest {
        val albumes = listOf(album(100), album(101).copy(portada = null, anio = null))

        dao.reemplazar(albumes)

        assertEquals(albumes.toSet(), dao.todos().toSet())
    }

    @Test
    fun reemplazarBorraLosAlbumesQueYaNoVienen() = runTest {
        dao.reemplazar(listOf(album(100), album(101), album(102)))

        dao.reemplazar(listOf(album(101, nombre = "Nombre nuevo"), album(103)))

        assertEquals(setOf(album(101, nombre = "Nombre nuevo"), album(103)), dao.todos().toSet())
    }

    @Test
    fun reemplazarConUnaListaVaciaDejaLaTablaVacia() = runTest {
        dao.reemplazar(listOf(album(100), album(101)))

        dao.reemplazar(emptyList())

        assertTrue(dao.todos().isEmpty())
    }

    @Test
    fun guardaYLeeUnCatalogoDelTamanoDelBackend() = runTest {
        // El backend de los tutores tenía 274 álbumes al medir M4 (add-crear-album/design.md).
        val albumes = (1..274).map { album(it) }

        dao.reemplazar(albumes)

        assertEquals(274, dao.todos().size)
        assertEquals(albumes.toSet(), dao.todos().toSet())
    }

    @Test
    fun loGuardadoSeConservaAlCerrarYVolverAAbrirLaBase() = runTest {
        val nombre = "album-dao-test.db"
        contexto.deleteDatabase(nombre)
        try {
            val primera = Room.databaseBuilder(contexto, VinilosDb::class.java, nombre).build()
            primera.albumDao().reemplazar(listOf(album(100), album(101)))
            primera.close()

            val segunda = Room.databaseBuilder(contexto, VinilosDb::class.java, nombre).build()
            val leidos = segunda.albumDao().todos()
            segunda.close()

            assertEquals(setOf(album(100), album(101)), leidos.toSet())
        } finally {
            contexto.deleteDatabase(nombre)
        }
    }
}
