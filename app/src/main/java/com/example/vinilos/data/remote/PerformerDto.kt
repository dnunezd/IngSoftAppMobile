package com.example.vinilos.data.remote

/**
 * Solo el campo que usa HU10 para comparar favoritos. Gson ignora el resto (name, image,
 * description) y deja en null el que falte; la comparación de favorito es solo por [id].
 */
data class PerformerDto(
    val id: Int?
)
