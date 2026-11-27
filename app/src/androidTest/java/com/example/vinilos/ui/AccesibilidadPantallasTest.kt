package com.example.vinilos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.ArtistAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.Performer
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Comment
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.artistas.ArtistsScreen
import com.example.vinilos.ui.artistas.ArtistDetailScreen
import com.example.vinilos.ui.catalog.CatalogScreen
import com.example.vinilos.ui.create.AddTrackActions
import com.example.vinilos.ui.create.CreateAlbumActions
import com.example.vinilos.ui.create.AddTrackScreen
import com.example.vinilos.ui.create.CreateAlbumScreen
import com.example.vinilos.ui.detail.AlbumContent
import com.example.vinilos.ui.detail.TagAddTrackDetalle
import com.example.vinilos.ui.detail.TagListaDetalleAlbum
import com.example.vinilos.ui.detail.presentarAlbum
import com.example.vinilos.ui.entry.CollectorsScreen
import com.example.vinilos.ui.entry.EntryScreen
import com.example.vinilos.ui.home.HomeScreen
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AddTrackUiState
import com.example.vinilos.viewmodel.AlbumsUiState
import com.example.vinilos.viewmodel.ArtistDetailUiState
import com.example.vinilos.viewmodel.ArtistsUiState
import com.example.vinilos.viewmodel.CollectorsUiState
import com.example.vinilos.viewmodel.CreateAlbumUiState
import com.example.vinilos.viewmodel.TrackSubmission
import com.example.vinilos.viewmodel.TargetAlbumInfo
import com.example.vinilos.viewmodel.validarTrack
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import androidx.compose.ui.test.isRoot
import com.example.vinilos.ui.comentar.AddCommentActions
import com.example.vinilos.ui.comentar.AddCommentScreen
import com.example.vinilos.ui.artistas.AssociateAlbumActions
import com.example.vinilos.ui.artistas.AssociateAlbumDialog
import com.example.vinilos.viewmodel.AssociateAlbumUiState
import com.example.vinilos.viewmodel.ListadoAsociable
import com.example.vinilos.viewmodel.CargaAlbum
import com.example.vinilos.viewmodel.AddCommentUiState
import com.example.vinilos.viewmodel.EnvioComment

/**
 * Accessibility regression (corregir-accesibilidad-scanner): enables Compose accessibility
 * checks (labels, touch targets, duplicate elements) and walks through the eight screens with
 * normal (1x) and enlarged (2x) font scale. Color contrast is covered in
 * `ContrasteDePaletaTest`, since these checks don't evaluate it.
 */
