package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.RecursoNoEncontrado
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

    /** La pantalla ya mostró el aviso de que no se pudo actualizar el detalle guardado. */
    fun avisoMostrado() {
        val estado = _uiState.value
        if (estado is ArtistaDetalleUiState.Exito && estado.errorAlActualizar) {
            _uiState.value = estado.copy(errorAlActualizar = false)
        }
    }

    /**
     * Con un detalle guardado, lo muestra de inmediato mientras consulta, y un fallo de la consulta solo
     * activa el aviso. Sin nada guardado, se comporta como antes. Un 404 ([RecursoNoEncontrado]) lleva a
     * `Error` aunque haya algo guardado: el artista ya no existe (add-cache-consultas, D5).
     */
    private fun cargar() {
        _uiState.value = ArtistaDetalleUiState.Cargando
        viewModelScope.launch {
            val guardado = repositorio.obtenerDetalleGuardado(tipo, id)
            if (guardado != null) _uiState.value = ArtistaDetalleUiState.Exito(guardado, actualizando = true)

            _uiState.value = repositorio.obtenerDetalle(tipo, id).fold(
                onSuccess = { ArtistaDetalleUiState.Exito(it) },
                onFailure = {
                    when {
                        it !is ErrorDeCarga -> throw it
                        guardado == null || it is RecursoNoEncontrado -> ArtistaDetalleUiState.Error
                        else -> ArtistaDetalleUiState.Exito(guardado, errorAlActualizar = true)
                    }
                }
            )
        }
    }
}
