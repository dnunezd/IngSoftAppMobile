package com.example.vinilos.data.model

/**
 * Detail of a musician or a band (HU04). [tipo] determines what [fecha] means (birth or founding,
 * in ISO; the screen displays it in the app's language) and whether there are members. It's named this way to avoid clashing with [Artista] (the performers of
 * an album) or with [Artist] (the listing row).
 */
data class ArtistDetail(
    val id: Int,
    val nombre: String,
    val imagen: String?,
    val descripcion: String?,
    val tipo: ArtistType,
    val fecha: String?,
    val integrantes: List<IntegranteArtista>,
    val albumes: List<AlbumArtista>,
    val premios: List<ArtistPrize>
)

data class IntegranteArtista(
    val id: Int,
    val nombre: String,
    val imagen: String?
)

data class AlbumArtista(
    val id: Int,
    val nombre: String,
    val portada: String?,
    val anio: String?
)

data class ArtistPrize(
    val id: Int,
    val nombre: String,
    val organizacion: String,
    /** In ISO, like [ArtistaDetalle.fecha]. */
    val fecha: String?
)