@RunWith(Parameterized::class)
class AccesibilidadPantallasTest(private val escalaDeFuente: Float) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "fuente {0}x")
        fun escalas() = listOf(1f, 2f)
    }

    @get:Rule
    val rule = createComposeRule()

    private val queen = Artist(101, "Queen", null, ArtistType.BANDA)
    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", null, ArtistType.MUSICO)
    private val albumes = listOf(
        AlbumSummary(103, "A Day at the Races", null, "Rock", "Queen", "1976"),
        AlbumSummary(100, "Buscando América", null, "Salsa", "Rubén Blades Bellido de Luna", "1984")
    )
    private val coleccionista = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co"))
    private val accionesComentar = AddCommentActions({}, {}, {}, {}, {}, {}, {})

    private val album = Album(
        100, "Buscando América", null, "Álbum de Rubén Blades y Seis del Solar", "Salsa", "Elektra",
        "1984-08-01", listOf(Track("Decisiones", "5:05")), listOf(Performer("Rubén Blades", null)),
        listOf(Comment("Excelente", 5))
    )

    /** Accessibility checks for the screen, plus verifying that each expected title is a heading. */
    private fun verificar(vararg encabezados: String, contenido: @Composable () -> Unit) {
        rule.enableAccessibilityChecks()
        rule.setContent { ConEscala { VinilosTheme { contenido() } } }
        // A dialog or a bottom sheet lives in its own window: each root is checked.
        val raices = rule.onAllNodes(isRoot())
        raices.fetchSemanticsNodes().indices.forEach { raices[it].tryPerformAccessibilityChecks() }
        encabezados.forEach {
            val heading = hasText(it, substring = true) and isHeading()
            // In a long list (or with the font at 200%) the title may not be composed yet.
            val yaCompuesto = rule.onAllNodes(heading).fetchSemanticsNodes().isNotEmpty()
            if (!yaCompuesto && rule.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes().isNotEmpty()) {
                rule.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(heading)
            }
            // There may be several with the same text (bar title and list title): one is enough.
            assertTrue(
                "Falta el encabezado \"$it\"",
                rule.onAllNodes(heading).fetchSemanticsNodes().isNotEmpty()
            )
        }
    }

    @Composable
    private fun ConEscala(contenido: @Composable () -> Unit) {
        val densidad = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(densidad.density, escalaDeFuente)) { contenido() }
    }

    @Test
    fun elBotonEntrarEsUnSoloElementoParaTalkBack() {
        rule.setContent { ConEscala { VinilosTheme { EntryScreen(onVisitante = {}, onColeccionista = {}) } } }
        rule.onNodeWithContentDescription("Entrar como Visitante").assertExists()
        rule.onNodeWithContentDescription("Entrar como Coleccionista").assertExists()
        // The "Entrar" text should not remain as a second element next to the description.
        rule.onAllNodesWithText("Entrar").assertCountEquals(0)
    }

    @Test
    fun losCamposDelFormularioAnuncianSuEtiquetaComoUnSoloElemento() {
        rule.setContent {
            ConEscala {
                VinilosTheme {
                    CreateAlbumScreen(
                        estado = CreateAlbumUiState(),
                        session = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co")),
                        acciones = CreateAlbumActions()
                    )
                }
            }
        }
        listOf("Nombre del álbum, obligatorio", "Portada (URL), obligatorio", "Descripción, obligatorio").forEach {
            rule.onNodeWithContentDescription(it).assertExists()
        }
        // Date and lists: the combined node carries label and value, is actionable, and the text inside
        // it does not remain as a second element (TalkBack used to stop on that text and read only the value).
        listOf(
            "Fecha de lanzamiento, obligatorio, DD/MM/AAAA" to "DD/MM/AAAA",
            "Género, obligatorio, Selecciona un género" to "Selecciona un género",
            "Disquera, obligatorio, Selecciona una disquera" to "Selecciona una disquera"
        ).forEach { (descripcion, textoInterno) ->
            rule.onNode(hasContentDescription(descripcion) and hasClickAction()).assertExists()
            rule.onAllNodesWithText(textoInterno).assertCountEquals(0)
        }
    }

    @Test
    fun elPromedioExponeSuNumeroYLasEstrellasNoLoRepiten() {
        rule.setContent { ConEscala { VinilosTheme { AlbumContent(presentarAlbum(album), onAtras = {}) } } }
        rule.onNode(hasText("5,0") and hasContentDescription("de 5 estrellas", substring = true)).assertExists()
        rule.onAllNodes(hasContentDescription("5,0 de 5 estrellas", substring = true)).assertCountEquals(0)
    }

    @Test
    fun cadaPistaEsUnSoloElemento() {
        rule.setContent { ConEscala { VinilosTheme { AlbumContent(presentarAlbum(album), onAtras = {}) } } }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Decisiones"))
        rule.onNode(hasText("01") and hasText("Decisiones") and hasText("5:05")).assertExists()
    }

    @Test
    fun cadaCommentEsUnSoloElementoAunqueSeRepitaLaCalificacion() {
        val conRepetidos = album.copy(comentarios = listOf(Comment("Excelente", 4), Comment("Muy bueno", 4)))
        rule.setContent { ConEscala { VinilosTheme { AlbumContent(presentarAlbum(conRepetidos), onAtras = {}) } } }
        listOf("Excelente", "Muy bueno").forEach { texto ->
            rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(texto))
            rule.onNode(hasText(texto) and hasContentDescription("4 / 5")).assertExists()
        }
    }

    @Test
    fun lasEstrellasDelCommentPublicadoSonSoloDeLectura() {
        rule.setContent {
            ConEscala {
                VinilosTheme {
                    AddCommentScreen(
                        estado = AddCommentUiState(
                            album = CargaAlbum.Listo(album), calificacion = 4, opinion = "Gran disco",
                            envio = EnvioComment.Publicado
                        ),
                        session = coleccionista, acciones = accionesComentar
                    )
                }
            }
        }
        rule.onNode(hasContentDescription("4 de 5 estrellas") and hasClickAction().not()).assertExists()
    }

    @Test
    fun ingreso() = verificar {
        EntryScreen(onVisitante = {}, onColeccionista = {})
    }

    @Test
    fun coleccionistas() = verificar("Quién") {
        CollectorsScreen(
            estado = CollectorsUiState.Success(
                listOf(
                    Collector(100, "Manolo Bellon", "manollo@caracol.com.co"),
                    Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co")
                )
            ),
            onBuscar = {}, onReintentar = {}, onSeleccionar = {}, onVisitante = {}, onAtras = {},
            onAvisoMostrado = {}
        )
    }

    @Test
    fun inicio() = verificar("Descubre música", "Catálogo de Álbumes", "Listado de Artistas") {
        HomeScreen(session = Session.Visitor, onVerAlbumes = {}, onVerArtistas = {})
    }

    @Test
    fun catalogo() = verificar("Álbumes", "Explora", "Catálogo") {
        CatalogScreen(
            estado = AlbumsUiState.Success(albumes),
            session = Session.Visitor,
            onActualizar = {}, onReintentar = {}, onAvisoMostrado = {}, onSeleccionar = {}, onAtras = {}
        )
    }

    @Test
    fun detalleDeAlbum() = verificar("Artistas", "Canciones", "Comentarios") {
        AlbumContent(presentarAlbum(album), onAtras = {})
    }

    @Test
    fun artistas() = verificar("Artistas", "Explora") {
        ArtistsScreen(
            estado = ArtistsUiState.Success(listOf(queen, ruben)),
            onBuscar = {}, onReintentar = {}, onSeleccionar = {}, onAtras = {}, onAvisoMostrado = {}
        )
    }

    @Test
    fun detalleDeArtista() = verificar("Artista", "Álbumes") {
        ArtistDetailScreen(
            estado = ArtistDetailUiState.Success(
                ArtistDetail(
                    101, "Queen", null, "Banda británica", ArtistType.BANDA, "1970-01-01T00:00:00.000Z",
                    emptyList(), listOf(ArtistAlbum(100, "Bailando con lobos", null, "2020")), emptyList()
                )
            ),
            onSeleccionarAlbum = {}, onReintentar = {}, onAtras = {}
        )
    }

    @Test
    fun addComment() = verificar("Comentar álbum", "Qué te pareció") {
        AddCommentScreen(
            estado = AddCommentUiState(album = CargaAlbum.Listo(album)),
            session = coleccionista, acciones = accionesComentar
        )
    }

    @Test
    fun comentarioPublicado() = verificar("Comentario publicado exitosamente") {
        AddCommentScreen(
            estado = AddCommentUiState(
                album = CargaAlbum.Listo(album), calificacion = 4, opinion = "Gran disco",
                envio = EnvioComment.Publicado
            ),
            session = coleccionista, acciones = accionesComentar
        )
    }

    @Test
    fun descartarComment() = verificar("Comentar álbum") {
        AddCommentScreen(
            estado = AddCommentUiState(
                album = CargaAlbum.Listo(album), calificacion = 3, opinion = "Borrador", confirmarDescarte = true
            ),
            session = coleccionista, acciones = accionesComentar
        )
    }

    @Test
    fun associateAlbum() = verificar("Asociar álbum a Queen") {
        AssociateAlbumDialog(
            estado = AssociateAlbumUiState(
                abierta = true, listado = ListadoAsociable.Listo(albumes), asociados = setOf(103)
            ),
            nombreArtista = "Queen", acciones = AssociateAlbumActions()
        )
    }

    @Test
    fun createAlbum() = verificar("Crear", "Nuevo") {
        CreateAlbumScreen(
            estado = CreateAlbumUiState(),
            session = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co")),
            acciones = CreateAlbumActions()
        )
    }

    // ---------- HU08: add track (PR review) ----------

    @Test
    fun addTrackFormularioEmptyConTarjetaDeAlbumDestino() = verificar("Agregar canción", "Datos de la canción") {
        AddTrackScreen(
            estado = AddTrackUiState(
                albumInfo = TargetAlbumInfo("Buscando América", null, "Rubén Blades")
            ),
            session = coleccionista,
            acciones = AddTrackActions()
        )
    }

    @Test
    fun addTrackConErrorDeFormatoYEnvioFallido() = verificar("Agregar canción") {
        AddTrackScreen(
            estado = AddTrackUiState(
                nombre = "Decisiones",
                duracion = "abc",
                errores = validarTrack("Decisiones", "abc"),
                envio = TrackSubmission.Fallida
            ),
            session = coleccionista,
            acciones = AddTrackActions()
        )
    }

    @Test
    fun addTrackGuardando() = verificar("Agregar canción") {
        AddTrackScreen(
            estado = AddTrackUiState(
                nombre = "Decisiones",
                duracion = "5:05",
                errores = emptyMap(),
                envio = TrackSubmission.Guardando
            ),
            session = coleccionista,
            acciones = AddTrackActions()
        )
    }

    @Test
    fun addTrackAlbumNotAvailable() = verificar("Agregar canción") {
        AddTrackScreen(
            estado = AddTrackUiState(envio = TrackSubmission.AlbumNotAvailable),
            session = coleccionista,
            acciones = AddTrackActions()
        )
    }

    @Test
    fun detalleDeAlbumConBotonAddTrackParaElColeccionista() {
        rule.enableAccessibilityChecks()
        rule.setContent {
            ConEscala {
                VinilosTheme {
                    AlbumContent(presentarAlbum(album), onAtras = {}, session = coleccionista)
                }
            }
        }
        rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasTestTag(TagAddTrackDetalle))
        rule.onNodeWithTag(TagAddTrackDetalle).assertExists()
        rule.onRoot().tryPerformAccessibilityChecks()
    }
}
