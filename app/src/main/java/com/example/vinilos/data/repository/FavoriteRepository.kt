package com.example.vinilos.data.repository

import com.example.vinilos.data.model.ArtistType

/** Se lanza cuando el backend confirma con un 404 que el artista ya no existe (HU10). */
class ArtistaNoDisponible(causa: Throwable? = null) : Exception("El artista ya no está disponible", causa)

/** Favoritos de artista del coleccionista (HU10). La implementación es [FavoriteRepositoryImpl]. */
interface FavoriteRepository {

    suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean>

    /** Falla con [ArtistaNoDisponible] si el backend ya no tiene el artista. */
    suspend fun agregarFavorito(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit>

    /**
     * Si el backend ya confirmó antes que el artista es favorito de ese coleccionista, según lo guardado
     * en el dispositivo (add-cache-consultas); `false` si no hay nada guardado o no se pudo leer.
     */
    suspend fun esFavoritoGuardado(collectorId: Int, artistId: Int): Boolean
}
