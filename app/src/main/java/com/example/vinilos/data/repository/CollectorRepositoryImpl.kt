package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coleccionistas del backend. La consulta y la conversión corren en [dispatcher], fuera del hilo principal. */
class CollectorRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: CollectorApi = adaptador.api(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CollectorRepository {

    override suspend fun obtenerColeccionistas(): Result<List<Collector>> = withContext(dispatcher) {
        adaptador.llamar { api.getCollectors() }.map { lista -> lista.mapNotNull { it.aModelo() } }
    }

    private fun CollectorDto.aModelo(): Collector? {
        val id = id ?: return null
        val nombre = name ?: return null
        val correo = email ?: return null
        return Collector(id, nombre, correo)
    }
}
