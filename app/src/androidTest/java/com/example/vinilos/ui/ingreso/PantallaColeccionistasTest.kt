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
        // Given
        val estado = CollectorsUiState.Exito(listOf(manolo, jaime))

        // When
        mostrar(estado)

        // Then
        rule.onNodeWithText("AUDIO FEED").assertIsDisplayed()
    }

    @Test
    fun laFranjaAudioFeedSeMuestraMientrasCarga() {
        // Given
        val estado = CollectorsUiState.Cargando

        // When
        mostrar(estado)

        // Then
        rule.onNodeWithText("AUDIO FEED").assertIsDisplayed()
    }

    @Test
    fun laFranjaAudioFeedSeMuestraConError() {
        // Given
        val estado = CollectorsUiState.Error

        // When
        mostrar(estado)

        // Then
        rule.onNodeWithText("AUDIO FEED").assertIsDisplayed()
    }

    @Test
    fun laFranjaAudioFeedSeMuestraSinColeccionistas() {
        // Given
        val estado = CollectorsUiState.Vacio

        // When
        mostrar(estado)

        // Then
        rule.onNodeWithText("AUDIO FEED").assertIsDisplayed()
    }

    @Test
    fun cargandoMuestraElIndicadorDeProgresoYNoElBuscador() {
        // Given
        val estado = CollectorsUiState.Cargando

        // When
        mostrar(estado)

        // Then
        rule.onNodeWithTag(TagProgreso).assertIsDisplayed()
        rule.onNodeWithTag(TagBuscador).assertDoesNotExist()
    }

    @Test
    fun conDatosMuestraAvatarNombreYCorreoDeCadaColeccionista() {
        // Given
        val estado = CollectorsUiState.Exito(listOf(manolo, jaime))

        // When
        mostrar(estado)

        // Then
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
        // Given
        mostrar(CollectorsUiState.Exito(listOf(manolo, jaime)))

        // When
        rule.onNodeWithTag(tagColeccionista(101)).performClick()

        // Then
        assertEquals(jaime, seleccionado)
    }

    @Test
    fun escribirEnElBuscadorAvisaConElTexto() {
        // Given
        mostrar(CollectorsUiState.Exito(listOf(manolo, jaime)))

        // When
        rule.onNodeWithTag(TagBuscador).performTextInput("bel")

        // Then
        assertEquals("bel", buscado)
    }

    @Test
    fun sinCoincidenciasMuestraElMensajeYConservaElBuscador() {
        // Given
        val estado = CollectorsUiState.Exito(listOf(manolo, jaime), consulta = "zzz")

        // When
        mostrar(estado)

        // Then
        rule.onNodeWithText("No encontramos coleccionistas con esa búsqueda").assertIsDisplayed()
        rule.onNodeWithTag(TagBuscador).assertIsDisplayed()
        rule.onNodeWithTag(tagColeccionista(100)).assertDoesNotExist()
    }

    @Test
    fun conUnaConsultaSoloMuestraLosVisibles() {
        // Given
        val estado = CollectorsUiState.Exito(listOf(manolo, jaime), consulta = "bel")

        // When
        mostrar(estado)

        // Then
        rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
        rule.onNodeWithText("Jaime Monsalve").assertDoesNotExist()
    }

    @Test
    fun errorMuestraElMensajeReintentarYLaOpcionVisitante() {
        // Given
        val estado = CollectorsUiState.Error

        // When
        mostrar(estado)

        // Then
        rule.onNodeWithText("No pudimos cargar los coleccionistas").assertIsDisplayed()
        rule.onNodeWithText("Reintentar").assertIsDisplayed()
        rule.onNodeWithTag(TagVisitanteError).assertIsDisplayed()
        rule.onNodeWithTag(TagBuscador).assertDoesNotExist()
    }

    @Test
    fun reintentarDelErrorAvisaSoloAReintentar() {
        // Given
        mostrar(CollectorsUiState.Error)

        // When
        rule.onNodeWithTag(TagReintentar).performClick()

        // Then
        assertEquals(1, reintentos)
        assertEquals(0, visitante)
    }

    @Test
    fun visitanteDelErrorAvisaSoloAlVisitante() {
        // Given
        mostrar(CollectorsUiState.Error)

        // When
        rule.onNodeWithTag(TagVisitanteError).performClick()

        // Then
        assertEquals(0, reintentos)
        assertEquals(1, visitante)
    }

    @Test
    fun vacioMuestraElMensajeYNoElBuscador() {
        // Given
        val estado = CollectorsUiState.Vacio

        // When
        mostrar(estado)

        // Then
        rule.onNodeWithText("Aún no hay coleccionistas registrados").assertIsDisplayed()
        rule.onNodeWithTag(TagBuscador).assertDoesNotExist()
    }

    @Test
    fun volverDesdeVacioAvisa() {
        // Given
        mostrar(CollectorsUiState.Vacio)

        // When
        rule.onNodeWithTag(TagVolver).performClick()

        // Then
        assertEquals(1, atras)
    }

    @Test
    fun laFlechaDeAtrasAvisa() {
        // Given
        mostrar(CollectorsUiState.Exito(listOf(manolo)))

        // When
        rule.onNodeWithTag(TagAtras).performClick()

        // Then
        assertEquals(1, atras)
    }
}
