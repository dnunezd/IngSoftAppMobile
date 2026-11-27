package com.example.vinilos.ui.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Performer
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.Comment
import com.example.vinilos.ui.theme.VinilosTheme
import org.junit.Rule
import org.junit.Test

class AlbumContentTest {
    @get:Rule
    val rule = createComposeRule()

    private val album = Album(
        100, "Buscando América", null, "El sexto álbum de Rubén Blades",
        "Salsa", "Elektra", "1984-08-01", emptyList(), emptyList(), emptyList()
    )

    @Test
    fun laFlechaDeRegresoNotificaAlContenedor() {
        var regresos = 0
        val presentacion = presentarAlbum(album)
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = { regresos++ }) }
        }

        rule.onNodeWithContentDescription("Atrás").performClick()

        org.junit.Assert.assertEquals(1, regresos)
    }

    @Test
    fun muestraLosArtistasConUnaImagenProvisionalParaCadaUno() {
        val presentacion = presentarAlbum(album.copy(
            artistas = listOf(Performer("Rubén Blades", null), Performer("Seis del Solar", null))
        ))
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = {}) }
        }

        listOf("Rubén Blades", "Seis del Solar").forEach { nombre ->
            rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(nombre))
            rule.onNodeWithText(nombre).assertIsDisplayed()
            rule.onNodeWithContentDescription("Imagen de $nombre").assertIsDisplayed()
        }
    }

    @Test
    fun muestraLaPortadaAntesDeLosMetadatos() {
        val presentacion = presentarAlbum(album)
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = {}) }
        }

        val imagen = rule.onNodeWithContentDescription("Portada del álbum, Buscando América")
            .assertIsDisplayed().fetchSemanticsNode()
        val nombre = rule.onNodeWithText("Buscando América").fetchSemanticsNode()
        org.junit.Assert.assertTrue(imagen.boundsInRoot.bottom <= nombre.boundsInRoot.top)
    }

    @Test
    fun conUrlMuestraUnaUnicaPortadaSinRomperLaPantalla() {
        val presentacion = presentarAlbum(album.copy(portada = "https://example.com/portada.jpg"))
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = {}) }
        }

        rule.onAllNodesWithContentDescription("Portada del álbum, Buscando América").assertCountEquals(1)
        rule.onNodeWithContentDescription("Portada del álbum, Buscando América").assertIsDisplayed()
    }

    @Test
    fun muestraLosMetadatosDelAlbumConsultado() {
        val presentacion = presentarAlbum(album)
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = {}) }
        }

        listOf(
            "Buscando América", "El sexto álbum de Rubén Blades",
            "Salsa", "Elektra", "1 de agosto de 1984"
        ).forEach { texto ->
            rule.onNodeWithText(texto).performScrollTo().assertIsDisplayed()
        }
        rule.onNodeWithText("Escuchar ahora").assertDoesNotExist()
    }

    @Test
    fun sinTrackesMuestraElMensajeExacto() {
        val presentacion = presentarAlbum(album)
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = {}) }
        }

        // performScrollToNode instead of performScrollTo: on smaller screens (e.g. API 23)
        // the message may fall outside the LazyColumn's already-composed nodes.
        rule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Este álbum aún no tiene canciones registradas"))
        rule.onNodeWithText("Este álbum aún no tiene canciones registradas").assertIsDisplayed()
    }

    @Test
    fun sinCommentsMuestraElMensajeYLaAusenciaDeCalificaciones() {
        val presentacion = presentarAlbum(album)
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = {}) }
        }

        val lista = rule.onNode(hasScrollToNodeAction())
        lista.performScrollToNode(hasText("Aún no hay comentarios para este álbum"))
        rule.onNodeWithText("Aún no hay comentarios para este álbum").assertIsDisplayed()
        lista.performScrollToNode(hasText("Sin calificaciones"))
        rule.onNodeWithText("Sin calificaciones").assertIsDisplayed()
    }

    @Test
    fun conCalificacionesMuestraLaFechaDeEstrenoYLasEstrellasDelPromedio() {
        // Average of (3+5)/2 = 4.0 -> 4 stars, different from each individual comment's
        // rating (3 and 5), so the header text isn't confused with a comment's.
        val presentacion = presentarAlbum(album.copy(
            comentarios = listOf(Comment("Gran disco", 3), Comment("Buen sonido", 5))
        ))
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = {}) }
        }

        rule.onNodeWithText("Estreno").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("1 de agosto de 1984").performScrollTo().assertIsDisplayed()
        // The average exposes its number as text, and the stars are described without repeating it.
        rule.onNode(hasText("4,0") and hasContentDescription("de 5 estrellas", substring = true))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calificacionFueraDeRangoNoCierraLaAppYMuestraLasEstrellasLimitadas() {
        // The backend validates 0..5, but a corrupted or out-of-range rating must not
        // throw an exception when drawing the stars: it is capped at 5 filled stars max.
        val presentacion = presentarAlbum(album.copy(
            comentarios = listOf(Comment("Comentario con dato corrupto", 6))
        ))
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = {}) }
        }

        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription("6 / 5"))
        rule.onNodeWithContentDescription("6 / 5").assertIsDisplayed()
    }

    @Test
    fun cancionesMuestraCadaNombreYDuracionSinMensajeDeEmpty() {
        val presentacion = presentarAlbum(album.copy(
            canciones = listOf(Track("Decisiones", "5:02"), Track("Plástico", "6:40"))
        ))
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = {}) }
        }

        val lista = rule.onNode(hasScrollToNodeAction())
        listOf("Decisiones", "5:02", "Plástico", "6:40").forEach {
            lista.performScrollToNode(hasText(it))
            rule.onNodeWithText(it).assertIsDisplayed()
        }
        rule.onNodeWithText("Este álbum aún no tiene canciones registradas").assertDoesNotExist()
    }

    @Test
    fun comentariosMuestraTextoNotaYPromedioSinDatosDeAutor() {
        val presentacion = presentarAlbum(album.copy(
            comentarios = listOf(Comment("Gran disco", 5), Comment("Buen sonido", 4))
        ))
        rule.setContent {
            VinilosTheme { AlbumContent(presentacion, onAtras = {}) }
        }

        // performScrollToNode is used instead of onNodeWithText(...).performScrollTo() because the
        // latter requires the node to already be composed; in a long list on a real device,
        // the final items (like the second comment) may not have been composed yet.
        val lista = rule.onNode(hasScrollToNodeAction())
        lista.performScrollToNode(hasText("Gran disco"))
        rule.onNodeWithText("Gran disco").assertIsDisplayed()
        lista.performScrollToNode(hasContentDescription("5 / 5"))
        rule.onNodeWithContentDescription("5 / 5").assertIsDisplayed()
        lista.performScrollToNode(hasText("Buen sonido"))
        rule.onNodeWithText("Buen sonido").assertIsDisplayed()
        lista.performScrollToNode(hasContentDescription("4 / 5"))
        rule.onNodeWithContentDescription("4 / 5").assertIsDisplayed()
        lista.performScrollToNode(hasText("4,5"))
        rule.onNode(hasText("4,5") and hasContentDescription("de 5 estrellas", substring = true)).assertIsDisplayed()
        rule.onNodeWithText("Aún no hay comentarios para este álbum").assertDoesNotExist()
        rule.onNodeWithText("Sin calificaciones").assertDoesNotExist()
        rule.onNodeWithText("Autor", substring = true).assertDoesNotExist()
    }
}
