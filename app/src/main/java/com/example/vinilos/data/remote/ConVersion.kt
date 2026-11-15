package com.example.vinilos.data.remote

import com.google.gson.JsonParseException
import retrofit2.HttpException
import retrofit2.Response

/**
 * Cuerpo de una respuesta exitosa junto con su versión, la cabecera `ETag` que asigna el backend. Con ella,
 * los repositorios saben si la respuesta cambió respecto de lo último que guardaron (optimizar-cache-e-imagenes,
 * D3). Una respuesta 304 que entrega la caché HTTP trae la misma `ETag`.
 */
data class ConVersion<out T>(val valor: T, val version: String?)

/**
 * Para usar dentro de [ServiceAdapter.llamar]: una respuesta no exitosa se lanza como [HttpException] y una sin
 * cuerpo como [JsonParseException]. Así `llamar` las traduce igual que cuando la interfaz devuelve el cuerpo
 * directo (un 404 sigue siendo [RecursoNoEncontrado]).
 */
fun <T> Response<T>.conVersion(): ConVersion<T> {
    if (!isSuccessful) throw HttpException(this)
    val cuerpo = body() ?: throw JsonParseException("La respuesta llegó sin cuerpo")
    return ConVersion(cuerpo, headers()["ETag"])
}
