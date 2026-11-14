package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.ArtistaNoDisponible
import com.example.vinilos.data.repository.FavoriteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Botón de favorito del detalle de artista (HU10): comprobar y guardar el favorito del coleccionista. */
class FavoritoViewModel(
    private val collectorId: Int,
    private val artistId: Int,
    private val tipo: ArtistType,
    private val repositorio: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavoritoUiState>(FavoritoUiState.Comprobando)
    val uiState: StateFlow<FavoritoUiState> = _uiState.asStateFlow()

    private var tarea: Job? = null
    private var consultaVigente = 0L

    init {
        comprobar()
    }

    fun reintentarComprobacion() {
        if (_uiState.value == FavoritoUiState.ErrorComprobacion) comprobar()
    }

    fun agregarFavorito() {
        if (_uiState.value != FavoritoUiState.NoFavorito && _uiState.value != FavoritoUiState.ErrorGuardado) return
        val consulta = ++consultaVigente
        tarea?.cancel()
        _uiState.value = FavoritoUiState.Guardando
        tarea = viewModelScope.launch {
            val resultado = repositorio.agregarFavorito(collectorId, artistId, tipo)
            if (consulta != consultaVigente) return@launch
            _uiState.value = resultado.fold(
                onSuccess = { FavoritoUiState.Favorito(mostrarConfirmacion = true) },
                onFailure = {
                    when (it) {
                        is ArtistaNoDisponible -> FavoritoUiState.NoDisponible
                        is ErrorDeCarga -> FavoritoUiState.ErrorGuardado
                        else -> throw it
                    }
                }
            )
        }
    }

    fun confirmacionMostrada() {
        val estado = _uiState.value
        if (estado is FavoritoUiState.Favorito && estado.mostrarConfirmacion) {
            _uiState.value = estado.copy(mostrarConfirmacion = false)
        }
    }

    private fun comprobar() {
        val consulta = ++consultaVigente
        tarea?.cancel()
        _uiState.value = FavoritoUiState.Comprobando
        tarea = viewModelScope.launch {
            val resultado = repositorio.esFavorito(collectorId, artistId)
            if (consulta != consultaVigente) return@launch
            _uiState.value = resultado.fold(
                onSuccess = { esFavorito ->
                    if (esFavorito) FavoritoUiState.Favorito(mostrarConfirmacion = false)
                    else FavoritoUiState.NoFavorito
                },
                onFailure = {
                    when (it) {
                        // Sin conexión, un favorito ya confirmado se muestra como tal: el botón queda
                        // desactivado igual y no hay riesgo de agregarlo dos veces (add-cache-consultas, D5).
                        is ErrorDeCarga ->
                            if (repositorio.esFavoritoGuardado(collectorId, artistId)) {
                                FavoritoUiState.Favorito(mostrarConfirmacion = false)
                            } else {
                                FavoritoUiState.ErrorComprobacion
                            }
                        else -> throw it
                    }
                }
            )
        }
    }
}
