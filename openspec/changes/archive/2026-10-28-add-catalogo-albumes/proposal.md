# Proposal

## Why

Hoy "Ver álbumes" lleva a una pantalla provisional con un botón fijo hacia el álbum 100, así que el usuario no puede recorrer los álbumes ni elegir el que le interesa. La HU01 reemplaza ese destino por el catálogo real, que además es la puerta de entrada al detalle de la HU02, ya implementado.

## What Changes

- Agrega el catálogo de álbumes que consume `GET /albums` y muestra cada álbum con su portada, nombre y género, ordenados alfabéticamente por nombre. Es igual para el Visitante y el Coleccionista; solo cambia la etiqueta de rol del encabezado.
- Sigue los mockups de Stitch de la HU01: cuadrícula de tarjetas, encabezado con flecha de atrás, etiqueta de rol ("VISITANTE"/"COLECCIONISTA") y título "Álbumes", titular "Explora álbumes", subtítulo, contador de lanzamientos y, en cada tarjeta, el artista y el año como datos complementarios.
- Muestra un indicador de progreso mientras carga; "No pudimos cargar el catálogo" con "Reintentar" ante fallos de conexión o del servidor; y "Aún no hay álbumes en el catálogo" cuando la lista está vacía. Los textos de la HU prevalecen sobre los del mockup.
- Muestra una portada por defecto cuando el álbum no tiene portada o no se puede descargar, sin afectar al resto de la lista.
- Permite actualizar el catálogo deslizando la lista hacia abajo, recorrer listas largas con fluidez y conservar la posición y los datos al girar la pantalla.
- Al tocar un álbum se abre su detalle (HU02); al volver del detalle se regresa al catálogo sin recargarlo.
- **BREAKING (interno)**: elimina `PantallaCatalogoProvisional`, su botón "Ver álbum de prueba", `AlbumIdProvisional` y sus textos y pruebas.
- Registra `AlbumRepository` en `AppContainer` como reemplazable (igual que coleccionistas y artistas) y lo pasa explícitamente desde `MainActivity`, para que las pruebas puedan usar un repositorio falso.

Fuera de alcance: buscador, botón de filtros, chips de género, selector de orden, botón de perfil del encabezado, "Restablecer filtros" y cualquier cambio a BackVynils.

## Objetivos de calidad

El código y las pruebas de la HU01 deben cumplir los objetivos de pruebas del sprint:

- **OBJ01, cobertura de criterios:** cada uno de los 10 criterios de aceptación tiene al menos una prueba automatizada (unitaria o de integración), escrita con TDD.
- **OBJ02, interfaz y navegación con Compose Test:** se prueban todos los estados visibles del catálogo y todas las rutas de navegación que entran o salen de él, y todas pasan.
- **OBJ03, E2E contra el backend real:** al menos 2 escenarios E2E contra BackVynils (se implementan 4), todos superados en un dispositivo físico, con su evidencia publicada en el repositorio.
- **OBJ04, compatibilidad:** la suite instrumentada y una verificación manual del flujo se ejecutan en al menos dos entornos, uno de ellos con API 23, con 0 cierres inesperados y el resultado de cada entorno registrado en el repositorio.

OBJ05 (reporte de resultados y gestión de defectos como issues) no se aplica por ahora en este cambio.

## Capabilities

### New Capabilities
- `catalogo-albumes`: consulta, presentación, orden, estados (carga, vacío, error), actualización y selección de los álbumes del catálogo.

### Modified Capabilities
<!-- Ninguna: "Ver álbumes" y "Atrás" desde el catálogo ya están en `pantalla-inicio`; el catálogo no muestra acciones exclusivas del coleccionista (`sesion-usuario`); la selección desde el catálogo ya la define el cambio abierto `add-detalle-album`. -->

## Impact

- **Datos** (`data.remote`, `data.repository`): `AlbumApi` agrega `GET albums`; `AlbumRepository` agrega la consulta del catálogo, que descarta solo los álbumes incompletos y ordena por nombre. Reutiliza `AlbumDto` y `ServiceAdapter`.
- **Estado** (`viewmodel`): nuevo `AlbumsViewModel` con `AlbumsUiState` (`Cargando`, `Exito`, `Vacio`, `Error`), `reintentar()` y `actualizar()`.
- **Interfaz** (`ui.catalogo`): nueva `PantallaCatalogo`; textos en `strings.xml`; vectores `ic_cat_*` copiados de Material Symbols (Apache 2.0). Reutiliza la portada con respaldo de la HU02 (Coil, `inicio_albumes`).
- **Navegación e inyección** (`navigation`, `AppContainer`, `MainActivity`): el destino `catalogo` usa la pantalla real y navega a `album/{id}`.
- **Pruebas**: unitarias (repositorio con MockWebServer, ViewModel, orden), instrumentadas con repositorio falso (estados, lista larga, actualización, rotación, navegación) y E2E `CatalogoE2ETest` contra BackVynils (E2E-01-01 a 01-04).
- **Resultados y evidencia** (`docs/pruebas/sprint-1/HU01/`, nuevo): matriz de trazabilidad, resultados por entorno, verificación manual e informe E2E. Se versiona en el repositorio, a diferencia de `e2e-evidencia/`, que está en `.gitignore`; las capturas E2E y manuales se conservan en el equipo de ejecución.
- **Dependencias**: ninguna nueva. "Deslizar para actualizar" usa la API experimental de Material 3 1.2.1, que ya está en el proyecto.
- **Coordinación**: el cambio de `AppContainer` coincide con lo comentado en el PR #71 (HU02), y la navegación catálogo → detalle cubre parte del issue #17 (T02-4, Lorena Nossa).
