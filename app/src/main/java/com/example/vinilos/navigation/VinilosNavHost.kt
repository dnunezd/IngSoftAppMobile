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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.data.repository.FavoriteRepository
import com.example.vinilos.ui.artistas.PantallaArtistas
import com.example.vinilos.ui.artistas.PantallaDetalleArtista
import com.example.vinilos.ui.catalogo.PantallaCatalogo
import com.example.vinilos.ui.crear.AccionesCrearAlbum
import com.example.vinilos.ui.crear.PantallaCrearAlbum
import com.example.vinilos.ui.detalle.PantallaDetalleAlbum
import com.example.vinilos.ui.ingreso.PantallaColeccionistas
import com.example.vinilos.ui.ingreso.PantallaIngreso
import com.example.vinilos.ui.inicio.PantallaInicio
import com.example.vinilos.viewmodel.AlbumViewModel
import com.example.vinilos.viewmodel.AlbumsViewModel
import com.example.vinilos.viewmodel.ArtistaDetalleViewModel
import com.example.vinilos.viewmodel.ArtistsViewModel
import com.example.vinilos.viewmodel.CollectorsViewModel
import com.example.vinilos.viewmodel.CrearAlbumViewModel
import com.example.vinilos.viewmodel.EnvioAlbum
import com.example.vinilos.viewmodel.FavoritoViewModel
import com.example.vinilos.viewmodel.SessionViewModel

object Rutas {
    const val Ingreso = "ingreso"
    const val Coleccionistas = "coleccionistas"
    const val Inicio = "inicio"
    const val Catalogo = "catalogo"
    const val Artistas = "artistas"

    /** Detalle del artista (HU04). El tipo distingue a un músico de una banda con el mismo id. */
    const val DetalleArtista = "artista/{tipo}/{id}"

    /** Detalle del álbum (HU02). */
    const val DetalleAlbum = "album/{id}"

    /** Formulario para crear un álbum (HU07), solo para el Coleccionista. */
    const val CrearAlbum = "crear-album"

    fun detalleArtista(tipo: ArtistType, id: Int) = "artista/${tipo.name}/$id"

    fun detalleAlbum(id: Int) = "album/$id"
}

/** Clave con la que el formulario le entrega al catálogo el id del álbum creado. */
private const val ClaveAlbumCreado = "album_creado"

/** Clave con la que el detalle del artista avisa al listado que un favorito ya no está disponible. */
private const val ClaveArtistaEliminado = "artista_eliminado"

/**
 * Grafo de navegación: ingreso -> (coleccionistas) -> inicio -> catálogo -> detalle del álbum |
 * crear álbum, y artistas -> detalle del artista -> detalle del álbum. Al entrar al
 * inicio el ingreso queda en la pila (para poder cambiar de perfil) y el selector sale de ella.
 */
