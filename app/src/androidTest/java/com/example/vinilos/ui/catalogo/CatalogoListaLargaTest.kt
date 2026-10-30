package com.example.vinilos.ui.catalogo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import com.example.vinilos.RepositorioAlbumesFalso
import org.junit.Rule
import org.junit.Test

/** Criterio 7: con muchos álbumes se recorre toda la lista sin que la app se detenga. */
class CatalogoListaLargaTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun conCincuentaAlbumesSeLlegaAlUltimoDesplazandose() {
        val repositorio = RepositorioAlbumesFalso(catalogo = listOf(Result.success(albumesDePrueba(50))))
        rule.setContent { CatalogoConViewModel(repositorio) }

        rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals("50 lanzamientos")
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(50)))

        rule.onNodeWithTag(tagAlbum(50)).assertIsDisplayed()
    }

    @Test
    fun alVolverArribaSeVeOtraVezElPrimero() {
        val repositorio = RepositorioAlbumesFalso(catalogo = listOf(Result.success(albumesDePrueba(50))))
        rule.setContent { CatalogoConViewModel(repositorio) }

        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(50)))
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(1)))

        rule.onNodeWithTag(tagAlbum(1)).assertIsDisplayed()
    }
}
