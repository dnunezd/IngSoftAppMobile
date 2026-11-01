package com.example.vinilos.viewmodel

import java.text.Normalizer
import java.util.Locale

private val MarcasDiacriticas = Regex("\\p{InCombiningDiacriticalMarks}+")

/** Texto comparable en las búsquedas: sin tildes y en minúsculas ("Monsálve" -> "monsalve"). */
internal fun normalizar(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(MarcasDiacriticas, "")
        .lowercase(Locale.ROOT)
