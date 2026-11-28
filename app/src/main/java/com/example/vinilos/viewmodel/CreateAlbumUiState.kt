package com.example.vinilos.viewmodel

const val LimiteDescripcion = 500

enum class AlbumField { Nombre, Portada, Fecha, Genero, Disquera, Descripcion }

enum class FieldError { Obligatorio, UrlInvalida }

sealed interface EnvioAlbum {
    data object Editando : EnvioAlbum
    data object Enviando : EnvioAlbum
    data object Fallido : EnvioAlbum
    data class Creado(val id: Int) : EnvioAlbum
}

/**
 * Form state. Unlike the other states in the app it isn't sealed: the data must be
 * preserved across all submission stages, so only [EnvioAlbum] is sealed.
 * [intentosFallidos] tells the screen it must scroll up to the error notice.
 */
data class CreateAlbumUiState(
    val nombre: String = "",
    val portada: String = "",
    val descripcion: String = "",
    val fechaMillis: Long? = null,
    val genero: String? = null,
    val disquera: String? = null,
    val errores: Map<AlbumField, FieldError> = emptyMap(),
    val intentosFallidos: Int = 0,
    val envio: EnvioAlbum = EnvioAlbum.Editando
) {
    val enviando: Boolean get() = envio == EnvioAlbum.Enviando
}

/** `http://` or `https://` followed by a domain; the port, path, and query string are optional. */
private val UrlPortada = Regex("""https?://[\p{L}\p{N}][^/?#:@\s]*([:/?#].*)?""", RegexOption.IGNORE_CASE)

fun portadaValida(portada: String): Boolean = UrlPortada.matches(portada.trim())

fun validarAlbum(estado: CreateAlbumUiState): Map<AlbumField, FieldError> = buildMap {
    if (estado.nombre.isBlank()) put(AlbumField.Nombre, FieldError.Obligatorio)
    when {
        estado.portada.isBlank() -> put(AlbumField.Portada, FieldError.Obligatorio)
        !portadaValida(estado.portada) -> put(AlbumField.Portada, FieldError.UrlInvalida)
    }
    if (estado.fechaMillis == null) put(AlbumField.Fecha, FieldError.Obligatorio)
    if (estado.genero == null) put(AlbumField.Genero, FieldError.Obligatorio)
    if (estado.disquera == null) put(AlbumField.Disquera, FieldError.Obligatorio)
    if (estado.descripcion.isBlank()) put(AlbumField.Descripcion, FieldError.Obligatorio)
}
