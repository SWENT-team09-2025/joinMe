package com.android.joinme.model.serie

import com.android.joinme.model.event.Event
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp

/** Note: This file was co-written with the help of AI (Claude) */

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
 * @property lastEventEndTime The end time of the last event in the series. Used for optimization to
 *   check if series is expired without loading all events. This is managed internally by the
 *   repository and should not be set manually.
 */
data class Serie
internal constructor(
    val serieId: String,
    val title: String,
    val description: String,
    val date: Timestamp,
    val participants: List<String>,
    val maxParticipants: Int,
    val visibility: Visibility,
    val eventIds: List<String>,
    val ownerId: String,
    val lastEventEndTime: Timestamp? = null,
) {
  companion object {
    /**
     * Creates a new Serie instance. This is the public API for creating Series.
     *
     * @param serieId Unique identifier for this series
     * @param title The title of the series
     * @param description Detailed description of what the series is about
     * @param date The starting date/time of the series
     * @param participants List of user IDs of participants who have joined the series
     * @param maxParticipants Maximum number of participants allowed per event in the series
     * @param visibility Visibility setting for events in this series (public or private)
     * @param eventIds List of event IDs belonging to this series
     * @param ownerId User ID of the series creator/owner
     * @return A new Serie instance with lastEventEndTime initialized to the serie's start date
     */
    @JvmStatic
    operator fun invoke(
        serieId: String,
        title: String,
        description: String,
        date: Timestamp,
        participants: List<String>,
        maxParticipants: Int,
        visibility: Visibility,
        eventIds: List<String>,
        ownerId: String,
    ): Serie {
      return Serie(
          serieId = serieId,
          title = title,
          description = description,
          date = date,
          participants = participants,
          maxParticipants = maxParticipants,
          visibility = visibility,
          eventIds = eventIds,
          ownerId = ownerId,
          lastEventEndTime = date)
    }
  }
}

/**
 * Data class combining a Serie with its associated Event objects. Useful for displaying series with
 * their events in the UI.
 *
 * @property serie The series object
 * @property events List of events associated with this series
 */
data class SerieWithEvents(val serie: Serie, val events: List<Event>)

/**
 * Calculates the total duration of the series.
 *
 * Since events follow each other without gaps, the total duration is the time span from the serie
 * start date to the last event end time.
 *
 * @return Total duration in minutes
 */
fun Serie.getTotalDuration(): Int {
  return try {
    val startTime = date.toDate().time
    val endTime = lastEventEndTime?.toDate()?.time ?: startTime
    val durationMillis = endTime - startTime
    (durationMillis / MILLIS_PER_MINUTE).toInt()
  } catch (e: Exception) {
    0
  }
}

/**
 * Checks if the series is currently active.
 *
 * A series is considered active if the current time falls between its start time and the last event
 * end time.
 *
 * @return True if the series is currently active, false otherwise
 */
fun Serie.isActive(): Boolean {
  val now = System.currentTimeMillis()
  return try {
    val startTime = date.toDate().time
    val endTime = lastEventEndTime?.toDate()?.time ?: startTime
    now >= startTime && now < endTime
  } catch (e: Exception) {
    false
  }
}

/**
 * Checks if the series has expired.
 *
 * A series is considered expired if the last event end time has passed.
 *
 * @return True if the series has finished, false otherwise
 */
fun Serie.isExpired(): Boolean {
  val now = System.currentTimeMillis()
  return try {
    val startTime = date.toDate().time
    val endTime = lastEventEndTime?.toDate()?.time ?: startTime
    // Only expired if the serie has actually run (endTime > startTime) and has finished
    if (endTime <= startTime) {
      false // Not started yet (no events added)
    } else {
      endTime < now
    }
  } catch (e: Exception) {
    false
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
 * @return Formatted duration string (e.g., "5h 30min", "5h", or "90min")
 */
fun Serie.getFormattedDuration(): String {
  val totalMinutes = getTotalDuration()
  val hours = totalMinutes / 60
  val minutes = totalMinutes % 60

  return when {
    hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
    hours > 0 -> "${hours}h"
    else -> "${minutes}min"
  }
}
