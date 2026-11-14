package com.example.vinilos.data.repository

import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.AlbumDetalleEntity
import com.example.vinilos.data.local.AlbumEntity
import com.example.vinilos.data.local.ArtistaDao
import com.example.vinilos.data.local.ArtistaDetalleEntity
import com.example.vinilos.data.local.ArtistaEntity
import com.example.vinilos.data.local.ColeccionistaDao
import com.example.vinilos.data.local.ColeccionistaEntity
import com.example.vinilos.data.local.FavoritoDao
import com.example.vinilos.data.local.FavoritoEntity
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.AlbumApi
import com.example.vinilos.data.remote.AlbumDto
import com.example.vinilos.data.remote.ArtistApi
import com.example.vinilos.data.remote.ArtistaDetalleDto
import com.example.vinilos.data.remote.BandDto
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
import com.example.vinilos.data.remote.CrearAlbumDto
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
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * Cada método de los repositorios corre en el dispatcher que recibe (en la app, `Dispatchers.Default`),
 * no en el de quien lo llama, que en los ViewModel es el hilo principal.
 *
 * Las APIs y el DAO falsos anotan el hilo en que se les llama y responden sin suspender, así que la
 * conversión que sigue a cada respuesta corre en ese mismo hilo: si la llamada ocurre en el hilo del
 * dispatcher, la conversión también.
 */
class RepositoriosDispatcherTest {

    /** Corre todo en un hilo con nombre propio. */
    private class DispatcherDePrueba : CoroutineDispatcher() {
        private val hilo = Executors.newSingleThreadExecutor { Thread(it, NOMBRE_HILO) }

        override fun dispatch(context: CoroutineContext, block: Runnable) = hilo.execute(block)

        fun cerrar() = hilo.shutdown()
    }

    private val hilos: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private fun anotarHilo() {
        hilos += hiloActual()
    }

    /** En modo depuración, kotlinx agrega " @coroutine#N" al nombre del hilo. */
    private fun hiloActual() = Thread.currentThread().name.substringBefore(" @")

    private val dispatcher = DispatcherDePrueba()
    private val adaptador = ServiceAdapter("http://localhost/")

    @After
    fun terminar() = dispatcher.cerrar()

    /** Al menos una llamada a la API o al DAO, y todas en el hilo del dispatcher. */
    private fun assertTodoEnElDispatcher() {
        assertTrue("La prueba debe llegar a la API o al DAO", hilos.isNotEmpty())
        assertEquals(listOf(NOMBRE_HILO), hilos.distinct())
        assertNotEquals("Quien llama no corre en el dispatcher", NOMBRE_HILO, hiloActual())
    }

    // ---------- Falsos ----------

    private val albumDto = AlbumDto(
        id = 100, name = "Buscando América", cover = null, description = "Salsa urbana", genre = "Salsa",
        recordLabel = "Elektra", releaseDate = "1984-08-01T00:00:00.000Z", tracks = emptyList(),
        performers = emptyList(), comments = emptyList()
    )

    private val albumApi = object : AlbumApi {
        override suspend fun obtenerAlbumes(): List<AlbumDto> = listOf(albumDto).also { anotarHilo() }
        override suspend fun obtenerAlbum(id: Int): AlbumDto = albumDto.also { anotarHilo() }
        override suspend fun crearAlbum(album: CrearAlbumDto): AlbumDto = albumDto.also { anotarHilo() }
    }

    private val albumDao = object : AlbumDao() {
        override suspend fun todos(): List<AlbumEntity> = emptyList<AlbumEntity>().also { anotarHilo() }
        override suspend fun borrarTodos() = anotarHilo()
        override suspend fun insertar(albumes: List<AlbumEntity>) = anotarHilo()
        override suspend fun detalle(id: Int): AlbumDetalleEntity? = null.also { anotarHilo() }
        override suspend fun guardarDetalle(detalle: AlbumDetalleEntity) = anotarHilo()
        override suspend fun borrarDetalles() = anotarHilo()
        override suspend fun insertarDetalles(detalles: List<AlbumDetalleEntity>) = anotarHilo()
        override suspend fun borrarDelCatalogo(id: Int) = anotarHilo()
        override suspend fun borrarDetalle(id: Int) = anotarHilo()
    }

    private val detalleDto = ArtistaDetalleDto(
        id = 1, name = "Queen", image = null, description = null, birthDate = null,
        creationDate = "1970-01-01T00:00:00.000Z", albums = emptyList(), musicians = emptyList(),
        performerPrizes = emptyList()
    )

    private val artistApi = object : ArtistApi {
        override suspend fun getMusicians(): List<MusicianDto> =
            listOf(MusicianDto(2, "Rubén Blades", null)).also { anotarHilo() }
        override suspend fun getBands(): List<BandDto> = listOf(BandDto(1, "Queen", null)).also { anotarHilo() }
        override suspend fun getMusician(id: Int): ArtistaDetalleDto = detalleDto.also { anotarHilo() }
        override suspend fun getBand(id: Int): ArtistaDetalleDto = detalleDto.also { anotarHilo() }
        override suspend fun getPerformerPrizes(): List<PremiacionDto> = emptyList<PremiacionDto>().also { anotarHilo() }
    }

    private val collectorApi = object : CollectorApi {
        override suspend fun getCollectors(): List<CollectorDto> =
            listOf(CollectorDto(100, "Manolo Bellon", "manollo@caracol.com.co")).also { anotarHilo() }
        override suspend fun getFavoritePerformers(collectorId: Int): List<PerformerDto> =
            listOf(PerformerDto(1)).also { anotarHilo() }
        override suspend fun addFavoriteBand(collectorId: Int, bandId: Int): PerformerDto =
            PerformerDto(bandId).also { anotarHilo() }
        override suspend fun addFavoriteMusician(collectorId: Int, musicianId: Int): PerformerDto =
            PerformerDto(musicianId).also { anotarHilo() }
    }

