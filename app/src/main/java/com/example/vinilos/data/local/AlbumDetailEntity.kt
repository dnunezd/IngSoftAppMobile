package com.example.vinilos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.Comment

/** Detalle de un álbum (HU02), con los mismos campos que `Album`. La fecha queda en ISO, sin formatear. */
@Entity(tableName = "album_detalle")
data class AlbumDetailEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val portada: String?,
    val descripcion: String,
    val genero: String,
    val disquera: String,
    val fechaLanzamiento: String,
    val canciones: List<Track>,
    val artistas: List<Artista>,
    val comentarios: List<Comment>
)
