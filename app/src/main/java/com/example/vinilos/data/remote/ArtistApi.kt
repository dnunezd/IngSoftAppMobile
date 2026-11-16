package com.example.vinilos.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/** Adaptador del servicio de artistas: la única pieza que conoce las rutas y el formato del backend. */
interface ArtistApi {
    @GET("musicians")
    suspend fun getMusicians(): Response<List<MusicianDto>>

    @GET("bands")
    suspend fun getBands(): Response<List<BandDto>>

    @GET("musicians/{id}")
    suspend fun getMusician(@Path("id") id: Int): Response<ArtistaDetalleDto>

    @GET("bands/{id}")
    suspend fun getBand(@Path("id") id: Int): Response<ArtistaDetalleDto>

    @GET("performerprizes")
    suspend fun getPerformerPrizes(): Response<List<PremiacionDto>>
}
