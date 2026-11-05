# Proposal

## Why

HU10 pide que un coleccionista pueda marcar artistas (músicos o bandas) como favoritos desde su detalle, y que ese estado persista entre aperturas de la app. Hoy no existe ningún consumo de los endpoints de favoritos del backend ni un botón de favorito en la UI; sin esto, los coleccionistas no tienen forma de expresar ni consultar sus gustos musicales.

## What Changes

- Agregar el consumo de `GET /collectors/{id}/performers`, `POST /collectors/{id}/bands/{bandId}` y `POST /collectors/{id}/musicians/{musicianId}` (repositorio + API Retrofit), distinguiendo 404 ("ya no existe") de errores de red/servidor, con sus pruebas unitarias.
- Agregar un ViewModel/estado de favorito con los casos: `Agregar Favorito`, `Guardando...` (deshabilitado), `Favorito` (deshabilitado), error de verificación inicial con reintento, y error de guardado con reintento; protegido contra respuestas tardías y doble toque.
- Integrar el botón de favorito en la pantalla provisional de detalle de artista (`PantallaArtistaProvisional`, el placeholder de HU03), marcado explícitamente como temporal hasta que exista el detalle real de HU04. No se crea una pantalla de detalle nueva.
- El botón solo se muestra si la sesión es de Coleccionista (reutiliza `Session.collectorActionsEnabled`); para Visitante no aparece.
- Agregar los mensajes al usuario: confirmación transitoria al guardar ("<nombre> se agregó a tus favoritos"), "El artista ya no está disponible" con botón para volver al listado (y refrescarlo sin ese artista), y "No se pudo guardar el favorito, intenta de nuevo" con reintento.
- Fuera de alcance de este change: la pantalla real de detalle de artista (HU04), las pruebas E2E (responsabilidad de otro integrante) y el diseño visual detallado (T10-1, a cargo de diseño).

## Capabilities

### New Capabilities
- `favoritos-artista`: consulta y registro de artistas favoritos de un coleccionista (verificación de estado, guardado, estados del botón y mensajes al usuario) desde el detalle de artista.

### Modified Capabilities
(ninguna; no se cambian requisitos de `listado-artistas` ni de otras capacidades existentes)

## Impact

- Nuevo: modelo/DTO de favorito o reuso de `Artist`/`Performer`, métodos Retrofit para los 3 endpoints (en `ArtistApi` o un nuevo API sibling, a decidir en design.md), un repositorio (nuevo o extensión de `ArtistRepository`), un ViewModel de favorito y su `UiState`, y las pruebas unitarias correspondientes.
- Modificado: `PantallaArtistaProvisional.kt` (agrega el botón y los mensajes), y el wiring en `VinilosNavHost.kt` para pasar el id del coleccionista (desde `SessionViewModel`/`Session.Collector`) al nuevo ViewModel.
- Sin cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`, `MainActivity.kt` ni `ServiceAdapter.kt`; no se agregan dependencias nuevas (coroutines ya están disponibles en el proyecto).
