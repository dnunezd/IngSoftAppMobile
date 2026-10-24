package com.example.vinilos.data.model

/** Músico o banda del catálogo. La identidad es el par ([type], [id]): un músico y una banda pueden compartir id. */
data class Artist(
    val id: Int,
    val name: String,
    val image: String?,
    val type: ArtistType
)

enum class ArtistType { MUSICO, BANDA }
