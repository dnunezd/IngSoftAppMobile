# Proposal

## Why

El curso pide adoptar Room en la aplicación, y hoy Vinilos no guarda nada en el dispositivo: cada apertura del catálogo depende de `GET /albums`. El backend de los tutores corre en un plan gratuito de Render que se duerme, y `ServiceAdapter` corta cada consulta a los 10 s, así que la primera apertura del catálogo con el servidor dormido termina en "No pudimos cargar el catálogo". Room como caché de solo lectura del catálogo resuelve ese problema en la pantalla principal de la app, con un alcance que se puede terminar y medir antes del cierre del Sprint 2 (15 de noviembre de 2026).

El análisis completo, con la comparación de las otras partes de la app, está en `docs/arquitectura/plan-adopcion-room.md`. Este cambio es la mejora M4 que `add-crear-album` dejó registrada en su `design.md` ("Mejoras futuras").

## What Changes

- Guarda en el dispositivo, con Room 2.6.1, la última respuesta completa y válida de `GET /albums`. Cada consulta exitosa reemplaza lo guardado.
- Al abrir el catálogo con álbumes guardados, los muestra de inmediato con el indicador de actualización mientras consulta el backend. Si la consulta falla, conserva los álbumes guardados y muestra el aviso que ya existe, "No pudimos actualizar el catálogo", en lugar de la pantalla de error.
- La pantalla de error "No pudimos cargar el catálogo" con "Reintentar" queda solo para cuando no hay nada guardado y la consulta falla.
- Deslizar para actualizar y la consulta que sigue a crear un álbum (HU07) también actualizan lo guardado. El formulario de HU07 y la insignia "NUEVO" no cambian.
- Las portadas no se guardan en Room: se guarda su URL, y las imágenes siguen en la caché de memoria y de disco de Coil. Se declara de forma explícita la caché de disco de Coil con los valores que Coil usa por defecto.
- El backend sigue siendo la fuente de verdad. Room no recibe escrituras propias de la app, y crear un álbum sigue necesitando conexión.

## Capabilities

### New Capabilities

Ninguna. Guardar el catálogo es un comportamiento del catálogo, no una capacidad aparte.

### Modified Capabilities
- `catalogo-albumes`: cambian "Error de carga recuperable" (el error solo aparece sin catálogo guardado) e "Indicador de carga" (el indicador de pantalla completa solo aparece sin catálogo guardado), y se agrega "Catálogo guardado en el dispositivo", con el comportamiento sin conexión y el de las portadas.

## Impact

- **Dependencias**: Room 2.6.1 (`room-runtime`, `room-ktx`, `room-compiler`) y el plugin KSP `1.9.0-1.0.13`, compatibles con Kotlin 1.9.0, AGP 8.6.0 y Gradle 8.7. El APK debug crece unos 0,15 MB (10,24 a 10,39 MB en la validación previa).
- **Datos**: nuevo paquete `data.local` (`AlbumEntity`, `AlbumDao`, `VinilosDb`) y esquema exportado en `app/schemas/`. `AlbumRepository` recibe un `AlbumDao` opcional; `obtenerAlbum` y `crearAlbum` no cambian.
- **Estado**: `AlbumsViewModel` cambia solo su carga inicial. `AlbumsUiState` no cambia.
- **Aplicación**: `VinilosApplication` crea la base de datos en `onCreate` y declara la caché de disco de Coil. `AppContainer` recibe el DAO.
- **Pruebas**: nuevas unitarias de repositorio y ViewModel, una instrumentada del DAO y una E2E sin conexión. Las pruebas existentes (`RecuperacionTest`, `CatalogoE2ETest` y el resto) crean su propio `AppContainer` sin DAO, así que no deberían cambiar.
- **Otros cambios**: `add-crear-album` aún no está archivado y también tiene un delta sobre `catalogo-albumes`. Sus requisitos no se tocan aquí, pero conviene archivarlo antes que este cambio para que los deltas se apliquen en orden.
- **Documentación**: README, wiki y la sección M4 de `add-crear-album/design.md`, que dice que Room no está en el enunciado de la Iteración 2.
- **Fuera de alcance**: caché del detalle de álbum, artistas, coleccionistas o favoritos; crear álbumes sin conexión, colas de envío o borradores; guardar imágenes en Room o descargar portadas por adelantado; ajustar la caché de memoria de Coil (mejora M3); observar la base con `Flow`.
