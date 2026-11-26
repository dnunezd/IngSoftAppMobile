package com.example.vinilos.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** Comments on an album (HU09). Kept separate from [AlbumApi] (add-comentar-album, D1). */
interface CommentApi {
    @POST("albums/{id}/comments")
    suspend fun postComment(@Path("id") albumId: Int, @Body comentario: NewCommentDto): CommentDto
}

/** Body of `POST /albums/{id}/comments`: the backend validates `description`, `rating` and the `collector` id. */
data class NewCommentDto(
    val description: String,
    val rating: Int,
    val collector: ColeccionistaRefDto
)

data class ColeccionistaRefDto(val id: Int)
