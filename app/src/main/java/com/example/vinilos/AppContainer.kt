package com.example.vinilos

import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumRepositoryImpl
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.ArtistRepositoryImpl
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.data.repository.FavoriteRepository
import com.example.vinilos.data.repository.FavoriteRepositoryImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Contenedor de dependencias manual: crea una sola vez el [ServiceAdapter] y los repositorios
 * que usan los ViewModel.
 */
class AppContainer(
    baseUrl: String = BuildConfig.BASE_URL,
    colectoresRepositorio: CollectorRepository? = null,
    artistasRepositorio: ArtistRepository? = null,
    albumesRepositorio: AlbumRepository? = null,
    favoritosRepositorio: FavoriteRepository? = null,
    /** Caché del catálogo (add-cache-catalogo). Sin él, el catálogo solo usa la red, como antes. */
    albumDao: AlbumDao? = null,
    /** Dónde corren los repositorios (red, conversión y caché): fuera del hilo principal. */
    dispatcherIO: CoroutineDispatcher = Dispatchers.IO
) {

    val serviceAdapter: ServiceAdapter = ServiceAdapter(baseUrl)

    /** Se puede reemplazar con [colectoresRepositorio] en las pruebas instrumentadas. */
    val collectorRepository: CollectorRepository =
        colectoresRepositorio ?: CollectorRepositoryImpl(serviceAdapter, dispatcher = dispatcherIO)

    /** Se puede reemplazar con [albumesRepositorio] en las pruebas instrumentadas. */
    val albumRepository: AlbumRepository =
        albumesRepositorio ?: AlbumRepositoryImpl(serviceAdapter, albumDao = albumDao, dispatcher = dispatcherIO)

    /** Se puede reemplazar con [artistasRepositorio] en las pruebas instrumentadas. */
    val artistRepository: ArtistRepository =
        artistasRepositorio ?: ArtistRepositoryImpl(serviceAdapter, dispatcherIO)

    /**
     * Se puede reemplazar con [favoritosRepositorio] en las pruebas instrumentadas (p. ej. con
     * un `MockWebServer`). Antes se creaba de forma autocontenida en `VinilosNavHost` con su
     * propio [ServiceAdapter], lo que ignoraba estos reemplazos; ver revisión de PR de HU10
     * (@lorennossauniandes).
     */
    val favoriteRepository: FavoriteRepository =
        favoritosRepositorio ?: FavoriteRepositoryImpl(serviceAdapter, dispatcher = dispatcherIO)
}
