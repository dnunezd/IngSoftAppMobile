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

    private fun entrarComoVisitante() {
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
    }

    /** Desde el ingreso, entra como el coleccionista de la lista real y espera la pantalla de inicio. */
    private fun entrarComoColeccionista(id: Int) {
        entrarComoColeccionista()
        esperarA(tagColeccionista(id))
        rule.onNodeWithTag(tagColeccionista(id)).performClick()
        esperarA(TagInicio)
    }

    /** Desde la pantalla de inicio, "Ver álbumes" lleva al catálogo. */
    private fun verAlbumes() {
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(TagCatalogo)
    }

    @Test
    fun ingresarComoVisitanteLlevaAlInicio() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            // When
            entrarComoVisitante()
            esperarA(TagInicio)

            // Then
            rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
        }
    }

    @Test
    fun desdeElInicioElVisitanteLlegaAlCatalogoConSuRol() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoVisitante()
            esperarA(TagInicio)

            // When
            verAlbumes()

            // Then
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Visitante")
        }
    }

    @Test
    fun ingresarComoColeccionistaMuestraLaListaReal() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            // When
            entrarComoColeccionista()
            esperarA(tagColeccionista(100))

            // Then
            rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
            rule.onNodeWithText("manollo@caracol.com.co").assertIsDisplayed()
            rule.onNodeWithText("MB").assertIsDisplayed()
            rule.onNodeWithText("Jaime Monsalve").assertIsDisplayed()
            rule.onNodeWithText("jmonsalve@rtvc.com.co").assertIsDisplayed()
        }
    }

    @Test
    fun elegirUnColeccionistaDeLaListaRealLlevaAlInicioConSuNombre() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            esperarA(tagColeccionista(100))

            // When
            rule.onNodeWithTag(tagColeccionista(100)).performClick()
            esperarA(TagInicio)

            // Then
            rule.onNodeWithText("¡Hola, Manolo Bellon!").assertIsDisplayed()
        }
    }

    @Test
    fun elColeccionistaElegidoLlegaAlCatalogoConSuRol() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)

            // When
            verAlbumes()

            // Then
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Manolo Bellon")
        }
    }

    @Test
    fun elBuscadorFiltraLosColeccionistasReales() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            esperarA(tagColeccionista(100))

            // When
            rule.onNodeWithTag(TagBuscador).performTextInput("RTVC")

            // Then
            rule.onNodeWithTag(tagColeccionista(101)).assertIsDisplayed()
            rule.onNodeWithTag(tagColeccionista(100)).assertDoesNotExist()
        }
    }

    @Test
    fun elegirUnResultadoFiltradoLlegaAlCatalogoConEseColeccionista() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            esperarA(tagColeccionista(100))
            rule.onNodeWithTag(TagBuscador).performTextInput("RTVC")

            // When
            rule.onNodeWithTag(tagColeccionista(101)).performClick()
            verAlbumes()

            // Then
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Jaime Monsalve")
        }
    }

    @Test
    fun atrasDesdeElSelectorVuelveAlIngreso() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            esperarA(tagColeccionista(100))

            // When
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagEntrarVisitante)

            // Then
            rule.onNodeWithTag(TagEntrarVisitante).assertIsDisplayed()
        }
    }

    @Test
    fun atrasDelSistemaDesdeElInicioVuelveAlIngresoYNoAlSelector() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(101)

            // When
            Espresso.pressBack()
            esperarA(TagEntrarVisitante)

            // Then
            rule.onAllNodesWithTag(TagBuscador).assertCountEquals(0)
        }
    }

    @Test
    fun elListadoDeArtistasVuelveAlInicioConLaFlechaConservandoLaSesion() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(101)

            // When
            rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
            esperarA(TagArtistas)
            rule.onNodeWithTag(TagAtras).performClick()
            verAlbumes()

            // Then
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Jaime Monsalve")
        }
    }

    @Test
    fun elCatalogoVuelveAlInicioConLaFlecha() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(101)
            verAlbumes()

            // When
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagInicio)

            // Then
            rule.onNodeWithTag(TagInicio).assertIsDisplayed()
        }
    }

    @Test
    fun cambiarDePerfilTrasVolverAlIngresoEntraComoVisitante() {
        // Given
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(101)
            Espresso.pressBack()
            esperarA(TagEntrarVisitante)

            // When
            entrarComoVisitante()
            verAlbumes()

            // Then
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Visitante")
        }
    }
}
