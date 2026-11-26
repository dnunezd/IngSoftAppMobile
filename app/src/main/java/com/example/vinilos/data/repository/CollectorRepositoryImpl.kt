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
 * Coleccionistas del backend. Con [coleccionistaDao], el listado además se guarda en el dispositivo
 * (add-cache-consultas); sin él, se comporta como antes.
 * Retrofit y Room hacen su I/O en sus propios hilos; lo que corre en [dispatcher] (`Dispatchers.Default`
 * por defecto) es el trabajo de CPU: convertir la respuesta. Así se puede llamar desde el hilo principal sin
 * bloquearlo.
 */
class CollectorRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: CollectorApi = adaptador.api(),
    private val coleccionistaDao: CollectorDao? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CollectorRepository {

    /** Protege la comparación, la escritura y la actualización de lo que sigue (optimizar-cache-e-imagenes, D4). */
    private val escritura = Mutex()
    private val copia = CopiaTabla<Int, CollectorEntity> { it.id }
    private var versionGuardada: String? = null

    /**
     * Si la consulta sale bien, lo guardado queda igual a la respuesta, en el orden del backend (D4). Solo se
     * escriben los coleccionistas nuevos o cambiados (una fila cambia también si cambió su posición).
     */
    override suspend fun getCollectors(): Result<List<Collector>> = withContext(dispatcher) {
        adaptador.call { api.getCollectors().conVersion() }
            .map { respuesta -> respuesta.valor.mapNotNull { it.aModelo() } to respuesta.version }
            .onSuccess { (coleccionistas, version) -> save(coleccionistas, version) }
            .map { (coleccionistas, _) -> coleccionistas }
    }

    /** Vacío si no hay caché o si no se pudo leer: la caché es una mejora, nunca una condición. */
    override suspend fun getCachedCollectors(): List<Collector> = withContext(dispatcher) {
        val dao = coleccionistaDao ?: return@withContext emptyList()
        try {
            dao.todos().map { Collector(it.id, it.nombre, it.correo) }
        } catch (e: SQLiteException) {
            emptyList()
        }
    }

    /**
     * Punto único de escritura (D4 y D5). Si no termina normalmente, se descarta lo que se recordaba y la
     * siguiente consulta reemplaza todo. Un fallo del almacenamiento no cambia el resultado de la consulta; la
     * cancelación y los errores de programación se relanzan.
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
