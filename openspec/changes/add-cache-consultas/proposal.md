# Proposal

## Why

Una revisión del proyecto observó: "No veo manejo de caching, el livedata no es la mejor implementación de caching, sobretodo que si se quedan sin internet, no van a mostrar nada". Hoy solo el catálogo de álbumes (HU01, cambio `add-cache-catalogo`) se guarda en el dispositivo. El selector de coleccionistas, el detalle de álbum, el listado y el detalle de artistas y el estado de favorito dependen de la red en cada apertura: sin conexión, o con el backend de Render dormido o caído (respondió 500 el 13 de noviembre de 2026), muestran solo un error. La app no usa `LiveData`: los ViewModel exponen `StateFlow`, y el problema real es que no hay caché fuera del catálogo.

Este cambio extiende a esas pantallas el mismo patrón que ya funciona en el catálogo, sin cambiar la arquitectura adoptada. El plan del equipo ("Plan de caché con Room: Vinilos sin conexión", 13 de noviembre de 2026) lo reparte en cinco piezas; aquí se entregan todas en la rama `feature/cache-consultas`, con un commit por pieza.

## What Changes

- Se guarda en el dispositivo, con Room, la última respuesta válida de cada consulta de lectura:
  - **HU00:** el listado de coleccionistas.
  - **HU02:** el detalle de cada álbum del catálogo. `GET /albums` ya trae canciones, intérpretes y comentarios, así que el detalle se guarda con la consulta del catálogo, sin peticiones nuevas, y también al abrir un detalle con conexión.
  - **HU03:** el listado unificado de músicos y bandas.
  - **HU04:** el detalle de cada artista que el usuario abre. No se precargan los que nunca abrió.
  - **HU10:** los artistas favoritos confirmados de cada coleccionista.
- Al abrir cualquiera de esas pantallas con datos guardados, se muestran de inmediato con un indicador de actualización mientras se consulta el backend. Si la consulta falla, se conservan los datos guardados y se informa una vez que no se pudieron actualizar, en lugar de la pantalla de error. El error con "Reintentar" queda solo para cuando no hay nada guardado.
- Cada consulta exitosa reemplaza por completo lo guardado de esa pantalla, así que lo que se borró en el backend desaparece.
- Si el backend confirma con un 404 que un álbum ya no existe, también se borra lo guardado de ese álbum.
- Sin conexión, el botón de favorito muestra "Favorito" desactivado si el artista ya estaba confirmado como favorito. Si no lo estaba, sigue mostrando el error de comprobación actual, porque agregar requiere conexión y no se debe permitir agregarlo dos veces.
- Las escrituras siguen requiriendo conexión: no hay cola de envíos ni borradores. Crear un álbum (HU07) no se guarda aparte, porque la consulta del catálogo que le sigue ya actualiza lo guardado.
- Las imágenes siguen en la caché de disco de Coil; Room guarda solo sus URL.

## Capabilities

### New Capabilities

Ninguna. Guardar los datos de cada pantalla es un comportamiento de esa pantalla, igual que en `add-cache-catalogo`.

### Modified Capabilities
- `ingreso-plataforma`: cambian "Indicador de progreso al cargar coleccionistas" y "Error al cargar coleccionistas" (solo sin listado guardado) y se agrega "Coleccionistas guardados en el dispositivo".
- `detalle-album`: cambian "Indicador de carga" y "Error de conexión o servidor" (solo sin detalle guardado) y se agrega "Detalle de álbum guardado en el dispositivo", que también cubre el 404 con detalle guardado.
- `listado-artistas`: cambia "Estados de carga, vacío y error" (indicador y error solo sin listado guardado) y se agrega "Listado de artistas guardado en el dispositivo".
- `detalle-artista`: cambian "Indicador de carga" y "Error de carga con reintento" (solo sin detalle guardado, salvo el 404) y se agrega "Detalle de artista guardado en el dispositivo".
- `favoritos-artista`: cambia "Botón desactivado si falla la comprobación de favorito" (con favorito ya confirmado se muestra "Favorito") y se agrega "Favoritos confirmados guardados en el dispositivo".

`detalle-album`, `listado-artistas`, `detalle-artista` y `favoritos-artista` todavía no están en `openspec/specs/`: los introducen `add-detalle-album`, `add-listado-artistas`, `add-detalle-artista` y `add-artistas-favoritos`, que siguen sin archivar. Este cambio se archiva después de ellos para que sus deltas se apliquen en orden.

## Impact

- **Datos locales:** `VinilosDb` pasa de la versión 1 a la 2, con tablas nuevas para coleccionistas, detalle de álbum, artistas, detalle de artista y favoritos, nuevos DAO y conversores de Gson para las listas anidadas. Se exporta `app/schemas/.../2.json`. La migración es destructiva: lo guardado se borra una vez al actualizar la app y la siguiente consulta lo vuelve a llenar.
- **Repositorios:** las cuatro interfaces suman funciones para leer lo guardado. Cada `*RepositoryImpl` recibe su DAO opcional y guarda tras cada consulta exitosa. Sin DAO se comportan como hoy.
- **ViewModel y estados:** `CollectorsUiState`, `AlbumUiState`, `ArtistsUiState` y `ArtistaDetalleUiState` suman los indicadores `actualizando` y `errorAlActualizar` que ya usa `AlbumsUiState`, y sus ViewModel leen lo guardado antes de consultar. `FavoritoViewModel` usa lo guardado si falla la comprobación.
- **Pantallas y textos:** las cuatro pantallas muestran el indicador de actualización y un aviso breve, con cuatro textos nuevos en `strings.xml`.
- **Aplicación:** `AppContainer` recibe los DAO nuevos y `VinilosApplication` los obtiene de `VinilosDb`.
- **Pruebas:** DAO (instrumentadas), repositorios y ViewModel (unitarias), una E2E sin conexión por pantalla y la suite completa en API 23 y API 35. Los repositorios falsos de las pruebas implementan las funciones nuevas.
- **Dependencias:** ninguna nueva. Room 2.6.1 y Gson ya están en el proyecto.
- **Documentación:** README, `docs/arquitectura/plan-adopcion-room.md` y evidencia en `docs/pruebas/sprint-2/`.
- **Fuera de alcance:** escrituras sin conexión (crear álbum, agregar favorito) y colas de envío; precargar detalles que el usuario no abrió; expiración por tiempo; observar Room con `Flow` como fuente única; guardar la sesión entre cierres de la app; guardar imágenes en Room.
