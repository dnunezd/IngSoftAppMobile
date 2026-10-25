package com.example.vinilos.ui.detalle

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.vinilos.data.model.Album
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AlbumUiState
import org.junit.Rule
import org.junit.Test

class PantallaDetalleAlbumTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun exitoMuestraElDetalleSinProgresoNiError() {
        val album = Album(
            100, "Buscando América", null, "Salsa", "Salsa", "Elektra",
            "1984-08-01", emptyList(), emptyList(), emptyList()
        )
        rule.setContent {
            VinilosTheme {
                PantallaDetalleAlbum(AlbumUiState.Exito(album), onAtras = {}, onReintentar = {})
            }
        }

        rule.onNodeWithText("Buscando América").assertIsDisplayed()
        rule.onNodeWithContentDescription("Cargando álbum").assertDoesNotExist()
        rule.onNodeWithText("No pudimos cargar el álbum").assertDoesNotExist()
        rule.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test
    fun duranteLaConsultaMuestraProgresoSinErrorNiReintento() {
        rule.setContent {
            VinilosTheme {
                PantallaDetalleAlbum(AlbumUiState.Cargando, onAtras = {}, onReintentar = {})
            }
        }

        rule.onNodeWithContentDescription("Cargando álbum").assertIsDisplayed()
        rule.onNodeWithText("No pudimos cargar el álbum").assertDoesNotExist()
        rule.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test
    fun albumNoDisponiblePermiteVolverSinOfrecerReintento() {
        var regresos = 0
        rule.setContent {
            VinilosTheme {
                PantallaDetalleAlbum(AlbumUiState.NoDisponible, onAtras = { regresos++ }, onReintentar = {})
            }
        }

        rule.onNodeWithText("El álbum ya no está disponible").assertIsDisplayed()
        rule.onNodeWithText("Reintentar").assertDoesNotExist()
        rule.onNodeWithContentDescription("Cargando álbum").assertDoesNotExist()
        rule.onNodeWithText("Volver al catálogo").performClick()
        org.junit.Assert.assertEquals(1, regresos)
    }

    @Test
    fun errorDeCargaMuestraElMensajeYNotificaElReintento() {
        var reintentos = 0
        rule.setContent {
            VinilosTheme {
                PantallaDetalleAlbum(AlbumUiState.Error, onAtras = {}, onReintentar = { reintentos++ })
            }
        }

        rule.onNodeWithText("No pudimos cargar el álbum").assertIsDisplayed()
        rule.onNodeWithText("El álbum ya no está disponible").assertDoesNotExist()
        rule.onNodeWithContentDescription("Cargando álbum").assertDoesNotExist()
        rule.onNodeWithText("Reintentar").performClick()
        org.junit.Assert.assertEquals(1, reintentos)
    }
}
