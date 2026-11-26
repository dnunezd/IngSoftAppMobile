package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

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
    abstract suspend fun detalle(id: Int): AlbumDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveDetail(detalle: AlbumDetailEntity)

    @Query("DELETE FROM album_detalle")
    abstract suspend fun borrarDetalles()

    @Insert
    abstract suspend fun insertarDetalles(detalles: List<AlbumDetailEntity>)

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
    open suspend fun reemplazarCatalogo(albumes: List<AlbumEntity>, detalles: List<AlbumDetailEntity>) {
        reemplazar(albumes)
        borrarDetalles()
        insertarDetalles(detalles)
    }

    // ---------- Escrituras incrementales (optimizar-cache-e-imagenes, D6) ----------

    @Upsert
    abstract suspend fun saveAlbums(albumes: List<AlbumEntity>)

    @Upsert
    abstract suspend fun saveDetails(detalles: List<AlbumDetailEntity>)

    @Query("DELETE FROM albumes WHERE id IN (:ids)")
    abstract suspend fun borrarAlbumesPorId(ids: List<Int>)

    @Query("DELETE FROM album_detalle WHERE id IN (:ids)")
    abstract suspend fun borrarDetallesPorId(ids: List<Int>)

    /**
     * Aplica en una sola transacción lo que cambió del catálogo y de los detalles. Cada tabla se reemplaza
     * entera o solo recibe sus diferencias, según lo que el repositorio sepa de ella.
     */
    @Transaction
    open suspend fun aplicarCatalogo(
        albumes: TableChanges<Int, AlbumEntity>,
        detalles: TableChanges<Int, AlbumDetailEntity>
    ) {
        if (albumes.reemplazarTodo) {
            reemplazar(albumes.save)
        } else {
            if (albumes.save.isNotEmpty()) saveAlbums(albumes.save)
            enGrupos(albumes.borrar) { borrarAlbumesPorId(it) }
        }
        if (detalles.reemplazarTodo) {
            borrarDetalles()
            insertarDetalles(detalles.save)
        } else {
            if (detalles.save.isNotEmpty()) saveDetails(detalles.save)
            enGrupos(detalles.borrar) { borrarDetallesPorId(it) }
        }
    }
}
