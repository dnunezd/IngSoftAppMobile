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

/** Solo se consulta al crearse y al reintentar desde el error, así que nunca hay dos consultas a la vez. */
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

    /** La pantalla ya mostró el aviso de que no se pudo actualizar el detalle guardado. */
    fun avisoMostrado() {
        val estado = _uiState.value
        if (estado is ArtistDetailUiState.Success && estado.errorAlActualizar) {
            _uiState.value = estado.copy(errorAlActualizar = false)
        }
    }

    /**
     * Un álbum se acaba de asociar al artista (HU15, D5): se agrega de inmediato a la discografía y se vuelve
     * a consultar el detalle, que también actualiza lo guardado. Si esa consulta falla, se conserva el álbum
     * agregado sin avisar de error: la asociación sí ocurrió.
     */
    fun albumAsociado(album: AlbumSummary) {
        val estado = _uiState.value as? ArtistDetailUiState.Success ?: return
        val detalle = estado.detalle
        // Se agrega al final: el orden de la discografía lo fija la consulta que sigue, con el mismo criterio del
        // repositorio (revisión del PR #114).
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

    /** La pantalla ya mostró "Álbum asociado". */
    fun albumAsociadoMostrado() {
        val estado = _uiState.value
        if (estado is ArtistDetailUiState.Success && estado.mostrarAlbumAsociado) {
            _uiState.value = estado.copy(mostrarAlbumAsociado = false)
        }
    }

    /**
     * Con un detalle guardado, lo muestra de inmediato mientras consulta, y un fallo de la consulta solo
     * activa el aviso. Sin nada guardado, se comporta como antes. Un 404 ([RecursoNoEncontrado]) lleva a
     * `Error` aunque haya algo guardado: el artista ya no existe (add-cache-consultas, D5).
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
