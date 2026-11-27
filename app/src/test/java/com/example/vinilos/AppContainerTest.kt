package com.example.vinilos

import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.AlbumDetailEntity
import com.example.vinilos.data.local.AlbumEntity
import com.example.vinilos.data.local.ArtistDao
import com.example.vinilos.data.local.ArtistDetailEntity
import com.example.vinilos.data.local.ArtistEntity
import com.example.vinilos.data.local.CollectorDao
import com.example.vinilos.data.local.CollectorEntity
import com.example.vinilos.data.local.FavoriteDao
import com.example.vinilos.data.local.FavoriteEntity
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.NewComment
import com.example.vinilos.data.repository.AlbumNotAvailable
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.CommentRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.TimeUnit

private class AlbumRepositoryFalso : AlbumRepository {
    override suspend fun getAlbum(id: Int): Result<Album> = Result.failure(AlbumNotAvailable())

    override suspend fun getAlbums(): Result<List<AlbumSummary>> = error("No se usa en esta prueba")
    override suspend fun getCachedAlbums(): List<AlbumSummary> = error("No se usa en esta prueba")
    override suspend fun getCachedAlbum(id: Int): Album = error("No se usa en esta prueba")
    override suspend fun createAlbum(album: NewAlbum): Result<Int> = error("No se usa en esta prueba")
    override suspend fun addTrack(
        albumId: Int,
        cancion: com.example.vinilos.data.model.Track
    ): Result<Unit> = error("No se usa en esta prueba")
}

