package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.ColeccionistaDao
import com.example.vinilos.data.local.ColeccionistaEntity
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    private val coleccionistaDao: ColeccionistaDao? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CollectorRepository {

    /** Si la consulta sale bien, reemplaza lo guardado conservando el orden del backend (D4). */
    override suspend fun obtenerColeccionistas(): Result<List<Collector>> = withContext(dispatcher) {
        adaptador.llamar { api.getCollectors() }
            .map { lista -> lista.mapNotNull { it.aModelo() } }
            .onSuccess { guardar(it) }
    }

    /** Vacío si no hay caché o si no se pudo leer: la caché es una mejora, nunca una condición. */
    override suspend fun obtenerColeccionistasGuardados(): List<Collector> = withContext(dispatcher) {
        val dao = coleccionistaDao ?: return@withContext emptyList()
        try {
            dao.todos().map { Collector(it.id, it.nombre, it.correo) }
        } catch (e: SQLiteException) {
            emptyList()
        }
    }

    /** Un fallo al escribir no cambia el resultado de la consulta, que ya llegó bien. */
    private suspend fun guardar(coleccionistas: List<Collector>) {
        val dao = coleccionistaDao ?: return
        try {
            dao.reemplazar(coleccionistas.mapIndexed { posicion, it -> ColeccionistaEntity(it.id, it.name, it.email, posicion) })
        } catch (e: SQLiteException) {
            // Se sigue mostrando la respuesta; la siguiente consulta exitosa vuelve a intentarlo.
        }
    }

    private fun CollectorDto.aModelo(): Collector? {
        val id = id ?: return null
        val nombre = name ?: return null
        val correo = email ?: return null
        return Collector(id, nombre, correo)
    }
}
