# Tasks

## 1. ServiceAdapter y sus pruebas

- [x] 1.1 Crear `ServiceAdapter` en `data/remote` (URL base, cliente `OkHttp` con timeouts de 10 s por defecto, Retrofit con Gson, `api(...)` y `llamar { }: Result<T>`) y mover `ErrorDeCarga` a `data/remote`; verificar con `./gradlew compileDebugKotlin`.
- [x] 1.2 Crear `ServiceAdapterTest` con `MockWebServer` (cliente de 300 ms) que cubra éxito, lista vacía, 404, 500, sin conexión, tiempo agotado, JSON inválido, cancelación relanzada y reintento tras un fallo; verificar con `./gradlew testDebugUnitTest --tests "*ServiceAdapterTest"` en verde.

## 2. Migrar repositorio y contenedor

- [x] 2.1 Cambiar `CollectorRepository` para recibir el `ServiceAdapter`, usar `llamar { api.getCollectors() }` y quedarse solo con el mapeo DTO a modelo; verificar con `./gradlew compileDebugKotlin`.
- [x] 2.2 Ajustar `AppContainer` para crear una única instancia de `ServiceAdapter`, compartirla con los repositorios y dejar de exponer `retrofit`; verificar que `RecuperacionTest` y `RotacionTest` compilan sin cambios de contrato (`./gradlew compileDebugAndroidTestKotlin`).
- [x] 2.3 Reducir `CollectorRepositoryTest` al mapeo (datos, campos desconocidos, elementos incompletos descartados) más un caso de propagación del error del adaptador, y actualizar los imports de `ErrorDeCarga`; verificar con `./gradlew testDebugUnitTest` en verde con al menos 43 pruebas.
- [x] 2.4 Adaptar los repositorios falsos (`CollectorsViewModelTest`, `androidTest/RepositorioFalso`) y `VinilosNavHostTest` al nuevo constructor y al nuevo paquete de `ErrorDeCarga`; verificar con `./gradlew testDebugUnitTest compileDebugAndroidTestKotlin`.

## 3. Documentación

- [x] 3.1 Actualizar el README (sección Estructura y párrafo de Arquitectura) para describir `ServiceAdapter` y que los repositorios solo hablan con el backend a través de él; verificar que el texto coincide con el código y que el conteo de pruebas unitarias del README sigue siendo correcto.

## 4. Verificación integrada

- [x] 4.1 Correr `./gradlew lint testDebugUnitTest` y verificar 0 errores de lint y todas las unitarias en verde.
- [x] 4.2 En el Samsung, correr `RecuperacionTest` y las instrumentadas sin E2E (`notClass=com.example.vinilos.IngresoE2ETest`) y verificar que pasan igual que antes del cambio.
- [x] 4.3 Con backend y `adb reverse`, correr `IngresoE2ETest` y verificar que el selector de coleccionistas carga los datos reales (el adaptador funciona contra BackVynils).

## Workflow follow-up

- Sincronizar la spec `acceso-backend` y archivar el cambio cuando el equipo revise la PR.
- Hacer commits separados por grupo (adaptador, migración, docs) y subirlos a la PR #64 o a una PR propia hacia `develop`.
