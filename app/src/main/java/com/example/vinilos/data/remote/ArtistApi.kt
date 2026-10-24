package com.example.vinilos.data.remote

import retrofit2.http.GET

/** Adaptador del servicio de artistas: la única pieza que conoce las rutas y el formato del backend. */
interface ArtistApi {
    @GET("musicians")
    suspend fun getMusicians(): List<MusicianDto>

    @GET("bands")
    suspend fun getBands(): List<BandDto>
}
