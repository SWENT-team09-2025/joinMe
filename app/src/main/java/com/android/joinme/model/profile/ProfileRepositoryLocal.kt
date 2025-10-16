package com.android.joinme.model.profile

import com.google.firebase.Timestamp

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
            updatedAt = now)
    profiles[testProfile.uid] = testProfile
  }

  override suspend fun getProfile(uid: String): Profile? {
    return profiles[uid]
  }

  override suspend fun createOrUpdateProfile(profile: Profile) {
    profiles[profile.uid] = profile
  }

  override suspend fun deleteProfile(uid: String) {
    profiles.remove(uid)
  }
}
