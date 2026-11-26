package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.AlbumDetailEntity
import com.example.vinilos.data.local.AlbumEntity
import com.example.vinilos.data.local.TableChanges
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.Comment
import com.example.vinilos.data.model.fechaIsoUtc
import com.example.vinilos.data.model.formatearReleaseDate
import com.example.vinilos.data.remote.AlbumApi
import com.example.vinilos.data.remote.AlbumDto
import com.example.vinilos.data.remote.ArtistaDto
import com.example.vinilos.data.remote.CreateAlbumDto
import com.example.vinilos.data.remote.CrearTrackDto
import com.example.vinilos.data.remote.ResourceNotFound
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.remote.TrackApi
import com.example.vinilos.data.remote.conVersion
import com.google.gson.JsonParseException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.ParseException

/**
 * Backend albums. With [albumDao], the catalog (add-cache-catalogo) and each album's detail
 * (add-cache-consultas) are also stored on the device; without it, it behaves as before, which is what
 * the tests that have no `Context` use.
 *
 * Retrofit and Room do their I/O on their own threads; what runs on [dispatcher] (`Dispatchers.Default`
 * by default) is CPU work: converting, sorting and validating dates. This way it can be called from the
 * main thread without blocking it.
 *
 * Only what changed is written (optimizar-cache-e-imagenes, D4 and D5): the repository remembers in memory the
 * last thing it wrote to each table and the version (`ETag`) of each query, and every write goes through
 * [escribir], which protects that memory with [escritura].
 */
class AlbumRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: AlbumApi = adaptador.api(),
    private val albumDao: AlbumDao? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val trackApi: TrackApi = adaptador.api()
) : AlbumRepository {

    /** Protects the comparison, the write and the update of whatever follows. */
    private val escritura = Mutex()
    private val copiaCatalogo = CopiaTabla<Int, AlbumEntity> { it.id }
    private val copiaDetalles = CopiaTabla<Int, AlbumDetailEntity> { it.id }
    private var versionCatalogo: String? = null
    private val versionesDetalle = Versiones<Int>()

    /**
     * Album detail (HU02). If it succeeds, it stores its detail if it changed; if the backend confirms with
     * a 404 that it no longer exists, it is removed from storage (add-cache-consultas, D4).
     */
    override suspend fun getAlbum(id: Int): Result<Album> = withContext(dispatcher) {
        val respuesta = adaptador.call {
            val conVersion = api.getAlbum(id).conVersion()
            conVersion.valor.aModelo() to conVersion.version
        }
        val error = respuesta.exceptionOrNull()
        if (error is ResourceNotFound) {
            borrarAlbumGuardado(id)
            Result.failure(AlbumNotAvailable(error.cause))
        } else {
            respuesta.onSuccess { (album, version) -> saveDetailIfChanged(album.aEntidad(), version) }
                .map { (album, _) -> album }
        }
    }

    /**
     * Catalog (HU01) sorted by name. Unlike the detail, the conversion is tolerant:
     * an album without id, name, or genre is discarded without affecting the others. If the query succeeds,
     * the stored catalog and details are left equal to the response: `GET /albums` already brings
     * tracks, performers, and comments (add-cache-consultas, D4).
     */
    override suspend fun getAlbums(): Result<List<AlbumSummary>> = withContext(dispatcher) {
        adaptador.call {
            val respuesta = api.getAlbums().conVersion()
            val albumes = respuesta.valor.mapNotNull { it.aResumen() }.sortedWith(ordenCatalogo())
            Triple(albumes, respuesta.valor.mapNotNull { it.aDetalleSiEsValido() }, respuesta.version)
        }.onSuccess { (albumes, detalles, version) -> saveCatalog(albumes, detalles, version) }
            .map { (albumes, _, _) -> albumes }
    }

    // ---------- Single write point (optimizar-cache-e-imagenes, D4 and D5) ----------

    private suspend fun saveCatalog(albumes: List<AlbumSummary>, detalles: List<Album>, version: String?) =
        escribir {
            if (version != null && version == versionCatalogo) return@escribir
            val cambiosCatalogo = copiaCatalogo.cambiosPara(albumes.map { it.aEntidad() })
            val cambiosDetalles = copiaDetalles.cambiosPara(detalles.map { it.aEntidad() })
            if (!cambiosCatalogo.vacios || !cambiosDetalles.vacios) {
                aplicarCatalogo(cambiosCatalogo, cambiosDetalles)
                copiaCatalogo.aplicar(cambiosCatalogo)
                copiaDetalles.aplicar(cambiosDetalles)
                // Rule 3 of D4: a changed detail invalidates the version of its own query.
                if (cambiosDetalles.reemplazarTodo) {
                    versionesDetalle.descartarTodas()
                } else {
                    (cambiosDetalles.save.map { it.id } + cambiosDetalles.borrar).forEach(versionesDetalle::descartar)
                }
            }
            versionCatalogo = version
        }

    private suspend fun saveDetailIfChanged(detalle: AlbumDetailEntity, version: String?) = escribir {
        if (versionesDetalle.igual(detalle.id, version)) return@escribir
        if (copiaDetalles.fila(detalle.id) != detalle) {
            saveDetail(detalle)
            copiaDetalles.guardada(detalle)
            versionCatalogo = null
        }
        versionesDetalle.anotar(detalle.id, version)
    }

    private suspend fun borrarAlbumGuardado(id: Int) = escribir {
        borrarAlbum(id)
        copiaCatalogo.borrada(id)
        copiaDetalles.borrada(id)
        versionesDetalle.descartar(id)
        versionCatalogo = null
    }

    /**
     * Runs [bloque] with [escritura] held. If the block does not finish normally, it's unknown what ended up in
     * Room, so whatever was remembered is discarded and the next write replaces everything (rule 2 of D4).
     * A storage failure does not change the query's result, which already arrived from the backend; the
     * cancellation and programming errors are rethrown.
     */
    private suspend fun escribir(bloque: suspend AlbumDao.() -> Unit) {
        val dao = albumDao ?: return
        escritura.withLock {
            try {
                dao.bloque()
            } catch (e: Throwable) {
                olvidarLoEscrito()
                if (e !is SQLiteException) throw e
            }
        }
    }

    private fun olvidarLoEscrito() {
        copiaCatalogo.descartar()
        copiaDetalles.descartar()
        versionCatalogo = null
        versionesDetalle.descartarTodas()
    }

    /**
     * Last catalog stored on the device, in the same order as the query. Empty if there's no
     * cache or it couldn't be read: the cache is an enhancement, never a precondition (add-cache-catalogo, D7).
     */
    override suspend fun getCachedAlbums(): List<AlbumSummary> = withContext(dispatcher) {
        leer(siNoHay = emptyList()) { todos().map { it.aResumen() }.sortedWith(ordenCatalogo()) }
    }

    override suspend fun getCachedAlbum(id: Int): Album? = withContext(dispatcher) {
        leer(siNoHay = null) { detalle(id)?.aModelo() }
    }

    /** Reading what's stored: without a DAO or if storage fails, it's as if there were nothing. */
    private suspend fun <T> leer(siNoHay: T, bloque: suspend AlbumDao.() -> T): T {
        val dao = albumDao ?: return siNoHay
        return try {
            dao.bloque()
        } catch (e: SQLiteException) {
            siNoHay
        }
    }

    private fun AlbumSummary.aEntidad() = AlbumEntity(id, nombre, portada, genero, artista, anio)

    private fun AlbumEntity.aResumen() = AlbumSummary(id, nombre, portada, genero, artista, anio)

    private fun Album.aEntidad() = AlbumDetailEntity(
        id, nombre, portada, descripcion, genero, disquera, fechaLanzamiento, canciones, artistas, comentarios
    )

    private fun AlbumDetailEntity.aModelo() = Album(
        id, nombre, portada, descripcion, genero, disquera, fechaLanzamiento, canciones, artistas, comentarios
    )

    /** Detail to store from the catalog: an album that doesn't pass the strict validation is not stored. */
    private fun AlbumDto.aDetalleSiEsValido(): Album? =
        try {
            aModelo()
        } catch (e: JsonParseException) {
            null
        }

    /**
     * Creates the album (HU07) and returns its id, which the catalog uses to mark it as new. The album
     * returned by the backend is stored right away, so the catalog shows it without waiting for `GET /albums`
     * (optimizar-cache-e-imagenes, D7). If the response is missing something needed or it can't be stored, the
     * creation is still reported as successful: the album arrives with the next catalog query.
     */
    override suspend fun createAlbum(album: NewAlbum): Result<Int> = withContext(dispatcher) {
        adaptador.call {
            val creado = api.createAlbum(album.aDto())
            requerido(creado.id, "id") to creado
        }.onSuccess { (_, creado) -> saveCreated(creado) }
            .map { (id, _) -> id }
    }

    /**
     * Associates a track with the album (HU08). The backend does not return the updated album, so, unlike
     * [crearAlbum], there is nothing to store "as-is"; still, if it succeeds, the stored detail is
     * updated right away with the new track, so as not to wait for the next `GET
     * /albums/{id}` (same D7 principle as [guardarCreado]).
     */
    override suspend fun addTrack(albumId: Int, cancion: Track): Result<Unit> =
        withContext(dispatcher) {
            val resultado = adaptador.call {
                val respuesta = trackApi.addTrack(
                    albumId,
                    CrearTrackDto(cancion.nombre, cancion.duracion)
                )
                if (!respuesta.isSuccessful) throw HttpException(respuesta)
            }
            val error = resultado.exceptionOrNull()
            if (error is ResourceNotFound) {
                Result.failure(AlbumNotAvailable(error.cause))
            } else {
                resultado.onSuccess { saveAddedTrack(albumId, cancion) }
            }
        }

    private suspend fun saveCreated(creado: AlbumDto) {
        val fila = creado.aResumen()?.aEntidad() ?: return
        val detalle = creado.aDetalleSiEsValido()?.aEntidad()
        escribir {
            aplicarCatalogo(
                TableChanges.diferencias(listOf(fila), emptyList()),
                TableChanges.diferencias(listOfNotNull(detalle), emptyList())
            )
            copiaCatalogo.guardada(fila)
            detalle?.let(copiaDetalles::guardada)
            // Rule 3 of D4: if the album is deleted on the backend, the catalog's previous version comes back.
            versionCatalogo = null
            versionesDetalle.descartar(fila.id)
        }
    }

    /**
     * If the album is stored, it adds the new track to it (D7); if not, it does nothing: there is no detail
     * to fix up and the next `GET /albums/{id}` brings it in full anyway.
     */
    private suspend fun saveAddedTrack(albumId: Int, cancion: Track) = escribir {
        detalle(albumId)?.let { actual ->
            val actualizado = actual.copy(canciones = actual.canciones + cancion)
            saveDetail(actualizado)
            copiaDetalles.guardada(actualizado)
            // The remembered version no longer corresponds to what's stored (same as D4 when the detail changes).
            versionesDetalle.descartar(albumId)
        }
    }

    private fun NewAlbum.aDto() = CreateAlbumDto(
        name = nombre,
        cover = portada,
        releaseDate = fechaIsoUtc(fechaLanzamientoMillis),
        description = descripcion,
        genre = genero,
        recordLabel = disquera
    )

    private fun AlbumDto.aResumen(): AlbumSummary? {
        return AlbumSummary(
            id = id ?: return null,
            nombre = name ?: return null,
            portada = cover,
            genero = genre ?: return null,
            artista = nombresDeArtistas(performers),
            anio = anioDe(releaseDate)
        )
    }

    /** Non-empty names joined with ", "; with none, an empty string (never null). */
    private fun nombresDeArtistas(artistas: List<ArtistaDto>?): String =
        artistas.orEmpty().mapNotNull { it.name?.takeIf(String::isNotBlank) }.joinToString(", ")

    /** Year of an ISO date; the date part is already the UTC day, so it does not depend on the time zone. */
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
            Track(requerido(it.name, "track.name"), requerido(it.duration, "track.duration"))
        },
        artistas = performers.orEmpty().map {
            Artista(requerido(it.name, "performer.name"), it.image)
        },
        comentarios = comments.orEmpty().map {
            Comment(requerido(it.description, "comment.description"), it.rating)
        }
    )

    private fun <T : Any> requerido(valor: T?, campo: String): T =
        valor ?: throw JsonParseException("Falta el campo $campo del álbum")

    /** Confirms that the date has a recognizable format before storing it unformatted. */
    private fun validarFecha(fechaIso: String): String {
        try {
            formatearReleaseDate(fechaIso)
        } catch (error: ParseException) {
            throw JsonParseException("Fecha de lanzamiento con formato inesperado: $fechaIso", error)
        }
        return fechaIso
    }
}
