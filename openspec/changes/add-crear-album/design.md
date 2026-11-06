# Design

## Context

`develop` ya tiene el catálogo (HU01), el detalle (HU02), la sesión con `collectorActionsEnabled` y `ServiceAdapter.llamar`. No hay ninguna pantalla con formulario ni selector de fecha.

Comportamiento de BackVynils comprobado en contenedores aislados el 2026-11-05:

| Envío | Respuesta |
|---|---|
| Álbum válido | 200 con los 6 campos e `id` (sin `tracks`, `performers` ni `comments`) |
| Dos álbumes idénticos | 200 en ambos: no hay restricción de unicidad |
| Con `id` (aunque sea `null`) o `tracks` | 400 `is not allowed` |
| Nombre vacío o ausente | 400 |
| Nombre de solo espacios | 200 |
| Portada `ftp://x` | 200: el backend no valida la URL |
| Género `Rock Clásico`, disquera `Apple Records` | 400 |
| Textos de 5000 caracteres | 200 |
| `1969-09-26T00:00:00.000Z` | Vuelve igual en `GET /albums/{id}`, con el backend en UTC y en America/Bogota |

Restricciones: Compose BOM 2024.04.01 (Material 3 1.2.1), `minSdk` 23 sin desugaring, ninguna dependencia nueva y textos literales de la HU.

## Goals / Non-Goals

**Goals:**
- Cumplir los 10 criterios de la HU07 con la estética de las HU ya implementadas.
- Mantener toda la lógica en funciones puras y ViewModels que se puedan probar sin dispositivo.
- Que el álbum nuevo se vea al volver, sin cambiar el regreso desde el detalle, que no vuelve a consultar.

**Non-Goals:**
- Sobrevivir a la muerte del proceso: el criterio solo pide conservar al girar y la sesión tampoco se persiste.
- Validar que la URL apunte a una imagen: el criterio solo exige el prefijo.
- Impedir nombres de álbum repetidos: el backend los permite.

## Decisions

### D1. Datos: DTO propio de 6 campos

`AlbumApi` agrega `@POST("albums") suspend fun crearAlbum(@Body album: CrearAlbumDto): AlbumDto`. `CrearAlbumDto` tiene solo `name`, `cover`, `releaseDate`, `description`, `genre` y `recordLabel`. No se reutiliza `AlbumDto`: el backend rechaza `id` y `tracks`, y funcionaría únicamente porque Gson omite los nulos.

`AlbumRepository.crearAlbum(album: AlbumNuevo): Result<Int>` usa `adaptador.llamar` y devuelve el `id` creado, que el catálogo necesita para la insignia. El 400 y los fallos de red llegan como `ErrorDeCarga`, igual que en las consultas; no se distinguen porque el mensaje es uno solo.

`data/model/OpcionesAlbum.kt` define `GenerosAlbum` y `DisquerasAlbum` con los valores exactos del backend, que no expone un endpoint para consultarlos. Se muestran sin traducir, como ya hace el catálogo.

### D2. Fecha siempre en UTC

`DatePickerState.selectedDateMillis` es la medianoche UTC del día elegido. Se guarda tal cual en el estado, y `data/model/FechaLanzamiento.kt` concentra tres funciones puras con `SimpleDateFormat` en UTC, sin `java.time`:

- `fechaIsoUtc(millis)`: `1969-09-26T00:00:00.000Z`, para enviar.
- `fechaVisible(millis)`: `26/09/1969`, para el campo.
- `hoyEnUtc(ahoraMillis, zona)`: la medianoche UTC del día **local** del teléfono.

La tercera evita un error real: a las 8 p. m. en Bogotá ya es mañana en UTC, así que comparar contra `System.currentTimeMillis()` dejaría elegir un día futuro. El límite se aplica con `SelectableDates`: `isSelectableDate` compara contra `hoyEnUtc` e `isSelectableYear` contra el año local.

*Alternativa descartada:* enviar la medianoche local. En zonas al este de UTC correría la fecha al día anterior.

### D3. Estado como `data class`, no sellado

```
data class CrearAlbumUiState(
  nombre, portada, descripcion: String = "",
  fechaMillis: Long? = null, genero: String? = null, disquera: String? = null,
  errores: Map<CampoAlbum, ErrorCampo> = emptyMap(),
  intentosFallidos: Int = 0,        // lleva la pantalla al inicio tras validar
  envio: EnvioAlbum = Editando
)
sealed interface EnvioAlbum { Editando; Enviando; Fallido; Creado(id: Int) }
```

