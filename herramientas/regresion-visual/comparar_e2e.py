"""Compara la evidencia de las E2E de dos versiones, escenario por escenario y paso por paso.

Uso: python comparar_e2e.py <corrida_base> <corrida_nueva> <carpeta_salida> <titulo_base> <titulo_nueva>
                             [ruido1.csv,ruido2.csv,...]

Cada corrida es una carpeta de `correr_e2e.py`: una subcarpeta por escenario con una captura por paso y
`pruebas.csv`. Solo se comparan los escenarios y pasos de la base, porque una regresión solo puede medirse sobre lo
que ya existía. Deja `resultado.csv` (un paso por fila), `pruebas.csv` (el estado de cada prueba en las dos
versiones) y, solo para los pasos que cambiaron, una imagen con la base, la nueva y las diferencias.

Los `resultado.csv` de ruido (una versión comparada consigo misma en dos corridas) marcan los pasos inestables: si un
paso cambia entre dos corridas de la misma versión (por ejemplo, la prueba captura mientras el catálogo aún carga o
el paso muestra un nombre con la hora), su diferencia entre versiones no dice nada y queda como "Inestable".
"""
import csv
import os
import sys

from comparar import comparar_carpetas


def estados(corrida):
    with open(os.path.join(corrida, "pruebas.csv"), encoding="utf-8") as archivo:
        return {(fila["clase"], fila["prueba"]): fila["estado"] for fila in csv.DictReader(archivo)}


def inestables(rutas_ruido):
    pasos = set()
    for ruta in filter(None, rutas_ruido.split(",")):
        with open(ruta, encoding="utf-8") as archivo:
            pasos |= {(f["escenario"], f["paso"]) for f in csv.DictReader(archivo) if f["estado"] == "Cambió"}
    return pasos


def main(base, nueva, salida, titulo_base, titulo_nueva, rutas_ruido=""):
    os.makedirs(salida, exist_ok=True)
    pasos_inestables = inestables(rutas_ruido)
    filas = []
    for escenario in sorted(e for e in os.listdir(base) if os.path.isdir(os.path.join(base, e))):
        if not os.path.isdir(os.path.join(nueva, escenario)):
            filas.append([escenario, "", "", "", "Escenario sin evidencia en la versión nueva"])
            continue
        pasos = comparar_carpetas(
            os.path.join(base, escenario), os.path.join(nueva, escenario), os.path.join(salida, escenario),
            (f"{titulo_base} · {escenario}", titulo_nueva), recortar=False, guardar_iguales=False,
        )
        for paso in pasos:
            if (escenario, paso[0]) in pasos_inestables:
                paso[-1] = "Inestable"
                imagen = os.path.join(salida, escenario, paso[0] + ".jpg")
                if os.path.exists(imagen):
                    os.remove(imagen)
            filas.append([escenario, *paso])
        if not os.listdir(os.path.join(salida, escenario)):
            os.rmdir(os.path.join(salida, escenario))
    with open(os.path.join(salida, "resultado.csv"), "w", newline="", encoding="utf-8") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow(["escenario", "paso", "porcentaje_distinto", "desplazamiento_px", "estado"])
        escritor.writerows(filas)

    antes, despues = estados(base), estados(nueva)
    with open(os.path.join(salida, "pruebas.csv"), "w", newline="", encoding="utf-8") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow(["clase", "prueba", titulo_base, titulo_nueva])
        for (clase, prueba), estado in sorted(antes.items()):
            escritor.writerow([clase, prueba, estado, despues.get((clase, prueba), "No existe")])

    cambios = [f for f in filas if f[-1] not in ("Igual", "Inestable")]
    print(f"{len(filas)} pasos comparados: {sum(f[-1] == 'Igual' for f in filas)} iguales, "
          f"{sum(f[-1] == 'Inestable' for f in filas)} inestables, {len(cambios)} con cambios")
    for fila in cambios:
        print(*fila, sep="\t")
    rotas = [(c, p, despues.get((c, p))) for (c, p), e in antes.items() if e == "OK" and despues.get((c, p)) != "OK"]
    print("Pruebas que pasaban y ya no:", rotas or "ninguna")


if __name__ == "__main__":
    main(*sys.argv[1:7])
