package com.example.vinilos.data.model

/** Musician or band from the catalog. Identity is the pair ([type], [id]): a musician and a band can share an id. */
data class Artist(
    val id: Int,
    val name: String,
    val image: String?,
    val type: ArtistType
)

enum class ArtistType { MUSICO, BANDA }
