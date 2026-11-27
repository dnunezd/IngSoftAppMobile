package com.example.vinilos.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CollectorApi {
    @GET("collectors")
    suspend fun getCollectors(): Response<List<CollectorDto>>

    /** Collector's favorites (HU10). The ids are unique between musicians and bands in the backend. */
    @GET("collectors/{collectorId}/performers")
    suspend fun getFavoritePerformers(@Path("collectorId") collectorId: Int): Response<List<PerformerDto>>

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
