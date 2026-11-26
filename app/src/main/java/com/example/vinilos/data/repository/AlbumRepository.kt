package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary

/** Se lanza cuando el backend confirma con un 404 que el álbum ya no existe (HU02). */
class AlbumNotAvailable(causa: Throwable? = null) : Exception("El álbum ya no está disponible", causa)

/** Álbumes de la app. La implementación es [AlbumRepositoryImpl]. */
interface AlbumRepository {

    /** Detalle del álbum (HU02); falla con [AlbumNoDisponible] si el backend ya no lo tiene. */
    suspend fun getAlbum(id: Int): Result<Album>

    /** Catálogo (HU01) ordenado por nombre. */
    suspend fun getAlbums(): Result<List<AlbumSummary>>

    /** Último catálogo guardado en el dispositivo; vacío si no hay caché o no se pudo leer. */
    suspend fun getCachedAlbums(): List<AlbumSummary>

    /** Detalle guardado en el dispositivo (add-cache-consultas); `null` si no hay o no se pudo leer. */
    suspend fun getCachedAlbum(id: Int): Album?

    /** Crea el álbum (HU07) y devuelve su id. */
    suspend fun createAlbum(album: NewAlbum): Result<Int>

    /** Asocia una canción existente al álbum indicado (HU08). */
    suspend fun addTrack(albumId: Int, cancion: Track): Result<Unit>
}
