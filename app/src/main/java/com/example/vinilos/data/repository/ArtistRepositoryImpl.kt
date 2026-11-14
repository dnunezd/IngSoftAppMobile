package com.example.vinilos.data.repository

import android.database.sqlite.SQLiteException
import com.example.vinilos.data.local.ArtistaDao
import com.example.vinilos.data.local.ArtistaDetalleEntity
import com.example.vinilos.data.local.ArtistaEntity
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.PremioArtista
import com.example.vinilos.data.model.formatearFechaLanzamiento
import com.example.vinilos.data.remote.ArtistApi
import com.example.vinilos.data.remote.ArtistaDetalleDto
import com.example.vinilos.data.remote.PremiacionArtistaDto
import com.example.vinilos.data.remote.PremiacionDto
import com.example.vinilos.data.remote.RecursoNoEncontrado
import com.example.vinilos.data.remote.ServiceAdapter
import com.google.gson.JsonParseException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.ParseException

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
    private val artistaDao: ArtistaDao? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ArtistRepository {

    /**
     * Músicos y bandas en un solo listado ordenado por nombre. Las dos consultas van en paralelo
     * dentro de una sola llamada al adaptador: si cualquiera falla, el resultado es el error de carga,
     * porque un listado parcial no cumpliría con mostrar ambos. Si sale bien, reemplaza lo guardado (D4).
     */
    override suspend fun obtenerArtistas(): Result<List<Artist>> = withContext(dispatcher) {
        adaptador.llamar {
            coroutineScope {
                val musicos = async { api.getMusicians() }
                val bandas = async { api.getBands() }
                musicos.await().mapNotNull { artista(it.id, it.name, it.image, ArtistType.MUSICO) } +
                    bandas.await().mapNotNull { artista(it.id, it.name, it.image, ArtistType.BANDA) }
            }
        }.map { artistas -> artistas.sortedWith(compareBy(ordenAlfabetico()) { it.name }) }
            .onSuccess { artistas -> escribir { reemplazar(artistas.map { ArtistaEntity(it.type, it.id, it.name, it.image) }) } }
    }

    override suspend fun obtenerArtistasGuardados(): List<Artist> = withContext(dispatcher) {
        leer(siNoHay = emptyList()) {
            todos().map { Artist(it.id, it.nombre, it.imagen, it.tipo) }.sortedWith(compareBy(ordenAlfabetico()) { it.name })
        }
    }

    override suspend fun obtenerDetalleGuardado(tipo: ArtistType, id: Int): ArtistaDetalle? = withContext(dispatcher) {
        leer(siNoHay = null) { detalle(tipo, id)?.aModelo() }
    }

    /** Lectura de lo guardado: sin DAO o si falla el almacenamiento, es como si no hubiera nada. */
    private suspend fun <T> leer(siNoHay: T, bloque: suspend ArtistaDao.() -> T): T {
        val dao = artistaDao ?: return siNoHay
        return try {
            dao.bloque()
        } catch (e: SQLiteException) {
            siNoHay
        }
    }

    /** Un fallo al escribir no cambia el resultado de la consulta, que ya llegó del backend. */
    private suspend fun escribir(bloque: suspend ArtistaDao.() -> Unit) {
        val dao = artistaDao ?: return
        try {
            dao.bloque()
        } catch (e: SQLiteException) {
            // Se sigue mostrando la respuesta; la siguiente consulta exitosa vuelve a intentarlo.
        }
    }

    private fun ArtistaDetalle.aEntidad() =
        ArtistaDetalleEntity(tipo, id, nombre, imagen, descripcion, fecha, integrantes, albumes, premios)

    private fun ArtistaDetalleEntity.aModelo() =
        ArtistaDetalle(id, nombre, imagen, descripcion, tipo, fecha, integrantes, albumes, premios)

    /**
     * Detalle del artista según su tipo. Las premiaciones solo se consultan si el artista tiene alguna,
     * porque el recurso del artista no trae el premio. Todo va en una sola llamada al adaptador: cualquier
     * fallo es el error de carga, incluido el de las premiaciones, porque "Sin premios" sería falso.
     * Si sale bien, guarda el detalle; si el backend confirma con un 404 que ya no existe, lo borra de lo
     * guardado y sigue devolviendo el mismo error (D4).
     */
    override suspend fun obtenerDetalle(tipo: ArtistType, id: Int): Result<ArtistaDetalle> =
        withContext(dispatcher) {
            adaptador.llamar {
                val respuesta = when (tipo) {
                    ArtistType.MUSICO -> api.getMusician(id)
                    ArtistType.BANDA -> api.getBand(id)
                }
                val premiaciones =
                    if (respuesta.performerPrizes.isNullOrEmpty()) emptyList() else api.getPerformerPrizes()
                respuesta.aDetalle(tipo, premiaciones)
            }.onSuccess { detalle -> escribir { guardarDetalle(detalle.aEntidad()) } }
                .onFailure { if (it is RecursoNoEncontrado) escribir { borrarDetalle(tipo, id) } }
        }

    /**
     * Conversión tolerante: solo el id y el nombre del artista son obligatorios, porque el backend compartido
     * tiene datos de prueba de otros equipos y un elemento malo no debe tumbar toda la pantalla.
     */
    private fun ArtistaDetalleDto.aDetalle(tipo: ArtistType, premiaciones: List<PremiacionDto>) = ArtistaDetalle(
        id = id ?: throw JsonParseException("El artista no tiene id"),
        nombre = name.conTexto() ?: throw JsonParseException("El artista no tiene nombre"),
        imagen = image,
        descripcion = description.conTexto(),
        tipo = tipo,
        fecha = fechaEnEspanol(if (tipo == ArtistType.MUSICO) birthDate else creationDate),
        integrantes = musicians.orEmpty().mapNotNull { dto ->
            IntegranteArtista(
                dto.id ?: return@mapNotNull null,
                dto.name.conTexto() ?: return@mapNotNull null,
                dto.image
            )
        },
        albumes = albums.orEmpty().mapNotNull { dto ->
            val fecha = dto.releaseDate?.takeIf { fechaEnEspanol(it) != null }
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
    ): List<PremioArtista> {
        val premioPorPremiacion = premiaciones.associateBy { it.id }
        return delArtista.mapNotNull { dto ->
            val premio = premioPorPremiacion[dto.id]?.prize ?: return@mapNotNull null
            val fecha = fechaEnEspanol(dto.premiationDate)
            PremioArtista(
                dto.id ?: return@mapNotNull null,
                premio.name.conTexto() ?: return@mapNotNull null,
                premio.organization.conTexto() ?: return@mapNotNull null,
                fecha
            ) to dto.premiationDate?.takeIf { fecha != null }
        }.masRecientesPrimero()
    }

    private fun String?.conTexto(): String? = this?.takeIf(String::isNotBlank)

    /** Fecha en español; una ausente o que no se pueda interpretar se trata como ausente. */
    private fun fechaEnEspanol(fechaIso: String?): String? =
        fechaIso?.let {
            try {
                formatearFechaLanzamiento(it)
            } catch (_: ParseException) {
                null
            }
        }

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
    }
}
