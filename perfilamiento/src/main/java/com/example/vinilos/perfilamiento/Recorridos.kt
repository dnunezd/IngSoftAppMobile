package com.example.vinilos.perfilamiento

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorridos del plan de pruebas de rendimiento del Sprint 3 (docs/performance/guia-manual.md), uno
 * por HU. Cada paso es una acción seguida de la espera que indica el plan (5, 3 o 2 s); los envíos
 * esperan la respuesta del backend hasta 10 s. Antes del paso 1 hay 5 s de reposo con la app abierta
 * en el ingreso, como línea base.
 *
 * Cada paso se avisa con [AppBajoPrueba.marcarPaso] para que el script anote el segundo de la
 * grabación en que ocurrió. Si un paso abre otra pantalla, la espera cuenta desde que esa pantalla
 * aparece.
 *
 * Los lanza `scripts/perfilamiento/Perfilar.ps1`, uno por grabación y con la app recién abierta:
 * `adb shell am instrument -w -r -e class com.example.vinilos.perfilamiento.Recorridos#hu01
 *  com.example.vinilos.perfilamiento/androidx.test.runner.AndroidJUnitRunner`
 *
 * HU08, HU09 y HU15 siguen los pasos de la guía manual: cada paso, una acción y 5 s (en los envíos,
 * la respuesta del backend, hasta 10 s, y luego los 5 s).
 *
 * Suponen BackVynils local con los datos de ejemplo (`sql/data.sql`): álbumes 100 Buscando América,
 * 101 Poeta del pueblo, 102 A Night at the Opera y 103 A Day at the Races; músico 100 Rubén Blades
 * Bellido de Luna (álbumes 100 y 101), banda 101 Queen y coleccionista 100 Manolo Bellon. El script
 * deja los datos en ese estado antes de cada grabación (favoritos de HU10, canción de HU08,
 * comentario de HU09 y asociación de HU15) y borra lo que crea cada recorrido.
 */
@RunWith(AndroidJUnit4::class)
class Recorridos {

    private val app = AppBajoPrueba()
    private val argumentos = InstrumentationRegistry.getArguments()

    /** "Perf 1" o "Perf 2" según la grabación (`-e grabacion 1` o `2`): el script borra lo que lo lleve. */
    private val marca get() = "Perf ${argumentos.getString("grabacion") ?: "1"}"

    /** Una acción y luego la espera del plan, en segundos. */
    private fun paso(numero: Int, accion: String, espera: Double, bloque: () -> Unit) {
        app.marcarPaso(numero, accion)
        bloque()
        app.esperar(espera)
    }

    /** La app ya está abierta en el ingreso (la abre el script): 5 s quieta antes del primer paso. */
    private fun reposoInicial() {
        app.llevarAlIngreso()
        app.marcarPaso(0, "Reposo inicial en el ingreso")
        app.esperar(CINCO)
    }

    private fun entrarComoVisitante() {
        app.tocar("entrar_visitante")
        app.esperarA("inicio")
    }

    private fun entrarComoManoloBellon() {
        app.tocar("entrar_coleccionista")
        app.conReintento { app.esperarA("coleccionista_$COLECCIONISTA").click() }
        app.esperarA("inicio")
    }

    private fun abrirAlbum(id: Int) {
        app.conReintento {
            (app.buscarDesplazando(app.tag("album_$id"), maximo = 6) ?: app.esperarA("album_$id")).click()
        }
        app.esperarA("atras")
    }

    private fun abrirArtista(tag: String) {
        app.conReintento { app.esperarA(tag).click() }
        app.esperarA("detalle_artista_nombre")
    }

    // ---------- HU01 · Catálogo de álbumes (plan 5.1) ----------

