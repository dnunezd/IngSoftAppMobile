# Spec Delta

## ADDED Requirements

### Requirement: Controles deshabilitados legibles
El texto y el icono de un botón deshabilitado SHALL tener una razón de contraste de al menos 4,5:1 con su fondo, aunque WCAG exima a los controles inactivos, para que el Accessibility Scanner no reporte "Text contrast" sobre ellos. Aplica a "Publicar comentario", "Guardar" (agregar canción) y "Asociar".

#### Scenario: Botón deshabilitado al abrir un formulario
- **WHEN** se abre Comentar álbum, Agregar canción o la ventana de asociar sin datos válidos
- **THEN** el botón principal se ve deshabilitado y su texto tiene al menos 4,5:1 con su fondo

### Requirement: Diálogos y hojas con la paleta de la app
Los diálogos y las hojas inferiores SHALL usar los colores de superficie de la paleta de la app y no los colores por defecto de Material, y sus acciones MUST cumplir 4,5:1 con ese fondo. El área táctil del asa de arrastre de una hoja MUST medir al menos 48 × 48 dp.

#### Scenario: Diálogo de descartar comentario
- **WHEN** el coleccionista sale de Comentar álbum con texto escrito
- **THEN** el diálogo "¿Descartar comentario?" usa un fondo de la paleta
- **AND** "Descartar y salir" y "Continuar escribiendo" tienen al menos 4,5:1 con ese fondo

#### Scenario: Asa de la ventana de asociar
- **WHEN** se abre la ventana "Asociar álbum a …"
- **THEN** el asa de arrastre tiene un área táctil de al menos 48 × 48 dp

### Requirement: Elementos compuestos anunciados como uno
Un elemento formado por varios textos que solo tienen sentido juntos MUST anunciarse como un solo elemento: cada pista del detalle del álbum (número, nombre y duración), cada comentario del detalle (texto y calificación) y las estrellas de solo lectura de un comentario publicado ("4 de 5 estrellas"). El promedio del detalle MUST exponer su número como texto y anunciar las estrellas con su valor ("5,0, de 5 estrellas"), sin repetir el número.

#### Scenario: Pistas con nombres o duraciones repetidos
- **WHEN** un álbum tiene dos canciones llamadas igual o con la misma duración
- **THEN** cada pista es un elemento distinto ("03, Prueba, 3:07" y "04, Prueba, abc")
- **AND** el Scanner no reporta "Item descriptions" sobre la lista de canciones

#### Scenario: Comentarios con la misma calificación
- **WHEN** dos comentarios del álbum tienen la misma calificación
- **THEN** cada comentario es un elemento distinto con su texto y su calificación

#### Scenario: Promedio del álbum
- **WHEN** se muestra el promedio con sus estrellas
- **THEN** el Scanner no reporta "Unexposed Text" sobre él
- **AND** TalkBack lee el número una sola vez

#### Scenario: Estrellas del comentario publicado
- **WHEN** se muestra el resumen del comentario publicado
- **THEN** las estrellas no son accionables y se anuncian como un solo elemento con la calificación

### Requirement: Pantallas de HU08, HU09 y HU15
Comentar álbum (formulario, diálogo de descarte y comentario publicado), Agregar canción y la ventana de asociar un álbum (con y sin álbum elegido) SHALL cumplir los mismos criterios que el resto de la app:
- Contraste de 4,5:1.
- Títulos de sección marcados como encabezados: "¿Qué te pareció este vinilo?", "¡Comentario publicado exitosamente!", "Datos de la canción" y "Asociar álbum a …".
- Portadas descritas como "Portada del álbum, " seguido del nombre.
- Ningún texto recortado, partido o apilado con la fuente al 200 %.

#### Scenario: Escaneo de las pantallas nuevas
- **WHEN** se ejecuta el Accessibility Scanner sobre esas pantallas y estados
- **THEN** no reporta contraste, destinos táctiles ni descripciones
- **AND** solo quedan las sugerencias de "Unexposed Text" sobre portadas con texto impreso que sirve el backend y sobre el fondo oscurecido de la hoja inferior de Material

#### Scenario: Fuente al 200 %
- **WHEN** la fuente del sistema está al 200 %
- **THEN** el número de pista del detalle ocupa una línea, el contador "0 / 400" de Comentar se lee en una fila y los títulos de la ventana de asociar no se truncan por la etiqueta "Ya asociado"
