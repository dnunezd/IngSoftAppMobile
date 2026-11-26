package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.remote.ResourceNotFound
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
    var guardados: List<AlbumSummary> = emptyList()
    val consultas = mutableListOf<CompletableDeferred<Result<List<AlbumSummary>>>>()

    override suspend fun getAlbums(): Result<List<AlbumSummary>> {
        val respuesta = CompletableDeferred<Result<List<AlbumSummary>>>()
        consultas += respuesta
        return respuesta.await()
    }

    override suspend fun getCachedAlbums(): List<AlbumSummary> = guardados
    override suspend fun getAlbum(id: Int): Result<Album> = error("No se usa en esta prueba")
    override suspend fun getCachedAlbum(id: Int): Album = error("No se usa en esta prueba")
    override suspend fun createAlbum(album: NewAlbum): Result<Int> = error("No se usa en esta prueba")
    override suspend fun addTrack(albumId: Int, cancion: Track): Result<Unit> = error("No se usa en esta prueba")
}

/** Asociaciones controlables: cada una espera a que la prueba la complete. */
private class AsociacionesArtistaFalsas : ArtistRepository {
    val pedidas = mutableListOf<Pair<Triple<ArtistType, Int, Int>, CompletableDeferred<Result<Unit>>>>()

    override suspend fun associateAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit> {
        val respuesta = CompletableDeferred<Result<Unit>>()
        pedidas += Triple(tipo, idArtista, idAlbum) to respuesta
        return respuesta.await()
    }

    override suspend fun getArtists(): Result<List<Artist>> = error("No se usa en esta prueba")
    override suspend fun getDetail(tipo: ArtistType, id: Int): Result<ArtistDetail> = error("No se usa en esta prueba")
    override suspend fun getCachedArtists(): List<Artist> = error("No se usa en esta prueba")
    override suspend fun getCachedDetail(tipo: ArtistType, id: Int): ArtistDetail = error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AssociateAlbumViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val buscando = AlbumSummary(100, "Buscando América", "p100", "Salsa", "Rubén Blades", "1984")
    private val noche = AlbumSummary(102, "A Night at the Opera", "p102", "Rock", "Queen", "1975")
    private val dia = AlbumSummary(103, "A Day at the Races", "p103", "Rock", "Queen", "1976")
    private val catalogo = listOf(dia, noche, buscando)

    private val albumes = CatalogoAsociableFalso()
    private val artistas = AsociacionesArtistaFalsas()

    private fun viewModel() = AssociateAlbumViewModel(ArtistType.MUSICO, 100, albumes, artistas)

