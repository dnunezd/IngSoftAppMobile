package com.example.vinilos.ui.catalogo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vinilos.RepositorioAlbumesFalso
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AlbumsViewModel

/** El catálogo conectado a su ViewModel real (creado como en el NavHost) y a un repositorio sin red. */
@Composable
fun CatalogoConViewModel(repositorio: RepositorioAlbumesFalso) {
    val viewModel: AlbumsViewModel = viewModel(
        factory = viewModelFactory { initializer { AlbumsViewModel(repositorio) } }
    )
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    VinilosTheme {
        PantallaCatalogo(
            estado = estado,
            session = Session.Visitor,
            onActualizar = viewModel::actualizar,
            onReintentar = viewModel::reintentar,
            onAvisoMostrado = viewModel::avisoMostrado,
            onSeleccionar = {},
            onAtras = {}
        )
    }
}

/** Álbumes "Álbum 01" a "Álbum NN" con ids 1 a NN, ya en orden alfabético. */
fun albumesDePrueba(cantidad: Int) = (1..cantidad).map { n ->
    AlbumResumen(n, "Álbum %02d".format(n), null, "Rock", "Artista $n", "19${(n % 90) + 10}")
}

/** Tags de las tarjetas de álbum en el orden en que están en la cuadrícula. */
fun ComposeTestRule.tagsDeAlbumesEnPantalla(): List<String> =
    onAllNodes(SemanticsMatcher("es un álbum") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("album_") == true
    }).fetchSemanticsNodes().map { it.config[SemanticsProperties.TestTag] }
