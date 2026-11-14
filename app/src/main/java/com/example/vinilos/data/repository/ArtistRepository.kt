package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle

/** Artistas (músicos y bandas) de la app. La implementación es [ArtistRepositoryImpl]. */
interface ArtistRepository {

    /** Músicos y bandas en un solo listado ordenado por nombre (HU03). */
    suspend fun obtenerArtistas(): Result<List<Artist>>

    /** Detalle del artista según su tipo (HU04). */
    suspend fun obtenerDetalle(tipo: ArtistType, id: Int): Result<ArtistaDetalle>

    /** Último listado guardado en el dispositivo (add-cache-consultas), ordenado por nombre; vacío si no hay. */
    suspend fun obtenerArtistasGuardados(): List<Artist>

    /** Detalle guardado de un artista que ya se abrió; `null` si no hay o no se pudo leer. */
    suspend fun obtenerDetalleGuardado(tipo: ArtistType, id: Int): ArtistaDetalle?
}
