package com.example.vinilos.data.remote

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CollectorApi {
    @GET("collectors")
    suspend fun getCollectors(): List<CollectorDto>

    /** Favoritos del coleccionista (HU10). Los ids son únicos entre músicos y bandas en el backend. */
    @GET("collectors/{collectorId}/performers")
    suspend fun getFavoritePerformers(@Path("collectorId") collectorId: Int): List<PerformerDto>

    @POST("collectors/{collectorId}/bands/{bandId}")
    suspend fun addFavoriteBand(
        @Path("collectorId") collectorId: Int,
        @Path("bandId") bandId: Int
    ): PerformerDto

    @POST("collectors/{collectorId}/musicians/{musicianId}")
    suspend fun addFavoriteMusician(
        @Path("collectorId") collectorId: Int,
        @Path("musicianId") musicianId: Int
    ): PerformerDto
}