Los demás ViewModels usan estados sellados, pero aquí los datos deben sobrevivir a todos los estados del envío; con un estado sellado cada variante los repetiría. Lo sellado queda en `EnvioAlbum`.

`CrearAlbumViewModel` se crea por destino, igual que los demás, y por eso sobrevive al giro. También sobrevive un envío en curso.

### D4. Validación pura y protección contra duplicados

`validarAlbum(estado): Map<CampoAlbum, ErrorCampo>` recorta los textos y marca `Obligatorio` en cada campo vacío. Si la portada no está vacía y no empieza con `http://` o `https://` (sin distinguir mayúsculas), marca `UrlInvalida`.

`crear()` termina de inmediato si `envio` es `Enviando` o `Creado`, antes de lanzar la corrutina. Esa es la protección real ante dos toques seguidos; desactivar el botón no basta porque el backend guarda los dos álbumes. Se envían los textos recortados.

Editar un campo borra solo el error de ese campo. No se valida al perder el foco: el criterio no lo pide y marcaría campos que el usuario aún no ha llegado a llenar.

*Riesgo aceptado:* si el timeout de 10 s vence después de que el backend guardó, reintentar duplica el álbum. Evitarlo exigiría cambiar el backend.

### D5. Regreso al catálogo con `savedStateHandle`

`AlbumsViewModel` sigue vivo en la pila y su `init` no se repite, así que el formulario debe avisarle:

1. Con `EnvioAlbum.Creado(id)`, la ruta `crear-album` escribe el `id` en `previousBackStackEntry.savedStateHandle` y hace `popBackStack`.
2. La ruta del catálogo observa esa clave, llama a `viewModel.albumCreado(id)` y la borra, para que girar no repita nada.
3. `albumCreado(id)` guarda el `id`, activa el aviso y el desplazamiento y actualiza la lista. Con `Exito` o `Vacio` usa `actualizar()`; con `Error` o `Cargando` usa `cargar()`, porque `actualizar()` no hace nada en esos estados.

La marca vive en un flujo aparte, `albumNuevo: StateFlow<AlbumNuevoCatalogo?>`, y no en `AlbumsUiState`, porque también debe existir mientras el estado es `Cargando` o `Error`.

Regresar desde el detalle no escribe la clave, así que sigue sin consultar.

### D6. Insignia "NUEVO" y desplazamiento

El backend no entrega fecha de creación, así que la insignia solo puede ser local. Dura mientras exista el `AlbumsViewModel`: sobrevive a girar, abrir un detalle y deslizar para actualizar, y desaparece al salir del catálogo. Si se crea otro álbum, la insignia pasa a ese.

Va en la esquina superior derecha de la portada, donde está el año, y lo reemplaza en esa tarjeta porque el año sigue visible junto al género. Usa `ChipEtiqueta` con `primary` y `onPrimary`.

El orden sigue siendo alfabético (no se adopta "Recientes" del mockup). Como el álbum puede quedar fuera de la vista, el catálogo se desplaza hasta él una sola vez, cuando la lista ya lo contiene. Esto exige sacar `rememberLazyGridState()` a una variable. El índice suma 2 por los elementos "titular" y "encabezado" de la cuadrícula.

Si la actualización falla, el álbum sí se creó: se muestra "Álbum creado" y luego el aviso existente "No pudimos actualizar el catálogo", sin insignia ni desplazamiento.

### D7. Navegación y bloqueos

- Ruta `crear-album`. No se usa `album/crear` para no acercarse a `album/{id}`.
- El botón "Crear álbum" del catálogo navega con `launchSingleTop`, como los demás accesos.
- `onCancelar` y la flecha solo hacen `popBackStack` si `crear-album` sigue siendo el destino actual. Dos toques rápidos sacarían también el catálogo.
- `BackHandler(enabled = enviando) {}` bloquea el atrás del sistema durante el envío; si no, se cancelaría una petición que el backend quizá ya guardó.
- Si la ruta se abre sin `collectorActionsEnabled` (la pila se restaura tras morir el proceso y la sesión no), regresa de inmediato.

### D8. Diseño visual

Se parte de lo implementado, no de los mockups de `Downloads/HU07`, de los que solo se toma la estructura. Colores y tipografía salen de `MaterialTheme`.

