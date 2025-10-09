package com.android.joinme.model.profile

/** Repository interface for managing user profiles. */
interface ProfileRepository {

  /**
   * Fetches a user profile by its unique identifier (UID).
   *
   * @param uid The unique identifier of the user profile.
   * @return The user profile if found, otherwise null.
   */
  suspend fun getProfile(uid: String): Profile?

  /**
   * Creates or updates a user profile.
   *
   * @param profile The user profile to create or update.
   */
  suspend fun createOrUpdateProfile(profile: Profile)

  /**
   * Deletes a user profile by its unique identifier (UID).
   *
   * @param uid The unique identifier of the user profile to delete.
   */
  suspend fun deleteProfile(uid: String)
}