    @Test
    fun hu01() {
        reposoInicial()
        paso(1, "Abrir la app y entrar al catálogo (Visitante > Entrar > Ver álbumes)", CINCO) {
            entrarComoVisitante()
            app.abrirCatalogo()
        }
        paso(2, "Desplazarse hasta la mitad de la lista", TRES) { app.desplazarAbajo() }
        paso(3, "Desplazarse hasta el final y regresar al inicio", CINCO) {
            app.desplazarHastaElFinal()
            // Sin arrastrar más allá del tope: eso dispararía «deslizar para actualizar» y otra consulta.
            app.subirHastaVer(app.tag("catalogo_contador"))
        }
    }

    // ---------- HU02 · Detalle de álbum (plan 5.2) ----------

    @Test
    fun hu02() {
        reposoInicial()
        paso(1, "Visitante > Entrar > Ver álbumes", TRES) {
            entrarComoVisitante()
            app.abrirCatalogo()
        }
        paso(2, "Seleccionar Buscando América (artistas, canciones y comentarios)", CINCO) { abrirAlbum(ALBUM) }
        paso(3, "Desplazarse por el contenido", TRES) { app.desplazarHastaElFinal() }
        paso(4, "Volver al catálogo y abrir el mismo álbum", CINCO) {
            app.atras()
            app.esperarA("catalogo_contador")
            abrirAlbum(ALBUM)
        }
    }

    // ---------- HU03 · Listado y búsqueda de artistas (plan 5.3) ----------

    @Test
    fun hu03() {
        reposoInicial()
        paso(1, "Entrar al listado (Visitante > Entrar > Ver artistas)", CINCO) {
            entrarComoVisitante()
            app.abrirArtistas()
        }
        paso(2, "Escribir Juan en el buscador", TRES) {
            app.tocar(BUSCADOR)
            app.escribirLetraPorLetra(BUSCADOR, "Juan")
        }
        paso(3, "Borrar el texto y escribir Rock", TRES) {
            app.borrarLetraPorLetra(BUSCADOR, "Juan")
            app.escribirLetraPorLetra(BUSCADOR, "Rock")
        }
        paso(4, "Borrar nuevamente y desplazarse por la lista", TRES) {
            app.borrarLetraPorLetra(BUSCADOR, "Rock")
            app.cerrarTeclado()
            app.esperarA("artistas_lista")
            app.desplazarHastaElFinal()
        }
    }

    // ---------- HU04 · Detalle de artista (plan 5.4) ----------

    @Test
    fun hu04() {
        reposoInicial()
        paso(1, "Visitante > Entrar > Ver artistas", TRES) {
            entrarComoVisitante()
            app.abrirArtistas()
        }
        paso(2, "Abrir Rubén Blades Bellido de Luna (artista con discografía)", CINCO) {
            abrirArtista("artista_musico_$MUSICO")
        }
        paso(3, "Desplazarse por biografía y estadísticas", TRES) {
            checkNotNull(app.buscarDesplazando(app.tag("detalle_artista_album_$ALBUM"))) {
                "La discografía de Rubén Blades no muestra Buscando América"
            }
        }
        paso(4, "Abrir Buscando América desde la discografía", CINCO) {
            app.conReintento { app.esperarA("detalle_artista_album_$ALBUM").click() }
            app.esperarA("atras")
        }
        paso(5, "Regresar al artista", TRES) {
            app.atras()
            app.esperarA("detalle_artista_nombre")
        }
        paso(6, "Volver al listado", CINCO) {
            app.atras()
            app.esperarA("artistas_lista")
        }
    }

    // ---------- HU07 · Crear un álbum (plan 5.5) ----------

