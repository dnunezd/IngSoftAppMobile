package com.example.vinilos.ui.ingreso

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.vinilos.data.model.Collector
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.CollectorsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PantallaColeccionistasTest {

    @get:Rule
    val rule = createComposeRule()

    private val manolo = Collector(100, "Manolo Bellon", "manollo@caracol.com.co")
    private val jaime = Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co")

    private var buscado: String? = null
    private var reintentos = 0
    private var seleccionado: Collector? = null
    private var visitante = 0
    private var atras = 0

    private fun mostrar(estado: CollectorsUiState) {
        rule.setContent {
            VinilosTheme {
                PantallaColeccionistas(
                    estado = estado,
                    onBuscar = { buscado = it },
                    onReintentar = { reintentos++ },
                    onSeleccionar = { seleccionado = it },
                    onVisitante = { visitante++ },
                    onAtras = { atras++ }
                )
            }
        }
    }

    @Test
    fun laFranjaAudioFeedSeMuestraEnTodosLosEstados() {
        mostrar(CollectorsUiState.Exito(listOf(manolo, jaime)))
        rule.onNodeWithText("AUDIO FEED").assertIsDisplayed()
    }

    @Test
    fun laFranjaAudioFeedSeMuestraMientrasCarga() {
        mostrar(CollectorsUiState.Cargando)
        rule.onNodeWithText("AUDIO FEED").assertIsDisplayed()
    }

    @Test
    fun laFranjaAudioFeedSeMuestraConError() {
        mostrar(CollectorsUiState.Error)
        rule.onNodeWithText("AUDIO FEED").assertIsDisplayed()
    }

    @Test
    fun laFranjaAudioFeedSeMuestraSinColeccionistas() {
        mostrar(CollectorsUiState.Vacio)
        rule.onNodeWithText("AUDIO FEED").assertIsDisplayed()
    }

    @Test
    fun cargandoMuestraElIndicadorDeProgresoYNoElBuscador() {
        mostrar(CollectorsUiState.Cargando)

        rule.onNodeWithTag(TagProgreso).assertIsDisplayed()
        rule.onNodeWithTag(TagBuscador).assertDoesNotExist()
    }

    @Test
    fun conDatosMuestraAvatarNombreYCorreoDeCadaColeccionista() {
        mostrar(CollectorsUiState.Exito(listOf(manolo, jaime)))

        rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
        rule.onNodeWithText("manollo@caracol.com.co").assertIsDisplayed()
        rule.onNodeWithText("MB").assertIsDisplayed()
        rule.onNodeWithText("Jaime Monsalve").assertIsDisplayed()
        rule.onNodeWithText("jmonsalve@rtvc.com.co").assertIsDisplayed()
        rule.onNodeWithText("JM").assertIsDisplayed()
        rule.onNodeWithText("Buscar coleccionista...").assertIsDisplayed()
    }

    @Test
    fun seleccionarUnColeccionistaAvisaConEseColeccionista() {
        mostrar(CollectorsUiState.Exito(listOf(manolo, jaime)))

        rule.onNodeWithTag(tagColeccionista(101)).performClick()

        assertEquals(jaime, seleccionado)
    }

    @Test
    fun escribirEnElBuscadorAvisaConElTexto() {
        mostrar(CollectorsUiState.Exito(listOf(manolo, jaime)))

        rule.onNodeWithTag(TagBuscador).performTextInput("bel")

        assertEquals("bel", buscado)
    }

    @Test
    fun sinCoincidenciasMuestraElMensajeYConservaElBuscador() {
        mostrar(CollectorsUiState.Exito(listOf(manolo, jaime), consulta = "zzz"))

        rule.onNodeWithText("No encontramos coleccionistas con esa búsqueda").assertIsDisplayed()
        rule.onNodeWithTag(TagBuscador).assertIsDisplayed()
        rule.onNodeWithTag(tagColeccionista(100)).assertDoesNotExist()
    }

    @Test
    fun conUnaConsultaSoloMuestraLosVisibles() {
        mostrar(CollectorsUiState.Exito(listOf(manolo, jaime), consulta = "bel"))

        rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
        rule.onNodeWithText("Jaime Monsalve").assertDoesNotExist()
    }

    @Test
    fun errorMuestraElMensajeReintentarYLaOpcionVisitante() {
        mostrar(CollectorsUiState.Error)

        rule.onNodeWithText("No pudimos cargar los coleccionistas").assertIsDisplayed()
        rule.onNodeWithText("Reintentar").assertIsDisplayed()
        rule.onNodeWithTag(TagVisitanteError).assertIsDisplayed()
        rule.onNodeWithTag(TagBuscador).assertDoesNotExist()
    }

    @Test
    fun reintentarYVisitanteDelErrorAvisan() {
        mostrar(CollectorsUiState.Error)

        rule.onNodeWithTag(TagReintentar).performClick()
        rule.onNodeWithTag(TagVisitanteError).performClick()

        assertEquals(1, reintentos)
        assertEquals(1, visitante)
    }

    @Test
    fun vacioMuestraElMensajeYLaOpcionDeVolver() {
        mostrar(CollectorsUiState.Vacio)

        rule.onNodeWithText("Aún no hay coleccionistas registrados").assertIsDisplayed()
        rule.onNodeWithTag(TagBuscador).assertDoesNotExist()

        rule.onNodeWithTag(TagVolver).performClick()
        assertEquals(1, atras)
    }

    @Test
    fun laFlechaDeAtrasAvisa() {
        mostrar(CollectorsUiState.Exito(listOf(manolo)))

        rule.onNodeWithTag(TagAtras).performClick()

        assertEquals(1, atras)
    }
}
