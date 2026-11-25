package com.example.vinilos.viewmodel

enum class CampoCancion {
    Nombre,
    Duracion
}

enum class ErrorCancion {
    Obligatorio,
    DuracionInvalida
}

private val formatoDuracion = Regex("[0-9]+:[0-5][0-9]")

fun validarCancion(nombre: String, duracion: String): Map<CampoCancion, ErrorCancion> = buildMap {
    if (nombre.isBlank()) put(CampoCancion.Nombre, ErrorCancion.Obligatorio)
    if (duracion.isBlank()) {
        put(CampoCancion.Duracion, ErrorCancion.Obligatorio)
    } else if (!formatoDuracion.matches(duracion)) {
        put(CampoCancion.Duracion, ErrorCancion.DuracionInvalida)
    }
}
