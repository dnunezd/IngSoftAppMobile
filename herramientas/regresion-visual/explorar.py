"""Ayuda para trazar el recorrido: ejecuta acciones y muestra los textos de la pantalla.

Uso: python explorar.py <serial> "accion1" "accion2" ...
Acciones: instalar:<apk> | abrir | tocar:<texto>[#indice] | debajo:<ancla>|<texto> | deslizar | atras |
          escribir:<texto> | teclado | esperar:<texto> | ver
"""
import sys

from pantalla import Pantalla

pantalla = Pantalla(sys.argv[1])
for accion in sys.argv[2:]:
    nombre, _, valor = accion.partition(":")
    if nombre == "instalar":
        pantalla.preparar_dispositivo()
        pantalla.instalar(valor)
    elif nombre == "abrir":
        pantalla.abrir_app()
    elif nombre == "tocar":
        texto, _, indice = valor.partition("#")
        pantalla.tocar(texto, int(indice or 0))
    elif nombre == "parcial":
        pantalla.tocar(valor, exacto=False)
    elif nombre == "debajo":
        ancla, _, texto = valor.partition("|")
        pantalla.tocar_debajo_de(ancla, texto)
    elif nombre == "deslizar":
        pantalla.deslizar()
    elif nombre == "atras":
        pantalla.atras()
    elif nombre == "escribir":
        pantalla.escribir(valor)
    elif nombre == "teclado":
        pantalla.ocultar_teclado()
    elif nombre == "esperar":
        pantalla.esperar(valor)
    elif nombre == "ver":
        pass
textos = [(Pantalla.texto(n), Pantalla.limites(n)[1]) for n in pantalla.nodos() if Pantalla.texto(n)]
print(" | ".join(f"{t}@{y}" for t, y in textos))
