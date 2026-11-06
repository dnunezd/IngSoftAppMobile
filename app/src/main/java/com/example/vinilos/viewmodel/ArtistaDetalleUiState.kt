package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.ArtistaDetalle

/** Un solo estado de error: el artista que ya no existe y el fallo de carga llevan al mismo mensaje. */
sealed interface ArtistaDetalleUiState {
    data object Cargando : ArtistaDetalleUiState
    data class Exito(val detalle: ArtistaDetalle) : ArtistaDetalleUiState
    data object Error : ArtistaDetalleUiState
}
