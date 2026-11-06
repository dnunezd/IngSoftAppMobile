package com.example.vinilos.viewmodel

const val LimiteDescripcion = 500

enum class CampoAlbum { Nombre, Portada, Fecha, Genero, Disquera, Descripcion }

enum class ErrorCampo { Obligatorio, UrlInvalida }

sealed interface EnvioAlbum {
    data object Editando : EnvioAlbum
    data object Enviando : EnvioAlbum
    data object Fallido : EnvioAlbum
    data class Creado(val id: Int) : EnvioAlbum
}

/**
 * Estado del formulario. A diferencia de los demás estados de la app no es sellado: los datos deben
 * conservarse en todas las etapas del envío, así que lo sellado es solo [EnvioAlbum].
 * [intentosFallidos] avisa a la pantalla que debe subir hasta el aviso de errores.
 */
data class CrearAlbumUiState(
    val nombre: String = "",
    val portada: String = "",
    val descripcion: String = "",
    val fechaMillis: Long? = null,
    val genero: String? = null,
    val disquera: String? = null,
    val errores: Map<CampoAlbum, ErrorCampo> = emptyMap(),
    val intentosFallidos: Int = 0,
    val envio: EnvioAlbum = EnvioAlbum.Editando
) {
    val enviando: Boolean get() = envio == EnvioAlbum.Enviando
}

fun portadaValida(portada: String): Boolean {
    val url = portada.trim()
    return url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)
}

fun validarAlbum(estado: CrearAlbumUiState): Map<CampoAlbum, ErrorCampo> = buildMap {
    if (estado.nombre.isBlank()) put(CampoAlbum.Nombre, ErrorCampo.Obligatorio)
    when {
        estado.portada.isBlank() -> put(CampoAlbum.Portada, ErrorCampo.Obligatorio)
        !portadaValida(estado.portada) -> put(CampoAlbum.Portada, ErrorCampo.UrlInvalida)
    }
    if (estado.fechaMillis == null) put(CampoAlbum.Fecha, ErrorCampo.Obligatorio)
    if (estado.genero == null) put(CampoAlbum.Genero, ErrorCampo.Obligatorio)
    if (estado.disquera == null) put(CampoAlbum.Disquera, ErrorCampo.Obligatorio)
    if (estado.descripcion.isBlank()) put(CampoAlbum.Descripcion, ErrorCampo.Obligatorio)
}
