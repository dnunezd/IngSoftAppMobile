"""Compara las capturas de dos versiones, pantalla por pantalla.

Uso: python comparar.py <carpeta_base> <carpeta_nueva> <carpeta_salida>

Solo compara las pantallas que existen en la versión base (una regresión solo puede medirse sobre lo que ya
existía). Por cada pantalla guarda una imagen con la base, la nueva y las diferencias en rojo, y escribe
`resultado.csv` con el porcentaje de píxeles distintos y el desplazamiento vertical que se corrigió al alinear.
La barra de estado y la de navegación del sistema no se comparan: no son parte de la app.
"""
import csv
import os
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFont

# Un píxel cuenta como distinto si algún canal cambia más que esto (evita el ruido del antialiasing).
UMBRAL_PIXEL = 40
# Por debajo de este porcentaje de píxeles distintos la pantalla se considera igual (ruido medido: 0 %).
UMBRAL_IGUAL = 0.1
# Fracción de la altura que ocupan las barras del sistema, arriba y abajo.
BARRA_ESTADO = 0.045
BARRA_NAVEGACION = 0.055
# Máximo desplazamiento vertical que se corrige al alinear (en píxeles de la captura).
DESPLAZAMIENTO_MAXIMO = 160


def recortar_sistema(imagen):
    ancho, alto = imagen.size
    return imagen.crop((0, int(alto * BARRA_ESTADO), ancho, int(alto * (1 - BARRA_NAVEGACION))))


