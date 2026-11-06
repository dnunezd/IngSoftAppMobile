package com.example.vinilos.ui.artistas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.FavoritoUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Botón de favorito del detalle provisional del artista (HU10), un estado a la vez. */
class PantallaArtistaProvisionalFavoritoTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun visitanteNoVeNingunBotonDeFavorito() {
        rule.setContent {
            VinilosTheme {
                PantallaArtistaProvisional(
                    nombre = "Rubén Blades",
                    tipo = ArtistType.MUSICO,
                    onAtras = {},
                    estadoFavorito = null
                )
            }
        }

        rule.onNodeWithTag(TagBotonFavorito).assertDoesNotExist()
    }

    @Test
    fun comprobandoMuestraElBotonDesactivado() {
        rule.setContent {
            VinilosTheme {
                PantallaArtistaProvisional(
                    nombre = "Rubén Blades",
                    tipo = ArtistType.MUSICO,
                    onAtras = {},
                    estadoFavorito = FavoritoUiState.Comprobando
                )
            }
        }

        rule.onNodeWithTag(TagBotonFavorito).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun noFavoritoMuestraElBotonActivoParaAgregar() {
        var toques = 0
        rule.setContent {
            VinilosTheme {
                PantallaArtistaProvisional(
                    nombre = "Rubén Blades",
                    tipo = ArtistType.MUSICO,
                    onAtras = {},
                    estadoFavorito = FavoritoUiState.NoFavorito,
                    onAgregarFavorito = { toques++ }
                )
            }
        }

        rule.onNodeWithText("Agregar Favorito").assertIsDisplayed().assertIsEnabled().performClick()
        assertEquals(1, toques)
    }

    @Test
    fun guardandoMuestraElBotonDesactivadoConSuTexto() {
        rule.setContent {
            VinilosTheme {
                PantallaArtistaProvisional(
                    nombre = "Rubén Blades",
                    tipo = ArtistType.MUSICO,
                    onAtras = {},
                    estadoFavorito = FavoritoUiState.Guardando
                )
            }
        }

        rule.onNodeWithText("Guardando...").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun favoritoMuestraElBotonDesactivadoConSuTexto() {
        rule.setContent {
            VinilosTheme {
                PantallaArtistaProvisional(
                    nombre = "Rubén Blades",
                    tipo = ArtistType.MUSICO,
                    onAtras = {},
                    estadoFavorito = FavoritoUiState.Favorito(mostrarConfirmacion = false)
                )
            }
        }

        rule.onNodeWithText("Favorito").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun errorComprobacionMuestraElBotonDesactivadoYPermiteReintentar() {
        var reintentos = 0
        rule.setContent {
            VinilosTheme {
                PantallaArtistaProvisional(
                    nombre = "Rubén Blades",
                    tipo = ArtistType.MUSICO,
                    onAtras = {},
                    estadoFavorito = FavoritoUiState.ErrorComprobacion,
                    onReintentarFavorito = { reintentos++ }
                )
            }
        }

        rule.onNodeWithTag(TagBotonFavorito).assertIsNotEnabled()
        rule.onNodeWithTag(TagErrorFavorito).assertIsDisplayed()
        rule.onNodeWithTag(TagReintentarFavorito).performClick()
        assertEquals(1, reintentos)
    }

    @Test
    fun errorGuardadoMuestraElMensajeYElBotonDisponibleParaReintentar() {
        var toques = 0
        rule.setContent {
            VinilosTheme {
                PantallaArtistaProvisional(
                    nombre = "Rubén Blades",
                    tipo = ArtistType.MUSICO,
                    onAtras = {},
                    estadoFavorito = FavoritoUiState.ErrorGuardado,
                    onAgregarFavorito = { toques++ }
                )
            }
        }

        rule.onNodeWithTag(TagErrorFavorito).assertIsDisplayed()
        rule.onNodeWithText("Agregar Favorito").assertIsEnabled().performClick()
        assertEquals(1, toques)
    }

    @Test
    fun noDisponibleMuestraElMensajeYElBotonParaVolverAlListado() {
        var regresos = 0
        rule.setContent {
            VinilosTheme {
                PantallaArtistaProvisional(
                    nombre = "Rubén Blades",
                    tipo = ArtistType.MUSICO,
                    onAtras = { regresos++ },
                    estadoFavorito = FavoritoUiState.NoDisponible
                )
            }
        }

        rule.onNodeWithTag(TagNoDisponibleFavorito).assertIsDisplayed()
        rule.onNodeWithTag(TagBotonFavorito).assertDoesNotExist()
        rule.onNodeWithText("Volver al listado de artistas").performClick()
        assertEquals(1, regresos)
    }

    @Test
    fun favoritoConConfirmacionMuestraElMensajeConElNombre() {
        rule.setContent {
            VinilosTheme {
                PantallaArtistaProvisional(
                    nombre = "Rubén Blades",
                    tipo = ArtistType.MUSICO,
                    onAtras = {},
                    estadoFavorito = FavoritoUiState.Favorito(mostrarConfirmacion = true)
                )
            }
        }

        rule.onNodeWithTag(TagConfirmacionFavorito).assertIsDisplayed()
        rule.onNodeWithText("Rubén Blades se agregó a tus favoritos").assertIsDisplayed()
    }
}