| Elemento | Referencia |
|---|---|
| Encabezado: flecha circular, chip "COLECCIONISTA" y título "Crear" | `Encabezado` privado de `PantallaCatalogo`, que pasa a compartirse como `EncabezadoConRol` sin cambiar su aspecto |
| Titular "Nuevo [álbum]" y subtítulo | `Titular` del catálogo: `headlineLarge` y píldora `primary` |
| Tarjeta blanca con los campos | Tarjetas del catálogo: `surface`, radio 24 dp |
| Campos: píldora de 52 dp con ícono a la izquierda | `Buscador` de artistas: `BasicTextField` con `decorationBox`; borde `outline`, y `error` si tiene error |
| Descripción: mínimo 120 dp, radio 24 dp, contador "N / 500" | Mockup 1; el texto se limita a 500 |
| Portada: miniatura de 56 dp, radio 12 dp, si la URL es válida | Mockup 1, con `PortadaAlbum` |
| Género y disquera en filas completas | Mockup 2; lado a lado, "Discos Fuentes" no cabe en 360 dp |
| Etiqueta con `*` en `primary`; error con ícono y texto en `error` | Mockup 2 |
| Aviso "Atención requerida" arriba | Mockup 2; al fallar la validación la pantalla sube hasta él |
| Error de envío justo sobre los botones | Mockup de la wiki, para que se vea donde se pulsó |
| "Crear álbum" (`primary`) y "Cancelar" (`surface`), en píldora | `Button` de reintentar del catálogo |
| Botón "Crear álbum" del catálogo: `ExtendedFloatingActionButton` en píldora, `primary`, abajo a la derecha | D10 |

La pantalla usa `Column` con `verticalScroll`, no `LazyColumn`, para no caer en el bug #74 y para que todos los campos conserven su estado. Es la misma columna en horizontal, limitada a 600 dp y centrada; el mockup 7 es vertical y no define otra distribución.

`Theme.kt` no define colores de error y ninguna pantalla los usa aún, así que se agregan `error` (`#BA1A1A`) y `errorContainer` (`#FFDAD6`) de `DESIGN.md` sin afectar lo existente.

Selector: un `Dialog` propio con `DatePicker(showModeToggle = false)`, porque el modo por defecto permite escribir la fecha, contra el criterio. No se usa `DatePickerDialog` porque no limita su altura: en horizontal dejaba afuera sus botones. El título es "Selecciona la fecha de lanzamiento" y los botones "Cancelar" y "Aceptar"; el diálogo se recuerda con `rememberSaveable`.

Íconos: `Icons.Filled` de `material-icons-core` (`Add`, `DateRange`, `KeyboardArrowDown`, `Warning`, `Refresh`), `ic_cat_album` existente y tres vectores nuevos: `ic_crear_enlace` (`link`), `ic_crear_genero` (`category`) e `ic_crear_disquera` (`business`). No se agrega `material-icons-extended`.

### D9. Textos

Prevalecen los criterios: "Crear álbum", "Creando...", "Cancelar", "Campo obligatorio", "Ingresa una URL válida", "Álbum creado", "No se pudo crear el álbum, intenta de nuevo". Donde los criterios no definen el texto se usa el de los mockups: "Reintentar creación" (coincide en ambos juegos), "Atención requerida" y "Corrige los campos marcados para continuar con el registro.".

### D10. Ubicación del botón "Crear álbum"

El acceso muestra el texto "Crear álbum" y no solo un `+`. Se compararon tres ubicaciones con prototipos en el Motorola Edge 30 y en el emulador API 23 reducido a 320 dp de ancho, también en horizontal:

| Ubicación | Resultado |
|---|---|
| **Fijo abajo a la derecha (elegida)** | Siempre visible, al alcance del pulgar y estable en todos los tamaños. Tapa parte de una tarjeta mientras se desplaza la lista |
| Encabezado, a la derecha del título | Limpio en el Motorola y no tapa nada, pero a 320 dp el texto se parte en dos líneas y queda lejos del pulgar |
| Bajo el titular, a todo el ancho | Muy claro al entrar, pero desaparece al desplazarse y baja el catálogo unos 50 dp |

Detalles: usa `ExtendedFloatingActionButton` con `shape = CircleShape`, porque la forma por defecto no coincide con los botones en píldora de la app; lleva `Icons.Filled.Add` y el texto siempre visible. La cuadrícula reserva 88 dp abajo para que la última tarjeta no quede tapada, y el aviso "Álbum creado" se muestra encima del botón.

### D11. Preparación para la segunda parte

La parte 1 no aplica M1 a M4, para que sirva de línea base. Sí se escribe de forma que aplicarlas después sea un cambio pequeño y localizado:

