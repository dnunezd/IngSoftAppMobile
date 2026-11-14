package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

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
}
