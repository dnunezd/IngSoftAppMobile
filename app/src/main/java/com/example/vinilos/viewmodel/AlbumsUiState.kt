package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.AlbumResumen

sealed interface AlbumsUiState {
    data object Cargando : AlbumsUiState

    /**
     * [albumes] ya viene ordenado por nombre. [actualizando] indica que se está deslizando para
     * actualizar sin quitar la lista; [errorAlActualizar], que esa actualización falló y la
     * pantalla debe avisarlo una vez.
     */
    data class Exito(
        val albumes: List<AlbumResumen>,
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false
    ) : AlbumsUiState

    data class Vacio(
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false
    ) : AlbumsUiState

    data object Error : AlbumsUiState
}
