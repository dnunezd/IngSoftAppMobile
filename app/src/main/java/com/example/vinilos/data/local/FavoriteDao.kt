package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** Favorites that the backend confirmed for each collector (HU10). */
@Dao
abstract class FavoriteDao {

    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE coleccionistaId = :coleccionistaId AND artistaId = :artistaId)")
    abstract suspend fun existe(coleccionistaId: Int, artistaId: Int): Boolean

    @Query("SELECT artistaId FROM favoritos WHERE coleccionistaId = :coleccionistaId")
    abstract suspend fun deColeccionista(coleccionistaId: Int): List<Int>

    @Query("DELETE FROM favoritos WHERE coleccionistaId = :coleccionistaId")
    abstract suspend fun borrarDe(coleccionistaId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertar(favoritos: List<FavoriteEntity>)

    /** The check brings all of the collector's favorites: whatever no longer comes is deleted. */
    @Transaction
    open suspend fun reemplazarDe(coleccionistaId: Int, artistaIds: List<Int>) {
        borrarDe(coleccionistaId)
        insertar(artistaIds.map { FavoriteEntity(coleccionistaId, it) })
    }

    suspend fun agregar(coleccionistaId: Int, artistaId: Int) =
        insertar(listOf(FavoriteEntity(coleccionistaId, artistaId)))

    // ---------- Incremental writes (optimizar-cache-e-imagenes, D6) ----------

    @Query("DELETE FROM favoritos WHERE coleccionistaId = :coleccionistaId AND artistaId IN (:artistaIds)")
    abstract suspend fun borrarDeColeccionista(coleccionistaId: Int, artistaIds: List<Int>)

    /** Applies what changed in a collector's favorites; the rows are the artist ids. */
    @Transaction
    open suspend fun aplicarDe(coleccionistaId: Int, cambios: TableChanges<Int, Int>) {
        if (cambios.reemplazarTodo) {
            reemplazarDe(coleccionistaId, cambios.save)
            return
        }
        if (cambios.save.isNotEmpty()) insertar(cambios.save.map { FavoriteEntity(coleccionistaId, it) })
        enGrupos(cambios.borrar) { borrarDeColeccionista(coleccionistaId, it) }
    }
}
