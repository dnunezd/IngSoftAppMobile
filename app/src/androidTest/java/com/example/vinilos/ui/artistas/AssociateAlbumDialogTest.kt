package com.example.vinilos.ui.artistas

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AssociateAlbumUiState
import com.example.vinilos.viewmodel.ListadoAsociable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Dialog to add an album to an artist (HU15): one acceptance criterion per test. */
class AssociateAlbumDialogTest {
    @get:Rule
    val rule = createComposeRule()

    private val buscando = AlbumSummary(100, "Buscando América", null, "Salsa", "Rubén Blades", "1984")
    private val noche = AlbumSummary(102, "A Night at the Opera", null, "Rock", "Queen", "1975")
    private val dia = AlbumSummary(103, "A Day at the Races", null, "Rock", "Queen", "1976")

    private val abierta = AssociateAlbumUiState(
        abierta = true,
        asociados = setOf(100),
        listado = ListadoAsociable.Listo(listOf(dia, noche, buscando))
    )

    private val buscados = mutableListOf<String>()
    private val seleccionados = mutableListOf<Int>()
    private var asociaciones = 0
    private var cancelaciones = 0
    private var reintentos = 0

    private fun mostrar(estado: AssociateAlbumUiState) {
        rule.setContent {
            VinilosTheme {
                AssociateAlbumDialog(
                    estado = estado,
                    nombreArtista = "Rubén Blades",
                    acciones = AssociateAlbumActions(
                        onBuscar = { buscados += it },
                        onSeleccionar = { seleccionados += it },
                        onAsociar = { asociaciones++ },
                        onCancelar = { cancelaciones++ },
                        onReintentarCarga = { reintentos++ }
                    )
                )
            }
        }
        rule.waitForIdle()
    }

    /** On small screens the list doesn't show every row: scroll to the one being checked. */
    private fun fila(id: Int) =
        rule.onNodeWithTag(TagListaAsociar).performScrollToNode(hasTestTag(tagAlbumAsociable(id))).let {
            rule.onNodeWithTag(tagAlbumAsociable(id))
        }

    @Test
    fun cerradaNoMuestraLaVentana() {
        mostrar(AssociateAlbumUiState())

        rule.onNodeWithTag(TagVentanaAsociar).assertDoesNotExist()
    }

    // Criterion 1
    @Test
    fun muestraElNombreDelArtistaAlQueSeLeAgregaElAlbum() {
        mostrar(abierta)

        rule.onNodeWithTag(TagTituloAsociar).assertIsDisplayed()
        rule.onNodeWithText("Asociar álbum a Rubén Blades").assertIsDisplayed()
    }

    // Criterion 3
    @Test
    fun muestraLaBarraDeBusquedaYAvisaLoQueSeEscribe() {
        mostrar(abierta)

        rule.onNodeWithTag(TagBusquedaAsociar).assertIsDisplayed().performTextInput("opera")

        assertEquals("opera", buscados.last())
    }

