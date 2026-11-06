# HU10 · Agregar artistas favoritos: pruebas y evidencia

Especificación: [`openspec/changes/add-artistas-favoritos/specs/favoritos-artista/spec.md`](../../../../openspec/changes/add-artistas-favoritos/specs/favoritos-artista/spec.md). Diseño técnico en [`design.md`](../../../../openspec/changes/add-artistas-favoritos/design.md). Tareas y su estado en [`tasks.md`](../../../../openspec/changes/add-artistas-favoritos/tasks.md).

Todas las pruebas unitarias (JVM) se escribieron con TDD: ROJO (falla antes de implementar) y VERDE (pasa con el código mínimo), una prueba a la vez. Las pruebas de Compose UI (instrumentadas) se escribieron junto con la implementación y se compilaron (`compileDebugAndroidTestKotlin`), pero su ejecución en dispositivo/emulador queda a cargo de otro integrante del equipo (ver "Pendiente" más abajo).

## Cada criterio de aceptación tiene al menos una prueba automatizada

| # | Criterio | Unitarias (JVM) | Compose UI (instrumentadas, pendientes de ejecutar) |
|---|---|---|---|
| 1 | Marcar como favorito desde el detalle; el botón pasa a mostrar "Favorito" | `FavoritoViewModelTest.noFavoritoPasaAGuardandoYLuegoAFavoritoConConfirmacion`, `FavoriteRepositoryTest.agregarFavoritoUsaElEndpointDeBandaCuandoElTipoEsBanda`, `…agregarFavoritoUsaElEndpointDeMusicoCuandoElTipoEsMusico` | `PantallaArtistaProvisionalFavoritoTest.favoritoMuestraElBotonDesactivadoConSuTexto` |
| 2 | Si ya es favorito, el botón aparece como "Favorito" y desactivado | `FavoritoViewModelTest.comprobandoPasaAFavoritoCuandoElArtistaYaEsFavorito`, `FavoriteRepositoryTest.esFavoritoEncuentraElArtistaPorIdEnElListadoDelColeccionista` | `PantallaArtistaProvisionalFavoritoTest.favoritoMuestraElBotonDesactivadoConSuTexto` |
| 3 | El botón refleja el estado de favorito cada vez que se abre el detalle (persistencia entre sesiones de la app) | `FavoritoViewModelTest.comprobandoPasaANoFavoritoCuandoElArtistaNoEsFavorito`, `FavoriteRepositoryTest.esFavoritoDevuelveFalsoCuandoElIdNoEstaEnElListado` | `PantallaArtistaProvisionalFavoritoTest.noFavoritoMuestraElBotonActivoParaAgregar` |
| 4 | Si falla la comprobación, el botón aparece desactivado con opción de reintentar | `FavoritoViewModelTest.comprobandoPasaAErrorComprobacionCuandoFallaLaConsulta`, `…reintentarComprobacionPasaDeErrorComprobacionAFavoritoOtraVez`, `FavoriteRepositoryTest.esFavoritoDevuelveErrorDeCargaSiLaConsultaFalla` | `PantallaArtistaProvisionalFavoritoTest.errorComprobacionMuestraElBotonDesactivadoYPermiteReintentar` |
| 5 | El Visitante no ve el botón de favorito | — (decisión de composición en `VinilosNavHost`, ver design.md) | `PantallaArtistaProvisionalFavoritoTest.visitanteNoVeNingunBotonDeFavorito` |
| 6 | Si el artista ya no existe al guardar, "El artista ya no está disponible" + botón para volver al listado actualizado | `FavoritoViewModelTest.guardarFavoritoPasaANoDisponibleCuandoElArtistaYaNoExiste` | `PantallaArtistaProvisionalFavoritoTest.noDisponibleMuestraElMensajeYElBotonParaVolverAlListado` |
| 7 | Si falla la conexión/servicio al guardar, "No se pudo guardar el favorito, intenta de nuevo" + botón disponible para reintentar | `FavoritoViewModelTest.guardarFavoritoPasaAErrorGuardadoYPermiteReintentar`, `FavoriteRepositoryTest.agregarFavoritoDevuelveErrorDeCargaSiFallaLaConexion` | `PantallaArtistaProvisionalFavoritoTest.errorGuardadoMuestraElMensajeYElBotonDisponibleParaReintentar`, `…errorGuardadoNoMuestraNingunaConfirmacion` |
| 8 | Mientras se guarda, el botón se desactiva y muestra "Guardando...", evitando toques repetidos | `FavoritoViewModelTest.segundoToqueMientrasGuardandoSeIgnora` | `PantallaArtistaProvisionalFavoritoTest.guardandoMuestraElBotonDesactivadoConSuTexto` |
| 9 | Al guardar correctamente, mensaje breve de confirmación con el nombre del artista, que desaparece solo tras unos segundos; no aparece si falla | `FavoritoViewModelTest.confirmacionMostradaOcultaElMensajeDeFavorito` | `PantallaArtistaProvisionalFavoritoTest.favoritoConConfirmacionMuestraElMensajeConElNombre`, `…laConfirmacionDesapareceSolaDespuesDeUnosSegundos` |

Cobertura: 9/9 criterios con al menos una prueba automatizada (unitaria o de Compose UI).

## Protecciones adicionales cubiertas por pruebas

| Comportamiento | Prueba |
|---|---|
| Respuesta tardía de una comprobación anterior no sobrescribe la vigente | `FavoritoViewModelTest.respuestaTardiaDeComprobacionNoReemplazaLaConsultaVigente` |
| Cancelación de la comprobación no se reporta como error de carga | `FavoriteRepositoryTest.cancelarLaComprobacionPropagaLaCancelacionSinErrorDeCarga` |

## Resultado de la última ejecución

- **Pruebas unitarias (JVM, `.\gradlew.bat testDebugUnitTest`)**: 170/170 OK (incluye las 8 de `FavoriteRepositoryTest` y las 11 de `FavoritoViewModelTest` de HU10, sin regresiones en el resto del proyecto).
- **Pruebas de Compose UI de HU10 (`PantallaArtistaProvisionalFavoritoTest`, 11 pruebas)**: implementadas y compiladas (`compileDebugAndroidTestKotlin` sin errores); **no ejecutadas** por quien implementó el código (sin acceso a emulador/dispositivo en esta sesión de trabajo). Deben ejecutarse en un dispositivo/emulador antes de cerrar la historia.

## Pendiente (no bloquea la implementación, sí el cierre de la historia)

- Ejecutar en dispositivo/emulador las 11 pruebas de `PantallaArtistaProvisionalFavoritoTest` (comando sugerido más abajo).
- Pruebas E2E de HU10: a cargo de otro integrante del equipo; no se incluyen en este change (`add-artistas-favoritos`).
- Diseño visual detallado del botón y los mensajes (T10-1): a cargo de diseño; esta implementación solo fija los estados y el contrato de datos (ver `design.md`).
- Detalle real de artista (HU04): el botón se integró en `PantallaArtistaProvisional` (pantalla provisional de HU03) de forma explícitamente temporal; se debe migrar cuando HU04 exista.

### Comando para ejecutar las pruebas instrumentadas pendientes

Con un emulador o dispositivo conectado:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
.\gradlew.bat connectedDebugAndroidTest --tests "com.example.vinilos.ui.artistas.PantallaArtistaProvisionalFavoritoTest"
```
