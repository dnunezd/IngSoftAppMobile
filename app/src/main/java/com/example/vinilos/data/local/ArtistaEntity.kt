package com.example.vinilos.data.local

import androidx.room.Entity
import com.example.vinilos.data.model.ArtistType

/** Fila del listado de artistas (HU03). La clave es (tipo, id): un músico y una banda pueden compartir id. */
@Entity(tableName = "artistas", primaryKeys = ["tipo", "id"])
data class ArtistaEntity(
    val tipo: ArtistType,
    val id: Int,
    val nombre: String,
    val imagen: String?
)
