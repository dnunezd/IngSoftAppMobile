package com.example.vinilos

import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.AlbumDetalleEntity
import com.example.vinilos.data.local.AlbumEntity
import com.example.vinilos.data.local.ArtistaDao
import com.example.vinilos.data.local.ArtistaDetalleEntity
import com.example.vinilos.data.local.ArtistaEntity
import com.example.vinilos.data.local.ColeccionistaDao
import com.example.vinilos.data.local.ColeccionistaEntity
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumNoDisponible
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

private class AlbumRepositoryFalso : AlbumRepository {
    override suspend fun obtenerAlbum(id: Int): Result<Album> = Result.failure(AlbumNoDisponible())

    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> = error("No se usa en esta prueba")
    override suspend fun obtenerAlbumesGuardados(): List<AlbumResumen> = error("No se usa en esta prueba")
    override suspend fun obtenerAlbumGuardado(id: Int): Album? = error("No se usa en esta prueba")
    override suspend fun crearAlbum(album: AlbumNuevo): Result<Int> = error("No se usa en esta prueba")
}

class AppContainerTest {

    private lateinit var servidor: MockWebServer

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun albumYColeccionistasUsanLaConfiguracionDelContenedor() = runTest {
        servidor.enqueue(MockResponse().setBody("[]"))
        servidor.enqueue(MockResponse().setResponseCode(404))
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString())

        assertEquals(
            emptyList<Collector>(),
            contenedor.collectorRepository.obtenerColeccionistas().getOrThrow()
        )
        val repositorio = contenedor.albumRepository
        assertSame(repositorio, contenedor.albumRepository)
        assertTrue(repositorio.obtenerAlbum(123).exceptionOrNull() is AlbumNoDisponible)
        assertEquals("/collectors", servidor.takeRequest(1, TimeUnit.SECONDS)?.path)
        assertEquals("/albums/123", servidor.takeRequest(1, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun albumRepositorioPuedeReemplazarseComoLosDemas() = runTest {
        val falso = AlbumRepositoryFalso()
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), albumesRepositorio = falso)

        assertSame(falso, contenedor.albumRepository)
        assertTrue(contenedor.albumRepository.obtenerAlbum(1).exceptionOrNull() is AlbumNoDisponible)
    }

    @Test
    fun elDaoDelContenedorLlegaAlRepositorioDeAlbumes() = runTest {
        val dao = AlbumDaoEnMemoria()
        servidor.enqueue(
            MockResponse().setBody("""[{"id": 100, "name": "Buscando América", "genre": "Salsa"}]""")
        )
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), albumDao = dao)

        contenedor.albumRepository.obtenerAlbumes().getOrThrow()

        assertEquals(listOf(100), dao.filas.map { it.id })
        assertEquals(listOf(100), contenedor.albumRepository.obtenerAlbumesGuardados().map { it.id })
    }

    @Test
    fun sinDaoElRepositorioDeAlbumesNoGuardaNada() = runTest {
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString())

        assertTrue(contenedor.albumRepository.obtenerAlbumesGuardados().isEmpty())
    }
    @Test
    fun elDaoDeColeccionistasLlegaAlRepositorio() = runTest {
        val dao = ColeccionistaDaoEnMemoria()
        servidor.enqueue(MockResponse().setBody("""[{"id": 100, "name": "Manolo Bellon", "email": "m@a.com"}]"""))
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), coleccionistaDao = dao)

        contenedor.collectorRepository.obtenerColeccionistas().getOrThrow()

        assertEquals(listOf(100), dao.filas.map { it.id })
        assertEquals(listOf(100), contenedor.collectorRepository.obtenerColeccionistasGuardados().map { it.id })
    }

    @Test
    fun elDaoDeArtistasLlegaAlRepositorio() = runTest {
        val dao = ArtistaDaoEnMemoria()
        servidor.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) = when (request.path) {
                "/bands" -> MockResponse().setBody("""[{"id": 101, "name": "Queen", "image": null}]""")
                else -> MockResponse().setBody("[]")
            }
        }
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), artistaDao = dao)

        contenedor.artistRepository.obtenerArtistas().getOrThrow()

        assertEquals(listOf(101), dao.filas.map { it.id })
        assertEquals(listOf(101), contenedor.artistRepository.obtenerArtistasGuardados().map { it.id })
    }
}

private class AlbumDaoEnMemoria : AlbumDao() {
    var filas: List<AlbumEntity> = emptyList()
    override suspend fun todos() = filas
    override suspend fun borrarTodos() { filas = emptyList() }
    override suspend fun insertar(albumes: List<AlbumEntity>) { filas = filas + albumes }
    override suspend fun detalle(id: Int): AlbumDetalleEntity? = null
    override suspend fun guardarDetalle(detalle: AlbumDetalleEntity) = Unit
    override suspend fun borrarDetalles() = Unit
    override suspend fun insertarDetalles(detalles: List<AlbumDetalleEntity>) = Unit
    override suspend fun borrarDelCatalogo(id: Int) { filas = filas.filterNot { it.id == id } }
    override suspend fun borrarDetalle(id: Int) = Unit

}

private class ColeccionistaDaoEnMemoria : ColeccionistaDao() {
    var filas: List<ColeccionistaEntity> = emptyList()
    override suspend fun todos() = filas
    override suspend fun borrarTodos() { filas = emptyList() }
    override suspend fun insertar(coleccionistas: List<ColeccionistaEntity>) { filas = filas + coleccionistas }
}

private class ArtistaDaoEnMemoria : ArtistaDao() {
    var filas: List<ArtistaEntity> = emptyList()
    override suspend fun todos() = filas
    override suspend fun borrarTodos() { filas = emptyList() }
    override suspend fun insertar(artistas: List<ArtistaEntity>) { filas = filas + artistas }
    override suspend fun detalle(tipo: ArtistType, id: Int): ArtistaDetalleEntity? = null
    override suspend fun guardarDetalle(detalle: ArtistaDetalleEntity) = Unit
    override suspend fun borrarDetalle(tipo: ArtistType, id: Int) = Unit
}
