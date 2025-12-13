package com.android.joinme.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.joinme.model.profile.Profile
import com.google.firebase.Timestamp

/**
 * Room database entity for caching Profile data locally.
 *
 * Profiles are stored with list fields serialized as JSON arrays and timestamps stored as separate
 * seconds/nanoseconds fields for Room compatibility. Use the extension functions [Profile.toEntity]
 * and [ProfileEntity.toProfile] to convert between domain and database models.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val uid: String,
    val photoUrl: String?,
    val username: String,
    val email: String,
    val dateOfBirth: String?,
    val country: String?,
    val interestsJson: String, // JSON array: ["interest1", "interest2"]
    val bio: String?,
    val createdAtSeconds: Long?,
    val createdAtNanoseconds: Int?,
    val updatedAtSeconds: Long?,
    val updatedAtNanoseconds: Int?,
    val fcmToken: String?,
    val eventsJoinedCount: Int,
    val followersCount: Int,
    val followingCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Converts a Profile domain model to a ProfileEntity for Room database storage.
 *
 * @return ProfileEntity representation of this Profile
 */
fun Profile.toEntity(): ProfileEntity {
  return ProfileEntity(
      uid = uid,
      photoUrl = photoUrl,
      username = username,
      email = email,
      dateOfBirth = dateOfBirth,
      country = country,
      interestsJson =
          interests.joinToString(",") { "\"$it\"" }.let { if (it.isEmpty()) "[]" else "[$it]" },
      bio = bio,
      createdAtSeconds = createdAt?.seconds,
      createdAtNanoseconds = createdAt?.nanoseconds,
      updatedAtSeconds = updatedAt?.seconds,
      updatedAtNanoseconds = updatedAt?.nanoseconds,
      fcmToken = fcmToken,
      eventsJoinedCount = eventsJoinedCount,
      followersCount = followersCount,
      followingCount = followingCount)
}

/**
 * Converts a ProfileEntity from Room database to a Profile domain model.
 *
 * @return Profile domain model representation of this ProfileEntity
 */
fun ProfileEntity.toProfile(): Profile {
  return Profile(
      uid = uid,
      photoUrl = photoUrl,
      username = username,
      email = email,
      dateOfBirth = dateOfBirth,
      country = country,
      interests =
          interestsJson
              .removeSurrounding("[", "]")
              .split(",")
              .map { it.trim().removeSurrounding("\"") }
              .filter { it.isNotEmpty() },
      bio = bio,
      createdAt =
          if (createdAtSeconds != null && createdAtNanoseconds != null) {
            Timestamp(createdAtSeconds, createdAtNanoseconds)
          } else null,
      updatedAt =
          if (updatedAtSeconds != null && updatedAtNanoseconds != null) {
            Timestamp(updatedAtSeconds, updatedAtNanoseconds)
          } else null,
      fcmToken = fcmToken,
      eventsJoinedCount = eventsJoinedCount,
      followersCount = followersCount,
      followingCount = followingCount)
}
