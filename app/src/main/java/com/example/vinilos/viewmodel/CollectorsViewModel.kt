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

    /** Filters on-device; only takes effect once the list is already loaded. */
    fun search(texto: String) {
        _uiState.update { estado ->
            if (estado is CollectorsUiState.Success) estado.copy(consulta = texto) else estado
        }
    }

    /** The screen already showed the notice that the saved list couldn't be refreshed. */
    fun avisoMostrado() {
        _uiState.update { estado ->
            if (estado is CollectorsUiState.Success) estado.copy(errorAlActualizar = false) else estado
        }
    }

    /**
     * With a saved list, it's shown immediately while querying, and a query failure only
     * triggers the notice. With nothing saved, it behaves as before: `Loading` and `Error` if it fails. The
     * search text typed in the meantime is preserved (add-cache-consultas, D5).
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
