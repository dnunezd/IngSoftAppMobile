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
import java.io.IOException

class AlbumNoDisponible(causa: Throwable? = null) : Exception("El álbum ya no está disponible", causa)

open class AlbumRepository(
    private val adaptador: ServiceAdapter,
    private val api: AlbumApi = adaptador.api()
) {

    open suspend fun obtenerAlbum(id: Int): Result<Album> {
        val respuesta = adaptador.llamar {
            val respuestaHttp = api.obtenerAlbum(id)
            if (!respuestaHttp.isSuccessful) throw HttpException(respuestaHttp)
            requerido(respuestaHttp.body(), "album").aModelo()
        }
        val error = respuesta.exceptionOrNull()
        if (error is HttpException) {
            return Result.failure(
                if (error.code() == 404) AlbumNoDisponible(error) else ErrorDeCarga(error)
            )
        }
        if (error is IOException || error is JsonParseException) {
            return Result.failure(ErrorDeCarga(error))
        }
        if (error != null) throw error
        return respuesta
    }

    private fun AlbumDto.aModelo() = Album(
        id = requerido(id, "id"),
        nombre = requerido(name, "name"),
        portada = cover,
        descripcion = requerido(description, "description"),
        genero = requerido(genre, "genre"),
        disquera = requerido(recordLabel, "recordLabel"),
        fechaLanzamiento = requerido(releaseDate, "releaseDate"),
        canciones = tracks.orEmpty().map {
            Cancion(requerido(it.name, "track.name"), requerido(it.duration, "track.duration"))
        },
        artistas = performers.orEmpty().map {
            Artista(requerido(it.name, "performer.name"), it.image)
        },
        comentarios = comments.orEmpty().map {
            Comentario(requerido(it.description, "comment.description"), it.rating)
        }
    )

    private fun <T : Any> requerido(valor: T?, campo: String): T =
        valor ?: throw JsonParseException("Falta el campo $campo del álbum")
}
