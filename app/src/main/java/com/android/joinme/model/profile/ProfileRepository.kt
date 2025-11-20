package com.android.joinme.model.profile

import android.content.Context
import android.net.Uri

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
   * Fetches multiple user profiles by their unique identifiers (UIDs).
   *
   * @param uids The list of unique identifiers of the user profiles.
   * @return The list of user profiles if all are found, otherwise null.
   */
  suspend fun getProfilesByIds(uids: List<String>): List<Profile>?

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

  /**
   * Uploads a profile photo for the given user and updates their profile.
   *
   * The photo will be stored at a deterministic path (users/{uid}/profile.jpg) to ensure
   * idempotency - subsequent uploads will replace the previous photo. After successful upload, the
   * profile's photoUrl field is automatically updated.
   *
   * @param context Android context for reading and processing the image
   * @param uid The unique identifier of the user
   * @param imageUri The local URI of the image to upload
   * @return The download URL of the uploaded image
   * @throws Exception if upload fails (network error, permissions, etc.)
   */
  suspend fun uploadProfilePhoto(context: Context, uid: String, imageUri: Uri): String

  /**
   * Deletes the profile photo for the given user and clears the photoUrl field.
   *
   * @param uid The unique identifier of the user
   * @throws Exception if deletion fails
   */
  suspend fun deleteProfilePhoto(uid: String)
}
