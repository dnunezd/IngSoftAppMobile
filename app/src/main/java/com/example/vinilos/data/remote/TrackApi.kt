package com.example.vinilos.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TrackApi {
    @POST("albums/{albumId}/tracks")
    suspend fun agregarCancion(
        @Path("albumId") albumId: Int,
        @Body cancion: CrearCancionDto
    ): Response<Unit>
}

data class CrearCancionDto(
    val name: String,
    val duration: String
)
