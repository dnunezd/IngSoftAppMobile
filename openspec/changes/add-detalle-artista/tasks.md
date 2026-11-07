# Tasks

Para cada comportamiento, la prueba se escribe y se comprueba fallida antes de implementarlo (ver D8). La verificación en dispositivo se hace en el Samsung físico por adb inalámbrico, con el backend desplegado por defecto.

## 1. Consumo del detalle, los premios y su mapeo (T04-2, issue #25)

- [x] 1.1 Escribir en `ArtistRepositoryTest` (con `MockWebServer`) las pruebas de la consulta por tipo: `GET /musicians/{id}` y `GET /bands/{id}` con el id correcto y mapeo a `ArtistaDetalle`; verificar que fallan por no existir la operación.
- [x] 1.2 Agregar a `ArtistApi` las tres consultas y sus DTOs, el modelo `ArtistaDetalle` y `obtenerDetalle(tipo, id)` en `ArtistRepository` sin premios; verificar con `testDebugUnitTest` que las pruebas de 1.1 pasan y que `ArtistDtoTest` sigue en verde.
- [x] 1.3 Implementar el cruce de premiaciones (D1) con pruebas primero: cruce por `id` de premiación, sin segunda petición si el artista no tiene premios, y `ErrorDeCarga` si falla solo `/performerprizes`; verificar con `testDebugUnitTest`.
- [x] 1.4 Implementar la conversión tolerante y el orden (D4, D5) con pruebas primero: artista sin id o sin nombre es error, omisión de álbumes, integrantes y premios incompletos, fecha ausente o malformada, año del álbum y orden del más reciente al más antiguo con los que no tienen fecha al final; verificar con `testDebugUnitTest`.
- [x] 1.5 Cubrir 404, 500, desconexión, respuesta ilegible y cancelación relanzada en ambos tipos; verificar con `testDebugUnitTest` (los casos que ya resuelve el adaptador pueden registrarse como regresión, sin afirmar un ROJO previo).

## 2. Estado y pantalla del detalle (T04-3, issue #26)

- [x] 2.1 Escribir `ArtistaDetalleViewModelTest` e implementar el ViewModel con `Cargando | Exito | Error`: carga al crearse, reintento solo desde `Error` y un resultado tardío no reemplaza la consulta vigente; verificar con `testDebugUnitTest`.
- [x] 2.2 Compartir `Encabezado`, `Error` y `EtiquetaTipo` de `PantallaArtistas` con un parámetro de texto donde haga falta, sin cambiar su aspecto (D6); verificar que `PantallaArtistasTest` sigue en verde.
- [x] 2.3 Implementar `PantallaDetalleArtista` en sus estados de carga (indicador centrado "Cargando artista...", tarjeta y tres filas de relleno, como el marco "Cargando" de Figma) y error ("No se pudo cargar el artista" y "Reintentar"), con el encabezado "Artista" y atrás disponible; verificar con una prueba de UI nueva en `androidTest`.
- [x] 2.4 Implementar la tarjeta principal: imagen con respaldo por tipo, nombre, `EtiquetaTipo` compartida, descripción y fecha con su rótulo según el tipo, conservando las etiquetas de prueba actuales; verificar con pruebas de UI para músico y banda, incluida la omisión de fecha y descripción.
- [x] 2.5 Implementar las secciones de integrantes (solo banda, con "Sin integrantes"), álbumes ("Sin álbumes") y premios ("Sin premios"), con contadores en singular y plural solo cuando hay elementos y el bloque gris con ícono cuando la sección está vacía; verificar con pruebas de UI de cada sección con datos y vacía, incluida una lista larga que se desplaza y que tocar un integrante no navega, y comparar a ojo en el Samsung con los marcos de Figma.
- [x] 2.6 Verificar en las pruebas de UI que una imagen de artista o una portada ausente muestra el respaldo (ícono de disco `ic_cat_album` en las miniaturas, D6) sin romper la fila, y agregar previews de los estados; comprobar con `lint` que no hay problemas ni textos sin recurso. Las pruebas de UI de 2.3 a 2.6 pasaron en el Samsung, pero `PantallaDetalleArtistaTest` no se incluye en este cambio: esos comportamientos no quedan con prueba automatizada propia.

