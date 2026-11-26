package com.example.vinilos.ui.ingreso

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.vinilos.ui.theme.VinilosTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EntryScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private var visitante = 0
    private var coleccionista = 0

    private fun mostrar() {
        rule.setContent {
            VinilosTheme {
                EntryScreen(onVisitante = { visitante++ }, onColeccionista = { coleccionista++ })
            }
        }
    }

    @Test
    fun muestraLasOpcionesVisitanteYColeccionistaConSuBotonEntrar() {
        mostrar()

        rule.onNodeWithText("Visitante").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Coleccionista").performScrollTo().assertIsDisplayed()
        // Each button is a single element and is announced with its content description (the "Entrar" text is inside it).
        rule.onNodeWithTag(TagEntrarVisitante).assertContentDescriptionEquals("Entrar como Visitante")
        rule.onNodeWithTag(TagEntrarColeccionista).assertContentDescriptionEquals("Entrar como Coleccionista")
    }

    @Test
    fun muestraLaAudioWaveStripConSuBpmYNoLaInsigniaDeVersion() {
        mostrar()

        rule.onNodeWithText("AUDIO WAVE", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("94 BPM", useUnmergedTree = true).assertIsDisplayed()
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
