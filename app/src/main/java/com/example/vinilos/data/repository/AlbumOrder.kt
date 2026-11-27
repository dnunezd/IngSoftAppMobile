package com.example.vinilos.data.repository

import com.example.vinilos.data.model.AlbumSummary
import java.text.Collator
import java.util.Locale

/**
 * Spanish alphabetical order with primary strength: it does not distinguish case or accents ("Álbum" next to
 * "album", "Ástor" next to "Alberto"). Returns a new `Collator` on every call because it is not safe to
 * share it between threads: each sort uses its own and several can run at the same time.
 */
internal fun ordenAlfabetico(): Collator = Collator.getInstance(Locale.forLanguageTag("es")).apply {
    strength = Collator.PRIMARY
}

/** Catalog order: by name and, if two names are equivalent, by id so it stays stable. */
internal fun ordenCatalogo(): Comparator<AlbumSummary> =
    compareBy<AlbumSummary, String>(ordenAlfabetico()) { it.nombre }.thenBy { it.id }
