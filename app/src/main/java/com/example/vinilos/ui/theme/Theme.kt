package com.example.vinilos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// The Vinilos mockup only exists in light mode, so the theme is always light and branded
// (no dynamic color or dark scheme).
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
    // Dialogs, menus and sheets take the palette instead of Material 3's default purple.
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
