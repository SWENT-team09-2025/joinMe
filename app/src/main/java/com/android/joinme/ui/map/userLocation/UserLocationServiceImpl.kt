package com.android.joinme.ui.map.userLocation

import android.annotation.SuppressLint
import android.content.Context
import com.android.joinme.model.map.UserLocation
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationServiceImpl(private val context: Context) : UserLocationService {

  private val fusedLocationClient: FusedLocationProviderClient =
      LocationServices.getFusedLocationProviderClient(context)

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

  override fun startLocationUpdates() {}

  override fun stopLocationUpdates() {}
}