    private val coleccionistaDao = object : ColeccionistaDao() {
        override suspend fun todos(): List<ColeccionistaEntity> = emptyList<ColeccionistaEntity>().also { anotarHilo() }
        override suspend fun borrarTodos() = anotarHilo()
        override suspend fun insertar(coleccionistas: List<ColeccionistaEntity>) = anotarHilo()
    }

    private val artistaDao = object : ArtistaDao() {
        override suspend fun todos(): List<ArtistaEntity> = emptyList<ArtistaEntity>().also { anotarHilo() }
        override suspend fun borrarTodos() = anotarHilo()
        override suspend fun insertar(artistas: List<ArtistaEntity>) = anotarHilo()
        override suspend fun detalle(tipo: ArtistType, id: Int): ArtistaDetalleEntity? = null.also { anotarHilo() }
        override suspend fun guardarDetalle(detalle: ArtistaDetalleEntity) = anotarHilo()
        override suspend fun borrarDetalle(tipo: ArtistType, id: Int) = anotarHilo()
    }

    private val favoritoDao = object : FavoritoDao() {
        override suspend fun existe(coleccionistaId: Int, artistaId: Int): Boolean = false.also { anotarHilo() }
        override suspend fun deColeccionista(coleccionistaId: Int): List<Int> = emptyList<Int>().also { anotarHilo() }
        override suspend fun borrarDe(coleccionistaId: Int) = anotarHilo()
        override suspend fun insertar(favoritos: List<FavoritoEntity>) = anotarHilo()
    }

    private fun albumes() = AlbumRepositoryImpl(adaptador, albumApi, albumDao, dispatcher)
    private fun artistas() = ArtistRepositoryImpl(adaptador, artistApi, artistaDao, dispatcher)
    private fun coleccionistas() = CollectorRepositoryImpl(adaptador, collectorApi, coleccionistaDao, dispatcher)
    private fun favoritos() = FavoriteRepositoryImpl(adaptador, collectorApi, favoritoDao, dispatcher)

    // ---------- Álbumes ----------

    @Test
    fun obtenerAlbumCorreEnElDispatcher() = runTest {
        assertTrue(albumes().obtenerAlbum(100).isSuccess)
        assertTodoEnElDispatcher()
    }

    @Test
    fun obtenerAlbumesYGuardarlosCorreEnElDispatcher() = runTest {
        assertTrue(albumes().obtenerAlbumes().isSuccess)
        assertEquals("Consulta y reemplazo del catálogo y de los detalles guardados", 5, hilos.size)
        assertTodoEnElDispatcher()
    }

    @Test
    fun obtenerAlbumesGuardadosCorreEnElDispatcher() = runTest {
        albumes().obtenerAlbumesGuardados()
        assertTodoEnElDispatcher()
    }

    @Test
    fun crearAlbumCorreEnElDispatcher() = runTest {
        val nuevo = AlbumNuevo("Buscando América", "https://example.com/p.jpg", 0L, "Salsa urbana", "Salsa", "Elektra")
        assertTrue(albumes().crearAlbum(nuevo).isSuccess)
        assertTodoEnElDispatcher()
    }

    // ---------- Artistas ----------

    @Test
    fun obtenerArtistasCorreEnElDispatcher() = runTest {
        assertEquals(listOf("Queen", "Rubén Blades"), artistas().obtenerArtistas().getOrThrow().map { it.name })
        assertTodoEnElDispatcher()
    }

    @Test
    fun obtenerDetalleCorreEnElDispatcher() = runTest {
        assertTrue(artistas().obtenerDetalle(ArtistType.BANDA, 1).isSuccess)
        assertTrue(artistas().obtenerDetalle(ArtistType.MUSICO, 1).isSuccess)
        assertTodoEnElDispatcher()
    }

    // ---------- Coleccionistas y favoritos ----------

    @Test
    fun obtenerColeccionistasCorreEnElDispatcher() = runTest {
        assertTrue(coleccionistas().obtenerColeccionistas().isSuccess)
        assertTodoEnElDispatcher()
    }

    @Test
    fun esFavoritoCorreEnElDispatcher() = runTest {
        assertTrue(favoritos().esFavorito(100, 1).getOrThrow())
        assertTodoEnElDispatcher()
    }

    @Test
    fun agregarFavoritoCorreEnElDispatcher() = runTest {
        assertTrue(favoritos().agregarFavorito(100, 1, ArtistType.BANDA).isSuccess)
        assertTrue(favoritos().agregarFavorito(100, 2, ArtistType.MUSICO).isSuccess)
        assertTodoEnElDispatcher()
    }

    // ---------- Lecturas de lo guardado (add-cache-consultas) ----------

    @Test
    fun obtenerAlbumGuardadoCorreEnElDispatcher() = runTest {
        albumes().obtenerAlbumGuardado(100)
        assertTodoEnElDispatcher()
    }

    @Test
    fun obtenerColeccionistasGuardadosCorreEnElDispatcher() = runTest {
        coleccionistas().obtenerColeccionistasGuardados()
        assertTodoEnElDispatcher()
    }

    @Test
    fun obtenerArtistasGuardadosCorreEnElDispatcher() = runTest {
        artistas().obtenerArtistasGuardados()
        assertTodoEnElDispatcher()
    }

    @Test
    fun obtenerDetalleGuardadoCorreEnElDispatcher() = runTest {
        artistas().obtenerDetalleGuardado(ArtistType.BANDA, 1)
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
