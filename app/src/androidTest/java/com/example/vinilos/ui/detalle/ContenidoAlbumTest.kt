package com.example.vinilos.ui.detalle

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.example.vinilos.data.model.Album
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
}
