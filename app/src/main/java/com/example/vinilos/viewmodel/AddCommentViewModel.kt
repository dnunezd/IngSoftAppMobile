package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.NewComment
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumNotAvailable
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.CommentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Comment on an album (HU09) as the collector [coleccionistaId], who is the one logged in. */
class AddCommentViewModel(
    private val albumId: Int,
    private val coleccionistaId: Int,
    private val albumRepository: AlbumRepository,
    private val comentarioRepository: CommentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCommentUiState())
    val uiState: StateFlow<AddCommentUiState> = _uiState.asStateFlow()

    init {
        cargarAlbum()
    }

    fun retry() {
        if (_uiState.value.album == CargaAlbum.Error) cargarAlbum()
    }

    /**
     * The detail screen is almost always the entry point, so the album is already cached; if not, it's
     * fetched (add-comentar-album, D3).
     */
    private fun cargarAlbum() {
        _uiState.update { it.copy(album = CargaAlbum.Loading) }
        viewModelScope.launch {
            val album = albumRepository.getCachedAlbum(albumId)
                ?: albumRepository.getAlbum(albumId).getOrElse {
                    // A 404 arrives as AlbumNoDisponible, which isn't ErrorDeCarga: it's also "could not be fetched".
                    if (it !is LoadError && it !is AlbumNotAvailable) throw it
                    null
                }
            _uiState.update { it.copy(album = album?.let(CargaAlbum::Listo) ?: CargaAlbum.Error) }
        }
    }

    fun calificar(calificacion: Int) {
        if (calificacion in 1..5) editar { copy(calificacion = calificacion) }
    }

    fun cambiarOpinion(opinion: String) = editar { copy(opinion = opinion.take(LimiteOpinion)) }

    /** While submitting or already published nothing changes; editing after an error hides the error (D4). */
    private fun editar(cambio: AddCommentUiState.() -> AddCommentUiState) {
        _uiState.update { estado ->
            if (estado.enviando || estado.publicado) estado
            else estado.cambio().copy(envio = EnvioComment.Editando)
        }
    }

    /**
     * The backend accepts duplicate comments, so this guard (and not just the disabled button) is
     * what prevents publishing twice with two quick taps (D4).
     */
    fun post() {
        val estado = _uiState.value
        if (!estado.puedePublicar) return
        _uiState.value = estado.copy(envio = EnvioComment.Enviando)
        val comentario = NewComment(
            coleccionistaId = coleccionistaId,
            calificacion = requireNotNull(estado.calificacion),
            descripcion = estado.opinion.trim()
        )
        viewModelScope.launch {
            val envio = comentarioRepository.postComment(albumId, comentario).fold(
                onSuccess = { EnvioComment.Publicado },
                onFailure = {
                    if (it !is LoadError) throw it
                    EnvioComment.Fallido
                }
            )
            _uiState.update { it.copy(envio = envio) }
        }
    }

    /** Back arrow, back button, or "OK" (D5 and D6). */
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
