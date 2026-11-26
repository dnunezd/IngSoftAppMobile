package com.example.vinilos.data.repository

import com.example.vinilos.data.model.ArtistType

/** Thrown when the backend confirms with a 404 that the artist no longer exists (HU10). */
class ArtistNotAvailable(causa: Throwable? = null) : Exception("El artista ya no está disponible", causa)

/** Artist favorites of the collector (HU10). The implementation is [FavoriteRepositoryImpl]. */
interface FavoriteRepository {

    suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean>

    /** Fails with [ArtistNotAvailable] if the backend no longer has the artist. */
    suspend fun addFavorite(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit>

    /**
     * Whether the backend already confirmed before that the artist is a favorite of that collector, according to
     * what's stored on the device (add-cache-consultas); `false` if nothing is stored or it couldn't be read.
     */
    suspend fun esFavoritoGuardado(collectorId: Int, artistId: Int): Boolean
}
