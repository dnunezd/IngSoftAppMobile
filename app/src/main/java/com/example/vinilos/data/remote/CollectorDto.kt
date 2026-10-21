package com.example.vinilos.data.remote

/**
 * Solo los campos de `CollectorDetail` que usa la app. Gson ignora el resto (comments,
 * favoritePerformers, collectorAlbums, telephone) y deja en null los que falten.
 */
data class CollectorDto(
    val id: Int?,
    val name: String?,
    val email: String?
)
