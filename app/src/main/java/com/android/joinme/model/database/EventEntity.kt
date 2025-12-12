package com.android.joinme.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp

/**
 * Room entity for caching Event data locally. Stores events for offline viewing.
 *
 * Note: Room doesn't support complex types like Location or Timestamp directly, so we flatten them
 * into primitive fields and convert back and forth using extension functions.
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val eventId: String,
    val type: String, // Store as string: "SPORTS", "ACTIVITY", "SOCIAL"
    val title: String,
    val description: String,

    // Location fields (nullable)
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val locationName: String?,

    // Date stored as timestamp (seconds + nanoseconds)
    val dateSeconds: Long,
    val dateNanoseconds: Int,
    val duration: Int,
    val participantsJson: String, // JSON array: ["uid1", "uid2"]
    val maxParticipants: Int,
    val visibility: String, // "PUBLIC" or "PRIVATE"
    val ownerId: String,
    val partOfASerie: Boolean,
    val groupId: String?,

    // Metadata
    val cachedAt: Long = System.currentTimeMillis()
)

/** Converts Event domain model to Room entity. */
fun Event.toEntity(): EventEntity {
  return EventEntity(
      eventId = eventId,
      type = type.name,
      title = title,
      description = description,
      locationLatitude = location?.latitude,
      locationLongitude = location?.longitude,
      locationName = location?.name,
      dateSeconds = date.seconds,
      dateNanoseconds = date.nanoseconds,
      duration = duration,
      participantsJson =
          participants.joinToString(",") { "\"$it\"" }.let { if (it.isEmpty()) "[]" else "[$it]" },
      maxParticipants = maxParticipants,
      visibility = visibility.name,
      ownerId = ownerId,
      partOfASerie = partOfASerie,
      groupId = groupId)
}

/** Converts Room entity back to Event domain model. */
fun EventEntity.toEvent(): Event {
  return Event(
      eventId = eventId,
      type = EventType.valueOf(type),
      title = title,
      description = description,
      location =
          if (locationLatitude != null && locationLongitude != null && locationName != null) {
            Location(locationLatitude, locationLongitude, locationName)
          } else null,
      date = Timestamp(dateSeconds, dateNanoseconds),
      duration = duration,
      participants =
          participantsJson
              .removeSurrounding("[", "]")
              .split(",")
              .map { it.trim().removeSurrounding("\"") }
              .filter { it.isNotEmpty() },
      maxParticipants = maxParticipants,
      visibility = EventVisibility.valueOf(visibility),
      ownerId = ownerId,
      partOfASerie = partOfASerie,
      groupId = groupId)
}
