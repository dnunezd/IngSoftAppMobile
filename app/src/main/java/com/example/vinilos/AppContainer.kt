package com.example.vinilos

import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.ArtistDao
import com.example.vinilos.data.local.CollectorDao
import com.example.vinilos.data.local.FavoriteDao
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumRepositoryImpl
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.ArtistRepositoryImpl
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.data.repository.CommentRepository
import com.example.vinilos.data.repository.CommentRepositoryImpl
import com.example.vinilos.data.repository.FavoriteRepository
import com.example.vinilos.data.repository.FavoriteRepositoryImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Manual dependency container: creates the [ServiceAdapter] and the repositories
 * used by the ViewModels a single time.
 */
class AppContainer(
    baseUrl: String = BuildConfig.BASE_URL,
    colectoresRepositorio: CollectorRepository? = null,
    artistasRepositorio: ArtistRepository? = null,
    albumesRepositorio: AlbumRepository? = null,
    favoritosRepositorio: FavoriteRepository? = null,
    comentariosRepositorio: CommentRepository? = null,
    /** Cache for the catalog and the album detail. Without it, only the network is used, as before. */
    albumDao: AlbumDao? = null,
    /** Cache for the collector selector (add-cache-consultas). */
    coleccionistaDao: CollectorDao? = null,
    /** Cache for the artist listing and detail (add-cache-consultas). */
    artistaDao: ArtistDao? = null,
    /** Cache for each collector's confirmed favorites (add-cache-consultas). */
    favoritoDao: FavoriteDao? = null,
    /** Where the repositories convert and sort data, off the main thread. Retrofit and Room use their own threads. */
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    /**
     * Folder for the API's HTTP cache (optimizar-cache-e-imagenes, D2). Without it, the client doesn't store
     * responses, which is what the tests use.
     */
    carpetaCacheHttp: File? = null
) {

    val serviceAdapter: ServiceAdapter = ServiceAdapter(baseUrl, ServiceAdapter.clientePorDefecto(carpetaCacheHttp))

    /** Can be replaced with `colectoresRepositorio` in instrumented tests. */
    val collectorRepository: CollectorRepository =
        colectoresRepositorio ?: CollectorRepositoryImpl(serviceAdapter, coleccionistaDao = coleccionistaDao, dispatcher = dispatcher)

    /** Can be replaced with `albumesRepositorio` in instrumented tests. */
    val albumRepository: AlbumRepository =
        albumesRepositorio ?: AlbumRepositoryImpl(serviceAdapter, albumDao = albumDao, dispatcher = dispatcher)

    /** Can be replaced with `artistasRepositorio` in instrumented tests. */
    val artistRepository: ArtistRepository =
        artistasRepositorio ?: ArtistRepositoryImpl(serviceAdapter, artistaDao = artistaDao, dispatcher = dispatcher)

    /**
     * Can be replaced with `favoritosRepositorio` in instrumented tests (e.g. with
     * a `MockWebServer`). It used to be created self-contained in `VinilosNavHost` with its
     * own [ServiceAdapter], which ignored these replacements; see the HU10 PR review
     * (@lorennossauniandes).
     */
    val favoriteRepository: FavoriteRepository =
        favoritosRepositorio ?: FavoriteRepositoryImpl(serviceAdapter, favoritoDao = favoritoDao, dispatcher = dispatcher)

    /** Album comments (HU09). Can be replaced with `comentariosRepositorio` in tests. */
    val comentarioRepository: CommentRepository =
        comentariosRepositorio ?: CommentRepositoryImpl(serviceAdapter, dispatcher = dispatcher)
}
