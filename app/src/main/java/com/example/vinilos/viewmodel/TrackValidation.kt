package com.example.vinilos.viewmodel

enum class TrackField {
    Nombre,
    Duracion
}

enum class TrackError {
    Obligatorio,
    DuracionInvalida
}

private val formatoDuracion = Regex("[0-9]+:[0-5][0-9]")

fun validarTrack(nombre: String, duracion: String): Map<TrackField, TrackError> = buildMap {
    if (nombre.isBlank()) put(TrackField.Nombre, TrackError.Obligatorio)
    if (duracion.isBlank()) {
        put(TrackField.Duracion, TrackError.Obligatorio)
    } else if (!formatoDuracion.matches(duracion)) {
        put(TrackField.Duracion, TrackError.DuracionInvalida)
    }
}
