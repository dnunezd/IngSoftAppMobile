package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.ArtistDetail

/** A single error state: the artist that no longer exists and the load failure lead to the same message. */
sealed interface ArtistDetailUiState {
    data object Loading : ArtistDetailUiState
    /**
     * [actualizando]: the cached detail is shown while the backend is queried.
     * [errorAlActualizar]: that query failed and the screen must report it once (add-cache-consultas, D5).
     * [mostrarAlbumAsociado]: an album was just associated and the screen must report it once (HU15).
     */
    data class Success(
        val detalle: ArtistDetail,
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false,
        val mostrarAlbumAsociado: Boolean = false
    ) : ArtistDetailUiState
    data object Error : ArtistDetailUiState
}
