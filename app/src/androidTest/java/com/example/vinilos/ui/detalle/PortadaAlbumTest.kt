package com.example.vinilos.ui.detalle

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import com.example.vinilos.ui.theme.VinilosTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PortadaAlbumTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    @SdkSuppress(minSdkVersion = 26) // captureToImage usa PixelCopy, disponible desde API 26.
    fun sinUrlMuestraLaPortadaConElIconoDeRespaldo() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = null, modifier = Modifier.size(160.dp)) }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
        assertTrue("Sin URL debe dibujarse el ícono sobre el fondo", dibujaAlgoSobreElFondo())
    }

    @Test
    fun conUnaUrlNoRompeLaPantallaMientrasCargaOFalla() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = "https://example.com/portada.jpg", modifier = Modifier.size(160.dp)) }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun unaUrlEnBlancoSeTrataComoAusenteYMuestraElIconoDeRespaldo() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = " \t ", modifier = Modifier.size(160.dp)) }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
        assertTrue("Una URL en blanco debe dibujar el ícono sobre el fondo", dibujaAlgoSobreElFondo())
    }

    @Test
    fun sinTamanoDelLlamadorLaPortadaSigueVisible() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = null) }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
    }

    /**
     * El ícono de respaldo lo dibuja Coil dentro de la misma imagen, así que no tiene nodo propio: se comprueba
     * que haya píxeles distintos del fondo, que se toma de la esquina (el ícono va centrado).
     */
    private fun dibujaAlgoSobreElFondo(): Boolean {
        val pixeles = rule.onNodeWithContentDescription("Portada del álbum").captureToImage().toPixelMap()
        val fondo = pixeles[0, 0]
        for (x in 0 until pixeles.width) {
            for (y in 0 until pixeles.height) {
                if (pixeles[x, y] != fondo) return true
            }
        }
        return false
    }
}
