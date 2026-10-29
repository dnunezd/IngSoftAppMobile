package com.example.vinilos

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.catalogo.TagCatalogo
import com.example.vinilos.ui.catalogo.TagRolActivo
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagBuscador
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import com.example.vinilos.ui.inicio.TagVerArtistas
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E de la HU00 contra el backend real BackVynils (datos de sql/data.sql: coleccionistas 100 y 101).
 *
 * Requiere el backend en el PC y el teléfono con `adb reverse tcp:3000 tcp:3000`, instalando la app con
 * `-PbaseUrl=http://localhost:3000/`. La recuperación ante fallos del backend se prueba, de forma
 * automática y sin backend, en [RecuperacionTest].
 *
 * Los escenarios FN00-ESC01 a FN00-ESC03 guardan un pantallazo por paso y una fila en el informe de
 * `.../files/e2e-evidencia/FN00_ESCxx/informe.md` (solo en el dispositivo, no en el repo).
 */
class IngresoE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container = AppContainer()
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun entrarComoColeccionista() {
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
    }

    /** Desde la pantalla de inicio, "Ver álbumes" lleva al catálogo. */
    private fun verAlbumes() {
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(TagCatalogo)
    }

    /** FN00-ESC01: ingresar a la plataforma como visitante. */
    @Test
    fun ingresarComoVisitanteLlevaAlInicioYDeAhiAlCatalogo() {
        val evidencia = "FN00_ESC01"
        ActivityScenario.launch(MainActivity::class.java).use {
            esperarA(TagEntrarVisitante)
            EvidenciaE2E.capturar(rule, evidencia, "01_ingreso")
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()

            esperarA(TagInicio)
            rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_inicio_visitante")
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Visitante")
            EvidenciaE2E.capturar(rule, evidencia, "03_catalogo_visitante")
        }
    }

    /** FN00-ESC02: ingresar a la plataforma como coleccionista. */
    @Test
    fun ingresarComoColeccionistaMuestraLaListaRealYSeEligeUno() {
        val evidencia = "FN00_ESC02"
        ActivityScenario.launch(MainActivity::class.java).use {
            esperarA(TagEntrarColeccionista)
            EvidenciaE2E.capturar(rule, evidencia, "01_ingreso")
            entrarComoColeccionista()

            esperarA(tagColeccionista(100))
            rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
            rule.onNodeWithText("manollo@caracol.com.co").assertIsDisplayed()
            rule.onNodeWithText("MB").assertIsDisplayed()
            rule.onNodeWithText("Jaime Monsalve").assertIsDisplayed()
            rule.onNodeWithText("jmonsalve@rtvc.com.co").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_listado_coleccionistas")

            rule.onNodeWithTag(tagColeccionista(100)).performClick()

            esperarA(TagInicio)
            rule.onNodeWithText("¡Hola, Manolo Bellon!").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_inicio_coleccionista")
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Manolo Bellon")
            EvidenciaE2E.capturar(rule, evidencia, "04_catalogo_coleccionista")
        }
    }

    /** FN00-ESC03: ingresar como coleccionista usando la búsqueda. */
    @Test
    fun elBuscadorFiltraLosColeccionistasReales() {
        val evidencia = "FN00_ESC03"
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            esperarA(tagColeccionista(100))
            EvidenciaE2E.capturar(rule, evidencia, "01_listado_coleccionistas")

            rule.onNodeWithTag(TagBuscador).performTextInput("RTVC")

            rule.onNodeWithTag(tagColeccionista(101)).assertIsDisplayed()
            rule.onNodeWithTag(tagColeccionista(100)).assertDoesNotExist()
            EvidenciaE2E.capturar(rule, evidencia, "02_busqueda_filtrada")

            rule.onNodeWithTag(tagColeccionista(101)).performClick()
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Jaime Monsalve")
            EvidenciaE2E.capturar(rule, evidencia, "03_catalogo_coleccionista")
        }
    }

    @Test
    fun navegaEntreIngresoInicioCatalogoYArtistasConAtras() {
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            esperarA(tagColeccionista(100))

            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagEntrarVisitante)

            // Entrar como coleccionista lleva al inicio, y "Atrás" del sistema al ingreso, no al selector.
            entrarComoColeccionista()
            esperarA(tagColeccionista(101))
            rule.onNodeWithTag(tagColeccionista(101)).performClick()
            esperarA(TagInicio)

            // Del inicio al listado de artistas y de vuelta con la flecha, conservando la sesión.
            rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
            esperarA(TagArtistas)
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagInicio)
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Jaime Monsalve")

            // Catálogo -> inicio -> ingreso.
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagInicio)
            Espresso.pressBack()
            esperarA(TagEntrarVisitante)
            rule.onAllNodesWithTag(TagBuscador).assertCountEquals(0)

            // Cambiar de perfil: ahora como visitante.
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Visitante")
        }
    }
}
