package com.example.vinilos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Row of the catalog stored on the device (add-cache-catalogo). It has the same fields as
 * [com.example.vinilos.data.model.AlbumResumen]: only what the catalog displays is stored. The cover
 * is the URL; the image itself is stored by Coil in its own cache.
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
