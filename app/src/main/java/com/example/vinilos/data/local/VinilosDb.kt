package com.example.vinilos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Base local de la app. Por ahora solo guarda la caché del catálogo de álbumes (add-cache-catalogo). */
@Database(entities = [AlbumEntity::class], version = 1, exportSchema = true)
abstract class VinilosDb : RoomDatabase() {

    abstract fun albumDao(): AlbumDao

    companion object {
        private const val NOMBRE = "vinilos.db"

        /**
         * La base es una caché que la siguiente consulta vuelve a llenar, así que un cambio de esquema
         * la borra en lugar de migrar datos (design.md, D6).
         */
        fun crear(context: Context): VinilosDb =
            Room.databaseBuilder(context, VinilosDb::class.java, NOMBRE)
                .fallbackToDestructiveMigration()
                .build()
    }
}
