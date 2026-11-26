package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.AlbumSummary

/**
 * Álbum recién creado (HU07). El backend no entrega fecha de creación, así que la marca es local y
 * dura lo que dure el catálogo. Va aparte de [AlbumsUiState] porque también existe mientras carga.
 */
data class NewAlbumCatalogo(
    val id: Int,
    val avisoPendiente: Boolean = true,
    val desplazamientoPendiente: Boolean = true
)

sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState

    /**
     * [albumes] ya viene ordenado por nombre. [actualizando] indica que se está deslizando para
     * actualizar sin quitar la lista; [errorAlActualizar], que esa actualización falló y la
     * pantalla debe avisarlo una vez.
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
