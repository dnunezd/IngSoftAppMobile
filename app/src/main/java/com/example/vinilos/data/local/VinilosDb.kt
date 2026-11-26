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
        AlbumDetailEntity::class,
        CollectorEntity::class,
        ArtistEntity::class,
        ArtistDetailEntity::class,
        FavoriteEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Conversores::class)
abstract class VinilosDb : RoomDatabase() {

    abstract fun albumDao(): AlbumDao
    abstract fun coleccionistaDao(): CollectorDao
    abstract fun artistaDao(): ArtistDao
    abstract fun favoritoDao(): FavoriteDao

    companion object {
        private const val NOMBRE = "vinilos.db"

        /**
         * La base es una caché que la siguiente consulta vuelve a llenar, así que un cambio de esquema
         * la borra en lugar de migrar datos (add-cache-catalogo D6, add-cache-consultas D1). La versión 3 guarda
 * las fechas del artista en ISO en lugar de en español (internacionalizar-ingles).
         */
        fun create(context: Context): VinilosDb =
            Room.databaseBuilder(context, VinilosDb::class.java, NOMBRE)
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
    }
}
