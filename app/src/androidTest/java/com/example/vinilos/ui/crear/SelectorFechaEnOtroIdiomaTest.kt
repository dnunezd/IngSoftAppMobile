package com.example.vinilos.ui.crear

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.CrearAlbumUiState
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Con un idioma sin traducción (francés), la app cae al español y el selector de fecha también debe
 * salir en español, incluidos los textos propios de Material ("Fecha seleccionada"). El Dialog del
 * selector abre otra composición con el contexto del dispositivo; sin aplicar ahí el idioma de la app,
 * esos textos salían en francés (internacionalizar-ingles, D4).
 */
class SelectorFechaEnOtroIdiomaTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private var escenario: ActivityScenario<ComponentActivity>? = null

    @Before
    fun appEnFrances() {
        assumeTrue("El idioma por aplicación existe desde Android 13", Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        contexto.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("fr")
    }

    @After
    fun appEnEspanol() {
        escenario?.close()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            contexto.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("es")
        }
    }

    @Test
    fun conElTelefonoEnUnIdiomaSinTraduccionElSelectorSaleEnEspanol() {
        escenario = ActivityScenario.launch(ComponentActivity::class.java).onActivity {
            it.setContent {
                VinilosTheme {
                    PantallaCrearAlbum(
                        estado = CrearAlbumUiState(),
                        session = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co")),
                        acciones = AccionesCrearAlbum()
                    )
                }
            }
        }
        rule.onNodeWithTag(TagFechaAlbum).performScrollTo().performClick()
        rule.onAllNodesWithText("Fecha seleccionada", useUnmergedTree = true).fetchSemanticsNodes().let {
            check(it.isNotEmpty()) { "El selector no muestra \"Fecha seleccionada\"" }
        }
        check(rule.onAllNodesWithText("Date sélectionnée", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            "El selector mezcla el francés del dispositivo"
        }
    }
}
