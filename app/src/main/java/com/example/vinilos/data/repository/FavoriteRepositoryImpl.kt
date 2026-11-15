package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.FavoritoDao
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.RecursoNoEncontrado
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.remote.conVersion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
     * Lo último que se escribió de los favoritos de cada coleccionista y su versión (optimizar-cache-e-imagenes,
     * D4). Solo se tocan con [escritura] tomado.
     */
    private val escritura = Mutex()
    private val copias = HashMap<Int, CopiaTabla<Int, Int>>()
    private val versiones = Versiones<Int>()

    /**
     * Compara solo por id: en el backend los ids de músico y banda son globalmente únicos. La respuesta
     * trae todos los favoritos del coleccionista, así que lo guardado de ese coleccionista queda igual a ella
     * (D4); solo se agregan y se quitan las diferencias.
     */
    override suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> =
        withContext(dispatcher) {
            adaptador.llamar {
                val respuesta = api.getFavoritePerformers(collectorId).conVersion()
                respuesta.valor.mapNotNull { it.id }.distinct() to respuesta.version
            }
                .onSuccess { (ids, version) -> guardarFavoritos(collectorId, ids, version) }
                .map { (ids, _) -> artistId in ids }
        }

    private suspend fun guardarFavoritos(collectorId: Int, ids: List<Int>, version: String?) = escribir {
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
     * Punto único de escritura (D4 y D5). Si el bloque no termina normalmente, se descarta lo que se recordaba
     * y la siguiente comprobación reemplaza los favoritos. Un fallo del almacenamiento no cambia el resultado;
     * la cancelación y los errores de programación se relanzan.
     */
    private suspend fun escribir(bloque: suspend FavoritoDao.() -> Unit) {
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
                resultado.onSuccess {
                    escribir {
                        agregar(collectorId, artistId)
                        copias[collectorId]?.guardada(artistId)
                        // Regla 3 de D4: si el favorito se quita en el backend, su versión anterior vuelve.
                        versiones.descartar(collectorId)
                    }
                }
            }
        }
}
