package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumNoDisponible
import com.example.vinilos.data.remote.ErrorDeCarga
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumViewModel(id: Int, private val repositorio: AlbumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Cargando)
    val uiState = _uiState.asStateFlow()

    private var idActual = id
    private var carga: Job? = null
    private var consultaVigente = 0L

    fun cargarAlbum(id: Int) {
        idActual = id
        cargar()
    }

    fun reintentar() {
        if (_uiState.value == AlbumUiState.Error) cargar()
    }

    init {
        cargar()
    }

    private fun cargar() {
        val consulta = ++consultaVigente
        val id = idActual
        carga?.cancel()
        _uiState.value = AlbumUiState.Cargando
        carga = viewModelScope.launch {
            val resultado = repositorio.obtenerAlbum(id)
            if (consulta != consultaVigente) return@launch
            _uiState.value = resultado.fold(
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
