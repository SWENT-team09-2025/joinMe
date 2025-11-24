package com.android.joinme.ui.map.userLocation

import android.annotation.SuppressLint
import android.content.Context
import com.android.joinme.model.map.UserLocation
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementation of [UserLocationService] that uses the Fused Location Provider API to retrieve and
 * stream the user's current location.
 *
 * @param context The application context used to access location services.
 */
class LocationServiceImpl(private val context: Context) : UserLocationService {

  private val fusedLocationClient: FusedLocationProviderClient =
      LocationServices.getFusedLocationProviderClient(context)

  /**
   * Continuously emits the user's location as a [Flow].
   *
   * Uses the Fused Location Provider to request location updates every few seconds. Each new
   * location is sent through the flow until it is closed.
   *
   * @return A cold [Flow] emitting [UserLocation] objects as updates are received. Emits `null` if
   *   no valid location is available.
   */
  @SuppressLint("MissingPermission")
  override fun getUserLocationFlow(): Flow<UserLocation?> = callbackFlow {
    val locationCallback =
        object : LocationCallback() {
          override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
              trySend(
                  UserLocation(
                      latitude = location.latitude,
                      longitude = location.longitude,
                      accuracy = location.accuracy))
            }
          }
        }

    val locationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .apply { setMinUpdateIntervalMillis(5_000L) }
            .build()

    fusedLocationClient.requestLocationUpdates(
        locationRequest, locationCallback, context.mainLooper)

    awaitClose { fusedLocationClient.removeLocationUpdates(locationCallback) }
  }

  /**
   * Retrieves the user's most recent known location once.
   *
   * This method does not continuously listen for updates.
   *
   * @return A [UserLocation] object representing the last known position, or `null` if no location
   *   data is available.
   */
  @SuppressLint("MissingPermission")
  override suspend fun getCurrentLocation(): UserLocation? {
    return try {
      val location = fusedLocationClient.lastLocation.await()
      location?.let {
        UserLocation(latitude = it.latitude, longitude = it.longitude, accuracy = it.accuracy)
      }
    } catch (e: Exception) {
      null
    }
  }
}
