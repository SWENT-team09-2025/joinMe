package com.android.joinme.model.event

import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import java.util.Locale

/**
 * Represents an event within the JoinMe application.
 *
 * Each event includes metadata such as type, title, description, location, and date. Events can be
 * public or private and may have a limited number of participants.
 */
data class Event(
    val eventId: String,
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

/**
 * Defines the type or category of an event.
 * - [SPORTS]: Events involving physical activity or competition (e.g., football, basketball).
 * - [ACTIVITY]: Recreational or hobby-based events (e.g., bowling, hiking).
 * - [SOCIAL]: Informal or social gatherings (e.g., bar outing, dinner).
 */
enum class EventType {
  SPORTS,
  ACTIVITY,
  SOCIAL
}

/**
 * Represents the visibility of an event.
 * - [PUBLIC]: The event is visible and open to all users.
 * - [PRIVATE]: The event is only visible to invited participants.
 */
enum class EventVisibility {
  PUBLIC,
  PRIVATE
}

/**
 * Converts the EventType enum to a more readable display string (camel case).
 *
 * @return A string representation of the EventType, formatted for display.
 */
fun EventType.displayString(): String =
    name.lowercase(Locale.ROOT).replaceFirstChar {
      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

/**
 * Converts the EventVisibility enum to a more readable display string (camel case).
 *
 * @return A string representation of the EventVisibility, formatted for display.
 */
fun EventVisibility.displayString(): String =
    name.lowercase(Locale.ROOT).replaceFirstChar {
      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