    /** El nombre lleva la grabación ("Perf 1" o "Perf 2"). Los campos se escriben letra por letra. */
    @Test
    fun hu07() {
        reposoInicial()
        paso(1, "Ingresar como Coleccionista (Manolo Bellon) y abrir la creación de álbum", TRES) {
            entrarComoManoloBellon()
            app.abrirCatalogo()
            app.tocar("catalogo_crear_album")
            app.esperarA("crear_formulario")
        }
        paso(2, "Diligenciar campos válidos", DOS) {
            escribirEnCampo("crear_nombre", marca)
            escribirEnCampo("crear_portada", PORTADA)
            elegirPrimerDiaDelMes()
            elegirOpcion("crear_genero", "Rock")
            elegirOpcion("crear_disquera", "EMI")
            escribirEnCampo("crear_descripcion", DESCRIPCION)
        }
        paso(3, "Enviar y observar hasta la respuesta (máximo 10 s)", 0.0) {
            app.conReintento { campo("crear_enviar").click() }
            // El catálogo se desplaza hasta el álbum nuevo: se espera su tarjeta, no el contador.
            app.esperarA(app.tagQueCumple("album_nuevo_\\d+"), RESPUESTA)
        }
        paso(4, "Verificar el resultado: el álbum nuevo en el catálogo", CINCO) {
            app.esperarA(By.textContains(marca), 5_000)
        }
    }

    /** Busca el campo desplazando el formulario si hace falta. */
    private fun campo(tag: String) =
        app.buscarDesplazando(app.tag(tag), maximo = 6) ?: error("No se encontró el campo $tag en el formulario")

    private fun escribirEnCampo(tag: String, texto: String) {
        app.conReintento { campo(tag).click() }
        app.escribirLetraPorLetra(tag, texto)
        app.cerrarTeclado()
    }

    private fun elegirOpcion(tag: String, opcion: String) {
        app.conReintento { campo(tag).click() }
        app.conReintento { app.esperarA(By.text(opcion), 5_000).click() }
        app.dispositivo.waitForIdle(1_500)
    }

    /** Abre el selector de fecha, toca el día 1 del mes que muestra y acepta. */
    private fun elegirPrimerDiaDelMes() {
        app.conReintento { campo("crear_fecha").click() }
        app.esperarA(By.text("Aceptar"), 5_000)
        // Cada día se expone con su fecha completa ("domingo, 1 de noviembre de 2026").
        app.conReintento {
            val dia = app.dispositivo.findObject(By.textContains(", 1 de "))
                ?: app.dispositivo.findObject(By.descContains(", 1 de "))
                ?: error("No se encontró el día 1 en el selector de fecha")
            dia.click()
        }
        app.conReintento { app.esperarA(By.text("Aceptar"), 5_000).click() }
        app.dispositivo.waitForIdle(1_500)
    }

    // ---------- HU08 · Asociar canciones a un álbum (guía manual) ----------

    /** Agrega a Poeta del pueblo la canción "Perf" de 5:05; el script la borra antes y después. */
    @Test
    fun hu08() {
        reposoInicial()
        paso(1, "Coleccionista > Entrar > Manolo Bellon", CINCO) { entrarComoManoloBellon() }
        paso(2, "Ver álbumes", CINCO) { app.abrirCatalogo() }
        paso(3, "Abrir Poeta del pueblo", CINCO) { abrirAlbum(ALBUM_PRUEBA) }
        paso(4, "Agregar canción", CINCO) {
            app.tocar("detalle_agregar_cancion")
            app.esperarA("agregar_cancion_formulario")
        }
        paso(5, "Escribir el nombre; Guardar sigue deshabilitado", CINCO) {
            escribirEnCampo("agregar_cancion_nombre", NOMBRE_PRUEBA)
            check(!campo("agregar_cancion_guardar").isEnabled) { "Guardar se habilitó solo con el nombre" }
        }
        paso(6, "Escribir la duración; Guardar se habilita", CINCO) {
            escribirEnCampo("agregar_cancion_duracion", DURACION)
            check(campo("agregar_cancion_guardar").isEnabled) { "Guardar no se habilitó con nombre y duración" }
        }
        paso(7, "Tocar Guardar una sola vez; vuelve al detalle con el aviso Canción agregada (hasta 10 s)", CINCO) {
            app.conReintento { campo("agregar_cancion_guardar").click() }
            app.esperarA(By.text("Canción agregada"), RESPUESTA)
        }
    }

