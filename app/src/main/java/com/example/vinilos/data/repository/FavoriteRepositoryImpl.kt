package com.example.vinilos.data.repository

import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.RecursoNoEncontrado
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Favoritos de artista del coleccionista (HU10): comprobar y guardar, sobre /collectors/{id}/...
 * Retrofit y Room hacen su I/O en sus propios hilos; lo que corre en [dispatcher] (`Dispatchers.Default`
 * por defecto) es el trabajo de CPU: revisar la respuesta. Así se puede llamar desde el hilo principal sin
 * bloquearlo.
 */
class FavoriteRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: CollectorApi = adaptador.api(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
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
            val error = resultado.exceptionOrNull()
            if (error is RecursoNoEncontrado) Result.failure(ArtistaNoDisponible(error.cause)) else resultado
        }
}
