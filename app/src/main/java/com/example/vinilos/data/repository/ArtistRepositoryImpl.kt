package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.ArtistDao
import com.example.vinilos.data.local.ArtistDetailEntity
import com.example.vinilos.data.local.ArtistEntity
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.ArtistPrize
import com.example.vinilos.data.model.formatearFechaONula
import com.example.vinilos.data.remote.ArtistApi
import com.example.vinilos.data.remote.ArtistDetailDto
import com.example.vinilos.data.remote.IdAlbumDto
import com.example.vinilos.data.remote.PremiacionArtistaDto
import com.example.vinilos.data.remote.PremiacionDto
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
 * Artistas del backend. Con [artistaDao], el listado y cada detalle abierto además se guardan en el
 * dispositivo (add-cache-consultas); sin él, se comporta como antes.
 * Retrofit y Room hacen su I/O en sus propios hilos; lo que corre en [dispatcher] (`Dispatchers.Default`
 * por defecto) es el trabajo de CPU: convertir, ordenar y validar fechas. Así se puede llamar desde el
 * hilo principal sin bloquearlo.
 */
class ArtistRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: ArtistApi = adaptador.api(),
    private val artistaDao: ArtistDao? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ArtistRepository {

    /** Protege la comparación, la escritura y la actualización de lo que sigue (optimizar-cache-e-imagenes, D4). */
    private val escritura = Mutex()
    private val copiaListado = CopiaTabla<Pair<ArtistType, Int>, ArtistEntity> { it.tipo to it.id }
    private val copiaDetalles = CopiaTabla<Pair<ArtistType, Int>, ArtistDetailEntity> { it.tipo to it.id }
    private var versionListado: String? = null
    private val versionesDetalle = Versiones<Pair<ArtistType, Int>>()

    /**
     * Músicos y bandas en un solo listado ordenado por nombre. Las dos consultas van en paralelo
     * dentro de una sola llamada al adaptador: si cualquiera falla, el resultado es el error de carga,
     * porque un listado parcial no cumpliría con mostrar ambos. Si sale bien, lo guardado queda igual a la
     * respuesta (D4); su versión combina la de músicos y la de bandas.
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

    /** Lectura de lo guardado: sin DAO o si falla el almacenamiento, es como si no hubiera nada. */
    private suspend fun <T> leer(siNoHay: T, bloque: suspend ArtistDao.() -> T): T {
        val dao = artistaDao ?: return siNoHay
        return try {
            dao.bloque()
        } catch (e: SQLiteException) {
            siNoHay
        }
    }

    // ---------- Punto único de escritura (optimizar-cache-e-imagenes, D4 y D5) ----------

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
     * Ejecuta [bloque] con [escritura] tomado. Si el bloque no termina normalmente, no se sabe qué quedó en
     * Room, así que se descarta lo que se recordaba (regla 2 de D4). Un fallo del almacenamiento no cambia el
     * resultado de la consulta; la cancelación y los errores de programación se relanzan.
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
     * Detalle del artista según su tipo. Las premiaciones solo se consultan si el artista tiene alguna,
     * porque el recurso del artista no trae el premio. Todo va en una sola llamada al adaptador: cualquier
     * fallo es el error de carga, incluido el de las premiaciones, porque "Sin premios" sería falso.
     * Si sale bien, guarda el detalle si cambió; si el backend confirma con un 404 que ya no existe, lo borra
     * de lo guardado y sigue devolviendo el mismo error (D4). La versión combina la del artista y la de las
     * premiaciones, o marca que no se pidieron.
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
     * Músico: `POST`, que agrega sin quitar a nadie. Banda: el `POST` del backend deja a la banda como única
     * intérprete del álbum, así que se lee su lista actual y se reemplaza por esa lista más el nuevo con
     * `PUT`. Si la lista no se puede leer completa, no se envía nada: un `PUT` incompleto le quitaría
     * álbumes a la banda (add-agregar-album-artista, D1).
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
                        api.reemplazarAlbumesDeBanda(idArtista, nuevos).exitosa()
                    }
                }
            }
        }

    /** Dentro de [ServiceAdapter.llamar]: una respuesta no exitosa se lanza para que se traduzca a `ErrorDeCarga`. */
    private fun Response<Unit>.exitosa() {
        if (!isSuccessful) throw HttpException(this)
    }

    /**
     * Conversión tolerante: solo el id y el nombre del artista son obligatorios, porque el backend compartido
     * tiene datos de prueba de otros equipos y un elemento malo no debe tumbar toda la pantalla.
     */
    private fun ArtistDetailDto.aDetalle(tipo: ArtistType, premiaciones: List<PremiacionDto>) = ArtistDetail(
        id = id ?: throw JsonParseException("El artista no tiene id"),
        nombre = name.conTexto() ?: throw JsonParseException("El artista no tiene nombre"),
        imagen = image,
        descripcion = description.conTexto(),
        tipo = tipo,
        fecha = fechaValida(if (tipo == ArtistType.MUSICO) birthDate else creationDate),
        integrantes = musicians.orEmpty().mapNotNull { dto ->
            IntegranteArtista(
                dto.id ?: return@mapNotNull null,
                dto.name.conTexto() ?: return@mapNotNull null,
                dto.image
            )
        },
        albumes = albums.orEmpty().mapNotNull { dto ->
            val fecha = fechaValida(dto.releaseDate)
            AlbumArtista(
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
        premiaciones: List<PremiacionDto>
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
     * La fecha se guarda en ISO y la pantalla la formatea en el idioma de la app: así lo guardado sirve en
     * cualquier idioma, también sin conexión. Una ausente o que no se pueda interpretar se trata como ausente.
     */
    private fun fechaValida(fechaIso: String?): String? = fechaIso?.takeIf { formatearFechaONula(it) != null }

    /** Ordena por la fecha ISO que acompaña a cada elemento, con los que no tienen fecha al final. */
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

        /** Parte de la versión del detalle cuando el artista no tiene premios y no se pidió `/performerprizes`. */
        const val PREMIACIONES_NO_PEDIDAS = "sin-premiaciones"
    }
}
