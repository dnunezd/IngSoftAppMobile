package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.ComentarioNuevo
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.AlbumNoDisponible
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.ComentarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Comentar un álbum (HU09) como el coleccionista [coleccionistaId], que es quien ingresó. */
class ComentarAlbumViewModel(
    private val albumId: Int,
    private val coleccionistaId: Int,
    private val albumRepository: AlbumRepository,
    private val comentarioRepository: ComentarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComentarAlbumUiState())
    val uiState: StateFlow<ComentarAlbumUiState> = _uiState.asStateFlow()

    init {
        cargarAlbum()
    }

    fun reintentar() {
        if (_uiState.value.album == CargaAlbum.Error) cargarAlbum()
    }

    /**
     * Casi siempre se llega desde el detalle, así que el álbum ya está guardado; si no, se consulta
     * (add-comentar-album, D3).
     */
    private fun cargarAlbum() {
        _uiState.update { it.copy(album = CargaAlbum.Cargando) }
        viewModelScope.launch {
            val album = albumRepository.obtenerAlbumGuardado(albumId)
                ?: albumRepository.obtenerAlbum(albumId).getOrElse {
                    // Un 404 llega como AlbumNoDisponible, que no es ErrorDeCarga: también es "no se pudo obtener".
                    if (it !is ErrorDeCarga && it !is AlbumNoDisponible) throw it
                    null
                }
            _uiState.update { it.copy(album = album?.let(CargaAlbum::Listo) ?: CargaAlbum.Error) }
        }
    }

    fun calificar(calificacion: Int) {
        if (calificacion in 1..5) editar { copy(calificacion = calificacion) }
    }

    fun cambiarOpinion(opinion: String) = editar { copy(opinion = opinion.take(LimiteOpinion)) }

    /** Durante el envío o ya publicado nada cambia; editar después de un error oculta el error (D4). */
    private fun editar(cambio: ComentarAlbumUiState.() -> ComentarAlbumUiState) {
        _uiState.update { estado ->
            if (estado.enviando || estado.publicado) estado
            else estado.cambio().copy(envio = EnvioComentario.Editando)
        }
    }

    /**
     * El backend acepta comentarios repetidos, así que esta guarda (y no solo el botón deshabilitado) es
     * la que evita publicar dos veces con dos toques seguidos (D4).
     */
    fun publicar() {
        val estado = _uiState.value
        if (!estado.puedePublicar) return
        _uiState.value = estado.copy(envio = EnvioComentario.Enviando)
        val comentario = ComentarioNuevo(
            coleccionistaId = coleccionistaId,
            calificacion = requireNotNull(estado.calificacion),
            descripcion = estado.opinion.trim()
        )
        viewModelScope.launch {
            val envio = comentarioRepository.publicarComentario(albumId, comentario).fold(
                onSuccess = { EnvioComentario.Publicado },
                onFailure = {
                    if (it !is ErrorDeCarga) throw it
                    EnvioComentario.Fallido
                }
            )
            _uiState.update { it.copy(envio = envio) }
        }
    }

    /** Flecha, botón atrás o "Aceptar" (D5 y D6). */
    fun pedirSalir() {
        _uiState.update { estado ->
            when {
                estado.enviando -> estado
                estado.tieneBorrador -> estado.copy(confirmarDescarte = true)
                else -> estado.copy(salir = true)
            }
        }
    }

    fun continuarEscribiendo() = _uiState.update { it.copy(confirmarDescarte = false) }

    fun descartar() = _uiState.update { it.copy(confirmarDescarte = false, salir = true) }
}
