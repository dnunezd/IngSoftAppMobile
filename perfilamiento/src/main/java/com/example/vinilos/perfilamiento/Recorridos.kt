package com.example.vinilos.perfilamiento

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorridos del plan de pruebas de rendimiento (docs/performance/plan-pruebas-performance.md).
 * Cada método es un escenario con los mismos pasos y pausas en todos los dispositivos.
 *
 * Los lanza `scripts/perfilamiento/Perfilar.ps1`, uno por uno:
 * `adb shell am instrument -w -e class com.example.vinilos.perfilamiento.Recorridos#hu01
 *  com.example.vinilos.perfilamiento/androidx.test.runner.AndroidJUnitRunner`
 *
 * Suponen BackVynils local con los datos de ejemplo (`sql/data.sql`, ids desde 100).
 */
@RunWith(AndroidJUnit4::class)
class Recorridos {

    private val app = AppBajoPrueba()

    private val cualquierAlbum = app.tagQueCumple("album_\\d+")
    private val cualquierArtista = app.tagQueCumple("artista_(musico|banda)_\\d+")

    /** El álbum 100 tiene artistas, canciones y comentarios; si no está, el primero visible. */
    private fun abrirAlbumCompleto() {
        (app.dispositivo.findObject(app.tag("album_100")) ?: app.esperarA(cualquierAlbum)).click()
        app.esperarA("atras")
    }

    /** El músico 100 tiene discografía; si no está, el primer artista visible. */
    private fun abrirArtistaConDiscografia() {
        (app.dispositivo.findObject(app.tag("artista_musico_100")) ?: app.esperarA(cualquierArtista)).click()
        app.esperarA("detalle_artista_nombre")
    }

    // ---------- Comprobación previa (no hace parte del plan) ----------

    /** Abre la app y llega al inicio como visitante: confirma que los recorridos pueden manejarla. */
    @Test
    fun comprobarManejo() {
        app.entrarComoVisitante()
        assertTrue("No se ve el acceso al catálogo", app.existe(app.tag("ver_albumes")))
        assertTrue("No se ve el acceso a los artistas", app.buscarDesplazando(app.tag("ver_artistas"), maximo = 6) != null)
        app.llevarAlIngreso()
    }

    // ---------- §05 HU01: consultar catálogo de álbumes ----------

    @Test
    fun hu01() {
        app.entrarComoVisitante()
        app.abrirCatalogo()                       // 1. Abrir la app y entrar al catálogo
        app.esperar(5.0)                          // 2. Esperar 5 s
        val hastaElFinal = medirLargoDelCatalogo()
        app.esperar(3.0)                          // 3. Mitad de la lista; esperar 3 s
        app.desplazarAbajo(hastaElFinal - hastaElFinal / 2)   // 4. Hasta el final...
        app.desplazarArriba(hastaElFinal + 1)     //    ...y regresar al inicio
        app.esperar(5.0)                          // 5. Esperar 5 s
    }

    /** Baja hasta el final para contar los pasos, vuelve arriba y deja la lista en la mitad. */
    private fun medirLargoDelCatalogo(): Int {
        val pasos = app.desplazarHastaElFinal()
        app.desplazarArriba(pasos + 1)
        app.desplazarAbajo(pasos / 2)
        return pasos
    }

    // ---------- §06 HU02: consultar detalle de álbum ----------

    @Test
    fun hu02() {
        app.entrarComoVisitante()
        app.abrirCatalogo()
        abrirAlbumCompleto()                      // 1. Álbum con artistas, canciones y comentarios
        app.esperar(5.0)                          // 2. Esperar 5 s
        app.desplazarHastaElFinal()               // 3. Desplazarse por el contenido
        app.esperar(3.0)
        app.atras()                               // 4. Volver al catálogo y abrir el mismo álbum
        app.esperarA("catalogo_contador")
        abrirAlbumCompleto()
        app.esperar(5.0)                          // 5. Esperar 5 s
    }

    // ---------- §07 HU03: listado y búsqueda de artistas ----------

