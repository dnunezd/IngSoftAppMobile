package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.AlbumNoDisponible
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EnvioCancion {
    Editando,
    Guardando,
    Agregada,
    Fallida,
    AlbumNoDisponible
}

/**
 * Datos del álbum destino para la tarjeta de contexto del formulario (nombre, portada, artista).
 * Se cargan con mejor esfuerzo: si la consulta falla o aún no responde, [AgregarCancionUiState.albumInfo]
 * se queda en `null` y el formulario sigue totalmente usable, sin afectar [EnvioCancion.AlbumNoDisponible]
 * (que solo lo dispara un fallo al guardar).
 */
data class InfoAlbumDestino(
    val nombre: String,
    val portada: String?,
    val artista: String?
)

data class AgregarCancionUiState(
    val nombre: String = "",
    val duracion: String = "",
    val errores: Map<CampoCancion, ErrorCancion> = validarCancion("", ""),
    val envio: EnvioCancion = EnvioCancion.Editando,
    val albumInfo: InfoAlbumDestino? = null
) {
    val guardando: Boolean get() = envio == EnvioCancion.Guardando
    val puedeGuardar: Boolean
        get() = errores.isEmpty() && envio !in setOf(
            EnvioCancion.Guardando,
            EnvioCancion.Agregada,
            EnvioCancion.AlbumNoDisponible
        )
}

class AgregarCancionViewModel(
    private val albumId: Int,
    private val repositorio: AlbumRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgregarCancionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        cargarInfoAlbum()
    }

    /**
     * Carga de mejor esfuerzo para la tarjeta "Álbum destino": si falla, se deja sin tarjeta y
     * el formulario sigue usable. No debe confundirse con [EnvioCancion.AlbumNoDisponible], que
     * solo lo dispara un fallo al guardar.
     */
    private fun cargarInfoAlbum() {
        viewModelScope.launch {
            repositorio.obtenerAlbum(albumId).onSuccess { album ->
                val info = InfoAlbumDestino(
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

    private fun editar(cambio: AgregarCancionUiState.() -> AgregarCancionUiState) {
        _uiState.update { estado ->
            if (estado.envio == EnvioCancion.Guardando ||
                estado.envio == EnvioCancion.Agregada ||
                estado.envio == EnvioCancion.AlbumNoDisponible
            ) {
                estado
            } else {
                val actualizado = estado.cambio()
                actualizado.copy(errores = validarCancion(actualizado.nombre, actualizado.duracion))
            }
        }
    }

    fun guardar() {
        val estado = _uiState.value
        if (estado.envio == EnvioCancion.Guardando ||
            estado.envio == EnvioCancion.Agregada ||
            estado.envio == EnvioCancion.AlbumNoDisponible
        ) return

        val errores = validarCancion(estado.nombre, estado.duracion)
        if (errores.isNotEmpty()) {
            _uiState.value = estado.copy(errores = errores, envio = EnvioCancion.Editando)
            return
        }

        _uiState.value = estado.copy(errores = emptyMap(), envio = EnvioCancion.Guardando)
        val cancion = Cancion(estado.nombre.trim(), estado.duracion)
        viewModelScope.launch {
            val envio = repositorio.agregarCancion(albumId, cancion).fold(
                onSuccess = { EnvioCancion.Agregada },
                onFailure = {
                    when (it) {
                        is AlbumNoDisponible -> EnvioCancion.AlbumNoDisponible
                        is ErrorDeCarga -> EnvioCancion.Fallida
                        else -> throw it
                    }
                }
            )
            _uiState.update { it.copy(envio = envio) }
        }
    }
}
