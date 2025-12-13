package com.android.joinme.model.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.map.Location

/**
 * Room entity for caching messages locally for offline viewing.
 *
 * Messages are stored per conversation and indexed by (conversationId, timestamp) for efficient
 * queries. The Location object is flattened into three nullable columns for simpler storage.
 *
 * @property id Unique identifier for the message (primary key)
 * @property conversationId ID of the conversation this message belongs to
 * @property senderId ID of the user who sent the message
 * @property senderName Display name of the sender
 * @property content Text content of the message
 * @property timestamp Unix timestamp (milliseconds) when the message was created
 * @property type Type of message stored as String (MessageType.name)
 * @property readByJson JSON array of user IDs who have read this message: ["uid1", "uid2"]
 * @property isPinned Whether this message is pinned in the conversation
 * @property isEdited Whether this message has been edited
 * @property locationLatitude Latitude coordinate (only for LOCATION type messages)
 * @property locationLongitude Longitude coordinate (only for LOCATION type messages)
 * @property locationName Location name (only for LOCATION type messages)
 * @property cachedAt Timestamp when this message was cached locally
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId", "timestamp"])])
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val type: String, // MessageType.name
    val readByJson: String, // JSON array: ["uid1", "uid2"]
    val isPinned: Boolean,
    val isEdited: Boolean,
    // Location fields (all nullable - only populated for LOCATION type messages)
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val locationName: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Converts a Message to a MessageEntity for Room storage.
 *
 * - Serializes readBy list as JSON array: ["uid1", "uid2"]
 * - Stores MessageType as String (type.name)
 * - Flattens Location object into three nullable columns
 *
 * @return MessageEntity ready for Room database insertion
 */
fun Message.toEntity(): MessageEntity {
  return MessageEntity(
      id = id,
      conversationId = conversationId,
      senderId = senderId,
      senderName = senderName,
      content = content,
      timestamp = timestamp,
      type = type.name,
      readByJson = readBy.joinToString(",") { "\"$it\"" }.let { if (it.isEmpty()) "[]" else "[$it]" },
      isPinned = isPinned,
      isEdited = isEdited,
      locationLatitude = location?.latitude,
      locationLongitude = location?.longitude,
      locationName = location?.name)
}

/**
 * Converts a MessageEntity back to a Message domain model.
 *
 * - Deserializes readByJson from JSON array format
 * - Converts type String back to MessageType enum (defaults to TEXT if invalid)
 * - Reconstructs Location object only if all three fields are non-null
 *
 * @return Message domain model
 */
fun MessageEntity.toMessage(): Message {
  return Message(
      id = id,
      conversationId = conversationId,
      senderId = senderId,
      senderName = senderName,
      content = content,
      timestamp = timestamp,
      type =
          try {
            MessageType.valueOf(type)
          } catch (_: IllegalArgumentException) {
            MessageType.TEXT
          },
      readBy =
          readByJson
              .removeSurrounding("[", "]")
              .split(",")
              .map { it.trim().removeSurrounding("\"") }
              .filter { it.isNotEmpty() },
      isPinned = isPinned,
      isEdited = isEdited,
      location =
          if (locationLatitude != null && locationLongitude != null && locationName != null) {
            Location(latitude = locationLatitude, longitude = locationLongitude, name = locationName)
          } else null)
}
