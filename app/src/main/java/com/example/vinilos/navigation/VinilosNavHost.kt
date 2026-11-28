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
import com.example.vinilos.data.model.Session
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.data.repository.CommentRepository
import com.example.vinilos.data.repository.FavoriteRepository
import com.example.vinilos.ui.artistas.AssociateAlbumActions
import com.example.vinilos.ui.artistas.ArtistsScreen
import com.example.vinilos.ui.artistas.ArtistDetailScreen
import com.example.vinilos.ui.catalog.CatalogScreen
import com.example.vinilos.ui.comentar.AddCommentActions
import com.example.vinilos.ui.comentar.AddCommentScreen
import com.example.vinilos.ui.create.AddTrackActions
import com.example.vinilos.ui.create.CreateAlbumActions
import com.example.vinilos.ui.create.AddTrackScreen
import com.example.vinilos.ui.create.CreateAlbumScreen
import com.example.vinilos.ui.detail.AlbumDetailScreen
import com.example.vinilos.ui.entry.CollectorsScreen
import com.example.vinilos.ui.entry.EntryScreen
import com.example.vinilos.ui.home.HomeScreen
import com.example.vinilos.viewmodel.AlbumViewModel
import com.example.vinilos.viewmodel.AddTrackViewModel
import com.example.vinilos.viewmodel.AlbumsViewModel
import com.example.vinilos.viewmodel.ArtistDetailViewModel
import com.example.vinilos.viewmodel.ArtistsViewModel
import com.example.vinilos.viewmodel.AssociateAlbumViewModel
import com.example.vinilos.viewmodel.CollectorsViewModel
import com.example.vinilos.viewmodel.AddCommentViewModel
import com.example.vinilos.viewmodel.CreateAlbumViewModel
import com.example.vinilos.viewmodel.EnvioAlbum
import com.example.vinilos.viewmodel.TrackSubmission
import com.example.vinilos.viewmodel.FavoriteViewModel
import com.example.vinilos.viewmodel.SessionViewModel

object Routes {
    const val INGRESO = "ingreso"
    const val COLECCIONISTAS = "coleccionistas"
    const val INICIO = "inicio"
    const val CATALOGO = "catalogo"
    const val ARTISTAS = "artistas"

    /** Artist detail (HU04). The type distinguishes a musician from a band with the same id. */
    const val DETALLE_ARTISTA = "artista/{tipo}/{id}"

    /** Album detail (HU02). */
    const val DETALLE_ALBUM = "album/{id}"

    /** Form to create an album (HU07), only for the Collector. */
    const val CREAR_ALBUM = "crear-album"

    /** Form to add a track to the album (HU08), only for the Collector. */
    const val AGREGAR_CANCION = "album/{id}/agregar-cancion"

    /** Comment on an album (HU09), only for the Collector. */
    const val COMENTAR_ALBUM = "album/{id}/comentar"

    fun detalleArtista(tipo: ArtistType, id: Int) = "artista/${tipo.name}/$id"

    fun detalleAlbum(id: Int) = "album/$id"

    fun addTrack(id: Int) = "album/$id/agregar-cancion"

    fun addComment(id: Int) = "album/$id/comentar"
}

/** Key with which the form delivers to the catalog the id of the created album. */
private const val ClaveAlbumCreado = "album_creado"

/** Result with which the form requests a reload and notifies the album detail screen. */
private const val ClaveTrackAgregada = "cancion_agregada"

/** Key with which the comment screen notifies the detail screen that it must be refreshed (add-comentar-album, D7). */
private const val ClaveCommentPublicado = "comentario_publicado"

/** Key with which the artist detail screen notifies the list that a favorite is no longer available. */
private const val ClaveArtistaEliminado = "artista_eliminado"

/** Key with which the add-track form notifies the detail screen that the album no longer exists. */
private const val ClaveAlbumEliminado = "album_eliminado"

/**
 * Navigation graph: entry -> (collectors) -> home -> catalog -> album detail |
 * create album, artists -> artist detail -> album detail, and album detail -> comment. On entering
 * home, entry stays on the stack (to be able to switch profile) and the selector is popped off it.
 */
