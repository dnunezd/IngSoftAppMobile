package com.example.vinilos.data.repository

import com.example.vinilos.data.model.NewComment
import com.example.vinilos.data.remote.ColeccionistaRefDto
import com.example.vinilos.data.remote.CommentApi
import com.example.vinilos.data.remote.NewCommentDto
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Publica comentarios con [ComentarioApi]. La respuesta se ignora: la pantalla de éxito muestra lo que
 * escribió el coleccionista y el detalle del álbum vuelve a consultarse (add-comentar-album, D1 y D7).
 */
class CommentRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: CommentApi = adaptador.api(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CommentRepository {

    override suspend fun postComment(albumId: Int, comentario: NewComment): Result<Unit> =
        withContext(dispatcher) {
            adaptador.call {
                api.postComment(albumId, comentario.aDto())
                Unit
            }
        }

    private fun NewComment.aDto() = NewCommentDto(
        description = descripcion,
        rating = calificacion,
        collector = ColeccionistaRefDto(coleccionistaId)
    )
}
