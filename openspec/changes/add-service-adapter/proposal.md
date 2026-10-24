# Proposal

## Why

La app consume un único backend (BackVynils) con varios endpoints (`/collectors`, `/albums`, `/musicians`, `/bands`). Hoy cada repositorio recibe directamente su interfaz de Retrofit (`CollectorRepository(CollectorApi)`), y la traducción de errores de red (`IOException`, `HttpException`, `JsonParseException` a `ErrorDeCarga`, con la cancelación relanzada) vive dentro de `CollectorRepository`. HU01 a HU03 agregarán repositorios de álbumes y artistas: sin un punto común, cada uno repetiría esa lógica y la forma de construir y compartir el cliente de red. Conviene definirlo ahora, con un solo repositorio que migrar, y no cuando haya cuatro.

## What Changes

- Nueva clase `ServiceAdapter` en `data/remote`: única clase que conoce el backend. Recibe la URL base, construye `OkHttp` y `Retrofit` (hoy lo hace `AppContainer`) y entrega las interfaces por entidad (`CollectorApi`, luego `AlbumApi`, etc.).
- El `ServiceAdapter` expone un método genérico `llamar { api.x() }: Result<T>` que centraliza la traducción de errores a `ErrorDeCarga` y relanza `CancellationException`.
- `ErrorDeCarga` pasa de `data/repository` a `data/remote`, junto al adaptador que lo produce.
- `CollectorRepository` recibe el `ServiceAdapter` en vez de `CollectorApi` y se queda solo con convertir DTO a modelo.
- `AppContainer` crea una única instancia de `ServiceAdapter` y se la pasa a todos los repositorios.
- Las pruebas unitarias de errores (sin conexión, tiempo agotado, 404, 500, JSON inválido, cancelación) pasan de `CollectorRepositoryTest` a una nueva `ServiceAdapterTest` con `MockWebServer`; `CollectorRepositoryTest` conserva el mapeo de datos.
- Se documenta el patrón en el README (sección de estructura y arquitectura) y se actualiza la lista de piezas de la capa de datos.
- Sin cambios visibles para el usuario: los textos, estados y flujos de la HU00 no cambian.

## Capabilities

### New Capabilities
- `acceso-backend`: cómo la app accede al backend BackVynils: punto único de acceso, traducción uniforme de errores de red y del servidor, cancelación de corrutinas y reutilización por todos los repositorios.

### Modified Capabilities

(Ninguna: los requisitos de `ingreso-plataforma`, `pantalla-inicio` y `sesion-usuario` no cambian; el error de carga que ve la persona es el mismo.)

## Impact

- Código: `data/remote` (nuevo `ServiceAdapter`, `ErrorDeCarga` movido), `data/repository/CollectorRepository`, `AppContainer`, y los imports de `ErrorDeCarga` en `CollectorsViewModelTest` y `VinilosNavHostTest` (el código de producción de `ViewModel` y navegación no lo referencia).
- Pruebas: `CollectorRepositoryTest` se divide con `ServiceAdapterTest`; los repositorios falsos de `CollectorsViewModelTest` y `androidTest/RepositorioFalso` pasan a construir el repositorio con un `ServiceAdapter` en lugar de un `CollectorApi` anónimo; `RecuperacionTest` y `RotacionTest` siguen usando `AppContainer` sin cambios de contrato.
- Documentación: README y la nueva spec `acceso-backend`.
- Sin dependencias nuevas ni cambios en el backend. Habilita que HU01 a HU03 reutilicen el adaptador al añadir sus repositorios.
- Fuera de alcance: crear `AlbumApi`/`MusicianApi` o sus repositorios (son de HU01 a HU03), cachés o reintentos automáticos, y cambios en la URL base o en la política de HTTP sin cifrar.
