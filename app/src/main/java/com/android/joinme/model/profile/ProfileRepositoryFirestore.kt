package com.android.joinme.model.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.joinme.model.chat.ConversationCleanupService
import com.android.joinme.model.utils.ImageProcessor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

const val PROFILES_COLLECTION_PATH = "profiles"
const val FOLLOWS_COLLECTION_PATH = "follows"

private const val F_USERNAME = "username"
private const val F_EMAIL = "email"
private const val F_PHOTO_URL = "photoUrl"
private const val F_COUNTRY = "country"
private const val F_BIO = "bio"
private const val F_INTERESTS = "interests"
private const val F_DOB = "dateOfBirth"
private const val F_CREATED_AT = "createdAt"
private const val F_UPDATED_AT = "updatedAt"
private const val F_FCM_TOKEN = "fcmToken"
private const val F_EVENTS_JOINED_COUNT = "eventsJoinedCount"
private const val F_FOLLOWERS_COUNT = "followersCount"
private const val F_FOLLOWING_COUNT = "followingCount"

// Follow relationship fields
private const val F_FOLLOW_ID = "id"
private const val F_FOLLOWER_ID = "followerId"
private const val F_FOLLOWED_ID = "followedId"

/**
 * Firestore implementation of [ProfileRepository] that manages user profile data in Firebase
 * Firestore and profile photos in Firebase Storage.
 *
 * This repository provides CRUD operations for user profiles, storing them in a Firestore
 * collection. Each profile document is keyed by the user's UID. All operations are asynchronous and
 * use Kotlin coroutines for clean, non-blocking execution.
 *
 * Profile photos are stored in Firebase Storage at: users/{uid}/profile.jpg
 *
 * @param db The Firestore database instance.
 * @param storage The Firebase Storage instance for photo uploads.
 */
class ProfileRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ProfileRepository {

  private val profilesCollection = db.collection(PROFILES_COLLECTION_PATH)
  private val followsCollection = db.collection(FOLLOWS_COLLECTION_PATH)

  companion object {
    private const val TAG = "ProfileRepositoryFirestore"
    private const val USERS_STORAGE_PATH = "users"
    private const val PROFILE_PHOTO_NAME = "profile.jpg"
  }

  /**
   * Retrieves a user profile by UID from Firestore. Throws [NoSuchElementException] if the profile
   * does not exist.
   */
  override suspend fun getProfile(uid: String): Profile {
    val document = profilesCollection.document(uid).get().await()
    return documentToProfile(document)
        ?: throw NoSuchElementException(
            "ProfileRepositoryFirestore: Profile with UID $uid not found")
  }

