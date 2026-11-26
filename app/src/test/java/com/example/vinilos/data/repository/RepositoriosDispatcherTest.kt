package com.example.vinilos.data.repository

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
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.NewComment
import com.example.vinilos.data.remote.AlbumApi
import com.example.vinilos.data.remote.AlbumDto
import com.example.vinilos.data.remote.ArtistApi
import com.example.vinilos.data.remote.ArtistDetailDto
import com.example.vinilos.data.remote.BandDto
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
import com.example.vinilos.data.remote.CommentApi
import com.example.vinilos.data.remote.CommentDto
import com.example.vinilos.data.remote.NewCommentDto
import com.example.vinilos.data.remote.CreateAlbumDto
import com.example.vinilos.data.remote.IdAlbumDto
import com.example.vinilos.data.remote.MusicianDto
import com.example.vinilos.data.remote.PerformerDto
import com.example.vinilos.data.remote.PremiacionDto
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * Each repository method runs on the dispatcher it receives (in the app, `Dispatchers.Default`),
 * not on the caller's, which in ViewModels is the main thread.
 *
 * The fake APIs and DAO record the thread they're called on and respond without suspending, so the
 * conversion that follows each response runs on that same thread: if the call happens on the
 * dispatcher's thread, the conversion does too.
 */
class RepositoriosDispatcherTest {

    /** Runs everything on a thread with its own name. */
    private class DispatcherDePrueba : CoroutineDispatcher() {
        private val hilo = Executors.newSingleThreadExecutor { Thread(it, NOMBRE_HILO) }

        override fun dispatch(context: CoroutineContext, block: Runnable) = hilo.execute(block)

        fun cerrar() = hilo.shutdown()
    }

    private val hilos: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private fun anotarHilo() {
        hilos += hiloActual()
    }

    /** In debug mode, kotlinx appends " @coroutine#N" to the thread name. */
    private fun hiloActual() = Thread.currentThread().name.substringBefore(" @")

    private val dispatcher = DispatcherDePrueba()
    private val adaptador = ServiceAdapter("http://localhost/")

    @After
    fun terminar() = dispatcher.cerrar()

    /** At least one call to the API or DAO, and all of them on the dispatcher's thread. */
    private fun assertTodoEnElDispatcher() {
        assertTrue("La prueba debe llegar a la API o al DAO", hilos.isNotEmpty())
        assertEquals(listOf(NOMBRE_HILO), hilos.distinct())
        assertNotEquals("Quien llama no corre en el dispatcher", NOMBRE_HILO, hiloActual())
    }

    // ---------- Fakes ----------

    private val albumDto = AlbumDto(
        id = 100, name = "Buscando América", cover = null, description = "Salsa urbana", genre = "Salsa",
        recordLabel = "Elektra", releaseDate = "1984-08-01T00:00:00.000Z", tracks = emptyList(),
        performers = emptyList(), comments = emptyList()
    )

    private val albumApi = object : AlbumApi {
        override suspend fun getAlbums(): Response<List<AlbumDto>> = Response.success(listOf(albumDto)).also { anotarHilo() }
        override suspend fun getAlbum(id: Int): Response<AlbumDto> = Response.success(albumDto).also { anotarHilo() }
        override suspend fun createAlbum(album: CreateAlbumDto): AlbumDto = albumDto.also { anotarHilo() }
    }

    private val albumDao = object : AlbumDao() {
        override suspend fun todos(): List<AlbumEntity> = emptyList<AlbumEntity>().also { anotarHilo() }
        override suspend fun borrarTodos() = anotarHilo()
        override suspend fun insertar(albumes: List<AlbumEntity>) = anotarHilo()
        override suspend fun detalle(id: Int): AlbumDetailEntity? = null.also { anotarHilo() }
        override suspend fun saveDetail(detalle: AlbumDetailEntity) = anotarHilo()
        override suspend fun borrarDetalles() = anotarHilo()
        override suspend fun insertarDetalles(detalles: List<AlbumDetailEntity>) = anotarHilo()
        override suspend fun borrarDelCatalogo(id: Int) = anotarHilo()
        override suspend fun borrarDetalle(id: Int) = anotarHilo()
        override suspend fun saveAlbums(albumes: List<AlbumEntity>) = anotarHilo()
        override suspend fun saveDetails(detalles: List<AlbumDetailEntity>) = anotarHilo()
        override suspend fun borrarAlbumesPorId(ids: List<Int>) = anotarHilo()
        override suspend fun borrarDetallesPorId(ids: List<Int>) = anotarHilo()
    }

