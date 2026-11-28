package com.example.vinilos.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.vinilos.R
import java.util.Locale

/**
 * Language in which the app shows its texts (internacionalizar-ingles, D1): the one chosen by
 * Android for the resources, not the device's own locale. With the phone set to French the app
 * falls back to Spanish, and dates and numbers must also come out in Spanish.
 */
fun idiomaDeLaApp(contexto: Context): Locale = Locale.forLanguageTag(contexto.getString(R.string.idioma))

@Composable
fun idiomaDeLaApp(): Locale = Locale.forLanguageTag(stringResource(R.string.idioma))
