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
import com.example.vinilos.data.repository.ComentarioRepository
import com.example.vinilos.data.repository.FavoriteRepository
import com.example.vinilos.ui.artistas.AccionesAsociarAlbum
import com.example.vinilos.ui.artistas.PantallaArtistas
import com.example.vinilos.ui.artistas.PantallaDetalleArtista
import com.example.vinilos.ui.catalogo.PantallaCatalogo
import com.example.vinilos.ui.comentar.AccionesComentarAlbum
import com.example.vinilos.ui.comentar.PantallaComentarAlbum
import com.example.vinilos.ui.crear.AccionesAgregarCancion
import com.example.vinilos.ui.crear.AccionesCrearAlbum
import com.example.vinilos.ui.crear.PantallaAgregarCancion
import com.example.vinilos.ui.crear.PantallaCrearAlbum
import com.example.vinilos.ui.detalle.PantallaDetalleAlbum
import com.example.vinilos.ui.ingreso.PantallaColeccionistas
import com.example.vinilos.ui.ingreso.PantallaIngreso
import com.example.vinilos.ui.inicio.PantallaInicio
import com.example.vinilos.viewmodel.AlbumViewModel
import com.example.vinilos.viewmodel.AgregarCancionViewModel
import com.example.vinilos.viewmodel.AlbumsViewModel
import com.example.vinilos.viewmodel.ArtistaDetalleViewModel
import com.example.vinilos.viewmodel.ArtistsViewModel
import com.example.vinilos.viewmodel.AsociarAlbumViewModel
import com.example.vinilos.viewmodel.CollectorsViewModel
import com.example.vinilos.viewmodel.ComentarAlbumViewModel
import com.example.vinilos.viewmodel.CrearAlbumViewModel
import com.example.vinilos.viewmodel.EnvioAlbum
import com.example.vinilos.viewmodel.EnvioCancion
import com.example.vinilos.viewmodel.FavoritoViewModel
import com.example.vinilos.viewmodel.SessionViewModel

object Rutas {
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

    fun agregarCancion(id: Int) = "album/$id/agregar-cancion"

    fun comentarAlbum(id: Int) = "album/$id/comentar"
}

/** Clave con la que el formulario le entrega al catálogo el id del álbum creado. */
private const val ClaveAlbumCreado = "album_creado"

/** Resultado con el que el formulario solicita recargar y avisar en el detalle del álbum. */
private const val ClaveCancionAgregada = "cancion_agregada"