@Composable
fun VinilosNavHost(
    navController: NavHostController,
    sessionViewModel: SessionViewModel,
    collectorRepository: CollectorRepository,
    artistRepository: ArtistRepository,
    albumRepository: AlbumRepository,
    favoriteRepository: FavoriteRepository,
    modifier: Modifier = Modifier
) {
    fun irAlInicio() {
        navController.navigate(Rutas.Inicio) {
            popUpTo(Rutas.Ingreso) { inclusive = false }
        }
    }

    /** Dos toques rápidos no deben sacar también el catálogo. */
    fun cerrarFormulario() {
        if (navController.currentDestination?.route == Rutas.CrearAlbum) navController.popBackStack()
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
        composable(Rutas.Catalogo) { entrada ->
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            val viewModel: AlbumsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AlbumsViewModel(albumRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            val albumNuevo by viewModel.albumNuevo.collectAsStateWithLifecycle()
            val albumCreado by entrada.savedStateHandle
                .getStateFlow<Int?>(ClaveAlbumCreado, null)
                .collectAsStateWithLifecycle()
            LaunchedEffect(albumCreado) {
                albumCreado?.let { id ->
                    viewModel.albumCreado(id)
                    // Se borra para que girar la pantalla no repita el aviso ni la consulta.
                    entrada.savedStateHandle[ClaveAlbumCreado] = null
                }
            }
            PantallaCatalogo(
                estado = estado,
                session = session,
                onActualizar = viewModel::actualizar,
                onReintentar = viewModel::reintentar,
                onAvisoMostrado = viewModel::avisoMostrado,
                onSeleccionar = { id ->
                    navController.navigate(Rutas.detalleAlbum(id)) { launchSingleTop = true }
                },
                onAtras = { navController.popBackStack() },
                albumNuevo = albumNuevo,
                onCrearAlbum = { navController.navigate(Rutas.CrearAlbum) { launchSingleTop = true } },
                onAvisoCreadoMostrado = viewModel::avisoCreadoMostrado,
                onDesplazadoAlNuevo = viewModel::desplazadoAlNuevo
            )
        }
        composable(Rutas.CrearAlbum) {
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            val viewModel: CrearAlbumViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { CrearAlbumViewModel(albumRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            // La pila se restaura al morir el proceso, pero la sesión no.
            LaunchedEffect(session) {
                if (!session.collectorActionsEnabled) cerrarFormulario()
            }
            val envio = estado.envio
            LaunchedEffect(envio) {
                if (envio is EnvioAlbum.Creado) {
                    navController.previousBackStackEntry?.savedStateHandle?.set(ClaveAlbumCreado, envio.id)
                    cerrarFormulario()
                }
            }
            PantallaCrearAlbum(
                estado = estado,
                session = session,
                acciones = AccionesCrearAlbum(
                    onNombre = viewModel::cambiarNombre,
                    onPortada = viewModel::cambiarPortada,
                    onFecha = viewModel::cambiarFecha,
                    onGenero = viewModel::cambiarGenero,
                    onDisquera = viewModel::cambiarDisquera,
                    onDescripcion = viewModel::cambiarDescripcion,
                    onCrear = viewModel::crear,
                    onCancelar = ::cerrarFormulario
                )
            )
        }
        composable(Rutas.Artistas) { entrada ->
            val viewModel: ArtistsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ArtistsViewModel(artistRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            val artistaEliminado by entrada.savedStateHandle
                .getStateFlow(ClaveArtistaEliminado, false)
                .collectAsStateWithLifecycle()
            LaunchedEffect(artistaEliminado) {
                if (artistaEliminado) {
                    // El artista ya no existe en el backend: se vuelve a consultar para que el
                    // listado no lo siga mostrando (popBackStack por sí solo no alcanza).
                    viewModel.reintentar()
                    entrada.savedStateHandle[ClaveArtistaEliminado] = false
                }
            }
            PantallaArtistas(
                estado = estado,
                onBuscar = viewModel::buscar,
                onReintentar = viewModel::reintentar,
                onSeleccionar = { artista ->
                    navController.navigate(Rutas.detalleArtista(artista.type, artista.id)) {
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
                navArgument("id") { type = NavType.IntType }
            )
        ) { entrada ->
            val argumentos = requireNotNull(entrada.arguments)
            val tipo = ArtistType.valueOf(requireNotNull(argumentos.getString("tipo")))
            val artistId = argumentos.getInt("id")
            // Ligado a la entrada de navegación: sobrevive a la rotación y a la ida y vuelta al álbum.
            val viewModel: ArtistaDetalleViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ArtistaDetalleViewModel(tipo, artistId, artistRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            val coleccionista = (session as? com.example.vinilos.data.model.Session.Collector)?.collector
            if (coleccionista != null) {
                // Solo el Coleccionista comprueba y guarda favoritos; el Visitante no ve el botón.
                val favoritoViewModel: FavoritoViewModel = viewModel(
                    key = "favorito-$artistId",
                    factory = viewModelFactory {
                        initializer {
                            FavoritoViewModel(coleccionista.id, artistId, tipo, favoriteRepository)
                        }
                    }
                )
                val estadoFavorito by favoritoViewModel.uiState.collectAsStateWithLifecycle()
                PantallaDetalleArtista(
                    estado = estado,
                    onSeleccionarAlbum = { idAlbum ->
                        navController.navigate(Rutas.detalleAlbum(idAlbum)) { launchSingleTop = true }
                    },
                    onReintentar = viewModel::reintentar,
                    onAtras = { navController.popBackStack() },
                    estadoFavorito = estadoFavorito,
                    onAgregarFavorito = favoritoViewModel::agregarFavorito,
                    onReintentarFavorito = favoritoViewModel::reintentarComprobacion,
                    onConfirmacionFavoritoMostrada = favoritoViewModel::confirmacionMostrada,
                    onVolverAlListado = {
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(ClaveArtistaEliminado, true)
                        navController.popBackStack()
                    }
                )
            } else {
                PantallaDetalleArtista(
                    estado = estado,
                    onSeleccionarAlbum = { idAlbum ->
                        navController.navigate(Rutas.detalleAlbum(idAlbum)) { launchSingleTop = true }
                    },
                    onReintentar = viewModel::reintentar,
                    onAtras = { navController.popBackStack() }
                )
            }
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