| Mejora | Cómo la deja lista la parte 1 |
|---|---|
| M1 | La lógica nueva queda en funciones puras (`validarAlbum`, `FechaLanzamiento.kt`), con pruebas que protegen cualquier optimización |
| M2 | Toda la lógica de red pasa por `AlbumRepository`, el único lugar donde después se inyectaría el despachador. La parte 1 no usa `Dispatchers` ni `withContext` |
| M3 | La vista previa reutiliza `PortadaAlbum`, así que la corrección de la imagen de respaldo llega sola. `CrearAlbumDto` vive en `data.remote`, que es el paquete que cubren las reglas `-keep` |
| M4 | El catálogo se refresca con `AlbumsViewModel`, sin saber de dónde salen los datos, así que Room puede entrar detrás de `AlbumRepository` sin tocar el formulario |

Al cerrar la parte 1 se repite la medición de `design.md` ("Mediciones") y se conserva como línea base de la parte 2, junto con el commit integrado a `develop`.

## Risks / Trade-offs

- **Sin pruebas de UI automatizadas:** la navegación, `savedStateHandle`, el selector y el giro solo se verifican a mano hasta que se retomen T07-5 y T07-6. Por eso la lógica se saca a funciones y ViewModels.
- **Idioma del calendario:** Material 3 toma el idioma del dispositivo. La prueba en un emulador en inglés mostró "Selected date", así que el diálogo fuerza español con `LocalConfiguration` y `LocalContext` cuando el dispositivo usa otro idioma.
- **Selector en horizontal:** con `DatePickerDialog` el calendario se deformaba y "Aceptar" quedaba fuera de la pantalla. En el diálogo propio el calendario se desplaza y los botones quedan fijos abajo.
- **APIs experimentales:** `DatePicker` y `ExposedDropdownMenuBox` necesitan `@OptIn`, como ya ocurre con deslizar para actualizar.
- **Portadas `http://`:** son válidas, pero `network_security_config.xml` bloquea ese tráfico hacia servidores externos y se verá la imagen genérica.
- **Duplicado por timeout:** ver D4.

## Open Questions

