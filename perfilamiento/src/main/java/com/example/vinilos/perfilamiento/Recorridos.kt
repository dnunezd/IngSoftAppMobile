package com.example.vinilos.perfilamiento

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorridos de la guía manual de perfilamiento (docs/performance/guia-manual.md), uno por HU, con
 * los mismos pasos: cada paso es una acción y luego esperar 5 s. Antes del paso 1 hay 5 s de reposo
 * con la app abierta en el ingreso, como línea base.
 *
 * Cada paso se avisa con [AppBajoPrueba.marcarPaso] para que el script anote el segundo de la
 * grabación en que ocurrió. Si un paso abre otra pantalla, los 5 s cuentan desde que esa pantalla
 * aparece.
 *
 * Los lanza `scripts/perfilamiento/Perfilar.ps1`, uno por grabación y con la app recién abierta:
 * `adb shell am instrument -w -r -e class com.example.vinilos.perfilamiento.Recorridos#hu01
 *  com.example.vinilos.perfilamiento/androidx.test.runner.AndroidJUnitRunner`
 *
 * Suponen BackVynils local con los datos de ejemplo (`sql/data.sql`): álbum 100 Buscando América,
 * músico 100 Rubén Blades Bellido de Luna, banda 101 Queen y coleccionista 100 Manolo Bellon.
 */
@RunWith(AndroidJUnit4::class)
class Recorridos {

    private val app = AppBajoPrueba()
    private val argumentos = InstrumentationRegistry.getArguments()

    /** Una acción y luego los 5 s de la guía. */
    private fun paso(numero: Int, accion: String, bloque: () -> Unit) {
        app.marcarPaso(numero, accion)
        bloque()
        app.esperar(PAUSA)
    }

