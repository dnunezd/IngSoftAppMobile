package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.ArtistNotAvailable
import com.example.vinilos.data.repository.FavoriteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Botón de favorito del detalle de artista (HU10): comprobar y guardar el favorito del coleccionista. */
class FavoriteViewModel(
    private val collectorId: Int,
    private val artistId: Int,
    private val tipo: ArtistType,
    private val repositorio: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavoriteUiState>(FavoriteUiState.Comprobando)
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()

    private var tarea: Job? = null
    private var consultaVigente = 0L

    init {
        comprobar()
    }

    fun retryCheck() {
        if (_uiState.value == FavoriteUiState.ErrorComprobacion) comprobar()
    }

    fun addFavorite() {
        if (_uiState.value != FavoriteUiState.NoFavorito && _uiState.value != FavoriteUiState.ErrorGuardado) return
        val consulta = ++consultaVigente
        tarea?.cancel()
        _uiState.value = FavoriteUiState.Guardando
        tarea = viewModelScope.launch {
            val resultado = repositorio.addFavorite(collectorId, artistId, tipo)
            if (consulta != consultaVigente) return@launch
            _uiState.value = resultado.fold(
                onSuccess = { FavoriteUiState.Favorito(mostrarConfirmacion = true) },
                onFailure = {
                    when (it) {
                        is ArtistNotAvailable -> FavoriteUiState.NotAvailable
                        is LoadError -> FavoriteUiState.ErrorGuardado
                        else -> throw it
                    }
                }
            )
        }
    }

    fun confirmacionMostrada() {
        val estado = _uiState.value
        if (estado is FavoriteUiState.Favorito && estado.mostrarConfirmacion) {
            _uiState.value = estado.copy(mostrarConfirmacion = false)
        }
    }

    private fun comprobar() {
        val consulta = ++consultaVigente
        tarea?.cancel()
        _uiState.value = FavoriteUiState.Comprobando
        tarea = viewModelScope.launch {
            val resultado = repositorio.esFavorito(collectorId, artistId)
            if (consulta != consultaVigente) return@launch
            _uiState.value = resultado.fold(
                onSuccess = { esFavorito ->
                    if (esFavorito) FavoriteUiState.Favorito(mostrarConfirmacion = false)
                    else FavoriteUiState.NoFavorito
                },
                onFailure = {
                    when (it) {
                        // Sin conexión, un favorito ya confirmado se muestra como tal: el botón queda
                        // desactivado igual y no hay riesgo de agregarlo dos veces (add-cache-consultas, D5).
                        is LoadError ->
                            if (repositorio.esFavoritoGuardado(collectorId, artistId)) {
                                FavoriteUiState.Favorito(mostrarConfirmacion = false)
                            } else {
                                FavoriteUiState.ErrorComprobacion
                            }
                        else -> throw it
                    }
                }
            )
        }
    }
}
