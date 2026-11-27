package com.example.vinilos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * App's local database: a read-only cache of backend queries. Version 1, the catalog
 * (add-cache-catalogo); version 2, collectors, album detail, artists and favorites
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
         * The database is a cache that the next query fills again, so a schema change wipes it
         * instead of migrating data (add-cache-catalogo D6, add-cache-consultas D1). Version 3 stores
 * the artist's dates in ISO instead of in Spanish (internacionalizar-ingles).
         */
        fun create(context: Context): VinilosDb =
            Room.databaseBuilder(context, VinilosDb::class.java, NOMBRE)
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
    }
}
