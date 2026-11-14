package com.example.vinilos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base local de la app: caché de solo lectura de las consultas al backend. Versión 1, el catálogo
 * (add-cache-catalogo); versión 2, coleccionistas, detalle de álbum, artistas y favoritos
 * (add-cache-consultas).
 */
@Database(
    entities = [
        AlbumEntity::class,
        AlbumDetalleEntity::class,
        ColeccionistaEntity::class,
        ArtistaEntity::class,
        ArtistaDetalleEntity::class,
        FavoritoEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Conversores::class)
abstract class VinilosDb : RoomDatabase() {

    abstract fun albumDao(): AlbumDao
    abstract fun coleccionistaDao(): ColeccionistaDao
    abstract fun artistaDao(): ArtistaDao
    abstract fun favoritoDao(): FavoritoDao

    companion object {
        private const val NOMBRE = "vinilos.db"

        /**
         * La base es una caché que la siguiente consulta vuelve a llenar, así que un cambio de esquema
         * la borra en lugar de migrar datos (add-cache-catalogo D6, add-cache-consultas D1).
         */
        fun crear(context: Context): VinilosDb =
            Room.databaseBuilder(context, VinilosDb::class.java, NOMBRE)
                .fallbackToDestructiveMigration()
                .build()
    }
}
