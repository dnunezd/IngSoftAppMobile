package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Collector

/** Coleccionistas de la app. La implementación es [CollectorRepositoryImpl]. */
interface CollectorRepository {

    suspend fun obtenerColeccionistas(): Result<List<Collector>>
}
