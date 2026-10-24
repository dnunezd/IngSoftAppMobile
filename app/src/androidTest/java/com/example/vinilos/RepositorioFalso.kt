package com.example.vinilos

import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.CollectorRepository

/** Repositorio de coleccionistas sin red para las pruebas de UI y de navegación. */
class RepositorioFalso(var resultado: Result<List<Collector>>) :
    CollectorRepository(ServiceAdapter("http://localhost/")) {
    var consultas = 0

    override suspend fun obtenerColeccionistas(): Result<List<Collector>> {
        consultas++
        return resultado
    }
}
