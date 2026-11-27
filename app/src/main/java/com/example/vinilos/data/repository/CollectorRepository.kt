package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Collector

/** App's collectors. The implementation is [CollectorRepositoryImpl]. */
interface CollectorRepository {

    suspend fun getCollectors(): Result<List<Collector>>

    /**
     * Last listing stored on the device (add-cache-consultas), in the order it arrived from the
     * backend; empty if there's no cache or it couldn't be read.
     */
    suspend fun getCachedCollectors(): List<Collector>
}
