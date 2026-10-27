package com.example.vinilos.ui.detalle

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.example.vinilos.ui.theme.VinilosTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PortadaAlbumTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun sinUrlMuestraLaIlustracionProvisional() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = null) }
        }

        rule.onNodeWithContentDescription("Imagen provisional del álbum").assertIsDisplayed()
    }

    @Test
    fun sinUrlNoMuestraUnaCargaAunqueElEstadoSeaCargando() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = null, estado = EstadoPortada.Cargando) }
        }

        rule.onNodeWithContentDescription("Imagen provisional del álbum").assertIsDisplayed()
        rule.onNodeWithContentDescription("Cargando portada del álbum").assertDoesNotExist()
    }

    @Test
    fun unaUrlEnBlancoSeTrataComoAusente() {
        rule.setContent {
            VinilosTheme { PortadaAlbum(url = " \t ", estado = EstadoPortada.Cargando) }
        }

        rule.onNodeWithContentDescription("Imagen provisional del álbum").assertIsDisplayed()
        rule.onNodeWithContentDescription("Cargando portada del álbum").assertDoesNotExist()
    }

    @Test
    fun conUrlYEstadoCargandoMuestraUnIndicador() {
        rule.setContent {
            VinilosTheme {
                PortadaAlbum(url = "https://example.com/portada.jpg", estado = EstadoPortada.Cargando)
            }
        }

        rule.onNodeWithContentDescription("Cargando portada del álbum").assertIsDisplayed()
        rule.onNodeWithContentDescription("Imagen provisional del álbum").assertDoesNotExist()
    }

    @Test
    fun siFallaLaCargaMuestraLaIlustracionProvisional() {
        rule.setContent {
            VinilosTheme {
                PortadaAlbum(url = "https://example.com/portada.jpg", estado = EstadoPortada.Error)
            }
        }

        rule.onNodeWithContentDescription("Imagen provisional del álbum").assertIsDisplayed()
        rule.onNodeWithContentDescription("Cargando portada del álbum").assertDoesNotExist()
    }

    @Test
    fun disponibleDibujaElPainterRecibidoSinImagenProvisional() {
        val dibujada = AtomicBoolean(false)
        val imagen = object : Painter() {
            override val intrinsicSize = Size.Unspecified

            override fun DrawScope.onDraw() {
                drawRect(Color.Magenta)
                dibujada.set(true)
            }
        }
        rule.setContent {
            VinilosTheme {
                PortadaAlbum(
                    url = "https://example.com/portada.jpg",
                    estado = EstadoPortada.Disponible(imagen),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        rule.onNodeWithContentDescription("Portada del álbum").assertIsDisplayed()
        rule.onNodeWithContentDescription("Imagen provisional del álbum").assertDoesNotExist()
        rule.onNodeWithContentDescription("Cargando portada del álbum").assertDoesNotExist()
        rule.waitForIdle()
        assertTrue("Debe dibujar el Painter recibido", dibujada.get())
    }
}
