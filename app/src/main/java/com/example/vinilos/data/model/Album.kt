package com.example.vinilos.data.model

data class Album(
    val id: Int,
    val nombre: String,
    val portada: String?,
    val descripcion: String,
    val genero: String,
    val disquera: String,
    val fechaLanzamiento: String,
    val canciones: List<Cancion>,
    val artistas: List<Artista>,
    val comentarios: List<Comentario>
)

data class Cancion(val nombre: String, val duracion: String)

data class Artista(val nombre: String, val imagen: String?)

data class Comentario(val descripcion: String, val calificacion: Int?)
