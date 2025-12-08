package com.android.joinme.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Monitors network connectivity status. Provides both synchronous and reactive (Flow) methods to
 * check network availability.
 */
class NetworkMonitor(private val context: Context) {

  private val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  /**
   * Returns current network status synchronously.
   *
   * @return true if device has internet connectivity, false otherwise
   */
  fun isOnline(): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  /**
   * Observes network connectivity changes as a Flow. Emits true when online, false when offline.
   *
   * The Flow will emit the initial state immediately, then emit whenever the network state changes.
   *
   * @return Flow that emits network connectivity status
   */
  fun observeNetworkStatus(): Flow<Boolean> = callbackFlow {
    val callback =
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            trySend(true)
          }

          override fun onLost(network: Network) {
            trySend(false)
          }

          override fun onCapabilitiesChanged(
              network: Network,
              networkCapabilities: NetworkCapabilities
          ) {
            val isOnline =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            trySend(isOnline)
          }
        }

    val request =
        NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

    connectivityManager.registerNetworkCallback(request, callback)

    // Emit initial state
    trySend(isOnline())

    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
  }
}
