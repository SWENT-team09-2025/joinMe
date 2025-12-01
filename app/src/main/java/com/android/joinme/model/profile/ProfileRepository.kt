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

  /**
   * Creates a follow relationship between two users.
   *
   * This method atomically:
   * 1. Creates a follow document in the "follows" collection
   * 2. Increments the follower's followingCount
   * 3. Increments the followed user's followersCount
   *
   * @param followerId The user ID of the person doing the following
   * @param followedId The user ID of the person being followed
   * @throws Exception if users are the same, already following, or operation fails
   */
  suspend fun followUser(followerId: String, followedId: String)

  /**
   * Removes a follow relationship between two users.
   *
   * This method atomically:
   * 1. Deletes the follow document from the "follows" collection
   * 2. Decrements the follower's followingCount
   * 3. Decrements the followed user's followersCount
   *
   * @param followerId The user ID of the person doing the unfollowing
   * @param followedId The user ID of the person being unfollowed
   * @throws Exception if not currently following or operation fails
   */
  suspend fun unfollowUser(followerId: String, followedId: String)

  /**
   * Checks if one user follows another.
   *
   * @param followerId The potential follower's user ID
   * @param followedId The potential followee's user ID
   * @return true if followerId follows followedId, false otherwise
   */
  suspend fun isFollowing(followerId: String, followedId: String): Boolean

  /**
   * Retrieves a list of profiles that the specified user follows.
   *
   * Results are ordered by follow ID (descending), which gives a rough chronological order since
   * Firestore auto-generated IDs are time-sortable.
   *
   * @param userId The user ID whose following list to retrieve
   * @param limit Maximum number of results (default 50)
   * @return List of Profile objects for users being followed
   */
  suspend fun getFollowing(userId: String, limit: Int = 50): List<Profile>

  /**
   * Retrieves a list of profiles that follow the specified user.
   *
   * Results are ordered by follow ID (descending), which gives a rough chronological order since
   * Firestore auto-generated IDs are time-sortable.
   *
   * @param userId The user ID whose followers to retrieve
   * @param limit Maximum number of results (default 50)
   * @return List of Profile objects for followers
   */
  suspend fun getFollowers(userId: String, limit: Int = 50): List<Profile>

  /**
   * Retrieves profiles that both specified users follow (mutual connections).
   *
   * This is useful for showing "Followed by users you follow" or similar social features.
   *
   * @param userId1 First user ID
   * @param userId2 Second user ID
   * @return List of Profile objects for users followed by both userId1 and userId2
   */
  suspend fun getMutualFollowing(userId1: String, userId2: String): List<Profile>
}
