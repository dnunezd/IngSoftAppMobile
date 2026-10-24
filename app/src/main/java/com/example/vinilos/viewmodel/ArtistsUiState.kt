package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Artist

sealed interface ArtistsUiState {

    data object Cargando : ArtistsUiState

    /**
     * [artistas] es el listado completo ya ordenado; [visibles] es el que resulta de aplicar la
     * búsqueda [consulta] por nombre (sin distinguir mayúsculas ni tildes).
     */
    data class Exito(
        val artistas: List<Artist>,
        val consulta: String = ""
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
