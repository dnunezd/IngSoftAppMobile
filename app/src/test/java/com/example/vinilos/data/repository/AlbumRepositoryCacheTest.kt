package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.AlbumDetalleEntity
import com.example.vinilos.data.local.AlbumEntity
import com.example.vinilos.data.model.AlbumResumen
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

/** Caché del catálogo en el repositorio (add-cache-catalogo, D4 y D7), con un DAO falso en memoria. */
class AlbumRepositoryCacheTest {

    /** Guarda en una lista; [fallaAlLeer] y [fallaAlEscribir] simulan un almacenamiento que no responde. */
    private class AlbumDaoFalso(inicial: List<AlbumEntity> = emptyList()) : AlbumDao() {
        var filas: List<AlbumEntity> = inicial
        var fallaAlLeer: Throwable? = null
        var fallaAlEscribir: Throwable? = null
        var escrituras = 0

        override suspend fun todos(): List<AlbumEntity> {
            fallaAlLeer?.let { throw it }
            return filas
        }

        override suspend fun borrarTodos() {
            fallaAlEscribir?.let { throw it }
            filas = emptyList()
        }

        override suspend fun insertar(albumes: List<AlbumEntity>) {
            fallaAlEscribir?.let { throw it }
            filas = filas + albumes
            escrituras++
        }

        /** Detalles guardados por id (add-cache-consultas). */
        var detalles: Map<Int, AlbumDetalleEntity> = emptyMap()

        override suspend fun detalle(id: Int): AlbumDetalleEntity? {
            fallaAlLeer?.let { throw it }
            return detalles[id]
        }

        override suspend fun guardarDetalle(detalle: AlbumDetalleEntity) {
            fallaAlEscribir?.let { throw it }
            detalles = detalles + (detalle.id to detalle)
        }

        override suspend fun borrarDetalles() {
            fallaAlEscribir?.let { throw it }
            detalles = emptyMap()
        }

        override suspend fun insertarDetalles(detalles: List<AlbumDetalleEntity>) {
            fallaAlEscribir?.let { throw it }
            this.detalles = this.detalles + detalles.associateBy { it.id }
        }

        override suspend fun borrarDelCatalogo(id: Int) {
            fallaAlEscribir?.let { throw it }
            filas = filas.filterNot { it.id == id }
        }

        override suspend fun borrarDetalle(id: Int) {
            fallaAlEscribir?.let { throw it }
            detalles = detalles - id
        }
    }

    private lateinit var servidor: MockWebServer
    private lateinit var dao: AlbumDaoFalso
    private lateinit var repositorio: AlbumRepository

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        dao = AlbumDaoFalso()
        repositorio = AlbumRepositoryImpl(ServiceAdapter(servidor.url("/").toString()), albumDao = dao)
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    private val catalogo = """
        [
          {"name": "Sin id", "genre": "Rock"},
          {"id": 100, "name": "Buscando América", "cover": "https://example.com/100.jpg", "genre": "Salsa",
           "releaseDate": "1984-08-01T00:00:00.000Z", "performers": [{"id": 1, "name": "Rubén Blades"}]},
          {"id": 102, "name": "A Night at the Opera", "genre": "Rock"}
        ]
    """.trimIndent()

    private val buscandoAmerica = AlbumResumen(
        id = 100, nombre = "Buscando América", portada = "https://example.com/100.jpg",
        genero = "Salsa", artista = "Rubén Blades", anio = "1984"
    )
    private val nightAtTheOpera = AlbumResumen(
        id = 102, nombre = "A Night at the Opera", portada = null, genero = "Rock", artista = "", anio = null
    )

    private fun fila(album: AlbumResumen) =
        AlbumEntity(album.id, album.nombre, album.portada, album.genero, album.artista, album.anio)

