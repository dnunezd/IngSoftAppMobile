package com.example.vinilos.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

/** Adaptador del servicio de artistas: la única pieza que conoce las rutas y el formato del backend. */
interface ArtistApi {
    @GET("musicians")
    suspend fun getMusicians(): List<MusicianDto>

    @GET("bands")
    suspend fun getBands(): List<BandDto>

    @GET("musicians/{id}")
    suspend fun getMusician(@Path("id") id: Int): ArtistaDetalleDto

    @GET("bands/{id}")
    suspend fun getBand(@Path("id") id: Int): ArtistaDetalleDto

    @GET("performerprizes")
    suspend fun getPerformerPrizes(): List<PremiacionDto>
}
