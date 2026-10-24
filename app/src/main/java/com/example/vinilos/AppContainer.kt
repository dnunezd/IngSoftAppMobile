package com.example.vinilos

import com.example.vinilos.data.remote.ArtistApi
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.CollectorRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Contenedor de dependencias manual: crea una sola vez el cliente de red y los repositorios
 * que usan los ViewModel.
 */
class AppContainer(
    baseUrl: String = BuildConfig.BASE_URL,
    colectoresRepositorio: CollectorRepository? = null,
    artistasRepositorio: ArtistRepository? = null
) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /** Se puede reemplazar con [colectoresRepositorio] en las pruebas instrumentadas. */
    val collectorRepository: CollectorRepository =
        colectoresRepositorio ?: CollectorRepository(retrofit.create(CollectorApi::class.java))

    /** Se puede reemplazar con [artistasRepositorio] en las pruebas instrumentadas. */
    val artistRepository: ArtistRepository =
        artistasRepositorio ?: ArtistRepository(retrofit.create(ArtistApi::class.java))

    private companion object {
        const val TIMEOUT_SEGUNDOS = 10L
    }
}
