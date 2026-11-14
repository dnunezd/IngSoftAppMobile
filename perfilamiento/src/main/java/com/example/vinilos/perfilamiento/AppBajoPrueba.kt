package com.example.vinilos.perfilamiento

import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/**
 * Maneja la app "perf" desde fuera de su proceso. Los elementos se buscan por su `testTag`, que la
 * app expone como resource-id (`testTagsAsResourceId` en `MainActivity`).
 *
 * No reinicia la app ni borra sus datos: eso lo decide el script orquestador, porque matar el proceso
 * cortaría la sesión del Profiler de Android Studio.
 */
class AppBajoPrueba {

    private val argumentos = InstrumentationRegistry.getArguments()
    val paquete: String = argumentos.getString("paquete") ?: "com.example.vinilos.perf"
    val dispositivo: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val ancho get() = dispositivo.displayWidth
    private val alto get() = dispositivo.displayHeight

    // ---------- Esperas ----------

    /** Pausa fija del plan de pruebas (los "esperar 5 s" de cada escenario). */
    fun esperar(segundos: Double) = SystemClock.sleep((segundos * 1000).toLong())

    fun tag(tag: String): BySelector = By.res(tag)

    fun tagQueCumple(expresion: String): BySelector = By.res(Pattern.compile(expresion))

    fun existe(selector: BySelector): Boolean = dispositivo.hasObject(selector)

    fun esperarA(selector: BySelector, milis: Long = 20_000): UiObject2 =
        dispositivo.wait(Until.findObject(selector), milis)
            ?: error("No apareció $selector en ${milis / 1000} s. ¿La app está en la pantalla esperada y el backend responde?")

    fun esperarA(tag: String, milis: Long = 20_000): UiObject2 = esperarA(tag(tag), milis)

    /** Toca el elemento; si no está a la vista, lo busca desplazando la pantalla hacia abajo. */
    fun tocar(tag: String, milis: Long = 20_000) = conReintento {
        val elemento = dispositivo.wait(Until.findObject(tag(tag)), 3_000)
            ?: buscarDesplazando(tag(tag), maximo = 6)
            ?: esperarA(tag, milis)
        elemento.click()
    }

    /**
     * Busca un elemento y actúa sobre él. Si la pantalla se redibuja entre encontrarlo y usarlo
     * (carga de datos o de imágenes), el elemento deja de existir (StaleObjectException): se vuelve
     * a buscar en vez de fallar el recorrido.
     */
    fun <T> conReintento(intentos: Int = 4, bloque: () -> T): T {
        repeat(intentos - 1) {
            try {
                return bloque()
            } catch (e: StaleObjectException) {
                SystemClock.sleep(250)
            }
        }
        return bloque()
    }

    // ---------- Arranque y navegación ----------

    /** Trae la app al frente (sin reiniciarla si ya corre) y la deja en la pantalla de ingreso. */
    fun llevarAlIngreso() {
        dispositivo.wakeUp()
        if (dispositivo.currentPackageName != paquete) {
            dispositivo.executeShellCommand("am start -n $paquete/com.example.vinilos.MainActivity")
            dispositivo.wait(Until.hasObject(By.pkg(paquete).depth(0)), 15_000)
        }
        repeat(8) {
            if (dispositivo.wait(Until.hasObject(tag("entrar_visitante")), 2_000)) return
            if (dispositivo.currentPackageName != paquete) {
                // Un "atrás" de más cerró la app: se vuelve a abrir, ya en el ingreso.
                dispositivo.executeShellCommand("am start -n $paquete/com.example.vinilos.MainActivity")
            } else {
                dispositivo.pressBack()
            }
        }
        esperarA("entrar_visitante")
    }

    fun entrarComoVisitante() {
        llevarAlIngreso()
        tocar("entrar_visitante")
        esperarA("inicio")
    }

    /** Ingresa con el primer coleccionista de la lista (en los datos de ejemplo, el 100). */
    fun entrarComoColeccionista() {
        llevarAlIngreso()
        tocar("entrar_coleccionista")
        conReintento { esperarA(tagQueCumple("coleccionista_\\d+")).click() }
        esperarA("inicio")
    }