    private val detalleDto = ArtistDetailDto(
        id = 1, name = "Queen", image = null, description = null, birthDate = null,
        creationDate = "1970-01-01T00:00:00.000Z", albums = emptyList(), musicians = emptyList(),
        performerPrizes = emptyList()
    )

    private val artistApi = object : ArtistApi {
        override suspend fun getMusicians(): Response<List<MusicianDto>> =
            Response.success(listOf(MusicianDto(2, "Rubén Blades", null))).also { anotarHilo() }
        override suspend fun getBands(): Response<List<BandDto>> = Response.success(listOf(BandDto(1, "Queen", null))).also { anotarHilo() }
        override suspend fun getMusician(id: Int): Response<ArtistDetailDto> = Response.success(detalleDto).also { anotarHilo() }
        override suspend fun getBand(id: Int): Response<ArtistDetailDto> = Response.success(detalleDto).also { anotarHilo() }
        override suspend fun getPerformerPrizes(): Response<List<PremiacionDto>> = Response.success(emptyList<PremiacionDto>()).also { anotarHilo() }
        override suspend fun associateAlbumWithPerformer(id: Int, albumId: Int): Response<Unit> = Response.success(Unit).also { anotarHilo() }
        override suspend fun reemplazarAlbumesDeBanda(id: Int, albumes: List<IdAlbumDto>): Response<Unit> =
            Response.success(Unit).also { anotarHilo() }
    }

    private val collectorApi = object : CollectorApi {
        override suspend fun getCollectors(): Response<List<CollectorDto>> =
            Response.success(listOf(CollectorDto(100, "Manolo Bellon", "manollo@caracol.com.co"))).also { anotarHilo() }
        override suspend fun getFavoritePerformers(collectorId: Int): Response<List<PerformerDto>> =
            Response.success(listOf(PerformerDto(1))).also { anotarHilo() }
        override suspend fun addFavoriteBand(collectorId: Int, bandId: Int): PerformerDto =
            PerformerDto(bandId).also { anotarHilo() }
        override suspend fun addFavoriteMusician(collectorId: Int, musicianId: Int): PerformerDto =
            PerformerDto(musicianId).also { anotarHilo() }
    }

    private val coleccionistaDao = object : CollectorDao() {
        override suspend fun todos(): List<CollectorEntity> = emptyList<CollectorEntity>().also { anotarHilo() }
        override suspend fun borrarTodos() = anotarHilo()
        override suspend fun insertar(coleccionistas: List<CollectorEntity>) = anotarHilo()
        override suspend fun save(coleccionistas: List<CollectorEntity>) = anotarHilo()
        override suspend fun borrarPorId(ids: List<Int>) = anotarHilo()
    }

    private val artistaDao = object : ArtistDao() {
        override suspend fun todos(): List<ArtistEntity> = emptyList<ArtistEntity>().also { anotarHilo() }
        override suspend fun borrarTodos() = anotarHilo()
        override suspend fun insertar(artistas: List<ArtistEntity>) = anotarHilo()
        override suspend fun detalle(tipo: ArtistType, id: Int): ArtistDetailEntity? = null.also { anotarHilo() }
        override suspend fun saveDetail(detalle: ArtistDetailEntity) = anotarHilo()
        override suspend fun borrarDetalle(tipo: ArtistType, id: Int) = anotarHilo()
        override suspend fun saveArtists(artistas: List<ArtistEntity>) = anotarHilo()
        override suspend fun borrarArtistas(tipo: ArtistType, ids: List<Int>) = anotarHilo()
    }

    private val favoritoDao = object : FavoriteDao() {
        override suspend fun existe(coleccionistaId: Int, artistaId: Int): Boolean = false.also { anotarHilo() }
        override suspend fun deColeccionista(coleccionistaId: Int): List<Int> = emptyList<Int>().also { anotarHilo() }
        override suspend fun borrarDe(coleccionistaId: Int) = anotarHilo()
        override suspend fun insertar(favoritos: List<FavoriteEntity>) = anotarHilo()
        override suspend fun borrarDeColeccionista(coleccionistaId: Int, artistaIds: List<Int>) = anotarHilo()
    }

    private fun albumes() = AlbumRepositoryImpl(adaptador, albumApi, albumDao, dispatcher)
    private fun artistas() = ArtistRepositoryImpl(adaptador, artistApi, artistaDao, dispatcher)
    private fun coleccionistas() = CollectorRepositoryImpl(adaptador, collectorApi, coleccionistaDao, dispatcher)
    private fun favoritos() = FavoriteRepositoryImpl(adaptador, collectorApi, favoritoDao, dispatcher)

