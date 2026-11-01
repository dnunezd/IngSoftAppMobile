package com.example.vinilos.data.remote

/**
 * Solo los campos de `Musician` y `Band` que usa el listado. Gson ignora el resto (description,
 * birthDate, creationDate, albums, musicians, performerPrizes) y deja en null los que falten.
 */
data class MusicianDto(
    val id: Int?,
    val name: String?,
    val image: String?
)

data class BandDto(
    val id: Int?,
    val name: String?,
    val image: String?
)
