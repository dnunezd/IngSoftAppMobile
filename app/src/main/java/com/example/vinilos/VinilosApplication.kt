package com.example.vinilos

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.example.vinilos.data.local.VinilosDb
import com.example.vinilos.data.remote.UserAgentInterceptor
import okhttp3.OkHttpClient

class VinilosApplication : Application(), ImageLoaderFactory {
    /**
     * Replaceable in instrumented tests, before opening the Activity. Created in [onCreate] and not
     * at declaration time, because the local database needs a `Context` that doesn't exist yet when the
     * application is constructed (add-cache-catalogo, D6).
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        val db = VinilosDb.create(this)
        container = AppContainer(
            albumDao = db.albumDao(),
            coleccionistaDao = db.coleccionistaDao(),
            artistaDao = db.artistaDao(),
            favoritoDao = db.favoritoDao(),
            // Separate from `image_cache`: OkHttp and Coil can't share their disk cache folder.
            carpetaCacheHttp = cacheDir.resolve("http")
        )
    }

    /**
     * Coil image loader used by all screens (covers and artist photos).
     *
     * The disk cache has the same values Coil uses by default, but it's declared explicitly so the
     * decision is documented: with the catalog saved in Room, covers that were already seen are shown
     * offline from this cache, and Room only stores its URL (add-cache-catalogo, D8).
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(UserAgentInterceptor("Vinilos/${BuildConfig.VERSION_NAME} (Android)"))
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
