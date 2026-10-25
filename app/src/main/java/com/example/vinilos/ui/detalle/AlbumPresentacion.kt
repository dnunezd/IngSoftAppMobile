package com.example.vinilos.ui.detalle

import com.example.vinilos.data.model.Album

data class AlbumPresentacion(
    val album: Album,
    val fechaLanzamiento: String
)

fun presentarAlbum(album: Album): AlbumPresentacion =
    AlbumPresentacion(album, formatearFechaLanzamiento(album.fechaLanzamiento))
