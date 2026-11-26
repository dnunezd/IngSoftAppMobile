package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail

/** App's artists (musicians and bands). The implementation is [ArtistRepositoryImpl]. */
interface ArtistRepository {

    /** Musicians and bands in a single listing sorted by name (HU03). */
    suspend fun getArtists(): Result<List<Artist>>

    /** Artist detail according to its type (HU04). */
    suspend fun getDetail(tipo: ArtistType, id: Int): Result<ArtistDetail>

    /** Last listing stored on the device (add-cache-consultas), sorted by name; empty if there's none. */
    suspend fun getCachedArtists(): List<Artist>

    /** Stored detail of an artist already opened; `null` if there's none or it couldn't be read. */
    suspend fun getCachedDetail(tipo: ArtistType, id: Int): ArtistDetail?

    /**
     * Adds album [idAlbum] to the artist without removing it from its other performers (HU15). Any failure,
     * including a 404, is a `LoadError`.
     */
    suspend fun associateAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit>
}
