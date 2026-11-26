package com.example.vinilos.data.model

/**
 * Detalle de un músico o una banda (HU04). [tipo] decide qué significa [fecha] (nacimiento o creación,
 * en ISO; la pantalla la muestra en el idioma de la app) y si hay integrantes. Se llama así para no chocar con [Artista] (los intérpretes de
 * un álbum) ni con [Artist] (la fila del listado).
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
    /** En ISO, como [ArtistaDetalle.fecha]. */
    val fecha: String?
)
