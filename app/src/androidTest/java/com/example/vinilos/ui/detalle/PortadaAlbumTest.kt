package com.example.vinilos.ui.detalle

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.vinilos.ui.theme.VinilosTheme
import org.junit.Rule
import org.junit.Test

class PortadaAlbumTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun sinUrlMuestraLaPortadaConElIconoDeRespaldo() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = null, modifier = Modifier.size(160.dp)) }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
        rule.onNodeWithTag(TagRespaldoPortada, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun conUnaUrlNoRompeLaPantallaMientrasCargaOFalla() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = "https://example.com/portada.jpg", modifier = Modifier.size(160.dp)) }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
    }

    @Test
    fun unaUrlEnBlancoSeTrataComoAusenteYMuestraElIconoDeRespaldo() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = " \t ", modifier = Modifier.size(160.dp)) }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
        rule.onNodeWithTag(TagRespaldoPortada, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun sinTamanoDelLlamadorLaPortadaSigueVisible() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = null) }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
    }
}
