package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** Error único de carga: sin conexión, tiempo agotado, error del servidor o respuesta ilegible. */
class ErrorDeCarga(causa: Throwable? = null) : Exception("No se pudo cargar la información", causa)

open class CollectorRepository(private val api: CollectorApi) {

    open suspend fun obtenerColeccionistas(): Result<List<Collector>> =
        try {
            Result.success(api.getCollectors().mapNotNull { it.aModelo() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(ErrorDeCarga(e))
        } catch (e: HttpException) {
            Result.failure(ErrorDeCarga(e))
        } catch (e: JsonParseException) {
            Result.failure(ErrorDeCarga(e))
        }

    private fun CollectorDto.aModelo(): Collector? {
        val id = id ?: return null
        val nombre = name ?: return null
        val correo = email ?: return null
        return Collector(id, nombre, correo)
    }
}
