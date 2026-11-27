package com.example.vinilos.data.local

import androidx.room.Entity
import com.example.vinilos.data.model.ArtistType

/** Row of the artist listing (HU03). The key is (tipo, id): a musician and a band can share an id. */
@Entity(tableName = "artistas", primaryKeys = ["tipo", "id"])
data class ArtistEntity(
    val tipo: ArtistType,
    val id: Int,
    val nombre: String,
    val imagen: String?
)
