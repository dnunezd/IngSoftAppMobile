"""Maneja la app desde afuera con adb, sin depender del código de cada versión.

Lee la jerarquía de la pantalla con `uiautomator dump` (Compose expone los textos a la accesibilidad), toca
por texto y guarda capturas con `screencap`. Así el mismo recorrido sirve para v1.1.0, v2.2.0 y develop.
"""
import os
import re
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET


def _ruta_adb():
    """adb del SDK de Android y, si no está, el del PATH.

    El SDK va primero porque en algunas máquinas el `adb` del PATH es otra versión que no es la del SDK. Se busca en
    ANDROID_HOME, en ANDROID_SDK_ROOT y en la ubicación por defecto de Android Studio en Windows, macOS y Linux.
    """
    ejecutable = "adb.exe" if os.name == "nt" else "adb"
    candidatos = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")]
    if os.environ.get("LOCALAPPDATA"):
        candidatos.append(os.path.join(os.environ["LOCALAPPDATA"], "Android", "Sdk"))
    candidatos += [os.path.expanduser("~/Library/Android/sdk"), os.path.expanduser("~/Android/Sdk")]
    for sdk in filter(None, candidatos):
        ruta = os.path.join(sdk, "platform-tools", ejecutable)
        if os.path.exists(ruta):
            return ruta
    return shutil.which("adb") or ejecutable


ADB = _ruta_adb()
PAQUETE = "com.example.vinilos"
ACTIVIDAD = f"{PAQUETE}/.MainActivity"


