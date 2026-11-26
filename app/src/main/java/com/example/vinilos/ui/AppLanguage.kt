package com.example.vinilos.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.vinilos.R
import java.util.Locale

/**
 * Idioma en que la app muestra sus textos (internacionalizar-ingles, D1): el de los recursos que
 * Android eligió, no el del dispositivo. Con el teléfono en francés la app cae al español, y las
 * fechas y números también deben salir en español.
 */
fun idiomaDeLaApp(contexto: Context): Locale = Locale.forLanguageTag(contexto.getString(R.string.idioma))

@Composable
fun idiomaDeLaApp(): Locale = Locale.forLanguageTag(stringResource(R.string.idioma))
