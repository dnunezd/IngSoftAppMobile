package com.example.vinilos

import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.CollectorRepository

/**
 * Contenedor de dependencias manual: crea una sola vez el cliente de red y los repositorios
 * que usan los ViewModel.
 */
class AppContainer(
    baseUrl: String = BuildConfig.BASE_URL,
    colectoresRepositorio: CollectorRepository? = null
) {

    val serviceAdapter = ServiceAdapter(baseUrl)

    /** Se puede reemplazar con [colectoresRepositorio] en las pruebas instrumentadas. */
    val collectorRepository: CollectorRepository =
        colectoresRepositorio ?: CollectorRepository(serviceAdapter)

    val albumRepository: AlbumRepository
        get() = error("Integración del repositorio de álbum pendiente")
}
