package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Artist

sealed interface ArtistsUiState {

    data object Loading : ArtistsUiState

    /**
     * [artistas] is the full list, already sorted; [visibles] is the result of applying the
     * [consulta] search by name (case- and accent-insensitive).
     * [actualizando]: the cached list is shown while the backend is queried.
     * [errorAlActualizar]: that query failed and the screen must report it once (add-cache-consultas, D5).
     */
    data class Success(
        val artistas: List<Artist>,
        val consulta: String = "",
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false
    ) : ArtistsUiState {
        val visibles: List<Artist> = filtrar(artistas, consulta)
    }

    data object Empty : ArtistsUiState

    data object Error : ArtistsUiState
}

internal fun filtrar(artistas: List<Artist>, consulta: String): List<Artist> {
    val buscado = normalizar(consulta.trim())
    if (buscado.isEmpty()) return artistas
    return artistas.filter { buscado in normalizar(it.name) }
}
