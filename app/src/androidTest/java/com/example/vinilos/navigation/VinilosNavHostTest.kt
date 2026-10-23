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

    private fun verAlbumes() {
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun verArtistas() {
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun flechaAtras() {
        rule.onNodeWithTag(TagAtras).performClick()
        rule.waitForIdle()
    }

    /** El botón "Atrás" del sistema: la pantalla de inicio no tiene flecha en el encabezado. */
    private fun atrasDelSistema() {
        rule.runOnIdle { nav.popBackStack() }
        rule.waitForIdle()
    }

    @Test
    fun laAppAbreEnElIngresoSinSesionYSinPantallaAnterior() {
        // Given
        val repositorio = RepositorioFalso(Result.success(listOf(manolo)))

        // When
        mostrar(repositorio)

        // Then: sin pantalla anterior, "Atrás" del sistema cierra la app
        assertEquals(Rutas.Ingreso, ruta())
        assertEquals(Session.None, session.session.value)
        assertNull(nav.previousBackStackEntry)
    }

    @Test
    fun visitanteEntraAlInicioConSesionDeVisitante() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))

        // When
        entrarComoVisitante()

        // Then
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)
        rule.onNodeWithTag(TagInicio).assertIsDisplayed()
        rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
    }

    @Test
    fun visitanteEntraAlInicioAunqueElBackendEsteCaido() {
        // Given
        val repositorio = RepositorioFalso(Result.failure(ErrorDeCarga()))
        mostrar(repositorio)

        // When
        entrarComoVisitante()

        // Then
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(0, repositorio.consultas)
    }

    @Test
    fun coleccionistaAbreElSelectorSinSeleccionarNada() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))

        // When
        entrarComoColeccionista()

        // Then
        assertEquals(Rutas.Coleccionistas, ruta())
        assertEquals(Session.None, session.session.value)
        rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
    }

    @Test
    fun elegirUnColeccionistaEntraAlInicioConEseColeccionista() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()

        // When
        elegir(jaime)

        // Then
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Collector(jaime), session.session.value)
        assertEquals(true, session.session.value.collectorActionsEnabled)
        rule.onNodeWithText("¡Hola, Jaime Monsalve!").assertIsDisplayed()
        rule.onNodeWithText("¡Hola, melómano!").assertDoesNotExist()
    }

    @Test
    fun atrasDesdeElSelectorVuelveAlIngresoSinSesion() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoColeccionista()

        // When
        flechaAtras()

        // Then
        assertEquals(Rutas.Ingreso, ruta())
        assertEquals(Session.None, session.session.value)
    }

    @Test
    fun atrasDesdeElSelectorEnErrorVuelveAlIngresoSinCerrar() {
        // Given
        mostrar(RepositorioFalso(Result.failure(ErrorDeCarga())))
        entrarComoColeccionista()
        rule.onNodeWithTag(TagReintentar).assertIsDisplayed()

        // When
        flechaAtras()

        // Then
        assertEquals(Rutas.Ingreso, ruta())
    }

    @Test
    fun visitanteDesdeElErrorEntraAlInicio() {
        // Given
        mostrar(RepositorioFalso(Result.failure(ErrorDeCarga())))
        entrarComoColeccionista()

        // When
        rule.onNodeWithTag(TagVisitanteError).performClick()
        rule.waitForIdle()

        // Then
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)
    }

    @Test
    fun atrasDesdeElInicioEntradoDesdeElErrorVuelveAlIngresoSinSesion() {
        // Given
        mostrar(RepositorioFalso(Result.failure(ErrorDeCarga())))
        entrarComoColeccionista()
        rule.onNodeWithTag(TagVisitanteError).performClick()
        rule.waitForIdle()

        // When
        atrasDelSistema()

        // Then
        assertEquals(Rutas.Ingreso, ruta())
        assertEquals(Session.None, session.session.value)
    }

    @Test
    fun atrasDesdeElInicioComoVisitanteVuelveAlIngresoSinSesion() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()

        // When
        atrasDelSistema()

        // Then
        assertEquals(Rutas.Ingreso, ruta())
        assertEquals(Session.None, session.session.value)
    }

    @Test
    fun atrasDesdeElInicioComoColeccionistaVuelveAlIngresoYNoAlSelector() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()
        elegir(manolo)
        rule.onNodeWithTag(TagInicio).assertIsDisplayed()

        // When
        atrasDelSistema()

        // Then
        assertEquals(Rutas.Ingreso, ruta())
        assertEquals(Session.None, session.session.value)
        assertNull(nav.previousBackStackEntry)
    }

    @Test
    fun cambiarDePerfilTrasVolverAlIngresoEntraConElNuevoPerfil() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()
        elegir(manolo)
        atrasDelSistema()

        // When
        entrarComoVisitante()

        // Then: el saludo cambia con el perfil
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)
        rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
        rule.onNodeWithText("¡Hola, Manolo Bellon!").assertDoesNotExist()
    }

    @Test
    fun elBuscadorFiltraLaLista() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()

        // When
        rule.onNodeWithTag(TagBuscador).performTextInput("rtvc")
        rule.waitForIdle()

        // Then
        rule.onNodeWithTag(tagColeccionista(100)).assertDoesNotExist()
        rule.onNodeWithTag(tagColeccionista(101)).assertIsDisplayed()
    }

    @Test
    fun elegirUnResultadoDeLaBusquedaEntraAlInicioConEseColeccionista() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()
        rule.onNodeWithTag(TagBuscador).performTextInput("rtvc")
        rule.waitForIdle()

        // When
        elegir(jaime)

        // Then
        assertEquals(Session.Collector(jaime), session.session.value)
        assertEquals(Rutas.Inicio, ruta())
    }

    @Test
    fun verAlbumesLlevaAlCatalogo() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()
        elegir(jaime)

        // When
        verAlbumes()

        // Then
        assertEquals(Rutas.Catalogo, ruta())
        rule.onNodeWithTag(TagCatalogo).assertIsDisplayed()
    }

    @Test
    fun laFlechaDelCatalogoVuelveAlInicioConservandoLaSesion() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo, jaime))))
        entrarComoColeccionista()
        elegir(jaime)
        verAlbumes()

        // When
        flechaAtras()

        // Then
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Collector(jaime), session.session.value)
    }

    @Test
    fun verArtistasLlevaAlListado() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()

        // When
        verArtistas()

        // Then
        assertEquals(Rutas.Artistas, ruta())
        rule.onNodeWithTag(TagArtistas).assertIsDisplayed()
    }

    @Test
    fun laFlechaDelListadoDeArtistasVuelveAlInicioConservandoLaSesion() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()
        verArtistas()

        // When
        flechaAtras()

        // Then
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)
    }

    @Test
    fun atrasDelSistemaDesdeElCatalogoVuelveAlInicio() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()
        verAlbumes()

        // When
        atrasDelSistema()

        // Then
        assertEquals(Rutas.Inicio, ruta())
    }

    @Test
    fun atrasDelSistemaDesdeLosArtistasVuelveAlInicioConservandoLaSesion() {
        // Given
        mostrar(RepositorioFalso(Result.success(listOf(manolo))))
        entrarComoVisitante()
        verArtistas()

        // When
        atrasDelSistema()

        // Then
        assertEquals(Rutas.Inicio, ruta())
        assertEquals(Session.Visitor, session.session.value)
    }
}