@Composable
fun VinilosNavHost(
    navController: NavHostController,
    sessionViewModel: SessionViewModel,
    collectorRepository: CollectorRepository,
    artistRepository: ArtistRepository,
    albumRepository: AlbumRepository,
    favoriteRepository: FavoriteRepository,
    comentarioRepository: CommentRepository,
    modifier: Modifier = Modifier
) {
    fun irAlInicio() {
        navController.navigate(Routes.INICIO) {
            popUpTo(Routes.INGRESO) { inclusive = false }
        }
    }

    /** Two quick taps shouldn't also pop the catalog. */
    fun cerrarFormulario() {
        if (navController.currentDestination?.route == Routes.CREAR_ALBUM) navController.popBackStack()
    }

    fun cerrarFormularioTrack() {
        if (navController.currentDestination?.route == Routes.AGREGAR_CANCION) navController.popBackStack()
    }

    /** Same as [cerrarFormulario]: only closes if comment screen is still the current destination. */
    fun cerrarComentar() {
        if (navController.currentDestination?.route == Routes.COMENTAR_ALBUM) navController.popBackStack()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.INGRESO,
        modifier = modifier
    ) {
        composable(Routes.INGRESO) {
            // Returning to entry (from the selector or home) leaves the session without a profile.
            LaunchedEffect(Unit) { sessionViewModel.clear() }
            EntryScreen(
                onVisitante = {
                    sessionViewModel.enterAsVisitor()
                    irAlInicio()
                },
                onColeccionista = { navController.navigate(Routes.COLECCIONISTAS) }
            )
        }
        composable(Routes.COLECCIONISTAS) {
            val viewModel: CollectorsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { CollectorsViewModel(collectorRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            CollectorsScreen(
                estado = estado,
                onBuscar = viewModel::search,
                onReintentar = viewModel::retry,
                onSeleccionar = { coleccionista ->
                    sessionViewModel.enterAsCollector(coleccionista)
                    irAlInicio()
                },
                onVisitante = {
                    sessionViewModel.enterAsVisitor()
                    irAlInicio()
                },
                onAtras = { navController.popBackStack() },
                onAvisoMostrado = viewModel::avisoMostrado
            )
        }
        composable(Routes.INICIO) {
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            HomeScreen(
                session = session,
                onVerAlbumes = { navController.navigate(Routes.CATALOGO) },
                onVerArtistas = { navController.navigate(Routes.ARTISTAS) }
            )
        }
        composable(Routes.CATALOGO) { entrada ->
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
                    // Cleared so rotating the screen doesn't repeat the notice or the query.
                    entrada.savedStateHandle[ClaveAlbumCreado] = null
                }
            }
            CatalogScreen(
                estado = estado,
                session = session,
                onActualizar = viewModel::actualizar,
                onReintentar = viewModel::retry,
                onAvisoMostrado = viewModel::avisoMostrado,
                onSeleccionar = { id ->
                    navController.navigate(Routes.detalleAlbum(id)) { launchSingleTop = true }
                },
                onAtras = { navController.popBackStack() },
                albumNuevo = albumNuevo,
                onCreateAlbum = { navController.navigate(Routes.CREAR_ALBUM) { launchSingleTop = true } },
                onAvisoCreadoMostrado = viewModel::avisoCreadoMostrado,
                onDesplazadoAlNuevo = viewModel::desplazadoAlNuevo
            )
        }
        composable(Routes.CREAR_ALBUM) {
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            val viewModel: CreateAlbumViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { CreateAlbumViewModel(albumRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            // The stack is restored when the process dies, but the session is not.
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
            CreateAlbumScreen(
                estado = estado,
                session = session,
                acciones = CreateAlbumActions(
                    onNombre = viewModel::cambiarNombre,
                    onPortada = viewModel::cambiarPortada,
                    onFecha = viewModel::cambiarFecha,
                    onGenero = viewModel::cambiarGenero,
                    onDisquera = viewModel::cambiarDisquera,
                    onDescripcion = viewModel::cambiarDescripcion,
                    onCrear = viewModel::create,
                    onCancelar = ::cerrarFormulario
                )
            )
        }
        composable(Routes.ARTISTAS) { entrada ->
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
                    // The artist no longer exists on the backend: it's re-fetched so the
                    // list stops showing it (popBackStack alone isn't enough).
                    viewModel.retry()
                    entrada.savedStateHandle[ClaveArtistaEliminado] = false
                }
            }
            ArtistsScreen(
                estado = estado,
                onBuscar = viewModel::search,
                onReintentar = viewModel::retry,
                onSeleccionar = { artista ->
                    navController.navigate(Routes.detalleArtista(artista.type, artista.id)) {
                        launchSingleTop = true
                    }
                },
                onAtras = { navController.popBackStack() },
                onAvisoMostrado = viewModel::avisoMostrado
            )
        }
        composable(
            Routes.DETALLE_ARTISTA,
            arguments = listOf(
                navArgument("tipo") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType }
            )
        ) { entrada ->
            val argumentos = requireNotNull(entrada.arguments)
            val tipo = ArtistType.valueOf(requireNotNull(argumentos.getString("tipo")))
            val artistId = argumentos.getInt("id")
            // Bound to the navigation entry: survives rotation and going back and forth to the album.
            val viewModel: ArtistDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ArtistDetailViewModel(tipo, artistId, artistRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            val coleccionista = (session as? Session.Collector)?.collector
            if (coleccionista != null) {
                // Only the Collector checks and saves favorites; the Visitor doesn't see the button.
                val favoritoViewModel: FavoriteViewModel = viewModel(
                    key = "favorito-$artistId",
                    factory = viewModelFactory {
                        initializer {
                            FavoriteViewModel(coleccionista.id, artistId, tipo, favoriteRepository)
                        }
                    }
                )
                val estadoFavorito by favoritoViewModel.uiState.collectAsStateWithLifecycle()
                // HU15: the sheet lives in the same entry as the detail screen, so the associated album is
                // delivered directly to the detail ViewModel, without savedStateHandle (add-agregar-album-artista, D5).
                val associateViewModel: AssociateAlbumViewModel = viewModel(
                    key = "asociar-$tipo-$artistId",
                    factory = viewModelFactory {
                        initializer { AssociateAlbumViewModel(tipo, artistId, albumRepository, artistRepository) }
                    }
                )
                val estadoAsociar by associateViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(estadoAsociar.albumAsociado) {
                    estadoAsociar.albumAsociado?.let {
                        viewModel.albumAsociado(it)
                        associateViewModel.asociadoNotificado()
                    }
                }
                ArtistDetailScreen(
                    estado = estado,
                    onSeleccionarAlbum = { idAlbum ->
                        navController.navigate(Routes.detalleAlbum(idAlbum)) { launchSingleTop = true }
                    },
                    onReintentar = viewModel::retry,
                    onAtras = { navController.popBackStack() },
                    estadoFavorito = estadoFavorito,
                    onAgregarFavorito = favoritoViewModel::addFavorite,
                    onReintentarFavorito = favoritoViewModel::retryCheck,
                    onConfirmacionFavoritoMostrada = favoritoViewModel::confirmacionMostrada,
                    onVolverAlListado = {
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(ClaveArtistaEliminado, true)
                        navController.popBackStack()
                    },
                    onAvisoMostrado = viewModel::avisoMostrado,
                    estadoAsociar = estadoAsociar,
                    accionesAsociar = AssociateAlbumActions(
                        onBuscar = associateViewModel::search,
                        onSeleccionar = associateViewModel::select,
                        onAsociar = associateViewModel::associate,
                        onCancelar = associateViewModel::cancelar,
                        onReintentarCarga = associateViewModel::retryLoad
                    ),
                    onAgregarAlbum = associateViewModel::abrir,
                    onAlbumAsociadoMostrado = viewModel::albumAsociadoMostrado
                )
            } else {
                ArtistDetailScreen(
                    estado = estado,
                    onSeleccionarAlbum = { idAlbum ->
                        navController.navigate(Routes.detalleAlbum(idAlbum)) { launchSingleTop = true }
                    },
                    onReintentar = viewModel::retry,
                    onAtras = { navController.popBackStack() },
                    onAvisoMostrado = viewModel::avisoMostrado
                )
            }
        }
        composable(
            Routes.DETALLE_ALBUM,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entrada ->
            val id = requireNotNull(entrada.arguments).getInt("id")
            val viewModel: AlbumViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AlbumViewModel(id, albumRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            val cancionAgregadaViewModel by viewModel.avisoTrackAgregada.collectAsStateWithLifecycle()
            val cancionAgregada by entrada.savedStateHandle
                .getStateFlow(ClaveTrackAgregada, false)
                .collectAsStateWithLifecycle()
            LaunchedEffect(cancionAgregada) {
                if (cancionAgregada) {
                    // Cleared immediately (not when the notice finishes showing) so rotating
                    // the screen while "Track added" is visible doesn't repeat the GET /albums/{id}.
                    // The notice itself is controlled by the ViewModel (see AlbumViewModel.cancionAgregada),
                    // which does survive rotation.
                    viewModel.cancionAgregada()
                    entrada.savedStateHandle[ClaveTrackAgregada] = false
                }
            }
            val comentarioPublicado by entrada.savedStateHandle
                .getStateFlow(ClaveCommentPublicado, false)
                .collectAsStateWithLifecycle()
            LaunchedEffect(comentarioPublicado) {
                if (comentarioPublicado) {
                    viewModel.actualizar()
                    // Cleared so rotating the screen doesn't repeat the query.
                    entrada.savedStateHandle[ClaveCommentPublicado] = false
                }
            }
            val albumEliminado by entrada.savedStateHandle
                .getStateFlow(ClaveAlbumEliminado, false)
                .collectAsStateWithLifecycle()
            LaunchedEffect(albumEliminado) {
                if (albumEliminado) {
                    // The album no longer exists on the backend (the form found out while saving): it's
                    // re-fetched so the detail screen shows "not available" instead of continuing to
                    // show the cached album with "Add track" enabled.
                    viewModel.actualizar()
                    entrada.savedStateHandle[ClaveAlbumEliminado] = false
                }
            }
            AlbumDetailScreen(
                estado = estado,
                onAtras = { navController.popBackStack() },
                onReintentar = viewModel::retry,
                onAvisoMostrado = viewModel::avisoMostrado,
                session = session,
                onAddTrack = { navController.navigate(Routes.addTrack(id)) },
                mostrarAvisoTrackAgregada = cancionAgregadaViewModel,
                onAvisoTrackAgregadaMostrado = viewModel::avisoTrackAgregadaMostrado,
                // The Visitor doesn't see the comment button (add-comentar-album, D8).
                onComentar = if (session.collectorActionsEnabled) {
                    { navController.navigate(Routes.addComment(id)) { launchSingleTop = true } }
                } else {
                    null
                }
            )
        }
        composable(
            Routes.AGREGAR_CANCION,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entrada ->
            val id = requireNotNull(entrada.arguments).getInt("id")
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            val viewModel: AddTrackViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AddTrackViewModel(id, albumRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(session) {
                if (!session.collectorActionsEnabled) cerrarFormularioTrack()
            }
            LaunchedEffect(estado.envio) {
                if (estado.envio == TrackSubmission.Agregada) {
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(ClaveTrackAgregada, true)
                    cerrarFormularioTrack()
                }
            }
            AddTrackScreen(
                estado = estado,
                session = session,
                acciones = AddTrackActions(
                    onNombre = viewModel::cambiarNombre,
                    onDuracion = viewModel::cambiarDuracion,
                    onGuardar = viewModel::save,
                    onCancelar = ::cerrarFormularioTrack,
                    onAtras = ::cerrarFormularioTrack,
                    onAlbumNotAvailable = {
                        // Unlike "cancel", the album no longer exists: the detail screen is notified
                        // so it refreshes and stops showing it as available (instead of falling back
                        // to its stale cache with "Add track" still enabled).
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(ClaveAlbumEliminado, true)
                        cerrarFormularioTrack()
                    }
                )
            )
        }
        composable(
            Routes.COMENTAR_ALBUM,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) {
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            val coleccionista = (session as? Session.Collector)?.collector
            // The stack is restored when the process dies, but the session is not.
            if (coleccionista == null) {
                LaunchedEffect(Unit) { cerrarComentar() }
                return@composable
            }
            val albumId = requireNotNull(it.arguments).getInt("id")
            val viewModel: AddCommentViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        AddCommentViewModel(albumId, coleccionista.id, albumRepository, comentarioRepository)
                    }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(estado.salir) {
                if (estado.salir) {
                    if (estado.publicado) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(ClaveCommentPublicado, true)
                    }
                    cerrarComentar()
                }
            }
            AddCommentScreen(
                estado = estado,
                session = session,
                acciones = AddCommentActions(
                    onCalificar = viewModel::calificar,
                    onOpinion = viewModel::cambiarOpinion,
                    onPublicar = viewModel::post,
                    onSalir = viewModel::pedirSalir,
                    onContinuarEscribiendo = viewModel::continuarEscribiendo,
                    onDescartar = viewModel::descartar,
                    onReintentarAlbum = viewModel::retry
                )
            )
        }
    }
}
