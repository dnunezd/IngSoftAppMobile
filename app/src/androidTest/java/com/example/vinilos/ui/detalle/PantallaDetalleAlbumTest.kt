package com.example.vinilos.ui.detalle

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
}
