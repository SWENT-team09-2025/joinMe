package com.android.joinme.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp

/**
 * Room entity for caching Serie data locally. Stores series for offline viewing.
 *
 * Note: Room doesn't support complex types like Timestamp directly, so we flatten them into
 * primitive fields and convert back and forth using extension functions.
 */
@Entity(tableName = "series")
data class SerieEntity(
    @PrimaryKey val serieId: String,
    val title: String,
    val description: String,
    // Date stored as timestamp (seconds + nanoseconds)
    val dateSeconds: Long,
    val dateNanoseconds: Int,
    val participantsJson: String, // JSON array: ["uid1", "uid2"]
    val maxParticipants: Int,
    val visibility: String, // "PUBLIC" or "PRIVATE"
    val eventIdsJson: String, // JSON array: ["eventId1", "eventId2"]
    val ownerId: String,
    val lastEventEndTimeSeconds: Long?,
    val lastEventEndTimeNanoseconds: Int?,
    val groupId: String?,
    // Metadata
    val cachedAt: Long = System.currentTimeMillis()
)

/** Converts Serie domain model to Room entity. */
fun Serie.toEntity(): SerieEntity {
  return SerieEntity(
      serieId = serieId,
      title = title,
      description = description,
      dateSeconds = date.seconds,
      dateNanoseconds = date.nanoseconds,
      participantsJson =
          participants.joinToString(",") { "\"$it\"" }.let { if (it.isEmpty()) "[]" else "[$it]" },
      maxParticipants = maxParticipants,
      visibility = visibility.name,
      eventIdsJson =
          eventIds.joinToString(",") { "\"$it\"" }.let { if (it.isEmpty()) "[]" else "[$it]" },
      ownerId = ownerId,
      lastEventEndTimeSeconds = lastEventEndTime?.seconds,
      lastEventEndTimeNanoseconds = lastEventEndTime?.nanoseconds,
      groupId = groupId)
}

/** Converts Room entity back to Serie domain model. */
fun SerieEntity.toSerie(): Serie {
  return Serie(
      serieId = serieId,
      title = title,
      description = description,
      date = Timestamp(dateSeconds, dateNanoseconds),
      participants =
          participantsJson
              .removeSurrounding("[", "]")
              .split(",")
              .map { it.trim().removeSurrounding("\"") }
              .filter { it.isNotEmpty() },
      maxParticipants = maxParticipants,
      visibility = Visibility.valueOf(visibility),
      eventIds =
          eventIdsJson
              .removeSurrounding("[", "]")
              .split(",")
              .map { it.trim().removeSurrounding("\"") }
              .filter { it.isNotEmpty() },
      ownerId = ownerId,
      lastEventEndTime =
          if (lastEventEndTimeSeconds != null && lastEventEndTimeNanoseconds != null) {
            Timestamp(lastEventEndTimeSeconds, lastEventEndTimeNanoseconds)
          } else null,
      groupId = groupId)
}
