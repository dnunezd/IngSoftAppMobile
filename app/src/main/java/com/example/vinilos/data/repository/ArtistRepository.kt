package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail

/** Artistas (músicos y bandas) de la app. La implementación es [ArtistRepositoryImpl]. */
interface ArtistRepository {

    /** Músicos y bandas en un solo listado ordenado por nombre (HU03). */
    suspend fun getArtists(): Result<List<Artist>>

    /** Detalle del artista según su tipo (HU04). */
    suspend fun getDetail(tipo: ArtistType, id: Int): Result<ArtistDetail>

    /** Último listado guardado en el dispositivo (add-cache-consultas), ordenado por nombre; vacío si no hay. */
    suspend fun getCachedArtists(): List<Artist>

    /** Detalle guardado de un artista que ya se abrió; `null` si no hay o no se pudo leer. */
    suspend fun getCachedDetail(tipo: ArtistType, id: Int): ArtistDetail?

    /**
     * Agrega el álbum [idAlbum] al artista sin quitárselo a sus otros intérpretes (HU15). Cualquier fallo,
     * incluido un 404, es un `ErrorDeCarga`.
     */
    suspend fun associateAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit>
}
