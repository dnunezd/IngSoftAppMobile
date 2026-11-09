# Design

> **Nota de actualización (post-implementación):** HU04 se fusionó a `develop` después de escrito este diseño y reemplazó `PantallaArtistaProvisional` por el detalle real (`PantallaDetalleArtista`). La integración de HU10 se migró a ese archivo sin cambiar el contrato (`FavoritoUiState`/`FavoritoViewModel` no cambiaron). Además, a pedido del usuario, la sección de favorito se rediseñó visualmente según mockups (botón en píldora con ícono, nota de bloqueo temporal, tarjetas de error/no-disponible) — ver `tasks.md`, sección 6. El contexto original de esta sección se conserva sin editar para no perder la motivación histórica de las decisiones.

## Context

El detalle de artista todavía no existe como tal (HU04 no está implementada); hoy la navegación lleva a `PantallaArtistaProvisional`, que solo recibe `nombre` y `tipo` desde la ruta `Rutas.DetalleArtista` (`artista/{tipo}/{id}?nombre={nombre}`) y no usa el `id` que ya viaja en la navegación. HU10 debe agregar el botón de favorito ahí, de forma explícitamente temporal.

El backend (`BackVynils`) ya expone los tres endpoints necesarios:
- `GET /collectors/{collectorId}/performers` → lista de performers favoritos del coleccionista (campos `id, name, image, description`; sin `type`). 404 si el coleccionista no existe.
- `POST /collectors/{collectorId}/bands/{bandId}` y `POST /collectors/{collectorId}/musicians/{musicianId}` → ambos ejecutan la misma lógica de asociación sobre un repositorio compartido de `Performer`; 404 si el coleccionista o el performer no existen, 200 con el performer si se guarda.

Dato clave verificado en el backend: `Performer` usa herencia de tabla única (TypeORM `@TableInheritance` + `@ChildEntity` en `Musician`/`Band`), por lo que los ids de músicos y bandas son globalmente únicos entre sí (comparten una sola secuencia). Esto permite comparar solo por `id` al determinar si un artista ya es favorito, sin necesitar el par `(tipo, id)` que otras partes del código (comentario en `Artist.kt`) asumen por precaución para el listado, pero que no aplica aquí.

Ver proposal.md (Why / What Changes) para la motivación completa y specs/favoritos-artista/spec.md para los requisitos.

## Goals / Non-Goals

**Goals:**
- Permitir comprobar y guardar el favorito de un artista desde `PantallaArtistaProvisional`, reutilizando los patrones ya establecidos en el proyecto (ServiceAdapter, excepción dedicada para 404, UiState sellado con reintento y protección contra respuestas tardías).
- Dejar explícito en código y specs que la integración es sobre la pantalla provisional de HU03, no sobre un detalle nuevo.

**Non-Goals:**
- No se construye el detalle real de artista (HU04).
- No se agregan pruebas E2E (responsabilidad de otro integrante del equipo).
- No se define el diseño visual detallado del botón/mensajes (T10-1, a cargo de diseño); este design solo fija los estados y el contrato de datos.

## Decisions

### Dónde viven los endpoints Retrofit: `CollectorApi`
Los tres endpoints cuelgan de `/collectors/{id}/...`, igual que el único método que ya existe en `CollectorApi` (`GET /collectors`). Se agregan ahí tres métodos nuevos en vez de extender `ArtistApi` (que solo conoce `/musicians` y `/bands`, sin noción de coleccionista) o crear un archivo nuevo. Alternativa descartada: una interfaz `CollectorPerformerApi` separada; se prefiere `CollectorApi` por cohesión de recurso y porque evita un archivo adicional para 3 métodos.

### Nuevo repositorio `FavoriteRepository`, no extender `ArtistRepository` ni `CollectorRepository`
`ArtistRepository` resuelve el catálogo de artistas (HU03) y `CollectorRepository` resuelve la selección de coleccionista (HU01); ninguno es dueño de la relación coleccionista-favorito. Se crea `FavoriteRepository(adaptador: ServiceAdapter)` con:
- `suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean>` (mapea la lista de `GET .../performers` a un `Boolean` comparando por `id`).
- `suspend fun agregarFavorito(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit>` (elige el endpoint POST según `tipo`).

Mismo patrón que `AlbumRepository`: el resultado 404 se traduce a una excepción dedicada `ArtistaNoDisponible`, distinta de `ErrorDeCarga`, revisando `(resultado.exceptionOrNull() as? ErrorDeCarga)?.cause as? HttpException)?.code() == 404` después de `adaptador.llamar { ... }`.

