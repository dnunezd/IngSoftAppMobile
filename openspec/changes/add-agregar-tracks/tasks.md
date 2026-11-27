# Tasks

Reglas para todos los grupos:

- **TDD obligatorio:** escribir primero las pruebas de cada comportamiento, ejecutarlas y registrar el ROJO antes del código de producción; implementar después el mínimo necesario y comprobar el VERDE. Correr las regresiones pertinentes al cerrar cada grupo.
- No afirmar un ROJO si el fallo solo demuestra una referencia temporal inexistente o un problema de compilación no relacionado con el comportamiento.
- No cambiar BackVynils ni inventar el endpoint: confirmar su contrato antes de implementar la llamada.
- Mantener los textos de los criterios en `strings.xml`.
- No implementar ni ejecutar E2E en este cambio; dejar su alcance en el grupo 7.

## 1. Contrato de BackVynils

- [x] 1.1 Auditar el controlador y servicio de referencia de BackVynils: `POST /albums/{albumId}/tracks`, cuerpo `name` y `duration`, respuesta 200 con track y 404 para álbum inexistente. Con el backend local activo se verificó `GET /albums`, un `POST` a un ID inexistente que respondió 404, y un guardado exitoso real: `POST /albums/103/tracks` creó el track `{"id":1,"name":"Tie Your Mother Down","duration":"5:05"}`, confirmado luego con `GET /albums/103`.
- [x] 1.2 Documentar el contrato de referencia en `design.md` y usarlo como contrato esperado de MockWebServer. La respuesta exitosa local quedó confirmada en 1.1.

## 2. Modelo, API y repositorio

- [x] 2.1 Escribir primero pruebas unitarias del repositorio: ID de álbum, método/ruta y cuerpo exactos según 1.1; respuesta exitosa; 404; error del servicio; desconexión y cancelación relanzada. Se corrigieron primero los fakes para que el ROJO observado fuera de comportamiento y no de compilación.
- [x] 2.2 Implementar `TrackApi`, el cuerpo de creación y la operación de `AlbumRepository`/`AlbumRepositoryImpl`; distinguir el 404 del álbum de los errores reintentables.
- [x] 2.3 Ejecutar las pruebas del repositorio y confirmar que pasan sin modificar el contrato ni ocultar errores inesperados.

## 3. Validación del formulario

- [x] 3.1 Escribir primero pruebas de la función pura: nombre vacío/espacios, duración vacía, duración `5:05`, minutos de dos o más dígitos, segundos `00` y `59`, segundos `60`, segundos de un dígito, separadores ausentes o adicionales, espacios y texto no numérico. ROJO confirmado.
- [x] 3.2 Implementar la validación del nombre y `m:ss`, con mensaje de formato exacto y sin agregar una restricción de duración mínima.
- [x] 3.3 Ejecutar las pruebas y confirmar el VERDE.

## 4. Estado y comportamiento del ViewModel

- [x] 4.1 Escribir primero pruebas del ViewModel para errores iniciales, edición, validación, envío único, éxito, error/reintento y 404. ROJO confirmado. La conservación al girar se verifica en la capa de UI.
- [x] 4.2 Implementar estado/ViewModel para validación, envío único, error recuperable, álbum no disponible y conservación ante cambios de configuración mediante el ViewModel.
- [x] 4.3 Ejecutar las pruebas del ViewModel y confirmar el VERDE.

## 5. Interfaz y navegación

- [ ] 5.1 Ejecutar primero en emulador las pruebas UI para el formulario y navegación y registrar el ROJO antes de la implementación visual. No se observó el ROJO de estas pruebas antes de implementar; no marcar como completado retrospectivamente.
- [x] 5.2 Implementar la acción exclusiva del Coleccionista, el formulario, los textos, la navegación y el refresco del detalle al guardar correctamente.
- [x] 5.3 Bloquear Cancelar y las acciones de salida durante el envío; ofrecer regreso desde el estado de álbum no disponible.
- [x] 5.4 Ejecutar las pruebas de interfaz/navegación y confirmar el VERDE en emulador API 37: 31 pruebas, 0 fallos.

## 6. Verificación de la implementación

- [x] 6.1 Ejecutar las pruebas unitarias pertinentes y `lint`.
- [x] 6.2 Compilar las pruebas instrumentadas existentes y verificar que no hay regresiones de compilación.
- [x] 6.3 Completar la prueba funcional manual con el backend local. Revisión visual (emulador API 23, build con `-PbaseUrl=http://localhost:3000/` + `adb reverse tcp:3000`): formulario vacío legible, ambos "Campo obligatorio" visibles, Guardar deshabilitado y Cancelar visible; también se verificó el detalle antes de abrir el formulario. Guardado exitoso confirmado end-to-end: se agregó "Tie Your Mother Down" (5:05) al álbum "A Day at the Races" (id 103) de Queen, apareció el snackbar "Canción agregada", la app regresó al detalle y la canción se mostró en la lista "01 Tie Your Mother Down — 5:05"; verificado también contra el backend con `GET /albums/103` (`tracks` contiene el nuevo registro).
- [x] 6.4 Validar el cambio con `openspec validate add-agregar-tracks --strict`.

## 7. E2E propuestas para un cambio separado (no implementar aquí)

Preparar posteriormente una clase E2E, responsable separado, backend local y limpieza de los datos creados. La propuesta inicial cubre un PDF por escenario:

- [ ] 7.1 **FN08-ESC01:** Visitante abre el detalle; no ve la acción para agregar canción.
- [ ] 7.2 **FN08-ESC02:** Coleccionista abre el formulario, agrega una canción válida; aparece "Canción agregada", regresa al mismo detalle y la canción aparece con su duración.
- [ ] 7.3 **FN08-ESC03:** Formulario vacío muestra "Campo obligatorio" en ambos campos desde que abre y "Guardar" deshabilitado; no se envía nada.
- [ ] 7.4 **FN08-ESC04:** Duración inválida muestra el mensaje de formato y mantiene "Guardar" deshabilitado; no se envía nada.
- [ ] 7.5 **FN08-ESC05:** Coleccionista escribe datos y cancela; el formulario se cierra sin petición y, al abrirlo de nuevo, aparece vacío.
- [ ] 7.6 **FN08-ESC06:** Envío demorado muestra "Guardando..." deshabilitado; toques repetidos producen una sola petición.
- [ ] 7.7 **FN08-ESC07:** Falla de conexión/servicio muestra el mensaje de reintento, conserva los datos y permite guardar correctamente al restablecerse el servicio.
- [ ] 7.8 **FN08-ESC08:** El backend responde 404 al guardar porque el álbum ya no existe; no se agrega la canción, aparece "El álbum ya no está disponible" y el usuario puede volver.
- [ ] 7.9 **FN08-ESC09:** Coleccionista gira la pantalla con datos escritos y confirma que se conservan; cubrir también que girar durante el envío no duplica la petición.

El cambio E2E futuro debe comprobar la estrategia de limpieza con el contrato real de BackVynils antes de crear datos, guardar evidencia PDF individual para los escenarios implementados y no reportar como ejecutados los escenarios que no se hayan corrido.
