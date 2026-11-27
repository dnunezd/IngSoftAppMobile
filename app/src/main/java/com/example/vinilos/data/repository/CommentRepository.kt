package com.example.vinilos.data.repository

import com.example.vinilos.data.model.NewComment

/** Comments on albums (HU09). The implementation is [ComentarioRepositoryImpl]. */
interface CommentRepository {

    /**
     * Posts the comment on the album. Any failure (no connection, server error, nonexistent album or
     * collector, invalid data) is a `LoadError`: the backend responds 404 both
     * for the album and for the collector, and the screen shows the same message for all of them.
     */
    suspend fun postComment(albumId: Int, comentario: NewComment): Result<Unit>
}
