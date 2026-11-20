package com.example.vinilos.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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

    /** Agrega el músico a los intérpretes del álbum sin quitar a los demás (HU15). */
    @POST("musicians/{id}/albums/{albumId}")
    suspend fun asociarAlbumAMusico(@Path("id") id: Int, @Path("albumId") albumId: Int): Response<Unit>

    /**
     * Reemplaza TODOS los álbumes de la banda por [albumes] (HU15). Con una lista vacía se los quita todos,
     * así que solo se llama con la lista actual más el nuevo. `POST bands/{id}/albums/{albumId}` no se usa:
     * deja a la banda como única intérprete del álbum (add-agregar-album-artista, D1).
     */
    @PUT("bands/{id}/albums")
    suspend fun reemplazarAlbumesDeBanda(@Path("id") id: Int, @Body albumes: List<IdAlbumDto>): Response<Unit>
}
