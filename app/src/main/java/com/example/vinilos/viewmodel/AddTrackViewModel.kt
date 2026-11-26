package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.repository.AlbumNotAvailable
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TrackSubmission {
    Editando,
    Guardando,
    Agregada,
    Fallida,
    AlbumNotAvailable
}

/**
 * Target album data for the form's context card (name, cover, artist).
 * Loaded on a best-effort basis: if the query fails or hasn't responded yet, [AgregarCancionUiState.albumInfo]
 * stays `null` and the form remains fully usable, without affecting [EnvioCancion.AlbumNoDisponible]
 * (which is only triggered by a failure while saving).
 */
data class TargetAlbumInfo(
    val nombre: String,
    val portada: String?,
    val artista: String?
)

data class AddTrackUiState(
    val nombre: String = "",
    val duracion: String = "",
    val errores: Map<TrackField, TrackError> = validarTrack("", ""),
    val envio: TrackSubmission = TrackSubmission.Editando,
    val albumInfo: TargetAlbumInfo? = null
) {
    val guardando: Boolean get() = envio == TrackSubmission.Guardando
    val puedeGuardar: Boolean
        get() = errores.isEmpty() && envio !in setOf(
            TrackSubmission.Guardando,
            TrackSubmission.Agregada,
            TrackSubmission.AlbumNotAvailable
        )
}

class AddTrackViewModel(
    private val albumId: Int,
    private val repositorio: AlbumRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddTrackUiState())
    val uiState = _uiState.asStateFlow()

    init {
        cargarInfoAlbum()
    }

    /**
     * Best-effort load for the "Target album" card from the local cache (Room), without network:
     * the detail screen already queried `GET /albums/{id}` when it opened, so repeating it here
     * would duplicate that query (the same "queries fired twice" problem seen in
     * HU02/HU10). If there's nothing in the cache, the card is simply omitted and the form stays usable; this
     * must not be confused with [EnvioCancion.AlbumNoDisponible], which is only triggered by a failure while saving.
     */
    private fun cargarInfoAlbum() {
        viewModelScope.launch {
            repositorio.getCachedAlbum(albumId)?.let { album ->
                val info = TargetAlbumInfo(
                    nombre = album.nombre,
                    portada = album.portada,
                    artista = album.artistas.firstOrNull()?.nombre
                )
                _uiState.update { it.copy(albumInfo = info) }
            }
        }
    }

    fun cambiarNombre(valor: String) = editar { copy(nombre = valor) }

    fun cambiarDuracion(valor: String) = editar { copy(duracion = valor) }

    private fun editar(cambio: AddTrackUiState.() -> AddTrackUiState) {
        _uiState.update { estado ->
            if (estado.envio == TrackSubmission.Guardando ||
                estado.envio == TrackSubmission.Agregada ||
                estado.envio == TrackSubmission.AlbumNotAvailable
            ) {
                estado
            } else {
                val actualizado = estado.cambio()
                actualizado.copy(
                    errores = validarTrack(actualizado.nombre, actualizado.duracion),
                    // If the "could not add" banner was still visible, editing already invalidates it:
                    // it's only shown again on the next failed save attempt.
                    envio = if (estado.envio == TrackSubmission.Fallida) TrackSubmission.Editando else estado.envio
                )
            }
        }
    }

    fun save() {
        val estado = _uiState.value
        if (estado.envio == TrackSubmission.Guardando ||
            estado.envio == TrackSubmission.Agregada ||
            estado.envio == TrackSubmission.AlbumNotAvailable
        ) return

        val errores = validarTrack(estado.nombre, estado.duracion)
        if (errores.isNotEmpty()) {
            _uiState.value = estado.copy(errores = errores, envio = TrackSubmission.Editando)
            return
        }

        _uiState.value = estado.copy(errores = emptyMap(), envio = TrackSubmission.Guardando)
        val cancion = Track(estado.nombre.trim(), estado.duracion)
        viewModelScope.launch {
            val envio = repositorio.addTrack(albumId, cancion).fold(
                onSuccess = { TrackSubmission.Agregada },
                onFailure = {
                    // Any unrecognized failure is treated the same as a load error (retryable
                    // from the banner) instead of rethrowing it and crashing the app.
                    when (it) {
                        is AlbumNotAvailable -> TrackSubmission.AlbumNotAvailable
                        else -> TrackSubmission.Fallida
                    }
                }
            )
            _uiState.update { it.copy(envio = envio) }
        }
    }
}
