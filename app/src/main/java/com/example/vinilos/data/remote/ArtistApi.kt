package com.example.vinilos.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Adapter for the artist service: the only piece that knows the backend's routes and format. */
interface ArtistApi {
    @GET("musicians")
    suspend fun getMusicians(): Response<List<MusicianDto>>

    @GET("bands")
    suspend fun getBands(): Response<List<BandDto>>

    @GET("musicians/{id}")
    suspend fun getMusician(@Path("id") id: Int): Response<ArtistDetailDto>

    @GET("bands/{id}")
    suspend fun getBand(@Path("id") id: Int): Response<ArtistDetailDto>

    @GET("performerprizes")
    suspend fun getPerformerPrizes(): Response<List<AwardDto>>

    /** Adds the musician to the album's performers without removing the others (HU15). */
    @POST("musicians/{id}/albums/{albumId}")
    suspend fun associateAlbumWithPerformer(@Path("id") id: Int, @Path("albumId") albumId: Int): Response<Unit>

    /**
     * Replaces ALL of the band's albums with [albumes] (HU15). An empty list removes them all,
     * so it's only called with the current list plus the new one. `POST bands/{id}/albums/{albumId}` is not
     * used: it would leave the band as the album's only performer (add-agregar-album-artista, D1).
     */
    @PUT("bands/{id}/albums")
    suspend fun replaceBandAlbums(@Path("id") id: Int, @Body albumes: List<IdAlbumDto>): Response<Unit>
}
