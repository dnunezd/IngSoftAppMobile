package com.example.vinilos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.artistas.PantallaArtistas
import com.example.vinilos.ui.artistas.PantallaDetalleArtista
import com.example.vinilos.ui.catalogo.PantallaCatalogo
import com.example.vinilos.ui.crear.AccionesAgregarCancion
import com.example.vinilos.ui.crear.AccionesCrearAlbum
import com.example.vinilos.ui.crear.PantallaAgregarCancion
import com.example.vinilos.ui.crear.PantallaCrearAlbum
import com.example.vinilos.ui.detalle.ContenidoAlbum
import com.example.vinilos.ui.detalle.TagAgregarCancionDetalle
import com.example.vinilos.ui.detalle.TagListaDetalleAlbum
import com.example.vinilos.ui.detalle.presentarAlbum
import com.example.vinilos.ui.ingreso.PantallaColeccionistas
import com.example.vinilos.ui.ingreso.PantallaIngreso
import com.example.vinilos.ui.inicio.PantallaInicio
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AgregarCancionUiState
import com.example.vinilos.viewmodel.AlbumsUiState
import com.example.vinilos.viewmodel.ArtistaDetalleUiState
import com.example.vinilos.viewmodel.ArtistsUiState
import com.example.vinilos.viewmodel.CollectorsUiState
import com.example.vinilos.viewmodel.CrearAlbumUiState
import com.example.vinilos.viewmodel.EnvioCancion
import com.example.vinilos.viewmodel.InfoAlbumDestino
import com.example.vinilos.viewmodel.validarCancion
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import androidx.compose.ui.test.isRoot
import com.example.vinilos.ui.comentar.AccionesComentarAlbum
import com.example.vinilos.ui.comentar.PantallaComentarAlbum
import com.example.vinilos.ui.artistas.AccionesAsociarAlbum
import com.example.vinilos.ui.artistas.VentanaAsociarAlbum
import com.example.vinilos.viewmodel.AsociarAlbumUiState
import com.example.vinilos.viewmodel.ListadoAsociable
import com.example.vinilos.viewmodel.CargaAlbum
import com.example.vinilos.viewmodel.ComentarAlbumUiState
import com.example.vinilos.viewmodel.EnvioComentario

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
    private val coleccionista = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co"))
    private val accionesComentar = AccionesComentarAlbum({}, {}, {}, {}, {}, {}, {})

    private val album = Album(
        100, "Buscando América", null, "Álbum de Rubén Blades y Seis del Solar", "Salsa", "Elektra",
        "1984-08-01", listOf(Cancion("Decisiones", "5:05")), listOf(Artista("Rubén Blades", null)),
        listOf(Comentario("Excelente", 5))
    )

    /** Comprobaciones de accesibilidad de la pantalla y, además, que cada título esperado sea un encabezado. */
    private fun verificar(vararg encabezados: String, contenido: @Composable () -> Unit) {
        rule.enableAccessibilityChecks()
        rule.setContent { ConEscala { VinilosTheme { contenido() } } }
        // Un diálogo o una hoja inferior viven en su propia ventana: se revisa cada raíz.
        val raices = rule.onAllNodes(isRoot())
        raices.fetchSemanticsNodes().indices.forEach { raices[it].tryPerformAccessibilityChecks() }
        encabezados.forEach {
            val encabezado = hasText(it, substring = true) and isHeading()
            // En una lista larga (o con la fuente al 200 %) el título puede no estar compuesto aún.
            val yaCompuesto = rule.onAllNodes(encabezado).fetchSemanticsNodes().isNotEmpty()
            if (!yaCompuesto && rule.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes().isNotEmpty()) {
                rule.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(encabezado)
            }
            // Puede haber varios con el mismo texto (título de la barra y del listado): basta con uno.
            assertTrue(
                "Falta el encabezado \"$it\"",
                rule.onAllNodes(encabezado).fetchSemanticsNodes().isNotEmpty()
            )
        }
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
    fun losCamposDelFormularioAnuncianSuEtiquetaComoUnSoloElemento() {
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
        listOf("Nombre del álbum, obligatorio", "Portada (URL), obligatorio", "Descripción, obligatorio").forEach {
            rule.onNodeWithContentDescription(it).assertExists()
        }
        // Fecha y listas: el nodo combinado lleva etiqueta y valor, es accionable y el texto de adentro
        // no queda como un segundo elemento (TalkBack se detenía en ese texto y decía solo el valor).
        listOf(
            "Fecha de lanzamiento, obligatorio, DD/MM/AAAA" to "DD/MM/AAAA",
            "Género, obligatorio, Selecciona un género" to "Selecciona un género",
            "Disquera, obligatorio, Selecciona una disquera" to "Selecciona una disquera"
        ).forEach { (descripcion, textoInterno) ->
            rule.onNode(hasContentDescription(descripcion) and hasClickAction()).assertExists()
            rule.onAllNodesWithText(textoInterno).assertCountEquals(0)
        }
    }

    @Test
    fun elPromedioExponeSuNumeroYLasEstrellasNoLoRepiten() {
        rule.setContent { ConEscala { VinilosTheme { ContenidoAlbum(presentarAlbum(album), onAtras = {}) } } }
        rule.onNode(hasText("5,0") and hasContentDescription("de 5 estrellas", substring = true)).assertExists()
        rule.onAllNodes(hasContentDescription("5,0 de 5 estrellas", substring = true)).assertCountEquals(0)
    }

    @Test
    fun cadaPistaEsUnSoloElemento() {
        rule.setContent { ConEscala { VinilosTheme { ContenidoAlbum(presentarAlbum(album), onAtras = {}) } } }
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Decisiones"))
        rule.onNode(hasText("01") and hasText("Decisiones") and hasText("5:05")).assertExists()
    }

    @Test
    fun cadaComentarioEsUnSoloElementoAunqueSeRepitaLaCalificacion() {
        val conRepetidos = album.copy(comentarios = listOf(Comentario("Excelente", 4), Comentario("Muy bueno", 4)))
        rule.setContent { ConEscala { VinilosTheme { ContenidoAlbum(presentarAlbum(conRepetidos), onAtras = {}) } } }
        listOf("Excelente", "Muy bueno").forEach { texto ->
            rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(texto))
            rule.onNode(hasText(texto) and hasContentDescription("4 / 5")).assertExists()
        }
    }

    @Test
    fun lasEstrellasDelComentarioPublicadoSonSoloDeLectura() {
        rule.setContent {
            ConEscala {
                VinilosTheme {
                    PantallaComentarAlbum(
                        estado = ComentarAlbumUiState(
                            album = CargaAlbum.Listo(album), calificacion = 4, opinion = "Gran disco",
                            envio = EnvioComentario.Publicado
                        ),
                        session = coleccionista, acciones = accionesComentar
                    )
                }
            }
        }
        rule.onNode(hasContentDescription("4 de 5 estrellas") and hasClickAction().not()).assertExists()
    }

    @Test
    fun ingreso() = verificar {
        PantallaIngreso(onVisitante = {}, onColeccionista = {})
    }

    @Test
    fun coleccionistas() = verificar("Quién") {
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
    fun inicio() = verificar("Descubre música", "Catálogo de Álbumes", "Listado de Artistas") {
        PantallaInicio(session = Session.Visitor, onVerAlbumes = {}, onVerArtistas = {})
    }

    @Test
    fun catalogo() = verificar("Álbumes", "Explora", "Catálogo") {
        PantallaCatalogo(
            estado = AlbumsUiState.Exito(albumes),
            session = Session.Visitor,
            onActualizar = {}, onReintentar = {}, onAvisoMostrado = {}, onSeleccionar = {}, onAtras = {}
        )
    }

    @Test
    fun detalleDeAlbum() = verificar("Artistas", "Canciones", "Comentarios") {
        ContenidoAlbum(presentarAlbum(album), onAtras = {})
    }

    @Test
    fun artistas() = verificar("Artistas", "Explora") {
        PantallaArtistas(
            estado = ArtistsUiState.Exito(listOf(queen, ruben)),
            onBuscar = {}, onReintentar = {}, onSeleccionar = {}, onAtras = {}, onAvisoMostrado = {}
        )
    }

    @Test
    fun detalleDeArtista() = verificar("Artista", "Álbumes") {
        PantallaDetalleArtista(
            estado = ArtistaDetalleUiState.Exito(
                ArtistaDetalle(
                    101, "Queen", null, "Banda británica", ArtistType.BANDA, "1970-01-01T00:00:00.000Z",
                    emptyList(), listOf(AlbumArtista(100, "Bailando con lobos", null, "2020")), emptyList()
                )
            ),
            onSeleccionarAlbum = {}, onReintentar = {}, onAtras = {}
        )
    }

    @Test
    fun comentarAlbum() = verificar("Comentar álbum", "Qué te pareció") {
        PantallaComentarAlbum(
            estado = ComentarAlbumUiState(album = CargaAlbum.Listo(album)),
            session = coleccionista, acciones = accionesComentar
        )
    }

    @Test
    fun comentarioPublicado() = verificar("Comentario publicado exitosamente") {
        PantallaComentarAlbum(
            estado = ComentarAlbumUiState(
                album = CargaAlbum.Listo(album), calificacion = 4, opinion = "Gran disco",
                envio = EnvioComentario.Publicado
            ),
            session = coleccionista, acciones = accionesComentar
        )
    }

    @Test
    fun descartarComentario() = verificar("Comentar álbum") {
        PantallaComentarAlbum(
            estado = ComentarAlbumUiState(
                album = CargaAlbum.Listo(album), calificacion = 3, opinion = "Borrador", confirmarDescarte = true
            ),
            session = coleccionista, acciones = accionesComentar
        )
    }

    @Test
    fun asociarAlbum() = verificar("Asociar álbum a Queen") {
        VentanaAsociarAlbum(
            estado = AsociarAlbumUiState(
                abierta = true, listado = ListadoAsociable.Listo(albumes), asociados = setOf(103)
            ),
            nombreArtista = "Queen", acciones = AccionesAsociarAlbum()
        )
    }

    @Test
    fun crearAlbum() = verificar("Crear", "Nuevo") {
        PantallaCrearAlbum(
            estado = CrearAlbumUiState(),
            session = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co")),
            acciones = AccionesCrearAlbum()
        )
    }

    // ---------- HU08: agregar canción (revisión de PR) ----------

    @Test
    fun agregarCancionFormularioVacioConTarjetaDeAlbumDestino() = verificar("Agregar canción", "Datos de la canción") {
        PantallaAgregarCancion(
            estado = AgregarCancionUiState(
                albumInfo = InfoAlbumDestino("Buscando América", null, "Rubén Blades")
            ),
            session = coleccionista,
            acciones = AccionesAgregarCancion()
        )
    }

    @Test
    fun agregarCancionConErrorDeFormatoYEnvioFallido() = verificar("Agregar canción") {
        PantallaAgregarCancion(
            estado = AgregarCancionUiState(
                nombre = "Decisiones",
                duracion = "abc",
                errores = validarCancion("Decisiones", "abc"),
                envio = EnvioCancion.Fallida
            ),
            session = coleccionista,
            acciones = AccionesAgregarCancion()
        )
    }

    @Test
    fun agregarCancionGuardando() = verificar("Agregar canción") {
        PantallaAgregarCancion(
            estado = AgregarCancionUiState(
                nombre = "Decisiones",
                duracion = "5:05",
                errores = emptyMap(),
                envio = EnvioCancion.Guardando
            ),
            session = coleccionista,
            acciones = AccionesAgregarCancion()
        )
    }

    @Test
    fun agregarCancionAlbumNoDisponible() = verificar("Agregar canción") {
        PantallaAgregarCancion(
            estado = AgregarCancionUiState(envio = EnvioCancion.AlbumNoDisponible),
            session = coleccionista,
            acciones = AccionesAgregarCancion()
        )
    }

    @Test
    fun detalleDeAlbumConBotonAgregarCancionParaElColeccionista() {
        rule.enableAccessibilityChecks()
        rule.setContent {
            ConEscala {
                VinilosTheme {
                    ContenidoAlbum(presentarAlbum(album), onAtras = {}, session = coleccionista)
                }
            }
        }
        rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasTestTag(TagAgregarCancionDetalle))
        rule.onNodeWithTag(TagAgregarCancionDetalle).assertExists()
        rule.onRoot().tryPerformAccessibilityChecks()
    }
}
