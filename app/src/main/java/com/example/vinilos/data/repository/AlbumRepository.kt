package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary

/** Thrown when the backend confirms with a 404 that the album no longer exists (HU02). */
class AlbumNotAvailable(causa: Throwable? = null) : Exception("El álbum ya no está disponible", causa)

/** App's albums. The implementation is [AlbumRepositoryImpl]. */
interface AlbumRepository {

    /** Album detail (HU02); fails with [AlbumNotAvailable] if the backend no longer has it. */
    suspend fun getAlbum(id: Int): Result<Album>

    /** Catalog (HU01) sorted by name. */
    suspend fun getAlbums(): Result<List<AlbumSummary>>

    /** Last catalog stored on the device; empty if there's no cache or it couldn't be read. */
    suspend fun getCachedAlbums(): List<AlbumSummary>

    /** Detail stored on the device (add-cache-consultas); `null` if there's none or it couldn't be read. */
    suspend fun getCachedAlbum(id: Int): Album?

    /** Creates the album (HU07) and returns its id. */
    suspend fun createAlbum(album: NewAlbum): Result<Int>

    /** Associates an existing track with the given album (HU08). */
    suspend fun addTrack(albumId: Int, cancion: Track): Result<Unit>
}
