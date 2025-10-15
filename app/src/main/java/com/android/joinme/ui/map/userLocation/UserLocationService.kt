package com.android.joinme.ui.map.userLocation

import com.android.joinme.model.map.UserLocation
import kotlinx.coroutines.flow.Flow

/**
 * Service interface that provides access to user location data.
 *
 * Implementations of this interface are responsible for retrieving the user's current position and
 * managing continuous location updates.
 */
interface UserLocationService {

  /**
   * Continuously emits location updates as a [Flow].
   *
   * Each emission represents the most recent [UserLocation]. The flow stops emitting when it is
   * closed or when updates are stopped.
   *
   * @return A cold [Flow] emitting [UserLocation] objects as they become available.
   */
  fun getUserLocationFlow(): Flow<UserLocation?>

  /**
   * Retrieves the user's most recent known location once.
   *
   * This is a one-time fetch and does not start continuous updates.
   *
   * @return A [UserLocation] object containing the last known position, or `null` if no location
   *   data is available.
   */
  suspend fun getCurrentLocation(): UserLocation?

  /**
   * Starts continuous location updates.
   *
   * Implementations should begin requesting location updates from the underlying location provider.
   */
  fun startLocationUpdates()

  /**
   * Stops continuous location updates.
   *
   * Implementations should cancel any active location update requests and release related
   * resources.
   */
  fun stopLocationUpdates()
}
