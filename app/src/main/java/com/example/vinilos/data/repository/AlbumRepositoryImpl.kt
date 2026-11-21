package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.AlbumDao
import com.example.vinilos.data.local.AlbumDetalleEntity
import com.example.vinilos.data.local.AlbumEntity
import com.example.vinilos.data.local.CambiosDeTabla
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.model.fechaIsoUtc
import com.example.vinilos.data.model.formatearFechaLanzamiento
import com.example.vinilos.data.remote.AlbumApi
import com.example.vinilos.data.remote.AlbumDto
import com.example.vinilos.data.remote.ArtistaDto
import com.example.vinilos.data.remote.CrearAlbumDto
import com.example.vinilos.data.remote.CrearCancionDto
import com.example.vinilos.data.remote.RecursoNoEncontrado
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
 * Álbumes del backend. Con [albumDao], el catálogo (add-cache-catalogo) y el detalle de cada álbum
 * (add-cache-consultas) además se guardan en el dispositivo; sin él, se comporta como antes, que es lo
 * que usan las pruebas que no tienen `Context`.
 *
 * Retrofit y Room hacen su I/O en sus propios hilos; lo que corre en [dispatcher] (`Dispatchers.Default`
 * por defecto) es el trabajo de CPU: convertir, ordenar y validar fechas. Así se puede llamar desde el
 * hilo principal sin bloquearlo.
 *
 * Solo se escribe lo que cambió (optimizar-cache-e-imagenes, D4 y D5): el repositorio recuerda en memoria lo
 * último que escribió en cada tabla y la versión (`ETag`) de cada consulta, y toda escritura pasa por
 * [escribir], que protege esa memoria con [escritura].
 */
class AlbumRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: AlbumApi = adaptador.api(),
    private val albumDao: AlbumDao? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val trackApi: TrackApi = adaptador.api()
) : AlbumRepository {

    /** Protege la comparación, la escritura y la actualización de lo que sigue. */
    private val escritura = Mutex()
    private val copiaCatalogo = CopiaTabla<Int, AlbumEntity> { it.id }
    private val copiaDetalles = CopiaTabla<Int, AlbumDetalleEntity> { it.id }
    private var versionCatalogo: String? = null
    private val versionesDetalle = Versiones<Int>()

    /**
     * Detalle del álbum (HU02). Si sale bien, guarda su detalle si cambió; si el backend confirma con
     * un 404 que ya no existe, lo borra de lo guardado (add-cache-consultas, D4).
     */
    override suspend fun obtenerAlbum(id: Int): Result<Album> = withContext(dispatcher) {
        val respuesta = adaptador.llamar {
            val conVersion = api.obtenerAlbum(id).conVersion()
            conVersion.valor.aModelo() to conVersion.version
        }
        val error = respuesta.exceptionOrNull()
        if (error is RecursoNoEncontrado) {
            borrarAlbumGuardado(id)
            Result.failure(AlbumNoDisponible(error.cause))
        } else {
            respuesta.onSuccess { (album, version) -> guardarDetalleSiCambio(album.aEntidad(), version) }
                .map { (album, _) -> album }
        }
    }

    /**
     * Catálogo (HU01) ordenado por nombre. A diferencia del detalle, la conversión es tolerante:
     * un álbum sin id, nombre o género se descarta sin afectar a los demás. Si la consulta sale bien,
     * lo guardado del catálogo y de los detalles queda igual a la respuesta: `GET /albums` ya trae
     * canciones, intérpretes y comentarios (add-cache-consultas, D4).
     */
    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> = withContext(dispatcher) {
        adaptador.llamar {
            val respuesta = api.obtenerAlbumes().conVersion()
            val albumes = respuesta.valor.mapNotNull { it.aResumen() }.sortedWith(ordenCatalogo())
            Triple(albumes, respuesta.valor.mapNotNull { it.aDetalleSiEsValido() }, respuesta.version)
        }.onSuccess { (albumes, detalles, version) -> guardarCatalogo(albumes, detalles, version) }
            .map { (albumes, _, _) -> albumes }
    }

    // ---------- Punto único de escritura (optimizar-cache-e-imagenes, D4 y D5) ----------

    private suspend fun guardarCatalogo(albumes: List<AlbumResumen>, detalles: List<Album>, version: String?) =
        escribir {
            if (version != null && version == versionCatalogo) return@escribir
            val cambiosCatalogo = copiaCatalogo.cambiosPara(albumes.map { it.aEntidad() })
            val cambiosDetalles = copiaDetalles.cambiosPara(detalles.map { it.aEntidad() })
            if (!cambiosCatalogo.vacios || !cambiosDetalles.vacios) {
                aplicarCatalogo(cambiosCatalogo, cambiosDetalles)
                copiaCatalogo.aplicar(cambiosCatalogo)
                copiaDetalles.aplicar(cambiosDetalles)
                // Regla 3 de D4: un detalle que cambió deja sin valor la versión de su propia consulta.
                if (cambiosDetalles.reemplazarTodo) {
                    versionesDetalle.descartarTodas()
                } else {
                    (cambiosDetalles.guardar.map { it.id } + cambiosDetalles.borrar).forEach(versionesDetalle::descartar)
                }
            }
            versionCatalogo = version
        }

    private suspend fun guardarDetalleSiCambio(detalle: AlbumDetalleEntity, version: String?) = escribir {
        if (versionesDetalle.igual(detalle.id, version)) return@escribir
        if (copiaDetalles.fila(detalle.id) != detalle) {
            guardarDetalle(detalle)
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
     * Ejecuta [bloque] con [escritura] tomado. Si el bloque no termina normalmente, no se sabe qué quedó en
     * Room, así que se descarta lo que se recordaba y la siguiente escritura reemplaza todo (regla 2 de D4).
     * Un fallo del almacenamiento no cambia el resultado de la consulta, que ya llegó del backend; la
     * cancelación y los errores de programación se relanzan.
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
     * Último catálogo guardado en el dispositivo, en el mismo orden que la consulta. Vacío si no hay
     * caché o si no se pudo leer: la caché es una mejora, nunca una condición (add-cache-catalogo, D7).
     */
    override suspend fun obtenerAlbumesGuardados(): List<AlbumResumen> = withContext(dispatcher) {
        leer(siNoHay = emptyList()) { todos().map { it.aResumen() }.sortedWith(ordenCatalogo()) }
    }

    override suspend fun obtenerAlbumGuardado(id: Int): Album? = withContext(dispatcher) {
        leer(siNoHay = null) { detalle(id)?.aModelo() }
    }

    /** Lectura de lo guardado: sin DAO o si falla el almacenamiento, es como si no hubiera nada. */
    private suspend fun <T> leer(siNoHay: T, bloque: suspend AlbumDao.() -> T): T {
        val dao = albumDao ?: return siNoHay
        return try {
            dao.bloque()
        } catch (e: SQLiteException) {
            siNoHay
        }
    }

    private fun AlbumResumen.aEntidad() = AlbumEntity(id, nombre, portada, genero, artista, anio)

    private fun AlbumEntity.aResumen() = AlbumResumen(id, nombre, portada, genero, artista, anio)

    private fun Album.aEntidad() = AlbumDetalleEntity(
        id, nombre, portada, descripcion, genero, disquera, fechaLanzamiento, canciones, artistas, comentarios
    )

    private fun AlbumDetalleEntity.aModelo() = Album(
        id, nombre, portada, descripcion, genero, disquera, fechaLanzamiento, canciones, artistas, comentarios
    )

    /** Detalle para guardar desde el catálogo: un álbum que no pasa la validación estricta no se guarda. */
    private fun AlbumDto.aDetalleSiEsValido(): Album? =
        try {
            aModelo()
        } catch (e: JsonParseException) {
            null
        }

    /**
     * Crea el álbum (HU07) y devuelve su id, que el catálogo usa para marcarlo como nuevo. El álbum que
     * devuelve el backend se guarda de inmediato, para que el catálogo lo muestre sin esperar `GET /albums`
     * (optimizar-cache-e-imagenes, D7). Si la respuesta no trae lo necesario o no se puede guardar, la
     * creación igual se informa como exitosa: el álbum llega con la siguiente consulta del catálogo.
     */
    override suspend fun crearAlbum(album: AlbumNuevo): Result<Int> = withContext(dispatcher) {
        adaptador.llamar {
            val creado = api.crearAlbum(album.aDto())
            requerido(creado.id, "id") to creado
        }.onSuccess { (_, creado) -> guardarCreado(creado) }
            .map { (id, _) -> id }
    }

    override suspend fun agregarCancion(albumId: Int, cancion: Cancion): Result<Unit> =
        withContext(dispatcher) {
            val resultado = adaptador.llamar {
                val respuesta = trackApi.agregarCancion(
                    albumId,
                    CrearCancionDto(cancion.nombre, cancion.duracion)
                )
                if (!respuesta.isSuccessful) throw HttpException(respuesta)
            }
            val error = resultado.exceptionOrNull()
            if (error is RecursoNoEncontrado) {
                Result.failure(AlbumNoDisponible(error.cause))
            } else {
                resultado
            }
        }

    private suspend fun guardarCreado(creado: AlbumDto) {
        val fila = creado.aResumen()?.aEntidad() ?: return
        val detalle = creado.aDetalleSiEsValido()?.aEntidad()
        escribir {
            aplicarCatalogo(
                CambiosDeTabla.diferencias(listOf(fila), emptyList()),
                CambiosDeTabla.diferencias(listOfNotNull(detalle), emptyList())
            )
            copiaCatalogo.guardada(fila)
            detalle?.let(copiaDetalles::guardada)
            // Regla 3 de D4: si el álbum se borra en el backend, la versión anterior del catálogo vuelve.
            versionCatalogo = null
            versionesDetalle.descartar(fila.id)
        }
    }

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
