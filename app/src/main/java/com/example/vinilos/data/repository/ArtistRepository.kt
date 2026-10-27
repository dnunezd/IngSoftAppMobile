package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ArtistApi
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.Collator
import java.util.Locale

open class ArtistRepository(private val adaptador: ServiceAdapter) {

    private val api: ArtistApi = adaptador.api()

    /**
     * Músicos y bandas en un solo listado ordenado por nombre. Las dos consultas van en paralelo
     * dentro de una sola llamada al adaptador: si cualquiera falla, el resultado es el error de carga,
     * porque un listado parcial no cumpliría con mostrar ambos.
     */
    open suspend fun obtenerArtistas(): Result<List<Artist>> =
        adaptador.llamar {
            coroutineScope {
                val musicos = async { api.getMusicians() }
                val bandas = async { api.getBands() }
                musicos.await().mapNotNull { artista(it.id, it.name, it.image, ArtistType.MUSICO) } +
                    bandas.await().mapNotNull { artista(it.id, it.name, it.image, ArtistType.BANDA) }
            }
        }.map { artistas -> artistas.sortedWith(compareBy(ordenAlfabetico) { it.name }) }

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
