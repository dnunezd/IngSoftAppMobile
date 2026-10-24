package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Collector

sealed interface CollectorsUiState {

    data object Cargando : CollectorsUiState

    /**
     * [coleccionistas] es la lista completa del backend; [visibles] es la que resulta de
     * aplicar la búsqueda [consulta] (nombre o correo, sin distinguir mayúsculas ni tildes).
     */
    data class Exito(
        val coleccionistas: List<Collector>,
        val consulta: String = ""
    ) : CollectorsUiState {
        val visibles: List<Collector> = filtrar(coleccionistas, consulta)
    }

    data object Vacio : CollectorsUiState

    data object Error : CollectorsUiState
}

private fun filtrar(coleccionistas: List<Collector>, consulta: String): List<Collector> {
    val buscado = normalizar(consulta.trim())
    if (buscado.isEmpty()) return coleccionistas
    return coleccionistas.filter {
        buscado in normalizar(it.name) || buscado in normalizar(it.email)
    }
}
