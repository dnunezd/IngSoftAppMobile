package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.AlbumEntity
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.model.fechaIsoUtc
import com.example.vinilos.data.remote.AlbumApi
import com.example.vinilos.data.remote.AlbumDto
import com.example.vinilos.data.remote.ArtistaDto
import com.example.vinilos.data.remote.CrearAlbumDto
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.ui.detalle.formatearFechaLanzamiento
import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.text.ParseException

/**
 * Álbumes del backend. Con [albumDao], el catálogo además se guarda en el dispositivo
 * (add-cache-catalogo); sin él, se comporta como antes, que es lo que usan las pruebas que no
 * tienen `Context`.
 */
class AlbumRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: AlbumApi = adaptador.api(),
    private val albumDao: AlbumDao? = null
) : AlbumRepository {

    override suspend fun obtenerAlbum(id: Int): Result<Album> {
        val respuesta = adaptador.llamar {
            val respuestaHttp = api.obtenerAlbum(id)
            if (!respuestaHttp.isSuccessful) throw HttpException(respuestaHttp)
            requerido(respuestaHttp.body(), "album").aModelo()
        }
        val error = respuesta.exceptionOrNull()
        if (error is ErrorDeCarga) {
            val causa = error.cause
            if (causa is HttpException && causa.code() == 404) {
                return Result.failure(AlbumNoDisponible(causa))
            }
        }
        return respuesta
    }

    /**
     * Catálogo (HU01) ordenado por nombre. A diferencia del detalle, la conversión es tolerante:
     * un álbum sin id, nombre o género se descarta sin afectar a los demás. Si la consulta sale bien,
     * la respuesta reemplaza el catálogo guardado.
     */
    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> =
        adaptador.llamar {
            api.obtenerAlbumes().mapNotNull { it.aResumen() }.sortedWith(ordenCatalogo)
        }.onSuccess { guardar(it) }

    /**
     * Último catálogo guardado en el dispositivo, en el mismo orden que la consulta. Vacío si no hay
     * caché o si no se pudo leer: la caché es una mejora, nunca una condición (design.md, D7).
     */
    override suspend fun obtenerAlbumesGuardados(): List<AlbumResumen> {
        val dao = albumDao ?: return emptyList()
        return try {
            dao.todos().map { it.aResumen() }.sortedWith(ordenCatalogo)
        } catch (e: SQLiteException) {
            emptyList()
        }
    }

    /** Un fallo al escribir no cambia el resultado de la consulta, que ya llegó bien. */
    private suspend fun guardar(albumes: List<AlbumResumen>) {
        val dao = albumDao ?: return
        try {
            dao.reemplazar(albumes.map { it.aEntidad() })
        } catch (e: SQLiteException) {
            // Se sigue mostrando la respuesta; la siguiente consulta exitosa vuelve a intentarlo.
        }
    }

    private fun AlbumResumen.aEntidad() = AlbumEntity(id, nombre, portada, genero, artista, anio)

    private fun AlbumEntity.aResumen() = AlbumResumen(id, nombre, portada, genero, artista, anio)

    /** Crea el álbum (HU07) y devuelve su id, que el catálogo usa para marcarlo como nuevo. */
    override suspend fun crearAlbum(album: AlbumNuevo): Result<Int> =
        adaptador.llamar { requerido(api.crearAlbum(album.aDto()).id, "id") }

    private fun AlbumNuevo.aDto() = CrearAlbumDto(
        name = nombre,
        cover = portada,
        releaseDate = fechaIsoUtc(fechaLanzamientoMillis),
        description = descripcion,
        genre = genero,
        recordLabel = disquera
    )

    private fun AlbumDto.aResumen(): AlbumResumen? {
        return AlbumResumen(
            id = id ?: return null,
            nombre = name ?: return null,
            portada = cover,
            genero = genre ?: return null,
            artista = nombresDeArtistas(performers),
            anio = anioDe(releaseDate)
        )
    }

    /** Nombres no vacíos unidos con ", "; sin ninguno, cadena vacía (nunca null). */
    private fun nombresDeArtistas(artistas: List<ArtistaDto>?): String =
        artistas.orEmpty().mapNotNull { it.name?.takeIf(String::isNotBlank) }.joinToString(", ")

    /** Año de una fecha ISO; la parte de fecha ya es el día UTC, así que no depende de la zona. */
    private fun anioDe(fechaIso: String?): String? =
        fechaIso?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }

    private fun AlbumDto.aModelo() = Album(
        id = requerido(id, "id"),
        nombre = requerido(name, "name"),
        portada = cover,
        descripcion = requerido(description, "description"),
        genero = requerido(genre, "genre"),
        disquera = requerido(recordLabel, "recordLabel"),
        fechaLanzamiento = validarFecha(requerido(releaseDate, "releaseDate")),
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

    /** Confirma que la fecha tenga un formato reconocible antes de guardarla sin formatear. */
    private fun validarFecha(fechaIso: String): String {
        try {
            formatearFechaLanzamiento(fechaIso)
        } catch (error: ParseException) {
            throw JsonParseException("Fecha de lanzamiento con formato inesperado: $fechaIso", error)
        }
        return fechaIso
    }
}
