package com.example.vinilos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.ArtistPrize
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Artistas guardados (add-cache-consultas, D1): listado completo y detalle por (tipo, id). */
@RunWith(AndroidJUnit4::class)
class ArtistDaoTest {

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: VinilosDb
    private lateinit var dao: ArtistDao

    @Before
    fun createBase() {
        db = Room.inMemoryDatabaseBuilder(contexto, VinilosDb::class.java).build()
        dao = db.artistaDao()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    private fun artista(tipo: ArtistType, id: Int, nombre: String = "Artista $id") =
        ArtistEntity(tipo = tipo, id = id, nombre = nombre, imagen = "https://img.example/$id.jpg")

    private fun detalle(tipo: ArtistType, id: Int, nombre: String = "Queen") = ArtistDetailEntity(
        tipo = tipo,
        id = id,
        nombre = nombre,
        imagen = null,
        descripcion = "Banda británica",
        fecha = "1970-01-01T00:00:00.000Z",
        integrantes = listOf(IntegranteArtista(1, "Freddie Mercury", null)),
        albumes = listOf(AlbumArtista(100, "A Night at the Opera", "https://img.example/100.jpg", "1975")),
        premios = listOf(ArtistPrize(7, "Grammy", "Academia", null))
    )

    // ---------- Listado ----------

    @Test
    fun unaBaseNuevaNoTieneArtistas() = runTest {
        assertTrue(dao.todos().isEmpty())
    }

    @Test
    fun unMusicoYUnaBandaConElMismoIdNoSePisan() = runTest {
        val musico = artista(ArtistType.MUSICO, 100, "Rubén Blades")
        val banda = artista(ArtistType.BANDA, 100, "Queen")

        dao.reemplazar(listOf(musico, banda))

        assertEquals(setOf(musico, banda), dao.todos().toSet())
    }

    @Test
    fun reemplazarBorraLosArtistasQueYaNoVienen() = runTest {
        dao.reemplazar(listOf(artista(ArtistType.MUSICO, 1), artista(ArtistType.BANDA, 2)))

        dao.reemplazar(listOf(artista(ArtistType.BANDA, 2, "Nombre nuevo")))

        assertEquals(listOf(artista(ArtistType.BANDA, 2, "Nombre nuevo")), dao.todos())
    }

    // ---------- Detalle ----------

    @Test
    fun sinDetalleGuardadoDevuelveNull() = runTest {
        assertNull(dao.detalle(ArtistType.BANDA, 100))
    }

    @Test
    fun saveDetailLoLeeConSusListas() = runTest {
        val guardado = detalle(ArtistType.BANDA, 100)

        dao.saveDetail(guardado)

        assertEquals(guardado, dao.detalle(ArtistType.BANDA, 100))
    }

    @Test
    fun saveDetailReemplazaElAnteriorSinTocarElDelOtroTipo() = runTest {
        dao.saveDetail(detalle(ArtistType.BANDA, 100))
        dao.saveDetail(detalle(ArtistType.MUSICO, 100, "Rubén Blades"))

        dao.saveDetail(detalle(ArtistType.BANDA, 100, "Queen (actualizado)"))

        assertEquals("Queen (actualizado)", dao.detalle(ArtistType.BANDA, 100)?.nombre)
        assertEquals("Rubén Blades", dao.detalle(ArtistType.MUSICO, 100)?.nombre)
    }

    @Test
    fun borrarDetalleSoloBorraEseArtista() = runTest {
        dao.saveDetail(detalle(ArtistType.BANDA, 100))
        dao.saveDetail(detalle(ArtistType.MUSICO, 100))

        dao.borrarDetalle(ArtistType.BANDA, 100)

        assertNull(dao.detalle(ArtistType.BANDA, 100))
        assertEquals(detalle(ArtistType.MUSICO, 100), dao.detalle(ArtistType.MUSICO, 100))
    }

    @Test
    fun unDetalleConListasVaciasSeLeeIgual() = runTest {
        val vacio = detalle(ArtistType.MUSICO, 5)
            .copy(fecha = null, descripcion = null, integrantes = emptyList(), albumes = emptyList(), premios = emptyList())

        dao.saveDetail(vacio)

        assertEquals(vacio, dao.detalle(ArtistType.MUSICO, 5))
    }

    // ---------- Escrituras incrementales (optimizar-cache-e-imagenes, D6) ----------

    @Test
    fun aplicarListadoBorraSoloElTipoYElIdIndicados() = runTest {
        dao.reemplazar(listOf(artista(ArtistType.MUSICO, 1), artista(ArtistType.BANDA, 1), artista(ArtistType.BANDA, 2)))

        dao.aplicarListado(
            TableChanges.diferencias(
                listOf(artista(ArtistType.BANDA, 2, "Nombre nuevo"), artista(ArtistType.MUSICO, 3)),
                listOf(ArtistType.BANDA to 1)
            )
        )

        assertEquals(
            setOf(artista(ArtistType.MUSICO, 1), artista(ArtistType.BANDA, 2, "Nombre nuevo"), artista(ArtistType.MUSICO, 3)),
            dao.todos().toSet()
        )
    }

    @Test
    fun aplicarListadoBorraMasIdsQueElLimiteDeSqlite() = runTest {
        dao.reemplazar((1..1200).map { artista(ArtistType.MUSICO, it) })

        dao.aplicarListado(TableChanges.diferencias(emptyList(), (1..1199).map { ArtistType.MUSICO to it }))

        assertEquals(listOf(1200), dao.todos().map { it.id })
    }
}
