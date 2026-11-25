package com.example.vinilos.ui.crear

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AgregarCancionUiState
import com.example.vinilos.viewmodel.CampoCancion
import com.example.vinilos.viewmodel.EnvioCancion
import com.example.vinilos.viewmodel.ErrorCancion
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PantallaAgregarCancionTest {
    @get:Rule
    val rule = createComposeRule()

    private val sesion = Session.Collector(Collector(100, "Coleccionista", "coleccionista@example.com"))

    @Test
    fun formularioVacioMuestraObligatoriosYGuardarDeshabilitado() {
        rule.setContent {
            VinilosTheme {
                PantallaAgregarCancion(AgregarCancionUiState(), sesion, AccionesAgregarCancion())
            }
        }

        rule.onAllNodesWithText("Campo obligatorio").assertCountEquals(2)
        rule.onNodeWithTag(TagGuardarCancion).assertIsNotEnabled()
    }

    @Test
    fun duracionInvalidaMuestraElFormatoEsperado() {
        val estado = AgregarCancionUiState(
            nombre = "Canción",
            duracion = "5:60",
            errores = mapOf(CampoCancion.Duracion to ErrorCancion.DuracionInvalida)
        )
        rule.setContent {
            VinilosTheme { PantallaAgregarCancion(estado, sesion, AccionesAgregarCancion()) }
        }

        rule.onNodeWithText("Usa el formato m:ss, por ejemplo 5:05").assertIsDisplayed()
        rule.onNodeWithTag(TagGuardarCancion).assertIsNotEnabled()
    }

    @Test
    fun guardandoDeshabilitaGuardarYCancelar() {
        val estado = AgregarCancionUiState(
            "Canción",
            "5:05",
            emptyMap(),
            EnvioCancion.Guardando
        )
        rule.setContent {
            VinilosTheme { PantallaAgregarCancion(estado, sesion, AccionesAgregarCancion()) }
        }

        rule.onNodeWithText("Guardando...").assertIsDisplayed()
        rule.onNodeWithTag(TagGuardarCancion).assertIsNotEnabled()
        rule.onNodeWithTag(TagCancelarAgregarCancion).assertIsNotEnabled()
    }

    @Test
    fun errorReintentableConservaElMensajeYHabilitaGuardar() {
        val estado = AgregarCancionUiState(
            "Canción",
            "5:05",
            emptyMap(),
            EnvioCancion.Fallida
        )
        rule.setContent {
            VinilosTheme { PantallaAgregarCancion(estado, sesion, AccionesAgregarCancion()) }
        }

        rule.onNodeWithText("No se pudo agregar la canción, intenta de nuevo").assertIsDisplayed()
        rule.onNodeWithTag(TagGuardarCancion).assertIsDisplayed()
    }

    @Test
    fun albumNoDisponibleOfreceRegresar() {
        var regresos = 0
        val estado = AgregarCancionUiState(envio = EnvioCancion.AlbumNoDisponible)
        rule.setContent {
            VinilosTheme {
                PantallaAgregarCancion(
                    estado,
                    sesion,
                    AccionesAgregarCancion(onAlbumNoDisponible = { regresos++ })
                )
            }
        }

        rule.onNodeWithText("El álbum ya no está disponible").assertIsDisplayed()
        rule.onNodeWithText("Volver").performClick()
        assertEquals(1, regresos)
    }

    @Test
    fun albumNoDisponibleConAtrasDelSistemaTambienNotifica() {
        var regresos = 0
        val estado = AgregarCancionUiState(envio = EnvioCancion.AlbumNoDisponible)
        rule.setContent {
            VinilosTheme {
                PantallaAgregarCancion(
                    estado,
                    sesion,
                    AccionesAgregarCancion(onAlbumNoDisponible = { regresos++ })
                )
            }
        }

        androidx.test.espresso.Espresso.pressBack()
        assertEquals(1, regresos)
    }
}
