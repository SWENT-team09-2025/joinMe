package com.android.joinme.model.profile

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.Timestamp
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-memory implementation of [ProfileRepository] for testing purposes.
 *
 * This repository stores profiles in a mutable map and doesn't persist data between app restarts.
 * It's designed for use in instrumented tests where Firebase is not available or desired.
 */
class ProfileRepositoryLocal : ProfileRepository {

  private val profiles = mutableMapOf<String, Profile>()

  init {
    // Add a default test user profile
    val now = Timestamp.now()
    val testProfile =
        Profile(
            uid = "test-user-123",
            username = "Test User",
            email = "test@joinme.com",
            photoUrl = "http://example.com/avatar.png",
            country = "Switzerland",
            bio = "Test user for E2E testing",
            interests = listOf("Sports", "Technology"),
            dateOfBirth = "1990-01-01",
            createdAt = now,
            updatedAt = now,
            eventsJoinedCount = 5)
    profiles[testProfile.uid] = testProfile
  }

  override suspend fun getProfile(uid: String): Profile? {
    return profiles[uid]
  }

  override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? {
    if (uids.isEmpty()) return emptyList()

    val result = uids.mapNotNull { getProfile(it) }
    // Return null if not all profiles were found
    return if (result.size == uids.size) result else null
  }

  override suspend fun createOrUpdateProfile(profile: Profile) {
    profiles[profile.uid] = profile
  }

  override suspend fun deleteProfile(uid: String) {
    profiles.remove(uid)
  }

  override suspend fun uploadProfilePhoto(context: Context, uid: String, imageUri: Uri): String =
      withContext(Dispatchers.IO) {
        // 1) Resolve current profile
        val current =
            getProfile(uid)
                ?: throw IllegalArgumentException("Profile with UID $uid does not exist")

        // 2) Ensure app-local pictures dir
        val picturesDir = File(context.filesDir, "profile_photos").apply { mkdirs() }

        // 3) If there was a previous local file, delete it
        current.photoUrl?.let { previousUrl ->
          // Only delete if it points into our app dir (avoid deleting arbitrary files)
          runCatching {
            val prev = previousUrl.toUri()
            if (prev.scheme == "file") {
              File(prev.path ?: "").takeIf { it.exists() && it.parentFile == picturesDir }?.delete()
            }
          }
        }

        // 4) Copy new content into app storage with a stable name
        val filename = "${uid}_${System.currentTimeMillis()}.jpg"
        val dest = File(picturesDir, filename)

        context.contentResolver.openInputStream(imageUri).use { inStream ->
          requireNotNull(inStream) { "Unable to open input stream for $imageUri" }
          FileOutputStream(dest).use { out -> inStream.copyTo(out) }
        }

        // 5) Build a file:// URL (or use FileProvider if you prefer content://)
        val fileUrl =
            dest.toURI().toURL().toString() // "file:///data/user/0/.../profile_photos/uid_....jpg"

        // 6) Update profile
        val updated = current.copy(photoUrl = fileUrl, updatedAt = Timestamp.now())
        profiles[uid] = updated

        return@withContext fileUrl
      }

  override suspend fun deleteProfilePhoto(uid: String) =
      withContext(Dispatchers.IO) {
        val current = profiles[uid] ?: return@withContext
        current.photoUrl?.let { url ->
          runCatching {
            val uri = url.toUri()
            if (uri.scheme == "file") {
              File(uri.path ?: "").takeIf { it.exists() }?.delete()
            }
          }
        }
        profiles[uid] = current.copy(photoUrl = null, updatedAt = Timestamp.now())
      }
}
