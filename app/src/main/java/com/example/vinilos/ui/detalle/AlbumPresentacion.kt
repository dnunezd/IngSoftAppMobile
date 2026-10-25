package com.example.vinilos.ui.detalle

import androidx.annotation.StringRes
import com.example.vinilos.data.model.Album

data class AlbumPresentacion(
    val album: Album,
    val fechaLanzamiento: String,
    @StringRes val mensajeCanciones: Int? = null
)

fun presentarAlbum(album: Album): AlbumPresentacion =
    AlbumPresentacion(album, formatearFechaLanzamiento(album.fechaLanzamiento))