    @Test
    fun cadaAlbumMuestraPortadaNombreYAnio() {
        mostrar(abierta)

        fila(102).assertIsDisplayed()
        rule.onNodeWithText("A Night at the Opera").assertIsDisplayed()
        rule.onNodeWithText("1975").assertIsDisplayed()
        rule.onNodeWithTag(tagPortadaAsociable(102), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun laFilaNoRepiteElNombreConLaDescripcionDeLaPortada() {
        // PR #114 review fix: TalkBack was reading "Imagen de A Night at the Opera" and then "A Night at the Opera".
        mostrar(abierta)

        fila(102).assert(!hasContentDescription("Imagen de A Night at the Opera"))
        fila(102).assert(hasText("A Night at the Opera"))
    }

    // Criterion 4
    @Test
    fun unAlbumYaAsociadoSeVeDeshabilitadoConLaEtiqueta() {
        mostrar(abierta)
        fila(100)

        rule.onNodeWithTag(tagYaAsociado(100), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Ya asociado", useUnmergedTree = true).assertIsDisplayed()
        fila(100).assertIsNotEnabled().performClick()
        assertTrue(seleccionados.isEmpty())
    }

    // Criterion 5
    @Test
    fun tocarUnAlbumDisponibleLoSelecciona() {
        mostrar(abierta)

        fila(102).assertIsEnabled().performClick()

        assertEquals(listOf(102), seleccionados)
    }

    @Test
    fun soloElAlbumElegidoSeVeSeleccionado() {
        mostrar(abierta.copy(seleccionado = 102))

        fila(102).assertIsSelected()
        fila(103).assertIsNotSelected()
    }

    // Criterion 6
    @Test
    fun associateEstaDeshabilitadoSinSeleccion() {
        mostrar(abierta)

        rule.onNodeWithTag(TagBotonAsociar).assertIsDisplayed().assertIsNotEnabled()
        rule.onNodeWithTag(TagBotonAsociar).assert(hasText("Asociar"))
    }

    // Criterion 7
    @Test
    fun conSeleccionAsociarSeHabilitaYAvisa() {
        mostrar(abierta.copy(seleccionado = 102))

        rule.onNodeWithTag(TagBotonAsociar).assertIsEnabled().performClick()

        assertEquals(1, asociaciones)
    }

    @Test
    fun mientrasAsociaElBotonQuedaDeshabilitado() {
        mostrar(abierta.copy(seleccionado = 102, asociando = true))

        rule.onNodeWithTag(TagBotonAsociar).assertIsNotEnabled()
        rule.onNodeWithTag(TagAsociandoAsociar, useUnmergedTree = true).assertIsDisplayed()
    }

    // Criterion 8
    @Test
    fun cancelarCierraSinAsociar() {
        mostrar(abierta.copy(seleccionado = 102))

        rule.onNodeWithTag(TagCancelarAsociar).assertIsDisplayed().performClick()

        assertEquals(1, cancelaciones)
        assertEquals(0, asociaciones)
    }

    @Test
    fun elBotonCerrarTambienCancela() {
        mostrar(abierta)

        rule.onNodeWithTag(TagCerrarAsociar).performClick()

        assertEquals(1, cancelaciones)
    }

    // Criterion 9
    @Test
    fun sinAlbumesDisponiblesMuestraElMensaje() {
        mostrar(abierta.copy(asociados = setOf(100, 102, 103)))

        rule.onNodeWithTag(TagEmptyAsociar).assertIsDisplayed()
        rule.onNodeWithText("No hay álbumes para asociar").assertIsDisplayed()
        rule.onNodeWithTag(TagBotonAsociar).assertIsNotEnabled()
    }

    @Test
    fun conElCatalogoEmptyMuestraElMensaje() {
        mostrar(abierta.copy(asociados = emptySet(), listado = ListadoAsociable.Listo(emptyList())))

        rule.onNodeWithText("No hay álbumes para asociar").assertIsDisplayed()
    }

    @Test
    fun unaBusquedaSinCoincidenciasMuestraSuPropioMensaje() {
        mostrar(abierta.copy(busqueda = "zzz"))

        rule.onNodeWithTag(TagSinResultadosAsociar).assertIsDisplayed()
        rule.onNodeWithText("No encontramos álbumes con esa búsqueda").assertIsDisplayed()
        rule.onNodeWithTag(TagEmptyAsociar).assertDoesNotExist()
    }

    // Criterion 10
    @Test
    fun siFallaAlAsociarMuestraElErrorYConservaLaSeleccion() {
        mostrar(abierta.copy(seleccionado = 102, errorAlAsociar = true))

        rule.onNodeWithTag(TagErrorAsociar).assertIsDisplayed()
        rule.onNodeWithText("No se pudo asociar el álbum, intenta de nuevo").assertIsDisplayed()
        fila(102).assertIsSelected()
        rule.onNodeWithTag(TagBotonAsociar).assertIsEnabled()
    }

    @Test
    fun sinErrorNoMuestraElMensajeDeError() {
        mostrar(abierta.copy(seleccionado = 102))

        rule.onNodeWithTag(TagErrorAsociar).assertDoesNotExist()
    }

    // Listing load and error (DoD 5)
    @Test
    fun mientrasCargaElListadoMuestraElIndicador() {
        mostrar(abierta.copy(listado = ListadoAsociable.Loading))

        rule.onNodeWithTag(TagLoadingAsociar).assertIsDisplayed()
        rule.onNodeWithText("Cargando álbumes...").assertIsDisplayed()
    }

    @Test
    fun siNoSePuedeCargarElListadoOfreceReintentar() {
        mostrar(abierta.copy(listado = ListadoAsociable.Error))

        rule.onNodeWithTag(TagErrorListadoAsociar).assertIsDisplayed()
        rule.onNodeWithText("No pudimos cargar los álbumes").assertIsDisplayed()
        rule.onNodeWithText("Reintentar").performClick()

        assertEquals(1, reintentos)
    }
}
