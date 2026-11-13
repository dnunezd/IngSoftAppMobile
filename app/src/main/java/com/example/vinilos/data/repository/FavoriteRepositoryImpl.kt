package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.FavoritoDao
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.RecursoNoEncontrado
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Favoritos de artista del coleccionista (HU10): comprobar y guardar, sobre /collectors/{id}/... Con
 * [favoritoDao], los favoritos que el backend confirma además se guardan en el dispositivo
 * (add-cache-consultas); sin él, se comporta como antes.
 * Retrofit y Room hacen su I/O en sus propios hilos; lo que corre en [dispatcher] (`Dispatchers.Default`
 * por defecto) es el trabajo de CPU: revisar la respuesta. Así se puede llamar desde el hilo principal sin
 * bloquearlo.
 */
class FavoriteRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: CollectorApi = adaptador.api(),
    private val favoritoDao: FavoritoDao? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : FavoriteRepository {

    /**
     * Compara solo por id: en el backend los ids de músico y banda son globalmente únicos. La respuesta
     * trae todos los favoritos del coleccionista, así que reemplaza los que tenía guardados (D4).
     */
    override suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> =
        withContext(dispatcher) {
            adaptador.llamar { api.getFavoritePerformers(collectorId).mapNotNull { it.id } }
                .onSuccess { ids -> escribir { reemplazarDe(collectorId, ids) } }
                .map { ids -> artistId in ids }
        }

    override suspend fun esFavoritoGuardado(collectorId: Int, artistId: Int): Boolean = withContext(dispatcher) {
        val dao = favoritoDao ?: return@withContext false
        try {
            dao.existe(collectorId, artistId)
        } catch (e: SQLiteException) {
            false
        }
    }

    /** Un fallo al escribir no cambia el resultado de la consulta, que ya llegó del backend. */
    private suspend fun escribir(bloque: suspend FavoritoDao.() -> Unit) {
        val dao = favoritoDao ?: return
        try {
            dao.bloque()
        } catch (e: SQLiteException) {
            // Se sigue mostrando la respuesta; la siguiente comprobación exitosa vuelve a intentarlo.
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
            if (error is RecursoNoEncontrado) {
                Result.failure(ArtistaNoDisponible(error.cause))
            } else {
                resultado.onSuccess { escribir { agregar(collectorId, artistId) } }
            }
        }
}
