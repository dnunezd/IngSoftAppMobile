package com.example.vinilos

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.test.runner.AndroidJUnitRunner

/**
 * The instrumented tests search for Spanish text. Since the app is now also available in English
 * (internacionalizar-ingles, D7), on an emulator with the system set to English the app would
 * launch in English and those searches would fail. On Android 13 or higher, the app's locale is
 * forced to Spanish before opening any screen. On earlier versions the app still follows the
 * device's locale.
 */
class VinilosTestRunner : AndroidJUnitRunner() {

    override fun onStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            targetContext.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags("es")
        }
        super.onStart()
    }
}