    // ---------- HU09 · Comentar un álbum (guía manual) ----------

    /**
     * Comenta A Day at the Races (sin comentarios) con 4 estrellas; el script borra el comentario
     * antes y después. El segundo toque en Publicar mientras publica no debe generar otro POST.
     */
    @Test
    fun hu09() {
        reposoInicial()
        paso(1, "Coleccionista > Entrar > Manolo Bellon", CINCO) { entrarComoManoloBellon() }
        paso(2, "Ver álbumes", CINCO) { app.abrirCatalogo() }
        paso(3, "Abrir A Day at the Races (sin comentarios)", CINCO) { abrirAlbum(ALBUM_COMENTAR) }
        paso(4, "Comentar álbum", CINCO) {
            app.tocar("detalle_comentar")
            app.esperarA("comentar_pantalla")
        }
        paso(5, "Tocar la cuarta estrella (Bueno (4/5))", CINCO) {
            app.tocar("comentar_estrella_$ESTRELLAS")
            app.esperarA("comentar_nivel", 5_000)
        }
        paso(6, "Escribir la opinión y cerrar el teclado; Publicar comentario se habilita", CINCO) {
            escribirEnCampo("comentar_opinion", OPINION)
            check(campo("comentar_publicar").isEnabled) { "Publicar comentario no se habilitó" }
        }
        paso(7, "Publicar una sola vez y tocar otra vez mientras dice Publicando...; sale ¡Comentario publicado exitosamente! (hasta 10 s)", CINCO) {
            app.conReintento { campo("comentar_publicar").click() }
            // Segundo toque inmediato, con el botón en "Publicando...": no debe enviar otro comentario.
            app.dispositivo.findObject(app.tag("comentar_publicar"))?.let { boton -> runCatching { boton.click() } }
            app.esperarA("comentar_publicado", RESPUESTA)
        }
        paso(8, "Aceptar; vuelve al detalle con el comentario", CINCO) {
            app.tocar("comentar_aceptar")
            app.esperarA("detalle_comentar")
        }
    }

    // ---------- HU10 · Agregar artistas a favoritos (plan 5.8) ----------

    /**
     * Queen no debe ser favorito al empezar: el script lo quita en la base local antes. El paso
     * "Quitar favorito" del plan no se hace: la app no permite quitar favoritos.
     */
    @Test
    fun hu10() {
        reposoInicial()
        paso(1, "Coleccionista (Manolo Bellon) > Ver artistas", TRES) {
            entrarComoManoloBellon()
            app.abrirArtistas()
        }
        paso(2, "Abrir Queen (artista no favorito)", TRES) {
            abrirArtista("artista_banda_$BANDA")
            botonFavorito()
        }
        paso(3, "Presionar Agregar Favorito una vez", TRES) {
            app.conReintento {
                checkNotNull(app.dispositivo.findObject(By.text("Agregar Favorito"))) {
                    "Queen ya es favorito: quita los favoritos del coleccionista 100 antes de grabar"
                }.click()
            }
        }
        paso(4, "Verificar el estado (el botón muestra Favorito)", TRES) {
            app.esperarA(By.text("Favorito"), RESPUESTA)
        }
        paso(5, "Salir y volver a abrir el artista", TRES) {
            app.atras()
            app.esperarA("artistas_lista")
            abrirArtista("artista_banda_$BANDA")
            botonFavorito()
        }
        paso(6, "Comprobar que el estado persiste (Favorito)", CINCO) {
            app.esperarA(By.text("Favorito"), RESPUESTA)
        }
    }

    /** El botón está debajo de la cabecera del artista: se baja hasta verlo si hace falta. */
    private fun botonFavorito() =
        app.buscarDesplazando(app.tag("detalle_artista_boton_favorito"), maximo = 6)
            ?: error("No aparece el botón de favorito: ¿se ingresó como coleccionista?")

