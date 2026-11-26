package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sheet to add an album to an artist (HU15). It lives in the artist detail screen's navigation
 * entry, so the open sheet, the search, and the selection survive rotation (D2).
 */
class AssociateAlbumViewModel(
    private val tipo: ArtistType,
    private val idArtista: Int,
    private val albumes: AlbumRepository,
    private val artistas: ArtistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssociateAlbumUiState())
    val uiState: StateFlow<AssociateAlbumUiState> = _uiState.asStateFlow()

    private var carga: Job? = null

    /** Opens the sheet from scratch with the albums the artist already has and loads the catalog. */
    fun abrir(asociados: Set<Int>) {
        _uiState.value = AssociateAlbumUiState(abierta = true, asociados = asociados)
        cargar()
    }

    fun search(texto: String) {
        _uiState.update { it.copy(busqueda = texto).sinSeleccionOculta() }
    }

    fun select(id: Int) {
        _uiState.update {
            if (it.asociando || it.yaAsociado(id)) it else it.copy(seleccionado = id, errorAlAsociar = false)
        }
    }

    /** Closes without associating. It can't be canceled while submitting: the backend may have already received it. */
    fun cancelar() {
        if (_uiState.value.asociando) return
        carga?.cancel()
        _uiState.value = AssociateAlbumUiState()
    }

    fun retryLoad() {
        if (_uiState.value.listado == ListadoAsociable.Error) cargar()
    }

    fun associate() {
        val estado = _uiState.value
        if (!estado.puedeAsociar) return
        val idAlbum = estado.seleccionado ?: return
        val album = (estado.listado as? ListadoAsociable.Listo)?.albumes?.firstOrNull { it.id == idAlbum } ?: return
        _uiState.value = estado.copy(asociando = true, errorAlAsociar = false)
        viewModelScope.launch {
            artistas.associateAlbum(tipo, idArtista, idAlbum).fold(
                onSuccess = {
                    carga?.cancel()
                    _uiState.value = AssociateAlbumUiState(albumAsociado = album)
                },
                onFailure = {
                    if (it !is LoadError) throw it
                    _uiState.update { actual -> actual.copy(asociando = false, errorAlAsociar = true) }
                }
            )
        }
    }

    fun asociadoNotificado() {
        _uiState.update { it.copy(albumAsociado = null) }
    }

    /**
     * Same approach as the catalog (D3): what's saved is shown immediately; if the query fails, it's
     * kept, and with nothing saved the error is shown with "Retry".
     */
    private fun cargar() {
        carga?.cancel()
        _uiState.update { it.copy(listado = ListadoAsociable.Loading) }
        carga = viewModelScope.launch {
            val guardados = albumes.getCachedAlbums()
            if (guardados.isNotEmpty()) _uiState.update { it.copy(listado = ListadoAsociable.Listo(guardados)) }
            albumes.getAlbums().fold(
                onSuccess = { lista ->
                    _uiState.update { it.copy(listado = ListadoAsociable.Listo(lista)).sinSeleccionOculta() }
                },
                onFailure = {
                    if (it !is LoadError) throw it
                    if (guardados.isEmpty()) _uiState.update { estado -> estado.copy(listado = ListadoAsociable.Error) }
                }
            )
        }
    }
}
