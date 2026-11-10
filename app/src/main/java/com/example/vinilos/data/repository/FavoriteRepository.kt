package com.example.vinilos.data.repository

import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import retrofit2.HttpException

/** Se lanza cuando el backend confirma con un 404 que el artista ya no existe (HU10). */
class ArtistaNoDisponible(causa: Throwable? = null) : Exception("El artista ya no está disponible", causa)

/** Favoritos de artista del coleccionista (HU10): comprobar y guardar, sobre /collectors/{id}/... */
open class FavoriteRepository(
    private val adaptador: ServiceAdapter,
    private val api: CollectorApi = adaptador.api()
) {

    /** Compara solo por id: en el backend los ids de músico y banda son globalmente únicos. */
    open suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> =
        adaptador.llamar {
            api.getFavoritePerformers(collectorId).any { it.id == artistId }
        }

    open suspend fun agregarFavorito(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit> {
        val resultado = adaptador.llamar {
            when (tipo) {
                ArtistType.BANDA -> api.addFavoriteBand(collectorId, artistId)
                ArtistType.MUSICO -> api.addFavoriteMusician(collectorId, artistId)
            }
            Unit
        }
        val error = resultado.exceptionOrNull()
        if (error is ErrorDeCarga) {
            val causa = error.cause
            if (causa is HttpException && causa.code() == 404) {
                return Result.failure(ArtistaNoDisponible(causa))
            }
        }
        return resultado
    }
}