    private val comentarioApi = object : CommentApi {
        override suspend fun postComment(albumId: Int, comentario: NewCommentDto): CommentDto =
            CommentDto(comentario.description, comentario.rating).also { anotarHilo() }
    }

    private fun comentarios() = CommentRepositoryImpl(adaptador, comentarioApi, dispatcher)

    // ---------- Albums ----------

    @Test
    fun getAlbumCorreEnElDispatcher() = runTest {
        assertTrue(albumes().getAlbum(100).isSuccess)
        assertTodoEnElDispatcher()
    }

    @Test
    fun getAlbumsYGuardarlosCorreEnElDispatcher() = runTest {
        assertTrue(albumes().getAlbums().isSuccess)
        assertEquals("Consulta y reemplazo del catálogo y de los detalles guardados", 5, hilos.size)
        assertTodoEnElDispatcher()
    }

    @Test
    fun getCachedAlbumsCorreEnElDispatcher() = runTest {
        albumes().getCachedAlbums()
        assertTodoEnElDispatcher()
    }

    @Test
    fun createAlbumCorreEnElDispatcher() = runTest {
        val nuevo = NewAlbum("Buscando América", "https://example.com/p.jpg", 0L, "Salsa urbana", "Salsa", "Elektra")
        assertTrue(albumes().createAlbum(nuevo).isSuccess)
        assertTodoEnElDispatcher()
    }

    // ---------- Artists ----------

    @Test
    fun getArtistsCorreEnElDispatcher() = runTest {
        assertEquals(listOf("Queen", "Rubén Blades"), artistas().getArtists().getOrThrow().map { it.name })
        assertTodoEnElDispatcher()
    }

    @Test
    fun associateAlbumCorreEnElDispatcher() = runTest {
        assertTrue(artistas().associateAlbum(ArtistType.MUSICO, 2, 100).isSuccess)
        assertTrue(artistas().associateAlbum(ArtistType.BANDA, 1, 100).isSuccess)
        assertTodoEnElDispatcher()
    }

    @Test
    fun getDetailCorreEnElDispatcher() = runTest {
        assertTrue(artistas().getDetail(ArtistType.BANDA, 1).isSuccess)
        assertTrue(artistas().getDetail(ArtistType.MUSICO, 1).isSuccess)
        assertTodoEnElDispatcher()
    }

    // ---------- Collectors and favorites ----------

    @Test
    fun getCollectorsCorreEnElDispatcher() = runTest {
        assertTrue(coleccionistas().getCollectors().isSuccess)
        assertTodoEnElDispatcher()
    }

    @Test
    fun esFavoritoCorreEnElDispatcher() = runTest {
        assertTrue(favoritos().esFavorito(100, 1).getOrThrow())
        assertTodoEnElDispatcher()
    }

    @Test
    fun addFavoriteCorreEnElDispatcher() = runTest {
        assertTrue(favoritos().addFavorite(100, 1, ArtistType.BANDA).isSuccess)
        assertTrue(favoritos().addFavorite(100, 2, ArtistType.MUSICO).isSuccess)
        assertTodoEnElDispatcher()
    }

    @Test
    fun postCommentCorreEnElDispatcher() = runTest {
        assertTrue(comentarios().postComment(100, NewComment(101, 5, "Gran prensado")).isSuccess)
        assertTodoEnElDispatcher()
    }

    // ---------- Reading saved data (add-cache-consultas) ----------

    @Test
    fun getCachedAlbumCorreEnElDispatcher() = runTest {
        albumes().getCachedAlbum(100)
        assertTodoEnElDispatcher()
    }

    @Test
    fun getCachedCollectorsCorreEnElDispatcher() = runTest {
        coleccionistas().getCachedCollectors()
        assertTodoEnElDispatcher()
    }

    @Test
    fun getCachedArtistsCorreEnElDispatcher() = runTest {
        artistas().getCachedArtists()
        assertTodoEnElDispatcher()
    }

    @Test
    fun getCachedDetailCorreEnElDispatcher() = runTest {
        artistas().getCachedDetail(ArtistType.BANDA, 1)
        assertTodoEnElDispatcher()
    }

    @Test
    fun esFavoritoGuardadoCorreEnElDispatcher() = runTest {
        favoritos().esFavoritoGuardado(100, 1)
        assertTodoEnElDispatcher()
    }

    private companion object {
        const val NOMBRE_HILO = "datos-de-prueba"
    }
}
