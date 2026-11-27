package com.example.vinilos.data.remote

import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellationException
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/** Single load error: no connection, timeout, server error, or unreadable response. */
open class LoadError(causa: Throwable? = null) : Exception("No se pudo cargar la información", causa)

/**
 * The backend responded 404: the requested resource does not exist. It's still a [LoadError] for callers who don't need
 * to distinguish it; repositories that do translate it into their own error without knowing about the HTTP protocol.
 */
class ResourceNotFound(causa: Throwable? = null) : LoadError(causa)

/**
 * Single access point to the BackVynils backend: builds the network client once,
 * provides the per-entity interfaces, and translates query failures into [LoadError].
 */
class ServiceAdapter(
    baseUrl: String,
    cliente: OkHttpClient = clientePorDefecto()
) {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(FormatErrorConverterFactory(GsonConverterFactory.create()))
        .build()

    fun <T> api(clase: Class<T>): T = retrofit.create(clase)

    inline fun <reified T> api(): T = api(T::class.java)

    /**
     * Executes a backend query and returns its result. Network, server, or response-reading
     * failures are reported as [LoadError] (a 404 as [ResourceNotFound]); cancellation
     * is rethrown.
     */
    suspend fun <T> call(bloque: suspend () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(LoadError(e))
        } catch (e: HttpException) {
            Result.failure(if (e.code() == HTTP_NO_ENCONTRADO) ResourceNotFound(e) else LoadError(e))
        } catch (e: JsonParseException) {
            Result.failure(LoadError(e))
        } catch (e: KotlinNullPointerException) {
            // Retrofit throws it when the response arrives with no body or with "null" and the function declares
            // a non-null type. NullPointerExceptions from our own code are not caught here.
            Result.failure(LoadError(e))
        }

    /**
     * Gson throws `IllegalStateException` when the response doesn't have the expected shape (for example,
     * an object where a list is expected). It's converted into [JsonSyntaxException] only when reading
     * the response, so that [llamar] reports it as [LoadError] without hiding other
     * `IllegalStateException`s, which are programming errors.
     */
    private class FormatErrorConverterFactory(private val gson: Converter.Factory) : Converter.Factory() {
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

    /**
     * Asks the backend to confirm every stored response before using it (`max-age=0`): OkHttp always sends
     * `If-None-Match` and never serves what's stored without asking, even if the backend or a proxy sends a
     * `max-age`. This way, without a connection the query fails as before and each screen shows what it stored in Room.
     */
    private object RevalidarConElBackend : Interceptor {
        private val siempreRevalidar = CacheControl.Builder().maxAge(0, TimeUnit.SECONDS).build()

        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val peticion = chain.request()
            if (peticion.method != "GET") return chain.proceed(peticion)
            return chain.proceed(peticion.newBuilder().cacheControl(siempreRevalidar).build())
        }
    }

    companion object {
        private const val TIMEOUT_SEGUNDOS = 10L
        private const val HTTP_NO_ENCONTRADO = 404
        private const val TAMANO_CACHE_HTTP = 10L * 1024 * 1024

        /**
         * API network client. With [carpetaCache], it stores responses in a 10 MiB disk HTTP cache and
         * revalidates every repeated query with its `ETag`: if nothing changed, the backend responds 304 with no
         * body and the stored response is used (optimizar-cache-e-imagenes, D2). The folder must be its own: Coil uses
         * a different one for images. Without a folder (tests), the client stores nothing, as before.
         */
        fun clientePorDefecto(carpetaCache: File? = null): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .apply {
                if (carpetaCache != null) {
                    cache(Cache(carpetaCache, TAMANO_CACHE_HTTP))
                    addInterceptor(RevalidarConElBackend)
                }
            }
            .build()
    }
}
