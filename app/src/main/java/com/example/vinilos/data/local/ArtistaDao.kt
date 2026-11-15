package com.example.vinilos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.vinilos.data.model.ArtistType

/**
 * Listado (HU03) y detalle (HU04) de artistas. El listado se lee sin `ORDER BY` porque se ordena en
 * Kotlin con `ordenAlfabetico()`: SQLite no ignora tildes.
 */
@Dao
abstract class ArtistaDao {

    @Query("SELECT * FROM artistas")
    abstract suspend fun todos(): List<ArtistaEntity>

    @Query("DELETE FROM artistas")
    abstract suspend fun borrarTodos()

    @Insert
    abstract suspend fun insertar(artistas: List<ArtistaEntity>)

    @Transaction
    open suspend fun reemplazar(artistas: List<ArtistaEntity>) {
        borrarTodos()
        insertar(artistas)
    }

    @Query("SELECT * FROM artista_detalle WHERE tipo = :tipo AND id = :id")
    abstract suspend fun detalle(tipo: ArtistType, id: Int): ArtistaDetalleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardarDetalle(detalle: ArtistaDetalleEntity)

    @Query("DELETE FROM artista_detalle WHERE tipo = :tipo AND id = :id")
    abstract suspend fun borrarDetalle(tipo: ArtistType, id: Int)

    // ---------- Escrituras incrementales (optimizar-cache-e-imagenes, D6) ----------

    @Upsert
    abstract suspend fun guardarArtistas(artistas: List<ArtistaEntity>)

    @Query("DELETE FROM artistas WHERE tipo = :tipo AND id IN (:ids)")
    abstract suspend fun borrarArtistas(tipo: ArtistType, ids: List<Int>)

    /** Aplica lo que cambió del listado. La clave de cada fila es (tipo, id). */
    @Transaction
    open suspend fun aplicarListado(cambios: CambiosDeTabla<Pair<ArtistType, Int>, ArtistaEntity>) {
        if (cambios.reemplazarTodo) {
            reemplazar(cambios.guardar)
            return
        }
        if (cambios.guardar.isNotEmpty()) guardarArtistas(cambios.guardar)
        cambios.borrar.groupBy({ it.first }, { it.second }).forEach { (tipo, ids) ->
            enGrupos(ids) { borrarArtistas(tipo, it) }
        }
    }
}
