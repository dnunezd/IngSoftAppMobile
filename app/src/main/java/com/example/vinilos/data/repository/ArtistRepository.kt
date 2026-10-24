package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ArtistApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.Collator
import java.util.Locale

open class ArtistRepository(private val api: ArtistApi) {

    /**
     * Músicos y bandas en un solo listado ordenado por nombre. Si cualquiera de las dos consultas
     * falla devuelve [ErrorDeCarga]: un listado parcial no cumpliría con mostrar ambos.
     */
    open suspend fun obtenerArtistas(): Result<List<Artist>> =
        try {
            val artistas = coroutineScope {
                val musicos = async { api.getMusicians() }
                val bandas = async { api.getBands() }
                musicos.await().mapNotNull { artista(it.id, it.name, it.image, ArtistType.MUSICO) } +
                    bandas.await().mapNotNull { artista(it.id, it.name, it.image, ArtistType.BANDA) }
            }
            Result.success(artistas.sortedWith(compareBy(ordenAlfabetico) { it.name }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Red, HTTP, JSON ilegible o cualquier otro fallo: nunca debe cerrar la app.
            Result.failure(ErrorDeCarga(e))
        }

    private fun artista(id: Int?, nombre: String?, imagen: String?, tipo: ArtistType): Artist? {
        id ?: return null
        nombre ?: return null
        return Artist(id, nombre, imagen, tipo)
    }

    private companion object {
        // Fuerza primaria: no distingue mayúsculas ni tildes ("Ástor" junto a "Alberto").
        val ordenAlfabetico: Collator = Collator.getInstance(Locale("es")).apply {
            strength = Collator.PRIMARY
        }
    }
}
