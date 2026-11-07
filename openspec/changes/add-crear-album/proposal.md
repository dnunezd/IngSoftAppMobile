# Proposal

## Why

El coleccionista solo puede consultar el catálogo; no tiene forma de aportar álbumes. La HU07 (Sprint 2) agrega esa primera acción de escritura. BackVynils ya ofrece `POST /albums` y `Session.collectorActionsEnabled` ya existe, pero ninguna pantalla lo usa todavía.

## What Changes

- Agrega un botón fijo "Crear álbum" abajo a la derecha del catálogo, visible solo para el Coleccionista, que abre el formulario "Crear".
- Agrega el formulario con nombre, portada (URL), fecha de lanzamiento, género, disquera y descripción. Género y disquera se eligen de listas con los valores exactos del backend, y la fecha se elige con un selector que no permite escribirla a mano ni elegir días posteriores a hoy.
- Valida al pulsar "Crear álbum": marca cada campo faltante con "Campo obligatorio", y la portada con "Ingresa una URL válida" si no empieza con `http://` o `https://`. Si algo falla, no se envía nada.
- Envía `POST /albums` con la fecha a medianoche UTC. Mientras envía, el botón muestra "Creando..." y se bloquean los campos, "Cancelar", la flecha y el botón atrás del sistema.
- Si el envío falla, muestra "No se pudo crear el álbum, intenta de nuevo", conserva los datos y el botón pasa a "Reintentar creación".
- Al crear, regresa al catálogo, muestra "Álbum creado", vuelve a consultar la lista, se desplaza hasta el álbum nuevo y lo marca con la insignia "NUEVO".
- Conserva todo lo escrito o elegido al girar la pantalla.

Decisiones confirmadas por el responsable de la HU sobre las contradicciones entre mockups y criterios, pendientes de socializar con el equipo: el botón "Crear álbum" no se desactiva por campos incompletos; "Cancelar" se bloquea durante el envío; los criterios prevalecen sobre los mockups; la descripción tiene un máximo de 500 caracteres y no tiene mínimo; la fecha no puede ser futura; se incorpora la insignia "NUEVO".

Fuera de alcance: barra inferior, buscador, filtros, orden "Recientes", borrador local, exportar, chips de edición, artista del álbum (HU15), carga de archivos de portada, cualquier cambio en BackVynils y las mejoras de desempeño descritas abajo.

**Rama:** `feature/hu07-crear-album`, basada en `origin/develop` (`371e2e5`). Antes de abrir el PR se integra lo que haya llegado a `origin/develop`.

## Segunda parte: mejoras futuras

La HU07 se implementa en dos partes. **Este cambio es la parte 1:** la funcionalidad, sin aplicar ninguna mejora de desempeño. **La parte 2** aplica las mejoras al final, en cambios propios de OpenSpec, y compara el resultado con la parte 1 para evidenciar las optimizaciones. Por eso la parte 1 debe quedar como línea base medible, aunque su diseño ya contempla esas mejoras (ver `design.md`, D11).

La Iteración 2 del curso pide aplicar estas técnicas a las historias de las iteraciones 1 y 2. El camino ya se validó y se midió en un Motorola Edge 30 y dos emuladores; el detalle y la evidencia están en `design.md`, sección "Mejoras futuras".

| # | Mejora | Camino validado | Cambio futuro |
|---|---|---|---|
| M1 | Micro-optimizaciones | Depurar los 48 avisos de lint, evitar trabajo repetido y analizar con SonarQube | `mejorar-desempeno` |
| M2 | Evitar ANR | Sacar del hilo principal el orden, los filtros y el arranque, y activar `StrictMode` en debug. Medido: sin riesgo real en el catálogo | `mejorar-desempeno` |
| M3 | Reducir memoria | Entregar release con R8 y reglas `-keep` (21 % menos memoria en el Motorola), ajustar Coil y mover la imagen de respaldo a Coil | `mejorar-desempeno` |
| M4 | Room | Caché local del catálogo con Room 2.6.1 y KSP, probado en los tres dispositivos | `add-cache-catalogo` |

La parte 1 no adelanta estas mejoras: sigue los patrones actuales de la app aunque la parte 2 vaya a cambiarlos, y no agrega despachadores, Room, R8 ni ajustes de Coil. Tampoco agrega avisos de lint nuevos.

## Objetivos de calidad

- Pruebas unitarias de toda la lógica nueva, escritas primero (repositorio, fecha, validación y ViewModels).
- **Diferido por decisión del usuario:** pruebas E2E (T07-5, #39), regresión (T07-6, #40) y pruebas instrumentadas nuevas. Mientras tanto, la navegación y la pantalla se verifican a mano en emulador y la HU no cumple el punto 3 de la definición de terminado del Sprint 2.

## Capabilities

### New Capabilities
- `crear-album`: formulario, validación, selector de fecha, envío, cancelación, manejo de errores y conservación al girar.

### Modified Capabilities
- `catalogo-albumes`: acceso a la creación solo para el Coleccionista y presentación del álbum recién creado. `sesion-usuario` no cambia: ya define que crear álbum es una acción exclusiva del coleccionista.

## Impact

- **Datos** (`data.remote`, `data.repository`, `data.model`): `AlbumApi.crearAlbum`, `CrearAlbumDto`, `AlbumRepository.crearAlbum`, modelo `AlbumNuevo`, listas de opciones y funciones de fecha.
- **Estado** (`viewmodel`): nuevo `CrearAlbumViewModel` con `CrearAlbumUiState`, validación pura y `AlbumsViewModel.albumCreado`.
- **Interfaz** (`ui.crear`, `ui.catalogo`, `ui.theme`): nueva `PantallaCrearAlbum`; botón "Crear álbum", aviso e insignia en `PantallaCatalogo`; encabezado con rol compartido; colores de error en el tema; textos en `strings.xml`; vectores `ic_crear_*` de Material Symbols (Apache 2.0).
- **Navegación** (`navigation`): ruta `crear-album` y resultado hacia el catálogo con `savedStateHandle`.
- **Manifiesto**: `windowSoftInputMode="adjustResize"` para que el teclado no tape los campos.
- **Pruebas**: unitarias nuevas. Las instrumentadas existentes deben seguir compilando sin cambios.
- **Dependencias**: ninguna nueva. El selector y las listas son APIs experimentales de Material 3 1.2.1, ya presente en el proyecto.
- **Backend**: sin cambios. Se comprobó contra BackVynils el 2026-11-05 (ver `design.md`).
- **Issues**: T07-2 (#36), T07-3 (#37) y T07-4 (#38). T07-7 (#41) queda pendiente hasta que se indique actualizar la wiki.
