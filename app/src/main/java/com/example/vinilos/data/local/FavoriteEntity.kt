package com.example.vinilos.data.local

import androidx.room.Entity

/** Artist that the backend confirmed as a collector's favorite (HU10). */
@Entity(tableName = "favoritos", primaryKeys = ["coleccionistaId", "artistaId"])
data class FavoriteEntity(
    val coleccionistaId: Int,
    val artistaId: Int
)
