package com.example.vinilos.ui.ingreso

import java.util.Locale

/** Iniciales de las dos primeras palabras del nombre, en mayúsculas ("Manolo Bellon" -> "MB"). */
fun iniciales(nombre: String): String =
    nombre.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().toString() }
        .uppercase(Locale.ROOT)
        .ifEmpty { "?" }