    /** La app ya está abierta en el ingreso (la abre el script): 5 s quieta antes del primer paso. */
    private fun reposoInicial() {
        app.llevarAlIngreso()
        app.marcarPaso(0, "Reposo inicial en el ingreso")
        app.esperar(PAUSA)
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

    private fun abrirBuscandoAmerica() {
        app.conReintento { app.esperarA("album_$ALBUM").click() }
        app.esperarA("atras")
    }

    private fun abrirArtista(tag: String) {
        app.conReintento { app.esperarA(tag).click() }
        app.esperarA("detalle_artista_nombre")
    }

    // ---------- HU01 · Catálogo de álbumes ----------

    @Test
    fun hu01() {
        reposoInicial()
        paso(1, "Visitante > Entrar") { entrarComoVisitante() }
        paso(2, "Ver álbumes") { app.abrirCatalogo() }
        paso(3, "Bajar hasta el final de la lista") { app.desplazarHastaElFinal() }
        paso(4, "Subir hasta el inicio, sin pasar del tope") { app.subirHastaVer(app.tag("catalogo_contador")) }
    }

    // ---------- HU02 · Detalle de álbum ----------

    @Test
    fun hu02() {
        reposoInicial()
        paso(1, "Visitante > Entrar") { entrarComoVisitante() }
        paso(2, "Ver álbumes") { app.abrirCatalogo() }
        paso(3, "Abrir Buscando América") { abrirBuscandoAmerica() }
        paso(4, "Bajar hasta el final (canciones y comentarios)") { app.desplazarHastaElFinal() }
        paso(5, "Atrás, al catálogo") {
            app.atras()
            app.esperarA("catalogo_contador")
        }
        paso(6, "Abrir Buscando América otra vez") { abrirBuscandoAmerica() }
    }

    // ---------- HU03 · Listado y búsqueda de artistas ----------

    @Test
    fun hu03() {
        reposoInicial()
        paso(1, "Visitante > Entrar") { entrarComoVisitante() }
        paso(2, "Ver artistas") { app.abrirArtistas() }
        paso(3, "Tocar el buscador y escribir Queen") {
            app.tocar(BUSCADOR)
            app.escribirLetraPorLetra(BUSCADOR, "Queen")
        }
        paso(4, "Borrar y escribir Juan (sin coincidencias)") {
            app.borrarLetraPorLetra(BUSCADOR, "Queen")
            app.escribirLetraPorLetra(BUSCADOR, "Juan")
            app.esperarA(By.text("No se encontraron artistas"), 5_000)
        }
        paso(5, "Borrar el texto y cerrar el teclado") {
            app.borrarLetraPorLetra(BUSCADOR, "Juan")
            app.cerrarTeclado()
        }
    }

    // ---------- HU04 · Detalle de artista ----------

    @Test
    fun hu04() {
        reposoInicial()
        paso(1, "Visitante > Entrar") { entrarComoVisitante() }
        paso(2, "Ver artistas") { app.abrirArtistas() }
        paso(3, "Abrir Rubén Blades Bellido de Luna") { abrirArtista("artista_musico_$MUSICO") }
        paso(4, "Bajar hasta los álbumes de su discografía") {
            checkNotNull(app.buscarDesplazando(app.tag("detalle_artista_album_$ALBUM"))) {
                "La discografía de Rubén Blades no muestra Buscando América"
            }
        }
        paso(5, "Abrir Buscando América") {
            app.conReintento { app.esperarA("detalle_artista_album_$ALBUM").click() }
            app.esperarA("atras")
        }
        paso(6, "Atrás, al artista") {
            app.atras()
            app.esperarA("detalle_artista_nombre")
        }
        paso(7, "Atrás, al listado") {
            app.atras()
            app.esperarA("artistas_lista")
        }
    }

    // ---------- HU07 · Crear un álbum ----------

    /**
     * El nombre lleva el número de grabación (`-e grabacion 1` o `2`): "Perf 1" y "Perf 2", como en
     * la guía. Los campos de texto se escriben letra por letra, para medir lo que cuesta cada cambio.
     */
    @Test
    fun hu07() {
        val nombre = "Perf ${argumentos.getString("grabacion") ?: "1"}"
        reposoInicial()
        paso(1, "Coleccionista > Entrar > Manolo Bellon") { entrarComoManoloBellon() }
        paso(2, "Ver álbumes") { app.abrirCatalogo() }
        paso(3, "Crear álbum") {
            app.tocar("catalogo_crear_album")
            app.esperarA("crear_formulario")
        }
        paso(4, "Llenar el formulario") {
            escribirEnCampo("crear_nombre", nombre)
            escribirEnCampo("crear_portada", PORTADA)
            elegirPrimerDiaDelMes()
            elegirOpcion("crear_genero", "Rock")
            elegirOpcion("crear_disquera", "EMI")
            escribirEnCampo("crear_descripcion", DESCRIPCION)
        }
        paso(5, "Tocar Crear álbum (vuelve al catálogo)") {
            app.conReintento { campo("crear_enviar").click() }
            // El catálogo se desplaza hasta el álbum nuevo: se espera su tarjeta, no el contador.
            app.esperarA(app.tagQueCumple("album_nuevo_\\d+"), 15_000)
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

    // ---------- HU10 · Agregar artistas a favoritos ----------

    /** Queen no debe ser favorito al empezar: el script quita los favoritos por la API antes. */
    @Test
    fun hu10() {
        reposoInicial()
        paso(1, "Coleccionista > Entrar > Manolo Bellon") { entrarComoManoloBellon() }
        paso(2, "Ver artistas") { app.abrirArtistas() }
        paso(3, "Abrir Queen") {
            abrirArtista("artista_banda_$BANDA")
            botonFavorito()
        }
        paso(4, "Agregar Favorito (el botón cambia a Favorito)") {
            app.conReintento {
                checkNotNull(app.dispositivo.findObject(By.text("Agregar Favorito"))) {
                    "Queen ya es favorito: quita los favoritos del coleccionista 100 antes de grabar"
                }.click()
            }
            app.esperarA(By.text("Favorito"), 10_000)
        }
        paso(5, "Atrás, al listado") {
            app.atras()
            app.esperarA("artistas_lista")
        }
        paso(6, "Abrir Queen otra vez (sigue en Favorito)") {
            abrirArtista("artista_banda_$BANDA")
            botonFavorito()
            app.esperarA(By.text("Favorito"), 10_000)
        }
    }

    /** El botón está debajo de la cabecera del artista: se baja hasta verlo si hace falta. */
    private fun botonFavorito() =
        app.buscarDesplazando(app.tag("detalle_artista_boton_favorito"), maximo = 6)
            ?: error("No aparece el botón de favorito: ¿se ingresó como coleccionista?")

    private companion object {
        const val PAUSA = 5.0
        const val BUSCADOR = "artistas_buscador"
        const val ALBUM = 100
        const val MUSICO = 100
        const val BANDA = 101
        const val COLECCIONISTA = 100
        const val PORTADA = "https://upload.wikimedia.org/wikipedia/en/4/42/Beatles_-_Abbey_Road.jpg"
        const val DESCRIPCION = "Album creado por el recorrido de perfilamiento."
    }
}