/** Clave con la que la pantalla de comentar le avisa al detalle que debe actualizarse (add-comentar-album, D7). */
private const val ClaveComentarioPublicado = "comentario_publicado"

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
    comentarioRepository: ComentarioRepository,
    modifier: Modifier = Modifier
) {
    fun irAlInicio() {
        navController.navigate(Rutas.INICIO) {
            popUpTo(Rutas.INGRESO) { inclusive = false }
        }
    }

    /** Dos toques rápidos no deben sacar también el catálogo. */
    fun cerrarFormulario() {
        if (navController.currentDestination?.route == Rutas.CREAR_ALBUM) navController.popBackStack()
    }

    fun cerrarFormularioCancion() {
        if (navController.currentDestination?.route == Rutas.AGREGAR_CANCION) navController.popBackStack()
    }

    /** Igual que [cerrarFormulario]: solo se cierra si comentar sigue siendo la pantalla actual. */
    fun cerrarComentar() {
        if (navController.currentDestination?.route == Rutas.COMENTAR_ALBUM) navController.popBackStack()
    }

    NavHost(
        navController = navController,
        startDestination = Rutas.INGRESO,
        modifier = modifier
    ) {
        composable(Rutas.INGRESO) {
            // Volver al ingreso (desde el selector o el inicio) deja la sesión sin perfil.
            LaunchedEffect(Unit) { sessionViewModel.clear() }
            PantallaIngreso(
                onVisitante = {
                    sessionViewModel.enterAsVisitor()
                    irAlInicio()
                },
                onColeccionista = { navController.navigate(Rutas.COLECCIONISTAS) }
            )
        }
        composable(Rutas.COLECCIONISTAS) {
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
                onAtras = { navController.popBackStack() },
                onAvisoMostrado = viewModel::avisoMostrado
            )
        }
        composable(Rutas.INICIO) {
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            PantallaInicio(
                session = session,
                onVerAlbumes = { navController.navigate(Rutas.CATALOGO) },
                onVerArtistas = { navController.navigate(Rutas.ARTISTAS) }
            )
        }
        composable(Rutas.CATALOGO) { entrada ->
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
                onCrearAlbum = { navController.navigate(Rutas.CREAR_ALBUM) { launchSingleTop = true } },
                onAvisoCreadoMostrado = viewModel::avisoCreadoMostrado,
                onDesplazadoAlNuevo = viewModel::desplazadoAlNuevo
            )
        }
        composable(Rutas.CREAR_ALBUM) {
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
        composable(Rutas.ARTISTAS) { entrada ->
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
                onAtras = { navController.popBackStack() },
                onAvisoMostrado = viewModel::avisoMostrado
            )
        }
        composable(
            Rutas.DETALLE_ARTISTA,
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
            val coleccionista = (session as? Session.Collector)?.collector
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
                // HU15: la ventana vive en la misma entrada que el detalle, así que el álbum asociado se
                // entrega directo al ViewModel del detalle, sin savedStateHandle (add-agregar-album-artista, D5).
                val asociarViewModel: AsociarAlbumViewModel = viewModel(
                    key = "asociar-$tipo-$artistId",
                    factory = viewModelFactory {
                        initializer { AsociarAlbumViewModel(tipo, artistId, albumRepository, artistRepository) }
                    }
                )
                val estadoAsociar by asociarViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(estadoAsociar.albumAsociado) {
                    estadoAsociar.albumAsociado?.let {
                        viewModel.albumAsociado(it)
                        asociarViewModel.asociadoNotificado()
                    }
                }
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
                    },
                    onAvisoMostrado = viewModel::avisoMostrado,
                    estadoAsociar = estadoAsociar,
                    accionesAsociar = AccionesAsociarAlbum(
                        onBuscar = asociarViewModel::buscar,
                        onSeleccionar = asociarViewModel::seleccionar,
                        onAsociar = asociarViewModel::asociar,
                        onCancelar = asociarViewModel::cancelar,
                        onReintentarCarga = asociarViewModel::reintentarCarga
                    ),
                    onAgregarAlbum = asociarViewModel::abrir,
                    onAlbumAsociadoMostrado = viewModel::albumAsociadoMostrado
                )
            } else {
                PantallaDetalleArtista(
                    estado = estado,
                    onSeleccionarAlbum = { idAlbum ->
                        navController.navigate(Rutas.detalleAlbum(idAlbum)) { launchSingleTop = true }
                    },
                    onReintentar = viewModel::reintentar,
                    onAtras = { navController.popBackStack() },
                    onAvisoMostrado = viewModel::avisoMostrado
                )
            }
        }
        composable(
            Rutas.DETALLE_ALBUM,
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
            val cancionAgregadaViewModel by viewModel.avisoCancionAgregada.collectAsStateWithLifecycle()
            val cancionAgregada by entrada.savedStateHandle
                .getStateFlow(ClaveCancionAgregada, false)
                .collectAsStateWithLifecycle()
            LaunchedEffect(cancionAgregada) {
                if (cancionAgregada) {
                    // Se borra de inmediato (no cuando el aviso termina de mostrarse) para que girar
                    // la pantalla mientras se ve "Canción agregada" no repita el GET /albums/{id}.
                    // El aviso en sí lo controla el ViewModel (ver AlbumViewModel.cancionAgregada),
                    // que sí sobrevive a la rotación.
                    viewModel.cancionAgregada()
                    entrada.savedStateHandle[ClaveCancionAgregada] = false
                }
            }
            val comentarioPublicado by entrada.savedStateHandle
                .getStateFlow(ClaveComentarioPublicado, false)
                .collectAsStateWithLifecycle()
            LaunchedEffect(comentarioPublicado) {
                if (comentarioPublicado) {
                    viewModel.actualizar()
                    // Se borra para que girar la pantalla no repita la consulta.
                    entrada.savedStateHandle[ClaveComentarioPublicado] = false
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
            PantallaDetalleAlbum(
                estado = estado,
                onAtras = { navController.popBackStack() },
                onReintentar = viewModel::reintentar,
                onAvisoMostrado = viewModel::avisoMostrado,
                session = session,
                onAgregarCancion = { navController.navigate(Rutas.agregarCancion(id)) },
                mostrarAvisoCancionAgregada = cancionAgregadaViewModel,
                onAvisoCancionAgregadaMostrado = viewModel::avisoCancionAgregadaMostrado,
                // El Visitante no ve el botón de comentar (add-comentar-album, D8).
                onComentar = if (session.collectorActionsEnabled) {
                    { navController.navigate(Rutas.comentarAlbum(id)) { launchSingleTop = true } }
                } else {
                    null
                }
            )
        }
        composable(
            Rutas.AGREGAR_CANCION,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entrada ->
            val id = requireNotNull(entrada.arguments).getInt("id")
            val session by sessionViewModel.session.collectAsStateWithLifecycle()
            val viewModel: AgregarCancionViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AgregarCancionViewModel(id, albumRepository) }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(session) {
                if (!session.collectorActionsEnabled) cerrarFormularioCancion()
            }
            LaunchedEffect(estado.envio) {
                if (estado.envio == EnvioCancion.Agregada) {
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(ClaveCancionAgregada, true)
                    cerrarFormularioCancion()
                }
            }
            PantallaAgregarCancion(
                estado = estado,
                session = session,
                acciones = AccionesAgregarCancion(
                    onNombre = viewModel::cambiarNombre,
                    onDuracion = viewModel::cambiarDuracion,
                    onGuardar = viewModel::guardar,
                    onCancelar = ::cerrarFormularioCancion,
                    onAtras = ::cerrarFormularioCancion,
                    onAlbumNoDisponible = {
                        // A diferencia de "cancelar", el álbum ya no existe: se le avisa al detalle
                        // para que se actualice y deje de mostrarlo como disponible (en vez de volver
                        // a su caché desactualizada con "Agregar canción" aún habilitado).
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(ClaveAlbumEliminado, true)
                        cerrarFormularioCancion()
                    }
                )
            )
        }
        composable(
            Rutas.COMENTAR_ALBUM,
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
            val viewModel: ComentarAlbumViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ComentarAlbumViewModel(albumId, coleccionista.id, albumRepository, comentarioRepository)
                    }
                }
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(estado.salir) {
                if (estado.salir) {
                    if (estado.publicado) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(ClaveComentarioPublicado, true)
                    }
                    cerrarComentar()
                }
            }
            PantallaComentarAlbum(
                estado = estado,
                session = session,
                acciones = AccionesComentarAlbum(
                    onCalificar = viewModel::calificar,
                    onOpinion = viewModel::cambiarOpinion,
                    onPublicar = viewModel::publicar,
                    onSalir = viewModel::pedirSalir,
                    onContinuarEscribiendo = viewModel::continuarEscribiendo,
                    onDescartar = viewModel::descartar,
                    onReintentarAlbum = viewModel::reintentar
                )
            )
        }
    }
}
