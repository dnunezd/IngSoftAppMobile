package com.example.vinilos.data.remote

/**
 * Only the fields of `Musician` and `Band` used by the listing. Gson ignores the rest (description,
 * birthDate, creationDate, albums, musicians, performerPrizes) and leaves missing ones as null.
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

/** Response from `GET /musicians/{id}` and `GET /bands/{id}`: each resource fills its own date and only the band has members. */
data class ArtistDetailDto(
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

/** Element of the body of `PUT /bands/{id}/albums`: the backend only reads the id. */
data class IdAlbumDto(val id: Int)

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

/** Award within the artist: it only carries its id and its date, without the prize itself. */
data class PremiacionArtistaDto(
    val id: Int?,
    val premiationDate: String?
)

/** Element of `GET /performerprizes`: its [id] is the award's, not the prize's. */
data class PremiacionDto(
    val id: Int?,
    val prize: PrizeDto?
)

data class PrizeDto(
    val name: String?,
    val organization: String?
)
