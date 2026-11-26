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
 * Nested lists of the details stored as JSON (add-cache-consultas, D2): the detail is always read and
 * replaced whole, so no child tables are needed.
 *
 * Gson ignores Kotlin's nullability: if an old row lacks a field that the model declares non-null,
 * it would be read as `null`. That's why **any change to [Cancion], [Artista], [Comentario],
 * [IntegranteArtista], [AlbumArtista] or [PremioArtista] must bump [VinilosDb]'s version**, which
 * wipes the stored data (destructive migration). If minification is enabled, those classes need a
 * `-keep` rule (see `proguard-rules.pro`).
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
     * The type of each list is created only once, instead of a new `TypeToken` on every read
     * (optimizar-cache-e-imagenes, D8). The stored JSON does not change.
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
