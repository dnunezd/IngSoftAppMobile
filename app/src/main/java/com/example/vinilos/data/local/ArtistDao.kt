package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.vinilos.data.model.ArtistType

/**
 * Listing (HU03) and detail (HU04) of artists. The listing is read without `ORDER BY` because it is
 * sorted in Kotlin with `ordenAlfabetico()`: SQLite does not ignore accents.
 */
@Dao
abstract class ArtistDao {

    @Query("SELECT * FROM artistas")
    abstract suspend fun todos(): List<ArtistEntity>

    @Query("DELETE FROM artistas")
    abstract suspend fun borrarTodos()

    @Insert
    abstract suspend fun insertar(artistas: List<ArtistEntity>)

    @Transaction
    open suspend fun reemplazar(artistas: List<ArtistEntity>) {
        borrarTodos()
        insertar(artistas)
    }

    @Query("SELECT * FROM artista_detalle WHERE tipo = :tipo AND id = :id")
    abstract suspend fun detalle(tipo: ArtistType, id: Int): ArtistDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveDetail(detalle: ArtistDetailEntity)

    @Query("DELETE FROM artista_detalle WHERE tipo = :tipo AND id = :id")
    abstract suspend fun borrarDetalle(tipo: ArtistType, id: Int)

    // ---------- Incremental writes (optimizar-cache-e-imagenes, D6) ----------

    @Upsert
    abstract suspend fun saveArtists(artistas: List<ArtistEntity>)

    @Query("DELETE FROM artistas WHERE tipo = :tipo AND id IN (:ids)")
    abstract suspend fun borrarArtistas(tipo: ArtistType, ids: List<Int>)

    /** Applies what changed in the listing. The key of each row is (tipo, id). */
    @Transaction
    open suspend fun aplicarListado(cambios: TableChanges<Pair<ArtistType, Int>, ArtistEntity>) {
        if (cambios.reemplazarTodo) {
            reemplazar(cambios.save)
            return
        }
        if (cambios.save.isNotEmpty()) saveArtists(cambios.save)
        cambios.borrar.groupBy({ it.first }, { it.second }).forEach { (tipo, ids) ->
            enGrupos(ids) { borrarArtistas(tipo, it) }
        }
    }
}
