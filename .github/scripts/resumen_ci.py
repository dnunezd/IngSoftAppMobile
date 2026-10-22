"""Resumen de calidad (pruebas y lint) en Markdown para el resumen de la ejecución de CI."""
import glob
import sys
import xml.etree.ElementTree as ET

RAIZ = "app/build"


def pruebas():
    total = fallos = omitidas = 0
    for ruta in glob.glob(f"{RAIZ}/test-results/testDebugUnitTest/*.xml"):
        suite = ET.parse(ruta).getroot()
        total += int(suite.get("tests", 0))
        fallos += int(suite.get("failures", 0)) + int(suite.get("errors", 0))
        omitidas += int(suite.get("skipped", 0))
    return total, fallos, omitidas


def lint():
    try:
        raiz = ET.parse(f"{RAIZ}/reports/lint-results-debug.xml").getroot()
    except (OSError, ET.ParseError):
        return None
    severidades = [i.get("severity") for i in raiz.findall("issue")]
    return severidades.count("Error") + severidades.count("Fatal"), severidades.count("Warning")


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    print("## Resumen de calidad\n")
    total, fallos, omitidas = pruebas()
    estado = "✅" if total and not fallos else "❌"
    print(f"{estado} **Pruebas unitarias:** {total} ejecutadas, {fallos} con fallo, {omitidas} omitidas\n")

    resultado = lint()
    if resultado:
        errores, avisos = resultado
        print(f"{'✅' if not errores else '❌'} **Lint:** {errores} errores, {avisos} avisos\n")
    else:
        print("❌ **Lint:** sin informe\n")


if __name__ == "__main__":
    main()
