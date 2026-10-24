# Spec Delta

## Purpose

Define el rango de versiones de Android en las que se puede instalar y usar Vinilos, y garantiza que la experiencia visible sea la misma en todo ese rango.

## ADDED Requirements

### Requirement: Versión mínima de Android soportada
La app SHALL poder instalarse y abrirse en dispositivos con Android 6.0 (API 23) o superior. La app MUST NOT exigir una versión de Android mayor que la 6.0 para instalarse.

#### Scenario: Instalación en Android 6.0
- **WHEN** se instala la app en un dispositivo con Android 6.0 (API 23)
- **THEN** la instalación se completa sin error de versión incompatible
- **AND** al abrirla se muestra la pantalla de ingreso

#### Scenario: Instalación en versiones posteriores
- **WHEN** se instala la app en un dispositivo con Android 8.0 o superior
- **THEN** la instalación se completa y la app se abre igual que antes de este cambio

### Requirement: Mismo comportamiento en todas las versiones soportadas
En cualquier versión soportada (API 23 o superior) la app SHALL mostrar las mismas pantallas, textos, estados y flujos de ingreso e inicio, y SHALL consultar al backend para listar coleccionistas con los mismos resultados y errores de carga.

#### Scenario: Flujo de ingreso en Android 6.0
- **WHEN** el usuario ingresa como Visitante o como coleccionista en un dispositivo con Android 6.0
- **THEN** ve la pantalla de inicio con el saludo, el titular y las tarjetas definidos en `pantalla-inicio`
- **AND** no aparece ningún cierre inesperado ni elemento sin dibujar

#### Scenario: Listado de coleccionistas en Android 6.0
- **WHEN** el usuario abre la elección de coleccionista en un dispositivo con Android 6.0 y el backend responde
- **THEN** se listan los coleccionistas igual que en versiones posteriores
- **AND** si el backend no responde se muestra el mismo error de carga que en versiones posteriores

### Requirement: Ícono del lanzador en todas las versiones soportadas
La app SHALL mostrar su ícono en el lanzador en todas las versiones soportadas: adaptativo desde Android 8.0 (API 26) y, en Android 6.0 a 7.1, un ícono de imagen fija equivalente.

#### Scenario: Ícono en Android 7.1 o anterior
- **WHEN** la app está instalada en un dispositivo con Android 6.0 a 7.1
- **THEN** el lanzador muestra el ícono de la app como imagen fija

#### Scenario: Ícono adaptativo en Android 8.0 o superior
- **WHEN** la app está instalada en un dispositivo con Android 8.0 o superior
- **THEN** el lanzador muestra el ícono adaptativo de la app
