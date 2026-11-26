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
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistDetailUiState
import com.example.vinilos.viewmodel.AssociateAlbumUiState
import com.example.vinilos.viewmodel.ListadoAsociable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Acceso a "Agregar álbum" desde el detalle del artista (HU15, criterios 2, 7 y 11). */
class ArtistDetailScreenAsociarTest {
    @get:Rule
    val rule = createComposeRule()

    private val ruben = ArtistDetail(
        id = 100, nombre = "Rubén Blades", imagen = null, descripcion = null, tipo = ArtistType.MUSICO,
        fecha = null, integrantes = emptyList(),
        albumes = listOf(AlbumArtista(100, "Buscando América", null, "1984"), AlbumArtista(101, "Poeta del pueblo", null, "1984")),
        premios = emptyList()
    )

    private val pedidos = mutableListOf<Set<Int>>()
    private var avisosMostrados = 0

    private fun mostrar(estado: ArtistDetailUiState, estadoAsociar: AssociateAlbumUiState?) {
        rule.setContent {
            VinilosTheme {
                ArtistDetailScreen(
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
        rule.onNodeWithTag(TagListaArtistDetail).performScrollToNode(hasTestTag(TagBotonAgregarAlbum))
    }

    // Criterio 11
    @Test
    fun elVisitanteNoVeAgregarAlbum() {
        mostrar(ArtistDetailUiState.Success(ruben), estadoAsociar = null)

        rule.onNodeWithTag(TagBotonAgregarAlbum).assertDoesNotExist()
        rule.onNodeWithText("Agregar álbum").assertDoesNotExist()
    }

    // Criterio 2
    @Test
    fun elColeccionistaVeAgregarAlbum() {
        mostrar(ArtistDetailUiState.Success(ruben), estadoAsociar = AssociateAlbumUiState())

        irAlBoton()
        rule.onNodeWithTag(TagBotonAgregarAlbum).assertIsDisplayed()
        rule.onNodeWithText("Agregar álbum").assertIsDisplayed()
    }

    @Test
    fun agregarAlbumPideAbrirLaVentanaConLosAlbumesQueYaTieneElArtista() {
        mostrar(ArtistDetailUiState.Success(ruben), estadoAsociar = AssociateAlbumUiState())

        irAlBoton()
        rule.onNodeWithTag(TagBotonAgregarAlbum).performClick()

        assertEquals(listOf(setOf(100, 101)), pedidos)
    }

    @Test
    fun mientrasCargaElDetalleNoHayBotonAgregarAlbum() {
        mostrar(ArtistDetailUiState.Loading, estadoAsociar = AssociateAlbumUiState())

        rule.onNodeWithTag(TagBotonAgregarAlbum).assertDoesNotExist()
    }

    @Test
    fun conLaVentanaAbiertaMuestraElNombreDelArtista() {
        mostrar(
            ArtistDetailUiState.Success(ruben),
            estadoAsociar = AssociateAlbumUiState(abierta = true, listado = ListadoAsociable.Listo(emptyList()))
        )
        rule.waitForIdle()

        rule.onNodeWithText("Asociar álbum a Rubén Blades").assertIsDisplayed()
    }

    // Criterio 7
    @Test
    fun alAsociarMuestraElMensajeAlbumAsociado() {
        mostrar(ArtistDetailUiState.Success(ruben, mostrarAlbumAsociado = true), estadoAsociar = AssociateAlbumUiState())

        rule.waitUntil(5_000) { rule.onAllNodesConTexto("Álbum asociado") }
        rule.onNodeWithText("Álbum asociado").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesConTexto(texto: String): Boolean =
        onAllNodes(androidx.compose.ui.test.hasText(texto)).fetchSemanticsNodes().isNotEmpty()
}
