package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.remote.ResourceNotFound
import com.example.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Only queried on creation and when retrying from error, so there are never two queries at once. */
class ArtistDetailViewModel(
    private val tipo: ArtistType,
    private val id: Int,
    private val repositorio: ArtistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistDetailUiState>(ArtistDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun retry() {
        if (_uiState.value == ArtistDetailUiState.Error) cargar()
    }

    /** The screen already showed the notice that the saved detail couldn't be refreshed. */
    fun avisoMostrado() {
        val estado = _uiState.value
        if (estado is ArtistDetailUiState.Success && estado.errorAlActualizar) {
            _uiState.value = estado.copy(errorAlActualizar = false)
        }
    }

    /**
     * An album was just associated with the artist (HU15, D5): it's added to the discography immediately and the
     * detail is re-queried, which also refreshes what's saved. If that query fails, the added album is
     * kept without reporting an error: the association did happen.
     */
    fun albumAsociado(album: AlbumSummary) {
        val estado = _uiState.value as? ArtistDetailUiState.Success ?: return
        val detalle = estado.detalle
        // Added at the end: the discography order is set by the query that follows, using the same criteria as the
        // repository (reviewed in PR #114).
        val albumes = if (detalle.albumes.any { it.id == album.id }) {
            detalle.albumes
        } else {
            detalle.albumes + AlbumArtista(album.id, album.nombre, album.portada, album.anio)
        }
        _uiState.value = estado.copy(
            detalle = detalle.copy(albumes = albumes),
            actualizando = true,
            mostrarAlbumAsociado = true
        )
        viewModelScope.launch {
            val resultado = repositorio.getDetail(tipo, id)
            val actual = _uiState.value as? ArtistDetailUiState.Success ?: return@launch
            _uiState.value = resultado.fold(
                onSuccess = { actual.copy(detalle = it, actualizando = false) },
                onFailure = {
                    if (it !is LoadError) throw it
                    actual.copy(actualizando = false)
                }
            )
        }
    }

    /** The screen already showed "Album associated". */
    fun albumAsociadoMostrado() {
        val estado = _uiState.value
        if (estado is ArtistDetailUiState.Success && estado.mostrarAlbumAsociado) {
            _uiState.value = estado.copy(mostrarAlbumAsociado = false)
        }
    }

    /**
     * With a saved detail, it's shown immediately while querying, and a query failure only
     * triggers the notice. With nothing saved, it behaves as before. A 404 ([RecursoNoEncontrado]) leads to
     * `Error` even if something is saved: the artist no longer exists (add-cache-consultas, D5).
     */
    private fun cargar() {
        _uiState.value = ArtistDetailUiState.Loading
        viewModelScope.launch {
            val guardado = repositorio.getCachedDetail(tipo, id)
            if (guardado != null) _uiState.value = ArtistDetailUiState.Success(guardado, actualizando = true)

            _uiState.value = repositorio.getDetail(tipo, id).fold(
                onSuccess = { ArtistDetailUiState.Success(it) },
                onFailure = {
                    when {
                        it !is LoadError -> throw it
                        guardado == null || it is ResourceNotFound -> ArtistDetailUiState.Error
                        else -> ArtistDetailUiState.Success(guardado, errorAlActualizar = true)
                    }
                }
            )
        }
    }
}
