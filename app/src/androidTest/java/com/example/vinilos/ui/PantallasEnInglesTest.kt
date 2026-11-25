package com.example.vinilos.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.comentar.AccionesComentarAlbum
import com.example.vinilos.ui.comentar.PantallaComentarAlbum
import com.example.vinilos.ui.crear.AccionesCrearAlbum
import com.example.vinilos.ui.crear.PantallaCrearAlbum
import com.example.vinilos.ui.detalle.ContenidoAlbum
import com.example.vinilos.ui.detalle.presentarAlbum
import com.example.vinilos.ui.ingreso.PantallaIngreso
import com.example.vinilos.ui.inicio.PantallaInicio
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.CargaAlbum
import com.example.vinilos.viewmodel.ComentarAlbumUiState
import com.example.vinilos.viewmodel.CrearAlbumUiState
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Pantallas en inglés (internacionalizar-ingles, D8): textos y descripciones traducidos, formatos del
 * idioma y las mismas comprobaciones de accesibilidad que en español. El runner deja la app en
 * español; aquí se cambia el idioma solo dentro de la composición.
 */
class PantallasEnInglesTest {

    @get:Rule
    val rule = createComposeRule()

    private val coleccionista = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co"))
    private val album = Album(
        100, "Buscando América", null, "Rubén Blades album", "Salsa", "Elektra", "1984-08-01",
        listOf(Cancion("Decisiones", "5:05")), emptyList(), listOf(Comentario("Great", 4), Comentario("Superb", 5))
    )

    @Composable
    private fun EnIngles(contenido: @Composable () -> Unit) {
        val configuracion = LocalConfiguration.current
        val contexto = LocalContext.current
        val ingles = remember(configuracion) { Configuration(configuracion).apply { setLocale(Locale.ENGLISH) } }
        val contextoIngles = remember(contexto, ingles) { contexto.createConfigurationContext(ingles) }
        CompositionLocalProvider(
            LocalConfiguration provides ingles,
            LocalContext provides contextoIngles,
            LocalResources provides contextoIngles.resources
        ) { VinilosTheme { contenido() } }
    }

    private fun mostrar(contenido: @Composable () -> Unit) {
        rule.enableAccessibilityChecks()
        rule.setContent { EnIngles(contenido) }
        val raices = rule.onAllNodes(isRoot())
        raices.fetchSemanticsNodes().indices.forEach { raices[it].tryPerformAccessibilityChecks() }
    }

    @Test
    fun ingresoEnIngles() {
        mostrar { PantallaIngreso(onVisitante = {}, onColeccionista = {}) }
        rule.onNodeWithContentDescription("Enter as Visitor").assertExists()
        rule.onNodeWithContentDescription("Enter as Collector").assertExists()
    }

    @Test
    fun inicioEnIngles() {
        mostrar { PantallaInicio(session = coleccionista, onVerAlbumes = {}, onVerArtistas = {}) }
        rule.onNodeWithText("Hello, Manolo Bellon!").assertExists()
        rule.onNodeWithText("Discover music in its purest format").assertExists()
    }

    @Test
    fun detalleDeAlbumConFechaYPromedioEnIngles() {
        mostrar { ContenidoAlbum(presentarAlbum(album, Locale.ENGLISH), onAtras = {}) }
        rule.onNodeWithText("August 1, 1984").assertExists()
        rule.onNode(hasText("4.5")).assertExists()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Tracks"))
        rule.onNodeWithText("Tracks").assertExists()
    }

    @Test
    fun crearAlbumEnIngles() {
        mostrar {
            PantallaCrearAlbum(estado = CrearAlbumUiState(), session = coleccionista, acciones = AccionesCrearAlbum())
        }
        rule.onNodeWithContentDescription("Album name, required").assertExists()
        rule.onNodeWithContentDescription("Genre, required, Select a genre").assertExists()
    }

    @Test
    fun comentarAlbumEnIngles() {
        mostrar {
            PantallaComentarAlbum(
                estado = ComentarAlbumUiState(album = CargaAlbum.Listo(album)),
                session = coleccionista,
                acciones = AccionesComentarAlbum({}, {}, {}, {}, {}, {}, {})
            )
        }
        rule.onNodeWithText("What did you think of this vinyl?").assertExists()
        rule.onNodeWithContentDescription("4 out of 5 stars").assertExists()
    }
}
