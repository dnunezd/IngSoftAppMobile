package com.example.vinilos.data.model

/** Data the Collector fills in the form (HU07). The date is the chosen UTC midnight. */
data class NewAlbum(
    val nombre: String,
    val portada: String,
    val fechaLanzamientoMillis: Long,
    val descripcion: String,
    val genero: String,
    val disquera: String
)
