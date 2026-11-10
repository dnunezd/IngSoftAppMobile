package com.example.vinilos.navigation

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Session
import com.example.vinilos.data.repository.ArtistaNoDisponible
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.artistas.TagBotonFavorito
import com.example.vinilos.ui.artistas.TagBuscadorArtistas
import com.example.vinilos.ui.artistas.TagDetalleArtista
import com.example.vinilos.ui.artistas.TagListaArtistas
import com.example.vinilos.ui.artistas.TagListaDetalleArtista
import com.example.vinilos.ui.artistas.TagNombreDetalleArtista
import com.example.vinilos.ui.artistas.TagTipoDetalleArtista
import com.example.vinilos.ui.artistas.tagAlbumArtista
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.catalogo.TagCatalogo
import com.example.vinilos.ui.catalogo.TagCuadriculaCatalogo
import com.example.vinilos.ui.catalogo.TagRolActivo
import com.example.vinilos.ui.catalogo.tagAlbum
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagBuscador
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.TagReintentar
import com.example.vinilos.ui.ingreso.TagVisitanteError
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import com.example.vinilos.ui.inicio.TagVerArtistas
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
    private val detalleQueen = ArtistaDetalle(
        101, "Queen", null, "Banda británica", ArtistType.BANDA, "1 de enero de 1970", emptyList(),
        listOf(AlbumArtista(100, "Bailando con lobos", null, "2020")), emptyList()
    )
    private val detalleRuben = ArtistaDetalle(
        100, "Rubén Blades Bellido de Luna", null, "Cantante", ArtistType.MUSICO, "16 de julio de 1948",
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
    /** En el catálogo hay un álbum con detalle (100) y otro que ya no existe en el backend (999). */
    private val catalogoDePrueba = listOf(
        AlbumResumen(100, "Bailando con lobos", null, "Rock", "", "2020"),
        AlbumResumen(999, "Zafiro perdido", null, "Folk", "", null)
    )
    private var albumes = RepositorioAlbumesFalso(
        mapOf(100 to Result.success(albumDePrueba)),
        catalogo = listOf(Result.success(catalogoDePrueba))
    )
    private val favoritos = RepositorioFavoritosFalso()

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
                    favoriteRepository = favoritos
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

    /** El botón "Atrás" del sistema: la pantalla de inicio no tiene flecha en el encabezado. */
    private fun atrasDelSistema() {
        rule.runOnIdle { nav.popBackStack() }
        rule.waitForIdle()
    }

    @Test
    fun laAppAbreEnElIngresoSinSesionYSinPantallaAnterior() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))

        assertEquals(Rutas.Ingreso, ruta())
        assertEquals(Session.None, session.session.value)
        // Sin pantalla anterior, "Atrás" del sistema cierra la app.
        assertNull(nav.previousBackStackEntry)
    }

    @Test
    fun visitanteEntraAlInicioConSesionDeVisitante() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))

        entrarComoVisitante()

        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)
        rule.onNodeWithTag(TagInicio).assertIsDisplayed()
        rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
    }

    @Test
    fun visitanteEntraAlInicioAunqueElBackendEsteCaido() {
        val repositorio = RepositorioFalso(Result.failure(ErrorDeCarga()))
        mostrar(repositorio)

        entrarComoVisitante()

        assertEquals(Rutas.Inicio, ruta())
        assertEquals(0, repositorio.consultas)
    }

    @Test
    fun coleccionistaAbreElSelectorSinSeleccionarNada() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))

        entrarComoColeccionista()

        assertEquals(Rutas.Coleccionistas, ruta())
        assertEquals(Session.None, session.session.value)
        rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
    }

    @Test
    fun elegirUnColeccionistaEntraAlInicioConEseColeccionista() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()

        elegir(jaime)

        assertEquals(Rutas.Inicio, ruta())
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

        assertEquals(Rutas.Ingreso, ruta())
        assertEquals(Session.None, session.session.value)
    }

    @Test
    fun atrasDesdeElSelectorEnErrorVuelveAlIngresoSinCerrar() {
        mostrar(RepositorioFalso(Result.failure(ErrorDeCarga())))
        entrarComoColeccionista()
        rule.onNodeWithTag(TagReintentar).assertIsDisplayed()

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()

        assertEquals(Rutas.Ingreso, ruta())
    }

    @Test
    fun visitanteDesdeElErrorEntraAlInicioYAtrasVuelveAlIngreso() {
        mostrar(RepositorioFalso(Result.failure(ErrorDeCarga())))
        entrarComoColeccionista()

        rule.onNodeWithTag(TagVisitanteError).performClick()
        rule.waitForIdle()
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)

        atrasDelSistema()
        assertEquals(Rutas.Ingreso, ruta())
        assertEquals(Session.None, session.session.value)
    }

    @Test
    fun atrasDesdeElInicioComoVisitanteVuelveAlIngresoSinSesion() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()

        atrasDelSistema()

        assertEquals(Rutas.Ingreso, ruta())
        assertEquals(Session.None, session.session.value)
    }

    @Test
    fun atrasDesdeElInicioComoColeccionistaVuelveAlIngresoYNoAlSelector() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()
        elegir(manolo)
        rule.onNodeWithTag(TagInicio).assertIsDisplayed()

        atrasDelSistema()

        assertEquals(Rutas.Ingreso, ruta())
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

        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)
        // El saludo cambia con el perfil.
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
        assertEquals(Rutas.Inicio, ruta())
    }

    @Test
    fun verAlbumesLlevaAlCatalogoYLaFlechaVuelveAlInicioConservandoLaSesion() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()
        elegir(jaime)

        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(Rutas.Catalogo, ruta())
        rule.onNodeWithTag(TagCatalogo).assertIsDisplayed()
        rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")
        rule.onNodeWithTag(tagAlbum(100)).assertIsDisplayed()

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Collector(jaime), session.session.value)
    }

    @Test
    fun verArtistasLlevaAlListadoYLaFlechaVuelveAlInicioConservandoLaSesion() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()

        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(Rutas.Artistas, ruta())
        rule.onNodeWithTag(TagArtistas).assertIsDisplayed()

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)
    }

    @Test
    fun atrasDelSistemaDesdeElCatalogoYLosArtistasVuelveAlInicio() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        irAlCatalogoComoVisitante()

        atrasDelSistema()
        assertEquals(Rutas.Inicio, ruta())

        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        atrasDelSistema()
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)
    }

    @Test
    fun seleccionarUnArtistaLlevaASuDetalleYAtrasVuelveAlListadoSinRepetirLaConsulta() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(TagBuscadorArtistas).performTextInput("que")
        rule.waitForIdle()

        rule.onNodeWithTag(tagArtista(queen)).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(Rutas.DetalleArtista, ruta())
        rule.onNodeWithTag(TagDetalleArtista).assertIsDisplayed()
        rule.onNodeWithTag(TagNombreDetalleArtista).assertTextEquals("Queen")
        rule.onNodeWithTag(TagTipoDetalleArtista).assertTextEquals("Banda")
        assertEquals(listOf(ArtistType.BANDA to 101), artistas.consultasDetalle)

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
        assertEquals(Rutas.Artistas, ruta())
        rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals("que")
        assertEquals(1, artistas.consultas)
    }

    @Test
    fun seleccionarUnAlbumDelArtistaAbreSuDetalleYVolverNoRepiteLaConsultaDelArtista() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(tagArtista(queen)).performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TagListaDetalleArtista).performScrollToNode(hasTestTag(tagAlbumArtista(100)))
        rule.onNodeWithTag(tagAlbumArtista(100)).performClick()
        rule.waitForIdle()
        assertEquals(Rutas.DetalleAlbum, ruta())
        assertEquals(100, albumes.ultimoId)

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
        assertEquals(Rutas.DetalleArtista, ruta())
        rule.onNodeWithTag(TagNombreDetalleArtista).assertTextEquals("Queen")
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
        rule.onNodeWithTag(TagNombreDetalleArtista).assertTextEquals("Rubén Blades Bellido de Luna")
        rule.onNodeWithTag(TagTipoDetalleArtista).assertTextEquals("Músico")

        atrasDelSistema()
        assertEquals(Rutas.Artistas, ruta())
        assertEquals(Session.Visitor, session.session.value)
    }

    @Test
    fun alVolverDesdeArtistaNoDisponibleElListadoSeActualizaSinElArtistaEliminado() {
        // Corrige un bug de revisión de PR: antes, "Volver al listado" solo hacía popBackStack()
        // y el listado conservaba al artista ya eliminado porque ArtistsViewModel solo consulta
        // en su init. Ahora debe forzar una nueva consulta (ArtistsViewModel.reintentar()).
        favoritos.esFavorito = Result.success(false)
        favoritos.agregarResultado = Result.failure(ArtistaNoDisponible())
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

        // El artista ya no existe en el backend real: la siguiente consulta del listado no debe
        // incluirlo.
        artistas.resultado = Result.success(listOf(queen))
        rule.onNodeWithText("Volver al listado de artistas").performClick()
        rule.waitForIdle()

        assertEquals(Rutas.Artistas, ruta())
        assertEquals(2, artistas.consultas)
        rule.onNodeWithTag(tagArtista(queen)).assertIsDisplayed()
        rule.onAllNodesWithTag(tagArtista(ruben)).assertCountEquals(0)
    }

    private fun irAlCatalogoComoVisitante() {
        entrarComoVisitante()
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

        assertEquals(Rutas.Catalogo, ruta())
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

        assertEquals(Rutas.DetalleAlbum, ruta())
        assertEquals(100, albumes.ultimoId)
        rule.onNodeWithText("Bailando con lobos").assertIsDisplayed()

        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
        assertEquals(Rutas.Catalogo, ruta())
        rule.onNodeWithTag(tagAlbum(100)).assertIsDisplayed()
        assertEquals(1, albumes.consultasCatalogo)
    }

    @Test
    fun atrasDelSistemaDesdeElDetalleVuelveAlCatalogoSinRepetirLaConsulta() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        irAlCatalogoComoVisitante()
        tocarAlbum(100)

        atrasDelSistema()

        assertEquals(Rutas.Catalogo, ruta())
        assertEquals(1, albumes.consultasCatalogo)
        assertEquals(Session.Visitor, session.session.value)
    }

    @Test
    fun elAlbumNoDisponibleMuestraElMensaje404YVolverLlevaAlCatalogo() {
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        irAlCatalogoComoVisitante()

        tocarAlbum(999)

        assertEquals(999, albumes.ultimoId)
        rule.onNodeWithText("El álbum ya no está disponible").assertIsDisplayed()
        rule.onNodeWithText("Volver al catálogo").performClick()
        rule.waitForIdle()
        assertEquals(Rutas.Catalogo, ruta())
        assertEquals(1, albumes.consultasCatalogo)
    }
}
