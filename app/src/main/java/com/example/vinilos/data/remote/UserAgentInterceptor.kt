package com.example.vinilos.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Identifies the app on every request. Some image servers, like Wikimedia, reject
 * OkHttp's generic user agent with a 403.
 */
class UserAgentInterceptor(private val agente: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("User-Agent", agente).build())
}
