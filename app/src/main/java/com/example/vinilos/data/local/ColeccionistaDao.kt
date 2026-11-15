package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/** Coleccionistas del selector (HU00): siempre la última respuesta completa, en el orden del backend. */
@Dao
abstract class ColeccionistaDao {

    @Query("SELECT * FROM coleccionistas ORDER BY posicion")
    abstract suspend fun todos(): List<ColeccionistaEntity>

    @Query("DELETE FROM coleccionistas")
    abstract suspend fun borrarTodos()

    @Insert
    abstract suspend fun insertar(coleccionistas: List<ColeccionistaEntity>)

    @Transaction
    open suspend fun reemplazar(coleccionistas: List<ColeccionistaEntity>) {
        borrarTodos()
        insertar(coleccionistas)
    }

    // ---------- Escrituras incrementales (optimizar-cache-e-imagenes, D6) ----------

    @Upsert
    abstract suspend fun guardar(coleccionistas: List<ColeccionistaEntity>)

    @Query("DELETE FROM coleccionistas WHERE id IN (:ids)")
    abstract suspend fun borrarPorId(ids: List<Int>)

    /** Aplica lo que cambió del listado; una fila cambia también si cambió su posición. */
    @Transaction
    open suspend fun aplicar(cambios: CambiosDeTabla<Int, ColeccionistaEntity>) {
        if (cambios.reemplazarTodo) {
            reemplazar(cambios.guardar)
            return
        }
        if (cambios.guardar.isNotEmpty()) guardar(cambios.guardar)
        enGrupos(cambios.borrar) { borrarPorId(it) }
    }
}
