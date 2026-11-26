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
 * Datos del álbum destino para la tarjeta de contexto del formulario (nombre, portada, artista).
 * Se cargan con mejor esfuerzo: si la consulta falla o aún no responde, [AgregarCancionUiState.albumInfo]
 * se queda en `null` y el formulario sigue totalmente usable, sin afectar [EnvioCancion.AlbumNoDisponible]
 * (que solo lo dispara un fallo al guardar).
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
     * Carga de mejor esfuerzo para la tarjeta "Álbum destino" desde la caché local (Room), sin red:
     * la pantalla de detalle ya consultó `GET /albums/{id}` al abrirse, así que repetirla aquí
     * duplicaría esa consulta (el mismo problema de "consultas que se lanzan dos veces" visto en
     * HU02/HU10). Si no hay nada en caché, se deja sin tarjeta y el formulario sigue usable; esto
     * no debe confundirse con [EnvioCancion.AlbumNoDisponible], que solo lo dispara un fallo al guardar.
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
                    // Si el banner de "no se pudo agregar" seguía visible, editar ya lo invalida:
                    // solo se vuelve a mostrar en el próximo intento fallido de guardar.
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
                    // Cualquier falla no reconocida se trata igual que un error de carga (reintentable
                    // desde el banner) en vez de relanzarla y tumbar la app.
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
