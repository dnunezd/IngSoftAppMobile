package com.example.vinilos.ui.detalle

import androidx.annotation.StringRes
import com.example.vinilos.R
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.formatearFechaLanzamiento
import com.example.vinilos.data.model.Espanol
import java.util.Locale

data class AlbumPresentacion(
    val album: Album,
    val fechaLanzamiento: String,
    @param:StringRes val mensajeCanciones: Int? = null,
    @param:StringRes val mensajeComentarios: Int? = null,
    val promedio: String? = null
)

/** [idioma]: el de la app, para que la fecha y el promedio coincidan con los textos (internacionalizar-ingles). */
fun presentarAlbum(album: Album, idioma: Locale = Espanol): AlbumPresentacion =
    AlbumPresentacion(
        album = album,
        fechaLanzamiento = formatearFechaLanzamiento(album.fechaLanzamiento, idioma),
        mensajeCanciones = R.string.detalle_sin_canciones.takeIf { album.canciones.isEmpty() },
        mensajeComentarios = R.string.detalle_sin_comentarios.takeIf { album.comentarios.isEmpty() },
        promedio = promedioCalificaciones(album.comentarios, idioma)
    )
