package com.example.vinilos.ui.artistas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistDetailUiState
import com.example.vinilos.viewmodel.FavoriteUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Favorite button on the real artist detail screen (HU10 over HU04), one state at a time. */
class ArtistDetailScreenFavoritoTest {
    @get:Rule
    val rule = createComposeRule()

    private val rubenBlades = ArtistDetail(
        id = 100,
        nombre = "Rubén Blades",
        imagen = null,
        descripcion = null,
        tipo = ArtistType.MUSICO,
        fecha = null,
        integrantes = emptyList(),
        albumes = emptyList(),
        premios = emptyList()
    )

    private fun mostrar(
        estadoFavorito: FavoriteUiState?,
        onAtras: () -> Unit = {},
        onVolverAlListado: () -> Unit = onAtras,
        onAgregarFavorito: () -> Unit = {},
        onReintentarFavorito: () -> Unit = {},
        onConfirmacionFavoritoMostrada: () -> Unit = {}
    ) {
        rule.setContent {
            VinilosTheme {
                ArtistDetailScreen(
                    estado = ArtistDetailUiState.Success(rubenBlades),
                    onSeleccionarAlbum = {},
                    onReintentar = {},
                    onAtras = onAtras,
                    estadoFavorito = estadoFavorito,
                    onAgregarFavorito = onAgregarFavorito,
                    onReintentarFavorito = onReintentarFavorito,
                    onConfirmacionFavoritoMostrada = onConfirmacionFavoritoMostrada,
                    onVolverAlListado = onVolverAlListado
                )
            }
        }
    }

    @Test
    fun visitanteNoVeNingunBotonDeFavorito() {
        mostrar(estadoFavorito = null)

        rule.onNodeWithTag(TagBotonFavorito).assertDoesNotExist()
    }

    @Test
    fun comprobandoMuestraElBotonDesactivado() {
        mostrar(estadoFavorito = FavoriteUiState.Comprobando)

        rule.onNodeWithTag(TagBotonFavorito).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun noFavoritoMuestraElBotonActivoParaAgregar() {
        var toques = 0
        mostrar(estadoFavorito = FavoriteUiState.NoFavorito, onAgregarFavorito = { toques++ })

        rule.onNodeWithText("Agregar Favorito").assertIsDisplayed().assertIsEnabled().performClick()
        assertEquals(1, toques)
    }

    @Test
    fun guardandoMuestraElBotonDesactivadoConSuTexto() {
        mostrar(estadoFavorito = FavoriteUiState.Guardando)

        rule.onNodeWithText("Guardando...").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun favoritoMuestraElBotonDesactivadoConSuTexto() {
        mostrar(estadoFavorito = FavoriteUiState.Favorito(mostrarConfirmacion = false))

        rule.onNodeWithText("Favorito").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun errorComprobacionMuestraElBotonDesactivadoYPermiteReintentar() {
        var reintentos = 0
        mostrar(
            estadoFavorito = FavoriteUiState.ErrorComprobacion,
            onReintentarFavorito = { reintentos++ }
        )

        rule.onNodeWithTag(TagBotonFavorito).assertIsNotEnabled()
        rule.onNodeWithTag(TagErrorFavorito).assertIsDisplayed()
        rule.onNodeWithTag(TagReintentarFavorito).performClick()
        assertEquals(1, reintentos)
    }

    @Test
    fun errorGuardadoMuestraElMensajeYElBotonDisponibleParaReintentar() {
        var toques = 0
        mostrar(estadoFavorito = FavoriteUiState.ErrorGuardado, onAgregarFavorito = { toques++ })

        rule.onNodeWithTag(TagErrorFavorito).assertIsDisplayed()
        rule.onNodeWithTag(TagBotonFavorito).assertDoesNotExist()
        rule.onNodeWithText("Reintentar guardar").assertIsEnabled().performClick()
        assertEquals(1, toques)
    }

    @Test
    fun errorGuardadoNoMuestraNingunaConfirmacion() {
        mostrar(estadoFavorito = FavoriteUiState.ErrorGuardado)

        rule.onNodeWithTag(TagConfirmacionFavorito).assertDoesNotExist()
        rule.onNodeWithText("Rubén Blades se agregó a tus favoritos").assertDoesNotExist()
    }

    @Test
    fun noDisponibleMuestraElMensajeYElBotonParaVolverAlListado() {
        var regresos = 0
        mostrar(estadoFavorito = FavoriteUiState.NotAvailable, onAtras = { regresos++ })

        rule.onNodeWithTag(TagNotAvailableFavorito).assertIsDisplayed()
        rule.onNodeWithTag(TagBotonFavorito).assertDoesNotExist()
        rule.onNodeWithText("Volver al listado de artistas").performClick()
        assertEquals(1, regresos)
    }

    @Test
    fun noDisponibleUsaUnCallbackDeVolverAlListadoDistintoDeAtras() {
        // Fixes a PR review bug: VinilosNavHost needs to distinguish "go back to the list
        // after artist not available" (which must force a new query) from a regular "back";
        // if both shared the same callback, there would be no way to tell them apart.
        var atras = 0
        var volverAlListado = 0
        mostrar(
            estadoFavorito = FavoriteUiState.NotAvailable,
            onAtras = { atras++ },
            onVolverAlListado = { volverAlListado++ }
        )

        rule.onNodeWithText("Volver al listado de artistas").performClick()

        assertEquals(0, atras)
        assertEquals(1, volverAlListado)
    }

    @Test
    fun favoritoConConfirmacionMuestraElMensajeConElNombre() {
        mostrar(estadoFavorito = FavoriteUiState.Favorito(mostrarConfirmacion = true))

        rule.onNodeWithTag(TagConfirmacionFavorito).assertIsDisplayed()
        rule.onNodeWithText("Rubén Blades se agregó a tus favoritos").assertIsDisplayed()
    }

    @Test
    fun laConfirmacionDesapareceSolaDespuesDeUnosSegundos() {
        rule.setContent {
            VinilosTheme {
                // Without remember, this line would re-run on every recomposition and reset
                // "estado" to mostrarConfirmacion = true, preventing the confirmation from disappearing.
                var estado by remember {
                    mutableStateOf<FavoriteUiState>(FavoriteUiState.Favorito(mostrarConfirmacion = true))
                }
                ArtistDetailScreen(
                    estado = ArtistDetailUiState.Success(rubenBlades),
                    onSeleccionarAlbum = {},
                    onReintentar = {},
                    onAtras = {},
                    estadoFavorito = estado,
                    onConfirmacionFavoritoMostrada = {
                        estado = FavoriteUiState.Favorito(mostrarConfirmacion = false)
                    }
                )
            }
        }

        rule.onNodeWithTag(TagConfirmacionFavorito).assertIsDisplayed()

        // The real screen (HU04) composes more content than the old placeholder screen
        // (albums, awards, image), so on slow emulators the margin over the 2.5s production
        // wait needs to be larger; the production value itself does not change.
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithTag(TagConfirmacionFavorito).fetchSemanticsNodes().isEmpty()
        }

        rule.onNodeWithTag(TagConfirmacionFavorito).assertDoesNotExist()
    }
}
