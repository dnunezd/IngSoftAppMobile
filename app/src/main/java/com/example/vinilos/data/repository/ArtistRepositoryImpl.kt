package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.ArtistDao
import com.example.vinilos.data.local.ArtistDetailEntity
import com.example.vinilos.data.local.ArtistEntity
import com.example.vinilos.data.model.ArtistAlbum
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.model.ArtistMember
import com.example.vinilos.data.model.ArtistPrize
import com.example.vinilos.data.model.formatearFechaONula
import com.example.vinilos.data.remote.ArtistApi
import com.example.vinilos.data.remote.ArtistDetailDto
import com.example.vinilos.data.remote.IdAlbumDto
import com.example.vinilos.data.remote.PremiacionArtistaDto
import com.example.vinilos.data.remote.AwardDto
import com.example.vinilos.data.remote.ResourceNotFound
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.remote.conVersion
import com.google.gson.JsonParseException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

/**
 * Backend artists. With [artistaDao], the listing and each opened detail are also stored on the
 * device (add-cache-consultas); without it, it behaves as before.
 * Retrofit and Room do their I/O on their own threads; what runs on [dispatcher] (`Dispatchers.Default`
 * by default) is CPU work: converting, sorting and validating dates. This way it can be called from the
 * main thread without blocking it.
 */
class ArtistRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: ArtistApi = adaptador.api(),
    private val artistaDao: ArtistDao? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ArtistRepository {

    /** Protects the comparison, the write and the update of whatever follows (optimizar-cache-e-imagenes, D4). */
    private val escritura = Mutex()
    private val copiaListado = CopiaTabla<Pair<ArtistType, Int>, ArtistEntity> { it.tipo to it.id }
    private val copiaDetalles = CopiaTabla<Pair<ArtistType, Int>, ArtistDetailEntity> { it.tipo to it.id }
    private var versionListado: String? = null
    private val versionesDetalle = Versiones<Pair<ArtistType, Int>>()

    /**
     * Musicians and bands in a single listing sorted by name. The two queries run in parallel
     * inside a single call to the adapter: if either fails, the result is the load error,
     * because a partial listing would fail to show both. If it succeeds, what's stored is left equal to the
     * response (D4); its version combines the musicians' and the bands' versions.
     */
    override suspend fun getArtists(): Result<List<Artist>> = withContext(dispatcher) {
        adaptador.call {
            coroutineScope {
                val musicos = async { api.getMusicians().conVersion() }
                val bandas = async { api.getBands().conVersion() }
                val deMusicos = musicos.await()
                val deBandas = bandas.await()
                val artistas = deMusicos.valor.mapNotNull { artista(it.id, it.name, it.image, ArtistType.MUSICO) } +
                    deBandas.valor.mapNotNull { artista(it.id, it.name, it.image, ArtistType.BANDA) }
                artistas to versionCompuesta(deMusicos.version, deBandas.version)
            }
        }.map { (artistas, version) -> artistas.sortedWith(compareBy(ordenAlfabetico()) { it.name }) to version }
            .onSuccess { (artistas, version) -> saveList(artistas, version) }
            .map { (artistas, _) -> artistas }
    }

    override suspend fun getCachedArtists(): List<Artist> = withContext(dispatcher) {
        leer(siNoHay = emptyList()) {
            todos().map { Artist(it.id, it.nombre, it.imagen, it.tipo) }.sortedWith(compareBy(ordenAlfabetico()) { it.name })
        }
    }

    override suspend fun getCachedDetail(tipo: ArtistType, id: Int): ArtistDetail? = withContext(dispatcher) {
        leer(siNoHay = null) { detalle(tipo, id)?.aModelo() }
    }

    /** Reading what's stored: without a DAO or if storage fails, it's as if there were nothing. */
    private suspend fun <T> leer(siNoHay: T, bloque: suspend ArtistDao.() -> T): T {
        val dao = artistaDao ?: return siNoHay
        return try {
            dao.bloque()
        } catch (e: SQLiteException) {
            siNoHay
        }
    }

    // ---------- Single write point (optimizar-cache-e-imagenes, D4 and D5) ----------

    private suspend fun saveList(artistas: List<Artist>, version: String?) = escribir {
        if (version != null && version == versionListado) return@escribir
        val cambios = copiaListado.cambiosPara(artistas.map { ArtistEntity(it.type, it.id, it.name, it.image) })
        if (!cambios.vacios) {
            aplicarListado(cambios)
            copiaListado.aplicar(cambios)
        }
        versionListado = version
    }

    private suspend fun saveDetailIfChanged(detalle: ArtistDetailEntity, version: String?) = escribir {
        val clave = detalle.tipo to detalle.id
        if (versionesDetalle.igual(clave, version)) return@escribir
        if (copiaDetalles.fila(clave) != detalle) {
            saveDetail(detalle)
            copiaDetalles.guardada(detalle)
        }
        versionesDetalle.anotar(clave, version)
    }

    private suspend fun borrarDetalleGuardado(tipo: ArtistType, id: Int) = escribir {
        borrarDetalle(tipo, id)
        copiaDetalles.borrada(tipo to id)
        versionesDetalle.descartar(tipo to id)
    }

    /**
     * Runs [bloque] with [escritura] held. If the block does not finish normally, it's unknown what ended up in
     * Room, so whatever was remembered is discarded (rule 2 of D4). A storage failure does not change the
     * query's result; the cancellation and programming errors are rethrown.
     */
    private suspend fun escribir(bloque: suspend ArtistDao.() -> Unit) {
        val dao = artistaDao ?: return
        escritura.withLock {
            try {
                dao.bloque()
            } catch (e: Throwable) {
                copiaListado.descartar()
                copiaDetalles.descartar()
                versionListado = null
                versionesDetalle.descartarTodas()
                if (e !is SQLiteException) throw e
            }
        }
    }

    private fun ArtistDetail.aEntidad() =
        ArtistDetailEntity(tipo, id, nombre, imagen, descripcion, fecha, integrantes, albumes, premios)

    private fun ArtistDetailEntity.aModelo() =
        ArtistDetail(id, nombre, imagen, descripcion, tipo, fecha, integrantes, albumes, premios)

    /**
     * Artist detail according to its type. Prizes are only queried if the artist has any,
     * because the artist resource does not carry the prize. Everything goes in a single call to the adapter: any
     * failure is the load error, including the prizes' failure, because "No prizes" would be false.
     * If it succeeds, it stores the detail if it changed; if the backend confirms with a 404 that it no longer exists, it is removed
     * from storage and keeps returning the same error (D4). The version combines the artist's and the
     * prizes', or marks that they were not requested.
     */
    override suspend fun getDetail(tipo: ArtistType, id: Int): Result<ArtistDetail> =
        withContext(dispatcher) {
            adaptador.call {
                val respuesta = when (tipo) {
                    ArtistType.MUSICO -> api.getMusician(id).conVersion()
                    ArtistType.BANDA -> api.getBand(id).conVersion()
                }
                val premiaciones = if (respuesta.valor.performerPrizes.isNullOrEmpty()) {
                    null
                } else {
                    api.getPerformerPrizes().conVersion()
                }
                val versionPremiaciones = if (premiaciones == null) PREMIACIONES_NO_PEDIDAS else premiaciones.version
                val version = versionCompuesta(respuesta.version, versionPremiaciones)
                respuesta.valor.aDetalle(tipo, premiaciones?.valor.orEmpty()) to version
            }.onSuccess { (detalle, version) -> saveDetailIfChanged(detalle.aEntidad(), version) }
                .onFailure { if (it is ResourceNotFound) borrarDetalleGuardado(tipo, id) }
                .map { (detalle, _) -> detalle }
        }

    /**
     * Musician: `POST`, which adds without removing anyone. Band: the backend's `POST` leaves the band as the
     * album's only performer, so its current list is read and replaced by that list plus the new one with
     * `PUT`. If the full list can't be read, nothing is sent: an incomplete `PUT` would remove
     * albums from the band (add-agregar-album-artista, D1).
     */
    override suspend fun associateAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit> =
        withContext(dispatcher) {
            adaptador.call {
                when (tipo) {
                    ArtistType.MUSICO -> api.associateAlbumWithPerformer(idArtista, idAlbum).exitosa()
                    ArtistType.BANDA -> {
                        val actuales = api.getBand(idArtista).conVersion().valor.albums
                            ?: throw JsonParseException("La banda llegó sin su lista de álbumes")
                        val ids = actuales.map { it.id ?: throw JsonParseException("Un álbum de la banda llegó sin id") }
                        val nuevos = (ids + idAlbum).distinct().map(::IdAlbumDto)
                        api.replaceBandAlbums(idArtista, nuevos).exitosa()
                    }
                }
            }
        }

    /** Inside [ServiceAdapter.llamar]: an unsuccessful response is thrown so it gets translated to `LoadError`. */
    private fun Response<Unit>.exitosa() {
        if (!isSuccessful) throw HttpException(this)
    }

    /**
     * Tolerant conversion: only the artist's id and name are required, because the shared backend
     * has test data from other teams and a bad element should not bring down the whole screen.
     */
    private fun ArtistDetailDto.aDetalle(tipo: ArtistType, premiaciones: List<AwardDto>) = ArtistDetail(
        id = id ?: throw JsonParseException("El artista no tiene id"),
        nombre = name.conTexto() ?: throw JsonParseException("El artista no tiene nombre"),
        imagen = image,
        descripcion = description.conTexto(),
        tipo = tipo,
        fecha = fechaValida(if (tipo == ArtistType.MUSICO) birthDate else creationDate),
        integrantes = musicians.orEmpty().mapNotNull { dto ->
            ArtistMember(
                dto.id ?: return@mapNotNull null,
                dto.name.conTexto() ?: return@mapNotNull null,
                dto.image
            )
        },
        albumes = albums.orEmpty().mapNotNull { dto ->
            val fecha = fechaValida(dto.releaseDate)
            ArtistAlbum(
                dto.id ?: return@mapNotNull null,
                dto.name.conTexto() ?: return@mapNotNull null,
                dto.cover,
                fecha?.take(4)?.takeIf { it.all(Char::isDigit) }
            ) to fecha
        }.masRecientesPrimero(),
        premios = premios(performerPrizes.orEmpty(), premiaciones)
    )

    private fun premios(
        delArtista: List<PremiacionArtistaDto>,
        premiaciones: List<AwardDto>
    ): List<ArtistPrize> {
        val premioPorPremiacion = premiaciones.associateBy { it.id }
        return delArtista.mapNotNull { dto ->
            val premio = premioPorPremiacion[dto.id]?.prize ?: return@mapNotNull null
            val fecha = fechaValida(dto.premiationDate)
            ArtistPrize(
                dto.id ?: return@mapNotNull null,
                premio.name.conTexto() ?: return@mapNotNull null,
                premio.organization.conTexto() ?: return@mapNotNull null,
                fecha
            ) to fecha
        }.masRecientesPrimero()
    }

    private fun String?.conTexto(): String? = this?.takeIf(String::isNotBlank)

    /**
     * The date is stored in ISO and the screen formats it in the app's language: this way what's stored works in
     * any language, even offline. One that's missing or can't be parsed is treated as missing.
     */
    private fun fechaValida(fechaIso: String?): String? = fechaIso?.takeIf { formatearFechaONula(it) != null }

    /** Sorts by the ISO date that accompanies each element, with those without a date at the end. */
    private fun <T> List<Pair<T, String?>>.masRecientesPrimero(): List<T> =
        sortedWith(
            compareBy<Pair<T, String?>> { it.second == null }.thenByDescending { it.second?.take(LARGO_DIA_ISO) }
        ).map { it.first }

    private fun artista(id: Int?, nombre: String?, imagen: String?, tipo: ArtistType): Artist? {
        id ?: return null
        nombre ?: return null
        return Artist(id, nombre, imagen, tipo)
    }

    private companion object {
        const val LARGO_DIA_ISO = 10 // "yyyy-MM-dd"

        /** Part of the detail's version when the artist has no prizes and `/performerprizes` wasn't requested. */
        const val PREMIACIONES_NO_PEDIDAS = "sin-premiaciones"
    }
}
