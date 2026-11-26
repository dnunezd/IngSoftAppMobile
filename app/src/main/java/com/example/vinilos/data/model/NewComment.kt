package com.example.vinilos.data.model

/** Comentario que el coleccionista que ingresó publica sobre un álbum (HU09). */
data class NewComment(
    val coleccionistaId: Int,
    /** De 1 a 5. */
    val calificacion: Int,
    val descripcion: String
)
