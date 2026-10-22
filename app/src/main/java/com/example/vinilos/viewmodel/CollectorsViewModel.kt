package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.repository.CollectorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollectorsViewModel(private val repositorio: CollectorRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<CollectorsUiState>(CollectorsUiState.Cargando)
    val uiState: StateFlow<CollectorsUiState> = _uiState.asStateFlow()

    private var carga: Job? = null

    init {
        cargar()
    }

    fun reintentar() {
        cargar()
    }

    /** Filtra en el dispositivo; solo tiene efecto cuando el listado ya está cargado. */
    fun buscar(texto: String) {
        _uiState.update { estado ->
            if (estado is CollectorsUiState.Exito) estado.copy(consulta = texto) else estado
        }
    }

    private fun cargar() {
        carga?.cancel()
        _uiState.value = CollectorsUiState.Cargando
        carga = viewModelScope.launch {
            _uiState.value = repositorio.obtenerColeccionistas().fold(
                onSuccess = { lista ->
                    if (lista.isEmpty()) CollectorsUiState.Vacio else CollectorsUiState.Exito(lista)
                },
                onFailure = { CollectorsUiState.Error }
            )
        }
    }
}
