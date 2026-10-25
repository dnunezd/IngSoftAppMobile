package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumNoDisponible
import com.example.vinilos.data.remote.ErrorDeCarga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumViewModel(private val id: Int, private val repositorio: AlbumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Cargando)
    val uiState = _uiState.asStateFlow()

    fun reintentar() {
        if (_uiState.value == AlbumUiState.Error) cargar()
    }

    init {
        cargar()
    }

    private fun cargar() {
        _uiState.value = AlbumUiState.Cargando
        viewModelScope.launch {
            _uiState.value = repositorio.obtenerAlbum(id).fold(
                onSuccess = { AlbumUiState.Exito(it) },
                onFailure = {
                    when (it) {
                        is AlbumNoDisponible -> AlbumUiState.NoDisponible
                        is ErrorDeCarga -> AlbumUiState.Error
                        else -> throw it
                    }
                }
            )
        }
    }
}
