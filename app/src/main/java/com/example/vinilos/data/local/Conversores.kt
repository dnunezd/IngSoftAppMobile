package com.example.vinilos.data.local

import androidx.room.TypeConverter
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.PremioArtista
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Listas anidadas de los detalles guardadas como JSON (add-cache-consultas, D2): el detalle siempre se
 * lee y se reemplaza entero, así que no hacen falta tablas hijas.
 *
 * Gson ignora la nulabilidad de Kotlin: si una fila vieja no tiene un campo que el modelo declara no
 * nulo, se leería con `null`. Por eso, **cualquier cambio en [Cancion], [Artista], [Comentario],
 * [IntegranteArtista], [AlbumArtista] o [PremioArtista] debe subir la versión de [VinilosDb]**, lo que
 * borra lo guardado (migración destructiva). Si se activa la minificación, esas clases necesitan un
 * `-keep` (ver `proguard-rules.pro`).
 */
class Conversores {

    private val gson = Gson()

    private inline fun <reified T> leer(json: String): T = gson.fromJson(json, object : TypeToken<T>() {}.type)

    @TypeConverter fun deCanciones(valor: List<Cancion>): String = gson.toJson(valor)
    @TypeConverter fun aCanciones(json: String): List<Cancion> = leer(json)

    @TypeConverter fun deArtistas(valor: List<Artista>): String = gson.toJson(valor)
    @TypeConverter fun aArtistas(json: String): List<Artista> = leer(json)

    @TypeConverter fun deComentarios(valor: List<Comentario>): String = gson.toJson(valor)
    @TypeConverter fun aComentarios(json: String): List<Comentario> = leer(json)

    @TypeConverter fun deIntegrantes(valor: List<IntegranteArtista>): String = gson.toJson(valor)
    @TypeConverter fun aIntegrantes(json: String): List<IntegranteArtista> = leer(json)

    @TypeConverter fun deAlbumesArtista(valor: List<AlbumArtista>): String = gson.toJson(valor)
    @TypeConverter fun aAlbumesArtista(json: String): List<AlbumArtista> = leer(json)

    @TypeConverter fun dePremios(valor: List<PremioArtista>): String = gson.toJson(valor)
    @TypeConverter fun aPremios(json: String): List<PremioArtista> = leer(json)
}
