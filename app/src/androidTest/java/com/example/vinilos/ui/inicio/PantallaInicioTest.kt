package com.example.vinilos.ui.inicio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagBuscador
import com.example.vinilos.ui.theme.VinilosTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PantallaInicioTest {

    @get:Rule
    val rule = createComposeRule()

    private var albumes = 0
    private var artistas = 0

    private fun mostrar(session: Session = Session.Visitor) {
        rule.setContent {
            VinilosTheme {
                PantallaInicio(
                    session = session,
                    onVerAlbumes = { albumes++ },
                    onVerArtistas = { artistas++ }
                )
            }
        }
    }

    @Test
    fun elColeccionistaVeSuNombreEnElSaludoYNoMelomano() {
        mostrar(Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co")))

        rule.onNodeWithText("¡Hola, Manolo Bellon!").assertIsDisplayed()
        rule.onNodeWithText("¡Hola, melómano!").assertDoesNotExist()
        // El resto de la pantalla es igual que para el visitante.
        rule.onNodeWithText("Descubre música en su formato más puro").assertIsDisplayed()
    }

    @Test
    fun sinSesionElSaludoEsElDelVisitante() {
        mostrar(Session.None)

        rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
    }

    @Test
    fun muestraSaludoTitularYSubtitulo() {
        mostrar()

        rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
        rule.onNodeWithText("Descubre música en su formato más puro").assertIsDisplayed()
        rule.onNodeWithText("Elige qué deseas explorar hoy en tu colección y viaja por el sonido análogo.")
            .assertIsDisplayed()
        rule.onNodeWithText("VINILO CLUB").assertIsDisplayed()
    }

    @Test
    fun muestraLaTarjetaDelCatalogoDeAlbumes() {
        mostrar()

        rule.onNodeWithText("CATÁLOGO COMPLETO · LPS").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Catálogo de Álbumes").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(
            "Explora cientos de discos clásicos, joyas vintage y ediciones limitadas en vinilo de 33 RPM."
        ).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Ver álbumes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun muestraLaTarjetaDelListadoDeArtistas() {
        mostrar()

        rule.onNodeWithText("LEYENDAS & BANDAS").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Listado de Artistas").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(
            "Conoce las trayectorias, bandas, discografías y reconocimientos de tus músicos favoritos."
        ).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Ver artistas").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun verAlbumesAvisaSoloAlCatalogo() {
        mostrar()

        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()

        assertEquals(1, albumes)
        assertEquals(0, artistas)
    }

    @Test
    fun verArtistasAvisaSoloAlListadoDeArtistas() {
        mostrar()

        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()

        assertEquals(0, albumes)
        assertEquals(1, artistas)
    }

    @Test
    fun elEncabezadoNoTieneFlechaDeAtrasNiBuscador() {
        mostrar()

        rule.onNodeWithTag(TagAtras).assertDoesNotExist()
        rule.onNodeWithTag(TagBuscador).assertDoesNotExist()
    }
}
