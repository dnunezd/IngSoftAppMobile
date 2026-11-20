package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.RecursoNoEncontrado
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Catálogo controlable: cada consulta espera a que la prueba la complete. */
private class CatalogoAsociableFalso : AlbumRepository {
    var guardados: List<AlbumResumen> = emptyList()
    val consultas = mutableListOf<CompletableDeferred<Result<List<AlbumResumen>>>>()

    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> {
        val respuesta = CompletableDeferred<Result<List<AlbumResumen>>>()
        consultas += respuesta
        return respuesta.await()
    }

    override suspend fun obtenerAlbumesGuardados(): List<AlbumResumen> = guardados
    override suspend fun obtenerAlbum(id: Int): Result<Album> = error("No se usa en esta prueba")
    override suspend fun obtenerAlbumGuardado(id: Int): Album = error("No se usa en esta prueba")
    override suspend fun crearAlbum(album: AlbumNuevo): Result<Int> = error("No se usa en esta prueba")
}

/** Asociaciones controlables: cada una espera a que la prueba la complete. */
private class AsociacionesArtistaFalsas : ArtistRepository {
    val pedidas = mutableListOf<Pair<Triple<ArtistType, Int, Int>, CompletableDeferred<Result<Unit>>>>()

    override suspend fun asociarAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit> {
        val respuesta = CompletableDeferred<Result<Unit>>()
        pedidas += Triple(tipo, idArtista, idAlbum) to respuesta
        return respuesta.await()
    }

    override suspend fun obtenerArtistas(): Result<List<Artist>> = error("No se usa en esta prueba")
    override suspend fun obtenerDetalle(tipo: ArtistType, id: Int): Result<ArtistaDetalle> = error("No se usa en esta prueba")
    override suspend fun obtenerArtistasGuardados(): List<Artist> = error("No se usa en esta prueba")
    override suspend fun obtenerDetalleGuardado(tipo: ArtistType, id: Int): ArtistaDetalle = error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AsociarAlbumViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val buscando = AlbumResumen(100, "Buscando América", "p100", "Salsa", "Rubén Blades", "1984")
    private val noche = AlbumResumen(102, "A Night at the Opera", "p102", "Rock", "Queen", "1975")
    private val dia = AlbumResumen(103, "A Day at the Races", "p103", "Rock", "Queen", "1976")
    private val catalogo = listOf(dia, noche, buscando)

    private val albumes = CatalogoAsociableFalso()
    private val artistas = AsociacionesArtistaFalsas()

    private fun viewModel() = AsociarAlbumViewModel(ArtistType.MUSICO, 100, albumes, artistas)

    /** Abre la ventana del músico 100, que ya tiene "Buscando América", con el catálogo cargado. */
    private fun TestScope.abiertaConCatalogo(): AsociarAlbumViewModel {
        val vm = viewModel()
        vm.abrir(setOf(100))
        advanceUntilIdle()
        albumes.consultas.last().complete(Result.success(catalogo))
        advanceUntilIdle()
        return vm
    }

    // ---------- Abrir y cargar ----------