### Estado del botón: un único `FavoritoUiState` sellado por ViewModel
Se crea `FavoritoViewModel(collectorId: Int, artistId: Int, tipo: ArtistType, repositorio: FavoriteRepository)` con `FavoritoUiState`:
- `Comprobando` – estado inicial, botón inactivo, sin texto de error.
- `ErrorComprobacion` – la consulta de favoritos falló; botón inactivo + opción de reintentar (`reintentarComprobacion()`).
- `NoFavorito` – botón "Agregar Favorito" activo.
- `Guardando` – botón "Guardando..." inactivo.
- `Favorito(mostrarConfirmacion: Boolean)` – botón "Favorito" inactivo; `mostrarConfirmacion` sigue el mismo patrón que `AlbumsUiState`/`avisoMostrado()`: la pantalla muestra el mensaje transitorio y llama a `confirmacionMostrada()` para apagarlo (temporización de "desaparece solo" en la capa de UI, no en el ViewModel).
- `ErrorGuardado` – mensaje "No se pudo guardar el favorito, intenta de nuevo"; botón vuelve a "Agregar Favorito" activo.
- `NoDisponible` – mensaje "El artista ya no está disponible"; la pantalla ofrece volver al listado (navegación ya existente, `popBackStack`), que al recargar vuelve a consultar `ArtistRepository.obtenerArtistas()` y por lo tanto ya no incluye el artista eliminado, sin lógica adicional.

> **Nota de actualización (revisión de PR):** el párrafo anterior era incorrecto. `popBackStack()` por sí solo NO vuelve a consultar el listado: `ArtistsViewModel` solo llama a `obtenerArtistas()` en su `init`, así que al regresar se reutiliza la instancia de ViewModel ya existente en la pila y se conserva la lista anterior (con el artista ya eliminado todavía visible). La corrección agrega un callback `onVolverAlListado` propio en `PantallaDetalleArtista` (distinto de `onAtras`) que, en `VinilosNavHost`, marca una bandera `ClaveArtistaEliminado` en el `SavedStateHandle` de la entrada anterior de la pila (mismo patrón que `ClaveAlbumCreado` de HU07); el destino `Rutas.Artistas` observa esa bandera y llama a `ArtistsViewModel.reintentar()` para forzar la nueva consulta antes de limpiarla.

Se reutiliza el contador `consultaVigente`/`Job?` de `AlbumViewModel` para ambas operaciones asíncronas (comprobar y guardar), evitando que una respuesta tardía de "comprobar" sobrescriba un "guardando" en curso, o viceversa.

### Visibilidad del botón: decisión en la capa de navegación/composición, no en el ViewModel
`Session.collectorActionsEnabled` ya determina Visitante vs. Coleccionista. `VinilosNavHost` ya colecta `sessionViewModel.session` en otras rutas (Catálogo, Inicio); se hace lo mismo en la ruta de detalle de artista: si `session.collectorActionsEnabled` es `false`, no se crea el `FavoritoViewModel` ni se renderiza el componente del botón (evita una llamada de red innecesaria para el Visitante, no solo ocultar visualmente).

### Comparación de favorito solo por `id`
Dado el hallazgo de ids globalmente únicos entre músicos y bandas en el backend real, `esFavorito` compara únicamente por `id` contra la lista de `GET /collectors/{id}/performers` (que no trae `type`). No se requiere lógica adicional de `(tipo, id)`.

## Risks / Trade-offs

- [La lista de favoritos (`GET .../performers`) no indica `type`] → No afecta a HU10 porque la comparación es solo por `id` (ver Decisión anterior); si una futura HU necesitara el tipo de cada favorito, se resolvería cruzando contra el catálogo ya cargado.
- [Dos POST distintos para banda/músico con la misma lógica de backend] → El repositorio decide el endpoint a partir del `tipo` que ya viaja en la navegación (`Rutas.DetalleArtista`), sin necesitar detectar el tipo desde la respuesta.
- [Mensaje de confirmación con el nombre del artista] → El ViewModel no necesita volver a consultar el nombre: ya lo recibe como parámetro (igual que hoy `PantallaArtistaProvisional` lo recibe de la navegación), evitando una dependencia adicional del DTO de respuesta del POST.
- [Doble toque mientras se guarda] → Cubierto por el estado `Guardando` (botón inactivo) más el `Job?` cancelable, igual que en `AlbumViewModel`.

## Open Questions

Ninguna; T10-1 (diseño visual) y HU04 (detalle real) quedan fuera de alcance de este change, según decisión ya confirmada con el usuario.
