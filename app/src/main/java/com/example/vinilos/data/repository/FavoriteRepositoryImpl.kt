package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.FavoriteDao
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.ResourceNotFound
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.remote.conVersion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Collector's artist favorites (HU10): check and store, over /collectors/{id}/... With
 * [favoritoDao], the favorites the backend confirms are also stored on the device
 * (add-cache-consultas); without it, it behaves as before.
 * Retrofit and Room do their I/O on their own threads; what runs on [dispatcher] (`Dispatchers.Default`
 * by default) is CPU work: checking the response. This way it can be called from the main thread without
 * blocking it.
 */
class FavoriteRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: CollectorApi = adaptador.api(),
    private val favoritoDao: FavoriteDao? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : FavoriteRepository {

    /**
     * Last thing written of each collector's favorites and its version (optimizar-cache-e-imagenes,
     * D4). Only touched with [escritura] held.
     */
    private val escritura = Mutex()
    private val copias = HashMap<Int, CopiaTabla<Int, Int>>()
    private val versiones = Versiones<Int>()

    /**
     * Compares only by id: in the backend, musician and band ids are globally unique. The response
     * brings all of the collector's favorites, so what's stored for that collector is left equal to it
     * (D4); only the differences are added and removed.
     */
    override suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> =
        withContext(dispatcher) {
            adaptador.call {
                val respuesta = api.getFavoritePerformers(collectorId).conVersion()
                respuesta.valor.mapNotNull { it.id }.distinct() to respuesta.version
            }
                .onSuccess { (ids, version) -> saveFavorites(collectorId, ids, version) }
                .map { (ids, _) -> artistId in ids }
        }

    private suspend fun saveFavorites(collectorId: Int, ids: List<Int>, version: String?) = escribir {
        if (versiones.igual(collectorId, version)) return@escribir
        val copia = copias.getOrPut(collectorId) { CopiaTabla { it } }
        val cambios = copia.cambiosPara(ids)
        if (!cambios.vacios) {
            aplicarDe(collectorId, cambios)
            copia.aplicar(cambios)
        }
        versiones.anotar(collectorId, version)
    }

    override suspend fun esFavoritoGuardado(collectorId: Int, artistId: Int): Boolean = withContext(dispatcher) {
        val dao = favoritoDao ?: return@withContext false
        try {
            dao.existe(collectorId, artistId)
        } catch (e: SQLiteException) {
            false
        }
    }

    /**
     * Single write point (D4 and D5). If the block does not finish normally, whatever was remembered is discarded
     * and the next check replaces the favorites. A storage failure does not change the result;
     * the cancellation and programming errors are rethrown.
     */
    private suspend fun escribir(bloque: suspend FavoriteDao.() -> Unit) {
        val dao = favoritoDao ?: return
        escritura.withLock {
            try {
                dao.bloque()
            } catch (e: Throwable) {
                copias.clear()
                versiones.descartarTodas()
                if (e !is SQLiteException) throw e
            }
        }
    }

    override suspend fun addFavorite(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit> =
        withContext(dispatcher) {
            val resultado = adaptador.call {
                when (tipo) {
                    ArtistType.BANDA -> api.addFavoriteBand(collectorId, artistId)
                    ArtistType.MUSICO -> api.addFavoriteMusician(collectorId, artistId)
                }
                Unit
            }
            val error = resultado.exceptionOrNull()
            if (error is ResourceNotFound) {
                Result.failure(ArtistNotAvailable(error.cause))
            } else {
                resultado.onSuccess {
                    escribir {
                        agregar(collectorId, artistId)
                        copias[collectorId]?.guardada(artistId)
                        // Rule 3 of D4: if the favorite is removed on the backend, its previous version comes back.
                        versiones.descartar(collectorId)
                    }
                }
            }
        }
}