  /**
   * Retrieves multiple user profiles by their UIDs from Firestore. Returns null if any profile is
   * not found or if an error occurs.
   */
  override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? {
    if (uids.isEmpty()) return emptyList()
    return try {
      uids.mapNotNull { uid ->
        try {
          getProfile(uid)
        } catch (_: NoSuchElementException) {
          null
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error fetching profiles", e)
      null
    }
  }
  /**
   * Creates or updates a user profile in Firestore. If the profile document already exists, it
   * updates the existing fields; otherwise, it creates a new document with the provided data.
   */
  override suspend fun createOrUpdateProfile(profile: Profile) {
    val docRef = profilesCollection.document(profile.uid)
    val snapshot = docRef.get().await()

    val base =
        mapOf(
            F_USERNAME to profile.username,
            F_PHOTO_URL to profile.photoUrl,
            F_COUNTRY to profile.country,
            F_BIO to profile.bio,
            F_INTERESTS to profile.interests,
            F_DOB to profile.dateOfBirth,
            F_FCM_TOKEN to profile.fcmToken,
            F_EVENTS_JOINED_COUNT to profile.eventsJoinedCount)

    val data =
        if (snapshot.exists()) {
          base + mapOf(F_UPDATED_AT to FieldValue.serverTimestamp())
        } else {
          base +
              mapOf(
                  F_EMAIL to profile.email, // set once
                  F_CREATED_AT to FieldValue.serverTimestamp(),
                  F_UPDATED_AT to FieldValue.serverTimestamp())
        }

    docRef.set(data, SetOptions.merge()).await()
  }

  override suspend fun deleteProfile(uid: String) {
    profilesCollection.document(uid).delete().await()

    // Delete all direct message conversations involving this user
    ConversationCleanupService.cleanupUserConversations(userId = uid)
  }

  /**
   * Uploads a profile photo for the given user UID to Firebase Storage, processes the image
   * (compression, orientation), and updates the user's profile document in Firestore with the new
   * photo URL. Returns the download URL of the uploaded photo.
   */
  override suspend fun uploadProfilePhoto(context: Context, uid: String, imageUri: Uri): String {
    try {

      // Step 1: Process the image (compress, fix orientation)
      // Create ImageProcessor on-demand with the provided context
      val imageProcessor = ImageProcessor(context)
      val processedBytes = imageProcessor.processImage(imageUri)

      // Step 2: Upload to Firebase Storage
      val storageRef =
          storage.reference.child(USERS_STORAGE_PATH).child(uid).child(PROFILE_PHOTO_NAME)

      storageRef.putBytes(processedBytes).await()

      // Step 3: Get the download URL
      val downloadUrl = storageRef.downloadUrl.await().toString()

      // Step 4: Update Firestore profile with new photoUrl
      val docRef = profilesCollection.document(uid)
      docRef
          .set(
              mapOf(F_PHOTO_URL to downloadUrl, F_UPDATED_AT to FieldValue.serverTimestamp()),
              SetOptions.merge())
          .await()

      return downloadUrl
    } catch (e: Exception) {
      Log.e(TAG, "Error uploading profile photo for user $uid", e)
      throw Exception("Failed to upload profile photo: ${e.message}", e)
    }
  }

  /**
   * Deletes the profile photo for the given user UID from Firebase Storage and clears the photoUrl
   * field in Firestore.
   */
  override suspend fun deleteProfilePhoto(uid: String) {
    try {

      // Step 1: Delete from Storage
      val storageRef =
          storage.reference.child(USERS_STORAGE_PATH).child(uid).child(PROFILE_PHOTO_NAME)

      try {
        storageRef.delete().await()
      } catch (e: Exception) {
        // File might not exist, log but continue
        Log.w(TAG, "Photo file not found in Storage, continuing to clear Firestore field", e)
      }

      // Step 2: Clear photoUrl in Firestore
      val docRef = profilesCollection.document(uid)
      docRef
          .set(
              mapOf(F_PHOTO_URL to null, F_UPDATED_AT to FieldValue.serverTimestamp()),
              SetOptions.merge())
          .await()
    } catch (e: Exception) {
      Log.e(TAG, "Error deleting profile photo for user $uid", e)
      throw Exception("Failed to delete profile photo: ${e.message}", e)
    }
  }

  /**
   * Converts a Firestore DocumentSnapshot to a Profile object. Returns null if required fields are
   * missing or if the document doesn't exist.
   */
  private fun documentToProfile(document: DocumentSnapshot): Profile? {
    return try {
      if (!document.exists()) return null

      // Required fields
      val username = document.getString(F_USERNAME)?.trim().orEmpty()
      val email = document.getString(F_EMAIL)?.trim().orEmpty()
      if (username.isEmpty() || email.isEmpty()) return null

      // Optional strings
      val photoUrl = document.getString(F_PHOTO_URL)?.takeIf { it.isNotBlank() }
      val country = document.getString(F_COUNTRY)?.takeIf { it.isNotBlank() }
      val bio = document.getString(F_BIO)?.takeIf { it.isNotBlank() }
      val dateOfBirth = document.getString(F_DOB)?.takeIf { it.isNotBlank() }
      val fcmToken = document.getString(F_FCM_TOKEN)?.takeIf { it.isNotBlank() }

      // Optional list<string>, guard against mixed types
      val interests: List<String> =
          (document.get(F_INTERESTS) as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

      // Timestamps
      val createdAt: Timestamp? = document.getTimestamp(F_CREATED_AT)
      val updatedAt: Timestamp? = document.getTimestamp(F_UPDATED_AT)

      // Counters
      val eventsJoinedCount = document.getLong(F_EVENTS_JOINED_COUNT)?.toInt() ?: 0
      val followersCount = document.getLong(F_FOLLOWERS_COUNT)?.toInt() ?: 0
      val followingCount = document.getLong(F_FOLLOWING_COUNT)?.toInt() ?: 0

      return Profile(
          uid = document.id,
          username = username,
          email = email,
          photoUrl = photoUrl,
          country = country,
          bio = bio,
          interests = interests,
          dateOfBirth = dateOfBirth,
          createdAt = createdAt,
          updatedAt = updatedAt,
          fcmToken = fcmToken,
          eventsJoinedCount = eventsJoinedCount,
          followersCount = followersCount,
          followingCount = followingCount)
    } catch (e: Exception) {
      Log.e(TAG, "Error converting document to Profile", e)
      null
    }
  }

  override suspend fun followUser(followerId: String, followedId: String) {
    if (followerId == followedId) {
      throw Exception("Cannot follow yourself")
    }

    // Check if already following
    if (isFollowing(followerId, followedId)) {
      throw Exception("Already following this user")
    }

    // Use a batch write for atomic operation
    val batch = db.batch()

    // 1. Create the follow relationship
    val followDoc = followsCollection.document()
    val follow =
        mapOf(
            F_FOLLOW_ID to followDoc.id,
            F_FOLLOWER_ID to followerId,
            F_FOLLOWED_ID to followedId,
        )
    batch.set(followDoc, follow)

    // 2. Increment follower's following count
    val followerRef = profilesCollection.document(followerId)
    batch.update(followerRef, F_FOLLOWING_COUNT, FieldValue.increment(1))

    // 3. Increment followed user's followers count
    val followedRef = profilesCollection.document(followedId)
    batch.update(followedRef, F_FOLLOWERS_COUNT, FieldValue.increment(1))

    // Commit all changes atomically
    batch.commit().await()
  }

  override suspend fun unfollowUser(followerId: String, followedId: String) {
    // Find the follow relationship
    val snapshot =
        followsCollection
            .whereEqualTo(F_FOLLOWER_ID, followerId)
            .whereEqualTo(F_FOLLOWED_ID, followedId)
            .get()
            .await()

    if (snapshot.isEmpty) {
      throw Exception("Not currently following this user")
    }

    // Use a batch write for atomic operation
    val batch = db.batch()

    // 1. Delete the follow relationship
    val followDoc = snapshot.documents.first()
    batch.delete(followDoc.reference)

    // 2. Decrement follower's following count
    val followerRef = profilesCollection.document(followerId)
    batch.update(followerRef, F_FOLLOWING_COUNT, FieldValue.increment(-1))

    // 3. Decrement followed user's followers count
    val followedRef = profilesCollection.document(followedId)
    batch.update(followedRef, F_FOLLOWERS_COUNT, FieldValue.increment(-1))

    // Commit all changes atomically
    batch.commit().await()
  }

  override suspend fun isFollowing(followerId: String, followedId: String): Boolean {
    val snapshot =
        followsCollection
            .whereEqualTo(F_FOLLOWER_ID, followerId)
            .whereEqualTo(F_FOLLOWED_ID, followedId)
            .limit(1)
            .get()
            .await()

    return !snapshot.isEmpty
  }

  override suspend fun getFollowing(userId: String, limit: Int): List<Profile> {
    // Get all follow relationships where this user is the follower
    val followsSnapshot =
        followsCollection
            .whereEqualTo(F_FOLLOWER_ID, userId)
            .orderBy(
                FieldPath.documentId(), com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

    val followedIds = followsSnapshot.documents.mapNotNull { it.getString(F_FOLLOWED_ID) }

    if (followedIds.isEmpty()) return emptyList()

    // Fetch profiles in batches (Firestore 'in' query limit is 10)
    return followedIds.chunked(10).flatMap { chunk ->
      profilesCollection.whereIn(FieldPath.documentId(), chunk).get().await().mapNotNull {
        documentToProfile(it)
      }
    }
  }

  override suspend fun getFollowers(userId: String, limit: Int): List<Profile> {
    // Get all follow relationships where this user is being followed
    val followsSnapshot =
        followsCollection
            .whereEqualTo(F_FOLLOWED_ID, userId)
            .orderBy(
                FieldPath.documentId(), com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

    val followerIds = followsSnapshot.documents.mapNotNull { it.getString(F_FOLLOWER_ID) }

    if (followerIds.isEmpty()) return emptyList()

    // Fetch profiles in batches (Firestore 'in' query limit is 10)
    return followerIds.chunked(10).flatMap { chunk ->
      profilesCollection.whereIn(FieldPath.documentId(), chunk).get().await().mapNotNull {
        documentToProfile(it)
      }
    }
  }

  override suspend fun getMutualFollowing(userId1: String, userId2: String): List<Profile> {
    // Get users that userId1 follows
    val user1Follows =
        followsCollection
            .whereEqualTo(F_FOLLOWER_ID, userId1)
            .get()
            .await()
            .mapNotNull { it.getString(F_FOLLOWED_ID) }
            .toSet()

    // Get users that userId2 follows
    val user2Follows =
        followsCollection
            .whereEqualTo(F_FOLLOWER_ID, userId2)
            .get()
            .await()
            .mapNotNull { it.getString(F_FOLLOWED_ID) }
            .toSet()

    // Find intersection
    val mutualIds = user1Follows.intersect(user2Follows).toList()

    if (mutualIds.isEmpty()) return emptyList()

    // Fetch profiles in batches
    return mutualIds.chunked(10).flatMap { chunk ->
      profilesCollection.whereIn(FieldPath.documentId(), chunk).get().await().mapNotNull {
        documentToProfile(it)
      }
    }
  }
}
