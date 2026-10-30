package com.example.vinilos.data.repository

import com.example.vinilos.data.model.AlbumResumen
import java.text.Collator
import java.util.Locale

// Fuerza primaria: no distingue mayúsculas ni tildes ("Álbum" junto a "album"), como en artistas.
private val ordenAlfabetico: Collator = Collator.getInstance(Locale("es")).apply {
    strength = Collator.PRIMARY
}

/** Orden del catálogo: por nombre y, si dos nombres son equivalentes, por id para que sea estable. */
internal val ordenCatalogo: Comparator<AlbumResumen> =
    compareBy<AlbumResumen, String>(ordenAlfabetico) { it.nombre }.thenBy { it.id }
