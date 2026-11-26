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

    private val _uiState = MutableStateFlow<CollectorsUiState>(CollectorsUiState.Loading)
    val uiState: StateFlow<CollectorsUiState> = _uiState.asStateFlow()

    private var carga: Job? = null
    private var consultaVigente = 0L

    init {
        cargar()
    }

    fun retry() {
        cargar()
    }

    /** Filtra en el dispositivo; solo tiene efecto cuando el listado ya está cargado. */
    fun search(texto: String) {
        _uiState.update { estado ->
            if (estado is CollectorsUiState.Success) estado.copy(consulta = texto) else estado
        }
    }

    /** La pantalla ya mostró el aviso de que no se pudo actualizar el listado guardado. */
    fun avisoMostrado() {
        _uiState.update { estado ->
            if (estado is CollectorsUiState.Success) estado.copy(errorAlActualizar = false) else estado
        }
    }

    /**
     * Con un listado guardado, lo muestra de inmediato mientras consulta, y un fallo de la consulta solo
     * activa el aviso. Sin nada guardado, se comporta como antes: `Cargando` y `Error` si falla. La
     * búsqueda escrita mientras tanto se conserva (add-cache-consultas, D5).
     */
    private fun cargar() {
        val consulta = ++consultaVigente
        carga?.cancel()
        _uiState.value = CollectorsUiState.Loading
        carga = viewModelScope.launch {
            val guardados = repositorio.getCachedCollectors()
            if (consulta != consultaVigente) return@launch
            if (guardados.isNotEmpty()) _uiState.value = CollectorsUiState.Success(guardados, actualizando = true)

            val resultado = repositorio.getCollectors()
            if (consulta != consultaVigente) return@launch
            val busqueda = (_uiState.value as? CollectorsUiState.Success)?.consulta.orEmpty()
            _uiState.value = resultado.fold(
                onSuccess = { lista ->
                    if (lista.isEmpty()) CollectorsUiState.Empty else CollectorsUiState.Success(lista, busqueda)
                },
                onFailure = {
                    if (guardados.isEmpty()) {
                        CollectorsUiState.Error
                    } else {
                        CollectorsUiState.Success(guardados, busqueda, errorAlActualizar = true)
                    }
                }
            )
        }
    }
}
