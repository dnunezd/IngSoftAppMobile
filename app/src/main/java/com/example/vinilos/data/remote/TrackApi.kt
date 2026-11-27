package com.example.vinilos.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TrackApi {
    @POST("albums/{albumId}/tracks")
    suspend fun addTrack(
        @Path("albumId") albumId: Int,
        @Body cancion: CrearTrackDto
    ): Response<Unit>
}

data class CrearTrackDto(
    val name: String,
    val duration: String
)
