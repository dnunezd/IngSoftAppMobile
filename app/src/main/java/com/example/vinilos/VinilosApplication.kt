package com.example.vinilos

import android.app.Application

class VinilosApplication : Application() {
    /** Reemplazable en las pruebas instrumentadas, antes de abrir la Activity. */
    var container: AppContainer = AppContainer()
}
