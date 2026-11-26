package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.ArtistDetail

/** Un solo estado de error: el artista que ya no existe y el fallo de carga llevan al mismo mensaje. */
sealed interface ArtistDetailUiState {
    data object Loading : ArtistDetailUiState
    /**
     * [actualizando]: se muestra el detalle guardado mientras se consulta el backend.
     * [errorAlActualizar]: esa consulta falló y la pantalla debe avisarlo una vez (add-cache-consultas, D5).
     * [mostrarAlbumAsociado]: se acaba de asociar un álbum y la pantalla debe avisarlo una vez (HU15).
     */
    data class Success(
        val detalle: ArtistDetail,
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false,
        val mostrarAlbumAsociado: Boolean = false
    ) : ArtistDetailUiState
    data object Error : ArtistDetailUiState
}
