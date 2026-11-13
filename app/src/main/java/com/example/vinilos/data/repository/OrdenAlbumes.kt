package com.example.vinilos.data.repository

import com.example.vinilos.data.model.AlbumResumen
import java.text.Collator
import java.util.Locale

/**
 * Orden alfabético en español con fuerza primaria: no distingue mayúsculas ni tildes ("Álbum" junto a
 * "album", "Ástor" junto a "Alberto"). Devuelve un `Collator` nuevo en cada llamada porque no es seguro
 * compartirlo entre hilos: cada ordenamiento usa el suyo y varios pueden correr a la vez.
 */
internal fun ordenAlfabetico(): Collator = Collator.getInstance(Locale("es")).apply {
    strength = Collator.PRIMARY
}

/** Orden del catálogo: por nombre y, si dos nombres son equivalentes, por id para que sea estable. */
internal fun ordenCatalogo(): Comparator<AlbumResumen> =
    compareBy<AlbumResumen, String>(ordenAlfabetico()) { it.nombre }.thenBy { it.id }
