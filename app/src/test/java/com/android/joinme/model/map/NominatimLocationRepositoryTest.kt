package com.android.joinme.model.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.io.IOException
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@SmallTest
class NominatimLocationRepositoryTest {

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  private lateinit var client: OkHttpClient
  private lateinit var call: Call
  private lateinit var repository: NominatimLocationRepository

  @Before
  fun setup() {
    client = mock(OkHttpClient::class.java)
    call = mock(Call::class.java)
    repository = NominatimLocationRepository(client)
    whenever(client.newCall(any<Request>())).thenReturn(call)
  }

  private fun createMockResponse(code: Int, body: String): Response {
    return Response.Builder()
        .request(Request.Builder().url("https://nominatim.openstreetmap.org/search").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("OK")
        .body(body.toResponseBody())
        .build()
  }

  @Test
  fun `search returns empty list when response body is empty`() = runTest {
    val response = createMockResponse(200, "[]")
    whenever(call.execute()).thenReturn(response)

    val results = repository.search("test query")

    assertTrue(results.isEmpty())
  }

  @Test
  fun `search returns list of locations for valid JSON response`() = runTest {
    val jsonResponse =
        """
      [
        {
          "lat": "46.5197",
          "lon": "6.6323",
          "display_name": "EPFL, Lausanne, Switzerland"
        },
        {
          "lat": "48.8566",
          "lon": "2.3522",
          "display_name": "Paris, France"
        }
      ]
    """
            .trimIndent()

    val response = createMockResponse(200, jsonResponse)
    whenever(call.execute()).thenReturn(response)

    val results = repository.search("epfl")

    assertEquals(2, results.size)
    assertEquals(46.5197, results[0].latitude, 0.0001)
    assertEquals(6.6323, results[0].longitude, 0.0001)
    assertEquals("EPFL, Lausanne, Switzerland", results[0].name)
    assertEquals(48.8566, results[1].latitude, 0.0001)
    assertEquals(2.3522, results[1].longitude, 0.0001)
    assertEquals("Paris, France", results[1].name)
  }

  @Test
  fun `search returns empty list for empty JSON array`() = runTest {
    val response = createMockResponse(200, "[]")
    whenever(call.execute()).thenReturn(response)

    val results = repository.search("nonexistent location")

    assertTrue(results.isEmpty())
  }

  @Test(expected = Exception::class)
  fun `search throws exception for unsuccessful response`() = runTest {
    val response = createMockResponse(404, "Not Found")
    whenever(call.execute()).thenReturn(response)

    repository.search("test query")
  }

  @Test(expected = IOException::class)
  fun `search throws IOException on network failure`() = runTest {
    whenever(call.execute()).thenThrow(IOException("Network error"))

    repository.search("test query")
  }

  @Test
  fun `search handles single location result`() = runTest {
    val jsonResponse =
        """
      [
        {
          "lat": "40.7128",
          "lon": "-74.0060",
          "display_name": "New York, NY, USA"
        }
      ]
    """
            .trimIndent()

    val response = createMockResponse(200, jsonResponse)
    whenever(call.execute()).thenReturn(response)

    val results = repository.search("new york")

    assertEquals(1, results.size)
    assertEquals(40.7128, results[0].latitude, 0.0001)
    assertEquals(-74.0060, results[0].longitude, 0.0001)
    assertEquals("New York, NY, USA", results[0].name)
  }

  @Test(expected = Exception::class)
  fun `search handles malformed JSON`() = runTest {
    val malformedJson = """{"invalid": "json" without proper structure"""

    val response = createMockResponse(200, malformedJson)
    whenever(call.execute()).thenReturn(response)

    repository.search("test query")
  }

  @Test
  fun `search handles locations with negative coordinates`() = runTest {
    val jsonResponse =
        """
      [
        {
          "lat": "-33.8688",
          "lon": "151.2093",
          "display_name": "Sydney, Australia"
        }
      ]
    """
            .trimIndent()

    val response = createMockResponse(200, jsonResponse)
    whenever(call.execute()).thenReturn(response)

    val results = repository.search("sydney")

    assertEquals(1, results.size)
    assertEquals(-33.8688, results[0].latitude, 0.0001)
    assertEquals(151.2093, results[0].longitude, 0.0001)
    assertEquals("Sydney, Australia", results[0].name)
  }

  @Test
  fun `search handles special characters in display name`() = runTest {
    val jsonResponse =
        """
      [
        {
          "lat": "48.2082",
          "lon": "16.3738",
          "display_name": "Wien, Österreich (Vienna, Austria)"
        }
      ]
    """
            .trimIndent()

    val response = createMockResponse(200, jsonResponse)
    whenever(call.execute()).thenReturn(response)

    val results = repository.search("vienna")

    assertEquals(1, results.size)
    assertEquals("Wien, Österreich (Vienna, Austria)", results[0].name)
  }

  @Test(expected = Exception::class)
  fun `search handles 500 internal server error`() = runTest {
    val response = createMockResponse(500, "Internal Server Error")
    whenever(call.execute()).thenReturn(response)

    repository.search("test query")
  }

  @Test(expected = Exception::class)
  fun `search handles 429 rate limit error`() = runTest {
    val response = createMockResponse(429, "Too Many Requests")
    whenever(call.execute()).thenReturn(response)

    repository.search("test query")
  }

  @Test
  fun `search handles zero coordinates`() = runTest {
    val jsonResponse =
        """
      [
        {
          "lat": "0.0",
          "lon": "0.0",
          "display_name": "Null Island"
        }
      ]
    """
            .trimIndent()

    val response = createMockResponse(200, jsonResponse)
    whenever(call.execute()).thenReturn(response)

    val results = repository.search("null island")

    assertEquals(1, results.size)
    assertEquals(0.0, results[0].latitude, 0.0001)
    assertEquals(0.0, results[0].longitude, 0.0001)
  }
}
