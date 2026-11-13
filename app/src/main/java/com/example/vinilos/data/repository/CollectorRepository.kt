package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Collector

/** Coleccionistas de la app. La implementación es [CollectorRepositoryImpl]. */
interface CollectorRepository {

    suspend fun obtenerColeccionistas(): Result<List<Collector>>

    /**
     * Último listado guardado en el dispositivo (add-cache-consultas), en el orden en que llegó del
     * backend; vacío si no hay caché o no se pudo leer.
     */
    suspend fun obtenerColeccionistasGuardados(): List<Collector>
}
