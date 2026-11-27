package com.example.vinilos.navigation

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.example.vinilos.RepositorioAlbumesFalso
import com.example.vinilos.RepositorioArtistasFalso
import com.example.vinilos.RepositorioFalso
import com.example.vinilos.RepositorioFavoritosFalso
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.ArtistAlbum
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.NewComment
import com.example.vinilos.data.model.Session
import com.example.vinilos.data.repository.ArtistNotAvailable
import com.example.vinilos.data.repository.CommentRepository
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.artistas.TagBotonAgregarAlbum
import com.example.vinilos.ui.artistas.TagBotonAsociar
import com.example.vinilos.ui.artistas.TagCancelarAsociar
import com.example.vinilos.ui.artistas.TagListaAsociar
import com.example.vinilos.ui.artistas.TagVentanaAsociar
import com.example.vinilos.ui.artistas.tagAlbumAsociable
import com.example.vinilos.ui.artistas.TagBotonFavorito
import com.example.vinilos.ui.artistas.TagBuscadorArtistas
import com.example.vinilos.ui.artistas.TagArtistDetail
import com.example.vinilos.ui.artistas.TagListaArtistas
import com.example.vinilos.ui.artistas.TagListaArtistDetail
import com.example.vinilos.ui.artistas.TagNombreArtistDetail
import com.example.vinilos.ui.artistas.TagTipoArtistDetail
import com.example.vinilos.ui.artistas.tagAlbumArtista
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.catalog.TagCatalogo
import com.example.vinilos.ui.catalog.TagCuadriculaCatalogo
import com.example.vinilos.ui.catalog.TagRolActivo
import com.example.vinilos.ui.catalog.tagAlbum
import com.example.vinilos.ui.create.TagCancelarAddTrack
import com.example.vinilos.ui.create.TagDuracionTrack
import com.example.vinilos.ui.create.TagGuardarTrack
import com.example.vinilos.ui.create.TagNombreTrack
import com.example.vinilos.ui.detail.TagAddTrackDetalle
import com.example.vinilos.ui.detail.TagListaDetalleAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.ui.entry.TagAtras
import com.example.vinilos.ui.entry.TagBuscador
import com.example.vinilos.ui.entry.TagEntrarColeccionista
import com.example.vinilos.ui.entry.TagEntrarVisitante
import com.example.vinilos.ui.entry.TagReintentar
import com.example.vinilos.ui.entry.TagVisitanteError
import com.example.vinilos.ui.entry.tagColeccionista
import com.example.vinilos.ui.home.TagInicio
import com.example.vinilos.ui.home.TagVerAlbumes
import com.example.vinilos.ui.home.TagVerArtistas
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.SessionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class VinilosNavHostTest {

    @get:Rule
    val rule = createComposeRule()

    private val manolo = Collector(100, "Manolo Bellon", "manollo@caracol.com.co")
    private val jaime = Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co")

    private lateinit var nav: TestNavHostController
    private val session = SessionViewModel()

    private val queen = Artist(101, "Queen", null, ArtistType.BANDA)
    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", null, ArtistType.MUSICO)
    private val detalleQueen = ArtistDetail(
        101, "Queen", null, "Banda británica", ArtistType.BANDA, "1970-01-01T00:00:00.000Z", emptyList(),
        listOf(ArtistAlbum(100, "Bailando con lobos", null, "2020")), emptyList()
    )
    private val detalleRuben = ArtistDetail(
        100, "Rubén Blades Bellido de Luna", null, "Cantante", ArtistType.MUSICO, "1948-07-16T00:00:00.000Z",
        emptyList(), emptyList(), emptyList()
    )
    private val artistas = RepositorioArtistasFalso(
        Result.success(listOf(queen, ruben)),
        mapOf(
            (ArtistType.BANDA to 101) to Result.success(detalleQueen),
            (ArtistType.MUSICO to 100) to Result.success(detalleRuben)
        )
    )

    private val albumDePrueba = Album(
        100, "Bailando con lobos", null, "Descripción de prueba",
        "Rock", "Disquera", "2020-01-01", emptyList(), emptyList(), emptyList()
    )
    /** The catalog has one album with detail (100) and another that no longer exists on the backend (999). */
    private val catalogoDePrueba = listOf(
        AlbumSummary(100, "Bailando con lobos", null, "Rock", "", "2020"),
        AlbumSummary(999, "Zafiro perdido", null, "Folk", "", null)
    )
    private var albumes = RepositorioAlbumesFalso(
        mapOf(100 to Result.success(albumDePrueba)),
        catalogo = listOf(Result.success(catalogoDePrueba))
    )
    private val favoritos = RepositorioFavoritosFalso()
    private val comentarios = object : CommentRepository {
        override suspend fun postComment(albumId: Int, comentario: NewComment): Result<Unit> =
            error("No se usa en esta prueba")
    }

    private fun mostrar(repositorio: RepositorioFalso, albumesRepositorio: RepositorioAlbumesFalso = albumes) {
        albumes = albumesRepositorio
        rule.setContent {
            val contexto = LocalContext.current
            nav = remember {
                TestNavHostController(contexto).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            }
            VinilosTheme {
                VinilosNavHost(
                    nav, session, repositorio, artistas, albumesRepositorio,
                    favoriteRepository = favoritos,
                    comentarioRepository = comentarios
                )
            }
        }
        rule.waitForIdle()
    }

    private fun ruta() = nav.currentBackStackEntry?.destination?.route

    private fun entrarComoVisitante() {
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun entrarComoColeccionista() {
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun elegir(coleccionista: Collector) {
        rule.onNodeWithTag(tagColeccionista(coleccionista.id)).performClick()
        rule.waitForIdle()
    }

    /** The system "Back" button: the home screen has no arrow in the header. */
    private fun atrasDelSistema() {
        rule.runOnIdle { nav.popBackStack() }
        rule.waitForIdle()
    }

    @Test
    fun laAppAbreEnElIngresoSinSesionYSinPantallaAnterior() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))

        assertEquals(Routes.INGRESO, ruta())
        assertEquals(Session.None, session.session.value)
        // With no previous screen, the system "Back" closes the app.
        assertNull(nav.previousBackStackEntry)
    }

    @Test
    fun visitanteEntraAlInicioConSesionDeVisitante() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))

        entrarComoVisitante()

        assertEquals(Routes.INICIO, ruta())
        assertEquals(Session.Visitor, session.session.value)
        rule.onNodeWithTag(TagInicio).assertIsDisplayed()
        rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
    }

    @Test
    fun visitanteEntraAlInicioAunqueElBackendEsteCaido() {
        val repositorio = RepositorioFalso(Result.failure(LoadError()))
        mostrar(repositorio)

        entrarComoVisitante()

        assertEquals(Routes.INICIO, ruta())
        assertEquals(0, repositorio.consultas)
    }

    @Test
    fun coleccionistaAbreElSelectorSinSeleccionarNada() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))

        entrarComoColeccionista()

        assertEquals(Routes.COLECCIONISTAS, ruta())
        assertEquals(Session.None, session.session.value)
        rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
    }

    @Test
    fun elegirUnColeccionistaEntraAlInicioConEseColeccionista() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()

        elegir(jaime)

        assertEquals(Routes.INICIO, ruta())
        assertEquals(Session.Collector(jaime), session.session.value)
        assertEquals(true, session.session.value.collectorActionsEnabled)
        rule.onNodeWithText("¡Hola, Jaime Monsalve!").assertIsDisplayed()
        rule.onNodeWithText("¡Hola, melómano!").assertDoesNotExist()
    }

    @Test
    fun atrasDesdeElSelectorVuelveAlIngresoSinSesion() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoColeccionista()

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()

        assertEquals(Routes.INGRESO, ruta())
        assertEquals(Session.None, session.session.value)
    }

    @Test
    fun atrasDesdeElSelectorEnErrorVuelveAlIngresoSinCerrar() {
        mostrar(RepositorioFalso(Result.failure(LoadError())))
        entrarComoColeccionista()
        rule.onNodeWithTag(TagReintentar).assertIsDisplayed()

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()

        assertEquals(Routes.INGRESO, ruta())
    }

    @Test
    fun visitanteDesdeElErrorEntraAlInicioYAtrasVuelveAlIngreso() {
        mostrar(RepositorioFalso(Result.failure(LoadError())))
        entrarComoColeccionista()

        rule.onNodeWithTag(TagVisitanteError).performClick()
        rule.waitForIdle()
        assertEquals(Routes.INICIO, ruta())
        assertEquals(Session.Visitor, session.session.value)

        atrasDelSistema()
        assertEquals(Routes.INGRESO, ruta())
        assertEquals(Session.None, session.session.value)
    }

    @Test
    fun atrasDesdeElInicioComoVisitanteVuelveAlIngresoSinSesion() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()

        atrasDelSistema()

        assertEquals(Routes.INGRESO, ruta())
        assertEquals(Session.None, session.session.value)
    }

    @Test
    fun atrasDesdeElInicioComoColeccionistaVuelveAlIngresoYNoAlSelector() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()
        elegir(manolo)
        rule.onNodeWithTag(TagInicio).assertIsDisplayed()

        atrasDelSistema()

        assertEquals(Routes.INGRESO, ruta())
        assertEquals(Session.None, session.session.value)
        assertNull(nav.previousBackStackEntry)
    }

    @Test
    fun cambiarDePerfilTrasVolverAlIngresoEntraConElNuevoPerfil() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()
        elegir(manolo)
        atrasDelSistema()

        entrarComoVisitante()

        assertEquals(Routes.INICIO, ruta())
        assertEquals(Session.Visitor, session.session.value)
        // The greeting changes with the profile.
        rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
        rule.onNodeWithText("¡Hola, Manolo Bellon!").assertDoesNotExist()
    }

    @Test
    fun elBuscadorFiltraLaListaYSeEligeUnResultado() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()

        rule.onNodeWithTag(TagBuscador).performTextInput("rtvc")
        rule.waitForIdle()

        rule.onNodeWithTag(tagColeccionista(100)).assertDoesNotExist()
        elegir(jaime)

        assertEquals(Session.Collector(jaime), session.session.value)
        assertEquals(Routes.INICIO, ruta())
    }

    @Test
    fun verAlbumesLlevaAlCatalogoYLaFlechaVuelveAlInicioConservandoLaSesion() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()
        elegir(jaime)

        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(Routes.CATALOGO, ruta())
        rule.onNodeWithTag(TagCatalogo).assertIsDisplayed()
        rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")
        rule.onNodeWithTag(tagAlbum(100)).assertIsDisplayed()

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
        assertEquals(Routes.INICIO, ruta())
        assertEquals(Session.Collector(jaime), session.session.value)
    }

    @Test
    fun verArtistasLlevaAlListadoYLaFlechaVuelveAlInicioConservandoLaSesion() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()

        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(Routes.ARTISTAS, ruta())
        rule.onNodeWithTag(TagArtistas).assertIsDisplayed()

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
        assertEquals(Routes.INICIO, ruta())
        assertEquals(Session.Visitor, session.session.value)
    }

    @Test
    fun atrasDelSistemaDesdeElCatalogoYLosArtistasVuelveAlInicio() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        irAlCatalogoComoVisitante()

        atrasDelSistema()
        assertEquals(Routes.INICIO, ruta())

        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        atrasDelSistema()
        assertEquals(Routes.INICIO, ruta())
        assertEquals(Session.Visitor, session.session.value)
    }

    @Test
    fun selectUnArtistaLlevaASuDetalleYAtrasVuelveAlListadoSinRepetirLaConsulta() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(TagBuscadorArtistas).performTextInput("que")
        rule.waitForIdle()

        rule.onNodeWithTag(tagArtista(queen)).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(Routes.DETALLE_ARTISTA, ruta())
        rule.onNodeWithTag(TagArtistDetail).assertIsDisplayed()
        rule.onNodeWithTag(TagNombreArtistDetail).assertTextEquals("Queen")
        rule.onNodeWithTag(TagTipoArtistDetail).assertTextEquals("Banda")
        assertEquals(listOf(ArtistType.BANDA to 101), artistas.consultasDetalle)

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
        assertEquals(Routes.ARTISTAS, ruta())
        rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals("que")
        assertEquals(1, artistas.consultas)
    }

    @Test
    fun selectUnAlbumDelArtistaAbreSuDetalleYVolverNoRepiteLaConsultaDelArtista() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(tagArtista(queen)).performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TagListaArtistDetail).performScrollToNode(hasTestTag(tagAlbumArtista(100)))
        rule.onNodeWithTag(tagAlbumArtista(100)).performClick()
        rule.waitForIdle()
        assertEquals(Routes.DETALLE_ALBUM, ruta())
        assertEquals(100, albumes.ultimoId)

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
        assertEquals(Routes.DETALLE_ARTISTA, ruta())
        rule.onNodeWithTag(TagNombreArtistDetail).assertTextEquals("Queen")
        assertEquals(listOf(ArtistType.BANDA to 101), artistas.consultasDetalle)
    }

    @Test
    fun elMusicoSeConsultaPorSuTipoEId() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(ruben)))
        rule.onNodeWithTag(tagArtista(ruben)).performClick()
        rule.waitForIdle()

        assertEquals(listOf(ArtistType.MUSICO to 100), artistas.consultasDetalle)
    }

    @Test
    fun atrasDelSistemaDesdeElDetalleDelArtistaVuelveAlListado() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(ruben)))
        rule.onNodeWithTag(tagArtista(ruben)).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(TagNombreArtistDetail).assertTextEquals("Rubén Blades Bellido de Luna")
        rule.onNodeWithTag(TagTipoArtistDetail).assertTextEquals("Músico")

        atrasDelSistema()
        assertEquals(Routes.ARTISTAS, ruta())
        assertEquals(Session.Visitor, session.session.value)
    }

    @Test
    fun alVolverDesdeArtistNotAvailableElListadoSeActualizaSinElArtistaEliminado() {
        // Fixes a PR review bug: previously, "Back to list" only called popBackStack()
        // and the list kept the already-removed artist because ArtistsViewModel only queries
        // in its init. It must now force a new query (ArtistsViewModel.reintentar()).
        favoritos.esFavorito = Result.success(false)
        favoritos.agregarResultado = Result.failure(ArtistNotAvailable())
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoColeccionista()
        elegir(manolo)
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(ruben)))
        rule.onNodeWithTag(tagArtista(ruben)).performClick()
        rule.waitForIdle()
        assertEquals(1, artistas.consultas)

        rule.onNodeWithTag(TagBotonFavorito).performScrollTo().performClick()
        rule.waitForIdle()

        // The artist no longer exists on the real backend: the next listing query must not
        // include it.
        artistas.resultado = Result.success(listOf(queen))
        rule.onNodeWithText("Volver al listado de artistas").performClick()
        rule.waitForIdle()

        assertEquals(Routes.ARTISTAS, ruta())
        assertEquals(2, artistas.consultas)
        rule.onNodeWithTag(tagArtista(queen)).assertIsDisplayed()
        rule.onAllNodesWithTag(tagArtista(ruben)).assertCountEquals(0)
    }

    // ---------- Add an album to an artist (HU15) ----------

    private fun abrirDetalleDeRubenComoColeccionista() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoColeccionista()
        elegir(manolo)
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(ruben)))
        rule.onNodeWithTag(tagArtista(ruben)).performClick()
        rule.waitForIdle()
    }

    private fun abrirVentanaAsociar() {
        rule.onNodeWithTag(TagListaArtistDetail).performScrollToNode(hasTestTag(TagBotonAgregarAlbum))
        rule.onNodeWithTag(TagBotonAgregarAlbum).performClick()
        rule.waitForIdle()
    }

    @Test
    fun elColeccionistaAsociaUnAlbumYLoVeEnLaDiscografiaDelArtista() {
        abrirDetalleDeRubenComoColeccionista()
        abrirVentanaAsociar()
        rule.onNodeWithText("Asociar álbum a Rubén Blades Bellido de Luna").assertIsDisplayed()

        // What the backend would respond when the detail is queried again.
        artistas.detalles = artistas.detalles + ((ArtistType.MUSICO to 100) to Result.success(
            detalleRuben.copy(albumes = listOf(ArtistAlbum(100, "Bailando con lobos", null, "2020")))
        ))
        rule.onNodeWithTag(TagListaAsociar).performScrollToNode(hasTestTag(tagAlbumAsociable(100)))
        rule.onNodeWithTag(tagAlbumAsociable(100)).performClick()
        rule.onNodeWithTag(TagBotonAsociar).performClick()
        rule.waitForIdle()

        assertEquals(listOf(Triple(ArtistType.MUSICO, 100, 100)), artistas.asociacionesPedidas)
        rule.onNodeWithTag(TagVentanaAsociar).assertDoesNotExist()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Álbum asociado").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag(TagListaArtistDetail).performScrollToNode(hasTestTag(tagAlbumArtista(100)))
        rule.onNodeWithTag(tagAlbumArtista(100)).assertIsDisplayed()
        assertEquals(2, artistas.consultasDetalle.size)
    }

    @Test
    fun cancelarCierraLaVentanaSinAsociar() {
        abrirDetalleDeRubenComoColeccionista()
        abrirVentanaAsociar()
        rule.onNodeWithTag(TagListaAsociar).performScrollToNode(hasTestTag(tagAlbumAsociable(100)))
        rule.onNodeWithTag(tagAlbumAsociable(100)).performClick()

        rule.onNodeWithTag(TagCancelarAsociar).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TagVentanaAsociar).assertDoesNotExist()
        assertEquals(emptyList<Triple<ArtistType, Int, Int>>(), artistas.asociacionesPedidas)
        assertEquals(Routes.DETALLE_ARTISTA, ruta())
    }

    @Test
    fun elVisitanteNoVeAgregarAlbumEnElDetalleDelArtista() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(tagArtista(queen)).performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TagListaArtistDetail).performScrollToNode(hasTestTag(tagAlbumArtista(100)))
        rule.onAllNodesWithTag(TagBotonAgregarAlbum).assertCountEquals(0)
    }

    private fun irAlCatalogoComoVisitante() {
        entrarComoVisitante()
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun irAlCatalogoComoColeccionista() {
        entrarComoColeccionista()
        elegir(manolo)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun tocarAlbum(id: Int) {
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(id)))
        rule.onNodeWithTag(tagAlbum(id)).performClick()
        rule.waitForIdle()
    }

    @Test
    fun elVisitanteVeElCatalogoConSusAlbumesYSuEtiqueta() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        irAlCatalogoComoVisitante()

        assertEquals(Routes.CATALOGO, ruta())
        rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
        rule.onNodeWithTag(tagAlbum(100)).assertIsDisplayed()
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(999)))
        rule.onNodeWithTag(tagAlbum(999)).assertIsDisplayed()
        assertEquals(1, albumes.consultasCatalogo)
    }

    @Test
    fun tocarUnAlbumAbreSuDetalleYLaFlechaVuelveAlCatalogoSinRepetirLaConsulta() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        irAlCatalogoComoVisitante()

        tocarAlbum(100)

        assertEquals(Routes.DETALLE_ALBUM, ruta())
        assertEquals(100, albumes.ultimoId)
        rule.onNodeWithText("Bailando con lobos").assertIsDisplayed()
        rule.onAllNodesWithTag(TagAddTrackDetalle).assertCountEquals(0)

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
        assertEquals(Routes.CATALOGO, ruta())
        rule.onNodeWithTag(tagAlbum(100)).assertIsDisplayed()
        assertEquals(1, albumes.consultasCatalogo)
    }

    @Test
    fun elColeccionistaAbreElFormularioDesdeElDetalleYCancelaSinEnviar() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        irAlCatalogoComoColeccionista()
        tocarAlbum(100)

        rule.onNodeWithTag(TagListaDetalleAlbum)
            .performScrollToNode(hasTestTag(TagAddTrackDetalle))
        rule.onNodeWithTag(TagAddTrackDetalle).performClick()
        rule.waitForIdle()
        assertEquals(Routes.AGREGAR_CANCION, ruta())

        rule.onNodeWithTag(TagDuracionTrack).performTextInput("5:05")
        rule.onNodeWithTag(TagNombreTrack).performTextInput("Decisiones")
        rule.onNodeWithTag(TagCancelarAddTrack).performScrollTo().performClick()
        rule.waitForIdle()

        assertEquals(Routes.DETALLE_ALBUM, ruta())
        assertEquals(emptyList<Pair<Int, com.example.vinilos.data.model.Track>>(), albumes.cancionesEnviadas)
    }

    @Test
    fun saveTrackRegresaAlDetalleActualizadoYConfirma() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        albumes.resultadoAddTrack = Result.success(Unit)
        irAlCatalogoComoColeccionista()
        tocarAlbum(100)

        rule.onNodeWithTag(TagListaDetalleAlbum)
            .performScrollToNode(hasTestTag(TagAddTrackDetalle))
        rule.onNodeWithTag(TagAddTrackDetalle).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(TagNombreTrack).performTextInput("Decisiones")
        rule.onNodeWithTag(TagDuracionTrack).performTextInput("5:05")
        rule.onNodeWithTag(TagGuardarTrack).performClick()
        rule.waitForIdle()

        assertEquals(Routes.DETALLE_ALBUM, ruta())
        rule.onNodeWithText("Decisiones").assertIsDisplayed()
        rule.onNodeWithText("5:05").assertIsDisplayed()
        rule.onNodeWithText("Canción agregada").assertIsDisplayed()
    }

    @Test
    fun atrasDelSistemaDesdeElDetalleVuelveAlCatalogoSinRepetirLaConsulta() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        irAlCatalogoComoVisitante()
        tocarAlbum(100)

        atrasDelSistema()

        assertEquals(Routes.CATALOGO, ruta())
        assertEquals(1, albumes.consultasCatalogo)
        assertEquals(Session.Visitor, session.session.value)
    }

    @Test
    fun elAlbumNotAvailableMuestraElMensaje404YVolverLlevaAlCatalogo() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        irAlCatalogoComoVisitante()

        tocarAlbum(999)

        assertEquals(999, albumes.ultimoId)
        rule.onNodeWithText("El álbum ya no está disponible").assertIsDisplayed()
        rule.onNodeWithText("Volver al catálogo").performClick()
        rule.waitForIdle()
        assertEquals(Routes.CATALOGO, ruta())
        assertEquals(1, albumes.consultasCatalogo)
    }
}
