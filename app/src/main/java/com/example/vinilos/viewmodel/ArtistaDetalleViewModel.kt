package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Solo se consulta al crearse y al reintentar desde el error, así que nunca hay dos consultas a la vez. */
class ArtistaDetalleViewModel(
    private val tipo: ArtistType,
    private val id: Int,
    private val repositorio: ArtistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistaDetalleUiState>(ArtistaDetalleUiState.Cargando)
    val uiState = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun reintentar() {
        if (_uiState.value == ArtistaDetalleUiState.Error) cargar()
    }

    private fun cargar() {
        _uiState.value = ArtistaDetalleUiState.Cargando
        viewModelScope.launch {
            _uiState.value = repositorio.obtenerDetalle(tipo, id).fold(
                onSuccess = { ArtistaDetalleUiState.Exito(it) },
                onFailure = { if (it is ErrorDeCarga) ArtistaDetalleUiState.Error else throw it }
            )
        }
    }
}
