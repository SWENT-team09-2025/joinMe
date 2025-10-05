package com.android.joinme.model.event

import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import java.util.Locale

data class Event(
    val uid: String,
    val type: EventType,
    val title: String,
    val description: String,
    val location: Location?,
    val date: Timestamp,
    val duration: Int,
    val participants: List<String>,
    val maxParticipants: Int,
    val visibility: EventVisibility,
    val ownerId: String
)

enum class EventType {
    SPORTS,
    ACTIVITY,
    SOCIAL
}

enum class EventVisibility {
    PUBLIC,
    PRIVATE
}

fun EventType.displayString(): String =
    name.replace("_", " ").lowercase(Locale.ROOT).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

fun EventVisibility.displayString(): String =
    name.replace("_", " ").lowercase(Locale.ROOT).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }