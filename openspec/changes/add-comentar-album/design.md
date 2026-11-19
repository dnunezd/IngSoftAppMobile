# Design

## Context

- **Backend:** BackVynils expone `POST /albums/{albumId}/comments`.
  - Cuerpo: `{"description": string, "rating": number, "collector": {"id": number}}`.
  - Responde 200 con el comentario guardado.
  - Valida con Joi que `description` sea un texto no vacío y `rating` esté entre 0 y 5.
  - Responde 404 si el álbum o el coleccionista no existen, con el mismo código para los dos, y 400 si los datos no son válidos.
  - Sin `collector` en el cuerpo, falla con 500.
- **App:**
  - El detalle del álbum (`PantallaDetalleAlbum` + `AlbumViewModel`) ya muestra los comentarios del álbum.
  - La sesión (`SessionViewModel`) sabe qué coleccionista ingresó.
  - HU07 (`CrearAlbumViewModel`) y HU10 (`FavoritoViewModel`) ya resolvieron un envío al backend con estados de envío, guarda contra dos toques y resultado hacia la pantalla anterior por `savedStateHandle`.

## Goals / Non-Goals

**Goals:**
- Cumplir los 7 criterios de aceptación de la HU09.
- Incluir lo que se decidió adoptar de los mockups: pantalla de éxito, diálogo de descarte, límite de 400 caracteres y texto de la calificación.

**Non-Goals:**
- Pruebas E2E (T09-5, #51).
- Elegir el álbum desde el menú principal.
- Editar o borrar comentarios.
- Guardar borradores entre cierres de la app.

## Decisions

### D1. Repositorio propio para los comentarios
`ComentarioRepository` (interfaz) y `ComentarioRepositoryImpl` publican el comentario con `AlbumApi`. Un repositorio aparte, en vez de un método nuevo en `AlbumRepository`, evita tocar los repositorios falsos de álbumes de todas las pruebas, y sigue la separación de `FavoriteRepository`.

```kotlin
interface ComentarioRepository {
    suspend fun publicarComentario(albumId: Int, comentario: ComentarioNuevo): Result<Unit>
}
data class ComentarioNuevo(val coleccionistaId: Int, val calificacion: Int, val descripcion: String)
```

- **Envío:** una interfaz Retrofit propia, `ComentarioApi.publicarComentario(@Path("id") albumId, @Body ComentarioNuevoDto)`, con `ComentarioNuevoDto(description, rating, collector = ColeccionistaRefDto(id))`. Va aparte de `AlbumApi` para no tocar las `AlbumApi` falsas de `AlbumRepositoryTest` y `RepositoriosDispatcherTest`.
- **Errores:** pasa por `ServiceAdapter.llamar`, así que cualquier fallo es `ErrorDeCarga`. El 404 no se distingue, porque el backend usa el mismo código para el álbum y para el coleccionista inexistentes, y el criterio pide el mismo mensaje para todos los errores.
- **Respuesta:** se ignora. La pantalla de éxito muestra lo que escribió el coleccionista, y el detalle vuelve a consultar el álbum.
- **Hilo:** corre en el dispatcher inyectado, como los demás repositorios.

### D2. Estado de la pantalla
`ComentarAlbumUiState` es un `data class`, como el de HU07, porque lo escrito debe conservarse en todas las etapas:

```kotlin
data class ComentarAlbumUiState(
    val album: CargaAlbum = CargaAlbum.Cargando,   // Cargando | Listo(album) | Error
    val calificacion: Int? = null,                  // 1..5
    val opinion: String = "",                       // máximo 400
    val envio: EnvioComentario = EnvioComentario.Editando, // Editando | Enviando | Fallido | Publicado
    val confirmarDescarte: Boolean = false,
    val salir: Boolean = false
)
```

Propiedades derivadas, con su prueba:
- `puedePublicar`: álbum listo, calificación elegida, opinión no vacía sin contar espacios, y sin envío en curso ni publicado.
- `tieneBorrador`: hay calificación u opinión, y no se ha publicado.

### D3. El álbum se obtiene en el ViewModel
`ComentarAlbumViewModel(albumId, coleccionista, albumRepository, comentarioRepository)` carga el álbum:
- primero con `obtenerAlbumGuardado(id)`, que casi siempre lo tiene, porque se llega desde el detalle;
- si no está guardado, con `obtenerAlbum(id)`.

Así la pantalla no depende del `ViewModel` del detalle, y se reconstruye sola si el proceso muere. Si las dos fallan, el estado es `Error`, con `reintentar()`.

### D4. Guardas del envío
`publicar()` no hace nada si `!puedePublicar`. El botón deshabilitado no basta: dos toques seguidos llegan antes de que la pantalla se redibuje, y el backend no impide comentarios repetidos.

Mientras el envío está en curso o después de publicar, editar la calificación o la opinión no cambia nada. Editar después de un error vuelve el envío a `Editando`, y con eso se oculta el mensaje de error.

### D5. Salir con borrador
- `pedirSalir()`:
  - durante el envío, no hace nada;
  - con borrador, abre el diálogo (`confirmarDescarte = true`);
  - si no, marca `salir = true`.
- `continuarEscribiendo()` cierra el diálogo.
- `descartar()` marca `salir = true`.
- Después de publicar, `pedirSalir()` también sale.

La navegación observa `salir` y cierra la pantalla, igual que HU07 observa `EnvioAlbum.Creado`. El diálogo vive en el estado, así que sobrevive al giro. La flecha y el `BackHandler` llaman a `pedirSalir()`.

### D6. Pantalla de éxito en la misma ruta
Con `EnvioComentario.Publicado`, la ruta `album/{id}/comentar` dibuja el contenido de éxito en lugar del formulario. No se navega a otra ruta, y el resumen sale del mismo estado. "Aceptar", la flecha y el botón atrás llaman a `pedirSalir()`, que con el comentario publicado sale directamente.

### D7. El detalle se actualiza al volver
Al salir después de publicar, la ruta deja `comentario_publicado = true` en el `savedStateHandle` del detalle. El detalle llama a `AlbumViewModel.actualizar()` y borra la clave.

`actualizar()` es nuevo:
- vuelve a consultar el álbum manteniendo a la vista el que ya se muestra, con `actualizando = true`, en lugar de pasar por `Cargando`;
- si la consulta falla, conserva el álbum con el aviso de error, como ya hace con lo guardado.

La caché HTTP revalida `GET /albums/{id}`. Como el comentario cambió el detalle, el backend responde 200 con el cuerpo nuevo, y el repositorio lo guarda en Room.

### D8. Acceso y sesión
- `PantallaDetalleAlbum` recibe `onComentar: (() -> Unit)? = null`, y el botón solo se dibuja si no es `null`. La navegación lo pasa solo si la sesión es de Coleccionista. Así el Visitante no lo ve, y las pruebas instrumentadas actuales no cambian.
- La ruta de comentar se cierra si se restaura sin sesión de Coleccionista, igual que la de crear álbum.

### D9. Textos
Todos los textos van en `strings.xml`. Las etiquetas de la calificación son "Muy malo", "Malo", "Regular", "Bueno" y "Excelente", con el formato "%1$s (%2$d/5)". Los textos de los criterios se usan tal cual:
- "Escribe tu opinión";
- "Publicar comentario";
- "Comentario publicado";
- "No se pudo publicar, intenta de nuevo".

## Risks / Trade-offs

- **El backend no distingue un álbum inexistente de un coleccionista inexistente:** los dos responden 404, así que se muestra el mismo mensaje. Lo pide así el criterio.
- **Comentarios repetidos:** el backend los acepta. Las guardas de D4 evitan el doble envío desde la app.
- **Sin pruebas instrumentadas nuevas de la pantalla:** la lógica queda en el `ViewModel`, con pruebas unitarias. La UI se verifica a mano, y el flujo completo, con las E2E de T09-5.

## Migration Plan

No aplica: no cambia el esquema de Room ni ningún dato guardado.
