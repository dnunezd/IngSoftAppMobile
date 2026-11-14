package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.ArtistaDetalle

/** Un solo estado de error: el artista que ya no existe y el fallo de carga llevan al mismo mensaje. */
sealed interface ArtistaDetalleUiState {
    data object Cargando : ArtistaDetalleUiState
    /**
     * [actualizando]: se muestra el detalle guardado mientras se consulta el backend.
     * [errorAlActualizar]: esa consulta falló y la pantalla debe avisarlo una vez (add-cache-consultas, D5).
     */
    data class Exito(
        val detalle: ArtistaDetalle,
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false
    ) : ArtistaDetalleUiState
    data object Error : ArtistaDetalleUiState
}
