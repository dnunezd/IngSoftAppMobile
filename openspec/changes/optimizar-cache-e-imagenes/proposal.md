# Proposal

## Why

El informe de oportunidades de mejora del perfilamiento (línea base «Before» del Sprint 2) identificó tres costos repetidos que este cambio corrige como optimización, sin funciones nuevas:

- **Punto 1.** Cada portada y cada avatar del detalle de álbum decodifican como imagen de espera una ilustración de Inicio de 852×480 px, aunque la imagen real cargue bien.
- **Punto 7.** Cada consulta exitosa reescribe en Room todo lo guardado, aunque no haya cambiado. Además, después de crear un álbum, el catálogo espera un `GET /albums` completo para mostrarlo, aunque el `POST` ya devolvió el álbum.
- **Punto 11.** El cliente de la API no tiene caché HTTP. Cada consulta repetida descarga el cuerpo completo, aunque los 9 endpoints `GET` que la app guarda en Room envían `ETag` y responden 304 cuando reciben `If-None-Match`, como se verificó en el backend local.

Es la parte 2 que anunció `add-crear-album`: las mejoras se aplican en cambios propios y se comparan con la línea base medida.

## What Changes

- **Imagen genérica liviana (punto 1).** La portada genérica pasa a ser el ícono de disco (`ic_cat_album`) sobre fondo neutro, el mismo patrón de las miniaturas del detalle de artista (HU04). Se usa en el catálogo, el detalle de álbum y la vista previa de crear álbum. La imagen genérica de los artistas del detalle de álbum pasa a ser `ic_artista_por_defecto`, como en HU03 y HU04. Inicio conserva sus ilustraciones.
- **Caché HTTP (punto 11).** El cliente de la API usa una caché de disco de 10 MiB en `cacheDir/http`. Una consulta repetida envía la `ETag` guardada; si nada cambió, el backend responde 304 sin cuerpo y la app usa la respuesta guardada. Toda respuesta se revalida con el backend antes de usarse, así que sin conexión el comportamiento no cambia: sigue mostrando lo guardado en Room.
- **Escribir en Room solo lo que cambió (punto 7):**
  - Los repositorios reciben la `ETag` de cada respuesta. Si coincide con la de lo último que se guardó, no se compara ni se escribe nada.
  - Si la `ETag` cambió o no viene, se compara la respuesta con una copia en memoria de lo último que se escribió.
  - Solo se escriben las filas nuevas o modificadas y se borran las que ya no llegan.
  - Aplica a catálogo y detalles de álbum, listado y detalle de artistas, coleccionistas y favoritos.
- **Álbum creado sin esperar la consulta (punto 7).** El álbum que devuelve `POST /albums` se guarda en Room. Al volver al catálogo, este aparece de inmediato desde lo guardado y el `GET /albums` revalida, con el mismo indicador y el mismo aviso de error que hoy. Esto revisa la decisión de `add-cache-consultas` de no guardar el álbum creado.
- **Tipos de Gson creados una sola vez (punto 7).** `Conversores` deja de crear un `TypeToken` en cada lectura.

## Capabilities

### New Capabilities
- `cache-consultas`: cómo se mantiene al día lo que la app guarda en el dispositivo. Lo guardado refleja la última respuesta; una respuesta sin cambios no escribe; un fallo al guardar se reintenta; el álbum creado se guarda. Hasta hoy, cada pantalla define qué guarda. Esta capacidad reúne las reglas comunes a todas.

### Modified Capabilities
- `acceso-backend`: se agrega la revalidación de las consultas repetidas con la caché HTTP. También se agrega que una respuesta guardada nunca se usa sin la confirmación del backend.
- `catalogo-albumes`: se agrega «Álbum creado visible sin esperar la consulta».

`acceso-backend` todavía no está en `openspec/specs/`: la introducen `add-service-adapter` y `add-detalle-album`, sin archivar. La revisión de «Mostrar el álbum recién creado» viene de `add-crear-album`, que tampoco está archivado. Por eso se agrega un requisito nuevo en vez de modificar ese. Este cambio se archiva después de esos tres y de `add-cache-consultas`.

El punto 1 no cambia requisitos: el catálogo, el detalle de álbum y el de artista siguen mostrando una imagen genérica cuando la imagen falta o no carga. Solo cambia cuál es esa imagen.

## Impact

- **Base:** `develop` con `add-cache-consultas` (#93). El `develop` local está 4 commits atrás de `origin/develop` y hay que actualizarlo antes de implementar.
- **UI:** `PortadaAlbum.kt`, `ContenidoAlbum.kt` (`FilaArtista`) y sus pruebas (`PortadaAlbumTest`, `ContenidoAlbumTest`).
- **Red:** `ServiceAdapter` (caché, revalidación forzada y `ETag` de la respuesta) y `AlbumApi`, `ArtistApi` y `CollectorApi`, cuyos 9 `GET` pasan a devolver `Response<...>`. También `AppContainer` y `VinilosApplication`.
- **Datos locales:**
  - `AlbumDao`, `ArtistaDao`, `ColeccionistaDao` y `FavoritoDao` suman escrituras incrementales con `@Upsert` y borrado por ids.
  - `Conversores` declara sus tipos como constantes.
  - Sin cambio de esquema: `VinilosDb` sigue en la versión 2.
- **Repositorios:** los cuatro `*RepositoryImpl` mantienen una copia en memoria y la versión de lo escrito. `AlbumRepositoryImpl.crearAlbum` guarda el álbum creado. Las interfaces no cambian, así que los repositorios falsos de las pruebas no se tocan.
- **ViewModel:** `AlbumsViewModel.albumCreado`.
- **Pruebas:**
  - Unitarias de `ServiceAdapter` y de los cuatro repositorios, con `MockWebServer` y cabeceras `ETag`.
  - Instrumentadas de los DAO y de `PortadaAlbum`.
  - Suite completa y perfilamiento «After» en el Samsung, con la guía manual (`docs/performance/guia-manual.md`).
- **Dependencias:** ninguna nueva. OkHttp 4.12.0 trae `Cache`, y Room 2.6.1 trae `@Upsert`.
- **Fuera de alcance:** los puntos 2 a 6, 8 a 10 y 12 del informe; saltarse la conversión de la respuesta cuando no cambió; expiración por tiempo; cambiar el comportamiento sin conexión.
