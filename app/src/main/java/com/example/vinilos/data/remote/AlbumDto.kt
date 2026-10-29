package com.example.vinilos.data.remote

data class AlbumDto(
    val id: Int?,
    val name: String?,
    val cover: String?,
    val description: String?,
    val genre: String?,
    val recordLabel: String?,
    val releaseDate: String?,
    val tracks: List<CancionDto>?,
    val performers: List<ArtistaDto>?,
    val comments: List<ComentarioDto>?
)

data class CancionDto(val name: String?, val duration: String?)

data class ArtistaDto(val name: String?, val image: String?)

data class ComentarioDto(val description: String?, val rating: Int?)
