package com.example.vinilos

import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.repository.CollectorRepository

/** Repositorio de coleccionistas sin red para las pruebas de UI y de navegación. */
class RepositorioFalso(
    var resultado: Result<List<Collector>>,
    /** Listado guardado en el dispositivo (add-cache-consultas); vacío, sin caché, como antes. */
    var guardados: List<Collector> = emptyList()
) : CollectorRepository {
    var consultas = 0

    override suspend fun obtenerColeccionistas(): Result<List<Collector>> {
        consultas++
        return resultado
    }

    override suspend fun obtenerColeccionistasGuardados(): List<Collector> = guardados
}
