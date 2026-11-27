"""Recorre la app instalada y guarda una captura por pantalla, con el mismo recorrido en todas las versiones.

Uso: python capturar.py <serial> <apk> <version> <carpeta_salida>
  <version>: 1 (v1.1.0), 2 (v2.2.0) o 3 (develop). Las pantallas que no existían en una versión se omiten.

Necesita BackVynils local con `adb reverse tcp:3000 tcp:3000` y un APK compilado con
`-PbaseUrl=http://localhost:3000/`. Todas las versiones deben capturarse seguidas, contra el mismo estado del
backend, en el mismo dispositivo.
"""
import os
import sys

from pantalla import Pantalla


def main(serial, apk, version, salida):
    version = int(version)
    os.makedirs(salida, exist_ok=True)
    p = Pantalla(serial)
    p.preparar_dispositivo()
    p.instalar(apk)

    def foto(nombre, desde=1):
        if version >= desde:
            p.capturar(os.path.join(salida, nombre + ".png"))
            print("  ", nombre)

    # ---------- Ingreso y selector de coleccionistas (HU00) ----------
    p.abrir_app()
    p.esperar("Visitante")
    foto("01_ingreso")
    p.deslizar()
    foto("02_ingreso_tarjeta_coleccionista")
    p.tocar_debajo_de("Coleccionista", "Entrar")
    p.esperar("Manolo Bellon")
    foto("03_selector_coleccionistas")
    p.tocar("Buscar coleccionista", exacto=False)
    p.escribir("rtvc")
    p.ocultar_teclado()
    foto("04_selector_busqueda")
    p.tocar("Jaime Monsalve")

    # ---------- Inicio, catálogo y detalle del álbum (HU00, HU01, HU02, HU07) ----------
    p.esperar("Ver álbumes", exacto=False)
    foto("05_inicio_coleccionista")
    p.tocar("Ver álbumes", exacto=False)
    p.esperar("Buscando América")
    foto("06_catalogo_coleccionista")
    if version >= 2:
        p.tocar("Crear álbum", exacto=False)
        p.esperar("Nombre del álbum", exacto=False)
        foto("07_crear_album_formulario", desde=2)
        p.atras()
        p.esperar("Buscando América")
    p.tocar("Buscando América")
    p.esperar("Estreno")
    foto("08_detalle_album")
    p.deslizar()
    foto("09_detalle_album_canciones")
    p.atras()
    p.esperar("Buscando América")
    p.atras()

    # ---------- Artistas (HU03, HU04, HU10) ----------
    p.esperar("Ver álbumes", exacto=False)
    p.deslizar_hasta("Ver artistas", exacto=False)
    foto("10_inicio_tarjeta_artistas")
    p.tocar("Ver artistas", exacto=False)
    p.esperar("Queen")
    foto("11_artistas")
    p.tocar("Buscar artista", exacto=False)
    p.escribir("blades")
    p.ocultar_teclado()
    foto("12_artistas_busqueda")
    p.tocar("Rubén Blades Bellido de Luna")
    p.esperar_quieta(2.5)
    foto("13_detalle_artista")
    if version >= 2:
        p.deslizar()
        foto("14_detalle_artista_albumes", desde=2)

    # ---------- Visitante (HU00, HU01) ----------
    p.abrir_app()
    p.tocar_debajo_de("Visitante", "Entrar")
    p.esperar("Ver álbumes", exacto=False)
    foto("15_inicio_visitante")
    p.tocar("Ver álbumes", exacto=False)
    p.esperar("Buscando América")
    foto("16_catalogo_visitante")


if __name__ == "__main__":
    main(*sys.argv[1:5])
