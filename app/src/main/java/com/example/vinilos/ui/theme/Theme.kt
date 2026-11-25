package com.example.vinilos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// El mockup de Vinilos solo existe en claro, así que el tema es siempre claro y de marca
// (sin color dinámico ni esquema oscuro).
private val EsquemaClaro = lightColorScheme(
    primary = NaranjaContraste,
    onPrimary = Superficie,
    primaryContainer = NaranjaSuave,
    onPrimaryContainer = NaranjaOscuro,
    secondary = NaranjaOscuro,
    onSecondary = Superficie,
    tertiary = NaranjaOscuro,
    background = Fondo,
    onBackground = TextoPrincipal,
    surface = Superficie,
    surfaceTint = Superficie,
    onSurface = TextoPrincipal,
    surfaceVariant = SuperficieVariante,
    // Diálogos, menús y hojas toman la paleta y no el lila por defecto de Material 3.
    surfaceContainerLowest = Superficie,
    surfaceContainerLow = Fondo,
    surfaceContainer = Fondo,
    surfaceContainerHigh = Superficie,
    surfaceContainerHighest = SuperficieVariante,
    onSurfaceVariant = TextoSecundario,
    outline = Contorno,
    error = ErrorFormulario,
    errorContainer = ErrorFormularioFondo,
    onErrorContainer = ErrorFormularioTexto
)

@Composable
fun VinilosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        content = content
    )
}
