package com.example.vinilos.ui.detalle

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AlbumUiState
import org.junit.Rule
import org.junit.Test

class PantallaDetalleAlbumTest {
    @get:Rule
    val rule = createComposeRule()

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
}