    /** Abre la ventana del músico 100, que ya tiene "Buscando América", con el catálogo cargado. */
    private fun TestScope.abiertaConCatalogo(): AssociateAlbumViewModel {
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
    fun alAbrirMuestraLoadingYConsultaElCatalogo() = runTest {
        val vm = viewModel()

        vm.abrir(setOf(100))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.abierta)
        assertEquals(ListadoAsociable.Loading, vm.uiState.value.listado)
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

        albumes.consultas.single().complete(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(ListadoAsociable.Listo(listOf(noche)), vm.uiState.value.listado)
    }

    @Test
    fun siFallaLaConsultaSinCatalogoGuardadoPasaAError() = runTest {
        val vm = viewModel()
        vm.abrir(emptySet())
        advanceUntilIdle()

        albumes.consultas.single().complete(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(ListadoAsociable.Error, vm.uiState.value.listado)
    }

    @Test
    fun retryLaCargaVuelveAConsultar() = runTest {
        val vm = viewModel()
        vm.abrir(emptySet())
        advanceUntilIdle()
        albumes.consultas.single().complete(Result.failure(LoadError()))
        advanceUntilIdle()

        vm.retryLoad()
        advanceUntilIdle()
        assertEquals(ListadoAsociable.Loading, vm.uiState.value.listado)
        albumes.consultas.last().complete(Result.success(catalogo))
        advanceUntilIdle()

        assertEquals(2, albumes.consultas.size)
        assertEquals(ListadoAsociable.Listo(catalogo), vm.uiState.value.listado)
    }

    @Test
    fun retrySinErrorNoVuelveAConsultar() = runTest {
        val vm = abiertaConCatalogo()

        vm.retryLoad()
        advanceUntilIdle()

        assertEquals(1, albumes.consultas.size)
    }

    // ---------- Búsqueda y mensajes de vacío ----------

    @Test
    fun searchFiltraPorNombreSinTildesNiMayusculas() = runTest {
        val vm = abiertaConCatalogo()

        vm.search("OPÉRA")

        assertEquals("OPÉRA", vm.uiState.value.busqueda)
        assertEquals(listOf(noche), vm.uiState.value.visibles)
    }

    @Test
    fun searchSinCoincidenciasActivaSinResultadosPeroNoSinDisponibles() = runTest {
        val vm = abiertaConCatalogo()

        vm.search("zzz")

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
    fun conElCatalogoEmptyNoHayDisponibles() = runTest {
        val vm = viewModel()
        vm.abrir(emptySet())
        advanceUntilIdle()
        albumes.consultas.single().complete(Result.success(emptyList()))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.sinDisponibles)
        assertFalse(vm.uiState.value.sinResultados)
    }

    @Test
    fun mientrasCargaNoMuestraNingunMensajeDeEmpty() = runTest {
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
    fun selectUnAlbumDisponibleHabilitaAsociar() = runTest {
        val vm = abiertaConCatalogo()

        vm.select(102)

        assertEquals(102, vm.uiState.value.seleccionado)
        assertTrue(vm.uiState.value.puedeAsociar)
    }

    @Test
    fun soloQuedaSeleccionadoUnAlbum() = runTest {
        val vm = abiertaConCatalogo()

        vm.select(102)
        vm.select(103)

        assertEquals(103, vm.uiState.value.seleccionado)
    }

    @Test
    fun unAlbumYaAsociadoNoSePuedeSeleccionar() = runTest {
        val vm = abiertaConCatalogo()

        vm.select(100)

        assertNull(vm.uiState.value.seleccionado)
        assertFalse(vm.uiState.value.puedeAsociar)
    }

    @Test
    fun unaBusquedaQueDejaFueraAlElegidoQuitaLaSeleccion() = runTest {
        // Revisión del PR #114: no se debe poder asociar un álbum que ya no se ve.
        val vm = abiertaConCatalogo()
        vm.select(102)

        vm.search("races")

        assertEquals(listOf(dia), vm.uiState.value.visibles)
        assertNull(vm.uiState.value.seleccionado)
        assertFalse(vm.uiState.value.puedeAsociar)
    }

    @Test
    fun unaBusquedaQueSigueMostrandoAlElegidoConservaLaSeleccion() = runTest {
        val vm = abiertaConCatalogo()
        vm.select(102)

        vm.search("opera")

        assertEquals(102, vm.uiState.value.seleccionado)
        assertTrue(vm.uiState.value.puedeAsociar)
    }

    @Test
    fun siElCatalogoActualizadoYaNoTraeAlElegidoQuitaLaSeleccion() = runTest {
        albumes.guardados = listOf(dia, noche)
        val vm = viewModel()
        vm.abrir(emptySet())
        advanceUntilIdle()
        vm.select(102)

        albumes.consultas.single().complete(Result.success(listOf(dia)))
        advanceUntilIdle()

        assertNull(vm.uiState.value.seleccionado)
        assertFalse(vm.uiState.value.puedeAsociar)
    }

    @Test
    fun unElegidoQueNoEstaEntreLosVisiblesNoSePuedeAsociar() {
        val estado = AssociateAlbumUiState(
            abierta = true,
            seleccionado = 102,
            listado = ListadoAsociable.Listo(listOf(dia))
        )

        assertFalse(estado.puedeAsociar)
    }

    // ---------- Asociar ----------

    @Test
    fun associateEnviaElAlbumElegidoAlArtistaYDeshabilitaElBoton() = runTest {
        val vm = abiertaConCatalogo()
        vm.select(102)

        vm.associate()
        advanceUntilIdle()

        assertEquals(Triple(ArtistType.MUSICO, 100, 102), artistas.pedidas.single().first)
        assertTrue(vm.uiState.value.asociando)
        assertFalse(vm.uiState.value.puedeAsociar)
        assertTrue(vm.uiState.value.abierta)
    }

    @Test
    fun alAsociarCierraLaVentanaYEntregaElAlbumAsociado() = runTest {
        val vm = abiertaConCatalogo()
        vm.select(102)
        vm.associate()
        advanceUntilIdle()

        artistas.pedidas.single().second.complete(Result.success(Unit))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.abierta)
        assertEquals(noche, vm.uiState.value.albumAsociado)
    }

    @Test
    fun asociadoNotificadoLimpiaElAlbumEntregado() = runTest {
        val vm = abiertaConCatalogo()
        vm.select(102)
        vm.associate()
        advanceUntilIdle()
        artistas.pedidas.single().second.complete(Result.success(Unit))
        advanceUntilIdle()

        vm.asociadoNotificado()

        assertNull(vm.uiState.value.albumAsociado)
    }

    @Test
    fun siFallaAlAsociarMuestraElErrorYConservaVentanaYSeleccion() = runTest {
        val vm = abiertaConCatalogo()
        vm.select(102)
        vm.associate()
        advanceUntilIdle()

        artistas.pedidas.single().second.complete(Result.failure(LoadError()))
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
        vm.select(102)
        vm.associate()
        advanceUntilIdle()

        artistas.pedidas.single().second.complete(Result.failure(ResourceNotFound()))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errorAlAsociar)
    }

    @Test
    fun retryDespuesDelErrorAsociaYCierra() = runTest {
        val vm = abiertaConCatalogo()
        vm.select(102)
        vm.associate()
        advanceUntilIdle()
        artistas.pedidas.single().second.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        vm.associate()
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
        vm.select(102)
        vm.associate()
        advanceUntilIdle()
        artistas.pedidas.single().second.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        vm.select(103)

        assertFalse(vm.uiState.value.errorAlAsociar)
    }

    @Test
    fun unSegundoToqueMientrasAsociaSeIgnora() = runTest {
        val vm = abiertaConCatalogo()
        vm.select(102)

        vm.associate()
        vm.associate()
        advanceUntilIdle()

        assertEquals(1, artistas.pedidas.size)
    }

    @Test
    fun mientrasAsociaNoSePuedeCambiarLaSeleccionNiCancelar() = runTest {
        val vm = abiertaConCatalogo()
        vm.select(102)
        vm.associate()
        advanceUntilIdle()

        vm.select(103)
        vm.cancelar()

        assertEquals(102, vm.uiState.value.seleccionado)
        assertTrue(vm.uiState.value.abierta)
    }

    @Test
    fun sinSeleccionAsociarNoHaceNada() = runTest {
        val vm = abiertaConCatalogo()

        vm.associate()
        advanceUntilIdle()

        assertTrue(artistas.pedidas.isEmpty())
    }

    // ---------- Cancelar ----------

    @Test
    fun cancelarCierraSinAsociar() = runTest {
        val vm = abiertaConCatalogo()
        vm.select(102)

        vm.cancelar()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.abierta)
        assertTrue(artistas.pedidas.isEmpty())
        assertNull(vm.uiState.value.albumAsociado)
    }

    @Test
    fun alVolverAAbrirDespuesDeCancelarEmpiezaLimpia() = runTest {
        val vm = abiertaConCatalogo()
        vm.select(102)
        vm.search("opera")
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

        assertEquals(ListadoAsociable.Loading, vm.uiState.value.listado)
    }
}
