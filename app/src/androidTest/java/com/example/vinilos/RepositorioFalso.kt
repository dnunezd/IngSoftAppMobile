package com.example.vinilos

import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.repository.CollectorRepository

/** Network-free collector repository for UI and navigation tests. */
class RepositorioFalso(
    var resultado: Result<List<Collector>>,
    /** List saved on the device (add-cache-consultas); empty, no cache, as before. */
    var guardados: List<Collector> = emptyList()
) : CollectorRepository {
    var consultas = 0

    override suspend fun getCollectors(): Result<List<Collector>> {
        consultas++
        return resultado
    }

    override suspend fun getCachedCollectors(): List<Collector> = guardados
}
