# Design

## Context

Hoy `AppContainer` construye `OkHttp` y `Retrofit` y le pasa `CollectorApi` a `CollectorRepository`, que además hace el `try/catch` que traduce `IOException`, `HttpException` y `JsonParseException` a `ErrorDeCarga` y relanza `CancellationException`. `retrofit` es público en el contenedor pero nadie lo usa fuera de él. Las pruebas de errores viven en `CollectorRepositoryTest` (contra `MockWebServer`) y varios repositorios falsos (`CollectorsViewModelTest`, `androidTest/RepositorioFalso`) heredan de `CollectorRepository` pasándole un `CollectorApi` anónimo. Ver proposal.md para la motivación y `specs/acceso-backend/spec.md` para el comportamiento exigido.

## Goals / Non-Goals

**Goals:**
- Un solo punto que construye el cliente de red y traduce errores, reutilizable por los repositorios de HU01 a HU03.
- Que agregar una entidad nueva sea: una interfaz Retrofit + un repositorio que mapea DTO a modelo, sin código de red propio.
- Cero cambios de comportamiento visible en la HU00.

**Non-Goals:**
- Interfaz o abstracción del adaptador para intercambiar Retrofit por otra librería (no se justifica con un solo backend).
- Reintentos automáticos, caché, autenticación o interceptores de registro.
- Inyección de dependencias con Hilt/Koin: se mantiene el contenedor manual.

## Decisions

**1. `ServiceAdapter` como clase concreta en `data/remote`, con dos responsabilidades.**
`ServiceAdapter(baseUrl: String, cliente: OkHttpClient = clientePorDefecto())` crea el `Retrofit` (convertidor Gson) y expone:
- `fun <T> api(clase: Class<T>): T` (con una variante `inline fun <reified T> api()`), que entrega las interfaces por entidad.
- `suspend fun <T> llamar(bloque: suspend () -> T): Result<T>`, que ejecuta la llamada y traduce los fallos.

*Alternativas:* (a) un `object` singleton, como el adaptador típico del curso con Volley: descartado porque la URL base cambia en las pruebas (`MockWebServer`) y un singleton obliga a reiniciarlo entre pruebas; (b) una interfaz `ServiceAdapter` con implementación real y falsa: descartado por ahora (YAGNI), los repositorios falsos ya sustituyen a nivel de repositorio; (c) dejar la traducción en una función de extensión suelta: no da el punto único de configuración que pide la spec.

**2. La traducción de errores conserva la regla actual.**
`llamar` captura `IOException`, `HttpException` y `JsonParseException` y devuelve `Result.failure(ErrorDeCarga(causa))`; relanza `CancellationException` antes de los demás `catch` (es subtipo de `IllegalStateException`, no de `IOException`, pero se mantiene el orden por claridad). Cualquier otra excepción (error de programación) NO se captura: se prefiere que falle visible a que se confunda con "sin conexión".

**3. `ErrorDeCarga` se mueve a `data/remote`.**
Lo produce el adaptador; dejarlo en `data/repository` haría que `remote` dependa de `repository`, al revés de la dirección de dependencias (`repository` → `remote`). Es un cambio de paquete: se actualizan los imports de las pruebas.

**4. El repositorio recibe el adaptador, no la API.**
`CollectorRepository(adaptador: ServiceAdapter)` obtiene `private val api = adaptador.api<CollectorApi>()` y expone `obtenerColeccionistas() = adaptador.llamar { api.getCollectors() }.map { lista -> lista.mapNotNull { it.aModelo() } }`. Sigue siendo `open` para los repositorios falsos.
*Alternativa:* inyectar `CollectorApi` y `ServiceAdapter` por separado: más argumentos por repositorio y el repositorio podría saltarse el adaptador.

**5. Los repositorios falsos construyen un `ServiceAdapter` que nunca llama a la red.**
`CollectorsViewModelTest.RepositorioFalso` y `androidTest/RepositorioFalso` pasan `ServiceAdapter("http://localhost/")`; Retrofit solo crea el proxy y no abre conexiones hasta que se invoca un método, y los falsos sobrescriben `obtenerColeccionistas()`.

**6. `AppContainer` crea una sola instancia y deja de exponer `retrofit`.**
`val serviceAdapter = ServiceAdapter(baseUrl)` y `collectorRepository = colectoresRepositorio ?: CollectorRepository(serviceAdapter)`. Los timeouts de 10 s pasan al cliente por defecto del adaptador. `RecuperacionTest`, `RotacionTest` e `IngresoE2ETest` no cambian porque siguen usando `AppContainer(baseUrl = ...)` o `AppContainer(colectoresRepositorio = ...)`.

**7. Pruebas.**
- `ServiceAdapterTest` (`MockWebServer`, cliente con timeouts de 300 ms): éxito, lista vacía, 404, 500, sin conexión (`DISCONNECT_AT_START`), tiempo agotado, JSON inválido y cancelación relanzada. Se prueba con un `llamar` sobre una interfaz mínima de la prueba y sobre `CollectorApi`.
- `CollectorRepositoryTest` conserva: mapeo de datos, DTO incompletos descartados, campos desconocidos ignorados, y un caso de propagación del error del adaptador.
- Los 43 tests unitarios actuales se redistribuyen; el total debe mantenerse en 43 o más.

**8. Documentación.**
README: la línea de `data/` en "Estructura" lista `ServiceAdapter` y `ErrorDeCarga` bajo `remote`, y el párrafo "Arquitectura" pasa de "solo el repositorio habla con la API" a "los repositorios hablan con el backend solo a través del `ServiceAdapter`, que traduce los errores". La spec `acceso-backend` se sincroniza al archivar.

## Risks / Trade-offs

- [Un `ServiceAdapter` sin interfaz acopla los repositorios a una clase concreta] → es una sola clase pequeña y los repositorios falsos ya cubren la sustitución; se extrae interfaz si algún día hace falta.
- [Los falsos construyen un adaptador "de mentira" con URL ficticia] → no hay red hasta invocar un método; si un falso lo invocara por error fallaría con `ErrorDeCarga`, visible en la prueba.
- [Mover `ErrorDeCarga` rompe imports de pruebas] → cambio mecánico; compilar `testDebugUnitTest` y `compileDebugAndroidTestKotlin` lo detecta.
- [No capturar excepciones genéricas puede dejar pasar un crash por un error inesperado] → intencional (ver decisión 2); la spec solo exige traducir fallos de red, servidor y respuesta ilegible.

## Migration Plan

Refactor interno sin cambios de datos ni de API del backend:
1. Crear `ServiceAdapter` y `ServiceAdapterTest` con los casos de error trasladados (compilan y pasan).
2. Mover `ErrorDeCarga`, migrar `CollectorRepository` y `AppContainer`; ajustar fakes e imports.
3. Correr unitarias, lint, `compileDebugAndroidTestKotlin` y, en el dispositivo, `RecuperacionTest` y las instrumentadas sin E2E.
4. Documentar en README.
Reversión: revertir los commits; no hay migraciones.