    @Test
    fun unaConsultaExitosaReemplazaLoGuardadoConLosAlbumesValidos() = runTest {
        dao.filas = listOf(AlbumEntity(999, "Ya no existe", null, "Pop", "", null))
        servidor.enqueue(MockResponse().setBody(catalogo))

        val albumes = repositorio.obtenerAlbumes().getOrThrow()

        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), albumes)
        assertEquals(setOf(fila(nightAtTheOpera), fila(buscandoAmerica)), dao.filas.toSet())
    }

    @Test
    fun unaRespuestaVaciaDejaLaCacheVacia() = runTest {
        dao.filas = listOf(fila(buscandoAmerica))
        servidor.enqueue(MockResponse().setBody("[]"))

        repositorio.obtenerAlbumes().getOrThrow()

        assertTrue(dao.filas.isEmpty())
    }

    @Test
    fun unaConsultaFallidaNoTocaLoGuardado() = runTest {
        dao.filas = listOf(fila(buscandoAmerica))
        servidor.enqueue(MockResponse().setResponseCode(500))

        val error = repositorio.obtenerAlbumes().exceptionOrNull()

        assertTrue(error is ErrorDeCarga)
        assertEquals(listOf(fila(buscandoAmerica)), dao.filas)
        assertEquals(0, dao.escrituras)
    }

    @Test
    fun loGuardadoSeDevuelveEnOrdenAlfabeticoSinDistinguirTildes() = runTest {
        dao.filas = listOf(
            AlbumEntity(3, "Poeta del pueblo", null, "Salsa", "", null),
            AlbumEntity(1, "Álbum blanco", null, "Rock", "", null),
            AlbumEntity(2, "Buscando América", null, "Salsa", "", null),
            AlbumEntity(4, "abbey Road", null, "Rock", "", null)
        )

        val nombres = repositorio.obtenerAlbumesGuardados().map { it.nombre }

        assertEquals(listOf("abbey Road", "Álbum blanco", "Buscando América", "Poeta del pueblo"), nombres)
    }

    @Test
    fun loGuardadoConservaTodosLosCamposDelAlbum() = runTest {
        dao.filas = listOf(fila(buscandoAmerica), fila(nightAtTheOpera))

        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), repositorio.obtenerAlbumesGuardados())
    }

    @Test
    fun sinDaoNoHayNadaGuardadoYElCatalogoSeConsultaComoAntes() = runTest {
        val sinCache = AlbumRepositoryImpl(ServiceAdapter(servidor.url("/").toString()))
        servidor.enqueue(MockResponse().setBody(catalogo))

        assertTrue(sinCache.obtenerAlbumesGuardados().isEmpty())
        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), sinCache.obtenerAlbumes().getOrThrow())
    }

    // ---- Fallos del almacenamiento (D7): la caché es una mejora, nunca una condición ----

    @Test
    fun siNoSePuedeLeerLaCacheNoHayNadaGuardado() = runTest {
        dao.filas = listOf(fila(buscandoAmerica))
        dao.fallaAlLeer = SQLiteException("disco no disponible")

        assertTrue(repositorio.obtenerAlbumesGuardados().isEmpty())
    }

    @Test
    fun siNoSePuedeEscribirLaCacheSeDevuelveIgualLaRespuestaDeRed() = runTest {
        dao.fallaAlEscribir = SQLiteException("disco lleno")
        servidor.enqueue(MockResponse().setBody(catalogo))

        assertEquals(listOf(nightAtTheOpera, buscandoAmerica), repositorio.obtenerAlbumes().getOrThrow())
    }

    @Test
    fun laCancelacionAlLeerLaCacheSeRelanza() = runTest {
        dao.fallaAlLeer = CancellationException("cancelada")

        try {
            repositorio.obtenerAlbumesGuardados()
            fail("La cancelación debe relanzarse")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
    }

    @Test
    fun laCancelacionAlEscribirLaCacheSeRelanza() = runTest {
        dao.fallaAlEscribir = CancellationException("cancelada")
        servidor.enqueue(MockResponse().setBody(catalogo))

        try {
            repositorio.obtenerAlbumes()
            fail("La cancelación debe relanzarse")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
    }
}
