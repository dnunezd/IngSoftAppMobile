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
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AsociarAlbumUiState
import com.example.vinilos.viewmodel.ListadoAsociable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Ventana para agregar un álbum a un artista (HU15): un criterio de aceptación por prueba. */
class VentanaAsociarAlbumTest {
    @get:Rule
    val rule = createComposeRule()

    private val buscando = AlbumResumen(100, "Buscando América", null, "Salsa", "Rubén Blades", "1984")
    private val noche = AlbumResumen(102, "A Night at the Opera", null, "Rock", "Queen", "1975")
    private val dia = AlbumResumen(103, "A Day at the Races", null, "Rock", "Queen", "1976")

    private val abierta = AsociarAlbumUiState(
        abierta = true,
        asociados = setOf(100),
        listado = ListadoAsociable.Listo(listOf(dia, noche, buscando))
    )

    private val buscados = mutableListOf<String>()
    private val seleccionados = mutableListOf<Int>()
    private var asociaciones = 0
    private var cancelaciones = 0
    private var reintentos = 0

    private fun mostrar(estado: AsociarAlbumUiState) {
        rule.setContent {
            VinilosTheme {
                VentanaAsociarAlbum(
                    estado = estado,
                    nombreArtista = "Rubén Blades",
                    acciones = AccionesAsociarAlbum(
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

    /** En pantallas pequeñas la lista no muestra todas las filas: se desplaza hasta la que se revisa. */
    private fun fila(id: Int) =
        rule.onNodeWithTag(TagListaAsociar).performScrollToNode(hasTestTag(tagAlbumAsociable(id))).let {
            rule.onNodeWithTag(tagAlbumAsociable(id))
        }

    @Test
    fun cerradaNoMuestraLaVentana() {
        mostrar(AsociarAlbumUiState())

        rule.onNodeWithTag(TagVentanaAsociar).assertDoesNotExist()
    }

    // Criterio 1
    @Test
    fun muestraElNombreDelArtistaAlQueSeLeAgregaElAlbum() {
        mostrar(abierta)

        rule.onNodeWithTag(TagTituloAsociar).assertIsDisplayed()
        rule.onNodeWithText("Asociar álbum a Rubén Blades").assertIsDisplayed()
    }

    // Criterio 3
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
        // Revisión del PR #114: TalkBack leía "Imagen de A Night at the Opera" y luego "A Night at the Opera".
        mostrar(abierta)

        fila(102).assert(!hasContentDescription("Imagen de A Night at the Opera"))
        fila(102).assert(hasText("A Night at the Opera"))
    }

    // Criterio 4
    @Test
    fun unAlbumYaAsociadoSeVeDeshabilitadoConLaEtiqueta() {
        mostrar(abierta)
        fila(100)

        rule.onNodeWithTag(tagYaAsociado(100), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Ya asociado", useUnmergedTree = true).assertIsDisplayed()
        fila(100).assertIsNotEnabled().performClick()
        assertTrue(seleccionados.isEmpty())
    }

    // Criterio 5
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

    // Criterio 6
    @Test
    fun asociarEstaDeshabilitadoSinSeleccion() {
        mostrar(abierta)

        rule.onNodeWithTag(TagBotonAsociar).assertIsDisplayed().assertIsNotEnabled()
        rule.onNodeWithTag(TagBotonAsociar).assert(hasText("Asociar"))
    }

    // Criterio 7
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

    // Criterio 8
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

    // Criterio 9
    @Test
    fun sinAlbumesDisponiblesMuestraElMensaje() {
        mostrar(abierta.copy(asociados = setOf(100, 102, 103)))

        rule.onNodeWithTag(TagVacioAsociar).assertIsDisplayed()
        rule.onNodeWithText("No hay álbumes para asociar").assertIsDisplayed()
        rule.onNodeWithTag(TagBotonAsociar).assertIsNotEnabled()
    }

    @Test
    fun conElCatalogoVacioMuestraElMensaje() {
        mostrar(abierta.copy(asociados = emptySet(), listado = ListadoAsociable.Listo(emptyList())))

        rule.onNodeWithText("No hay álbumes para asociar").assertIsDisplayed()
    }

    @Test
    fun unaBusquedaSinCoincidenciasMuestraSuPropioMensaje() {
        mostrar(abierta.copy(busqueda = "zzz"))

        rule.onNodeWithTag(TagSinResultadosAsociar).assertIsDisplayed()
        rule.onNodeWithText("No encontramos álbumes con esa búsqueda").assertIsDisplayed()
        rule.onNodeWithTag(TagVacioAsociar).assertDoesNotExist()
    }

    // Criterio 10
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

    // Carga y error del listado (DoD 5)
    @Test
    fun mientrasCargaElListadoMuestraElIndicador() {
        mostrar(abierta.copy(listado = ListadoAsociable.Cargando))

        rule.onNodeWithTag(TagCargandoAsociar).assertIsDisplayed()
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
