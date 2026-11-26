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
import com.example.vinilos.ui.catalogo.CatalogScreen
import com.example.vinilos.ui.comentar.AddCommentActions
import com.example.vinilos.ui.comentar.AddCommentScreen
import com.example.vinilos.ui.create.AddTrackActions
import com.example.vinilos.ui.create.CreateAlbumActions
import com.example.vinilos.ui.create.AddTrackScreen
import com.example.vinilos.ui.create.CreateAlbumScreen
import com.example.vinilos.ui.detalle.AlbumDetailScreen
import com.example.vinilos.ui.ingreso.CollectorsScreen
import com.example.vinilos.ui.ingreso.EntryScreen
import com.example.vinilos.ui.inicio.HomeScreen
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

    /** Detalle del artista (HU04). El tipo distingue a un músico de una banda con el mismo id. */
    const val DETALLE_ARTISTA = "artista/{tipo}/{id}"

    /** Detalle del álbum (HU02). */
    const val DETALLE_ALBUM = "album/{id}"

    /** Formulario para crear un álbum (HU07), solo para el Coleccionista. */
    const val CREAR_ALBUM = "crear-album"

    /** Formulario para agregar una canción al álbum (HU08), solo para el Coleccionista. */
    const val AGREGAR_CANCION = "album/{id}/agregar-cancion"

    /** Comentar un álbum (HU09), solo para el Coleccionista. */
    const val COMENTAR_ALBUM = "album/{id}/comentar"

    fun detalleArtista(tipo: ArtistType, id: Int) = "artista/${tipo.name}/$id"

    fun detalleAlbum(id: Int) = "album/$id"

    fun addTrack(id: Int) = "album/$id/agregar-cancion"

    fun addComment(id: Int) = "album/$id/comentar"
}

/** Clave con la que el formulario le entrega al catálogo el id del álbum creado. */
private const val ClaveAlbumCreado = "album_creado"

/** Resultado con el que el formulario solicita recargar y avisar en el detalle del álbum. */
private const val ClaveTrackAgregada = "cancion_agregada"

/** Clave con la que la pantalla de comentar le avisa al detalle que debe actualizarse (add-comentar-album, D7). */
private const val ClaveCommentPublicado = "comentario_publicado"

/** Clave con la que el detalle del artista avisa al listado que un favorito ya no está disponible. */
private const val ClaveArtistaEliminado = "artista_eliminado"

/** Clave con la que el formulario de agregar canción avisa al detalle que el álbum ya no existe. */
private const val ClaveAlbumEliminado = "album_eliminado"

/**
 * Grafo de navegación: ingreso -> (coleccionistas) -> inicio -> catálogo -> detalle del álbum |
 * crear álbum, artistas -> detalle del artista -> detalle del álbum, y detalle del álbum -> comentar. Al entrar al
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
    comentarioRepository: CommentRepository,
    modifier: Modifier = Modifier
) {
    fun irAlInicio() {
        navController.navigate(Routes.INICIO) {
            popUpTo(Routes.INGRESO) { inclusive = false }
        }
    }

    /** Dos toques rápidos no deben sacar también el catálogo. */
    fun cerrarFormulario() {
        if (navController.currentDestination?.route == Routes.CREAR_ALBUM) navController.popBackStack()
    }

    fun cerrarFormularioTrack() {
        if (navController.currentDestination?.route == Routes.AGREGAR_CANCION) navController.popBackStack()
    }

    /** Igual que [cerrarFormulario]: solo se cierra si comentar sigue siendo la pantalla actual. */
    fun cerrarComentar() {
        if (navController.currentDestination?.route == Routes.COMENTAR_ALBUM) navController.popBackStack()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.INGRESO,
        modifier = modifier
    ) {
        composable(Routes.INGRESO) {
            // Volver al ingreso (desde el selector o el inicio) deja la sesión sin perfil.
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
                    // Se borra para que girar la pantalla no repita el aviso ni la consulta.
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
                    // El artista ya no existe en el backend: se vuelve a consultar para que el
                    // listado no lo siga mostrando (popBackStack por sí solo no alcanza).
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
            // Ligado a la entrada de navegación: sobrevive a la rotación y a la ida y vuelta al álbum.
            val viewModel: ArtistDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ArtistDetailViewModel(tipo, artistId, artistRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            val coleccionista = (session as? Session.Collector)?.collector
            if (coleccionista != null) {
                // Solo el Coleccionista comprueba y guarda favoritos; el Visitante no ve el botón.
                val favoritoViewModel: FavoriteViewModel = viewModel(
                    key = "favorito-$artistId",
                    factory = viewModelFactory {
                        initializer {
                            FavoriteViewModel(coleccionista.id, artistId, tipo, favoriteRepository)
                        }
                    }
                )
                val estadoFavorito by favoritoViewModel.uiState.collectAsStateWithLifecycle()
                // HU15: la ventana vive en la misma entrada que el detalle, así que el álbum asociado se
                // entrega directo al ViewModel del detalle, sin savedStateHandle (add-agregar-album-artista, D5).
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
                    // Se borra de inmediato (no cuando el aviso termina de mostrarse) para que girar
                    // la pantalla mientras se ve "Canción agregada" no repita el GET /albums/{id}.
                    // El aviso en sí lo controla el ViewModel (ver AlbumViewModel.cancionAgregada),
                    // que sí sobrevive a la rotación.
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
                    // Se borra para que girar la pantalla no repita la consulta.
                    entrada.savedStateHandle[ClaveCommentPublicado] = false
                }
            }
            val albumEliminado by entrada.savedStateHandle
                .getStateFlow(ClaveAlbumEliminado, false)
                .collectAsStateWithLifecycle()
            LaunchedEffect(albumEliminado) {
                if (albumEliminado) {
                    // El álbum ya no existe en el backend (lo supo el formulario al guardar): se
                    // vuelve a consultar para que el detalle pase a "no disponible" en vez de seguir
                    // mostrando el álbum en caché con "Agregar canción" habilitado.
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
                // El Visitante no ve el botón de comentar (add-comentar-album, D8).
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
                        // A diferencia de "cancelar", el álbum ya no existe: se le avisa al detalle
                        // para que se actualice y deje de mostrarlo como disponible (en vez de volver
                        // a su caché desactualizada con "Agregar canción" aún habilitado).
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
            // La pila se restaura al morir el proceso, pero la sesión no.
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
