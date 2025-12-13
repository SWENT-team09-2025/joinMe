package com.android.joinme.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.Group

/**
 * Room database entity for caching Group data locally.
 *
 * Groups are stored with list fields serialized as JSON arrays for Room compatibility. Use the
 * extension functions [Group.toEntity] and [GroupEntity.toGroup] to convert between domain and
 * database models.
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // EventType.name
    val description: String,
    val ownerId: String,
    val memberIdsJson: String, // JSON array: ["uid1", "uid2"]
    val eventIdsJson: String, // JSON array: ["eventId1", "eventId2"]
    val serieIdsJson: String, // JSON array: ["serieId1", "serieId2"]
    val photoUrl: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Converts a Group domain model to a GroupEntity for Room database storage.
 *
 * @return GroupEntity representation of this Group
 */
fun Group.toEntity(): GroupEntity {
  return GroupEntity(
      id = id,
      name = name,
      category = category.name,
      description = description,
      ownerId = ownerId,
      memberIdsJson =
          memberIds.joinToString(",") { "\"$it\"" }.let { if (it.isEmpty()) "[]" else "[$it]" },
      eventIdsJson =
          eventIds.joinToString(",") { "\"$it\"" }.let { if (it.isEmpty()) "[]" else "[$it]" },
      serieIdsJson =
          serieIds.joinToString(",") { "\"$it\"" }.let { if (it.isEmpty()) "[]" else "[$it]" },
      photoUrl = photoUrl)
}

/**
 * Converts a GroupEntity from Room database to a Group domain model.
 *
 * @return Group domain model representation of this GroupEntity
 */
fun GroupEntity.toGroup(): Group {
  return Group(
      id = id,
      name = name,
      category = EventType.valueOf(category),
      description = description,
      ownerId = ownerId,
      memberIds =
          memberIdsJson
              .removeSurrounding("[", "]")
              .split(",")
              .map { it.trim().removeSurrounding("\"") }
              .filter { it.isNotEmpty() },
      eventIds =
          eventIdsJson
              .removeSurrounding("[", "]")
              .split(",")
              .map { it.trim().removeSurrounding("\"") }
              .filter { it.isNotEmpty() },
      serieIds =
          serieIdsJson
              .removeSurrounding("[", "]")
              .split(",")
              .map { it.trim().removeSurrounding("\"") }
              .filter { it.isNotEmpty() },
      photoUrl = photoUrl)
}
