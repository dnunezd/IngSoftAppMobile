package com.example.vinilos

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.compose.rememberNavController
import com.example.vinilos.navigation.VinilosNavHost
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.SessionViewModel

class MainActivity : ComponentActivity() {

    // Atado a la Activity: la sesión es la misma para todas las pantallas y sobrevive a la rotación.
    private val sessionViewModel: SessionViewModel by viewModels()

    @OptIn(ExperimentalComposeUiApi::class)
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
                    albumRepository = container.albumRepository,
                    favoriteRepository = container.favoriteRepository,
                    // Expone cada testTag como resource-id de accesibilidad: así UI Automator
                    // (módulo :perfilamiento) encuentra los elementos desde otro proceso.
                    modifier = Modifier.semantics { testTagsAsResourceId = true }
                )
            }
        }
    }
}