    @Test
    fun hu03() {
        app.entrarComoVisitante()
        app.abrirArtistas()                       // 1. Entrar al listado y esperar 5 s
        app.esperar(5.0)
        app.tocar("artistas_buscador")
        app.teclear("Juan")                       // 2. Escribir "Juan"; esperar 3 s
        app.esperar(3.0)
        app.borrarTecleando(4)                    // 3. Borrar y escribir "Rock"; esperar 3 s
        app.teclear("Rock")
        app.esperar(3.0)
        app.borrarTecleando(4)                    // 4. Borrar y desplazarse por la lista
        app.cerrarTeclado()
        val pasos = app.desplazarHastaElFinal()
        app.desplazarArriba(pasos + 1)
        app.esperar(3.0)                          // 5. Esperar 3 s
    }

    // ---------- §08 HU04: detalle de artista ----------

    @Test
    fun hu04() {
        app.entrarComoVisitante()
        app.abrirArtistas()
        abrirArtistaConDiscografia()              // 1. Abrir un artista con discografía
        app.esperar(5.0)                          // 2. Esperar 5 s
        app.desplazarAbajo(2)                     // 3. Biografía y estadísticas
        val album = app.buscarDesplazando(app.tagQueCumple("detalle_artista_album_\\d+"))
        checkNotNull(album) { "El artista abierto no muestra álbumes en su discografía" }
        album.click()                             // 4. Abrir un álbum de la discografía; esperar 5 s
        app.esperar(5.0)
        app.atras()                               // 5. Regresar al artista; esperar 3 s
        app.esperarA("detalle_artista_nombre")
        app.esperar(3.0)
        app.atras()                               // 6. Volver al listado
        app.esperarA("artistas_lista")
    }

    // ---------- §09 HU07: crear un álbum ----------

    @Test
    fun hu07() {
        app.entrarComoColeccionista()             // 1. Coleccionista -> creación de álbum
        app.abrirCatalogo()
        app.tocar("catalogo_crear_album")
        app.esperarA("crear_formulario")
        app.esperar(3.0)                          // 2. Esperar 3 s

        // 3. Campos válidos; el nombre lleva la hora para no duplicar álbumes.
        escribirEnCampo("crear_nombre", "Perf ${System.currentTimeMillis()}")
        escribirEnCampo("crear_portada", "https://upload.wikimedia.org/wikipedia/en/4/42/Beatles_-_Abbey_Road.jpg")
        elegirPrimerDiaDelMes()
        elegirOpcion("crear_genero", "Rock")
        elegirOpcion("crear_disquera", "EMI")
        escribirEnCampo("crear_descripcion", "Album creado por el recorrido de perfilamiento.")
        app.esperar(2.0)                          //    Esperar 2 s antes de enviar

        campo("crear_enviar").click()             // 4. Enviar y observar la respuesta (máximo 10 s)
        app.esperarA(app.tag("catalogo_contador"), 10_000)   // 5. De vuelta en el catálogo: se creó
        app.esperar(2.0)
    }

    /** Busca el campo desplazando el formulario si hace falta. */
    private fun campo(tag: String) =
        app.buscarDesplazando(app.tag(tag), maximo = 6) ?: error("No se encontró el campo $tag en el formulario")

    private fun escribirEnCampo(tag: String, texto: String) {
        campo(tag).text = texto
        app.cerrarTeclado()
    }

    private fun elegirOpcion(tag: String, opcion: String) {
        campo(tag).click()
        app.esperarA(By.text(opcion), 5_000).click()
        app.dispositivo.waitForIdle(1_500)
    }

    /** Abre el selector de fecha, toca el día 1 del mes que muestra y acepta. */
    private fun elegirPrimerDiaDelMes() {
        campo("crear_fecha").click()
        app.esperarA(By.text("Aceptar"), 5_000)
        // Cada día se expone con su fecha completa ("domingo, 1 de noviembre de 2026").
        val dia = app.dispositivo.findObject(By.textContains(", 1 de "))
            ?: app.dispositivo.findObject(By.descContains(", 1 de "))
            ?: error("No se encontró el día 1 en el selector de fecha")
        dia.click()
        app.esperarA(By.text("Aceptar"), 5_000).click()
        app.dispositivo.waitForIdle(1_500)
    }

