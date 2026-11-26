package com.example.vinilos.data.local

import androidx.room.TypeConverter
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.Comment
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.ArtistPrize
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

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

    private fun <T> leer(json: String, tipo: Type): T = gson.fromJson(json, tipo)

    @TypeConverter fun deTrackes(valor: List<Track>): String = gson.toJson(valor)
    @TypeConverter fun aTrackes(json: String): List<Track> = leer(json, TIPO_CANCIONES)

    @TypeConverter fun deArtistas(valor: List<Artista>): String = gson.toJson(valor)
    @TypeConverter fun aArtistas(json: String): List<Artista> = leer(json, TIPO_ARTISTAS)

    @TypeConverter fun deComments(valor: List<Comment>): String = gson.toJson(valor)
    @TypeConverter fun aComments(json: String): List<Comment> = leer(json, TIPO_COMENTARIOS)

    @TypeConverter fun deIntegrantes(valor: List<IntegranteArtista>): String = gson.toJson(valor)
    @TypeConverter fun aIntegrantes(json: String): List<IntegranteArtista> = leer(json, TIPO_INTEGRANTES)

    @TypeConverter fun deAlbumesArtista(valor: List<AlbumArtista>): String = gson.toJson(valor)
    @TypeConverter fun aAlbumesArtista(json: String): List<AlbumArtista> = leer(json, TIPO_ALBUMES_ARTISTA)

    @TypeConverter fun dePrizes(valor: List<ArtistPrize>): String = gson.toJson(valor)
    @TypeConverter fun aPrizes(json: String): List<ArtistPrize> = leer(json, TIPO_PREMIOS)

    /**
     * El tipo de cada lista se crea una sola vez, en lugar de un `TypeToken` nuevo en cada lectura
     * (optimizar-cache-e-imagenes, D8). El JSON guardado no cambia.
     */
    private companion object {
        val TIPO_CANCIONES: Type = object : TypeToken<List<Track>>() {}.type
        val TIPO_ARTISTAS: Type = object : TypeToken<List<Artista>>() {}.type
        val TIPO_COMENTARIOS: Type = object : TypeToken<List<Comment>>() {}.type
        val TIPO_INTEGRANTES: Type = object : TypeToken<List<IntegranteArtista>>() {}.type
        val TIPO_ALBUMES_ARTISTA: Type = object : TypeToken<List<AlbumArtista>>() {}.type
        val TIPO_PREMIOS: Type = object : TypeToken<List<ArtistPrize>>() {}.type
    }
}
