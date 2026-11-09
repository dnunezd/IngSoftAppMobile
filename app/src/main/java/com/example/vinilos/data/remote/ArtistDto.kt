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

/** Respuesta de `GET /musicians/{id}` y `GET /bands/{id}`: cada recurso llena su fecha y solo la banda trae integrantes. */
data class ArtistaDetalleDto(
    val id: Int?,
    val name: String?,
    val image: String?,
    val description: String?,
    val birthDate: String?,
    val creationDate: String?,
    val albums: List<AlbumArtistaDto>?,
    val musicians: List<IntegranteDto>?,
    val performerPrizes: List<PremiacionArtistaDto>?
)

data class AlbumArtistaDto(
    val id: Int?,
    val name: String?,
    val cover: String?,
    val releaseDate: String?
)

data class IntegranteDto(
    val id: Int?,
    val name: String?,
    val image: String?
)

/** Premiación dentro del artista: solo trae su id y su fecha, sin el premio. */
data class PremiacionArtistaDto(
    val id: Int?,
    val premiationDate: String?
)

/** Elemento de `GET /performerprizes`: su [id] es el de la premiación, no el del premio. */
data class PremiacionDto(
    val id: Int?,
    val prize: PremioDto?
)

data class PremioDto(
    val name: String?,
    val organization: String?
)
