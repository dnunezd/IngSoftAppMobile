package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumNotAvailable
import com.example.vinilos.data.remote.LoadError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumViewModel(id: Int, private val repositorio: AlbumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var idActual = id
    private var carga: Job? = null
    private var consultaVigente = 0L

    /**
     * Owner of the "Track added" notice: it lives in the ViewModel (survives a rotation) instead of
     * depending on the navigation key, which is cleared as soon as [actualizar] fires (see [cancionAgregada]).
     * This way, if the Activity is recreated while the notice is on screen, the GET /albums/{id} isn't repeated.
     */
    private val _avisoTrackAgregada = MutableStateFlow(false)
    val avisoTrackAgregada = _avisoTrackAgregada.asStateFlow()

    /** Called only once upon returning from the add-track form with success. */
    fun cancionAgregada() {
        _avisoTrackAgregada.value = true
        actualizar()
    }

    /** The screen already showed the "Track added" notice. */
    fun avisoTrackAgregadaMostrado() {
        _avisoTrackAgregada.value = false
    }

    fun cargarAlbum(id: Int) {
        idActual = id
        cargar()
    }

    fun retry() {
        if (_uiState.value == AlbumUiState.Error) cargar()
    }

    /** The screen already showed the notice that the saved detail couldn't be refreshed. */
    fun avisoMostrado() {
        val estado = _uiState.value
        if (estado is AlbumUiState.Success && estado.errorAlActualizar) {
            _uiState.value = estado.copy(errorAlActualizar = false)
        }
    }

    /**
     * Re-queries the album while keeping the one already on screen visible, for example after
     * adding a track to it or commenting on it (add-comentar-album, D7). With no album on screen, it loads as at
     * the start.
     */
    fun actualizar() {
        cargar(visible = (_uiState.value as? AlbumUiState.Success)?.album)
    }

    init {
        cargar()
    }

    /**
     * With a saved detail, it's shown immediately while querying, and a query failure only
     * triggers the notice. With nothing saved, it behaves as before: `Loading` and `Error` if it fails. A 404
     * always leads to `NotAvailable` (add-cache-consultas, D5).
     */
    private fun cargar(visible: Album? = null) {
        val consulta = ++consultaVigente
        val id = idActual
        carga?.cancel()
        _uiState.value = visible?.let { AlbumUiState.Success(it, actualizando = true) } ?: AlbumUiState.Loading
        carga = viewModelScope.launch {
            // With an album already on screen, it stands in for the saved one.
            val guardado = visible ?: repositorio.getCachedAlbum(id)
            if (consulta != consultaVigente) return@launch
            if (visible == null && guardado != null) _uiState.value = AlbumUiState.Success(guardado, actualizando = true)

            val resultado = repositorio.getAlbum(id)
            if (consulta != consultaVigente) return@launch
            _uiState.value = resultado.fold(
                onSuccess = { AlbumUiState.Success(it) },
                onFailure = {
                    when (it) {
                        is AlbumNotAvailable -> AlbumUiState.NotAvailable
                        is LoadError ->
                            guardado?.let { album -> AlbumUiState.Success(album, errorAlActualizar = true) }
                                ?: AlbumUiState.Error
                        else -> throw it
                    }
                }
            )
        }
    }
}
