package com.example.vinilos.data.remote

data class AlbumDto(
    val id: Int?,
    val name: String?,
    val cover: String?,
    val description: String?,
    val genre: String?,
    val recordLabel: String?,
    val releaseDate: String?,
    val tracks: List<TrackDto>?,
    val performers: List<ArtistaDto>?,
    val comments: List<CommentDto>?
)

/** Cuerpo de `POST /albums`: el backend rechaza con 400 cualquier otro campo, incluso `id`. */
data class CreateAlbumDto(
    val name: String,
    val cover: String,
    val releaseDate: String,
    val description: String,
    val genre: String,
    val recordLabel: String
)

data class TrackDto(val name: String?, val duration: String?)

data class ArtistaDto(val name: String?, val image: String?)

data class CommentDto(val description: String?, val rating: Int?)
