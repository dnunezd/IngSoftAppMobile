package com.example.vinilos.data.repository

import com.example.vinilos.data.model.NewComment
import com.example.vinilos.data.remote.CollectorRefDto
import com.example.vinilos.data.remote.CommentApi
import com.example.vinilos.data.remote.NewCommentDto
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Posts comments with [ComentarioApi]. The response is ignored: the success screen shows what the
 * collector wrote and the album detail is queried again (add-comentar-album, D1 and D7).
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
        collector = CollectorRefDto(coleccionistaId)
    )
}
