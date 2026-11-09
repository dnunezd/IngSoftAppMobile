package com.example.vinilos.data.model

/** Datos que el Coleccionista llena en el formulario (HU07). La fecha es la medianoche UTC elegida. */
data class AlbumNuevo(
    val nombre: String,
    val portada: String,
    val fechaLanzamientoMillis: Long,
    val descripcion: String,
    val genero: String,
    val disquera: String
)
