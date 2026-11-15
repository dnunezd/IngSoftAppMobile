# Tasks

## 1. Preparación

- [x] 1.1 Actualizar el `develop` local a `origin/develop` (avance rápido, incluye #93 `add-cache-consultas`) y crear la rama `feature/optimizar-cache-e-imagenes`. Verificar que `git log -1 develop` es `32eb0ef` o posterior y que `./gradlew testDebugUnitTest` pasa como línea base.

## 2. Imagen genérica liviana (D1)

- [x] 2.1 En `PortadaAlbum.kt`, reemplazar la ilustración `inicio_albumes` por el patrón de HU04:
  - un `Box` con el `modifier` del llamador, fondo `surfaceVariant` y contenido centrado;
  - debajo, `Icon(ic_cat_album)` teñido con `onSurfaceVariant`, con `fillMaxSize(0.4f)` y la etiqueta `TagRespaldoPortada`;
  - encima, `AsyncImage` con `matchParentSize()` y sin `placeholder`, `error` ni `fallback`.

  Verificar que la app compila sin tocar el catálogo, el detalle ni la vista previa de crear álbum.
- [x] 2.2 En `FilaArtista` (`ContenidoAlbum.kt`), usar `painterResource(R.drawable.ic_artista_por_defecto)` como imagen de espera, de error y de respaldo. Verificar con `git grep` que `inicio_albumes` e `inicio_artistas` solo quedan en `PantallaInicio.kt`.
- [x] 2.3 Actualizar las pruebas de UI:
  - en `PortadaAlbumTest`, agregar que el ícono de respaldo (`TagRespaldoPortada`) se ve sin URL y con una URL en blanco;
  - en `ContenidoAlbumTest`, renombrar `muestraUnaIlustracionLocalProvisionalAntesDeLosMetadatos` a `muestraLaPortadaAntesDeLosMetadatos`.

  Verificar que las dos clases pasan en el Samsung con `connectedDebugAndroidTest`, filtrado a esas clases.
- [x] 2.4 Verificar a mano en el Samsung, con capturas:
  - un álbum con portada inválida muestra el disco sobre fondo neutro en el catálogo y en el detalle;
  - un artista sin imagen en el detalle de álbum muestra la silueta;
  - Inicio conserva sus ilustraciones.
- [x] 2.5 Dejar `PortadaAlbum` como un solo `AsyncImage` (D1): `IconoCentrado` como `placeholder`, `error` y `fallback`, y fondo `surfaceVariant` en el `modifier`. Quitar el `background` duplicado del catálogo y de la vista previa de crear álbum. Hacer que `FilaAlbum` (detalle de artista) use `PortadaAlbum` y borrar `tagRespaldoAlbum`. En `PortadaAlbumTest`, comprobar el respaldo por los píxeles (`captureToImage`) en lugar de `TagRespaldoPortada`.
- [x] 2.6 Verificar en el Samsung que `PortadaAlbumTest`, `ContenidoAlbumTest` y las E2E de HU01, HU02, HU04 y HU07 pasan, y que el disco se ve igual que antes en el catálogo, el detalle, la vista previa de crear álbum y los álbumes del artista.

## 3. Caché HTTP siempre revalidada (D2)

- [x] 3.1 En `ServiceAdapter`, hacer pública `clientePorDefecto(carpetaCache: File? = null)`. Con carpeta, agregar `Cache(carpetaCache, 10 MiB)` y un interceptor de aplicación que ponga `Cache-Control: max-age=0` en las peticiones `GET`. Agregar a `ServiceAdapterTest` pruebas con `MockWebServer` y `TemporaryFolder`:
  - la segunda consulta lleva `If-None-Match` con la `ETag` de la primera;
  - un 304 entrega los mismos datos como éxito;
  - con `Cache-Control: max-age=600`, la segunda consulta igual llega al servidor;
  - sin conexión, con una respuesta guardada, devuelve `ErrorDeCarga`;
  - sin carpeta, no se envía `If-None-Match`.
- [x] 3.2 Agregar `carpetaCacheHttp: File? = null` a `AppContainer`, que construye `ServiceAdapter(baseUrl, ServiceAdapter.clientePorDefecto(carpetaCacheHttp))`. `VinilosApplication` le pasa `cacheDir.resolve("http")`, separada de `image_cache`. Agregar a `AppContainerTest` que, con carpeta, una consulta repetida envía `If-None-Match`. Verificar que los demás casos de `AppContainerTest` siguen pasando sin carpeta.
- [x] 3.3 Documentar en el KDoc de `clientePorDefecto` y de `VinilosApplication` por qué la caché se revalida siempre y por qué va en una carpeta aparte. Verificar el texto en la revisión del commit.

## 4. Versión de las respuestas (D3)

- [x] 4.1 Agregar a `data.remote` la clase `ConVersion<T>(valor, version)` y la extensión `Response<T>.conVersion()`. Una respuesta no exitosa lanza `HttpException(this)`, y un cuerpo nulo lanza `JsonParseException`. Agregar a `ServiceAdapterTest` que, dentro de `llamar`:
  - un 200 con `ETag` entrega esa versión;
  - un 404 da `RecursoNoEncontrado`;
  - un 500 da `ErrorDeCarga`;
  - un 200 sin cuerpo da `ErrorDeCarga`;
  - un 304 servido desde la caché entrega la versión.
- [x] 4.2 Cambiar a `Response<...>` los 9 `GET` de `AlbumApi`, `ArtistApi` y `CollectorApi`. En los repositorios, leer `.conVersion().valor`, todavía sin usar la versión, y ajustar las llamadas a `api.getCollectors()` de `ServiceAdapterTest`. Verificar que toda la suite unitaria pasa sin otros cambios: el comportamiento es idéntico.

## 5. Escrituras incrementales en los DAO (D6)

- [x] 5.1 En `AlbumDao`, agregar:
  - `@Upsert` de `AlbumEntity` y de `AlbumDetalleEntity`;
  - el borrado por ids en `albumes` y `album_detalle`, en grupos de 500;
  - `@Transaction aplicarCatalogo(...)`.

  Agregar a `AlbumDaoTest` (instrumentada) que el upsert cambia solo la fila modificada, que el borrado funciona con 1.200 ids y que `aplicarCatalogo` es atómica.
- [x] 5.2 Agregar a `ArtistaDao`, `ColeccionistaDao` y `FavoritoDao` sus upsert, sus borrados por ids en grupos de 500 y sus transacciones `aplicarListado`, `aplicar` y `aplicarDe`, con sus pruebas en `ArtistaDaoTest`, `ColeccionistaDaoTest` y `FavoritoDaoTest`. Verificar que las cuatro clases pasan en el Samsung.
- [x] 5.3 Implementar los métodos nuevos en los DAO falsos de `AppContainerTest`, de las cuatro `*RepositoryCacheTest` y de `RepositoriosDispatcherTest`, contando las escrituras y los borrados. Verificar que, tras compilar, `git diff app/schemas` está vacío (sin cambio de esquema) y que la suite unitaria pasa.

## 6. Copia en memoria y escritura solo de lo que cambió (D4, D5)

- [x] 6.1 Crear en `data.repository` el ayudante genérico de la copia de una tabla, con sus filas por clave, la marca de completa, parcial o desconocida y el cálculo de qué filas guardar y qué claves borrar, y el registro de versiones por consulta. Agregar pruebas unitarias para:
  - filas nuevas, cambiadas, borradas y sin cambios;
  - la copia desconocida, que pide reemplazar la tabla;
  - la copia parcial, que compara una sola fila.
- [x] 6.2 En `AlbumRepositoryImpl`, pasar por un único punto de escritura con `Mutex` el catálogo, el detalle y el borrado por 404, con las reglas 1 a 4 de D4 y los pasos de D5. Agregar a `AlbumRepositoryCacheTest`:
  - con la misma `ETag`, cero escrituras;
  - con otra `ETag` y el mismo contenido, cero escrituras;
  - un álbum cambiado escribe solo su fila y su detalle;
  - un álbum que ya no llega se borra por id;
  - la primera carga reemplaza todo;
  - tras un fallo de `SQLiteException`, la siguiente consulta sin cambios reemplaza todo;
  - una cancelación durante la escritura descarta la copia;
  - escribir un detalle o borrarlo por 404 descarta la versión del catálogo;
  - si el backend vuelve a una `ETag` anterior después de un 404, el catálogo se vuelve a escribir.
- [x] 6.3 En `ArtistRepositoryImpl`, hacer lo mismo para el listado, con la versión compuesta de músicos y bandas, y para el detalle, con la versión del artista y de `/performerprizes` («no pedida» si no hay premios), más el 404. Agregar a `ArtistRepositoryCacheTest` el escenario «Detalle armado con varias respuestas» de la spec `cache-consultas`, junto con los casos de misma versión y de mismo contenido.
- [x] 6.4 En `CollectorRepositoryImpl`, hacer lo mismo para el listado. Agregar a `CollectorRepositoryCacheTest` que un coleccionista nuevo al principio escribe solo las filas cuya posición cambió, y que la misma `ETag` no escribe.
- [x] 6.5 En `FavoriteRepositoryImpl`, hacer que `esFavorito` agregue y quite solo las diferencias por coleccionista, y que `agregarFavorito` pase por el punto de escritura y descarte la versión de ese coleccionista. Agregar a `FavoriteRepositoryCacheTest` que, si el favorito se quita en el backend y vuelve la `ETag` anterior, lo guardado se corrige.
- [x] 6.6 Actualizar el KDoc de los cuatro repositorios y de los DAO (qué escribe cada uno y cuándo no), y el párrafo «Sin conexión» del README. Verificar que la suite unitaria completa pasa.

## 7. Álbum creado sin esperar la consulta (D7)

- [x] 7.1 En `AlbumRepositoryImpl.crearAlbum`, convertir el `AlbumDto` del `POST` con `aResumen()` y `aDetalleSiEsValido()` y guardarlo por el punto de escritura, que descarta la versión del catálogo. Agregar a `AlbumRepositoryCacheTest`:
  - con una respuesta completa, quedan guardados la fila y el detalle;
  - con una respuesta sin nombre o sin género, se devuelve el id sin guardar;
  - si falla el almacenamiento, se devuelve el id;
  - si el backend luego vuelve a la `ETag` previa a la creación, el catálogo se reescribe sin el álbum.
- [x] 7.2 En `AlbumsViewModel.albumCreado`, con el catálogo en `Exito` o `Vacio`, poner el indicador, leer lo guardado y, si incluye el id, mostrar `Exito(guardados, actualizando = true)` antes de consultar. Con `Cargando` o `Error`, seguir llamando a `cargar()`. Agregar a `AlbumsViewModelTest` y `AlbumsViewModelCacheTest`:
  - el álbum nuevo se ve antes de que responda la consulta;
  - si la consulta falla, queda `Exito` con el nuevo y `errorAlActualizar`;
  - sin el nuevo guardado, el comportamiento es el de antes.

  Verificar que las pruebas actuales de `albumCreado` siguen pasando.
- [ ] 7.3 Verificar en el Samsung, contra el backend local con `adb reverse`, que al crear un álbum el catálogo lo muestra con «NUEVO» y el indicador antes de que termine `GET /albums`, y que girar la pantalla no repite el aviso ni la consulta.

## 8. Tipos de Gson creados una sola vez (D8)

- [x] 8.1 En `Conversores`, declarar un `Type` constante por lista y hacer que `leer` lo reciba. Verificar que `ConversoresTest` pasa y agregar que leer dos veces el mismo JSON da listas iguales.

## 9. Verificación integrada

- [x] 9.1 Correr `./gradlew testDebugUnitTest lintDebug` y verificar que pasan sin avisos de lint nuevos.
- [x] 9.2 Correr la suite instrumentada completa en el Samsung (`connectedDebugAndroidTest`), incluidas las E2E y `CatalogoSinConexionE2ETest`, y verificar que pasa.
- [ ] 9.3 Hacer el perfilamiento «After» en el Samsung con `docs/performance/guia-manual.md` y anotar solo lo observado en las grabaciones:
  - **HU01, recarga sin cambios en la misma grabación:** sin escrituras en `arch_disk_io`, sin `Gson.toJson` desde `Conversores` y con 304 en Connection View.
  - **HU01, paso 2:** asignaciones de `Bitmap` desde `PortadaAlbum`.
  - **HU07, paso 5:** el álbum visible antes de que responda `GET /albums`, sin reescribir el catálogo.

  Revisar también si el backend de Render responde 304 (pregunta abierta de `design.md`).
- [x] 9.4 Correr `openspec validate optimizar-cache-e-imagenes --strict` y verificar que pasa.

## Workflow follow-up

- Archivar este cambio después de `add-service-adapter`, `add-detalle-album`, `add-crear-album` y `add-cache-consultas`, para que los deltas de `acceso-backend` y `catalogo-albumes` se apliquen en orden.
