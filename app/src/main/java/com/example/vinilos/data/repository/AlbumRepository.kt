package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.remote.AlbumApi
import com.example.vinilos.data.remote.AlbumDto
import com.example.vinilos.data.remote.ServiceAdapter
import com.google.gson.JsonParseException
import retrofit2.HttpException

class AlbumNoDisponible(causa: Throwable? = null) : Exception("El álbum ya no está disponible", causa)

open class AlbumRepository(private val adaptador: ServiceAdapter) {

    private val api = adaptador.api<AlbumApi>()

    open suspend fun obtenerAlbum(id: Int): Result<Album> {
        val respuesta = adaptador.llamar { api.obtenerAlbum(id) }
        val error = respuesta.exceptionOrNull()
        if (error is HttpException) {
            return Result.failure(
                if (error.code() == 404) AlbumNoDisponible(error) else ErrorDeCarga(error)
            )
        }
        return respuesta.map { it.aModelo() }
    }

    private fun AlbumDto.aModelo() = Album(
        id = requerido(id, "id"),
        nombre = requerido(name, "name"),
        portada = cover,
        descripcion = requerido(description, "description"),
        genero = requerido(genre, "genre"),
        disquera = requerido(recordLabel, "recordLabel"),
        fechaLanzamiento = requerido(releaseDate, "releaseDate"),
        canciones = requerido(tracks, "tracks").map {
            Cancion(requerido(it.name, "track.name"), requerido(it.duration, "track.duration"))
        },
        artistas = requerido(performers, "performers").map {
            Artista(requerido(it.name, "performer.name"), it.image)
        },
        comentarios = requerido(comments, "comments").map {
            Comentario(requerido(it.description, "comment.description"), it.rating)
        }
    )

    private fun <T : Any> requerido(valor: T?, campo: String): T =
        valor ?: throw JsonParseException("Falta el campo $campo del álbum")
}
