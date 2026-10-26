package com.example.vinilos.data.remote

import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Error único de carga: sin conexión, tiempo agotado, error del servidor o respuesta ilegible. */
class ErrorDeCarga(causa: Throwable? = null) : Exception("No se pudo cargar la información", causa)

/**
 * Único punto de acceso al backend BackVynils: construye el cliente de red una sola vez,
 * entrega las interfaces por entidad y traduce los fallos de las consultas a [ErrorDeCarga].
 */
class ServiceAdapter(
    baseUrl: String,
    cliente: OkHttpClient = clientePorDefecto()
) {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> api(clase: Class<T>): T = retrofit.create(clase)

    inline fun <reified T> api(): T = api(T::class.java)

    /**
     * Ejecuta una consulta al backend y devuelve su resultado. Los fallos de red, del servidor o de
     * lectura de la respuesta se reportan como [ErrorDeCarga]; la cancelación se relanza.
     */
    suspend fun <T> llamar(bloque: suspend () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(ErrorDeCarga(e))
        } catch (e: HttpException) {
            Result.failure(ErrorDeCarga(e))
        } catch (e: JsonParseException) {
            Result.failure(ErrorDeCarga(e))
        }

    private companion object {
        const val TIMEOUT_SEGUNDOS = 10L

        fun clientePorDefecto(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .build()
    }
}
