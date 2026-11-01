package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumsViewModel(private val repositorio: AlbumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumsUiState>(AlbumsUiState.Cargando)
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    private var carga: Job? = null
    private var consultaVigente = 0L

    init {
        cargar()
    }

    fun reintentar() {
        if (_uiState.value == AlbumsUiState.Error) cargar()
    }

    /**
     * Deslizar para actualizar: solo con el catálogo ya cargado (lista o vacío). La lista sigue a la
     * vista mientras se consulta y, si la consulta falla, se conserva y se activa el aviso.
     */
    fun actualizar() {
        val anterior = _uiState.value
        _uiState.value = anterior.conIndicadores(actualizando = true, errorAlActualizar = false) ?: return
        consultar(alFallar = anterior.conIndicadores(actualizando = false, errorAlActualizar = true)!!)
    }

    /** La pantalla ya mostró el aviso de que falló la actualización. */
    fun avisoMostrado() {
        val estado = _uiState.value
        val actualizando = (estado as? AlbumsUiState.Exito)?.actualizando
            ?: (estado as? AlbumsUiState.Vacio)?.actualizando ?: return
        _uiState.value = estado.conIndicadores(actualizando, errorAlActualizar = false)!!
    }

    /** Copia de [AlbumsUiState.Exito] o [AlbumsUiState.Vacio] con otros indicadores; null en los demás. */
    private fun AlbumsUiState.conIndicadores(actualizando: Boolean, errorAlActualizar: Boolean) =
        when (this) {
            is AlbumsUiState.Exito -> copy(actualizando = actualizando, errorAlActualizar = errorAlActualizar)
            is AlbumsUiState.Vacio -> copy(actualizando = actualizando, errorAlActualizar = errorAlActualizar)
            else -> null
        }

    private fun cargar() {
        _uiState.value = AlbumsUiState.Cargando
        consultar(alFallar = AlbumsUiState.Error)
    }

    private fun consultar(alFallar: AlbumsUiState) {
        val consulta = ++consultaVigente
        carga?.cancel()
        carga = viewModelScope.launch {
            val resultado = repositorio.obtenerAlbumes()
            if (consulta != consultaVigente) return@launch
            _uiState.value = resultado.fold(
                onSuccess = { albumes ->
                    if (albumes.isEmpty()) AlbumsUiState.Vacio() else AlbumsUiState.Exito(albumes)
                },
                onFailure = {
                    when (it) {
                        is ErrorDeCarga -> alFallar
                        else -> throw it
                    }
                }
            )
        }
    }
}
