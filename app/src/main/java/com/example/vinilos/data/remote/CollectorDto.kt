package com.example.vinilos.data.remote

/**
 * Only the fields of `CollectorDetail` used by the app. Gson ignores the rest (comments,
 * favoritePerformers, collectorAlbums, telephone) and leaves missing ones as null.
 */
data class CollectorDto(
    val id: Int?,
    val name: String?,
    val email: String?
)