    fun abrirCatalogo() {
        tocar("ver_albumes")
        esperarA("catalogo_contador")
    }

    fun abrirArtistas() {
        tocar("ver_artistas")
        esperarA("artistas_lista")
    }

    /** El botón "atrás" de la barra de la app; si la pantalla no lo tiene, el del sistema. */
    fun atras() {
        conReintento {
            val boton = dispositivo.findObject(tag("atras"))
            if (boton != null) boton.click() else dispositivo.pressBack()
        }
        dispositivo.waitForIdle(2_000)
    }

    // ---------- Gestos ----------

    /** Desplaza el contenido [veces] pantallas parciales hacia abajo (dedo de abajo hacia arriba). */
    fun desplazarAbajo(veces: Int = 1) = repeat(veces) {
        dispositivo.swipe(ancho / 2, (alto * 0.72).toInt(), ancho / 2, (alto * 0.32).toInt(), 28)
        SystemClock.sleep(350)
    }

    fun desplazarArriba(veces: Int = 1) = repeat(veces) {
        dispositivo.swipe(ancho / 2, (alto * 0.32).toInt(), ancho / 2, (alto * 0.72).toInt(), 28)
        SystemClock.sleep(350)
    }

    /** Desplaza hasta el final: se detiene cuando dos pantallas seguidas ya no cambian. */
    fun desplazarHastaElFinal(maximo: Int = 25): Int {
        var pasos = 0
        var anterior = huella()
        while (pasos < maximo) {
            desplazarAbajo()
            pasos++
            val actual = huella()
            if (actual == anterior) break
            anterior = actual
        }
        return pasos
    }

    /** Desplaza hacia abajo hasta que aparezca [selector]; devuelve null si no aparece. */
    fun buscarDesplazando(selector: BySelector, maximo: Int = 15): UiObject2? {
        repeat(maximo) {
            dispositivo.findObject(selector)?.let { return it }
            desplazarAbajo()
        }
        return dispositivo.findObject(selector)
    }

    /** Resumen de lo visible, para saber si un desplazamiento movió algo. */
    private fun huella(): String {
        // Mientras cargan datos o imágenes la pantalla se redibuja y un elemento ya leído deja de
        // existir (StaleObjectException): se vuelve a leer en vez de fallar el recorrido.
        repeat(4) {
            try {
                return dispositivo.findObjects(By.pkg(paquete))
                    .joinToString("|") { "${it.resourceName}:${it.text}:${it.visibleBounds.top}" }
            } catch (e: StaleObjectException) {
                SystemClock.sleep(250)
            }
        }
        // Sigue cambiando: se devuelve un valor único para que el desplazamiento continúe.
        return "cambiando-${SystemClock.uptimeMillis()}"
    }

    // ---------- Texto ----------

    /** Escribe tecla por tecla, como una persona, para que se vea el costo de cada cambio de texto. */
    fun teclear(texto: String, pausaMilis: Long = 120) {
        val mapa = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        val eventos = mapa.getEvents(texto.toCharArray()) ?: error("No se puede teclear \"$texto\" con el teclado virtual")
        eventos.filter { it.action == KeyEvent.ACTION_DOWN }.forEach { evento ->
            dispositivo.pressKeyCode(evento.keyCode, evento.metaState)
            SystemClock.sleep(pausaMilis)
        }
    }

    fun borrarTecleando(caracteres: Int, pausaMilis: Long = 80) = repeat(caracteres) {
        dispositivo.pressKeyCode(KeyEvent.KEYCODE_DEL)
        SystemClock.sleep(pausaMilis)
    }

    /** Cierra el teclado si está abierto (el "atrás" con teclado abierto no navega). */
    fun cerrarTeclado() {
        val abierto = dispositivo.executeShellCommand("dumpsys input_method").contains("mInputShown=true")
        if (abierto) {
            dispositivo.pressBack()
            dispositivo.waitForIdle(1_500)
        }
    }
}
