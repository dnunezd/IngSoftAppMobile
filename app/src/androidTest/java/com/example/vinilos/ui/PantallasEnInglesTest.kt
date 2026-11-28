package com.example.vinilos.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Comment
import com.example.vinilos.data.model.ArtistPrize
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.artistas.ArtistDetailScreen
import com.example.vinilos.ui.artistas.TagFechaArtistDetail
import com.example.vinilos.ui.comentar.AddCommentActions
import com.example.vinilos.ui.comentar.AddCommentScreen
import com.example.vinilos.ui.create.CreateAlbumActions
import com.example.vinilos.ui.create.CreateAlbumScreen
import com.example.vinilos.ui.detail.AlbumContent
import com.example.vinilos.ui.detail.presentarAlbum
import com.example.vinilos.ui.entry.EntryScreen
import com.example.vinilos.ui.home.HomeScreen
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistDetailUiState
import com.example.vinilos.viewmodel.CargaAlbum
import com.example.vinilos.viewmodel.AddCommentUiState
import com.example.vinilos.viewmodel.CreateAlbumUiState
import java.util.Locale
import org.junit.Rule
import org.junit.Test

/**
 * Screens in English (internacionalizar-ingles, D8): translated texts and descriptions, locale
 * formats, and the same accessibility checks as in Spanish. The runner leaves the app in
 * Spanish; here the locale is changed only within the composition.
 */
class PantallasEnInglesTest {

    @get:Rule
    val rule = createComposeRule()

    private val coleccionista = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co"))
    private val album = Album(
        100, "Buscando América", null, "Rubén Blades album", "Salsa", "Elektra", "1984-08-01",
        listOf(Track("Decisiones", "5:05")), emptyList(), listOf(Comment("Great", 4), Comment("Superb", 5))
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
        mostrar { EntryScreen(onVisitante = {}, onColeccionista = {}) }
        rule.onNodeWithContentDescription("Enter as Visitor").assertExists()
        rule.onNodeWithContentDescription("Enter as Collector").assertExists()
    }

    @Test
    fun inicioEnIngles() {
        mostrar { HomeScreen(session = coleccionista, onVerAlbumes = {}, onVerArtistas = {}) }
        rule.onNodeWithText("Hello, Manolo Bellon!").assertExists()
        rule.onNodeWithText("Discover music in its purest format").assertExists()
    }

    @Test
    fun detalleDeAlbumConFechaYPromedioEnIngles() {
        mostrar { AlbumContent(presentarAlbum(album, Locale.ENGLISH), onAtras = {}) }
        rule.onNodeWithText("August 1, 1984").assertExists()
        rule.onNode(hasText("4.5")).assertExists()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Tracks"))
        rule.onNodeWithText("Tracks").assertExists()
    }

    @Test
    fun createAlbumEnIngles() {
        mostrar {
            CreateAlbumScreen(estado = CreateAlbumUiState(), session = coleccionista, acciones = CreateAlbumActions())
        }
        rule.onNodeWithContentDescription("Album name, required").assertExists()
        rule.onNodeWithContentDescription("Genre, required, Select a genre").assertExists()
    }

    @Test
    fun addCommentEnIngles() {
        mostrar {
            AddCommentScreen(
                estado = AddCommentUiState(album = CargaAlbum.Listo(album)),
                session = coleccionista,
                acciones = AddCommentActions({}, {}, {}, {}, {}, {}, {})
            )
        }
        rule.onNodeWithText("What did you think of this vinyl?").assertExists()
        rule.onNodeWithContentDescription("4 out of 5 stars").assertExists()
    }

    /**
     * The saved detail (e.g. opened earlier in Spanish) carries the date in ISO format and shows up in
     * English, even offline: no Spanish-formatted date is left stored in Room.
     */
    @Test
    fun detalleDeArtistaGuardadoEnIngles() {
        val queen = ArtistDetail(
            101, "Queen", null, "British band", ArtistType.BANDA, "1970-01-01T00:00:00.000Z",
            emptyList(), emptyList(), listOf(ArtistPrize(1, "Grammy Award", "NARAS", "1978-12-10T00:00:00.000Z"))
        )
        mostrar {
            ArtistDetailScreen(
                estado = ArtistDetailUiState.Success(queen, errorAlActualizar = true),
                onSeleccionarAlbum = {},
                onReintentar = {},
                onAtras = {},
                onAvisoMostrado = {}
            )
        }
        rule.onNodeWithTag(TagFechaArtistDetail).assertTextEquals("January 1, 1970")
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("December 10, 1978"))
        rule.onNodeWithText("December 10, 1978").assertExists()
    }
}
