package com.example.vinilos.ui.detalle

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.ui.theme.VinilosTheme
import org.junit.Rule
import org.junit.Test

class ContenidoAlbumTest {
    @get:Rule
    val rule = createComposeRule()

    private val album = Album(
        100, "Buscando América", null, "El sexto álbum de Rubén Blades",
        "Salsa", "Elektra", "1984-08-01", emptyList(), emptyList(), emptyList()
    )

    @Test
    fun muestraLosArtistasConUnaImagenProvisionalParaCadaUno() {
        val presentacion = presentarAlbum(album.copy(
            artistas = listOf(Artista("Rubén Blades", null), Artista("Seis del Solar", null))
        ))
        rule.setContent {
            VinilosTheme { ContenidoAlbum(presentacion, onAtras = {}) }
        }

        listOf("Rubén Blades", "Seis del Solar").forEach { nombre ->
            rule.onNodeWithText(nombre).performScrollTo().assertIsDisplayed()
            rule.onNodeWithContentDescription("Imagen provisional de $nombre").assertIsDisplayed()
        }
    }

    @Test
    fun muestraUnaIlustracionLocalProvisionalAntesDeLosMetadatos() {
        val presentacion = presentarAlbum(album)
        rule.setContent {
            VinilosTheme { ContenidoAlbum(presentacion, onAtras = {}) }
        }

        val imagen = rule.onNodeWithContentDescription("Imagen provisional del álbum")
            .assertIsDisplayed().fetchSemanticsNode()
        val nombre = rule.onNodeWithText("Buscando América").fetchSemanticsNode()
        org.junit.Assert.assertTrue(imagen.boundsInRoot.bottom <= nombre.boundsInRoot.top)
    }

    @Test
    fun muestraLosMetadatosDelAlbumConsultado() {
        val presentacion = presentarAlbum(album)
        rule.setContent {
            VinilosTheme { ContenidoAlbum(presentacion, onAtras = {}) }
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
    fun sinCancionesMuestraElMensajeExacto() {
        val presentacion = presentarAlbum(album)
        rule.setContent {
            VinilosTheme { ContenidoAlbum(presentacion, onAtras = {}) }
        }

        rule.onNodeWithText("Este álbum aún no tiene canciones registradas")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sinComentariosMuestraElMensajeYLaAusenciaDeCalificaciones() {
        val presentacion = presentarAlbum(album)
        rule.setContent {
            VinilosTheme { ContenidoAlbum(presentacion, onAtras = {}) }
        }

        rule.onNodeWithText("Aún no hay comentarios para este álbum")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Sin calificaciones").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cancionesMuestraCadaNombreYDuracionSinMensajeDeVacio() {
        val presentacion = presentarAlbum(album.copy(
            canciones = listOf(Cancion("Decisiones", "5:02"), Cancion("Plástico", "6:40"))
        ))
        rule.setContent {
            VinilosTheme { ContenidoAlbum(presentacion, onAtras = {}) }
        }

        listOf("Decisiones", "5:02", "Plástico", "6:40").forEach {
            rule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        rule.onNodeWithText("Este álbum aún no tiene canciones registradas").assertDoesNotExist()
    }

    @Test
    fun comentariosMuestraTextoNotaYPromedioSinDatosDeAutor() {
        val presentacion = presentarAlbum(album.copy(
            comentarios = listOf(Comentario("Gran disco", 5), Comentario("Buen sonido", 4))
        ))
        rule.setContent {
            VinilosTheme { ContenidoAlbum(presentacion, onAtras = {}) }
        }

        listOf("Gran disco", "5 / 5", "Buen sonido", "4 / 5", "4,5").forEach {
            rule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        rule.onNodeWithText("Aún no hay comentarios para este álbum").assertDoesNotExist()
        rule.onNodeWithText("Sin calificaciones").assertDoesNotExist()
        rule.onNodeWithText("Autor", substring = true).assertDoesNotExist()
    }
}
