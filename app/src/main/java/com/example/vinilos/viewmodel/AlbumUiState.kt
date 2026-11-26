package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Album

sealed interface AlbumUiState {
    data object Loading : AlbumUiState

    /**
     * [actualizando]: se muestra el detalle guardado mientras se consulta el backend.
     * [errorAlActualizar]: esa consulta falló y la pantalla debe avisarlo una vez (add-cache-consultas, D5).
     */
    data class Success(
        val album: Album,
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false
    ) : AlbumUiState

    data object NotAvailable : AlbumUiState
    data object Error : AlbumUiState
}
