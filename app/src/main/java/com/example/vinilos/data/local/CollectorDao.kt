package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/** Collectors for the selector (HU00): always the latest full response, in the backend's order. */
@Dao
abstract class CollectorDao {

    @Query("SELECT * FROM coleccionistas ORDER BY posicion")
    abstract suspend fun todos(): List<CollectorEntity>

    @Query("DELETE FROM coleccionistas")
    abstract suspend fun borrarTodos()

    @Insert
    abstract suspend fun insertar(coleccionistas: List<CollectorEntity>)

    @Transaction
    open suspend fun reemplazar(coleccionistas: List<CollectorEntity>) {
        borrarTodos()
        insertar(coleccionistas)
    }

    // ---------- Incremental writes (optimizar-cache-e-imagenes, D6) ----------

    @Upsert
    abstract suspend fun save(coleccionistas: List<CollectorEntity>)

    @Query("DELETE FROM coleccionistas WHERE id IN (:ids)")
    abstract suspend fun borrarPorId(ids: List<Int>)

    /** Applies what changed in the listing; a row also changes if its position changed. */
    @Transaction
    open suspend fun aplicar(cambios: TableChanges<Int, CollectorEntity>) {
        if (cambios.reemplazarTodo) {
            reemplazar(cambios.save)
            return
        }
        if (cambios.save.isNotEmpty()) save(cambios.save)
        enGrupos(cambios.borrar) { borrarPorId(it) }
    }
}
