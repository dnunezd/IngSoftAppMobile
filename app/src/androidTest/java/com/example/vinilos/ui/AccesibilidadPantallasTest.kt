package com.example.vinilos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.artistas.PantallaArtistas
import com.example.vinilos.ui.artistas.PantallaDetalleArtista
import com.example.vinilos.ui.catalogo.PantallaCatalogo
import com.example.vinilos.ui.crear.AccionesCrearAlbum
import com.example.vinilos.ui.crear.PantallaCrearAlbum
import com.example.vinilos.ui.detalle.ContenidoAlbum
import com.example.vinilos.ui.detalle.presentarAlbum
import com.example.vinilos.ui.ingreso.PantallaColeccionistas
import com.example.vinilos.ui.ingreso.PantallaIngreso
import com.example.vinilos.ui.inicio.PantallaInicio
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AlbumsUiState
import com.example.vinilos.viewmodel.ArtistaDetalleUiState
import com.example.vinilos.viewmodel.ArtistsUiState
import com.example.vinilos.viewmodel.CollectorsUiState
import com.example.vinilos.viewmodel.CrearAlbumUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Regresión de accesibilidad (corregir-accesibilidad-scanner): activa las comprobaciones de
 * accesibilidad de Compose (etiquetas, destinos táctiles, elementos duplicados) y recorre las ocho
 * pantallas con la fuente normal (1x) y ampliada (2x). El contraste de color se cubre en
 * `ContrasteDePaletaTest`, porque estas comprobaciones no lo evalúan.
 */
@RunWith(Parameterized::class)
class AccesibilidadPantallasTest(private val escalaDeFuente: Float) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "fuente {0}x")
        fun escalas() = listOf(1f, 2f)
    }

    @get:Rule
    val rule = createComposeRule()

    private val queen = Artist(101, "Queen", null, ArtistType.BANDA)
    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", null, ArtistType.MUSICO)
    private val albumes = listOf(
        AlbumResumen(103, "A Day at the Races", null, "Rock", "Queen", "1976"),
        AlbumResumen(100, "Buscando América", null, "Salsa", "Rubén Blades Bellido de Luna", "1984")
    )
    private val album = Album(
        100, "Buscando América", null, "Álbum de Rubén Blades y Seis del Solar", "Salsa", "Elektra",
        "1984-08-01", listOf(Cancion("Decisiones", "5:05")), emptyList(), listOf(Comentario("Excelente", 5))
    )

    private fun verificar(contenido: @Composable () -> Unit) {
        rule.enableAccessibilityChecks()
        rule.setContent { ConEscala { VinilosTheme { contenido() } } }
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Composable
    private fun ConEscala(contenido: @Composable () -> Unit) {
        val densidad = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(densidad.density, escalaDeFuente)) { contenido() }
    }

    @Test
    fun elBotonEntrarEsUnSoloElementoParaTalkBack() {
        rule.setContent { ConEscala { VinilosTheme { PantallaIngreso(onVisitante = {}, onColeccionista = {}) } } }
        rule.onNodeWithContentDescription("Entrar como Visitante").assertExists()
        rule.onNodeWithContentDescription("Entrar como Coleccionista").assertExists()
        // El texto "Entrar" no debe quedar como un segundo elemento junto a la descripción.
        rule.onAllNodesWithText("Entrar").assertCountEquals(0)
    }

    @Test
    fun losCamposDelFormularioAnuncianSuEtiqueta() {
        rule.setContent {
            ConEscala {
                VinilosTheme {
                    PantallaCrearAlbum(
                        estado = CrearAlbumUiState(),
                        session = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co")),
                        acciones = AccionesCrearAlbum()
                    )
                }
            }
        }
        listOf(
            "Nombre del álbum", "Portada (URL)", "Fecha de lanzamiento, DD/MM/AAAA",
            "Género, Selecciona un género", "Disquera, Selecciona una disquera", "Descripción"
        ).forEach { rule.onNodeWithContentDescription(it).assertExists() }
    }

    @Test
    fun ingreso() = verificar {
        PantallaIngreso(onVisitante = {}, onColeccionista = {})
    }

    @Test
    fun coleccionistas() = verificar {
        PantallaColeccionistas(
            estado = CollectorsUiState.Exito(
                listOf(
                    Collector(100, "Manolo Bellon", "manollo@caracol.com.co"),
                    Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co")
                )
            ),
            onBuscar = {}, onReintentar = {}, onSeleccionar = {}, onVisitante = {}, onAtras = {},
            onAvisoMostrado = {}
        )
    }

    @Test
    fun inicio() = verificar {
        PantallaInicio(session = Session.Visitor, onVerAlbumes = {}, onVerArtistas = {})
    }

    @Test
    fun catalogo() = verificar {
        PantallaCatalogo(
            estado = AlbumsUiState.Exito(albumes),
            session = Session.Visitor,
            onActualizar = {}, onReintentar = {}, onAvisoMostrado = {}, onSeleccionar = {}, onAtras = {}
        )
    }

    @Test
    fun detalleDeAlbum() = verificar {
        ContenidoAlbum(presentarAlbum(album), onAtras = {})
    }

    @Test
    fun artistas() = verificar {
        PantallaArtistas(
            estado = ArtistsUiState.Exito(listOf(queen, ruben)),
            onBuscar = {}, onReintentar = {}, onSeleccionar = {}, onAtras = {}, onAvisoMostrado = {}
        )
    }

    @Test
    fun detalleDeArtista() = verificar {
        PantallaDetalleArtista(
            estado = ArtistaDetalleUiState.Exito(
                ArtistaDetalle(
                    101, "Queen", null, "Banda británica", ArtistType.BANDA, "1 de enero de 1970",
                    emptyList(), listOf(AlbumArtista(100, "Bailando con lobos", null, "2020")), emptyList()
                )
            ),
            onSeleccionarAlbum = {}, onReintentar = {}, onAtras = {}
        )
    }

    @Test
    fun crearAlbum() = verificar {
        PantallaCrearAlbum(
            estado = CrearAlbumUiState(),
            session = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co")),
            acciones = AccionesCrearAlbum()
        )
    }
}
