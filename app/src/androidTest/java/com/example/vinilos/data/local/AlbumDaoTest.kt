package com.example.vinilos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import android.database.sqlite.SQLiteConstraintException
import org.junit.Assert.fail
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

    // ---------- Detalle (add-cache-consultas, D1 y D4) ----------

    private fun detalle(id: Int, nombre: String = "Álbum $id") = AlbumDetalleEntity(
        id = id,
        nombre = nombre,
        portada = "https://portadas.example/$id.jpg",
        descripcion = "Descripción $id",
        genero = "Salsa",
        disquera = "Elektra",
        fechaLanzamiento = "1984-08-01T00:00:00.000Z",
        canciones = listOf(Cancion("Decisiones", "5:05"), Cancion("GDBD", "4:30")),
        artistas = listOf(Artista("Rubén Blades", null)),
        comentarios = listOf(Comentario("Excelente", 5), Comentario("Sin calificación", null))
    )

    @Test
    fun sinDetalleGuardadoDevuelveNull() = runTest {
        assertNull(dao.detalle(100))
    }

    @Test
    fun guardarDetalleLoLeeConSusListas() = runTest {
        dao.guardarDetalle(detalle(100))

        assertEquals(detalle(100), dao.detalle(100))
    }

    @Test
    fun guardarDetalleReemplazaElAnterior() = runTest {
        dao.guardarDetalle(detalle(100))

        dao.guardarDetalle(detalle(100, nombre = "Nombre nuevo").copy(canciones = emptyList()))

        assertEquals(detalle(100, nombre = "Nombre nuevo").copy(canciones = emptyList()), dao.detalle(100))
    }

    @Test
    fun borrarAlbumLoQuitaDelCatalogoYDelDetalle() = runTest {
        dao.reemplazarCatalogo(listOf(album(100), album(101)), listOf(detalle(100), detalle(101)))

        dao.borrarAlbum(100)

        assertEquals(listOf(album(101)), dao.todos())
        assertNull(dao.detalle(100))
        assertEquals(detalle(101), dao.detalle(101))
    }

    @Test
    fun reemplazarCatalogoReemplazaLasDosTablas() = runTest {
        dao.reemplazarCatalogo(listOf(album(100), album(101)), listOf(detalle(100), detalle(101)))

        // El 102 no pasó la validación estricta: está en el catálogo pero no tiene detalle.
        dao.reemplazarCatalogo(listOf(album(101), album(102)), listOf(detalle(101, nombre = "Nuevo")))

        assertEquals(setOf(album(101), album(102)), dao.todos().toSet())
        assertNull(dao.detalle(100))
        assertEquals("Nuevo", dao.detalle(101)?.nombre)
        assertNull(dao.detalle(102))
    }

    @Test
    fun guardaElCatalogoConSuDetalleDelTamanoDelBackend() = runTest {
        val albumes = (1..274).map { album(it) }
        val detalles = (1..274).map { detalle(it) }

        dao.reemplazarCatalogo(albumes, detalles)

        assertEquals(274, dao.todos().size)
        assertEquals(detalle(274), dao.detalle(274))
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

    // ---------- Escrituras incrementales (optimizar-cache-e-imagenes, D6) ----------

    @Test
    fun guardarAlbumesCambiaSoloLasFilasRecibidas() = runTest {
        dao.reemplazar(listOf(album(100), album(101)))

        dao.guardarAlbumes(listOf(album(101, "Nombre nuevo"), album(102)))

        assertEquals(setOf(album(100), album(101, "Nombre nuevo"), album(102)), dao.todos().toSet())
    }

    @Test
    fun aplicarCatalogoGuardaYBorraSoloLasDiferencias() = runTest {
        dao.reemplazarCatalogo(listOf(album(100), album(101)), listOf(detalle(100), detalle(101)))

        dao.aplicarCatalogo(
            CambiosDeTabla.diferencias(listOf(album(102)), listOf(100)),
            CambiosDeTabla.diferencias(listOf(detalle(101, "Nombre nuevo")), listOf(100))
        )

        assertEquals(setOf(album(101), album(102)), dao.todos().toSet())
        assertNull(dao.detalle(100))
        assertEquals(detalle(101, "Nombre nuevo"), dao.detalle(101))
    }

    @Test
    fun aplicarCatalogoPuedeReemplazarUnaTablaYCambiarSoloLaOtra() = runTest {
        dao.reemplazarCatalogo(listOf(album(100)), listOf(detalle(100), detalle(999)))

        dao.aplicarCatalogo(
            CambiosDeTabla.diferencias(listOf(album(101)), emptyList()),
            CambiosDeTabla.reemplazar(listOf(detalle(101)))
        )

        assertEquals(setOf(album(100), album(101)), dao.todos().toSet())
        assertNull(dao.detalle(100))
        assertNull(dao.detalle(999))
        assertEquals(detalle(101), dao.detalle(101))
    }

    @Test
    fun borrarPorIdFuncionaConMasIdsQueElLimiteDeSqlite() = runTest {
        dao.reemplazarCatalogo((1..1200).map { album(it) }, (1..1200).map { detalle(it) })

        dao.aplicarCatalogo(
            CambiosDeTabla.diferencias(emptyList(), (1..1199).toList()),
            CambiosDeTabla.diferencias(emptyList(), (2..1200).toList())
        )

        assertEquals(listOf(1200), dao.todos().map { it.id })
        assertEquals(detalle(1), dao.detalle(1))
        assertNull(dao.detalle(1200))
    }

    @Test
    fun aplicarCatalogoEsAtomico() = runTest {
        dao.reemplazarCatalogo(listOf(album(100)), listOf(detalle(100)))

        try {
            dao.aplicarCatalogo(
                CambiosDeTabla.diferencias(listOf(album(101)), listOf(100)),
                // Un id repetido hace fallar la inserción después de cambiar el catálogo.
                CambiosDeTabla.reemplazar(listOf(detalle(1), detalle(1)))
            )
            fail("La inserción con un id repetido debía fallar")
        } catch (e: SQLiteConstraintException) {
            // Esperado.
        }

        assertEquals(listOf(album(100)), dao.todos())
        assertEquals(detalle(100), dao.detalle(100))
    }
}