## 3. Navegación y reemplazo del destino provisional (T04-4, issue #27)

- [x] 3.1 Escribir en `VinilosNavHostTest` las pruebas de: seleccionar músico y banda abre su detalle por tipo e id; atrás vuelve al listado con la búsqueda y sin repetir su consulta; seleccionar un álbum abre su detalle; volver regresa al artista sin repetir la consulta; verificar que fallan antes del cambio.
- [x] 3.2 Cambiar `Rutas.DetalleArtista` a `artista/{tipo}/{id}`, conectar el destino con el ViewModel y la navegación a `Rutas.detalleAlbum`, y actualizar `RepositorioArtistasFalso`; verificar que las pruebas de 3.1 pasan.
- [x] 3.3 Eliminar `PantallaArtistaProvisional` y el texto `artista_detalle_titulo`, y adaptar `ArtistasE2ETest` (FN03) para esperar el fin de la carga en lugar del destino provisional; verificar con búsqueda que no quedan referencias y que `:app:assembleDebugAndroidTest` compila.
- [x] 3.4 Agregar prueba de rotación en `RotacionTest` que confirme que el detalle cargado se conserva sin repetir la consulta; verificar en el Samsung.

## 4. Pruebas E2E (T04-5, issue #28, responsable Michael Mora)

- [ ] 4.1 Coordinar con Michael Mora quién escribe las E2E de la HU04 y avisarle de que FN03 cambió y de que Queen no tiene integrantes en el backend desplegado; queda verificado cuando el reparto conste en el issue #28.
- [x] 4.2 Ejecutar contra el backend real el flujo de un músico y una banda, revisar sus datos y abrir un álbum (Rubén Blades tiene premio y álbumes; Queen, premio, álbumes y ningún integrante); verificar con `OK` en la ejecución instrumentada sobre el Samsung, sin marcar como verificado lo que quede bloqueado. El flujo se comprobó con una prueba que no se incluye en este cambio: las E2E que queden en el repositorio corresponden a T04-5.

## 5. Verificación e integración

- [x] 5.1 Ejecutar `testDebugUnitTest`, `lint` y la suite instrumentada completa en el Samsung para confirmar que HU00 a HU03 no se afectaron; verificar `BUILD SUCCESSFUL` sin fallos.
- [ ] 5.2 Comprobar en API 23 que las fechas y las imágenes se ven bien, según la definición de terminado del sprint, que pide probar en un emulador; como aquí no se arranca el emulador, pedir esta corrida al usuario o a un compañero y dejar constancia del resultado.

## 6. Documentación (T04-6, issue #29)

- [x] 6.1 Actualizar la fila de la HU04 en el README con el estado real y, si aplica, la clase E2E en su tabla de evidencia; verificar revisando el diff.
- [ ] 6.2 Redactar para el equipo el texto de la wiki de la HU04 con los supuestos de este cambio ("Sin integrantes", orden del más reciente al más antiguo, error completo si fallan los premios, omisión de datos incompletos, avatar omitido e integrantes no seleccionables); publicarlo requiere la confirmación del usuario y queda verificado cuando la página lo refleje.
- [ ] 6.3 Listar las diferencias entre la pantalla y los mockups (avatar del encabezado omitido, orden del más reciente al más antiguo y sección "Sin integrantes", que no tiene marco) para actualizar Figma; verificar que la lista esté en el issue #29.

## Workflow follow-up

- Abrir el PR hacia `develop` con `Closes` de los issues #25, #26 y #27, y al menos dos revisores, según la definición de terminado.
- Archivar este cambio después de `add-listado-artistas`, retirando de esa spec la frase del destino provisional.
- Reportar al equipo el texto "Volver al catálogo" del álbum no disponible cuando se llega desde un artista.
- Reportar a Lorena Nossa que las pantallas de la HU10 en Figma usan otro diseño del detalle del artista y otro texto de botón.
