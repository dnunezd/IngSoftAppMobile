"""Resumen de calidad (pruebas, cobertura y lint) en Markdown para el resumen de la ejecución de CI."""
import glob
import sys
import xml.etree.ElementTree as ET

RAIZ = "app/build"
PAQUETES_LOGICA = ("data", "viewmodel")


def pruebas():
    total = fallos = omitidas = 0
    for ruta in glob.glob(f"{RAIZ}/test-results/testDebugUnitTest/*.xml"):
        suite = ET.parse(ruta).getroot()
        total += int(suite.get("tests", 0))
        fallos += int(suite.get("failures", 0)) + int(suite.get("errors", 0))
        omitidas += int(suite.get("skipped", 0))
    return total, fallos, omitidas


def sumar(elementos, tipo):
    cubiertas = perdidas = 0
    for elemento in elementos:
        for contador in elemento.findall("counter"):
            if contador.get("type") == tipo:
                cubiertas += int(contador.get("covered"))
                perdidas += int(contador.get("missed"))
    total = cubiertas + perdidas
    return f"{cubiertas / total * 100:.1f}% ({cubiertas}/{total})" if total else "-"


def cobertura():
    try:
        raiz = ET.parse(f"{RAIZ}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml").getroot()
    except (OSError, ET.ParseError):
        return None
    paquetes = raiz.findall("package")
    filas = []
    for base in PAQUETES_LOGICA:
        del_grupo = [p for p in paquetes if p.get("name").startswith(f"com/example/vinilos/{base}")]
        filas.append((base, sumar(del_grupo, "LINE"), sumar(del_grupo, "BRANCH")))
    return filas, sumar([raiz], "LINE")


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

    datos = cobertura()
    if datos:
        filas, global_ = datos
        print("**Cobertura de las pruebas unitarias** (mínimos exigidos en `data` y `viewmodel`: líneas 90 %, ramas 85 %)\n")
        print("| Paquete | Líneas | Ramas |\n|---|---|---|")
        for nombre, lineas, ramas in filas:
            print(f"| `{nombre}` | {lineas} | {ramas} |")
        print(f"\nTodo el código (incluye la UI de Compose, que se prueba con las instrumentadas): líneas {global_}.")
    else:
        print("❌ **Cobertura:** sin informe")


if __name__ == "__main__":
    main()
