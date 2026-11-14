package com.example.vinilos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Coleccionista del selector (HU00). [posicion] conserva el orden en que llegó del backend. */
@Entity(tableName = "coleccionistas")
data class ColeccionistaEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val correo: String,
    val posicion: Int
)
