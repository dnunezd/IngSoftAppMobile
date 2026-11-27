package com.example.vinilos.data.model

data class Album(
    val id: Int,
    val nombre: String,
    val portada: String?,
    val descripcion: String,
    val genero: String,
    val disquera: String,
    val fechaLanzamiento: String,
    val canciones: List<Track>,
    val artistas: List<Performer>,
    val comentarios: List<Comment>
)

data class Track(val nombre: String, val duracion: String)

data class Performer(val nombre: String, val imagen: String?)

data class Comment(val descripcion: String, val calificacion: Int?)
