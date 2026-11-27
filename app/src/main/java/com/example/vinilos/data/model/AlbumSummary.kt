package com.example.vinilos.data.model

data class AlbumSummary(
    val id: Int,
    val nombre: String,
    val portada: String?,
    val genero: String,
    val artista: String,
    val anio: String?
)
