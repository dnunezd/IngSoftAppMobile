package com.example.vinilos

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.vinilos.data.remote.AgenteDeUsuarioInterceptor
import okhttp3.OkHttpClient

class VinilosApplication : Application(), ImageLoaderFactory {
    /** Reemplazable en las pruebas instrumentadas, antes de abrir la Activity. */
    var container: AppContainer = AppContainer()

    /** Cargador de imágenes de Coil que usan todas las pantallas (artistas y, luego, portadas). */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(AgenteDeUsuarioInterceptor("Vinilos/${BuildConfig.VERSION_NAME} (Android)"))
                    .build()
            }
            .crossfade(true)
            .build()
}
