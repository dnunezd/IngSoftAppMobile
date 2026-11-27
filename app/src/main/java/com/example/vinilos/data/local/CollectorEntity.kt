package com.example.vinilos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Collector for the selector (HU00). [posicion] preserves the order in which it arrived from the backend. */
@Entity(tableName = "coleccionistas")
data class CollectorEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val correo: String,
    val posicion: Int
)
