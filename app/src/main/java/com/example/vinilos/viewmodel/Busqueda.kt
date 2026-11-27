package com.example.vinilos.viewmodel

import java.text.Normalizer
import java.util.Locale

private val MarcasDiacriticas = Regex("\\p{InCombiningDiacriticalMarks}+")

/** Text comparable for searches: without accents and in lowercase ("Monsálve" -> "monsalve"). */
internal fun normalizar(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(MarcasDiacriticas, "")
        .lowercase(Locale.ROOT)
