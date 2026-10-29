package com.example.vinilos.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.ui.artistas.PantallaArtistaProvisional
import com.example.vinilos.ui.artistas.PantallaArtistas
import com.example.vinilos.ui.catalogo.PantallaCatalogoProvisional
import com.example.vinilos.ui.detalle.PantallaDetalleAlbum
import com.example.vinilos.ui.ingreso.PantallaColeccionistas
import com.example.vinilos.ui.ingreso.PantallaIngreso
import com.example.vinilos.ui.inicio.PantallaInicio
import com.example.vinilos.viewmodel.AlbumViewModel
import com.example.vinilos.viewmodel.ArtistsViewModel
import com.example.vinilos.viewmodel.CollectorsViewModel
import com.example.vinilos.viewmodel.SessionViewModel

object Rutas {
    const val Ingreso = "ingreso"
    const val Coleccionistas = "coleccionistas"
    const val Inicio = "inicio"
    const val Catalogo = "catalogo"
    const val Artistas = "artistas"

    /** Detalle del artista (HU04). El nombre viaja para el destino provisional; HU04 lo consulta por id. */
    const val DetalleArtista = "artista/{tipo}/{id}?nombre={nombre}"

    /** Detalle del álbum (HU02). */
    const val DetalleAlbum = "album/{id}"

    fun detalleArtista(tipo: ArtistType, id: Int, nombre: String) =
        "artista/${tipo.name}/$id?nombre=${Uri.encode(nombre)}"

    fun detalleAlbum(id: Int) = "album/$id"
}

/**
 * Grafo de navegación: ingreso -> (coleccionistas) -> inicio -> catálogo | artistas -> detalle del
 * artista. Al entrar al
 * inicio el ingreso queda en la pila (para poder cambiar de perfil) y el selector sale de ella.
 */
@Composable
fun VinilosNavHost(
    navController: NavHostController,
    sessionViewModel: SessionViewModel,
    collectorRepository: CollectorRepository,
    artistRepository: ArtistRepository,
    albumRepository: AlbumRepository,
    modifier: Modifier = Modifier
) {
    fun irAlInicio() {
        navController.navigate(Rutas.Inicio) {
            popUpTo(Rutas.Ingreso) { inclusive = false }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Rutas.Ingreso,
        modifier = modifier
    ) {
        composable(Rutas.Ingreso) {
            // Volver al ingreso (desde el selector o el inicio) deja la sesión sin perfil.
            LaunchedEffect(Unit) { sessionViewModel.clear() }
            PantallaIngreso(
                onVisitante = {
                    sessionViewModel.enterAsVisitor()
                    irAlInicio()
                },
                onColeccionista = { navController.navigate(Rutas.Coleccionistas) }
            )
        }
        composable(Rutas.Coleccionistas) {
            val viewModel: CollectorsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { CollectorsViewModel(collectorRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            PantallaColeccionistas(
                estado = estado,
                onBuscar = viewModel::buscar,
                onReintentar = viewModel::reintentar,
                onSeleccionar = { coleccionista ->
                    sessionViewModel.enterAsCollector(coleccionista)
                    irAlInicio()
                },
                onVisitante = {
                    sessionViewModel.enterAsVisitor()
                    irAlInicio()
                },
                onAtras = { navController.popBackStack() }
            )
        }
        composable(Rutas.Inicio) {
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            PantallaInicio(
                session = session,
                onVerAlbumes = { navController.navigate(Rutas.Catalogo) },
                onVerArtistas = { navController.navigate(Rutas.Artistas) }
            )
        }
        composable(Rutas.Catalogo) {
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            PantallaCatalogoProvisional(
                session = session,
                onAtras = { navController.popBackStack() },
                onSeleccionarAlbum = { id ->
                    navController.navigate(Rutas.detalleAlbum(id)) { launchSingleTop = true }
                }
            )
        }
        composable(Rutas.Artistas) {
            val viewModel: ArtistsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ArtistsViewModel(artistRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            PantallaArtistas(
                estado = estado,
                onBuscar = viewModel::buscar,
                onReintentar = viewModel::reintentar,
                onSeleccionar = { artista ->
                    navController.navigate(Rutas.detalleArtista(artista.type, artista.id, artista.name)) {
                        launchSingleTop = true
                    }
                },
                onAtras = { navController.popBackStack() }
            )
        }
        composable(
            Rutas.DetalleArtista,
            arguments = listOf(
                navArgument("tipo") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType },
                navArgument("nombre") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entrada ->
            val argumentos = requireNotNull(entrada.arguments)
            PantallaArtistaProvisional(
                nombre = argumentos.getString("nombre").orEmpty(),
                tipo = ArtistType.valueOf(requireNotNull(argumentos.getString("tipo"))),
                onAtras = { navController.popBackStack() }
            )
        }
        composable(
            Rutas.DetalleAlbum,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entrada ->
            val id = requireNotNull(entrada.arguments).getInt("id")
            val viewModel: AlbumViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AlbumViewModel(id, albumRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            PantallaDetalleAlbum(
                estado = estado,
                onAtras = { navController.popBackStack() },
                onReintentar = viewModel::reintentar
            )
        }
    }
}
