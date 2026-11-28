package com.example.vinilos.ui.create

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
import com.example.vinilos.viewmodel.AddTrackUiState
import com.example.vinilos.viewmodel.TrackField
import com.example.vinilos.viewmodel.TrackSubmission
import com.example.vinilos.viewmodel.TrackError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddTrackScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val sesion = Session.Collector(Collector(100, "Coleccionista", "coleccionista@example.com"))

    @Test
    fun formularioEmptyMuestraObligatoriosYGuardarDeshabilitado() {
        rule.setContent {
            VinilosTheme {
                AddTrackScreen(AddTrackUiState(), sesion, AddTrackActions())
            }
        }

        rule.onAllNodesWithText("Campo obligatorio").assertCountEquals(2)
        rule.onNodeWithTag(TagGuardarTrack).assertIsNotEnabled()
    }

    @Test
    fun duracionInvalidaMuestraElFormatoEsperado() {
        val estado = AddTrackUiState(
            nombre = "Canción",
            duracion = "5:60",
            errores = mapOf(TrackField.Duracion to TrackError.DuracionInvalida)
        )
        rule.setContent {
            VinilosTheme { AddTrackScreen(estado, sesion, AddTrackActions()) }
        }

        rule.onNodeWithText("Usa el formato m:ss, por ejemplo 5:05").assertIsDisplayed()
        rule.onNodeWithTag(TagGuardarTrack).assertIsNotEnabled()
    }

    @Test
    fun guardandoDeshabilitaGuardarYCancelar() {
        val estado = AddTrackUiState(
            "Canción",
            "5:05",
            emptyMap(),
            TrackSubmission.Guardando
        )
        rule.setContent {
            VinilosTheme { AddTrackScreen(estado, sesion, AddTrackActions()) }
        }

        rule.onNodeWithText("Guardando…").assertIsDisplayed()
        rule.onNodeWithTag(TagGuardarTrack).assertIsNotEnabled()
        rule.onNodeWithTag(TagCancelarAddTrack).assertIsNotEnabled()
    }

    @Test
    fun errorReintentableConservaElMensajeYHabilitaGuardar() {
        val estado = AddTrackUiState(
            "Canción",
            "5:05",
            emptyMap(),
            TrackSubmission.Fallida
        )
        rule.setContent {
            VinilosTheme { AddTrackScreen(estado, sesion, AddTrackActions()) }
        }

        rule.onNodeWithText("No se pudo agregar la canción, intenta de nuevo").assertIsDisplayed()
        rule.onNodeWithTag(TagGuardarTrack).assertIsDisplayed()
    }

    @Test
    fun albumNotAvailableOfreceRegresar() {
        var regresos = 0
        val estado = AddTrackUiState(envio = TrackSubmission.AlbumNotAvailable)
        rule.setContent {
            VinilosTheme {
                AddTrackScreen(
                    estado,
                    sesion,
                    AddTrackActions(onAlbumNotAvailable = { regresos++ })
                )
            }
        }

        rule.onNodeWithText("El álbum ya no está disponible").assertIsDisplayed()
        rule.onNodeWithText("Volver").performClick()
        assertEquals(1, regresos)
    }

    @Test
    fun albumNotAvailableConAtrasDelSistemaTambienNotifica() {
        var regresos = 0
        val estado = AddTrackUiState(envio = TrackSubmission.AlbumNotAvailable)
        rule.setContent {
            VinilosTheme {
                AddTrackScreen(
                    estado,
                    sesion,
                    AddTrackActions(onAlbumNotAvailable = { regresos++ })
                )
            }
        }

        androidx.test.espresso.Espresso.pressBack()
        assertEquals(1, regresos)
    }
}
