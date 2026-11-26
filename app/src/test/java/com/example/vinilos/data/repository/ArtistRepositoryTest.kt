package com.example.vinilos.data.repository

import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.ArtistPrize
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class ArtistRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: ArtistRepository

    // Response for each route; the two requests run in parallel, so responses are dispatched by path rather than order.
    private var musicos: MockResponse = MockResponse().setBody("[]")
    private var bandas: MockResponse = MockResponse().setBody("[]")
    private val otrasRoutes = mutableMapOf<String, MockResponse>()

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/musicians" -> musicos
                "/bands" -> bandas
                else -> otrasRoutes[request.path] ?: MockResponse().setResponseCode(404)
            }
        }
        servidor.start()
        val cliente = OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .build()
        repositorio = ArtistRepositoryImpl(ServiceAdapter(servidor.url("/").toString(), cliente))
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun uneMusicosYBandasEnUnSoloListado() = runTest {
        musicos = MockResponse().setBody(
            """
            [{"id": 100, "name": "Rubén Blades Bellido de Luna", "image": "https://img/ruben.jpg",
              "description": "Cantante", "birthDate": "1948-07-16T00:00:00.000Z",
              "albums": [], "performerPrizes": []}]
            """.trimIndent()
        )
        bandas = MockResponse().setBody(
            """
            [{"id": 101, "name": "Queen", "image": "https://img/queen.jpg",
              "description": "Banda británica", "creationDate": "1970-01-01T00:00:00.000Z",
              "albums": [], "musicians": [], "performerPrizes": []}]
            """.trimIndent()
        )

        val resultado = repositorio.getArtists().getOrThrow()

        assertEquals(
            listOf(
                Artist(101, "Queen", "https://img/queen.jpg", ArtistType.BANDA),
                Artist(100, "Rubén Blades Bellido de Luna", "https://img/ruben.jpg", ArtistType.MUSICO)
            ),
            resultado
        )
        val rutas = listOf(servidor.takeRequest().path, servidor.takeRequest().path)
        assertEquals(setOf("/musicians", "/bands"), rutas.toSet())
    }

    @Test
    fun ambasVaciasDevuelveListaVacia() = runTest {
        assertEquals(emptyList<Artist>(), repositorio.getArtists().getOrThrow())
    }

    @Test
    fun soloMusicosDevuelveLosMusicos() = runTest {
        musicos = MockResponse().setBody("""[{"id": 1, "name": "Ástor Piazzolla", "image": "x"}]""")

        assertEquals(
            listOf(Artist(1, "Ástor Piazzolla", "x", ArtistType.MUSICO)),
            repositorio.getArtists().getOrThrow()
        )
    }

    @Test
    fun mismoIdEnMusicoYBandaSonArtistasDistintos() = runTest {
        musicos = MockResponse().setBody("""[{"id": 7, "name": "Freddie Mercury", "image": "m"}]""")
        bandas = MockResponse().setBody("""[{"id": 7, "name": "Queen", "image": "b"}]""")

        val resultado = repositorio.getArtists().getOrThrow()

        assertEquals(
            listOf(
                Artist(7, "Freddie Mercury", "m", ArtistType.MUSICO),
                Artist(7, "Queen", "b", ArtistType.BANDA)
            ),
            resultado
        )
    }

    @Test
    fun ordenaPorNombreSinDistinguirMayusculasNiTildes() = runTest {
        musicos = MockResponse().setBody(
            """[{"id": 1, "name": "rubén Blades", "image": null}, {"id": 2, "name": "Ástor Piazzolla", "image": null}]"""
        )
        bandas = MockResponse().setBody(
            """[{"id": 3, "name": "Queen", "image": null}, {"id": 4, "name": "aerosmith", "image": null}]"""
        )

        val nombres = repositorio.getArtists().getOrThrow().map { it.name }

        assertEquals(listOf("aerosmith", "Ástor Piazzolla", "Queen", "rubén Blades"), nombres)
    }

    @Test
    fun descartaElementosSinIdOSinNombre() = runTest {
        musicos = MockResponse().setBody(
            """[{"name": "Sin id"}, {"id": 2, "image": "x"}, {"id": 3, "name": "Válido"}]"""
        )

        assertEquals(
            listOf(Artist(3, "Válido", null, ArtistType.MUSICO)),
            repositorio.getArtists().getOrThrow()
        )
    }

    @Test
    fun errorDelServidorEnUnaDeLasDosDevuelveError() = runTest {
        musicos = MockResponse().setBody("""[{"id": 1, "name": "Ástor Piazzolla"}]""")
        bandas = MockResponse().setResponseCode(500)

        val resultado = repositorio.getArtists()

        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull() is LoadError)
    }

    @Test
    fun desconexionDevuelveError() = runTest {
        musicos = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)

        val resultado = repositorio.getArtists()

        assertTrue(resultado.exceptionOrNull() is LoadError)
    }

    @Test
    fun tiempoAgotadoDevuelveError() = runTest {
        bandas = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)

        val resultado = repositorio.getArtists()

        assertTrue(resultado.exceptionOrNull() is LoadError)
    }

    @Test
    fun jsonInvalidoDevuelveError() = runTest {
        musicos = MockResponse().setBody("""{"no": "es una lista"}""")

        val resultado = repositorio.getArtists()

        assertTrue(resultado.exceptionOrNull() is LoadError)
    }

    @Test
    fun detalleDeMusicoConsultaSuRutaYLoMapea() = runTest {
        otrasRoutes["/musicians/100"] = MockResponse().setBody(
            """
            {"id": 100, "name": "Rubén Blades", "image": "https://img/ruben.jpg",
             "description": "Cantante", "birthDate": "1948-07-16T00:00:00.000Z",
             "albums": [{"id": 100, "name": "Buscando América", "cover": "https://img/ba.jpg",
                         "releaseDate": "1984-08-01T00:00:00.000Z", "genre": "Salsa"}],
             "performerPrizes": []}
            """.trimIndent()
        )

        val detalle = repositorio.getDetail(ArtistType.MUSICO, 100).getOrThrow()

        assertEquals(
            ArtistDetail(
                id = 100,
                nombre = "Rubén Blades",
                imagen = "https://img/ruben.jpg",
                descripcion = "Cantante",
                tipo = ArtistType.MUSICO,
                fecha = "1948-07-16T00:00:00.000Z",
                integrantes = emptyList(),
                albumes = listOf(AlbumArtista(100, "Buscando América", "https://img/ba.jpg", "1984")),
                premios = emptyList()
            ),
            detalle
        )
        assertEquals("/musicians/100", servidor.takeRequest().path)
    }

    @Test
    fun detalleDeBandaConsultaSuRutaYLoMapea() = runTest {
        otrasRoutes["/bands/101"] = MockResponse().setBody(
            """
            {"id": 101, "name": "Queen", "image": "https://img/queen.jpg",
             "description": "Banda británica", "creationDate": "1970-01-01T00:00:00.000Z",
             "albums": [],
             "musicians": [{"id": 100, "name": "Freddie Mercury", "image": "https://img/freddie.jpg",
                            "description": "Cantante", "birthDate": "1946-09-05T00:00:00.000Z"}],
             "performerPrizes": []}
            """.trimIndent()
        )

        val detalle = repositorio.getDetail(ArtistType.BANDA, 101).getOrThrow()

        assertEquals(
            ArtistDetail(
                id = 101,
                nombre = "Queen",
                imagen = "https://img/queen.jpg",
                descripcion = "Banda británica",
                tipo = ArtistType.BANDA,
                fecha = "1970-01-01T00:00:00.000Z",
                integrantes = listOf(IntegranteArtista(100, "Freddie Mercury", "https://img/freddie.jpg")),
                albumes = emptyList(),
                premios = emptyList()
            ),
            detalle
        )
        assertEquals("/bands/101", servidor.takeRequest().path)
    }

    @Test
    fun mismoIdConsultaElRecursoDelTipoPedido() = runTest {
        otrasRoutes["/musicians/7"] = MockResponse().setBody("""{"id": 7, "name": "Freddie Mercury"}""")
        otrasRoutes["/bands/7"] = MockResponse().setBody("""{"id": 7, "name": "Queen"}""")

        assertEquals("Queen", repositorio.getDetail(ArtistType.BANDA, 7).getOrThrow().nombre)
        assertEquals("/bands/7", servidor.takeRequest().path)
    }

    @Test
    fun cruzaLosPrizesPorElIdDeLaPremiacionYNoPorElDelPrize() = runTest {
        otrasRoutes["/musicians/100"] = MockResponse().setBody(
            """
            {"id": 100, "name": "Rubén Blades",
             "performerPrizes": [{"id": 100, "premiationDate": "1978-12-10T00:00:00.000Z"}]}
            """.trimIndent()
        )
        otrasRoutes["/performerprizes"] = MockResponse().setBody(
            """
            [{"id": 200, "premiationDate": "2000-01-01T00:00:00.000Z",
              "prize": {"id": 100, "name": "Premio ajeno", "organization": "Otra"}},
             {"id": 100, "premiationDate": "1978-12-10T00:00:00.000Z",
              "prize": {"id": 200, "name": "Grammy Award", "organization": "NARAS"}}]
            """.trimIndent()
        )

        val detalle = repositorio.getDetail(ArtistType.MUSICO, 100).getOrThrow()

        assertEquals(
            listOf(ArtistPrize(100, "Grammy Award", "NARAS", "1978-12-10T00:00:00.000Z")),
            detalle.premios
        )
    }

    @Test
    fun sinPrizesNoConsultaLasPremiaciones() = runTest {
        otrasRoutes["/bands/101"] = MockResponse().setBody("""{"id": 101, "name": "Queen", "performerPrizes": []}""")

        repositorio.getDetail(ArtistType.BANDA, 101).getOrThrow()

        assertEquals(1, servidor.requestCount)
    }

    @Test
    fun falloSoloDeLasPremiacionesEsLoadError() = runTest {
        otrasRoutes["/musicians/100"] = MockResponse().setBody(
            """{"id": 100, "name": "Rubén Blades", "performerPrizes": [{"id": 100}]}"""
        )
        otrasRoutes["/performerprizes"] = MockResponse().setResponseCode(500)

        val resultado = repositorio.getDetail(ArtistType.MUSICO, 100)

        assertTrue(resultado.exceptionOrNull() is LoadError)
    }

    @Test
    fun artistaSinNombreEsLoadError() = runTest {
        otrasRoutes["/musicians/100"] = MockResponse().setBody("""{"id": 100, "image": "x"}""")

        assertTrue(repositorio.getDetail(ArtistType.MUSICO, 100).exceptionOrNull() is LoadError)
    }

    @Test
    fun artistaConNombreEnBlancoEsLoadError() = runTest {
        otrasRoutes["/musicians/100"] = MockResponse().setBody("""{"id": 100, "name": "  "}""")

        assertTrue(repositorio.getDetail(ArtistType.MUSICO, 100).exceptionOrNull() is LoadError)
    }

    @Test
    fun artistaSinIdEsLoadError() = runTest {
        otrasRoutes["/bands/101"] = MockResponse().setBody("""{"name": "Queen"}""")

        assertTrue(repositorio.getDetail(ArtistType.BANDA, 101).exceptionOrNull() is LoadError)
    }

    @Test
    fun omiteLosAlbumesEIntegrantesIncompletosSinAfectarALosDemas() = runTest {
        otrasRoutes["/bands/101"] = MockResponse().setBody(
            """
            {"id": 101, "name": "Queen",
             "albums": [{"id": 1, "name": "Válido"}, {"name": "Sin id"}, {"id": 2}, {"id": 3, "name": " "}],
             "musicians": [{"id": 4, "name": "Válida"}, {"name": "Sin id"}, {"id": 5}, {"id": 6, "name": ""}]}
            """.trimIndent()
        )

        val detalle = repositorio.getDetail(ArtistType.BANDA, 101).getOrThrow()

        assertEquals(listOf("Válido"), detalle.albumes.map { it.nombre })
        assertEquals(listOf("Válida"), detalle.integrantes.map { it.nombre })
    }

    @Test
    fun omiteLosPrizesSinDatosDisponiblesSinAfectarALosDemas() = runTest {
        otrasRoutes["/musicians/100"] = MockResponse().setBody(
            """
            {"id": 100, "name": "Rubén Blades",
             "performerPrizes": [{"id": 1}, {"id": 2}, {"id": 3}, {"id": 4}, {"id": 5}]}
            """.trimIndent()
        )
        otrasRoutes["/performerprizes"] = MockResponse().setBody(
            """
            [{"id": 1, "prize": {"name": "Válido", "organization": "Org"}},
             {"id": 2, "prize": {"organization": "Sin nombre"}},
             {"id": 3, "prize": {"name": "Sin organización"}},
             {"id": 4}]
            """.trimIndent()
        )

        val detalle = repositorio.getDetail(ArtistType.MUSICO, 100).getOrThrow()

        assertEquals(listOf("Válido"), detalle.premios.map { it.nombre })
    }

    @Test
    fun fechaYDescripcionAusentesOMalformadasSeOmiten() = runTest {
        otrasRoutes["/musicians/1"] = MockResponse().setBody(
            """{"id": 1, "name": "Sin datos"}"""
        )
        otrasRoutes["/musicians/2"] = MockResponse().setBody(
            """{"id": 2, "name": "Malformados", "birthDate": "no es una fecha", "description": "  "}"""
        )

        val sinDatos = repositorio.getDetail(ArtistType.MUSICO, 1).getOrThrow()
        val malformados = repositorio.getDetail(ArtistType.MUSICO, 2).getOrThrow()

        assertEquals(null, sinDatos.fecha)
        assertEquals(null, sinDatos.descripcion)
        assertEquals(null, malformados.fecha)
        assertEquals(null, malformados.descripcion)
    }

    @Test
    fun laBandaUsaSuFechaDeCreacionYNoLaDeNacimiento() = runTest {
        otrasRoutes["/bands/101"] = MockResponse().setBody(
            """{"id": 101, "name": "Queen", "birthDate": "1948-07-16T00:00:00.000Z"}"""
        )

        assertEquals(null, repositorio.getDetail(ArtistType.BANDA, 101).getOrThrow().fecha)
    }

    @Test
    fun ordenaLosAlbumesDelMasRecienteAlMasAntiguoConLosSinFechaAlFinal() = runTest {
        otrasRoutes["/musicians/100"] = MockResponse().setBody(
            """
            {"id": 100, "name": "Rubén Blades",
             "albums": [{"id": 1, "name": "Sin fecha"},
                        {"id": 2, "name": "Siembra", "releaseDate": "1978-08-01T00:00:00.000Z"},
                        {"id": 3, "name": "Malformada", "releaseDate": "2020-99-99T00:00:00.000Z"},
                        {"id": 4, "name": "Buscando América", "releaseDate": "1984-08-01T00:00:00.000Z"},
                        {"id": 5, "name": "Mismo año, antes", "releaseDate": "1984-02-01T00:00:00.000Z"}]}
            """.trimIndent()
        )

        val albumes = repositorio.getDetail(ArtistType.MUSICO, 100).getOrThrow().albumes

        assertEquals(
            listOf("Buscando América", "Mismo año, antes", "Siembra", "Sin fecha", "Malformada"),
            albumes.map { it.nombre }
        )
        assertEquals(listOf("1984", "1984", "1978", null, null), albumes.map { it.anio })
    }

    @Test
    fun ordenaLosPrizesDelMasRecienteAlMasAntiguoConLosSinFechaAlFinal() = runTest {
        otrasRoutes["/musicians/100"] = MockResponse().setBody(
            """
            {"id": 100, "name": "Rubén Blades",
             "performerPrizes": [{"id": 1},
                                 {"id": 2, "premiationDate": "1978-12-10T00:00:00.000Z"},
                                 {"id": 3, "premiationDate": "no es una fecha"},
                                 {"id": 4, "premiationDate": "1985-02-26T00:00:00.000Z"}]}
            """.trimIndent()
        )
        otrasRoutes["/performerprizes"] = MockResponse().setBody(
            """
            [{"id": 1, "prize": {"name": "Sin fecha", "organization": "Org"}},
             {"id": 2, "prize": {"name": "De 1978", "organization": "Org"}},
             {"id": 3, "prize": {"name": "Malformada", "organization": "Org"}},
             {"id": 4, "prize": {"name": "De 1985", "organization": "Org"}}]
            """.trimIndent()
        )

        val premios = repositorio.getDetail(ArtistType.MUSICO, 100).getOrThrow().premios

        assertEquals(listOf("De 1985", "De 1978", "Sin fecha", "Malformada"), premios.map { it.nombre })
        assertEquals(
            listOf("1985-02-26T00:00:00.000Z", "1978-12-10T00:00:00.000Z", null, null),
            premios.map { it.fecha }
        )
    }

    @Test
    fun losIntegrantesConservanElOrdenDelBackend() = runTest {
        otrasRoutes["/bands/101"] = MockResponse().setBody(
            """
            {"id": 101, "name": "Queen",
             "musicians": [{"id": 2, "name": "Roger Taylor"}, {"id": 1, "name": "Brian May"}]}
            """.trimIndent()
        )

        val integrantes = repositorio.getDetail(ArtistType.BANDA, 101).getOrThrow().integrantes

        assertEquals(listOf("Roger Taylor", "Brian May"), integrantes.map { it.nombre })
    }

    @Test
    fun artistaInexistenteEsLoadErrorEnAmbosTipos() = runTest {
        assertTrue(repositorio.getDetail(ArtistType.MUSICO, 999).exceptionOrNull() is LoadError)
        assertTrue(repositorio.getDetail(ArtistType.BANDA, 999).exceptionOrNull() is LoadError)
    }

    @Test
    fun errorDelServidorEsLoadErrorEnAmbosTipos() = runTest {
        otrasRoutes["/musicians/1"] = MockResponse().setResponseCode(500)
        otrasRoutes["/bands/1"] = MockResponse().setResponseCode(500)

        assertTrue(repositorio.getDetail(ArtistType.MUSICO, 1).exceptionOrNull() is LoadError)
        assertTrue(repositorio.getDetail(ArtistType.BANDA, 1).exceptionOrNull() is LoadError)
    }

    @Test
    fun desconexionEsLoadErrorEnAmbosTipos() = runTest {
        otrasRoutes["/musicians/1"] = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        otrasRoutes["/bands/1"] = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)

        assertTrue(repositorio.getDetail(ArtistType.MUSICO, 1).exceptionOrNull() is LoadError)
        assertTrue(repositorio.getDetail(ArtistType.BANDA, 1).exceptionOrNull() is LoadError)
    }

    @Test
    fun respuestaIlegibleEsLoadErrorEnAmbosTipos() = runTest {
        otrasRoutes["/musicians/1"] = MockResponse().setBody("""["no es un objeto"]""")
        otrasRoutes["/bands/1"] = MockResponse().setBody("""{"id": """)

        assertTrue(repositorio.getDetail(ArtistType.MUSICO, 1).exceptionOrNull() is LoadError)
        assertTrue(repositorio.getDetail(ArtistType.BANDA, 1).exceptionOrNull() is LoadError)
    }

    @Test
    fun laCancelacionSeRelanzaEnAmbosTipos() = runTest {
        otrasRoutes["/musicians/1"] = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
        otrasRoutes["/bands/1"] = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)

        for (tipo in ArtistType.entries) {
            var resultado: Result<ArtistDetail>? = null
            val consulta = launch(Dispatchers.Default) { resultado = repositorio.getDetail(tipo, 1) }
            servidor.takeRequest()
            consulta.cancelAndJoin()

            // If the adapter swallowed the cancellation, the request would finish with a result.
            assertEquals(null, resultado)
        }
    }
}
