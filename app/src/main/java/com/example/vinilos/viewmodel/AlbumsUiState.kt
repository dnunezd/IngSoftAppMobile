package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.AlbumResumen

/**
 * Álbum recién creado (HU07). El backend no entrega fecha de creación, así que la marca es local y
 * dura lo que dure el catálogo. Va aparte de [AlbumsUiState] porque también existe mientras carga.
 */
data class AlbumNuevoCatalogo(
    val id: Int,
    val avisoPendiente: Boolean = true,
    val desplazamientoPendiente: Boolean = true
)

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
