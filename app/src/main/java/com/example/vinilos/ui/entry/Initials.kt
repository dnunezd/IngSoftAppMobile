package com.example.vinilos.ui.entry

import java.util.Locale

/** Initials from the first two words of the name, uppercase ("Manolo Bellon" -> "MB"). */
fun iniciales(nombre: String): String =
    nombre.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().toString() }
        .uppercase(Locale.ROOT)
        .ifEmpty { "?" }
