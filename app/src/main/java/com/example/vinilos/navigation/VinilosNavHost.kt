package com.example.vinilos.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.ui.artistas.PantallaArtistasProvisional
import com.example.vinilos.ui.catalogo.PantallaCatalogoProvisional
import com.example.vinilos.ui.ingreso.PantallaColeccionistas
import com.example.vinilos.ui.ingreso.PantallaIngreso
import com.example.vinilos.ui.inicio.PantallaInicio
import com.example.vinilos.viewmodel.CollectorsViewModel
import com.example.vinilos.viewmodel.SessionViewModel

object Rutas {
    const val Ingreso = "ingreso"
    const val Coleccionistas = "coleccionistas"
    const val Inicio = "inicio"
    const val Catalogo = "catalogo"
    const val Artistas = "artistas"
}

/**
 * Grafo de navegación: ingreso -> (coleccionistas) -> inicio -> catálogo | artistas. Al entrar al
 * inicio el ingreso queda en la pila (para poder cambiar de perfil) y el selector sale de ella.
 */
@Composable
fun VinilosNavHost(
    navController: NavHostController,
    sessionViewModel: SessionViewModel,
    collectorRepository: CollectorRepository,
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
                onAtras = { navController.popBackStack() }
            )
        }
        composable(Rutas.Artistas) {
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            PantallaArtistasProvisional(
                session = session,
                onAtras = { navController.popBackStack() }
            )
        }
    }
}
