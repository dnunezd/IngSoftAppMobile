package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** Favoritos que el backend confirmó para cada coleccionista (HU10). */
@Dao
abstract class FavoritoDao {

    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE coleccionistaId = :coleccionistaId AND artistaId = :artistaId)")
    abstract suspend fun existe(coleccionistaId: Int, artistaId: Int): Boolean

    @Query("SELECT artistaId FROM favoritos WHERE coleccionistaId = :coleccionistaId")
    abstract suspend fun deColeccionista(coleccionistaId: Int): List<Int>

    @Query("DELETE FROM favoritos WHERE coleccionistaId = :coleccionistaId")
    abstract suspend fun borrarDe(coleccionistaId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertar(favoritos: List<FavoritoEntity>)

    /** La comprobación trae todos los favoritos del coleccionista: lo que ya no viene, se borra. */
    @Transaction
    open suspend fun reemplazarDe(coleccionistaId: Int, artistaIds: List<Int>) {
        borrarDe(coleccionistaId)
        insertar(artistaIds.map { FavoritoEntity(coleccionistaId, it) })
    }

    suspend fun agregar(coleccionistaId: Int, artistaId: Int) =
        insertar(listOf(FavoritoEntity(coleccionistaId, artistaId)))
}
