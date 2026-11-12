package com.example.vinilos

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.example.vinilos.data.local.VinilosDb
import com.example.vinilos.data.remote.AgenteDeUsuarioInterceptor
import okhttp3.OkHttpClient

class VinilosApplication : Application(), ImageLoaderFactory {
    /**
     * Reemplazable en las pruebas instrumentadas, antes de abrir la Activity. Se crea en [onCreate] y no
     * al declararla, porque la base local necesita un `Context` que aún no existe al construir la
     * aplicación (add-cache-catalogo, D6).
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(albumDao = VinilosDb.crear(this).albumDao())
    }

    /**
     * Cargador de imágenes de Coil que usan todas las pantallas (portadas y fotos de artistas).
     *
     * La caché de disco tiene los mismos valores que Coil usa por defecto, pero se declara para que la
     * decisión quede escrita: con el catálogo guardado en Room, las portadas que ya se vieron se muestran
     * sin conexión desde esta caché, y Room solo guarda su URL (add-cache-catalogo, D8).
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(AgenteDeUsuarioInterceptor("Vinilos/${BuildConfig.VERSION_NAME} (Android)"))
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
}
