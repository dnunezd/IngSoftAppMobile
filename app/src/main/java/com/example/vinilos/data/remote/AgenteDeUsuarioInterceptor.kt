package com.example.vinilos.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Identifica a la app en cada petición. Algunos servidores de imágenes, como Wikimedia, rechazan
 * con 403 el agente de usuario genérico de OkHttp.
 */
class AgenteDeUsuarioInterceptor(private val agente: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("User-Agent", agente).build())
}
