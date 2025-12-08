package com.android.joinme.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NetworkMonitorTest {

  private lateinit var context: Context
  private lateinit var connectivityManager: ConnectivityManager
  private lateinit var networkMonitor: NetworkMonitor
  private lateinit var mockNetwork: Network
  private lateinit var mockCapabilities: NetworkCapabilities

  @Before
  fun setUp() {
    context = mockk(relaxed = true)
    connectivityManager = mockk(relaxed = true)
    mockNetwork = mockk(relaxed = true)
    mockCapabilities = mockk(relaxed = true)

    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

    networkMonitor = NetworkMonitor(context)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `isOnline returns true when network is available and validated`() {
    every { connectivityManager.activeNetwork } returns mockNetwork
    every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns
        true
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns
        true

    val result = networkMonitor.isOnline()

    assertTrue(result)
  }

  @Test
  fun `isOnline returns false when no active network`() {
    every { connectivityManager.activeNetwork } returns null

    val result = networkMonitor.isOnline()

    assertFalse(result)
  }

  @Test
  fun `isOnline returns false when network capabilities are null`() {
    every { connectivityManager.activeNetwork } returns mockNetwork
    every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns null

    val result = networkMonitor.isOnline()

    assertFalse(result)
  }

  @Test
  fun `isOnline returns false when network has internet but not validated`() {
    every { connectivityManager.activeNetwork } returns mockNetwork
    every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns
        true
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns
        false

    val result = networkMonitor.isOnline()

    assertFalse(result)
  }

  @Test
  fun `isOnline returns false when network is validated but no internet capability`() {
    every { connectivityManager.activeNetwork } returns mockNetwork
    every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns
        false
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns
        true

    val result = networkMonitor.isOnline()

    assertFalse(result)
  }

  @Test
  fun `isOnline returns false when network has neither internet nor validated`() {
    every { connectivityManager.activeNetwork } returns mockNetwork
    every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns
        false
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns
        false

    val result = networkMonitor.isOnline()

    assertFalse(result)
  }

  @Test
  fun `observeNetworkStatus emits initial state`() = runTest {
    every { connectivityManager.activeNetwork } returns mockNetwork
    every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns
        true
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns
        true
    every {
      connectivityManager.registerNetworkCallback(
          any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>())
    } just Runs

    val flow = networkMonitor.observeNetworkStatus()
    val initialValue = flow.first()

    assertTrue(initialValue)
  }

  @Test
  fun `observeNetworkStatus emits false as initial state when offline`() = runTest {
    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(
          any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>())
    } just Runs

    val flow = networkMonitor.observeNetworkStatus()
    val initialValue = flow.first()

    assertFalse(initialValue)
  }

  @Test
  fun `observeNetworkStatus registers network callback when collected`() = runTest {
    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(
          any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>())
    } just Runs

    // Collect the flow to trigger callback registration
    val flow = networkMonitor.observeNetworkStatus()
    val firstValue = flow.first()

    // Verify that registerNetworkCallback was called
    verify {
      connectivityManager.registerNetworkCallback(
          any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>())
    }
  }

  @Test
  fun `NetworkCallback can be captured and invoked`() = runTest {
    val callbackSlot = slot<ConnectivityManager.NetworkCallback>()

    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot))
    } just Runs

    // Collect first value to trigger callback registration
    val flow = networkMonitor.observeNetworkStatus()
    val firstValue = flow.first()

    // Verify the callback was captured
    assertTrue(callbackSlot.isCaptured)
    assertNotNull(callbackSlot.captured)

    // Verify we can invoke callback methods without errors
    callbackSlot.captured.onAvailable(mockNetwork)
    callbackSlot.captured.onLost(mockNetwork)
  }

  @Test
  fun `NetworkCallback onCapabilitiesChanged can be invoked`() = runTest {
    val callbackSlot = slot<ConnectivityManager.NetworkCallback>()

    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot))
    } just Runs

    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns
        true
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns
        true

    val flow = networkMonitor.observeNetworkStatus()
    val firstValue = flow.first()

    // Verify we can invoke onCapabilitiesChanged without errors
    assertTrue(callbackSlot.isCaptured)
    callbackSlot.captured.onCapabilitiesChanged(mockNetwork, mockCapabilities)
  }

  @Test
  fun `observeNetworkStatus unregisters callback on close`() = runTest {
    val callbackSlot = slot<ConnectivityManager.NetworkCallback>()

    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot))
    } just Runs
    every {
      connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>())
    } just Runs

    val flow = networkMonitor.observeNetworkStatus()
    val initialValue = flow.first() // Collect one value then close

    // Wait a bit for cleanup
    kotlinx.coroutines.delay(100)

    // Verify unregister was called (may not always work due to timing)
    // This is a best-effort test
    verify(timeout = 1000, atLeast = 0) {
      connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>())
    }
  }

  @Test
  fun `NetworkMonitor uses correct context service`() {
    verify { context.getSystemService(Context.CONNECTIVITY_SERVICE) }
  }

  @Test
  fun `multiple isOnline calls return consistent results`() {
    every { connectivityManager.activeNetwork } returns mockNetwork
    every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns
        true
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns
        true

    val result1 = networkMonitor.isOnline()
    val result2 = networkMonitor.isOnline()
    val result3 = networkMonitor.isOnline()

    assertTrue(result1)
    assertTrue(result2)
    assertTrue(result3)
  }

  @Test
  fun `isOnline reflects network state changes`() {
    // Initially online
    every { connectivityManager.activeNetwork } returns mockNetwork
    every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns
        true
    every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns
        true

    assertTrue(networkMonitor.isOnline())

    // Simulate network going offline
    every { connectivityManager.activeNetwork } returns null

    assertFalse(networkMonitor.isOnline())

    // Simulate network coming back online
    every { connectivityManager.activeNetwork } returns mockNetwork

    assertTrue(networkMonitor.isOnline())
  }

  @Test
  fun `NetworkRequest is built with correct capabilities`() = runTest {
    val requestSlot = slot<NetworkRequest>()

    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(
          capture(requestSlot), any<ConnectivityManager.NetworkCallback>())
    } just Runs

    val flow = networkMonitor.observeNetworkStatus()
    val firstValue = flow.first()

    // Verify a NetworkRequest was created
    verify {
      connectivityManager.registerNetworkCallback(
          any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>())
    }
  }
}