def alinear(base, nueva):
    """Desplazamiento vertical (en píxeles) que mejor hace coincidir [nueva] con [base].

    Al deslizar, el gesto se detiene unos píxeles más arriba o más abajo en cada corrida: sin alinear, toda la
    pantalla cuenta como distinta. Se busca en ±DESPLAZAMIENTO_MAXIMO sobre una versión reducida y luego se afina.
    """
    def error(a, b, dy):
        ancho, alto = a.size
        if dy >= 0:
            recorte_a, recorte_b = a.crop((0, dy, ancho, alto)), b.crop((0, 0, ancho, alto - dy))
        else:
            recorte_a, recorte_b = a.crop((0, 0, ancho, alto + dy)), b.crop((0, -dy, ancho, alto))
        histograma = ImageChops.difference(recorte_a, recorte_b).histogram()
        return sum(i * n for i, n in enumerate(histograma)) / max(1, recorte_a.size[0] * recorte_a.size[1])

    escala = 4
    gris_a = base.convert("L").resize((base.size[0] // escala, base.size[1] // escala))
    gris_b = nueva.convert("L").resize((base.size[0] // escala, base.size[1] // escala))
    rango = DESPLAZAMIENTO_MAXIMO // escala
    grueso = min(range(-rango, rango + 1), key=lambda dy: error(gris_a, gris_b, dy)) * escala
    gris_a, gris_b = base.convert("L"), nueva.convert("L")
    return min(range(grueso - escala, grueso + escala + 1), key=lambda dy: error(gris_a, gris_b, dy))


def desplazar(imagen, dy):
    """Mueve la imagen dy píxeles hacia abajo (negativo: hacia arriba), rellenando con blanco."""
    lienzo = Image.new("RGB", imagen.size, "white")
    lienzo.paste(imagen, (0, dy))
    return lienzo


def rellenar(imagen, tamano):
    lienzo = Image.new("RGB", tamano, "white")
    lienzo.paste(imagen, (0, 0))
    return lienzo


def comparar(base, nueva):
    """Devuelve (porcentaje de píxeles distintos, máscara de diferencias, desplazamiento aplicado)."""
    if base.size != nueva.size:
        nueva = nueva.resize(base.size)
    dy = alinear(base, nueva)
    if dy:
        nueva = desplazar(nueva, dy)
    diferencia = ImageChops.difference(base, nueva).convert("L")
    # La franja que el desplazamiento deja sin contenido no se cuenta como diferencia.
    if dy > 0:
        diferencia.paste(0, (0, 0, base.size[0], dy))
    elif dy < 0:
        diferencia.paste(0, (0, base.size[1] + dy, base.size[0], base.size[1]))
    mascara = diferencia.point(lambda valor: 255 if valor > UMBRAL_PIXEL else 0)
    distintos = mascara.histogram()[255]
    return 100.0 * distintos / (base.size[0] * base.size[1]), mascara, dy


def composicion(base, nueva, mascara, titulo_base, titulo_nueva):
    """Base | nueva | nueva con las diferencias en rojo, a media resolución."""
    rojo = Image.new("RGB", nueva.size, (230, 0, 0))
    resaltada = Image.composite(rojo, nueva.convert("RGB").point(lambda v: int(v * 0.55 + 115)), mascara)
    paneles = [base.convert("RGB"), nueva.convert("RGB").resize(base.size), resaltada.resize(base.size)]
    escala = 0.45
    ancho = int(base.size[0] * escala)
    alto = int(base.size[1] * escala)
    margen, cabecera = 16, 56
    lienzo = Image.new("RGB", (3 * ancho + 4 * margen, alto + cabecera + margen), "white")
    dibujo = ImageDraw.Draw(lienzo)
    try:
        fuente = ImageFont.truetype("arial.ttf", 26)
    except OSError:
        fuente = ImageFont.load_default()
    for i, (panel, titulo) in enumerate(zip(paneles, [titulo_base, titulo_nueva, "Diferencias"])):
        x = margen + i * (ancho + margen)
        dibujo.text((x, 14), titulo, fill="black", font=fuente)
        lienzo.paste(panel.resize((ancho, alto)), (x, cabecera))
    return lienzo


def comparar_carpetas(carpeta_base, carpeta_nueva, salida, titulos, recortar=True, guardar_iguales=True):
    """Compara las capturas de dos carpetas y devuelve una fila por captura de la base.

    [recortar]: quita las barras del sistema (capturas de pantalla completa). Las de `EvidenciaE2E` en API 26 o
    superior ya son solo la app y no se recortan.
    """
    os.makedirs(salida, exist_ok=True)
    filas = []
    for nombre in sorted(f for f in os.listdir(carpeta_base) if f.endswith(".png")):
        ruta_nueva = os.path.join(carpeta_nueva, nombre)
        if not os.path.exists(ruta_nueva):
            filas.append([nombre[:-4], "", "", "Sin captura en la versión nueva"])
            continue
        base = Image.open(os.path.join(carpeta_base, nombre)).convert("RGB")
        nueva = Image.open(ruta_nueva).convert("RGB")
        if recortar:
            base, nueva = recortar_sistema(base), recortar_sistema(nueva)
        if base.size != nueva.size:
            # P. ej., una prueba que en una versión dibuja bajo las barras del sistema y en otra no. Se ponen las
            # dos sobre un lienzo común, sin deformarlas, y la alineación vertical corrige el corrimiento.
            tamano = (max(base.size[0], nueva.size[0]), max(base.size[1], nueva.size[1]))
            base, nueva = rellenar(base, tamano), rellenar(nueva, tamano)
        porcentaje, mascara, dy = comparar(base, nueva)
        estado = "Igual" if porcentaje < UMBRAL_IGUAL else "Cambió"
        if estado == "Cambió" or guardar_iguales:
            composicion(base, desplazar(nueva, dy) if dy else nueva, mascara, *titulos).save(
                os.path.join(salida, nombre[:-4] + ".jpg"), quality=82
            )
        filas.append([nombre[:-4], f"{porcentaje:.2f}", dy, estado])
    return filas


def main(carpeta_base, carpeta_nueva, salida):
    titulos = (os.path.basename(os.path.normpath(carpeta_base)), os.path.basename(os.path.normpath(carpeta_nueva)))
    filas = comparar_carpetas(carpeta_base, carpeta_nueva, salida, titulos)
    with open(os.path.join(salida, "resultado.csv"), "w", newline="", encoding="utf-8") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow(["pantalla", "porcentaje_distinto", "desplazamiento_px", "estado"])
        escritor.writerows(filas)
    for fila in filas:
        print(*fila, sep="\t")


if __name__ == "__main__":
    main(*sys.argv[1:4])
