package com.example.vinilos.data.local

import androidx.room.Entity

/** Artista que el backend confirmó como favorito de un coleccionista (HU10). */
@Entity(tableName = "favoritos", primaryKeys = ["coleccionistaId", "artistaId"])
data class FavoriteEntity(
    val coleccionistaId: Int,
    val artistaId: Int
)
