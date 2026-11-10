# Spec Delta

## MODIFIED Requirements

### Requirement: Indicador de carga
Mientras se obtienen los álbumes y no hay un catálogo guardado en el dispositivo, la app SHALL mostrar un indicador de progreso en lugar de una pantalla vacía. Si hay un catálogo guardado, la app MUST mostrarlo en lugar del indicador de pantalla completa.

#### Scenario: Consulta en curso
- **WHEN** el catálogo se está consultando por primera vez y no hay un catálogo guardado
- **THEN** se muestra un indicador de progreso
- **AND** no se muestra una pantalla en blanco

#### Scenario: Consulta en curso con catálogo guardado
- **WHEN** el catálogo se está consultando y hay un catálogo guardado
- **THEN** se muestran los álbumes guardados con el indicador de actualización sobre la lista
- **AND** no se muestra el indicador de progreso de pantalla completa

### Requirement: Error de carga recuperable
Si el catálogo no se puede cargar por un problema de conexión o del servidor y no hay un catálogo guardado en el dispositivo, la app SHALL mostrar el mensaje "No pudimos cargar el catálogo" con un botón "Reintentar", y MUST NOT cerrarse.

#### Scenario: Backend no disponible
- **WHEN** no hay un catálogo guardado y la consulta falla por falta de conexión, por un error del servidor o por una respuesta ilegible
- **THEN** se muestra "No pudimos cargar el catálogo" y el botón "Reintentar"
- **AND** la app no se cierra

#### Scenario: Reintentar con éxito
- **WHEN** el usuario pulsa "Reintentar" y el backend ya responde
- **THEN** se muestra el indicador de progreso y luego el catálogo

## ADDED Requirements

### Requirement: Catálogo guardado en el dispositivo
La app SHALL guardar en el dispositivo la última respuesta exitosa del catálogo y mostrarla al abrir el catálogo mientras lo consulta. Cada consulta exitosa, incluida la de deslizar para actualizar y la que sigue a crear un álbum, MUST reemplazar por completo lo guardado. Lo guardado MUST conservarse al cerrar y volver a abrir la app. La app MUST NOT modificar lo guardado por su cuenta: solo lo reemplaza con respuestas del backend.

#### Scenario: Abrir sin conexión con catálogo guardado
- **WHEN** el usuario ya vio el catálogo en una apertura anterior de la app
- **AND** lo abre sin conexión o con el servidor caído
- **THEN** se muestran los álbumes guardados en orden alfabético
- **AND** se informa brevemente "No pudimos actualizar el catálogo", una sola vez
- **AND** no se muestra "No pudimos cargar el catálogo"

#### Scenario: Abrir con conexión y catálogo guardado
- **WHEN** hay un catálogo guardado y el backend responde con álbumes distintos
- **THEN** se muestran primero los álbumes guardados
- **AND** al terminar la consulta se muestran los álbumes de la respuesta

#### Scenario: Álbum eliminado en el backend
- **WHEN** un álbum guardado ya no está en la respuesta del backend
- **THEN** deja de mostrarse después de la consulta
- **AND** no vuelve a aparecer en la siguiente apertura

#### Scenario: El backend responde sin álbumes
- **WHEN** hay un catálogo guardado y el backend responde una lista vacía
- **THEN** se muestra "Aún no hay álbumes en el catálogo"
- **AND** la siguiente apertura sin conexión muestra "No pudimos cargar el catálogo"

#### Scenario: Álbum incompleto
- **WHEN** el backend entrega un álbum sin identificador, sin nombre o sin género
- **THEN** ese álbum no se guarda ni se muestra
- **AND** los demás álbumes se guardan normalmente

#### Scenario: Falla el almacenamiento del dispositivo
- **WHEN** no se puede leer ni escribir el catálogo guardado
- **THEN** el catálogo se comporta como si no hubiera nada guardado
- **AND** la app no se cierra

#### Scenario: Crear un álbum sin conexión
- **WHEN** el Coleccionista ve el catálogo guardado sin conexión y envía el formulario de crear álbum
- **THEN** el envío falla con el mensaje y el reintento que ya tiene el formulario
- **AND** el álbum no se agrega al catálogo guardado

### Requirement: Portadas del catálogo guardado
Las portadas del catálogo guardado SHALL mostrarse sin conexión cuando la imagen ya se descargó antes y sigue disponible en el dispositivo. En cualquier otro caso, la tarjeta MUST mostrar la imagen genérica y la lista MUST seguir funcionando. La app MUST NOT descargar portadas por adelantado para tenerlas sin conexión.

#### Scenario: Portada vista antes
- **WHEN** el usuario abre sin conexión el catálogo guardado
- **AND** la portada de un álbum se mostró en una apertura anterior y el dispositivo aún la conserva
- **THEN** su tarjeta muestra esa portada

#### Scenario: Portada nunca vista
- **WHEN** el usuario abre sin conexión el catálogo guardado
- **AND** la portada de un álbum nunca se mostró
- **THEN** su tarjeta muestra la imagen genérica
- **AND** los demás álbumes y el desplazamiento siguen funcionando
