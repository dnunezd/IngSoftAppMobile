package com.example.vinilos.data.remote

import com.google.gson.JsonParseException
import retrofit2.HttpException
import retrofit2.Response

/**
 * Body of a successful response along with its version, the `ETag` header assigned by the backend. With it,
 * the repositories know whether the response changed relative to the last one they stored (optimizar-cache-e-imagenes,
 * D3). A 304 response delivered by the HTTP cache carries the same `ETag`.
 */
data class ConVersion<out T>(val valor: T, val version: String?)

/**
 * For use inside [ServiceAdapter.llamar]: an unsuccessful response is thrown as [HttpException] and one without
 * a body as [JsonParseException]. That way `llamar` translates them the same way it does when the interface returns the
 * body directly (a 404 is still [RecursoNoEncontrado]).
 */
fun <T> Response<T>.conVersion(): ConVersion<T> {
    if (!isSuccessful) throw HttpException(this)
    val cuerpo = body() ?: throw JsonParseException("La respuesta llegó sin cuerpo")
    return ConVersion(cuerpo, headers()["ETag"])
}
