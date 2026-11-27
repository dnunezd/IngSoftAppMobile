package com.example.vinilos.e2e

import com.example.vinilos.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Album a test creates in the backend: the name carries a timestamp so it isn't confused with another one. */
data class AlbumDePrueba(val id: Int, val nombre: String)

/**
 * Albums owned by the E2E tests that write to BackVynils (HU08, HU09 and HU15): each scenario creates its
 * own with `POST /albums`, adds tracks, comments or artists from the app, and [borrarTodos] leaves it
 * as it was, even if the test fails midway.
 *
 * BackVynils won't delete an album that has comments or tracks (the database prevents it), so its
 * comments are deleted first (`DELETE /albums/{id}/comments/{id}`) and its tracks
 * (`DELETE /albums/{id}/tracks/{id}`, which only unlinks them from the album). When the album is deleted, the
 * database also removes its relation with the artists. `DELETE /musicians/{id}/albums/{id}` is not used: in
 * BackVynils its filter returns nothing and would remove all of the musician's albums.
 */
class AlbumesDePrueba(private val baseUrl: String = BuildConfig.BASE_URL) {

    private val http = OkHttpClient()
    private val creados = mutableListOf<Int>()

    fun create(prefijo: String): AlbumDePrueba {
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
        // The `album` id sequence starts at 1, but the sample data uses ids 100 to 103: when it
        // reaches them, BackVynils responds 500 for a duplicate key. Each failed attempt advances the
        // sequence, so it's enough to retry until the occupied ids are passed.
        repeat(INTENTOS) {
            val id = http.newCall(Request.Builder().url("${baseUrl}albums").post(cuerpo).build()).execute().use { respuesta ->
                if (respuesta.isSuccessful) JSONObject(requireNotNull(respuesta.body).string()).getInt("id") else null
            }
            if (id != null) {
                synchronized(creados) { creados += id }
                return AlbumDePrueba(id, nombre)
            }
        }
        error("BackVynils no creó el álbum de prueba después de $INTENTOS intentos")
    }

    fun album(id: Int): JSONObject =
        http.newCall(Request.Builder().url("${baseUrl}albums/$id").build()).execute()
            .use { JSONObject(requireNotNull(it.body).string()) }

    /** Ids of a musician's albums (`musicians`) or a band's (`bands`) depending on the backend. */
    fun albumesDe(ruta: String, artistaId: Int): Set<Int> {
        val albumes = http.newCall(Request.Builder().url("$baseUrl$ruta/$artistaId").build()).execute()
            .use { JSONObject(requireNotNull(it.body).string()).getJSONArray("albums") }
        return ids(albumes)
    }

    /** Deletes an album with no tracks or comments mid-test, as if someone else had deleted it. */
    fun borrar(album: AlbumDePrueba) {
        borrar("albums/${album.id}")
        synchronized(creados) { creados -= album.id }
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

    private companion object {
        /** More than the sample albums (ids 100 to 103) that the sequence may run into in a row. */
        const val INTENTOS = 10
    }

    private fun ids(lista: JSONArray): Set<Int> = (0 until lista.length()).map { lista.getJSONObject(it).getInt("id") }.toSet()
}
