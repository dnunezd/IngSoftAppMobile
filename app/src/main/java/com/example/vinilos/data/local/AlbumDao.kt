package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * Access to the catalog cache and the detail of each album. It is an abstract class and not an interface
 * so that transactions can have a body without depending on `-Xjvm-default` (add-cache-catalogo, D2). No
 * `ORDER BY`: the order is given by `ordenCatalogo`, because SQLite does not ignore accents (D3).
 */
@Dao
abstract class AlbumDao {

    @Query("SELECT * FROM albumes")
    abstract suspend fun todos(): List<AlbumEntity>

    @Query("DELETE FROM albumes")
    abstract suspend fun borrarTodos()

    @Insert
    abstract suspend fun insertar(albumes: List<AlbumEntity>)

    /** The cache is always the latest full response from the backend: whatever no longer comes is deleted. */
    @Transaction
    open suspend fun reemplazar(albumes: List<AlbumEntity>) {
        borrarTodos()
        insertar(albumes)
    }

    // ---------- Detail (add-cache-consultas, D4) ----------

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

    /** The backend confirmed with a 404 that the album no longer exists: it stops being stored in both tables. */
    @Transaction
    open suspend fun borrarAlbum(id: Int) {
        borrarDelCatalogo(id)
        borrarDetalle(id)
    }

    /**
     * Replaces the catalog and the details with the same response from `GET /albums`. An album can be
     * in [albumes] without being in [detalles] if it did not pass the strict detail validation.
     */
    @Transaction
    open suspend fun reemplazarCatalogo(albumes: List<AlbumEntity>, detalles: List<AlbumDetailEntity>) {
        reemplazar(albumes)
        borrarDetalles()
        insertarDetalles(detalles)
    }

    // ---------- Incremental writes (optimizar-cache-e-imagenes, D6) ----------

    @Upsert
    abstract suspend fun saveAlbums(albumes: List<AlbumEntity>)

    @Upsert
    abstract suspend fun saveDetails(detalles: List<AlbumDetailEntity>)

    @Query("DELETE FROM albumes WHERE id IN (:ids)")
    abstract suspend fun borrarAlbumesPorId(ids: List<Int>)

    @Query("DELETE FROM album_detalle WHERE id IN (:ids)")
    abstract suspend fun borrarDetallesPorId(ids: List<Int>)

    /**
     * Applies, in a single transaction, what changed in the catalog and in the details. Each table is
     * either replaced entirely or only receives its differences, depending on what the repository knows about it.
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
