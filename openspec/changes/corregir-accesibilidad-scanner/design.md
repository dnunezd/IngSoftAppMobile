# Design

## Context

Hallazgos del Scanner (emulador API 35, backend local; `Text contrast` salvo que se indique):

| Pantalla | Elementos |
|---|---|
| Ingreso | "Beats," (`TitularGris`), "Sound", sticker "Beats & Sound", chip "Acceso Libre", botones "Entrar"; *Item descriptions*: flecha de "Entrar" |
| Coleccionistas | chips "CURATED VINYLS" y "Explore Sounds", "eres?", iniciales MB/JM; *Item descriptions*: dos iconos del encabezado |
| Inicio | "VINILO CLUB", botones "Ver álbumes" y "Ver artistas" |
| Catálogo | chip "VINILO CATALOG", chip "álbumes", tarjetas de álbum (4 sugerencias en total) |
| Detalle de álbum | "Rock"/"Salsa", "Artistas", "Canciones", "Comentarios", número de pista, "5,0" y estrellas |
| Artistas | chips naranja; *Item descriptions*: encabezado "Artistas" |
| Crear álbum | chip "álbum", botón "Crear álbum" |
| Detalle de artista | sin hallazgos |

Causa común: el naranja `Naranja` `#FF4D26` da 3,3:1 con blanco y unos 3,2:1 sobre `Fondo`, y `TitularGris` `#8A8A93` queda en torno a 3,2:1. Los tonos `NaranjaOscuro` `#AF2900`, `ChipTexto` `#C2411E` y `TextoTenue` `#71717A` deberían superar 4,5:1 (se verifica en la tarea 1.1).

## Goals / Non-Goals

**Goals:**
- Que el Scanner no reporte contraste ni descripciones en las 8 pantallas.
- Cambiar lo mínimo de la paleta del Figma.
- Dejar una prueba automática de regresión.

**Non-Goals:**
- Rediseñar pantallas, tipografías o textos.
- Modo oscuro, escala de fuente y recorrido completo con TalkBack.

## Decisions

1. **Un token nuevo, `NaranjaContraste`, en vez de oscurecer `Naranja` en todos lados.** Se usa como relleno de botones y chips con texto blanco y como color de texto naranja sobre fondo claro. `Naranja` se conserva para elementos no textuales (iconos, ondas, decoración), que solo exigen 3:1. El valor se elige entre `#D13A12` y `NaranjaOscuro` verificando ≥ 4,5:1 con blanco y con `Fondo`; se prefiere el más cercano al original. *Alternativa:* usar `NaranjaOscuro` directo, más simple pero visiblemente más apagado.
2. **`TitularGris` pasa a `TextoTenue`** (o a un gris equivalente ≥ 4,5:1). El efecto de "dos tonos" del titular se mantiene.
3. **Avatares:** las iniciales usan el color de texto oscuro de cada variante (`AvatarAzul`, `AvatarGris`) sobre su fondo claro, en vez de blanco sobre gradiente.
4. **Semántica:** los iconos decorativos llevan `contentDescription = null` y el contenedor usa `semantics(mergeDescendants = true)`; los encabezados usan `semantics { heading() }`. No se agregan textos visibles.
5. **Pruebas:** una unitaria que calcula la razón de contraste entre pares fondo/texto de los tokens (falla rápido, sin emulador) y una instrumentada con `enableAccessibilityChecks()` que navega por las 8 pantallas con los repositorios falsos ya existentes. *Descartada:* depender solo del Scanner manual, que no es reproducible en CI.

## Risks / Trade-offs

- El naranja de los botones se ve algo más oscuro que en el Figma. Se valida con capturas antes/después y se registra la desviación.
- Las comprobaciones de Compose (ATF) no evalúan contraste sobre imágenes ni gradientes, así que el Scanner sigue siendo la verificación final; se repite al terminar y se guarda el resultado.
- Las tarjetas del catálogo se reportan por texto sobre portadas variables: si el problema es la insignia de año, se le da un fondo más opaco.
