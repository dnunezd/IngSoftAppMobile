package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Artist

sealed interface ArtistsUiState {

    data object Cargando : ArtistsUiState

    /**
     * [artistas] es el listado completo ya ordenado; [visibles] es el que resulta de aplicar la
     * búsqueda [consulta] por nombre (sin distinguir mayúsculas ni tildes).
     * [actualizando]: se muestra el listado guardado mientras se consulta el backend.
     * [errorAlActualizar]: esa consulta falló y la pantalla debe avisarlo una vez (add-cache-consultas, D5).
     */
    data class Exito(
        val artistas: List<Artist>,
        val consulta: String = "",
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false
    ) : ArtistsUiState {
        val visibles: List<Artist> = filtrar(artistas, consulta)
    }

    data object Vacio : ArtistsUiState

    data object Error : ArtistsUiState
}

private fun filtrar(artistas: List<Artist>, consulta: String): List<Artist> {
    val buscado = normalizar(consulta.trim())
    if (buscado.isEmpty()) return artistas
    return artistas.filter { buscado in normalizar(it.name) }
}
