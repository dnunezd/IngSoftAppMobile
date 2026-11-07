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
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistaDetalleUiState
import com.example.vinilos.viewmodel.FavoritoUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Botón de favorito en el detalle real del artista (HU10 sobre HU04), un estado a la vez. */
class PantallaDetalleArtistaFavoritoTest {
    @get:Rule
    val rule = createComposeRule()

    private val rubenBlades = ArtistaDetalle(
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
        estadoFavorito: FavoritoUiState?,
        onAtras: () -> Unit = {},
        onAgregarFavorito: () -> Unit = {},
        onReintentarFavorito: () -> Unit = {},
        onConfirmacionFavoritoMostrada: () -> Unit = {}
    ) {
        rule.setContent {
            VinilosTheme {
                PantallaDetalleArtista(
                    estado = ArtistaDetalleUiState.Exito(rubenBlades),
                    onSeleccionarAlbum = {},
                    onReintentar = {},
                    onAtras = onAtras,
                    estadoFavorito = estadoFavorito,
                    onAgregarFavorito = onAgregarFavorito,
                    onReintentarFavorito = onReintentarFavorito,
                    onConfirmacionFavoritoMostrada = onConfirmacionFavoritoMostrada
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
        mostrar(estadoFavorito = FavoritoUiState.Comprobando)

        rule.onNodeWithTag(TagBotonFavorito).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun noFavoritoMuestraElBotonActivoParaAgregar() {
        var toques = 0
        mostrar(estadoFavorito = FavoritoUiState.NoFavorito, onAgregarFavorito = { toques++ })

        rule.onNodeWithText("Agregar Favorito").assertIsDisplayed().assertIsEnabled().performClick()
        assertEquals(1, toques)
    }

    @Test
    fun guardandoMuestraElBotonDesactivadoConSuTexto() {
        mostrar(estadoFavorito = FavoritoUiState.Guardando)

        rule.onNodeWithText("Guardando...").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun favoritoMuestraElBotonDesactivadoConSuTexto() {
        mostrar(estadoFavorito = FavoritoUiState.Favorito(mostrarConfirmacion = false))

        rule.onNodeWithText("Favorito").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun errorComprobacionMuestraElBotonDesactivadoYPermiteReintentar() {
        var reintentos = 0
        mostrar(
            estadoFavorito = FavoritoUiState.ErrorComprobacion,
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
        mostrar(estadoFavorito = FavoritoUiState.ErrorGuardado, onAgregarFavorito = { toques++ })

        rule.onNodeWithTag(TagErrorFavorito).assertIsDisplayed()
        rule.onNodeWithText("Agregar Favorito").assertIsEnabled().performClick()
        assertEquals(1, toques)
    }

    @Test
    fun errorGuardadoNoMuestraNingunaConfirmacion() {
        mostrar(estadoFavorito = FavoritoUiState.ErrorGuardado)

        rule.onNodeWithTag(TagConfirmacionFavorito).assertDoesNotExist()
        rule.onNodeWithText("Rubén Blades se agregó a tus favoritos").assertDoesNotExist()
    }

    @Test
    fun noDisponibleMuestraElMensajeYElBotonParaVolverAlListado() {
        var regresos = 0
        mostrar(estadoFavorito = FavoritoUiState.NoDisponible, onAtras = { regresos++ })

        rule.onNodeWithTag(TagNoDisponibleFavorito).assertIsDisplayed()
        rule.onNodeWithTag(TagBotonFavorito).assertDoesNotExist()
        rule.onNodeWithText("Volver al listado de artistas").performClick()
        assertEquals(1, regresos)
    }

    @Test
    fun favoritoConConfirmacionMuestraElMensajeConElNombre() {
        mostrar(estadoFavorito = FavoritoUiState.Favorito(mostrarConfirmacion = true))

        rule.onNodeWithTag(TagConfirmacionFavorito).assertIsDisplayed()
        rule.onNodeWithText("Rubén Blades se agregó a tus favoritos").assertIsDisplayed()
    }

    @Test
    fun laConfirmacionDesapareceSolaDespuesDeUnosSegundos() {
        rule.setContent {
            VinilosTheme {
                // Sin remember, esta línea se re-ejecutaría en cada recomposición y reiniciaría
                // "estado" a mostrarConfirmacion = true, impidiendo que la confirmación desaparezca.
                var estado by remember {
                    mutableStateOf<FavoritoUiState>(FavoritoUiState.Favorito(mostrarConfirmacion = true))
                }
                PantallaDetalleArtista(
                    estado = ArtistaDetalleUiState.Exito(rubenBlades),
                    onSeleccionarAlbum = {},
                    onReintentar = {},
                    onAtras = {},
                    estadoFavorito = estado,
                    onConfirmacionFavoritoMostrada = {
                        estado = FavoritoUiState.Favorito(mostrarConfirmacion = false)
                    }
                )
            }
        }

        rule.onNodeWithTag(TagConfirmacionFavorito).assertIsDisplayed()

        // La pantalla real (HU04) compone más contenido que la antigua pantalla provisional
        // (álbumes, premios, imagen), por lo que en emuladores lentos el margen sobre los 2.5s
        // de espera de producción debe ser mayor; el valor de producción no cambia.
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithTag(TagConfirmacionFavorito).fetchSemanticsNodes().isEmpty()
        }

        rule.onNodeWithTag(TagConfirmacionFavorito).assertDoesNotExist()
    }
}
