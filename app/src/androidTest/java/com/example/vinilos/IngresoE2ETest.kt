package com.example.vinilos

import android.util.Log
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
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.catalogo.TagCatalogo
import com.example.vinilos.ui.catalogo.TagRolActivo
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagBuscador
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.TagMensajeError
import com.example.vinilos.ui.ingreso.TagProgreso
import com.example.vinilos.ui.ingreso.TagReintentar
import com.example.vinilos.ui.ingreso.TagVisitanteError
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import com.example.vinilos.ui.inicio.TagVerArtistas
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E de la HU00 contra el backend real BackVynils (datos de sql/data.sql: coleccionistas 100 y 101).
 *
 * Requiere el backend en el PC y el teléfono con `adb reverse tcp:3000 tcp:3000`, instalando la app con
 * `-PbaseUrl=http://localhost:3000/`. La prueba de recuperación necesita además que alguien levante el
 * backend mientras corre: solo se ejecuta con el argumento `e2eRecuperacion=true`.
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

    @Test
    fun ingresarComoVisitanteLlevaAlInicioYDeAhiAlCatalogo() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()

            esperarA(TagInicio)
            rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Visitante")
        }
    }

    @Test
    fun ingresarComoColeccionistaMuestraLaListaRealYSeEligeUno() {
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()

            esperarA(tagColeccionista(100))
            rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
            rule.onNodeWithText("manollo@caracol.com.co").assertIsDisplayed()
            rule.onNodeWithText("MB").assertIsDisplayed()
            rule.onNodeWithText("Jaime Monsalve").assertIsDisplayed()
            rule.onNodeWithText("jmonsalve@rtvc.com.co").assertIsDisplayed()

            rule.onNodeWithTag(tagColeccionista(100)).performClick()

            esperarA(TagInicio)
            rule.onNodeWithText("¡Hola, Manolo Bellon!").assertIsDisplayed()
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Manolo Bellon")
        }
    }

    @Test
    fun elBuscadorFiltraLosColeccionistasReales() {
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            esperarA(tagColeccionista(100))

            rule.onNodeWithTag(TagBuscador).performTextInput("RTVC")

            rule.onNodeWithTag(tagColeccionista(101)).assertIsDisplayed()
            rule.onNodeWithTag(tagColeccionista(100)).assertDoesNotExist()

            rule.onNodeWithTag(tagColeccionista(101)).performClick()
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Jaime Monsalve")
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

    /**
     * Backend detenido al empezar: se ve el error, el Visitante sigue disponible, y al levantar el
     * backend "Reintentar" muestra la lista. Solo corre con `e2eRecuperacion=true`.
     */
    @Test
    fun conElBackendCaidoSeVeElErrorYReintentarRecuperaLaLista() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("e2eRecuperacion") == "true")

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()

            esperarA(TagMensajeError)
            rule.onNodeWithText("No pudimos cargar los coleccionistas").assertIsDisplayed()
            rule.onNodeWithTag(TagVisitanteError).assertIsDisplayed()
            Log.i("E2E", "ERROR_VISIBLE")

            // Reintenta como lo haría una persona hasta que el backend responda.
            val limite = System.currentTimeMillis() + 120_000
            while (System.currentTimeMillis() < limite) {
                if (rule.onAllNodesWithTag(tagColeccionista(100)).fetchSemanticsNodes().isNotEmpty()) break
                if (rule.onAllNodesWithTag(TagReintentar).fetchSemanticsNodes().isNotEmpty()) {
                    rule.onNodeWithTag(TagReintentar).performClick()
                    rule.waitUntil(15_000) {
                        rule.onAllNodesWithTag(TagProgreso).fetchSemanticsNodes().isEmpty()
                    }
                }
                Thread.sleep(3_000)
            }

            rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
            Log.i("E2E", "RECUPERADO")
        }
    }
}
