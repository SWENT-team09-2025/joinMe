package com.android.joinme.model.map

import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.joinme.ui.map.userLocation.LocationServiceImpl
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@SmallTest
class LocationServiceImplTest {

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  private lateinit var context: Context
  private lateinit var fusedLocationClient: FusedLocationProviderClient
  private lateinit var locationService: LocationServiceImpl

  @Before
  fun setup() {
    context = mock(Context::class.java)
    fusedLocationClient = mock(FusedLocationProviderClient::class.java)

    // Mock LocationServices.getFusedLocationProviderClient
    mockkStatic(LocationServices::class)
    every { LocationServices.getFusedLocationProviderClient(context) } returns fusedLocationClient

    locationService = LocationServiceImpl(context)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `getUserLocationFlow emits location updates`() = runTest {
    val testLocation = UserLocation(46.5187, 6.5629, 5.0f)

    // Capturer le callback AVANT de lancer le Flow
    val callbackCaptor = argumentCaptor<LocationCallback>()
    whenever(
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(),
                callbackCaptor.capture(),
                anyOrNull<Looper>() // ok pour nullable
                ))
        .thenReturn(Tasks.forResult(null))

    val emissions = mutableListOf<UserLocation?>()
    val job = launch { locationService.getUserLocationFlow().take(1).collect { emissions.add(it) } }

    advanceUntilIdle()

    // Simuler l'émission avec la location mockée
    val androidLocation =
        mock(Location::class.java).apply {
          doReturn(testLocation.latitude).`when`(this).latitude
          doReturn(testLocation.longitude).`when`(this).longitude
          doReturn(testLocation.accuracy).`when`(this).accuracy
        }

    callbackCaptor.firstValue.onLocationResult(LocationResult.create(listOf(androidLocation)))

    advanceUntilIdle()
    job.cancel()

    assertEquals(1, emissions.size)
    assertEquals(testLocation.latitude, emissions[0]!!.latitude, 0.0001)
    assertEquals(testLocation.longitude, emissions[0]!!.longitude, 0.0001)
    assertEquals(testLocation.accuracy, emissions[0]?.accuracy)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `getUserLocationFlow removes updates on close`() = runTest {
    // Capturer le callback AVANT de lancer le Flow
    val callbackCaptor = argumentCaptor<LocationCallback>()
    whenever(
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(), callbackCaptor.capture(), anyOrNull<Looper>()))
        .thenReturn(Tasks.forResult(null))

    val job = launch { locationService.getUserLocationFlow().collect {} }

    advanceUntilIdle()

    job.cancel()
    advanceUntilIdle()

    // Maintenant, le captor contient bien le callback
    verify(fusedLocationClient).removeLocationUpdates(callbackCaptor.firstValue)
  }

  @Test
  fun `getCurrentLocation returns null when no location available`() = runTest {
    whenever(fusedLocationClient.lastLocation).thenReturn(Tasks.forResult(null))
    val result = locationService.getCurrentLocation()
    assertNull(result)
  }

  @Test
  fun `getCurrentLocation returns location when available`() = runTest {
    val testLocation = UserLocation(46.5187, 6.5629, 5.0f)
    val mockLocation =
        mock(Location::class.java).apply {
          doReturn(testLocation.latitude).`when`(this).latitude
          doReturn(testLocation.longitude).`when`(this).longitude
          doReturn(testLocation.accuracy).`when`(this).accuracy
        }

    whenever(fusedLocationClient.lastLocation).thenReturn(Tasks.forResult(mockLocation))

    val result = locationService.getCurrentLocation()
    assertNotNull(result)
    assertEquals(testLocation.latitude, result!!.latitude, 0.0001)
    assertEquals(testLocation.longitude, result.longitude, 0.0001)
    assertEquals(testLocation.accuracy, result.accuracy)
  }

  @Test
  fun `getCurrentLocation returns null on exception`() = runTest {
    whenever(fusedLocationClient.lastLocation)
        .thenReturn(Tasks.forException(Exception("Test exception")))
    val result = locationService.getCurrentLocation()
    assertNull(result)
  }

  @Test
  fun `start and stop LocationUpdates does nothing`() {
    locationService.startLocationUpdates()
    locationService.stopLocationUpdates()
    // Just check it doesn't crash
  }


  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `getUserLocationFlow handles null lastLocation in result`() = runTest {
    // Capturer le callback avant de lancer le flow
    val callbackCaptor = argumentCaptor<LocationCallback>()
    whenever(
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(), callbackCaptor.capture(), anyOrNull<Looper>()))
        .thenReturn(Tasks.forResult(null))

    val emissions = mutableListOf<UserLocation?>()
    val job = launch { locationService.getUserLocationFlow().take(1).collect { emissions.add(it) } }

    advanceUntilIdle()

    // Simuler callback avec location null
    callbackCaptor.firstValue.onLocationResult(LocationResult.create(emptyList()))

    advanceUntilIdle()
    job.cancel()

    // Comme il n'y avait pas de location, emissions doit rester vide
    assertTrue(emissions.isEmpty())
  }
}