- Los criterios 5 y 6 de la wiki deben ajustarse: "Cancelar" no aplica durante el envío y falta la regla de fecha no futura. Se confirma con el equipo y se actualiza en T07-7.
- Los mockups locales y los de la wiki no coinciden y T07-1 (#5) sigue abierta. Se resuelve en T07-7.
- La rúbrica evalúa escenarios automatizados con Espresso o Kraken para las historias de las iteraciones 1 y 2, así que T07-5 no puede quedar diferida más allá del cierre de la iteración si la HU07 es una de las cinco.
- La rúbrica dice "iniciando en la versión L" (API 21) y `minSdk` es 23; `lower-min-sdk-23` dejó fuera de alcance bajar de 23. Falta confirmarlo con los tutores.

## Mejoras futuras

Esta sección es la segunda parte de la implementación y no se aplica en este cambio. Registra el camino validado el 2026-11-05 para retomarlo después. La validación se hizo sobre `origin/develop` (`371e2e5`), con lectura del código y una copia desechable fuera del repositorio, que se ejecutó en tres dispositivos.

### Mediciones

Dispositivos: Motorola Edge 30 (físico, Android 14), `Pixel_8_API_34` y `Nexus_5X_API_23` (emuladores). Recorrido: entrar como Visitante, abrir el catálogo real del backend de los tutores (274 álbumes, solo `GET`) y deslizar 12 veces. Mediana de 3 corridas; memoria en MB de PSS.

| Dispositivo | Variante | Java | Nativa | Gráficos | Total | Arranque | Cuadros lentos |
|---|---|---|---|---|---|---|---|
| Motorola | Debug actual | 21,6 | 22,2 | 79,5 | 173,2 | 857 ms | 36 de 526 |
| Motorola | Debug + respaldo con Coil | 20,8 | 21,8 | 79,4 | 172,1 | 847 ms | 37 de 532 |
| Motorola | Release + R8 + respaldo con Coil | 12,7 | 22,2 | 88,8 | 137,3 | 207 ms | 0 de 638 |
| Pixel 8 | Debug actual | 12,8 | 52,2 | n/d | 104,7 | 988 ms | 91 de 265 |
| Pixel 8 | Debug + respaldo con Coil | 11,8 | 49,0 | n/d | 101,0 | 1859 ms | 86 de 244 |
| Pixel 8 | Release + R8 + respaldo con Coil | 7,9 | 46,6 | n/d | 68,1 | 629 ms | 12 de 575 |
| Nexus 5X (API 23) | Debug actual | 29,5 | 8,4 | n/d | 75,1 | 4297 ms | 111 de 188 |
| Nexus 5X (API 23) | Debug + respaldo con Coil | 22,0 | 8,4 | n/d | 67,8 | 2426 ms | 205 de 229 |
| Nexus 5X (API 23) | Release + R8 + respaldo con Coil | 18,7 | 8,2 | n/d | 43,2 | 2095 ms | 237 de 240 |

Límites de estas cifras: los tiempos y cuadros de los emuladores no son fiables por el renderizado y la variación entre corridas; solo sirven los del Motorola. La memoria sí se compara dentro de cada dispositivo. Las portadas llegan de servidores externos y agregan ruido. Release frente a debug no aísla a R8: también quita el modo depurable. No se midieron artistas, detalle ni búsquedas.

### M1. Micro-optimizaciones

Lint reporta hoy 0 errores y 48 avisos:

| Avisos | Regla | Acción propuesta |
|---|---|---|
| 30 | `GradleDependency` | Revisar uno a uno: subir Kotlin arrastra el compilador de Compose, KSP y Room |
| 7 | `UnusedResources` | Eliminar (`colors.xml`) |
| 7 | `UnusedAttribute` | Revisar; es esperable con `minSdk` 23 |
| 3 | `AndroidGradlePluginVersion` | Igual que `GradleDependency` |
| 1 | `RedundantLabel` | Quitar `android:label` de `MainActivity` |

En el código:

- `formatearFechaLanzamiento` crea dos `Regex` y dos `SimpleDateFormat` en cada llamada. Los `Regex` pueden ser constantes.
- `ordenAlfabetico` está duplicado en `OrdenAlbumes.kt` y `ArtistRepository.kt`.
- Las búsquedas de artistas y coleccionistas normalizan cada nombre en cada tecla. Se puede normalizar una vez al cargar.

SonarQube: el plugin `org.sonarqube` 5.1.0.4882 se aplica y la tarea `sonar` queda disponible con Gradle 8.7. **El análisis no se ejecutó**, porque requiere un servidor. Hay dos opciones: `sonarqube:community` local con Docker, o SonarCloud, gratuito para repositorios públicos, con el token como secreto de GitHub. Se recomienda el local para no depender de permisos sobre el repositorio.

La wiki exige detallar las optimizaciones, así que cada una se registra con su estado antes y después.

### M2. Evitar ANR

La app no usa `Dispatchers` ni `withContext` en ningún archivo. Retrofit no bloquea el hilo principal, pero lo que ocurre después de cada respuesta sí corre en él, porque `viewModelScope` usa `Dispatchers.Main`:

| Trabajo en el hilo principal | Dónde | Solución |
|---|---|---|
| Convertir y ordenar con `Collator` | `AlbumRepository.obtenerAlbumes`, `ArtistRepository.obtenerArtistas` | `withContext` con un despachador inyectado por constructor, con `Dispatchers.Default` por defecto |
| Filtrar con `Normalizer` y `Regex` en cada tecla | `ArtistsUiState.kt`, `CollectorsUiState.kt` | Normalizar una vez (M1) y filtrar fuera del hilo principal |
| Crear Retrofit y OkHttp al arrancar | `VinilosApplication.container`, `AppContainer` | Crear el adaptador y los repositorios con `by lazy` |

Precauciones: un `Collator` compartido no debe usarse desde varios hilos, así que se crea una instancia por ordenamiento. El despachador inyectado permite que las unitarias sigan usando `MainDispatcherRule`.

Medido en el hilo principal, en debug: convertir y ordenar 274 álbumes tarda unos 9,5 ms en el Motorola (1,5 ms en release) y crear `AppContainer` unos 40 ms (3 ms en release). El peor caso fue 137 ms en el emulador API 23. `StrictMode` (`detectAll`) no reportó ninguna violación y no hubo cierres inesperados en las variantes con reglas.

**Conclusión:** en el recorrido medido no hay riesgo real de ANR, cuyo umbral es de 5 s. M2 se hace porque la rúbrica pide evidenciar la práctica y para proteger catálogos más grandes, no para corregir un problema medido. Se deja `StrictMode` activo solo en debug.

### M3. Reducir memoria

- **Portadas en Coil (mayor oportunidad):** en el Motorola, "Gráficos" ocupa 79,5 MB de 173,2 MB, casi la mitad. La hipótesis es que son las portadas, pero falta confirmarla con el perfilador. Se probaría fijar `memoryCache { maxSizePercent(...) }` y medir de nuevo. También se puede reutilizar el `OkHttpClient` de `ServiceAdapter` con `newBuilder()`, para no mantener dos grupos de conexiones e hilos.
- **Variante release con R8 (mayor efecto medido):** en el Motorola la memoria total baja 21 % (173,2 a 137,3 MB), el arranque 76 % (857 a 207 ms) y los cuadros lentos pasan de 36 a 0. El APK pasa de 10,24 a 1,88 MB. Hoy se entrega el debug.
  - **Las reglas `-keep` son obligatorias.** Sin ellas, la app se cierra al abrir el catálogo; se comprobó en el Motorola (`ClassCastException` dentro de Retrofit) y en el Nexus 5X. Con reglas para `data.remote`, `Signature`, `Continuation`, `retrofit2.Call` y `retrofit2.Response` funcionó en los tres dispositivos.
  - La variante release no está firmada. En la prueba se firmó con la llave de debug; el equipo debe decidir cómo firmar la entrega.
- **Imagen de respaldo (efecto solo en versiones antiguas):** `PortadaAlbum` usa `painterResource` con una imagen de 852×480, y `ContenidoAlbum` hace lo mismo con otra similar. Pasar la de `PortadaAlbum` a Coil con `ImageRequest.placeholder(id)` no cambia la memoria en Android 14 (Motorola 173,2 a 172,1 MB, dentro del ruido), pero en API 23 baja 7,3 MB (10 %). El cambio es pequeño y vale la pena, sin ser prioritario. La suposición inicial de que cada tarjeta costaba 1,6 MB no se confirmó en Android 14.
- **Verificación:** LeakCanary en `debugImplementation` y Memory Profiler.

### M4. Room

Compilación validada en la copia desechable: Room 2.6.1 con KSP `1.9.0-1.0.13` funciona con Kotlin 1.9.0, AGP 8.6.0 y Gradle 8.7. Generó `AlbumDao_Impl` y `VinilosDb_Impl`, las 152 unitarias pasaron y el APK debug subió de 10,24 a 10,39 MB. Una prueba instrumentada pasó en los tres dispositivos, incluido API 23: con 274 álbumes, reemplazar tardó de 15 a 26 ms y leer de 7 a 26 ms. Se usó JDK 21; falta repetirlo con el JDK 17 de la CI. No se debe subir Room sin revisar antes su compatibilidad con Kotlin 1.9.0.

Alcance recomendado: caché de solo lectura del catálogo.

- Nuevo paquete `data/local` con `AlbumEntity`, `AlbumDao` y `VinilosDb`, con `exportSchema = true`.
- `AlbumRepository` recibe `albumDao: AlbumDao? = null`. Con `null` se comporta como hoy, lo que mantiene funcionando los repositorios falsos y `AppContainerTest`, que no tienen `Context`.
- `VinilosApplication` crea la base de datos y se la entrega a `AppContainer`.
- Cada consulta exitosa reemplaza la caché en una transacción. Si la red falla, se muestra la caché.
- `ServiceAdapter` sigue siendo el único acceso al backend. `POST /albums` sigue requiriendo conexión: no se crea una cola de envíos ni un borrador. La HU07 no necesita cambios, porque la consulta posterior a crear ya llena la caché.

Consecuencias que debe resolver ese cambio:

- Modifica el requisito "Error de carga recuperable" de `catalogo-albumes`: el error solo aparecería sin caché. Afecta a `RecuperacionTest` y a `CatalogoE2ETest`.
- Probar el DAO exige pruebas instrumentadas con `Room.inMemoryDatabaseBuilder`, porque el proyecto no tiene Robolectric.
- Room mejora el uso sin conexión y evita pantallas vacías, pero no reduce la memoria. No se debe presentar como evidencia de M3. Tampoco está en el enunciado de la Iteración 2.

### Orden sugerido

1. Variante release con R8 y reglas `-keep`: es la de mayor efecto medido, pero requiere decidir la firma.
2. Medir y ajustar las portadas en Coil: es el mayor consumo de memoria.
3. M2: dejar la práctica aplicada y `StrictMode` en debug.
4. M1, junto con la imagen de respaldo, para dejar el lint limpio y el detalle en la wiki.
5. M4, porque cambia el comportamiento y un requisito del catálogo.
