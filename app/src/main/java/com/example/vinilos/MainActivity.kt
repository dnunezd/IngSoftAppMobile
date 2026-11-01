package com.example.vinilos

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.example.vinilos.navigation.VinilosNavHost
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.SessionViewModel

class MainActivity : ComponentActivity() {

    // Atado a la Activity: la sesión es la misma para todas las pantallas y sobrevive a la rotación.
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // El tema es siempre claro: los íconos de las barras del sistema deben ser oscuros
        // aunque el teléfono esté en modo oscuro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val container = (application as VinilosApplication).container
        setContent {
            VinilosTheme {
                VinilosNavHost(
                    navController = rememberNavController(),
                    sessionViewModel = sessionViewModel,
                    collectorRepository = container.collectorRepository,
                    artistRepository = container.artistRepository,
                    albumRepository = container.albumRepository
                )
            }
        }
    }
}
