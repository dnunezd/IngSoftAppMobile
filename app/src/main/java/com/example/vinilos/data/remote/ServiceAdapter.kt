package com.example.vinilos.data.remote

import kotlinx.coroutines.CancellationException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ServiceAdapter(
    private val baseUrl: String
) {

    @PublishedApi
    internal val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    inline fun <reified T> api(): T = retrofit.create(T::class.java)

    suspend fun <T> llamar(operacion: suspend () -> T): Result<T> =
        try {
            Result.success(operacion())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}