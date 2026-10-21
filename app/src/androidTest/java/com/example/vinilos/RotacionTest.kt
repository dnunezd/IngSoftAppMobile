package com.example.vinilos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.vinilos.data.model.Collector
import com.example.vinilos.ui.catalogo.TagRolActivo
import com.example.vinilos.ui.ingreso.TagBuscador
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Recrea la Activity (lo que pasa al girar la pantalla) y comprueba que no se pierde nada. */
class RotacionTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val manolo = Collector(100, "Manolo Bellon", "manollo@caracol.com.co")
    private val jaime = Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co")

    private fun conRepositorio(repositorio: RepositorioFalso) {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(colectoresRepositorio = repositorio)
    }

    @Test
    fun alGirarElSelectorConservaListaYBusquedaSinVolverAConsultar() {
        val repositorio = RepositorioFalso(Result.success(listOf(manolo, jaime)))
        conRepositorio(repositorio)

        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
            rule.onNodeWithTag(TagBuscador).performTextInput("bel")
            rule.waitForIdle()

            escenario.recreate()
            rule.waitForIdle()

            rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
            rule.onNodeWithText("Jaime Monsalve").assertDoesNotExist()
            rule.onNodeWithTag(TagBuscador).assertTextEquals("bel")
            assertEquals(1, repositorio.consultas)
        }
    }

    @Test
    fun alGirarLaPantallaDeInicioSigueVisibleYConservaLaSesion() {
        conRepositorio(RepositorioFalso(Result.success(listOf(manolo, jaime))))

        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
            rule.onNodeWithTag(tagColeccionista(101)).performClick()
            rule.onNodeWithTag(TagInicio).assertIsDisplayed()

            escenario.recreate()
            rule.waitForIdle()

            rule.onNodeWithTag(TagInicio).assertIsDisplayed()
            rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Jaime Monsalve")
        }
    }

    @Test
    fun alGirarElCatalogoConservaLaSesionDelColeccionista() {
        conRepositorio(RepositorioFalso(Result.success(listOf(manolo, jaime))))

        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
            rule.onNodeWithTag(tagColeccionista(101)).performClick()
            rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Jaime Monsalve")

            escenario.recreate()
            rule.waitForIdle()

            rule.onNodeWithTag(TagRolActivo).assertTextEquals("Coleccionista: Jaime Monsalve")
        }
    }
}
