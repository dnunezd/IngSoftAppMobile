"""Corre las E2E de una versión, guarda el resultado de cada prueba y trae sus capturas de evidencia.

Uso: python correr_e2e.py <serial> <apk_app> <apk_pruebas> <runner> <carpeta_salida> <Clase1,Clase2,...>
  <runner>: `androidx.test.runner.AndroidJUnitRunner` (v1.1.0 y v2.2.0) o `com.example.vinilos.VinilosTestRunner`
  (desde develop). Las clases van sin el paquete `com.example.vinilos.`.

Cada versión corre su propia copia de las pruebas: las de una versión anterior no compilan contra el código de la
siguiente (por ejemplo, desde la v2.2.0 los repositorios son interfaces), pero los escenarios y los nombres de los
pasos con captura son los mismos. Deja en <carpeta_salida> una carpeta por escenario (`FN01_ESC03/02_catalogo.png`)
y `pruebas.csv` con el estado de cada prueba.
"""
import csv
import os
import re
import subprocess
import sys

from pantalla import ADB, PAQUETE

PAQUETE_PRUEBAS = PAQUETE + ".test"
EVIDENCIA = f"/sdcard/Android/data/{PAQUETE}/files/e2e-evidencia"
ESTADOS = {"0": "OK", "-1": "Error", "-2": "Falla", "-3": "Ignorada", "-4": "Omitida"}


def adb(serial, *args):
    return subprocess.run([ADB, "-s", serial, *args], capture_output=True, encoding="utf-8", errors="replace")


def main(serial, apk_app, apk_pruebas, runner, salida, clases):
    os.makedirs(salida, exist_ok=True)
    for paquete in (PAQUETE_PRUEBAS, PAQUETE):
        adb(serial, "uninstall", paquete)
    for apk in (apk_app, apk_pruebas):
        resultado = adb(serial, "install", "-r", "-g", apk)
        if resultado.returncode != 0:
            raise SystemExit(f"No se pudo instalar {apk}: {resultado.stdout}{resultado.stderr}")
    adb(serial, "reverse", "tcp:3000", "tcp:3000")
    adb(serial, "shell", f"rm -rf {EVIDENCIA}")

    nombres = ",".join(f"com.example.vinilos.{clase}" for clase in clases.split(","))
    salida_cruda = adb(serial, "shell", "am", "instrument", "-r", "-w", "-e", "class", nombres,
                       f"{PAQUETE_PRUEBAS}/{runner}").stdout
    with open(os.path.join(salida, "instrumentacion.txt"), "w", encoding="utf-8") as archivo:
        archivo.write(salida_cruda)

    # Formato -r: cada prueba emite un bloque de INSTRUMENTATION_STATUS seguido de su código (1 = inicio).
    pruebas, actual = [], {}
    for linea in salida_cruda.splitlines():
        m = re.match(r"INSTRUMENTATION_STATUS: (class|test)=(.*)", linea)
        if m:
            actual[m.group(1)] = m.group(2).strip()
        m = re.match(r"INSTRUMENTATION_STATUS_CODE: (-?\d+)", linea)
        if m:
            if m.group(1) != "1":
                pruebas.append([actual.get("class", "").rsplit(".", 1)[-1], actual.get("test", ""), ESTADOS.get(m.group(1), m.group(1))])
            actual = {}
    with open(os.path.join(salida, "pruebas.csv"), "w", newline="", encoding="utf-8") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow(["clase", "prueba", "estado"])
        escritor.writerows(pruebas)

    adb(serial, "pull", f"{EVIDENCIA}/.", salida)
    ok = sum(1 for p in pruebas if p[2] == "OK")
    print(f"{ok}/{len(pruebas)} OK; fallos: {[p[1] for p in pruebas if p[2] not in ('OK', 'Omitida')]}")


if __name__ == "__main__":
    main(*sys.argv[1:7])
