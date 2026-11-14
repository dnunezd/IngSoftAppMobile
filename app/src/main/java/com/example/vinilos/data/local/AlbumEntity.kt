package com.example.vinilos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila del catálogo guardado en el dispositivo (add-cache-catalogo). Tiene los mismos campos que
 * [com.example.vinilos.data.model.AlbumResumen]: solo se guarda lo que el catálogo muestra. La portada
 * es la URL; la imagen la guarda Coil en su propia caché.
 */
@Entity(tableName = "albumes")
data class AlbumEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val portada: String?,
    val genero: String,
    val artista: String,
    val anio: String?
)
