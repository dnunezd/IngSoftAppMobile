package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
import com.example.vinilos.data.remote.ServiceAdapter

open class CollectorRepository(
    private val adaptador: ServiceAdapter,
    private val api: CollectorApi = adaptador.api()
) {

    open suspend fun obtenerColeccionistas(): Result<List<Collector>> =
        adaptador.llamar { api.getCollectors() }.map { lista -> lista.mapNotNull { it.aModelo() } }

    private fun CollectorDto.aModelo(): Collector? {
        val id = id ?: return null
        val nombre = name ?: return null
        val correo = email ?: return null
        return Collector(id, nombre, correo)
    }
}
