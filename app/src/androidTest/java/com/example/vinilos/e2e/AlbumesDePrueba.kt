package com.example.vinilos.e2e

import com.example.vinilos.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Álbum que una prueba crea en el backend: el nombre lleva la hora para no confundirse con otro. */
data class AlbumDePrueba(val id: Int, val nombre: String)

/**
 * Álbumes propios de las E2E que escriben en BackVynils (HU08, HU09 y HU15): cada escenario crea el suyo
 * con `POST /albums`, le agrega canciones, comentarios o artistas desde la app, y [borrarTodos] lo deja
 * como estaba, aunque la prueba falle a mitad de camino.
 *
 * BackVynils no borra un álbum que tiene comentarios o canciones (la base de datos lo impide), así que
 * primero se borran sus comentarios (`DELETE /albums/{id}/comments/{id}`) y sus canciones
 * (`DELETE /albums/{id}/tracks/{id}`, que solo las desvincula del álbum). Al borrar el álbum, la base de
 * datos quita también su relación con los artistas. No se usa `DELETE /musicians/{id}/albums/{id}`: en
 * BackVynils su filtro no devuelve nada y le quitaría al músico todos sus álbumes.
 */
class AlbumesDePrueba(private val baseUrl: String = BuildConfig.BASE_URL) {

    private val http = OkHttpClient()
    private val creados = mutableListOf<Int>()

    fun crear(prefijo: String): AlbumDePrueba {
        val nombre = "$prefijo ${System.currentTimeMillis()}"
        val cuerpo = JSONObject()
            .put("name", nombre)
            .put("cover", "https://upload.wikimedia.org/wikipedia/en/4/42/Beatles_-_Abbey_Road.jpg")
            .put("releaseDate", "2020-01-01T00:00:00.000Z")
            .put("description", "Álbum creado por una prueba E2E.")
            .put("genre", "Rock")
            .put("recordLabel", "EMI")
            .toString()
            .toRequestBody("application/json".toMediaType())
        val id = http.newCall(Request.Builder().url("${baseUrl}albums").post(cuerpo).build()).execute()
            .use { JSONObject(requireNotNull(it.body).string()).getInt("id") }
        synchronized(creados) { creados += id }
        return AlbumDePrueba(id, nombre)
    }

    fun album(id: Int): JSONObject =
        http.newCall(Request.Builder().url("${baseUrl}albums/$id").build()).execute()
            .use { JSONObject(requireNotNull(it.body).string()) }

    /** Ids de los álbumes de un músico (`musicians`) o de una banda (`bands`) según el backend. */
    fun albumesDe(ruta: String, artistaId: Int): Set<Int> {
        val albumes = http.newCall(Request.Builder().url("$baseUrl$ruta/$artistaId").build()).execute()
            .use { JSONObject(requireNotNull(it.body).string()).getJSONArray("albums") }
        return ids(albumes)
    }

    fun borrarTodos() {
        synchronized(creados) { creados.toList() }.forEach { id ->
            runCatching {
                val album = album(id)
                ids(album.getJSONArray("comments")).forEach { borrar("albums/$id/comments/$it") }
                ids(album.getJSONArray("tracks")).forEach { borrar("albums/$id/tracks/$it") }
            }
            borrar("albums/$id")
        }
    }

    private fun borrar(ruta: String) =
        http.newCall(Request.Builder().url("$baseUrl$ruta").delete().build()).execute().close()

    private fun ids(lista: JSONArray): Set<Int> = (0 until lista.length()).map { lista.getJSONObject(it).getInt("id") }.toSet()
}
