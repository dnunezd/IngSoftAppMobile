package com.example.vinilos.data.remote

import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Type
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
        .addConverterFactory(LecturaComoErrorDeFormato(GsonConverterFactory.create()))
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
        } catch (e: KotlinNullPointerException) {
            // Retrofit la lanza cuando la respuesta llega sin cuerpo o con "null" y la función declara
            // un tipo no nulo. Los NullPointerException del código propio no se atrapan.
            Result.failure(ErrorDeCarga(e))
        }

    /**
     * Gson lanza `IllegalStateException` cuando la respuesta no tiene la forma esperada (por ejemplo,
     * un objeto donde se espera una lista). Solo al leer la respuesta se convierte en
     * [JsonSyntaxException], para que [llamar] la reporte como [ErrorDeCarga] sin ocultar los demás
     * `IllegalStateException`, que son errores de programación.
     */
    private class LecturaComoErrorDeFormato(private val gson: Converter.Factory) : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<out Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *>? {
            val delegado = gson.responseBodyConverter(type, annotations, retrofit) ?: return null
            return Converter<ResponseBody, Any?> { cuerpo ->
                try {
                    delegado.convert(cuerpo)
                } catch (e: IllegalStateException) {
                    throw JsonSyntaxException(e)
                }
            }
        }

        override fun requestBodyConverter(
            type: Type,
            parameterAnnotations: Array<out Annotation>,
            methodAnnotations: Array<out Annotation>,
            retrofit: Retrofit
        ) = gson.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
    }

    private companion object {
        const val TIMEOUT_SEGUNDOS = 10L

        fun clientePorDefecto(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .build()
    }
}
