# Tasks

## 1. Consumo del detalle y acceso compartido al backend (T02-2)

- [x] 1.1 Escribir primero pruebas unitarias del Service Adapter con `MockWebServer`: respuesta exitosa, 404 con status preservado, 500, desconexión, timeout, JSON ilegible y cancelación relanzada. Excepción autorizada: el adaptador ya estaba implementado; estos casos se verificaron como caracterización, sin afirmar un ROJO retroactivo. La cancelación se comprueba directamente en la operación suspendida.
- [x] 1.2 Crear/reutilizar el Service Adapter como único constructor de Retrofit y punto común para ejecutar llamadas; garantizar que los errores HTTP retienen el código de estado necesario para los repositorios.
- [x] 1.3 Migrar `CollectorRepository` para usar el Service Adapter compartido sin cambiar el comportamiento observable de HU00; actualizar sus pruebas y los repositorios falsos existentes. No modificar archivos compartidos de Gradle ni agregar dependencias.
- [x] 1.4 Escribir primero pruebas de `AlbumApi`/repositorio que comprueben `GET /albums/{id}`, el ID enviado, el mapeo de datos relacionados, el 404 y los fallos de red/servidor.
- [x] 1.5 Implementar API, DTOs y modelos de dominio para álbum, tracks, artistas y comentarios con solo los datos que HU02 necesita. No exponer ni mostrar identidad de autor.
- [x] 1.6 Implementar `AlbumRepository`: ejecutar la consulta por el Service Adapter, convertir DTO a modelo, traducir 404 a `AlbumNoDisponible` y dejar los demás fallos como error recuperable de carga. Verificar éxito, 404, 500, desconexión, timeout, respuesta ilegible y cancelación. El timeout y la cancelación se verificaron como regresiones autorizadas del comportamiento ya cubierto, sin fabricar un ROJO.

## 2. Estado, lógica y pantalla de detalle (T02-3)

- [ ] 2.0 Usar la pantalla adjunta de Stitch como referencia visual para ordenar portada, metadatos y secciones de contenido; omitir "Escuchar ahora" y no implementar interacciones de la imagen que no estén en los criterios de HU02.
- [ ] 2.1 Escribir pruebas unitarias para la función pura de fecha de texto a texto: `1984-08-01` produce `1 de agosto de 1984`; incluir timestamp ISO UTC y verificar resultado idéntico en distintas zonas horarias. Confirmar que fallan antes de implementar.
- [ ] 2.2 Implementar el formateo aislado en UTC, con idioma español determinista. No formatear fechas en Compose. La ejecución en API 23 depende de la integración externa de minSdk/desugaring; no editar Gradle en esta tarea.
- [ ] 2.3 Escribir pruebas del ViewModel antes de implementarlo: `Cargando → Exito`, `Cargando → NoDisponible`, `Cargando → Error`, reintento `Error → Cargando → Exito` y que un resultado tardío no reemplace la consulta vigente.
- [ ] 2.4 Implementar ViewModel y estado de UI para el álbum por ID, con reintento solo desde el error de conexión/servidor.
- [ ] 2.5 Escribir pruebas unitarias de presentación antes de implementar los cálculos: canciones vacías, comentarios vacíos, promedio aritmético, ausencia de datos de autor y salida de fecha formateada.
- [ ] 2.6 Implementar la pantalla Compose siguiendo la jerarquía visual de Stitch: portada arriba, nombre, descripción, género, disquera, fecha y secciones de canciones, artistas, comentarios y promedio. Mostrar exactamente "Este álbum aún no tiene canciones registradas", "Aún no hay comentarios para este álbum" y "Sin calificaciones" cuando corresponda; no mostrar quién escribió cada comentario ni agregar el botón "Escuchar ahora".
- [ ] 2.7 Definir un componente de portada con fallback genérico y punto de integración para una futura librería. Escribir pruebas del placeholder/error antes de implementar el componente. No agregar la librería ni simular funciones de red fuera del alcance.
- [ ] 2.8 Implementar indicador de progreso, mensaje 404 "El álbum ya no está disponible" con acción de volver, y mensaje de error "No pudimos cargar el álbum" con "Reintentar". Verificar cada estado con pruebas de UI.

## 3. Navegación desde y hacia el catálogo (T02-4)

- [ ] 3.1 Escribir primero pruebas de navegación para que seleccionar un álbum pase su ID al detalle y para que volver desde éxito o 404 regrese al catálogo; confirmar que fallen antes del cambio.
- [ ] 3.2 Agregar la ruta parametrizada del detalle e integrarla con la acción de selección del catálogo HU01. Mantener el catálogo existente como destino y no duplicar su implementación.
- [ ] 3.3 Ejecutar pruebas de navegación/UI para varios IDs, estado 404 y regreso; coordinar la integración con HU01 para evitar conflictos sobre el destino provisional.

## 4. Pruebas E2E (T02-5)

- [ ] 4.1 Preparar pruebas instrumentadas contra BackVynils real para un álbum con canciones y comentarios y otro sin canciones ni comentarios.
- [ ] 4.2 Verificar los datos presentados, la lista de artistas, la portada/fallback, el promedio, el texto de vacíos y el retorno al catálogo; comprobar la fecha en dispositivo API 23 una vez integrado el cambio de minSdk/desugaring.
- [ ] 4.3 Ejecutar unitarias y pruebas instrumentadas pertinentes con backend disponible; documentar cualquier prueba bloqueada por backend, dispositivo o integración de HU01 sin marcarla como verificada.

## 5. Documentación y mockups (T02-6)

- [ ] 5.1 Actualizar la wiki de HU02 con los criterios implementados, estados de carga/error/404, fecha UTC, privacidad del autor y promedio.
- [ ] 5.2 Actualizar los mockups para incluir detalle, progreso, error recuperable, álbum no disponible, listas sin elementos y portada genérica.
- [ ] 5.3 Confirmar con el equipo la precisión/redondeo del promedio, el tratamiento de fechas ISO inválidas, el fallback de imágenes de artistas y el flujo de carga de portada sin dependencia nueva; reflejar las decisiones en la wiki y cerrar las preguntas abiertas.
- [ ] 5.4 Después de integrar el cambio transversal de compatibilidad, actualizar en README el mínimo API 23 y la configuración de desarrollo pertinente. No cambiar Gradle desde HU02.
