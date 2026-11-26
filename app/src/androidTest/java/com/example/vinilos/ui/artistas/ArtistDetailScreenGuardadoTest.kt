package com.example.vinilos.ui.artistas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistDetailUiState
import org.junit.Rule
import org.junit.Test

/** Detalle de artista guardado (add-cache-consultas, D6): indicador de actualización y aviso. */
class ArtistDetailScreenGuardadoTest {

    @get:Rule
    val rule = createComposeRule()

    private val queen = ArtistDetail(
        101, "Queen", null, "Banda británica", ArtistType.BANDA, "1970-01-01T00:00:00.000Z",
        emptyList(), emptyList(), emptyList()
    )

    private var avisos = 0

    private fun mostrar(estado: ArtistDetailUiState) {
        rule.setContent {
            VinilosTheme {
                ArtistDetailScreen(
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
        mostrar(ArtistDetailUiState.Success(queen, actualizando = true))

        rule.onNodeWithTag(TagActualizandoArtistDetail).assertIsDisplayed()
        rule.onNodeWithContentDescription("Actualizando artista").assertIsDisplayed()
        rule.onNodeWithTag(TagNombreArtistDetail).assertIsDisplayed()
        rule.onNodeWithTag(TagProgresoArtistDetail).assertDoesNotExist()
    }

    @Test
    fun siNoSePudoActualizarAvisaUnaVezSinQuitarElDetalle() {
        mostrar(ArtistDetailUiState.Success(queen, errorAlActualizar = true))

        rule.onNodeWithText("No se pudo actualizar el artista").assertIsDisplayed()
        rule.onNodeWithTag(TagNombreArtistDetail).assertIsDisplayed()
        rule.onNodeWithText("No se pudo cargar el artista").assertDoesNotExist()
        rule.waitUntil(10_000) { avisos == 1 }
    }

    /** La fecha se guarda en ISO y la pantalla la muestra en el idioma de la app (internacionalizar-ingles). */
    @Test
    fun laFechaGuardadaSeMuestraEnElAppLanguage() {
        mostrar(ArtistDetailUiState.Success(queen))

        rule.onNodeWithTag(TagFechaArtistDetail).assertTextEquals("1 de enero de 1970")
    }
}
