# Design

## Context

Ver proposal.md (Why). Las E2E existentes (`IngresoE2ETest`, `CatalogoE2ETest`, `AlbumDetalleE2ETest`, `ArtistasE2ETest`) arrancan `MainActivity` con `createEmptyComposeRule`, reemplazan repositorios en `VinilosApplication.container` y guardan evidencia con `EvidenciaE2E.capturar(rule, carpeta, paso)`. Desde #79 no asumen los datos de ejemplo de BackVynils, porque el backend desplegado de los tutores es compartido y sus datos cambian.

Datos observados el 2026-11-07: en el backend desplegado, Rubén Blades (músico 100) tiene 5 álbumes y 1 premio, Adele (músico 2) no tiene álbumes ni premios, y Queen (banda 101) tiene álbumes y premio pero ningún integrante. Los datos de ejemplo del backend local tampoco tienen bandas con integrantes.

## Goals / Non-Goals

**Goals:**
- Cubrir con E2E los criterios de la HU04 que dependen del flujo completo: acceso desde el listado, datos del músico y de la banda, álbumes con navegación a la HU02, premios, secciones vacías, imagen por defecto, error con reintento y rotación.
- Que las pruebas pasen contra el backend local y contra el desplegado, sin cambiar el código de la app.

**Non-Goals:**
- Pruebas de regresión (no se harán para esta historia).
- El indicador de carga: contra el backend real la respuesta es demasiado rápida para observarlo de forma estable; lo cubren el ViewModel y las pruebas de UI.
- Pruebas de UI de `PantallaDetalleArtista` (observación de la revisión de #82, fuera de la T04-5).

## Decisions

**D1. Elegir el artista a partir de los datos reales.** Antes de abrir la app, la prueba consulta con un `ArtistRepository` real el listado y el detalle de los artistas, y elige el que cumple la condición del escenario (por ejemplo, un músico con álbumes, o un artista sin álbumes ni premios). Si no existe, el escenario se omite con `Assume`. *Alternativa:* fijar Rubén Blades y Queen por id; se descarta porque otro equipo puede cambiar esos datos.

**D2. Repositorio real que cuenta las consultas del detalle.** Un `ArtistRepository` que llama al real y cuenta `obtenerDetalle`, para comprobar que volver desde el álbum y girar la pantalla no repiten la consulta. Es el mismo patrón de `ArtistasReales` en FN03 y `AlbumesRealesContados` en FN01.

**D3. `MockWebServer` para lo que el backend real no puede mostrar.** ESC05 corta la conexión de `GET /musicians/{id}` hasta que la prueba "levanta" el servidor; ESC06 responde una banda con dos integrantes sin imagen. *Alternativa:* crear una banda con integrantes en el backend local con `POST`; se descarta porque deja datos y no funcionaría igual en el desplegado.

**D4. Navegar como el usuario.** Todos los escenarios llegan al detalle desde el inicio del Visitante y el listado de artistas (desplazando la lista hasta la fila), porque el primer criterio de la HU04 es el acceso desde la HU03.

**D5. Evidencia en el repositorio.** Cada escenario escribe en su carpeta `FN04_ESCxx`. Tras la corrida se traen con `adb pull` y se versionan solo los PDF en `docs/pruebas/sprint-2/HU04/e2e/` (las capturas sueltas quedan dentro del PDF), con un README de trazabilidad.

## Risks / Trade-offs

- [Otro equipo cambia los datos durante la corrida] → Las pruebas leen los datos justo antes de cada escenario; el riesgo queda acotado a cambios en esos segundos.
- [El backend desplegado está dormido] → Despertarlo antes con `curl`; `candidatosReales` consulta antes de abrir la app, así que la primera respuesta lenta no cae dentro de una espera de la UI.
- [Un escenario se omite por falta de datos] → La omisión queda visible en el resultado (`Assume`). Con los datos de ejemplo del backend local (solo Rubén Blades y Queen, ambos con álbumes) se omite FN04-ESC04; por eso la evidencia oficial se genera contra el backend desplegado, que no se modifica porque estas pruebas solo consultan.
