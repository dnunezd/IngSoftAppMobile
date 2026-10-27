package com.example.vinilos.ui.artistas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PantallaArtistasTest {

    @get:Rule
    val rule = createComposeRule()

    private val queen = Artist(101, "Queen", null, ArtistType.BANDA)
    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", "http://localhost:1/no-existe.jpg", ArtistType.MUSICO)
    private val todos = listOf(queen, ruben)

    private var estado by mutableStateOf<ArtistsUiState>(ArtistsUiState.Cargando)
    private var buscado: String? = null
    private var reintentos = 0
    private var seleccionado: Artist? = null
    private var atras = 0

    private fun mostrar(inicial: ArtistsUiState) {
        estado = inicial
        rule.setContent {
            VinilosTheme {
                PantallaArtistas(
                    estado = estado,
                    onBuscar = {
                        buscado = it
                        // Como el ViewModel: la búsqueda solo cambia el estado cuando ya hay datos.
                        (estado as? ArtistsUiState.Exito)?.let { actual -> estado = actual.copy(consulta = it) }
                    },
                    onReintentar = { reintentos++ },
                    onSeleccionar = { seleccionado = it },
                    onAtras = { atras++ }
                )
            }
        }
    }

    @Test
    fun muestraElEncabezadoYElTitularDelMockup() {
        mostrar(ArtistsUiState.Exito(todos))

        rule.onNodeWithText("Explora").assertIsDisplayed()
        rule.onNodeWithText("artistas").assertIsDisplayed()
        rule.onNodeWithText("LEYENDAS & BANDAS").assertIsDisplayed()
        rule.onNodeWithText("Músicos y bandas del catálogo, en orden alfabético.").assertIsDisplayed()
        rule.onNodeWithText("Buscar artista por nombre...").assertIsDisplayed()
    }

    @Test
    fun cadaFilaMuestraNombreYEtiquetaSegunElTipo() {
        mostrar(ArtistsUiState.Exito(todos))

        rule.onNodeWithTag(tagArtista(queen)).assertIsDisplayed()
        rule.onNodeWithText("Queen").assertIsDisplayed()
        rule.onNodeWithText("Banda").assertIsDisplayed()
        rule.onNodeWithText("Rubén Blades Bellido de Luna").assertIsDisplayed()
        rule.onNodeWithText("Músico").assertIsDisplayed()
    }

    @Test
    fun lasFilasRespetanElOrdenDelEstado() {
        mostrar(ArtistsUiState.Exito(todos))

        // onAllNodes devuelve los nodos en el orden del árbol, que es el orden en pantalla.
        val filas = rule.onAllNodes(hasTestTag(tagArtista(queen)) or hasTestTag(tagArtista(ruben)))
        filas.assertCountEquals(2)
        filas[0].assert(hasTestTag(tagArtista(queen)))
        filas[1].assert(hasTestTag(tagArtista(ruben)))
    }

    @Test
    fun elContadorIndicaLosArtistasVisibles() {
        mostrar(ArtistsUiState.Exito(todos))
        rule.onNodeWithTag(TagContadorArtistas).assertTextEquals("2 artistas")
    }

    @Test
    fun escribirEnElBuscadorAvisaYFiltra() {
        mostrar(ArtistsUiState.Exito(todos))

        rule.onNodeWithTag(TagBuscadorArtistas).performTextInput("que")
        rule.waitForIdle()

        assertEquals("que", buscado)
        rule.onNodeWithTag(TagContadorArtistas).assertTextEquals("1 artista")
        rule.onAllNodesWithTag(tagArtista(ruben)).assertCountEquals(0)
    }

    @Test
    fun busquedaSinCoincidenciasMuestraElMensajeYConservaElBuscador() {
        mostrar(ArtistsUiState.Exito(todos))

        rule.onNodeWithTag(TagBuscadorArtistas).performTextInput("zzz")
        rule.waitForIdle()

        rule.onNodeWithTag(TagVacioArtistas).assertTextEquals("No se encontraron artistas")
        rule.onNodeWithText("Prueba con otro nombre.").assertIsDisplayed()
        rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals("zzz")
    }

    @Test
    fun catalogoSinArtistasMuestraElMensaje() {
        mostrar(ArtistsUiState.Vacio)

        rule.onNodeWithTag(TagVacioArtistas).assertTextEquals("No se encontraron artistas")
        rule.onAllNodesWithTag(TagContadorArtistas).assertCountEquals(0)
    }

    @Test
    fun mientrasCargaMuestraElIndicadorYElBuscadorDeshabilitado() {
        mostrar(ArtistsUiState.Cargando)

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
        mostrar(ArtistsUiState.Exito(listOf(ruben)))
        rule.waitForIdle()

        rule.onNodeWithTag(tagArtista(ruben)).assertIsDisplayed()
        rule.onNodeWithText("Rubén Blades Bellido de Luna").assertIsDisplayed()
    }

    @Test
    fun tocarUnaFilaSeleccionaEseArtista() {
        mostrar(ArtistsUiState.Exito(todos))

        rule.onNodeWithTag(tagArtista(ruben)).performScrollTo().performClick()

        assertEquals(ruben, seleccionado)
    }

    @Test
    fun laFlechaDeAtrasAvisa() {
        mostrar(ArtistsUiState.Exito(todos))

        rule.onNodeWithTag(TagAtras).performClick()

        assertEquals(1, atras)
    }
}
