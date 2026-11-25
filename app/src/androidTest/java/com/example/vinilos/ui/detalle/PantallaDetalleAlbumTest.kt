package com.example.vinilos.ui.detalle

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.vinilos.data.model.Album
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AlbumUiState
import org.junit.Rule
import org.junit.Test

class PantallaDetalleAlbumTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun exitoMuestraElDetalleSinProgresoNiError() {
        val album = Album(
            100, "Buscando América", null, "Salsa", "Salsa", "Elektra",
            "1984-08-01", emptyList(), emptyList(), emptyList()
        )
        rule.setContent {
            VinilosTheme {
                PantallaDetalleAlbum(AlbumUiState.Exito(album), onAtras = {}, onReintentar = {})
            }
        }

        rule.onNodeWithText("Buscando América").assertIsDisplayed()
        rule.onNodeWithContentDescription("Cargando álbum").assertDoesNotExist()
        rule.onNodeWithText("No pudimos cargar el álbum").assertDoesNotExist()
        rule.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test
    fun duranteLaConsultaMuestraProgresoSinErrorNiReintento() {
        rule.setContent {
            VinilosTheme {
                PantallaDetalleAlbum(AlbumUiState.Cargando, onAtras = {}, onReintentar = {})
            }
        }

        rule.onNodeWithContentDescription("Cargando álbum").assertIsDisplayed()
        rule.onNodeWithText("No pudimos cargar el álbum").assertDoesNotExist()
        rule.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test
    fun albumNoDisponiblePermiteVolverSinOfrecerReintento() {
        var regresos = 0
        rule.setContent {
            VinilosTheme {
                PantallaDetalleAlbum(AlbumUiState.NoDisponible, onAtras = { regresos++ }, onReintentar = {})
            }
        }

        rule.onNodeWithText("El álbum ya no está disponible").assertIsDisplayed()
        rule.onNodeWithText("Reintentar").assertDoesNotExist()
        rule.onNodeWithContentDescription("Cargando álbum").assertDoesNotExist()
        rule.onNodeWithText("Volver al catálogo").performClick()
        org.junit.Assert.assertEquals(1, regresos)
    }

    @Test
    fun errorDeCargaMuestraElMensajeYNotificaElReintento() {
        var reintentos = 0
        rule.setContent {
            VinilosTheme {
                PantallaDetalleAlbum(AlbumUiState.Error, onAtras = {}, onReintentar = { reintentos++ })
            }
        }

        rule.onNodeWithText("No pudimos cargar el álbum").assertIsDisplayed()
        rule.onNodeWithText("El álbum ya no está disponible").assertDoesNotExist()
        rule.onNodeWithContentDescription("Cargando álbum").assertDoesNotExist()
        rule.onNodeWithText("Reintentar").performClick()
        org.junit.Assert.assertEquals(1, reintentos)
    }

    // ---------- Detalle guardado (add-cache-consultas, D6) ----------

    private val guardado = Album(
        100, "Buscando América", null, "Salsa", "Salsa", "Elektra",
        "1984-08-01", emptyList(), emptyList(), emptyList()
    )

    @Test
    fun conElDetalleGuardadoActualizandoseMuestraElIndicadorPequeno() {
        rule.setContent {
            VinilosTheme {
                PantallaDetalleAlbum(AlbumUiState.Exito(guardado, actualizando = true), onAtras = {}, onReintentar = {})
            }
        }

        rule.onNodeWithText("Buscando América").assertIsDisplayed()
        rule.onNodeWithTag(TagActualizandoDetalle).assertIsDisplayed()
        rule.onNodeWithContentDescription("Actualizando álbum").assertIsDisplayed()
        rule.onNodeWithContentDescription("Cargando álbum").assertDoesNotExist()
    }

    @Test
    fun siNoSePudoActualizarAvisaUnaVezSinQuitarElDetalle() {
        var avisos = 0
        rule.setContent {
            VinilosTheme {
                PantallaDetalleAlbum(
                    AlbumUiState.Exito(guardado, errorAlActualizar = true),
                    onAtras = {},
                    onReintentar = {},
                    onAvisoMostrado = { avisos++ }
                )
            }
        }

        rule.onNodeWithText("No pudimos actualizar el álbum").assertIsDisplayed()
        rule.onNodeWithText("Buscando América").assertIsDisplayed()
        rule.onNodeWithText("No pudimos cargar el álbum").assertDoesNotExist()
        rule.onNodeWithTag(TagActualizandoDetalle).assertDoesNotExist()
        rule.waitUntil(10_000) { avisos == 1 }
    }

    // ---------- Aviso "Canción agregada" (revisión de PR) ----------

    @Test
    fun elAvisoDeCancionAgregadaSeMarcaComoMostradoAunSiSeSaleAntesDeQueTermine() {
        var marcados = 0
        val mostrarPantalla = mutableStateOf(true)
        rule.setContent {
            VinilosTheme {
                if (mostrarPantalla.value) {
                    PantallaDetalleAlbum(
                        AlbumUiState.Exito(guardado),
                        onAtras = {},
                        onReintentar = {},
                        mostrarAvisoCancionAgregada = true,
                        onAvisoCancionAgregadaMostrado = { marcados++ }
                    )
                }
            }
        }

        rule.onNodeWithText("Canción agregada").assertIsDisplayed()
        // Simula salir del detalle (p. ej. abrir "Agregar canción" y cancelar) antes de que
        // el aviso termine de mostrarse: su LaunchedEffect se cancela a mitad de camino.
        mostrarPantalla.value = false
        rule.waitUntil(10_000) { marcados == 1 }
    }
}
