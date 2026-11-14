package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * Acceso a la caché del catálogo y del detalle de cada álbum. Es una clase abstracta y no una interfaz
 * para que las transacciones tengan cuerpo sin depender de `-Xjvm-default` (add-cache-catalogo, D2). Sin
 * `ORDER BY`: el orden lo da `ordenCatalogo`, porque SQLite no ignora tildes (D3).
 */
@Dao
abstract class AlbumDao {

    @Query("SELECT * FROM albumes")
    abstract suspend fun todos(): List<AlbumEntity>

    @Query("DELETE FROM albumes")
    abstract suspend fun borrarTodos()

    @Insert
    abstract suspend fun insertar(albumes: List<AlbumEntity>)

    /** La caché es siempre la última respuesta completa del backend: lo que ya no viene, se borra. */
    @Transaction
    open suspend fun reemplazar(albumes: List<AlbumEntity>) {
        borrarTodos()
        insertar(albumes)
    }

    // ---------- Detalle (add-cache-consultas, D4) ----------

    @Query("SELECT * FROM album_detalle WHERE id = :id")
    abstract suspend fun detalle(id: Int): AlbumDetalleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardarDetalle(detalle: AlbumDetalleEntity)

    @Query("DELETE FROM album_detalle")
    abstract suspend fun borrarDetalles()

    @Insert
    abstract suspend fun insertarDetalles(detalles: List<AlbumDetalleEntity>)

    @Query("DELETE FROM albumes WHERE id = :id")
    abstract suspend fun borrarDelCatalogo(id: Int)

    @Query("DELETE FROM album_detalle WHERE id = :id")
    abstract suspend fun borrarDetalle(id: Int)

    /** El backend confirmó con un 404 que el álbum ya no existe: deja de estar guardado en ambas tablas. */
    @Transaction
    open suspend fun borrarAlbum(id: Int) {
        borrarDelCatalogo(id)
        borrarDetalle(id)
    }

    /**
     * Reemplaza el catálogo y los detalles con la misma respuesta de `GET /albums`. Un álbum puede estar
     * en [albumes] sin estar en [detalles] si no pasó la validación estricta del detalle.
     */
    @Transaction
    open suspend fun reemplazarCatalogo(albumes: List<AlbumEntity>, detalles: List<AlbumDetalleEntity>) {
        reemplazar(albumes)
        borrarDetalles()
        insertarDetalles(detalles)
    }
}
