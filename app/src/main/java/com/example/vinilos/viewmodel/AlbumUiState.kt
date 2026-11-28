package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Album

sealed interface AlbumUiState {
    data object Loading : AlbumUiState

    /**
     * [actualizando]: the cached detail is shown while the backend is queried.
     * [errorAlActualizar]: that query failed and the screen must report it once (add-cache-consultas, D5).
     */
    data class Success(
        val album: Album,
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false
    ) : AlbumUiState

    data object NotAvailable : AlbumUiState
    data object Error : AlbumUiState
}
