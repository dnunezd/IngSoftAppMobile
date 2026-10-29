package com.example.vinilos.ui.catalogo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.example.vinilos.RepositorioAlbumesFalso
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.remote.ErrorDeCarga
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Criterio 10: deslizar la lista hacia abajo actualiza el catálogo sin reiniciar la app. */
class CatalogoActualizarTest {

    @get:Rule
    val rule = createComposeRule()

    private val races = AlbumResumen(103, "A Day at the Races", null, "Rock", "Queen", "1976")
    private val buscando = AlbumResumen(100, "Buscando América", null, "Salsa", "Rubén Blades", "1984")
    private val poeta = AlbumResumen(101, "Poeta del pueblo", null, "Salsa", "Rubén Blades", "1984")

    private fun deslizarHaciaAbajo() {
        rule.onNodeWithTag(TagCuadriculaCatalogo).performTouchInput {
            swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600)
        }
        rule.waitForIdle()
    }

    @Test
    fun alDeslizarApareceElAlbumNuevoEnSuPosicionAlfabetica() {
        val repositorio = RepositorioAlbumesFalso(
            catalogo = listOf(Result.success(listOf(races, poeta)), Result.success(listOf(races, buscando, poeta)))
        )
        rule.setContent { CatalogoConViewModel(repositorio) }
        rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals("2 lanzamientos")

        deslizarHaciaAbajo()

        rule.waitUntil(timeoutMillis = 5_000) { repositorio.consultasCatalogo == 2 }
        rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals("3 lanzamientos")
        assertEquals(listOf(races, buscando, poeta).map { tagAlbum(it.id) }, rule.tagsDeAlbumesEnPantalla().take(3))
    }

    @Test
    fun alDeslizarElCatalogoVacioMuestraLosAlbumesQueLleguen() {
        val repositorio = RepositorioAlbumesFalso(
            catalogo = listOf(Result.success(emptyList()), Result.success(listOf(races)))
        )
        rule.setContent { CatalogoConViewModel(repositorio) }
        rule.onNodeWithTag(TagVacioCatalogo).assertIsDisplayed()

        deslizarHaciaAbajo()

        rule.waitUntil(timeoutMillis = 5_000) { repositorio.consultasCatalogo == 2 }
        rule.onNodeWithTag(tagAlbum(races.id)).assertIsDisplayed()
    }

    @Test
    fun siFallaLaActualizacionSeConservaLaListaYSeAvisa() {
        val repositorio = RepositorioAlbumesFalso(
            catalogo = listOf(Result.success(listOf(races, poeta)), Result.failure(ErrorDeCarga()))
        )
        rule.setContent { CatalogoConViewModel(repositorio) }

        deslizarHaciaAbajo()

        rule.waitUntil(timeoutMillis = 5_000) { repositorio.consultasCatalogo == 2 }
        rule.onNodeWithText("No pudimos cargar el catálogo").assertIsDisplayed()
        rule.onNodeWithTag(tagAlbum(races.id)).assertIsDisplayed()
        rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals("2 lanzamientos")
    }

    @Test
    fun enErrorDeslizarNoConsultaYReintentarSi() {
        val repositorio = RepositorioAlbumesFalso(
            catalogo = listOf(Result.failure(ErrorDeCarga()), Result.success(listOf(races)))
        )
        rule.setContent { CatalogoConViewModel(repositorio) }
        rule.onNodeWithTag(TagErrorCatalogo).assertIsDisplayed()

        deslizarHaciaAbajo()
        assertEquals(1, repositorio.consultasCatalogo)

        rule.onNodeWithTag(TagReintentarCatalogo).performClick()
        rule.waitUntil(timeoutMillis = 5_000) { repositorio.consultasCatalogo == 2 }
        rule.onNodeWithTag(tagAlbum(races.id)).assertIsDisplayed()
    }
}
