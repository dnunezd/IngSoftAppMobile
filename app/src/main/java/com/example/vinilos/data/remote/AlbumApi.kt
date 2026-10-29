package com.example.vinilos.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AlbumApi {
    @GET("albums/{id}")
    suspend fun obtenerAlbum(@Path("id") id: Int): Response<AlbumDto>
}
