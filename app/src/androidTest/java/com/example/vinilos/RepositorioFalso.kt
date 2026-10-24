package com.example.vinilos

import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
import com.example.vinilos.data.repository.CollectorRepository

/** Repositorio de coleccionistas sin red para las pruebas de UI y de navegación. */
class RepositorioFalso(var resultado: Result<List<Collector>>) : CollectorRepository(
    object : CollectorApi {
        override suspend fun getCollectors(): List<CollectorDto> = error("no se usa")
    }
) {
    var consultas = 0

    override suspend fun obtenerColeccionistas(): Result<List<Collector>> {
        consultas++
        return resultado
    }
}
