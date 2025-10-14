package com.android.joinme.ui.map.userLocation

import com.android.joinme.model.map.UserLocation
import kotlinx.coroutines.flow.Flow

interface UserLocationService {
  fun getUserLocationFlow(): Flow<UserLocation?>

  suspend fun getCurrentLocation(): UserLocation?

  fun startLocationUpdates()

  fun stopLocationUpdates()
}
