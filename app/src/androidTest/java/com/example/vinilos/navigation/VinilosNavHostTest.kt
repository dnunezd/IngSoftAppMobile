package com.example.vinilos.navigation

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.example.vinilos.RepositorioFalso
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Session
import com.example.vinilos.data.repository.ErrorDeCarga
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.catalogo.TagCatalogo
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

    private fun mostrar(repositorio: RepositorioFalso) {
        rule.setContent {
            val contexto = LocalContext.current
            nav = remember {
                TestNavHostController(contexto).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            }
            VinilosTheme { VinilosNavHost(nav, session, repositorio) }
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
        entrarComoVisitante()

        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        rule.waitForIdle()
        atrasDelSistema()
        assertEquals(Rutas.Inicio, ruta())

        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
        atrasDelSistema()
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)
    }
}
