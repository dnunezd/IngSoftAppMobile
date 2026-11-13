package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.model.AlbumResumen

/** Se lanza cuando el backend confirma con un 404 que el álbum ya no existe (HU02). */
class AlbumNoDisponible(causa: Throwable? = null) : Exception("El álbum ya no está disponible", causa)

/** Álbumes de la app. La implementación es [AlbumRepositoryImpl]. */
interface AlbumRepository {

    /** Detalle del álbum (HU02); falla con [AlbumNoDisponible] si el backend ya no lo tiene. */
    suspend fun obtenerAlbum(id: Int): Result<Album>

    /** Catálogo (HU01) ordenado por nombre. */
    suspend fun obtenerAlbumes(): Result<List<AlbumResumen>>

    /** Último catálogo guardado en el dispositivo; vacío si no hay caché o no se pudo leer. */
    suspend fun obtenerAlbumesGuardados(): List<AlbumResumen>

    /** Detalle guardado en el dispositivo (add-cache-consultas); `null` si no hay o no se pudo leer. */
    suspend fun obtenerAlbumGuardado(id: Int): Album?

    /** Crea el álbum (HU07) y devuelve su id. */
    suspend fun crearAlbum(album: AlbumNuevo): Result<Int>
}
