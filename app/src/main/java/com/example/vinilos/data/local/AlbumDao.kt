package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/**
 * Acceso a la caché del catálogo. Es una clase abstracta y no una interfaz para que [reemplazar] tenga
 * cuerpo sin depender de `-Xjvm-default` (design.md, D2). Sin `ORDER BY`: el orden lo da `ordenCatalogo`,
 * porque SQLite no ignora tildes (D3).
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
}