class Pantalla:
    def __init__(self, serial):
        self.serial = serial

    # ---------- adb ----------

    def adb(self, *args, binario=False):
        salida = subprocess.run([ADB, "-s", self.serial, *args], capture_output=True, check=True)
        return salida.stdout if binario else salida.stdout.decode("utf-8", "replace")

    def shell(self, comando):
        return self.adb("shell", comando)

    # ---------- preparación ----------

    def preparar_dispositivo(self):
        """Pantalla quieta y comparable entre versiones: sin animaciones, en vertical y con la barra de estado fija."""
        for clave in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
            self.shell(f"settings put global {clave} 0")
        self.shell("settings put system accelerometer_rotation 0")
        self.shell("settings put system user_rotation 0")
        self.shell("settings put global sysui_demo_allowed 1")
        for orden in (
            "enter",
            "clock -e hhmm 1200",
            "battery -e level 100 -e plugged false",
            "network -e wifi show -e level 4 -e mobile hide",
            "notifications -e visible false",
        ):
            partes = orden.split(" ", 1)
            extra = f" -e command {partes[0]}" + (f" {partes[1]}" if len(partes) > 1 else "")
            self.shell(f"am broadcast -a com.android.systemui.demo{extra}")

    def instalar(self, apk):
        self.adb("uninstall", PAQUETE) if PAQUETE in self.shell("pm list packages") else None
        self.adb("install", "-r", "-g", apk)
        # Desde develop la app también está en inglés y sigue el idioma del sistema; v1.1.0 y v2.2.0 solo
        # tienen español. Se fija el español solo para la app (Android 13 o superior), sin tocar el sistema.
        self.shell(f"cmd locale set-app-locales {PAQUETE} --locales es-CO")
        time.sleep(3)  # el cambio de idioma se aplica de forma asíncrona tras instalar

    def abrir_app(self):
        self.shell(f"am force-stop {PAQUETE}")
        self.shell(f"am start -W -n {ACTIVIDAD}")
        self.esperar_quieta()

    # ---------- lectura ----------

    def nodos(self):
        for _ in range(5):
            xml = self.shell("uiautomator dump --compressed /sdcard/ui.xml >/dev/null 2>&1; cat /sdcard/ui.xml")
            inicio = xml.find("<?xml")
            if inicio >= 0:
                try:
                    raiz = ET.fromstring(xml[inicio:].strip())
                    return list(raiz.iter("node"))
                except ET.ParseError:
                    pass
            time.sleep(0.5)
        return []

    @staticmethod
    def texto(nodo):
        return (nodo.get("text") or "") or (nodo.get("content-desc") or "")

    @staticmethod
    def limites(nodo):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", nodo.get("bounds")))
        return x1, y1, x2, y2

    def buscar(self, texto, exacto=True):
        resultado = []
        for nodo in self.nodos():
            valor = self.texto(nodo)
            if (valor == texto) if exacto else (texto.lower() in valor.lower()):
                resultado.append(nodo)
        return resultado

    def existe(self, texto, exacto=True):
        return bool(self.buscar(texto, exacto))

    def esperar(self, texto, exacto=True, segundos=20):
        limite = time.time() + segundos
        while time.time() < limite:
            if self.existe(texto, exacto):
                self.esperar_quieta()
                return True
            time.sleep(0.7)
        raise TimeoutError(f"No apareció '{texto}'")

    def esperar_quieta(self, segundos=1.5):
        """Las imágenes de red y las listas tardan un poco en asentarse."""
        time.sleep(segundos)

    # ---------- acciones ----------

    def tocar_nodo(self, nodo):
        x1, y1, x2, y2 = self.limites(nodo)
        self.shell(f"input tap {(x1 + x2) // 2} {(y1 + y2) // 2}")

    def tocar(self, texto, indice=0, exacto=True, esperar=True):
        if esperar:
            self.esperar(texto, exacto)
        candidatos = self.buscar(texto, exacto)
        if len(candidatos) <= indice:
            raise LookupError(f"No hay un '{texto}' número {indice + 1}")
        self.tocar_nodo(candidatos[indice])
        self.esperar_quieta()

    def tocar_debajo_de(self, ancla, texto):
        """Toca el primer [texto] que está debajo de [ancla] (p. ej. el "Entrar" de la tarjeta Visitante)."""
        self.esperar(ancla)
        y_ancla = self.limites(self.buscar(ancla)[0])[1]
        # Sin exactitud: desde develop el botón se anuncia como "Entrar como Visitante".
        candidatos = sorted(
            (n for n in self.buscar(texto, exacto=False) if self.limites(n)[1] > y_ancla), key=lambda n: self.limites(n)[1]
        )
        if not candidatos:
            raise LookupError(f"No hay '{texto}' debajo de '{ancla}'")
        self.tocar_nodo(candidatos[0])
        self.esperar_quieta()

    def deslizar_hasta(self, texto, exacto=True, intentos=12):
        for _ in range(intentos):
            if self.existe(texto, exacto):
                return
            self.deslizar()
        raise LookupError(f"No se llegó a '{texto}'")

    def deslizar(self, hacia_abajo=True):
        ancho, alto = self.tamano()
        x = ancho // 2
        desde, hasta = (int(alto * 0.75), int(alto * 0.35)) if hacia_abajo else (int(alto * 0.35), int(alto * 0.75))
        # Lento a propósito: un gesto rápido deja inercia y la lista se detiene en un punto distinto cada vez.
        self.shell(f"input swipe {x} {desde} {x} {hasta} 1500")
        self.esperar_quieta(1.0)

    def escribir(self, texto):
        self.shell("input text " + texto.replace(" ", "%s"))
        self.esperar_quieta(1.0)

    def ocultar_teclado(self):
        if "mInputShown=true" in self.shell("dumpsys input_method"):
            self.shell("input keyevent 111")  # ESC cierra el teclado sin salir de la pantalla
            self.esperar_quieta(1.0)

    def atras(self):
        self.shell("input keyevent 4")
        self.esperar_quieta()

    def tamano(self):
        m = re.search(r"(\d+)x(\d+)", self.shell("wm size"))
        return int(m.group(1)), int(m.group(2))

    # ---------- captura ----------

    def capturar(self, destino):
        self.esperar_quieta(1.0)
        with open(destino, "wb") as archivo:
            archivo.write(self.adb("exec-out", "screencap", "-p", binario=True))
