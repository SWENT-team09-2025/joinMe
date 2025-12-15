package com.android.joinme.model.map

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * Repository implementation backed by the public OpenStreetMap Nominatim API.
 *
 * Used to resolve a free-text query into a list of real world locations (lat/lon + human readable
 * name).
 *
 * Notes:
 * - Network call is dispatched on `Dispatchers.IO`
 * - Results are limited to Switzerland (`countrycodes=ch`)
 * - Nominatim requires a proper `User-Agent` header (ours is compliant)
 */
class NominatimLocationRepository(private val client: OkHttpClient) : LocationRepository {

  /**
   * Parses the JSON returned by Nominatim into a list of [Location].
   *
   * Expected format: Array of objects with "latitude", "longitude", and "name".
   *
   * @throws org.json.JSONException if the body is malformed
   */
  private fun parseBody(body: String): List<Location> {
    val jsonArray = JSONArray(body)

    return List(jsonArray.length()) { i ->
      val jsonObject = jsonArray.getJSONObject(i)
      val lat = jsonObject.getDouble("lat")
      val lon = jsonObject.getDouble("lon")
      val name = jsonObject.getString("display_name")
      Location(lat, lon, name)
    }
  }

  /**
   * Searches Nominatim with the given textual [query].
   *
   * @param query Free-text query such as "lausanne" or "epfl".
   * @return A list of up to 5 matching [Location] objects.
   * @throws IOException if the underlying HTTP client fails
   * @throws Exception if the HTTP response code is not 2xx
   */
  override suspend fun search(query: String): List<Location> =
      withContext(Dispatchers.IO) {
        // Using HttpUrl.Builder to properly construct the URL with query parameters.
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .addQueryParameter("limit", "5")
                .addQueryParameter("countrycodes", "ch") // Limit to Switzerland
                .build()

        // Create the request with a custom User-Agent and optional Referer
        val request = Request.Builder().url(url).header("User-Agent", "JoinMe Android App").build()

        try {
          val response = client.newCall(request).execute()
          response.use {
            if (!response.isSuccessful) {
              throw Exception("Unexpected code $response")
            }

            val body = response.body?.string()
            return@withContext if (body != null) {
              parseBody(body)
            } else {
              emptyList()
            }
          }
        } catch (e: IOException) {
          throw e
        }
      }
}
