package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Collector

sealed interface CollectorsUiState {

    data object Loading : CollectorsUiState

    /**
     * [coleccionistas] is the full backend list; [visibles] is the result of applying the
     * [consulta] search (name or email, case- and accent-insensitive).
     * [actualizando]: the cached list is shown while the backend is queried.
     * [errorAlActualizar]: that query failed and the screen must report it once (add-cache-consultas, D5).
     */
    data class Success(
        val coleccionistas: List<Collector>,
        val consulta: String = "",
        val actualizando: Boolean = false,
        val errorAlActualizar: Boolean = false
    ) : CollectorsUiState {
        val visibles: List<Collector> = filtrar(coleccionistas, consulta)
    }

    data object Empty : CollectorsUiState

    data object Error : CollectorsUiState
}

private fun filtrar(coleccionistas: List<Collector>, consulta: String): List<Collector> {
    val buscado = normalizar(consulta.trim())
    if (buscado.isEmpty()) return coleccionistas
    return coleccionistas.filter {
        buscado in normalizar(it.name) || buscado in normalizar(it.email)
    }
}
