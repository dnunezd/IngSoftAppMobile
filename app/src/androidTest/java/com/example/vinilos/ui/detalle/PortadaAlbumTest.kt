package com.example.vinilos.ui.detalle

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.example.vinilos.ui.theme.VinilosTheme
import org.junit.Rule
import org.junit.Test

class PortadaAlbumTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun sinUrlMuestraLaPortadaConLaIlustracionLocal() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = null) }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
    }

    @Test
    fun conUnaUrlNoRompeLaPantallaMientrasCargaOFalla() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = "https://example.com/portada.jpg") }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
    }

    @Test
    fun unaUrlEnBlancoSeTrataComoAusente() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = " \t ") }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
    }
}
