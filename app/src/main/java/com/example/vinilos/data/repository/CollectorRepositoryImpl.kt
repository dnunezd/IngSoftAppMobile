package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.CollectorDao
import com.example.vinilos.data.local.CollectorEntity
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.remote.conVersion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Backend collectors. With [coleccionistaDao], the listing is also stored on the device
 * (add-cache-consultas); without it, it behaves as before.
 * Retrofit and Room do their I/O on their own threads; what runs on [dispatcher] (`Dispatchers.Default`
 * by default) is CPU work: converting the response. This way it can be called from the main thread without
 * blocking it.
 */
class CollectorRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: CollectorApi = adaptador.api(),
    private val coleccionistaDao: CollectorDao? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CollectorRepository {

    /** Protects the comparison, the write and the update of whatever follows (optimizar-cache-e-imagenes, D4). */
    private val escritura = Mutex()
    private val copia = CopiaTabla<Int, CollectorEntity> { it.id }
    private var versionGuardada: String? = null

    /**
     * If the query succeeds, what's stored is left equal to the response, in the backend's order (D4). Only
     * the new or changed collectors are written (a row also changes if its position changed).
     */
    override suspend fun getCollectors(): Result<List<Collector>> = withContext(dispatcher) {
        adaptador.call { api.getCollectors().conVersion() }
            .map { respuesta -> respuesta.valor.mapNotNull { it.aModelo() } to respuesta.version }
            .onSuccess { (coleccionistas, version) -> save(coleccionistas, version) }
            .map { (coleccionistas, _) -> coleccionistas }
    }

    /** Empty if there's no cache or it couldn't be read: the cache is an enhancement, never a precondition. */
    override suspend fun getCachedCollectors(): List<Collector> = withContext(dispatcher) {
        val dao = coleccionistaDao ?: return@withContext emptyList()
        try {
            dao.todos().map { Collector(it.id, it.nombre, it.correo) }
        } catch (e: SQLiteException) {
            emptyList()
        }
    }

    /**
     * Single write point (D4 and D5). If it doesn't finish normally, whatever was remembered is discarded and the
     * next query replaces everything. A storage failure does not change the query's result; the
     * cancellation and programming errors are rethrown.
     */
    private suspend fun save(coleccionistas: List<Collector>, version: String?) {
        val dao = coleccionistaDao ?: return
        escritura.withLock {
            try {
                if (version != null && version == versionGuardada) return@withLock
                val cambios = copia.cambiosPara(
                    coleccionistas.mapIndexed { posicion, it -> CollectorEntity(it.id, it.name, it.email, posicion) }
                )
                if (!cambios.vacios) {
                    dao.aplicar(cambios)
                    copia.aplicar(cambios)
                }
                versionGuardada = version
            } catch (e: Throwable) {
                copia.descartar()
                versionGuardada = null
                if (e !is SQLiteException) throw e
            }
        }
    }

    private fun CollectorDto.aModelo(): Collector? {
        val id = id ?: return null
        val nombre = name ?: return null
        val correo = email ?: return null
        return Collector(id, nombre, correo)
    }
}
