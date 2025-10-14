package com.android.joinme.model.map

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)
