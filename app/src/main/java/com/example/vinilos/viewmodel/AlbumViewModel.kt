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

    /** La pantalla ya mostró el aviso de que no se pudo actualizar el detalle guardado. */
    fun avisoMostrado() {
        val estado = _uiState.value
        if (estado is AlbumUiState.Exito && estado.errorAlActualizar) {
            _uiState.value = estado.copy(errorAlActualizar = false)
        }
    }

    init {
        cargar()
    }

    /**
     * Con un detalle guardado, lo muestra de inmediato mientras consulta, y un fallo de la consulta solo
     * activa el aviso. Sin nada guardado, se comporta como antes: `Cargando` y `Error` si falla. Un 404
     * siempre lleva a `NoDisponible` (add-cache-consultas, D5).
     */
    private fun cargar() {
        val consulta = ++consultaVigente
        val id = idActual
        carga?.cancel()
        _uiState.value = AlbumUiState.Cargando
        carga = viewModelScope.launch {
            val guardado = repositorio.obtenerAlbumGuardado(id)
            if (consulta != consultaVigente) return@launch
            if (guardado != null) _uiState.value = AlbumUiState.Exito(guardado, actualizando = true)

            val resultado = repositorio.obtenerAlbum(id)
            if (consulta != consultaVigente) return@launch
            _uiState.value = resultado.fold(
                onSuccess = { AlbumUiState.Exito(it) },
                onFailure = {
                    when (it) {
                        is AlbumNoDisponible -> AlbumUiState.NoDisponible
                        is ErrorDeCarga ->
                            guardado?.let { album -> AlbumUiState.Exito(album, errorAlActualizar = true) }
                                ?: AlbumUiState.Error
                        else -> throw it
                    }
                }
            )
        }
    }
}