    // ---------- HU15 · Asociar un álbum a un artista (guía manual) ----------

    /**
     * Asocia A Night at the Opera (102) a Rubén Blades (un músico: en una banda la app reemplaza toda
     * su lista de álbumes); el script quita la asociación antes y después. La ventana de asociar es una
     * hoja inferior (otra ventana), donde los `testTag` no se exponen: dentro de ella se busca por texto
     * y por tipo de campo.
     */
    @Test
    fun hu15() {
        reposoInicial()
        paso(1, "Coleccionista > Entrar > Manolo Bellon", CINCO) { entrarComoManoloBellon() }
        paso(2, "Ver artistas", CINCO) { app.abrirArtistas() }
        paso(3, "Abrir Rubén Blades Bellido de Luna", CINCO) { abrirArtista("artista_musico_$MUSICO") }
        paso(4, "Bajar hasta los álbumes y tocar Agregar álbum", CINCO) {
            app.tocar("asociar_boton_agregar")
            app.esperarA(VENTANA_ASOCIAR)
        }
        paso(5, "En Buscar álbum por título..., escribir Night", CINCO) {
            app.conReintento { app.esperarA(BUSQUEDA_EN_VENTANA).click() }
            app.escribirLetraPorLetra(BUSQUEDA_EN_VENTANA, BUSQUEDA_ASOCIAR)
            app.cerrarTeclado()
            app.esperarA(By.text(NOMBRE_ALBUM_ASOCIAR), 5_000)
        }
        paso(6, "Tocar A Night at the Opera; Asociar se habilita", CINCO) {
            app.conReintento { app.esperarA(By.text(NOMBRE_ALBUM_ASOCIAR)).click() }
        }
        paso(7, "Tocar Asociar una sola vez; la ventana se cierra con el aviso Álbum asociado (hasta 10 s)", CINCO) {
            app.conReintento { app.esperarA(By.text("Asociar")).click() }
            check(app.dispositivo.wait(Until.gone(VENTANA_ASOCIAR), RESPUESTA)) {
                "La ventana de asociar no se cerró en ${RESPUESTA / 1000} s: ¿el backend respondió?"
            }
            app.esperarA(By.text("Álbum asociado"), RESPUESTA)
        }
        paso(8, "Bajar hasta los álbumes; aparece A Night at the Opera", CINCO) {
            checkNotNull(app.buscarDesplazando(app.tag("detalle_artista_album_$ALBUM_ASOCIAR"), maximo = 8)) {
                "A Night at the Opera no aparece en la discografía de Rubén Blades"
            }
        }
    }

    private companion object {
        const val CINCO = 5.0
        const val TRES = 3.0
        const val DOS = 2.0
        const val RESPUESTA = 10_000L
        const val BUSCADOR = "artistas_buscador"
        const val ALBUM = 100
        const val ALBUM_PRUEBA = 101
        const val ALBUM_ASOCIAR = 102
        const val ALBUM_COMENTAR = 103
        const val NOMBRE_PRUEBA = "Perf"
        const val OPINION = "Perf: comentario del recorrido de perfilamiento."
        const val BUSQUEDA_ASOCIAR = "Night"
        const val NOMBRE_ALBUM_ASOCIAR = "A Night at the Opera"
        val VENTANA_ASOCIAR: BySelector = By.textStartsWith("Asociar álbum a ")
        val BUSQUEDA_EN_VENTANA: BySelector = By.clazz("android.widget.EditText")
        const val MUSICO = 100
        const val BANDA = 101
        const val COLECCIONISTA = 100
        const val ESTRELLAS = 4
        const val DURACION = "5:05"
        const val PORTADA = "https://upload.wikimedia.org/wikipedia/en/4/42/Beatles_-_Abbey_Road.jpg"
        const val DESCRIPCION = "Album creado por el recorrido de perfilamiento."
    }
}
