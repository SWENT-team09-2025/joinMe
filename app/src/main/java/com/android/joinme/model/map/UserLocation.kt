package com.android.joinme.model.map

/**
 * Represents the geographical position of a user.
 *
 * This model contains the user's latitude, longitude, and optional accuracy. A timestamp is
 * automatically added when the location is created.
 *
 * @property latitude The user's latitude in decimal degrees.
 * @property longitude The user's longitude in decimal degrees.
 * @property accuracy The estimated accuracy of the location in meters (optional).
 * @property timestamp The time when the location was recorded, in milliseconds since epoch.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)
