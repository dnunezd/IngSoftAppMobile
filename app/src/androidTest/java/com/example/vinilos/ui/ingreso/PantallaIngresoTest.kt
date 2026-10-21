package com.example.vinilos.ui.ingreso

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.vinilos.ui.theme.VinilosTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PantallaIngresoTest {

    @get:Rule
    val rule = createComposeRule()

    private var visitante = 0
    private var coleccionista = 0

    private fun mostrar() {
        rule.setContent {
            VinilosTheme {
                PantallaIngreso(onVisitante = { visitante++ }, onColeccionista = { coleccionista++ })
            }
        }
    }

    @Test
    fun muestraLasOpcionesVisitanteYColeccionistaConSuBotonEntrar() {
        mostrar()

        rule.onNodeWithText("Visitante").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Coleccionista").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Entrar").assertCountEquals(2)
    }

    @Test
    fun muestraLaFranjaAudioWaveConSuBpmYNoLaInsigniaDeVersion() {
        mostrar()

        rule.onNodeWithText("AUDIO WAVE").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("94 BPM").assertIsDisplayed()
        rule.onNodeWithText("v1.0.4").assertDoesNotExist()
    }

    @Test
    fun entrarComoVisitanteAvisaSoloAlVisitante() {
        mostrar()

        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()

        assertEquals(1, visitante)
        assertEquals(0, coleccionista)
    }

    @Test
    fun entrarComoColeccionistaAvisaSoloAlColeccionista() {
        mostrar()

        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()

        assertEquals(0, visitante)
        assertEquals(1, coleccionista)
    }
}
