package com.example.vinilos

import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.data.repository.FavoriteRepository

/**
 * Contenedor de dependencias manual: crea una sola vez el [ServiceAdapter] y los repositorios
 * que usan los ViewModel.
 */
class AppContainer(
    baseUrl: String = BuildConfig.BASE_URL,
    colectoresRepositorio: CollectorRepository? = null,
    artistasRepositorio: ArtistRepository? = null,
    albumesRepositorio: AlbumRepository? = null,
    favoritosRepositorio: FavoriteRepository? = null
) {

    val serviceAdapter: ServiceAdapter = ServiceAdapter(baseUrl)

    /** Se puede reemplazar con [colectoresRepositorio] en las pruebas instrumentadas. */
    val collectorRepository: CollectorRepository =
        colectoresRepositorio ?: CollectorRepository(serviceAdapter)

    /** Se puede reemplazar con [albumesRepositorio] en las pruebas instrumentadas. */
    val albumRepository: AlbumRepository =
        albumesRepositorio ?: AlbumRepository(serviceAdapter)

    /** Se puede reemplazar con [artistasRepositorio] en las pruebas instrumentadas. */
    val artistRepository: ArtistRepository =
        artistasRepositorio ?: ArtistRepository(serviceAdapter)

    /**
     * Se puede reemplazar con [favoritosRepositorio] en las pruebas instrumentadas (p. ej. con
     * un `MockWebServer`). Antes se creaba de forma autocontenida en `VinilosNavHost` con su
     * propio [ServiceAdapter], lo que ignoraba estos reemplazos; ver revisión de PR de HU10
     * (@lorennossauniandes).
     */
    val favoriteRepository: FavoriteRepository =
        favoritosRepositorio ?: FavoriteRepository(serviceAdapter)
}
