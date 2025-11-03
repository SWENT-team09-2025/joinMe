package com.android.joinme.model.serie

import com.android.joinme.model.event.Event
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp

/** Milliseconds per minute conversion factor */
private const val MILLIS_PER_MINUTE = 60_000L

/**
 * Represents a series of recurring events within the JoinMe application.
 *
 * A series groups multiple events together that share common properties like title, description,
 * max participants, and visibility. Each event in the series maintains its own specific date and
 * duration.
 *
 * @property serieId Unique identifier for this series
 * @property title The title of the series
 * @property description Detailed description of what the series is about
 * @property date The starting date/time of the series
 * @property participants List of user IDs of participants who have joined the series
 * @property maxParticipants Maximum number of participants allowed per event in the series
 * @property visibility Visibility setting for events in this series (public or private)
 * @property eventIds List of event IDs belonging to this series
 * @property ownerId User ID of the series creator/owner
 */
data class Serie(
    val serieId: String,
    val title: String,
    val description: String,
    val date: Timestamp,
    val participants: List<String>,
    val maxParticipants: Int,
    val visibility: Visibility,
    val eventIds: List<String>,
    val ownerId: String,
)

/**
 * Data class combining a Serie with its associated Event objects. Useful for displaying series with
 * their events in the UI.
 *
 * @property serie The series object
 * @property events List of events associated with this series
 */
data class SerieWithEvents(val serie: Serie, val events: List<Event>)

/**
 * Calculates the total duration of all events in the series.
 *
 * @param events List of all events to search through
 * @return Total duration in minutes
 */
fun Serie.getTotalDuration(events: List<Event>): Int {
  return events.filter { it.eventId in eventIds }.sumOf { it.duration }
}

/**
 * Checks if the series has any currently active/ongoing events.
 *
 * An event is considered active if the current time falls between its start time and end time.
 *
 * @param events List of all events to check
 * @return True if at least one event in the series is currently active, false otherwise
 */
fun Serie.isActive(events: List<Event>): Boolean {
  val now = System.currentTimeMillis()
  return events.any { event ->
    if (event.eventId !in eventIds) return@any false
    try {
      val startTime = event.date.toDate().time
      val endTime = startTime + (event.duration * MILLIS_PER_MINUTE)
      now >= startTime && now < endTime
    } catch (e: Exception) {
      false
    }
  }
}

/**
 * Checks if all events in the series have already occurred.
 *
 * A series is considered expired if all its events have finished (end time has passed).
 *
 * @param events List of all events to check
 * @return True if all events in the series have finished or if the series has no events, false if
 *   any event is ongoing or upcoming
 */
fun Serie.isExpired(events: List<Event>): Boolean {
  val serieEvents = events.filter { it.eventId in eventIds }
  if (serieEvents.isEmpty()) return true

  val now = System.currentTimeMillis()
  return serieEvents.all { event ->
    try {
      val endTimeMillis = event.date.toDate().time + (event.duration * MILLIS_PER_MINUTE)
      endTimeMillis < now
    } catch (e: Exception) {
      false
    }
  }
}

/**
 * Checks if the series is upcoming.
 *
 * A series is considered upcoming if its date hasn't occurred yet (date is in the future).
 *
 * @return True if the series date is in the future, false otherwise
 */
fun Serie.isUpcoming(): Boolean {
  val now = System.currentTimeMillis()
  return try {
    date.toDate().time > now
  } catch (e: Exception) {
    false
  }
}

/**
 * Retrieves all events belonging to this series, sorted by date.
 *
 * @param events List of all events to search through
 * @return List of events belonging to this series, sorted chronologically by start date
 */
fun Serie.getSerieEvents(events: List<Event>): List<Event> {
  return events
      .filter { it.eventId in eventIds }
      .sortedBy {
        try {
          it.date.toDate().time
        } catch (e: Exception) {
          0L
        }
      }
}

/**
 * Gets the total number of events in the series.
 *
 * @return The count of events in this series
 */
fun Serie.getTotalEventsCount(): Int = eventIds.size

/**
 * Formats the total duration of the series into a human-readable string.
 *
 * @param events List of all events to calculate duration from
 * @return Formatted duration string (e.g., "5h 30min", "5h", or "90min")
 */
fun Serie.getFormattedDuration(events: List<Event>): String {
  val totalMinutes = getTotalDuration(events)
  val hours = totalMinutes / 60
  val minutes = totalMinutes % 60

  return when {
    hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
    hours > 0 -> "${hours}h"
    else -> "${minutes}min"
  }
}
