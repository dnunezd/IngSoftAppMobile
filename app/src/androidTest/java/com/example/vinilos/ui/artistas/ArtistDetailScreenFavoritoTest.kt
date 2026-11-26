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

/** Botón de favorito en el detalle real del artista (HU10 sobre HU04), un estado a la vez. */
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
        // Corrige un bug de revisión de PR: VinilosNavHost necesita distinguir "volver al
        // listado tras artista no disponible" (que debe forzar una nueva consulta) de "atrás"
        // normal; si ambos compartieran el mismo callback, no habría forma de diferenciarlos.
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
                // Sin remember, esta línea se re-ejecutaría en cada recomposición y reiniciaría
                // "estado" a mostrarConfirmacion = true, impidiendo que la confirmación desaparezca.
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

        // La pantalla real (HU04) compone más contenido que la antigua pantalla provisional
        // (álbumes, premios, imagen), por lo que en emuladores lentos el margen sobre los 2.5s
        // de espera de producción debe ser mayor; el valor de producción no cambia.
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithTag(TagConfirmacionFavorito).fetchSemanticsNodes().isEmpty()
        }

        rule.onNodeWithTag(TagConfirmacionFavorito).assertDoesNotExist()
    }
}
