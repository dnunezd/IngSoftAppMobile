package com.example.vinilos.data.local

import androidx.room.Entity
import com.example.vinilos.data.model.Artista

/** Artista que el backend confirmó como favorito de un coleccionista (HU10). */
@Entity(tableName = "favoritos", primaryKeys = ["coleccionistaId", "artistaId"])
data class FavoritoEntity(
    val coleccionistaId: Int,
    val artistaId: Int
)