    // ---------- §10 HU10: agregar artistas a favoritos ----------

    /**
     * La app solo permite agregar favoritos (no quitarlos), así que los pasos 4 y 5 del plan
     * ("quitar" y "marcar nuevamente") no aplican: se agrega una vez y se comprueba que persiste.
     * Requiere un artista que aún no sea favorito: el script recrea el backend antes de este escenario.
     */
    @Test
    fun hu10() {
        app.entrarComoColeccionista()             // 1. Coleccionista -> artista no favorito
        app.abrirArtistas()
        val artista = app.esperarA(cualquierArtista)
        val tagDelArtista = artista.resourceName
        artista.click()
        app.esperarA("detalle_artista_nombre")
        app.esperar(3.0)                          // 2. Esperar 3 s y presionar favorito una vez
        botonFavorito().click()
        app.esperar(3.0)                          // 3. Esperar 3 s y verificar el estado
        app.atras()                               // 5. Salir y volver a abrir el artista
        app.esperarA("artistas_lista")
        app.esperarA(tagDelArtista).click()
        app.esperarA("detalle_artista_nombre")
        botonFavorito()                           // 6. El botón sigue ahí con el estado guardado
        app.esperar(3.0)
    }

    private fun botonFavorito() =
        app.buscarDesplazando(app.tag("detalle_artista_boton_favorito"), maximo = 6)
            ?: error("No aparece el botón de favorito: ¿se ingresó como coleccionista?")

    // ---------- §11 Caída y recuperación del backend (tres fases; el script apaga y enciende) ----------

    /** Fase 1, con el backend activo: cargar el catálogo. */
    @Test
    fun caidaFase1CargarCatalogo() {
        app.entrarComoVisitante()
        app.abrirCatalogo()
        app.esperar(3.0)
    }

    /** Fase 2, con el backend detenido: volver a entrar al catálogo y esperar la respuesta de la UI. */
    @Test
    fun caidaFase2ConsultarSinBackend() {
        // Desde el ingreso: el script puede haber borrado los datos de la app (variante sin caché).
        app.entrarComoVisitante()
        app.tocar("ver_albumes")
        // Con consultas guardadas la app muestra la lista; sin ellas, el error con "Reintentar".
        app.esperarA(app.tagQueCumple("catalogo_(error|contador)"), 30_000)
        app.dispositivo.findObject(app.tag("catalogo_reintentar"))?.click()
        app.esperar(12.0)
    }

    /** Fase 3, con el backend otra vez activo: reintentar y confirmar que se recupera. */
    @Test
    fun caidaFase3Recuperar() {
        val reintentar = app.dispositivo.findObject(app.tag("catalogo_reintentar"))
        if (reintentar != null) {
            reintentar.click()
        } else {
            app.entrarComoVisitante()
            app.tocar("ver_albumes")
        }
        app.esperar(5.0)
        app.esperarA("catalogo_contador")
    }

    // ---------- §12 Navegación y memoria ----------

    /** Tres vueltas catálogo -> álbum -> artistas -> artista. El Heap Dump lo toma el script. */
    @Test
    fun memoriaNavegacion() {
        app.entrarComoVisitante()
        repeat(3) {
            app.abrirCatalogo()                   // 1. Catálogo y detalle de álbum
            abrirAlbumCompleto()
            app.esperar(2.0)
            app.atras()                           // 2. Regresar; listado y detalle de artista
            app.esperarA("catalogo_contador")
            app.atras()
            app.esperarA("inicio")
            app.abrirArtistas()
            abrirArtistaConDiscografia()
            app.esperar(2.0)
            app.atras()                           // 3. Regresar al listado y repetir
            app.esperarA("artistas_lista")
            app.atras()
            app.esperarA("inicio")
        }
        app.esperar(5.0)                          // 4. Esperar 5 s al terminar
    }
}
