package com.example.vinilos.ui.artistas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistaDetalleUiState
import org.junit.Rule
import org.junit.Test

/** Detalle de artista guardado (add-cache-consultas, D6): indicador de actualización y aviso. */
class PantallaDetalleArtistaGuardadoTest {

    @get:Rule
    val rule = createComposeRule()

    private val queen = ArtistaDetalle(
        101, "Queen", null, "Banda británica", ArtistType.BANDA, "1 de enero de 1970",
        emptyList(), emptyList(), emptyList()
    )

    private var avisos = 0

    private fun mostrar(estado: ArtistaDetalleUiState) {
        rule.setContent {
            VinilosTheme {
                PantallaDetalleArtista(
                    estado = estado,
                    onSeleccionarAlbum = {},
                    onReintentar = {},
                    onAtras = {},
                    onAvisoMostrado = { avisos++ }
                )
            }
        }
    }

    @Test
    fun conElDetalleGuardadoActualizandoseMuestraElIndicadorPequeno() {
        mostrar(ArtistaDetalleUiState.Exito(queen, actualizando = true))

        rule.onNodeWithTag(TagActualizandoDetalleArtista).assertIsDisplayed()
        rule.onNodeWithContentDescription("Actualizando artista").assertIsDisplayed()
        rule.onNodeWithTag(TagNombreDetalleArtista).assertIsDisplayed()
        rule.onNodeWithTag(TagProgresoDetalleArtista).assertDoesNotExist()
    }

    @Test
    fun siNoSePudoActualizarAvisaUnaVezSinQuitarElDetalle() {
        mostrar(ArtistaDetalleUiState.Exito(queen, errorAlActualizar = true))

        rule.onNodeWithText("No se pudo actualizar el artista").assertIsDisplayed()
        rule.onNodeWithTag(TagNombreDetalleArtista).assertIsDisplayed()
        rule.onNodeWithText("No se pudo cargar el artista").assertDoesNotExist()
        rule.waitUntil(10_000) { avisos == 1 }
    }
}
