package com.example.vinilos.data.model

/**
 * Detalle de un músico o una banda (HU04). [tipo] decide qué significa [fecha] (nacimiento o creación,
 * ya en español) y si hay integrantes. Se llama así para no chocar con [Artista] (los intérpretes de
 * un álbum) ni con [Artist] (la fila del listado).
 */
data class ArtistaDetalle(
    val id: Int,
    val nombre: String,
    val imagen: String?,
    val descripcion: String?,
    val tipo: ArtistType,
    val fecha: String?,
    val integrantes: List<IntegranteArtista>,
    val albumes: List<AlbumArtista>,
    val premios: List<PremioArtista>
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

data class PremioArtista(
    val id: Int,
    val nombre: String,
    val organizacion: String,
    val fecha: String?
)
