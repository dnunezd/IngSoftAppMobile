package com.example.vinilos.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** Comentarios de un álbum (HU09). Va aparte de [AlbumApi] (add-comentar-album, D1). */
interface ComentarioApi {
    @POST("albums/{id}/comments")
    suspend fun publicarComentario(@Path("id") albumId: Int, @Body comentario: ComentarioNuevoDto): ComentarioDto
}

/** Cuerpo de `POST /albums/{id}/comments`: el backend valida `description`, `rating` y el id de `collector`. */
data class ComentarioNuevoDto(
    val description: String,
    val rating: Int,
    val collector: ColeccionistaRefDto
)

data class ColeccionistaRefDto(val id: Int)
