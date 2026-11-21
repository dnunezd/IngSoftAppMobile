package com.example.vinilos.ui.artistas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistaDetalleUiState
import com.example.vinilos.viewmodel.AsociarAlbumUiState
import com.example.vinilos.viewmodel.ListadoAsociable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Acceso a "Agregar álbum" desde el detalle del artista (HU15, criterios 2, 7 y 11). */
class PantallaDetalleArtistaAsociarTest {
    @get:Rule
    val rule = createComposeRule()

    private val ruben = ArtistaDetalle(
        id = 100, nombre = "Rubén Blades", imagen = null, descripcion = null, tipo = ArtistType.MUSICO,
        fecha = null, integrantes = emptyList(),
        albumes = listOf(AlbumArtista(100, "Buscando América", null, "1984"), AlbumArtista(101, "Poeta del pueblo", null, "1984")),
        premios = emptyList()
    )

    private val pedidos = mutableListOf<Set<Int>>()
    private var avisosMostrados = 0

    private fun mostrar(estado: ArtistaDetalleUiState, estadoAsociar: AsociarAlbumUiState?) {
        rule.setContent {
            VinilosTheme {
                PantallaDetalleArtista(
                    estado = estado,
                    onSeleccionarAlbum = {},
                    onReintentar = {},
                    onAtras = {},
                    estadoAsociar = estadoAsociar,
                    onAgregarAlbum = { pedidos += it },
                    onAlbumAsociadoMostrado = { avisosMostrados++ }
                )
            }
        }
    }

    private fun irAlBoton() {
        rule.onNodeWithTag(TagListaDetalleArtista).performScrollToNode(hasTestTag(TagBotonAgregarAlbum))
    }

    // Criterio 11
    @Test
    fun elVisitanteNoVeAgregarAlbum() {
        mostrar(ArtistaDetalleUiState.Exito(ruben), estadoAsociar = null)

        rule.onNodeWithTag(TagBotonAgregarAlbum).assertDoesNotExist()
        rule.onNodeWithText("Agregar álbum").assertDoesNotExist()
    }

    // Criterio 2
    @Test
    fun elColeccionistaVeAgregarAlbum() {
        mostrar(ArtistaDetalleUiState.Exito(ruben), estadoAsociar = AsociarAlbumUiState())

        irAlBoton()
        rule.onNodeWithTag(TagBotonAgregarAlbum).assertIsDisplayed()
        rule.onNodeWithText("Agregar álbum").assertIsDisplayed()
    }

    @Test
    fun agregarAlbumPideAbrirLaVentanaConLosAlbumesQueYaTieneElArtista() {
        mostrar(ArtistaDetalleUiState.Exito(ruben), estadoAsociar = AsociarAlbumUiState())

        irAlBoton()
        rule.onNodeWithTag(TagBotonAgregarAlbum).performClick()

        assertEquals(listOf(setOf(100, 101)), pedidos)
    }

    @Test
    fun mientrasCargaElDetalleNoHayBotonAgregarAlbum() {
        mostrar(ArtistaDetalleUiState.Cargando, estadoAsociar = AsociarAlbumUiState())

        rule.onNodeWithTag(TagBotonAgregarAlbum).assertDoesNotExist()
    }

    @Test
    fun conLaVentanaAbiertaMuestraElNombreDelArtista() {
        mostrar(
            ArtistaDetalleUiState.Exito(ruben),
            estadoAsociar = AsociarAlbumUiState(abierta = true, listado = ListadoAsociable.Listo(emptyList()))
        )
        rule.waitForIdle()

        rule.onNodeWithText("Asociar álbum a Rubén Blades").assertIsDisplayed()
    }

    // Criterio 7
    @Test
    fun alAsociarMuestraElMensajeAlbumAsociado() {
        mostrar(ArtistaDetalleUiState.Exito(ruben, mostrarAlbumAsociado = true), estadoAsociar = AsociarAlbumUiState())

        rule.waitUntil(5_000) { rule.onAllNodesConTexto("Álbum asociado") }
        rule.onNodeWithText("Álbum asociado").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesConTexto(texto: String): Boolean =
        onAllNodes(androidx.compose.ui.test.hasText(texto)).fetchSemanticsNodes().isNotEmpty()
}
