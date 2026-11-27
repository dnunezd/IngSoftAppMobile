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

    // Tied to the Activity: the session is the same for all screens and survives rotation.
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The theme is always light: the system bar icons must be dark
        // even if the phone is in dark mode.
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
                    comentarioRepository = container.comentarioRepository
                )
            }
        }
    }
}
