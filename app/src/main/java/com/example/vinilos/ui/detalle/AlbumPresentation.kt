package com.example.vinilos.ui.detalle

import androidx.annotation.StringRes
import com.example.vinilos.R
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.formatearReleaseDate
import com.example.vinilos.data.model.Espanol
import java.util.Locale

data class AlbumPresentation(
    val album: Album,
    val fechaLanzamiento: String,
    @param:StringRes val mensajeTrackes: Int? = null,
    @param:StringRes val mensajeComments: Int? = null,
    val promedio: String? = null
)

/** [idioma]: the app's locale, so the date and average match the texts (internationalize-english). */
fun presentarAlbum(album: Album, idioma: Locale = Espanol): AlbumPresentation =
    AlbumPresentation(
        album = album,
        fechaLanzamiento = formatearReleaseDate(album.fechaLanzamiento, idioma),
        mensajeTrackes = R.string.detalle_sin_canciones.takeIf { album.canciones.isEmpty() },
        mensajeComments = R.string.detalle_sin_comentarios.takeIf { album.comentarios.isEmpty() },
        promedio = promedioCalificaciones(album.comentarios, idioma)
    )
