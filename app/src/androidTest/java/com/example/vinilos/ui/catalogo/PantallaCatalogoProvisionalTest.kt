package com.example.vinilos.ui.catalogo

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.theme.VinilosTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PantallaCatalogoProvisionalTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun elEnlaceProvisionalNotificaElIdDelAlbumDePrueba() {
        var idSeleccionado: Int? = null
        rule.setContent {
            VinilosTheme {
                PantallaCatalogoProvisional(
                    session = Session.Visitor,
                    onAtras = {},
                    onSeleccionarAlbum = { idSeleccionado = it }
                )
            }
        }

        rule.onNodeWithTag(TagCatalogoAlbumProvisional).performClick()

        assertEquals(AlbumIdProvisional, idSeleccionado)
    }
}
