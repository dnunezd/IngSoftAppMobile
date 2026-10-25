package com.example.vinilos.ui.detalle

import androidx.annotation.StringRes
import com.example.vinilos.R
import com.example.vinilos.data.model.Album

data class AlbumPresentacion(
    val album: Album,
    val fechaLanzamiento: String,
    @StringRes val mensajeCanciones: Int? = null
)

fun presentarAlbum(album: Album): AlbumPresentacion =
    AlbumPresentacion(
        album = album,
        fechaLanzamiento = formatearFechaLanzamiento(album.fechaLanzamiento),
        mensajeCanciones = R.string.detalle_sin_canciones.takeIf { album.canciones.isEmpty() }
    )
