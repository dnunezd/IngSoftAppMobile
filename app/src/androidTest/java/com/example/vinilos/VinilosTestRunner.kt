package com.example.vinilos

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.test.runner.AndroidJUnitRunner

/**
 * Las pruebas instrumentadas buscan textos en español. Desde que la app también está en inglés
 * (internacionalizar-ingles, D7), en un emulador con el sistema en inglés la app saldría en inglés
 * y esas búsquedas fallarían. En Android 13 o superior se fija el idioma de la app en español antes
 * de abrir cualquier pantalla. En versiones anteriores la app sigue el idioma del dispositivo.
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