    @Test
    fun empiezaCerradaYSinConsultar() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.abierta)
        assertTrue(albumes.consultas.isEmpty())
    }

    @Test
    fun alAbrirMuestraCargandoYConsultaElCatalogo() = runTest {
        val vm = viewModel()

        vm.abrir(setOf(100))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.abierta)
        assertEquals(ListadoAsociable.Cargando, vm.uiState.value.listado)
        assertEquals(1, albumes.consultas.size)
    }

    @Test
    fun conElCatalogoMuestraTodosLosAlbumesYMarcaLosYaAsociados() = runTest {
        val vm = abiertaConCatalogo()

        assertEquals(ListadoAsociable.Listo(catalogo), vm.uiState.value.listado)
        assertEquals(catalogo, vm.uiState.value.visibles)
        assertTrue(vm.uiState.value.yaAsociado(100))
        assertFalse(vm.uiState.value.yaAsociado(102))
    }

    @Test
    fun conCatalogoGuardadoLoMuestraMientrasConsulta() = runTest {
        albumes.guardados = listOf(noche)
        val vm = viewModel()

        vm.abrir(emptySet())
        advanceUntilIdle()

        assertEquals(ListadoAsociable.Listo(listOf(noche)), vm.uiState.value.listado)
        albumes.consultas.single().complete(Result.success(catalogo))
        advanceUntilIdle()
        assertEquals(ListadoAsociable.Listo(catalogo), vm.uiState.value.listado)
    }

    @Test
    fun siFallaLaConsultaConCatalogoGuardadoSeQuedaConLoGuardadoSinError() = runTest {
        albumes.guardados = listOf(noche)
        val vm = viewModel()
        vm.abrir(emptySet())
        advanceUntilIdle()

        albumes.consultas.single().complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(ListadoAsociable.Listo(listOf(noche)), vm.uiState.value.listado)
    }

    @Test
    fun siFallaLaConsultaSinCatalogoGuardadoPasaAError() = runTest {
        val vm = viewModel()
        vm.abrir(emptySet())
        advanceUntilIdle()

        albumes.consultas.single().complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(ListadoAsociable.Error, vm.uiState.value.listado)
    }

    @Test
    fun reintentarLaCargaVuelveAConsultar() = runTest {
        val vm = viewModel()
        vm.abrir(emptySet())
        advanceUntilIdle()
        albumes.consultas.single().complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        vm.reintentarCarga()
        advanceUntilIdle()
        assertEquals(ListadoAsociable.Cargando, vm.uiState.value.listado)
        albumes.consultas.last().complete(Result.success(catalogo))
        advanceUntilIdle()

        assertEquals(2, albumes.consultas.size)
        assertEquals(ListadoAsociable.Listo(catalogo), vm.uiState.value.listado)
    }

    @Test
    fun reintentarSinErrorNoVuelveAConsultar() = runTest {
        val vm = abiertaConCatalogo()

        vm.reintentarCarga()
        advanceUntilIdle()

        assertEquals(1, albumes.consultas.size)
    }

    // ---------- Búsqueda y mensajes de vacío ----------

    @Test
    fun buscarFiltraPorNombreSinTildesNiMayusculas() = runTest {
        val vm = abiertaConCatalogo()

        vm.buscar("OPÉRA")

        assertEquals("OPÉRA", vm.uiState.value.busqueda)
        assertEquals(listOf(noche), vm.uiState.value.visibles)
    }

    @Test
    fun buscarSinCoincidenciasActivaSinResultadosPeroNoSinDisponibles() = runTest {
        val vm = abiertaConCatalogo()

        vm.buscar("zzz")

        assertTrue(vm.uiState.value.sinResultados)
        assertFalse(vm.uiState.value.sinDisponibles)
    }

    @Test
    fun conTodosLosAlbumesYaAsociadosNoHayDisponibles() = runTest {
        val vm = viewModel()
        vm.abrir(setOf(100, 102, 103))
        advanceUntilIdle()
        albumes.consultas.single().complete(Result.success(catalogo))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.sinDisponibles)
        assertFalse(vm.uiState.value.sinResultados)
        // Los ya asociados se siguen viendo, deshabilitados.
        assertEquals(catalogo, vm.uiState.value.visibles)
    }

    @Test
    fun conElCatalogoVacioNoHayDisponibles() = runTest {
        val vm = viewModel()
        vm.abrir(emptySet())
        advanceUntilIdle()
        albumes.consultas.single().complete(Result.success(emptyList()))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.sinDisponibles)
        assertFalse(vm.uiState.value.sinResultados)
    }

    @Test
    fun mientrasCargaNoMuestraNingunMensajeDeVacio() = runTest {
        val vm = viewModel()
        vm.abrir(emptySet())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.sinDisponibles)
        assertFalse(vm.uiState.value.sinResultados)
    }

    // ---------- Selección ----------

    @Test
    fun sinSeleccionNoSePuedeAsociar() = runTest {
        val vm = abiertaConCatalogo()

        assertNull(vm.uiState.value.seleccionado)
        assertFalse(vm.uiState.value.puedeAsociar)
    }

    @Test
    fun seleccionarUnAlbumDisponibleHabilitaAsociar() = runTest {
        val vm = abiertaConCatalogo()

        vm.seleccionar(102)

        assertEquals(102, vm.uiState.value.seleccionado)
        assertTrue(vm.uiState.value.puedeAsociar)
    }

    @Test
    fun soloQuedaSeleccionadoUnAlbum() = runTest {
        val vm = abiertaConCatalogo()

        vm.seleccionar(102)
        vm.seleccionar(103)

        assertEquals(103, vm.uiState.value.seleccionado)
    }

    @Test
    fun unAlbumYaAsociadoNoSePuedeSeleccionar() = runTest {
        val vm = abiertaConCatalogo()

        vm.seleccionar(100)

        assertNull(vm.uiState.value.seleccionado)
        assertFalse(vm.uiState.value.puedeAsociar)
    }

    @Test
    fun laSeleccionSeConservaAunqueElFiltroLaDejeFuera() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)

        vm.buscar("races")

        assertEquals(listOf(dia), vm.uiState.value.visibles)
        assertEquals(102, vm.uiState.value.seleccionado)
        assertTrue(vm.uiState.value.puedeAsociar)
    }

    // ---------- Asociar ----------

    @Test
    fun asociarEnviaElAlbumElegidoAlArtistaYDeshabilitaElBoton() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)

        vm.asociar()
        advanceUntilIdle()

        assertEquals(Triple(ArtistType.MUSICO, 100, 102), artistas.pedidas.single().first)
        assertTrue(vm.uiState.value.asociando)
        assertFalse(vm.uiState.value.puedeAsociar)
        assertTrue(vm.uiState.value.abierta)
    }

    @Test
    fun alAsociarCierraLaVentanaYEntregaElAlbumAsociado() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)
        vm.asociar()
        advanceUntilIdle()

        artistas.pedidas.single().second.complete(Result.success(Unit))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.abierta)
        assertEquals(noche, vm.uiState.value.albumAsociado)
    }

    @Test
    fun asociadoNotificadoLimpiaElAlbumEntregado() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)
        vm.asociar()
        advanceUntilIdle()
        artistas.pedidas.single().second.complete(Result.success(Unit))
        advanceUntilIdle()

        vm.asociadoNotificado()

        assertNull(vm.uiState.value.albumAsociado)
    }

    @Test
    fun siFallaAlAsociarMuestraElErrorYConservaVentanaYSeleccion() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)
        vm.asociar()
        advanceUntilIdle()

        artistas.pedidas.single().second.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertTrue(estado.abierta)
        assertTrue(estado.errorAlAsociar)
        assertFalse(estado.asociando)
        assertEquals(102, estado.seleccionado)
        assertTrue(estado.puedeAsociar)
        assertNull(estado.albumAsociado)
    }

    @Test
    fun unArtistaOAlbumQueYaNoExisteTambienEsErrorAlAsociar() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)
        vm.asociar()
        advanceUntilIdle()

        artistas.pedidas.single().second.complete(Result.failure(RecursoNoEncontrado()))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errorAlAsociar)
    }

    @Test
    fun reintentarDespuesDelErrorAsociaYCierra() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)
        vm.asociar()
        advanceUntilIdle()
        artistas.pedidas.single().second.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        vm.asociar()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.errorAlAsociar)
        artistas.pedidas.last().second.complete(Result.success(Unit))
        advanceUntilIdle()

        assertEquals(2, artistas.pedidas.size)
        assertFalse(vm.uiState.value.abierta)
        assertEquals(noche, vm.uiState.value.albumAsociado)
    }

    @Test
    fun elegirOtroAlbumBorraElError() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)
        vm.asociar()
        advanceUntilIdle()
        artistas.pedidas.single().second.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        vm.seleccionar(103)

        assertFalse(vm.uiState.value.errorAlAsociar)
    }

    @Test
    fun unSegundoToqueMientrasAsociaSeIgnora() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)

        vm.asociar()
        vm.asociar()
        advanceUntilIdle()

        assertEquals(1, artistas.pedidas.size)
    }

    @Test
    fun mientrasAsociaNoSePuedeCambiarLaSeleccionNiCancelar() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)
        vm.asociar()
        advanceUntilIdle()

        vm.seleccionar(103)
        vm.cancelar()

        assertEquals(102, vm.uiState.value.seleccionado)
        assertTrue(vm.uiState.value.abierta)
    }

    @Test
    fun sinSeleccionAsociarNoHaceNada() = runTest {
        val vm = abiertaConCatalogo()

        vm.asociar()
        advanceUntilIdle()

        assertTrue(artistas.pedidas.isEmpty())
    }

    // ---------- Cancelar ----------

    @Test
    fun cancelarCierraSinAsociar() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)

        vm.cancelar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.abierta)
        assertTrue(artistas.pedidas.isEmpty())
        assertNull(vm.uiState.value.albumAsociado)
    }

    @Test
    fun alVolverAAbrirDespuesDeCancelarEmpiezaLimpia() = runTest {
        val vm = abiertaConCatalogo()
        vm.seleccionar(102)
        vm.buscar("opera")
        vm.cancelar()

        vm.abrir(setOf(100))
        advanceUntilIdle()

        val estado = vm.uiState.value
        assertTrue(estado.abierta)
        assertNull(estado.seleccionado)
        assertEquals("", estado.busqueda)
        assertFalse(estado.errorAlAsociar)
    }

    @Test
    fun unaRespuestaTardiaDelCatalogoDeUnaAperturaAnteriorSeIgnora() = runTest {
        val vm = viewModel()
        vm.abrir(emptySet())
        advanceUntilIdle()
        vm.cancelar()
        vm.abrir(emptySet())
        advanceUntilIdle()

        albumes.consultas.first().complete(Result.success(listOf(noche)))
        advanceUntilIdle()

        assertEquals(ListadoAsociable.Cargando, vm.uiState.value.listado)
    }
}
