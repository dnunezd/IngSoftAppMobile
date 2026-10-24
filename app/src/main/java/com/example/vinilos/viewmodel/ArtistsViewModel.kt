package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArtistsViewModel(private val repositorio: ArtistRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistsUiState>(ArtistsUiState.Cargando)
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

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
            if (estado is ArtistsUiState.Exito) estado.copy(consulta = texto) else estado
        }
    }

    private fun cargar() {
        carga?.cancel()
        _uiState.value = ArtistsUiState.Cargando
        carga = viewModelScope.launch {
            _uiState.value = repositorio.obtenerArtistas().fold(
                onSuccess = { lista ->
                    if (lista.isEmpty()) ArtistsUiState.Vacio else ArtistsUiState.Exito(lista)
                },
                onFailure = { ArtistsUiState.Error }
            )
        }
    }
}
