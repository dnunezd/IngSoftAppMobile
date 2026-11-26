package com.example.vinilos.data.model

/** Comment that the logged-in collector posts on an album (HU09). */
data class NewComment(
    val coleccionistaId: Int,
    /** From 1 to 5. */
    val calificacion: Int,
    val descripcion: String
)