private class CommentRepositoryFalso : CommentRepository {
    override suspend fun postComment(albumId: Int, comentario: NewComment): Result<Unit> = Result.success(Unit)
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
            contenedor.collectorRepository.getCollectors().getOrThrow()
        )
        val repositorio = contenedor.albumRepository
        assertSame(repositorio, contenedor.albumRepository)
        assertTrue(repositorio.getAlbum(123).exceptionOrNull() is AlbumNotAvailable)
        assertEquals("/collectors", servidor.takeRequest(1, TimeUnit.SECONDS)?.path)
        assertEquals("/albums/123", servidor.takeRequest(1, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun albumRepositorioPuedeReemplazarseComoLosDemas() = runTest {
        val falso = AlbumRepositoryFalso()
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), albumesRepositorio = falso)

        assertSame(falso, contenedor.albumRepository)
        assertTrue(contenedor.albumRepository.getAlbum(1).exceptionOrNull() is AlbumNotAvailable)
    }

    @Test
    fun comentariosUsanLaConfiguracionDelContenedor() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"id": 7, "description": "Gran prensado", "rating": 5}"""))
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString())

        val repositorio = contenedor.comentarioRepository
        assertSame(repositorio, contenedor.comentarioRepository)
        assertTrue(repositorio.postComment(100, NewComment(101, 5, "Gran prensado")).isSuccess)
        assertEquals("/albums/100/comments", servidor.takeRequest(1, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun comentarioRepositorioPuedeReemplazarseComoLosDemas() {
        val falso = CommentRepositoryFalso()
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), comentariosRepositorio = falso)

        assertSame(falso, contenedor.comentarioRepository)
    }

    @Test
    fun elDaoDelContenedorLlegaAlRepositorioDeAlbumes() = runTest {
        val dao = AlbumDaoEnMemoria()
        servidor.enqueue(
            MockResponse().setBody("""[{"id": 100, "name": "Buscando América", "genre": "Salsa"}]""")
        )
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), albumDao = dao)

        contenedor.albumRepository.getAlbums().getOrThrow()

        assertEquals(listOf(100), dao.filas.map { it.id })
        assertEquals(listOf(100), contenedor.albumRepository.getCachedAlbums().map { it.id })
    }

    @Test
    fun sinDaoElRepositorioDeAlbumesNoGuardaNada() = runTest {
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString())

        assertTrue(contenedor.albumRepository.getCachedAlbums().isEmpty())
    }
    @Test
    fun elDaoDeColeccionistasLlegaAlRepositorio() = runTest {
        val dao = CollectorDaoEnMemoria()
        servidor.enqueue(MockResponse().setBody("""[{"id": 100, "name": "Manolo Bellon", "email": "m@a.com"}]"""))
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), coleccionistaDao = dao)

        contenedor.collectorRepository.getCollectors().getOrThrow()

        assertEquals(listOf(100), dao.filas.map { it.id })
        assertEquals(listOf(100), contenedor.collectorRepository.getCachedCollectors().map { it.id })
    }

    @Test
    fun elDaoDeArtistasLlegaAlRepositorio() = runTest {
        val dao = ArtistDaoEnMemoria()
        servidor.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) = when (request.path) {
                "/bands" -> MockResponse().setBody("""[{"id": 101, "name": "Queen", "image": null}]""")
                else -> MockResponse().setBody("[]")
            }
        }
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), artistaDao = dao)

        contenedor.artistRepository.getArtists().getOrThrow()

        assertEquals(listOf(101), dao.filas.map { it.id })
        assertEquals(listOf(101), contenedor.artistRepository.getCachedArtists().map { it.id })
    }

    @Test
    fun elDaoDeFavoritosLlegaAlRepositorio() = runTest {
        val dao = FavoriteDaoEnMemoria()
        servidor.enqueue(MockResponse().setBody("""[{"id": 5}]"""))
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), favoritoDao = dao)

        assertTrue(contenedor.favoriteRepository.esFavorito(100, 5).getOrThrow())

        assertTrue(contenedor.favoriteRepository.esFavoritoGuardado(100, 5))
    }

    @get:Rule
    val carpeta = TemporaryFolder()

    @Test
    fun conCarpetaDeCacheLaConsultaRepetidaEnviaLaVersionGuardada() = runTest {
        servidor.enqueue(MockResponse().setHeader("ETag", "W/\"c1\"").setBody("""[{"id": 1, "name": "A", "email": "a@a.com"}]"""))
        servidor.enqueue(MockResponse().setResponseCode(304).setHeader("ETag", "W/\"c1\""))
        val contenedor = AppContainer(baseUrl = servidor.url("/").toString(), carpetaCacheHttp = carpeta.newFolder("http"))

        contenedor.collectorRepository.getCollectors().getOrThrow()
        val repetida = contenedor.collectorRepository.getCollectors().getOrThrow()

        assertEquals(listOf(1), repetida.map { it.id })
        servidor.takeRequest(1, TimeUnit.SECONDS)
        assertEquals("W/\"c1\"", servidor.takeRequest(1, TimeUnit.SECONDS)?.getHeader("If-None-Match"))
    }
}

private class AlbumDaoEnMemoria : AlbumDao() {
    var filas: List<AlbumEntity> = emptyList()
    override suspend fun todos() = filas
    override suspend fun borrarTodos() { filas = emptyList() }
    override suspend fun insertar(albumes: List<AlbumEntity>) { filas = filas + albumes }
    override suspend fun detalle(id: Int): AlbumDetailEntity? = null
    override suspend fun saveDetail(detalle: AlbumDetailEntity) = Unit
    override suspend fun borrarDetalles() = Unit
    override suspend fun insertarDetalles(detalles: List<AlbumDetailEntity>) = Unit
    override suspend fun borrarDelCatalogo(id: Int) { filas = filas.filterNot { it.id == id } }
    override suspend fun borrarDetalle(id: Int) = Unit
    override suspend fun saveAlbums(albumes: List<AlbumEntity>) {
        filas = filas.filterNot { fila -> albumes.any { it.id == fila.id } } + albumes
    }
    override suspend fun saveDetails(detalles: List<AlbumDetailEntity>) = Unit
    override suspend fun borrarAlbumesPorId(ids: List<Int>) { filas = filas.filterNot { it.id in ids } }
    override suspend fun borrarDetallesPorId(ids: List<Int>) = Unit
}

private class CollectorDaoEnMemoria : CollectorDao() {
    var filas: List<CollectorEntity> = emptyList()
    override suspend fun todos() = filas
    override suspend fun borrarTodos() { filas = emptyList() }
    override suspend fun insertar(coleccionistas: List<CollectorEntity>) { filas = filas + coleccionistas }
    override suspend fun save(coleccionistas: List<CollectorEntity>) {
        filas = filas.filterNot { fila -> coleccionistas.any { it.id == fila.id } } + coleccionistas
    }
    override suspend fun borrarPorId(ids: List<Int>) { filas = filas.filterNot { it.id in ids } }
}

private class ArtistDaoEnMemoria : ArtistDao() {
    var filas: List<ArtistEntity> = emptyList()
    override suspend fun todos() = filas
    override suspend fun borrarTodos() { filas = emptyList() }
    override suspend fun insertar(artistas: List<ArtistEntity>) { filas = filas + artistas }
    override suspend fun detalle(tipo: ArtistType, id: Int): ArtistDetailEntity? = null
    override suspend fun saveDetail(detalle: ArtistDetailEntity) = Unit
    override suspend fun borrarDetalle(tipo: ArtistType, id: Int) = Unit
    override suspend fun saveArtists(artistas: List<ArtistEntity>) {
        filas = filas.filterNot { fila -> artistas.any { it.tipo == fila.tipo && it.id == fila.id } } + artistas
    }
    override suspend fun borrarArtistas(tipo: ArtistType, ids: List<Int>) {
        filas = filas.filterNot { it.tipo == tipo && it.id in ids }
    }
}

private class FavoriteDaoEnMemoria : FavoriteDao() {
    var filas: Set<FavoriteEntity> = emptySet()
    override suspend fun existe(coleccionistaId: Int, artistaId: Int) = FavoriteEntity(coleccionistaId, artistaId) in filas
    override suspend fun deColeccionista(coleccionistaId: Int) =
        filas.filter { it.coleccionistaId == coleccionistaId }.map { it.artistaId }
    override suspend fun borrarDe(coleccionistaId: Int) { filas = filas.filterNot { it.coleccionistaId == coleccionistaId }.toSet() }
    override suspend fun insertar(favoritos: List<FavoriteEntity>) { filas = filas + favoritos }
    override suspend fun borrarDeColeccionista(coleccionistaId: Int, artistaIds: List<Int>) {
        filas = filas.filterNot { it.coleccionistaId == coleccionistaId && it.artistaId in artistaIds }.toSet()
    }
}
