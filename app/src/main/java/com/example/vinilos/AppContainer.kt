package com.example.vinilos

import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.ArtistaDao
import com.example.vinilos.data.local.ColeccionistaDao
import com.example.vinilos.data.local.FavoritoDao
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumRepositoryImpl
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.ArtistRepositoryImpl
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.data.repository.ComentarioRepository
import com.example.vinilos.data.repository.ComentarioRepositoryImpl
import com.example.vinilos.data.repository.FavoriteRepository
import com.example.vinilos.data.repository.FavoriteRepositoryImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File

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
    comentariosRepositorio: ComentarioRepository? = null,
    /** Caché del catálogo y del detalle de álbum. Sin él, solo se usa la red, como antes. */
    albumDao: AlbumDao? = null,
    /** Caché del selector de coleccionistas (add-cache-consultas). */
    coleccionistaDao: ColeccionistaDao? = null,
    /** Caché del listado y del detalle de artistas (add-cache-consultas). */
    artistaDao: ArtistaDao? = null,
    /** Caché de los favoritos confirmados de cada coleccionista (add-cache-consultas). */
    favoritoDao: FavoritoDao? = null,
    /** Dónde convierten y ordenan los repositorios, fuera del hilo principal. Retrofit y Room usan sus hilos. */
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    /**
     * Carpeta de la caché HTTP de la API (optimizar-cache-e-imagenes, D2). Sin ella, el cliente no guarda
     * respuestas, que es lo que usan las pruebas.
     */
    carpetaCacheHttp: File? = null
) {

    val serviceAdapter: ServiceAdapter = ServiceAdapter(baseUrl, ServiceAdapter.clientePorDefecto(carpetaCacheHttp))

    /** Se puede reemplazar con `colectoresRepositorio` en las pruebas instrumentadas. */
    val collectorRepository: CollectorRepository =
        colectoresRepositorio ?: CollectorRepositoryImpl(serviceAdapter, coleccionistaDao = coleccionistaDao, dispatcher = dispatcher)

    /** Se puede reemplazar con `albumesRepositorio` en las pruebas instrumentadas. */
    val albumRepository: AlbumRepository =
        albumesRepositorio ?: AlbumRepositoryImpl(serviceAdapter, albumDao = albumDao, dispatcher = dispatcher)

    /** Se puede reemplazar con `artistasRepositorio` en las pruebas instrumentadas. */
    val artistRepository: ArtistRepository =
        artistasRepositorio ?: ArtistRepositoryImpl(serviceAdapter, artistaDao = artistaDao, dispatcher = dispatcher)

    /**
     * Se puede reemplazar con `favoritosRepositorio` en las pruebas instrumentadas (p. ej. con
     * un `MockWebServer`). Antes se creaba de forma autocontenida en `VinilosNavHost` con su
     * propio [ServiceAdapter], lo que ignoraba estos reemplazos; ver revisión de PR de HU10
     * (@lorennossauniandes).
     */
    val favoriteRepository: FavoriteRepository =
        favoritosRepositorio ?: FavoriteRepositoryImpl(serviceAdapter, favoritoDao = favoritoDao, dispatcher = dispatcher)

    /** Comentarios de los álbumes (HU09). Se puede reemplazar con `comentariosRepositorio` en las pruebas. */
    val comentarioRepository: ComentarioRepository =
        comentariosRepositorio ?: ComentarioRepositoryImpl(serviceAdapter, dispatcher = dispatcher)
}
