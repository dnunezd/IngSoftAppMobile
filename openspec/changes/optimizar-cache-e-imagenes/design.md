# Design

## Context

Ver `proposal.md` (Why). El estado de partida es `origin/develop` con `add-cache-consultas` (#93):

- **Imágenes.** `PortadaAlbum` y `FilaArtista` (`ContenidoAlbum.kt`) pasan `painterResource(R.drawable.inicio_albumes)` y `painterResource(R.drawable.inicio_artistas)` como `placeholder`, `error` y `fallback` de `AsyncImage`. Las dos son PNG de `drawable-nodpi`, de unos 860×480 px. El detalle de artista (HU04) ya resolvió lo mismo con otro patrón: un `Box` con fondo `surfaceVariant`, el ícono `ic_cat_album` teñido debajo y `AsyncImage` encima, sin imagen de espera. `add-crear-album` (M3) midió que cambiar estas imágenes de espera baja 7,3 MB en API 23 y no cambia la memoria en Android 14.
- **Red.** `ServiceAdapter.clientePorDefecto()` (privada) crea el `OkHttpClient` sin caché. `AppContainer` construye `ServiceAdapter(baseUrl)`. Las interfaces de Retrofit devuelven el cuerpo directo (`List<AlbumDto>`, `AlbumDto`, etc.), así que los repositorios no ven las cabeceras. `llamar` traduce `HttpException` (404 → `RecursoNoEncontrado`), `IOException`, `JsonParseException` y `KotlinNullPointerException` a `ErrorDeCarga`.
- **Room.** Cada repositorio escribe tras cada consulta exitosa con `reemplazar*` (borrar todo e insertar todo) o `guardarDetalle` (`REPLACE`), dentro de `escribir { }`, que ignora `SQLiteException` (`add-cache-consultas`, D4). `Conversores` crea un `TypeToken` en cada lectura. `VinilosDb` está en la versión 2, con migración destructiva.
- **Álbum creado.** `crearAlbum` devuelve solo el id del `AlbumDto` que responde el `POST`. `AlbumsViewModel.albumCreado()` llama a `actualizar()`, que deja la lista anterior a la vista hasta que responde `GET /albums`.
- **Backend.** Los 9 `GET` que se guardan en Room envían `ETag` débiles (`W/"..."`, generadas por Express) y responden 304 a `If-None-Match`. No envían `Cache-Control` ni `Last-Modified`. Esto se verificó en el backend local; el de Render no se revisó.

## Goals / Non-Goals

**Goals:**
- Que una recarga sin cambios no escriba en Room ni convierta listas a JSON.
- Que una consulta repetida sin cambios no descargue el cuerpo.
- Que la imagen genérica no decodifique un mapa de bits grande.
- Que el álbum creado se vea sin esperar `GET /albums`.
- Que todo esto no cambie el comportamiento sin conexión ni los resultados que ve el usuario.

**Non-Goals:**
- Saltarse la conversión DTO → modelo cuando la respuesta no cambió: el repositorio sigue devolviendo modelos recién convertidos.
- Evitar peticiones: la caché HTTP ahorra bytes, no peticiones. Evitarlas corresponde a otras mejoras identificadas en el perfilamiento, como no volver a consultar un detalle reciente.
- Llenar la copia en memoria con lo leído de Room al abrir la app (ver D5).
- Cambiar los estados, textos o indicadores de las pantallas, salvo cuándo aparece el álbum creado.

## Decisions

### D1. Imagen genérica: ícono vectorial sobre fondo neutro, debajo de la imagen

`PortadaAlbum` queda como un solo `AsyncImage`:

- **Fondo:** el `modifier` del llamador más `background(surfaceVariant)`. Los llamadores ya no ponen su propio fondo.
- **Respaldo:** un `Painter` propio (`IconoCentrado`) que dibuja `painterResource(R.drawable.ic_cat_album)` centrado, teñido con `onSurfaceVariant` y a un 40 % del lado menor. Así se ve proporcionado en la tarjeta del catálogo, en los 220 dp del detalle y en las miniaturas de 56 dp. No tiene tamaño propio, así que Coil le da todo el espacio de la imagen con cualquier `ContentScale`.
- **Uso:** ese `Painter` va como `placeholder`, `error` y `fallback`. Coil lo dibuja sin URL, mientras carga y si falla, y deja de dibujarlo cuando la imagen carga.

La firma de `PortadaAlbum` no cambia. Las miniaturas de álbum del detalle de artista (`FilaAlbum`, HU04) pasan a usar `PortadaAlbum` en lugar de su propio `Box`. Las imágenes de artista e integrante ya son un solo `AsyncImage` con la silueta vectorial como respaldo, así que no cambian.

*Primera versión, reemplazada:* el patrón de las miniaturas de HU04, con un `Box`, el `Icon` debajo y `AsyncImage` encima con `matchParentSize()`. Quitaba los `Bitmap` de respaldo, pero el ícono quedaba compuesto y se dibujaba debajo de cada imagen ya cargada. El perfilamiento de HU02 mostró que `PortadaAlbum` creaba de 14 a 59 % más objetos de Compose que antes. Con un solo `AsyncImage` desaparecen el `Box`, el `Icon` y la etiqueta de prueba. Las pruebas comprueban el respaldo por los píxeles de la imagen.

En `FilaArtista`, la imagen de espera pasa a `painterResource(R.drawable.ic_artista_por_defecto)`, como en HU03. El modelo `Artista` del álbum no tiene tipo, así que no se usa `ic_banda_por_defecto`. Inicio no cambia.

*Alternativas descartadas:*
- **Copias reducidas de las ilustraciones:** el usuario las descartó, porque se verían borrosas en la portada de 220 dp del detalle.
- **Solo un color de fondo:** las specs piden una «imagen genérica», y un color liso no se lee como imagen.
- **`SubcomposeAsyncImage` con espacios para carga y error:** la subcomposición es más costosa en listas perezosas.
- **`ImageRequest.placeholder(id)` con el mismo PNG:** Coil seguiría decodificando el mapa de bits.

### D2. Caché HTTP en el cliente de la API, siempre revalidada

- **Cliente.** `ServiceAdapter.clientePorDefecto(carpetaCache: File? = null)` deja de ser privada. Con carpeta, agrega `Cache(carpetaCache, 10 MiB)` y un interceptor de aplicación que pone `Cache-Control: max-age=0` en las peticiones `GET`.
- **Aplicación.** `AppContainer` recibe `carpetaCacheHttp: File? = null` y construye `ServiceAdapter(baseUrl, ServiceAdapter.clientePorDefecto(carpetaCacheHttp))`. `VinilosApplication` le pasa `cacheDir.resolve("http")`, separada de `image_cache`, la carpeta de Coil.
- **Funcionamiento.** Con `max-age=0`, OkHttp nunca entrega una respuesta guardada sin preguntar. Siempre envía `If-None-Match` con la `ETag` guardada, y ante un 304 entrega la respuesta guardada con código 200 y las cabeceras combinadas, que incluyen la `ETag`. Sin conexión, la petición falla con `IOException` y `llamar` la reporta como `ErrorDeCarga`, igual que hoy (spec `acceso-backend`).
- **Escrituras.** Un `POST /albums` invalida en OkHttp la entrada guardada de `/albums`, así que el siguiente `GET /albums` responde 200 completo.
- **Pruebas.** Sin carpeta (pruebas unitarias e instrumentadas que reemplazan el contenedor), el cliente queda como hoy.

`Cache()` no toca el disco al construirse: abre su diario en la primera petición, en un hilo de OkHttp.

*Alternativa descartada:* confiar en que el backend no envíe `Cache-Control`. Hoy no lo envía, pero un `max-age` en Render o en un proxy haría que OkHttp entregara respuestas viejas sin preguntar, y cambiaría el comportamiento sin conexión.

*Alternativa descartada:* forzar la revalidación con un interceptor de red que reescriba la respuesta a `no-cache`. Funciona igual, pero altera lo que guarda la caché. El `max-age=0` de la petición es la forma estándar de pedir revalidación.

### D3. La versión de cada respuesta llega a los repositorios

Los 9 `GET` de `AlbumApi`, `ArtistApi` y `CollectorApi` pasan a devolver `Response<...>`. Los `POST` no cambian. En `data.remote` se agregan:

- `data class ConVersion<T>(val valor: T, val version: String?)`, donde `version` es la cabecera `ETag`.
- `fun <T> Response<T>.conVersion(): ConVersion<T>`. Si la respuesta no es exitosa, lanza `HttpException(this)`. Si llega sin cuerpo, lanza `JsonParseException`.

Así, `llamar` no cambia y sigue traduciendo los errores igual (el 404 sigue siendo `RecursoNoEncontrado`). Los repositorios siguen combinando varias peticiones dentro de un solo `llamar`, como hoy en el listado de artistas y en el detalle con premiaciones.

**Versión compuesta.** Cuando lo guardado sale de varias respuestas, la versión es la lista de sus `ETag`:

- **Listado de artistas:** músicos y bandas.
- **Detalle de artista:** el artista y `/performerprizes`. Si las premiaciones no se pidieron, esa parte se marca como «no pedida».

La versión compuesta solo sirve para comparar si todas sus partes tienen `ETag`.

*Alternativa descartada:* un interceptor que recuerde la `ETag` de cada URL. Acopla los repositorios a las rutas HTTP, y no distingue si la respuesta se llegó a guardar.

### D4. Una copia en memoria y un único punto de escritura por repositorio

Cada `*RepositoryImpl` con DAO mantiene en RAM:

- **Una copia de lo último que escribió en cada tabla.** Es un mapa de la clave a la entidad, con una marca que indica si está **completa** (se conoce toda la tabla) o **parcial** (solo algunas filas).
- **La versión** de la última respuesta escrita, por cada consulta.
- **Un `Mutex`** que protege la comparación, la escritura y la actualización de la copia.

| Repositorio | Tablas y copia | Versión por consulta | Escrituras que pasan por el punto |
|---|---|---|---|
| `AlbumRepositoryImpl` | `albumes` (completa); `album_detalle` (parcial, completa tras escribir el catálogo) | catálogo; detalle por id | catálogo, detalle, 404, álbum creado |
| `ArtistRepositoryImpl` | `artistas` (completa); `artista_detalle` (parcial) | listado; detalle por (tipo, id) | listado, detalle, 404 |
| `CollectorRepositoryImpl` | `coleccionistas` (completa) | listado | listado |
| `FavoriteRepositoryImpl` | `favoritos`, por coleccionista (completa por coleccionista) | favoritos por coleccionista | comprobación, agregar |

La conversión ocurre fuera del `Mutex`. Las lecturas (`obtener*Guardados`) no lo toman y no cambian la copia.

Reglas para que la copia y Room no se separen:

1. **Solo el punto de escritura usa los métodos de escritura del DAO.** Lo pasa todo: el borrado por 404, el detalle, el álbum que devuelve el `POST` y el favorito agregado. Hoy esas llamadas ya están todas en los `escribir { }`, así que se reemplazan ahí.
2. **La copia y la versión se actualizan solo después de que la escritura termina bien.** Si la escritura lanza cualquier excepción, la copia de esa tabla pasa a ser desconocida y se descartan sus versiones; luego la excepción sigue su curso: `SQLiteException` se ignora, como hoy, y `CancellationException` se relanza. Así, la siguiente escritura reemplaza la tabla entera, como hoy, aunque una cancelación haya llegado después de que Room confirmara la transacción.
3. **Cuando una escritura cambia una fila, se descartan las versiones de las otras consultas que también escriben esa fila:**
   - escribir el detalle de un álbum, borrarlo por 404 o guardar el álbum creado descarta la versión del catálogo;
   - si el catálogo cambia el detalle de un álbum, se descarta la versión de ese detalle;
   - agregar un favorito descarta la versión de los favoritos de ese coleccionista.

   Sin esta regla, que el backend vuelva a un estado anterior dejaría lo guardado viejo. Ocurre en el perfilamiento: la guía borra los álbumes creados y quita los favoritos entre grabaciones, así que el backend vuelve a responder con una `ETag` que ya se había visto.
4. **Al abrir la app, la copia está vacía (desconocida).** La primera escritura de cada tabla la reemplaza entera, como hoy, y deja la copia completa.

*Alternativa descartada:* comparar contra lo leído de Room en cada escritura. Leer el catálogo con sus detalles exige convertir de JSON todas sus listas, que es casi el mismo costo que se quiere evitar.

### D5. Cómo se decide qué escribir

Para cada consulta exitosa, dentro del `Mutex`:

1. **Misma versión.** Si la versión de la respuesta existe y es igual a la guardada para esa consulta, no se hace nada más.
2. **Copia completa (listados).** Se compara cada entidad nueva con la de la copia, con la igualdad de las clases de datos. Se calcula qué filas guardar (nuevas o distintas) y qué claves borrar (las que están en la copia y ya no llegan). Si no hay nada que guardar ni borrar, no se escribe.
3. **Copia desconocida o parcial en una escritura de tabla completa.** Se reemplaza la tabla con `reemplazar*`, como hoy. En el catálogo, las dos tablas se deciden por separado. Si la de `albumes` está completa pero la de `album_detalle` es parcial, se escriben las diferencias del catálogo y se reemplazan los detalles.
4. **Una sola fila (detalle de álbum o de artista).** Si la fila está en la copia y es igual, no se escribe. Si no está o es distinta, se escribe esa fila.
5. **Escritura terminada bien.** Se aplican los cambios a la copia, se guarda la versión y se descartan las versiones que indica la regla 3 de D4. Si no hubo escritura porque nada cambió, igual se guarda la versión.

En el detalle de artista, el `RecursoNoEncontrado` sigue borrando la fila. El borrado pasa por el punto de escritura y quita esa clave de la copia y de las versiones.

*Alternativa descartada:* llenar la copia con lo que `obtener*Guardados` lee de Room al abrir la app. Ahorraría la primera escritura del catálogo, del listado de artistas y de los coleccionistas. Pero esas lecturas no traen los detalles de álbum, que son la escritura costosa. Leerlos todos al abrir costaría lo mismo que escribirlos. Queda para otro cambio si el perfilamiento lo justifica.

### D6. Escrituras incrementales en los DAO

Sin cambios en las entidades, así que no cambia el esquema: `VinilosDb` sigue en la versión 2 y `app/schemas/.../2.json` no cambia. Cada DAO suma:

- **`AlbumDao`:** `@Upsert` de `AlbumEntity` y de `AlbumDetalleEntity`; borrar por ids en `albumes` y en `album_detalle`; y `@Transaction aplicarCatalogo(...)`, que guarda y borra en las dos tablas a la vez.
- **`ArtistaDao`:** `@Upsert` de `ArtistaEntity`; borrar por tipo e ids; y `@Transaction aplicarListado(...)`.
- **`ColeccionistaDao`:** `@Upsert`; borrar por ids; y `@Transaction aplicar(...)`.
- **`FavoritoDao`:** borrar los ids de un coleccionista; y `@Transaction aplicarDe(coleccionistaId, agregar, quitar)`.

Los borrados por ids se parten en grupos de 500 dentro de la transacción. SQLite limita una consulta a 999 parámetros en versiones antiguas de Android (API 23). `reemplazar*`, `guardarDetalle` y `agregar` se conservan: se usan con la copia desconocida y para una sola fila.

*Alternativa descartada:* comparar en SQL, con un `INSERT ... ON CONFLICT DO UPDATE ... WHERE` distinto. Habría que convertir a JSON todas las filas para comparar, que es el costo que se quiere evitar.

### D7. Álbum creado: guardar la respuesta del `POST` y mostrar lo guardado

- **Repositorio.** `crearAlbum` convierte el `AlbumDto` del `POST` con `aResumen()` y `aDetalleSiEsValido()`, las mismas conversiones del catálogo. Si el resumen es válido, lo guarda por el punto de escritura: guarda la fila, el detalle si es válido, actualiza la copia si se conoce y descarta la versión del catálogo. Luego devuelve el id, como hoy. Si la conversión o la escritura fallan, igual devuelve el id. La interfaz `AlbumRepository` no cambia.
- **ViewModel.** `AlbumsViewModel.albumCreado(id)`:
  - **Catálogo en `Exito` o `Vacio`:** pone el indicador de actualización de inmediato, como `actualizar()`, y luego lee lo guardado.
    - Si lo guardado trae el id, muestra `Exito(guardados, actualizando = true)`. Si la consulta falla, deja `Exito(guardados, errorAlActualizar = true)`.
    - Si no lo trae, sigue como `actualizar()` hoy.
  - **Catálogo en `Cargando` o `Error`:** llama a `cargar()`, que ya lee lo guardado antes de consultar y ahora lo encontrará con el álbum.

  Todo corre dentro de `lanzar`, así que una carga posterior cancela esta, como hoy.
- **Pantalla.** El aviso «Álbum creado», la insignia «NUEVO» y el desplazamiento no cambian. Solo ocurren antes, cuando el álbum aparece en la lista.

Se mantiene el indicador de actualización durante la revalidación, en lugar de una consulta sin indicador. Así se conserva el escenario «Falla la consulta posterior» de `add-crear-album`, y la pantalla no muestra un estado nuevo. Esto revisa la línea de `add-cache-consultas` que decía que el álbum creado no se guarda aparte.

### D8. Tipos de Gson como constantes

`Conversores` declara un `Type` por lista (`List<Cancion>`, `List<Artista>`, `List<Comentario>`, `List<IntegranteArtista>`, `List<AlbumArtista>` y `List<PremioArtista>`), creado una sola vez con `object : TypeToken<...>() {}.type`. `leer` recibe ese tipo en lugar de crear uno por lectura. El JSON guardado no cambia, así que no hay migración.

### D9. Pruebas y orden de los commits

Cada commit deja la suite verde:

1. Imagen genérica (D1).
2. Caché HTTP (D2).
3. Versión en las respuestas (D3).
4. DAO incrementales (D6).
5. Copia y escritura incremental por repositorio (D4, D5): un commit por repositorio.
6. Álbum creado (D7).
7. Tipos de Gson (D8).
8. Verificación y perfilamiento «After».

Pruebas:

- **Unitarias de los repositorios:** usan `MockWebServer` con cabeceras `ETag` y los DAO falsos que ya existen, que cuentan escrituras. Cubren la misma versión, otra versión con el mismo contenido, los cambios parciales, el fallo y la cancelación al escribir, la regla de descarte de versiones (incluido el caso en que el backend vuelve a una `ETag` anterior) y el álbum creado.
- **Unitarias de `ServiceAdapter`:** cubren `If-None-Match`, el 304, `max-age` ignorado y la falta de conexión con una respuesta guardada. Usan una carpeta temporal (`TemporaryFolder`).
- **Instrumentadas:** los DAO (upsert, borrado por ids, más de 999 ids) y `PortadaAlbum` (se ve el ícono de respaldo sin URL).
- **Dispositivo:** todo se instala y prueba en el Samsung por adb inalámbrico. El backend del PC se alcanza con `adb reverse`.

## Risks / Trade-offs

- **[La copia en RAM se separa de Room y se omite una escritura necesaria]** → Hay un único punto de escritura, la copia se descarta ante cualquier escritura que no termine bien y se aplica la regla de descarte de versiones (D4). Las pruebas cubren cada camino, incluido el backend que vuelve a una `ETag` anterior. Si algo falla, en el peor caso lo guardado queda viejo hasta la siguiente respuesta distinta; lo que se muestra con conexión siempre es la respuesta del backend.
- **[Memoria de las copias]** → La copia del catálogo incluye los detalles: canciones, intérpretes y comentarios de cada álbum. Con los 274 álbumes medidos en `add-crear-album` (M4), son del orden de cientos de KB, y se comparten las mismas instancias que se escribieron.
- **[La `ETag` del backend desplegado se comporta distinto o se pierde en un proxy]** → Sin `ETag` se compara el contenido y se descarga completo, que es lo que pasa hoy. Las `ETag` débiles de Express salen de un hash del cuerpo, así que una colisión es despreciable.
- **[La fecha del álbum que devuelve el `POST` tiene otro formato que la de `GET /albums`]** → La comparación lo ve como un cambio y escribe esa fila una vez más. Es correcto, solo menos óptimo.
- **[La imagen genérica liviana no se nota en memoria en el Samsung]** → `add-crear-album` midió que en Android 14 no cambia y que en API 23 baja 7,3 MB. En el perfilamiento «After» se revisan las asignaciones de `Bitmap` desde `PortadaAlbum`, no solo el total. Medir en API 23 queda a decisión del usuario.
- **[El ícono de respaldo se ve a través de una portada con transparencia]** → Las portadas del catálogo son JPG o PNG opacos. HU04 ya acepta este caso.
- **[Doble almacenamiento en disco]** → La caché HTTP (hasta 10 MiB, en `cacheDir`, que el sistema puede vaciar) guarda los cuerpos además de Room. Es lo que permite el 304 sin cuerpo.
- **[`Response<T>` con R8]** → Hoy `isMinifyEnabled = false`. Si se activa, las reglas `-keep` medidas en `add-crear-album` (M3) ya incluyen `retrofit2.Response`.

## Migration Plan

- No hay migración de datos: el esquema de Room no cambia y el JSON guardado tampoco. La carpeta `cacheDir/http` se crea sola con la primera petición.
- Antes de implementar, actualizar el `develop` local a `origin/develop` y crear la rama del cambio desde ahí.
- **Reversión:** revertir los commits. La carpeta `cacheDir/http` queda sin uso y el sistema la puede vaciar.

## Open Questions

- ¿El backend de Render envía las mismas `ETag` y responde 304? No cambia el diseño: sin `ETag`, la comparación de contenido evita igual la escritura. Se revisa durante el perfilamiento «After» en Connection View.
