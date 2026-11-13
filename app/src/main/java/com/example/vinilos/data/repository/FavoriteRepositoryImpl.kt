package com.example.vinilos.data.repository

import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Favoritos de artista del coleccionista (HU10): comprobar y guardar, sobre /collectors/{id}/...
 * Las consultas corren en [dispatcher], fuera del hilo principal.
 */
class FavoriteRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: CollectorApi = adaptador.api(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FavoriteRepository {

    /** Compara solo por id: en el backend los ids de músico y banda son globalmente únicos. */
    override suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> =
        withContext(dispatcher) {
            adaptador.llamar {
                api.getFavoritePerformers(collectorId).any { it.id == artistId }
            }
        }

    override suspend fun agregarFavorito(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit> =
        withContext(dispatcher) {
            val resultado = adaptador.llamar {
                when (tipo) {
                    ArtistType.BANDA -> api.addFavoriteBand(collectorId, artistId)
                    ArtistType.MUSICO -> api.addFavoriteMusician(collectorId, artistId)
                }
                Unit
            }
            val causa = (resultado.exceptionOrNull() as? ErrorDeCarga)?.cause
            if (causa is HttpException && causa.code() == 404) {
                Result.failure(ArtistaNoDisponible(causa))
            } else {
                resultado
            }
        }
}
