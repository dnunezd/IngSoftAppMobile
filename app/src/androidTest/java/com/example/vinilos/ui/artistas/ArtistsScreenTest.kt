package com.example.vinilos.ui.artistas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.ui.entry.TagAtras
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArtistsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val queen = Artist(101, "Queen", null, ArtistType.BANDA)
    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", "http://localhost:1/no-existe.jpg", ArtistType.MUSICO)
    private val todos = listOf(queen, ruben)

    private var estado by mutableStateOf<ArtistsUiState>(ArtistsUiState.Loading)
    private var avisos = 0
    private var buscado: String? = null
    private var reintentos = 0
    private var seleccionado: Artist? = null
    private var atras = 0

    private fun mostrar(inicial: ArtistsUiState) {
        estado = inicial
        rule.setContent {
            VinilosTheme {
                ArtistsScreen(
                    estado = estado,
                    onBuscar = {
                        buscado = it
                        // Like the ViewModel: searching only changes the state once there is data.
                        (estado as? ArtistsUiState.Success)?.let { actual -> estado = actual.copy(consulta = it) }
                    },
                    onReintentar = { reintentos++ },
                    onSeleccionar = { seleccionado = it },
                    onAtras = { atras++ },
                    onAvisoMostrado = { avisos++ }
                )
            }
        }
    }

    /** On short screens the list only composes what is visible: it must be scrolled to the node. */
    private fun verEnLista(nodo: SemanticsMatcher) =
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(nodo)

    @Test
    fun muestraElEncabezadoYElTitularDelMockup() {
        mostrar(ArtistsUiState.Success(todos))

        rule.onNodeWithText("Explora").assertIsDisplayed()
        rule.onNodeWithText("artistas").assertIsDisplayed()
        rule.onNodeWithText("LEYENDAS & BANDAS").assertIsDisplayed()
        rule.onNodeWithText("Músicos y bandas del catálogo, en orden alfabético.").assertIsDisplayed()
        rule.onNodeWithText("Buscar artista por nombre...").assertIsDisplayed()
    }

    @Test
    fun cadaFilaMuestraNombreYEtiquetaSegunElTipo() {
        mostrar(ArtistsUiState.Success(todos))

        verEnLista(hasTestTag(tagArtista(queen)))
        rule.onNodeWithTag(tagArtista(queen)).assertIsDisplayed()
        rule.onNodeWithText("Queen").assertIsDisplayed()
        rule.onNodeWithText("Banda").assertIsDisplayed()
        verEnLista(hasTestTag(tagArtista(ruben)))
        rule.onNodeWithText("Rubén Blades Bellido de Luna").assertIsDisplayed()
        rule.onNodeWithText("Músico").assertIsDisplayed()
    }

    @Test
    fun lasFilasRespetanElOrdenDelEstado() {
        mostrar(ArtistsUiState.Success(todos))
        // With the first row at the top, the second one also ends up composed.
        verEnLista(hasTestTag(tagArtista(queen)))

        // onAllNodes returns the nodes in tree order, which is the order on screen.
        val filas = rule.onAllNodes(hasTestTag(tagArtista(queen)) or hasTestTag(tagArtista(ruben)))
        filas.assertCountEquals(2)
        filas[0].assert(hasTestTag(tagArtista(queen)))
        filas[1].assert(hasTestTag(tagArtista(ruben)))
    }

    @Test
    fun elContadorIndicaLosArtistasVisibles() {
        mostrar(ArtistsUiState.Success(todos))
        rule.onNodeWithTag(TagContadorArtistas).assertTextEquals("2 artistas")
    }

    @Test
    fun escribirEnElBuscadorAvisaYFiltra() {
        mostrar(ArtistsUiState.Success(todos))

        rule.onNodeWithTag(TagBuscadorArtistas).performTextInput("que")
        rule.waitForIdle()

        assertEquals("que", buscado)
        rule.onNodeWithTag(TagContadorArtistas).assertTextEquals("1 artista")
        rule.onAllNodesWithTag(tagArtista(ruben)).assertCountEquals(0)
    }

    @Test
    fun busquedaSinCoincidenciasMuestraElMensajeYConservaElBuscador() {
        mostrar(ArtistsUiState.Success(todos))

        rule.onNodeWithTag(TagBuscadorArtistas).performTextInput("zzz")
        rule.waitForIdle()

        rule.onNodeWithTag(TagEmptyArtistas).assertTextEquals("No se encontraron artistas")
        verEnLista(hasText("Prueba con otro nombre."))
        rule.onNodeWithText("Prueba con otro nombre.").assertIsDisplayed()
        rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals("zzz")
    }

    @Test
    fun catalogoSinArtistasMuestraElMensaje() {
        mostrar(ArtistsUiState.Empty)

        rule.onNodeWithTag(TagEmptyArtistas).assertTextEquals("No se encontraron artistas")
        rule.onAllNodesWithTag(TagContadorArtistas).assertCountEquals(0)
    }

    @Test
    fun mientrasCargaMuestraElIndicadorYElBuscadorDeshabilitado() {
        mostrar(ArtistsUiState.Loading)

        rule.onNodeWithTag(TagProgresoArtistas).assertIsDisplayed()
        rule.onNodeWithText("Cargando artistas...").assertIsDisplayed()
        rule.onNodeWithTag(TagBuscadorArtistas).assertIsNotEnabled()
    }

    @Test
    fun errorMuestraElMensajeYReintentarAvisa() {
        mostrar(ArtistsUiState.Error)

        rule.onNodeWithTag(TagErrorArtistas).assertTextEquals("No se pudo cargar el listado de artistas")
        rule.onNodeWithTag(TagReintentarArtistas).performScrollTo().performClick()

        assertEquals(1, reintentos)
        rule.onAllNodesWithTag(TagProgresoArtistas).assertCountEquals(0)
    }

    @Test
    fun imagenQueNoCargaNoRompeLaFila() {
        mostrar(ArtistsUiState.Success(listOf(ruben)))
        rule.waitForIdle()

        rule.onNodeWithTag(tagArtista(ruben)).assertIsDisplayed()
        rule.onNodeWithText("Rubén Blades Bellido de Luna").assertIsDisplayed()
    }

    @Test
    fun tocarUnaFilaSeleccionaEseArtista() {
        mostrar(ArtistsUiState.Success(todos))

        verEnLista(hasTestTag(tagArtista(ruben)))
        rule.onNodeWithTag(tagArtista(ruben)).performClick()

        assertEquals(ruben, seleccionado)
    }

    @Test
    fun laFlechaDeAtrasAvisa() {
        mostrar(ArtistsUiState.Success(todos))

        rule.onNodeWithTag(TagAtras).performClick()

        assertEquals(1, atras)
    }

    // ---------- Saved listing (add-cache-consultas, D6) ----------

    @Test
    fun conElListadoGuardadoActualizandoseMuestraElIndicadorPequenoYLaLista() {
        mostrar(ArtistsUiState.Success(todos, actualizando = true))

        rule.onNodeWithTag(TagActualizandoArtistas).assertIsDisplayed()
        rule.onNodeWithText("Queen").assertIsDisplayed()
        rule.onNodeWithTag(TagProgresoArtistas).assertDoesNotExist()
    }

    @Test
    fun siNoSePudoActualizarAvisaUnaVezSinQuitarLaLista() {
        mostrar(ArtistsUiState.Success(todos, errorAlActualizar = true))

        rule.onNodeWithText("No se pudo actualizar el listado de artistas").assertIsDisplayed()
        rule.onNodeWithText("Queen").assertIsDisplayed()
        rule.onNodeWithTag(TagErrorArtistas).assertDoesNotExist()
        rule.waitUntil(10_000) { avisos == 1 }
    }
}
