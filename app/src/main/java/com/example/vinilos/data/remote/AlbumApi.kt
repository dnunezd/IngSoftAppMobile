package com.example.vinilos.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AlbumApi {
    @GET("albums")
    suspend fun obtenerAlbumes(): List<AlbumDto>

    @GET("albums/{id}")
    suspend fun obtenerAlbum(@Path("id") id: Int): Response<AlbumDto>

    @POST("albums")
    suspend fun crearAlbum(@Body album: CrearAlbumDto): AlbumDto
}
