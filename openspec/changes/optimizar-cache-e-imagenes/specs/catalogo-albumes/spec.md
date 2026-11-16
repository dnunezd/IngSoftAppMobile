# Spec Delta

## ADDED Requirements

### Requirement: Álbum creado visible sin esperar la consulta
Al volver del formulario tras crear un álbum, el catálogo SHALL mostrar de inmediato el álbum creado en su posición alfabética, con la insignia "NUEVO", sin esperar la respuesta de `GET /albums`. La consulta MUST seguir haciéndose, con el indicador de actualización, y su respuesta reemplaza la lista. Si la app no pudo guardar el álbum creado, el catálogo se comporta como antes: el álbum aparece cuando responde la consulta.

#### Scenario: Backend lento
- **WHEN** el usuario crea "Abbey Road" y regresa al catálogo mientras `GET /albums` aún no responde
- **THEN** "Abbey Road" ya aparece en su posición alfabética con la insignia "NUEVO" y queda visible
- **AND** se muestra el indicador de actualización hasta que la consulta responde

#### Scenario: Falla la consulta posterior
- **WHEN** el álbum se creó y la consulta del catálogo que le sigue falla
- **THEN** se informa "No pudimos actualizar el catálogo"
- **AND** el catálogo sigue mostrando los álbumes que ya se veían y el álbum creado

#### Scenario: Otro dispositivo agregó un álbum
- **WHEN** el usuario crea un álbum y, mientras tanto, otro dispositivo agregó otro
- **THEN** el álbum creado aparece de inmediato y el otro aparece cuando responde la consulta

#### Scenario: Sin álbum guardado
- **WHEN** el usuario crea un álbum y la app no pudo guardarlo en el dispositivo
- **THEN** el álbum aparece cuando responde la consulta del catálogo, como antes
