package com.example.vinilos.ui.catalogo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AlbumsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PantallaCatalogoTest {

    @get:Rule
    val rule = createComposeRule()

    private val races = AlbumResumen(103, "A Day at the Races", null, "Rock", "Queen", "1976")
    private val opera = AlbumResumen(102, "A Night at the Opera", "http://localhost:1/no-existe.jpg", "Rock", "Queen", "1975")
    private val buscando = AlbumResumen(100, "Buscando América", " ", "Salsa", "Rubén Blades Bellido de Luna", "1984")
    private val poeta = AlbumResumen(101, "Poeta del pueblo", null, "Salsa", "Rubén Blades Bellido de Luna", "1984")
    private val ejemplo = listOf(races, opera, buscando, poeta)

    private var estado by mutableStateOf<AlbumsUiState>(AlbumsUiState.Cargando)
    private var actualizaciones = 0
    private var reintentos = 0
    private var avisosMostrados = 0
    private var seleccionado: Int? = null
    private var atras = 0
    private var enPantalla by mutableStateOf(true)

    private fun mostrar(inicial: AlbumsUiState, session: Session = Session.Visitor) {
        estado = inicial
        rule.setContent {
            VinilosTheme {
                if (enPantalla) {
                    PantallaCatalogo(
                        estado = estado,
                        session = session,
                        onActualizar = { actualizaciones++ },
                        onReintentar = { reintentos++ },
                        onAvisoMostrado = { avisosMostrados++ },
                        onSeleccionar = { seleccionado = it },
                        onAtras = { atras++ }
                    )
                }
            }
        }
    }

    private fun verAlbum(id: Int) =
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(id)))

    @Test
    fun mientrasCargaMuestraElIndicadorDeProgreso() {
        mostrar(AlbumsUiState.Cargando)

        rule.onNodeWithTag(TagProgresoCatalogo).assertIsDisplayed()
        rule.onNodeWithText("Cargando catálogo de vinilos...").assertIsDisplayed()
    }

    @Test
    fun cadaAlbumMuestraSuPortadaNombreYGenero() {
        mostrar(AlbumsUiState.Exito(ejemplo))

        ejemplo.forEach { album ->
            // La tarjeta es clickable: su nodo fusiona la portada, el nombre y el género.
            val tarjeta = hasTestTag(tagAlbum(album.id)) and
                hasContentDescription("Portada de ${album.nombre}") and
                hasText(album.nombre) and
                hasText("${album.genero} · ${album.anio}")
            rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(tarjeta)
            rule.onNode(tarjeta).assertIsDisplayed()
        }
    }

    @Test
    fun losAlbumesSeMuestranEnElOrdenRecibido() {
        mostrar(AlbumsUiState.Exito(ejemplo))

        val etiquetas = rule.tagsDeAlbumesEnPantalla()

        assertEquals(ejemplo.map { tagAlbum(it.id) }.take(etiquetas.size), etiquetas)
    }

    @Test
    fun conArtistaYAnioLosMuestra() {
        mostrar(AlbumsUiState.Exito(listOf(buscando)))

        rule.onNodeWithText("Rubén Blades Bellido de Luna").assertIsDisplayed()
        rule.onNodeWithText("Salsa · 1984").assertIsDisplayed()
    }

    @Test
    fun sinArtistaNiAnioNoMuestraHuecosNiNull() {
        val sinDatos = AlbumResumen(5, "Álbum sin datos", null, "Folk", "", null)
        mostrar(AlbumsUiState.Exito(listOf(sinDatos)))

        rule.onNodeWithText("Álbum sin datos").assertIsDisplayed()
        rule.onNodeWithText("Folk").assertIsDisplayed()
        rule.onAllNodesWithText("null", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText(" · ", substring = true).assertCountEquals(0)
    }

    @Test
    fun elContadorIndicaCuantosAlbumesHay() {
        mostrar(AlbumsUiState.Exito(ejemplo))
        rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals("4 lanzamientos")

        estado = AlbumsUiState.Exito(listOf(races))
        rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals("1 lanzamiento")
    }

    @Test
    fun unaPortadaVaciaOInvalidaMuestraLaImagenGenericaSinRomperLaLista() {
        mostrar(AlbumsUiState.Exito(ejemplo))

        listOf(opera, buscando, poeta).forEach { album ->
            verAlbum(album.id)
            rule.onNodeWithContentDescription("Portada de ${album.nombre}").assertIsDisplayed()
            rule.onNodeWithText(album.nombre).assertIsDisplayed()
        }
    }

    @Test
    fun sinAlbumesMuestraElMensajeDeCatalogoVacio() {
        mostrar(AlbumsUiState.Vacio())

        rule.onNodeWithTag(TagVacioCatalogo).assertTextEquals("Aún no hay álbumes en el catálogo")
    }

    @Test
    fun enErrorMuestraElMensajeYReintentarLlamaAlCallback() {
        mostrar(AlbumsUiState.Error)

        rule.onNodeWithTag(TagErrorCatalogo).assertTextEquals("No pudimos cargar el catálogo")
        rule.onNodeWithText("Reintentar").assertIsDisplayed()
        rule.onNodeWithTag(TagReintentarCatalogo).performClick()

        assertEquals(1, reintentos)
    }

    @Test
    fun mientrasActualizaMuestraElIndicadorSinQuitarLaLista() {
        mostrar(AlbumsUiState.Exito(ejemplo, actualizando = true))

        rule.onNodeWithTag(TagActualizandoCatalogo).assertIsDisplayed()
        rule.onNodeWithText(races.nombre).assertIsDisplayed()
    }

    @Test
    fun siFallaLaActualizacionAvisaYConservaLaLista() {
        mostrar(AlbumsUiState.Exito(ejemplo, errorAlActualizar = true))

        rule.onNodeWithText("No pudimos actualizar el catálogo").assertIsDisplayed()
        rule.onNodeWithText(races.nombre).assertIsDisplayed()
        rule.waitUntil(timeoutMillis = 10_000) { avisosMostrados == 1 }
    }

    @Test
    fun siLaPantallaSaleMientrasSeVeElAvisoQuedaComoMostrado() {
        // Al tocar un álbum o girar, el catálogo sale de la composición antes de que el aviso
        // termine; si no quedara marcado, volvería a aparecer al regresar.
        mostrar(AlbumsUiState.Exito(ejemplo, errorAlActualizar = true))
        rule.onNodeWithText("No pudimos actualizar el catálogo").assertIsDisplayed()
        assertEquals(0, avisosMostrados)

        enPantalla = false
        rule.waitForIdle()

        assertEquals(1, avisosMostrados)
    }

    @Test
    fun laEtiquetaDeRolIndicaVisitante() {
        mostrar(AlbumsUiState.Exito(ejemplo), Session.Visitor)

        rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
    }

    @Test
    fun laEtiquetaDeRolIndicaColeccionista() {
        mostrar(AlbumsUiState.Exito(ejemplo), Session.Collector(Collector(101, "Jaime Monsalve", "j@m.co")))

        rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")
    }

    @Test
    fun tocarUnAlbumEntregaSuId() {
        mostrar(AlbumsUiState.Exito(ejemplo))

        verAlbum(buscando.id)
        rule.onNodeWithTag(tagAlbum(buscando.id)).performClick()

        assertEquals(buscando.id, seleccionado)
    }

    @Test
    fun laFlechaDeAtrasLlamaAlCallback() {
        mostrar(AlbumsUiState.Exito(ejemplo))

        rule.onNodeWithTag(TagAtras).performClick()

        assertEquals(1, atras)
    }
}
