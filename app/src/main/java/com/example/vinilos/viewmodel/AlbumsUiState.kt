package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.AlbumSummary

/**
 * Newly created album (HU07). The backend doesn't provide a creation date, so the marker is local and
 * lasts as long as the catalog does. It's kept apart from [AlbumsUiState] because it also exists while loading.
 */
data class NewAlbumCatalogo(
    val id: Int,
    val avisoPendiente: Boolean = true,
    val desplazamientoPendiente: Boolean = true
)

sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState

    /**
     * [albumes] already comes sorted by name. [actualizando] indicates that a pull-to-refresh is
     * in progress without clearing the list; [errorAlActualizar], that the refresh failed and the
     * screen must report it once.
     */
    data class Success(
        val albumes: List<AlbumSummary>,
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false
    ) : AlbumsUiState

    data class Empty(
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false
    ) : AlbumsUiState

    data object Error : AlbumsUiState
}
