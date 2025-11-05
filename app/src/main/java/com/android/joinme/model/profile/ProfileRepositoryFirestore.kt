package com.android.joinme.model.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.joinme.model.utils.ImageProcessor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

const val PROFILES_COLLECTION_PATH = "profiles"

private const val F_USERNAME = "username"
private const val F_EMAIL = "email"
private const val F_PHOTO_URL = "photoUrl"
private const val F_COUNTRY = "country"
private const val F_BIO = "bio"
private const val F_INTERESTS = "interests"
private const val F_DOB = "dateOfBirth"
private const val F_CREATED_AT = "createdAt"
private const val F_UPDATED_AT = "updatedAt"

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
class ProfileRepositoryFirestore(db: FirebaseFirestore, private val storage: FirebaseStorage) :
    ProfileRepository {

  private val profilesCollection = db.collection(PROFILES_COLLECTION_PATH)

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
            F_DOB to profile.dateOfBirth)

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
  }

  /**
   * Uploads a profile photo for the given user UID to Firebase Storage, processes the image
   * (compression, orientation), and updates the user's profile document in Firestore with the new
   * photo URL. Returns the download URL of the uploaded photo.
   */
  override suspend fun uploadProfilePhoto(context: Context, uid: String, imageUri: Uri): String {
    try {
      Log.d(TAG, "Starting photo upload for user $uid from URI: $imageUri")

      // Step 1: Process the image (compress, fix orientation)
      // Create ImageProcessor on-demand with the provided context
      val imageProcessor = ImageProcessor(context)
      val processedBytes = imageProcessor.processImage(imageUri)
      Log.d(TAG, "Image processed, size: ${processedBytes.size} bytes")

      // Step 2: Upload to Firebase Storage
      val storageRef =
          storage.reference.child(USERS_STORAGE_PATH).child(uid).child(PROFILE_PHOTO_NAME)

      val uploadTask = storageRef.putBytes(processedBytes).await()
      Log.d(TAG, "Upload completed: ${uploadTask.metadata?.path}")

      // Step 3: Get the download URL
      val downloadUrl = storageRef.downloadUrl.await().toString()
      Log.d(TAG, "Download URL retrieved: $downloadUrl")

      // Step 4: Update Firestore profile with new photoUrl
      val docRef = profilesCollection.document(uid)
      docRef
          .update(mapOf(F_PHOTO_URL to downloadUrl, F_UPDATED_AT to FieldValue.serverTimestamp()))
          .await()
      Log.d(TAG, "Profile updated with new photo URL")

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
      Log.d(TAG, "Deleting profile photo for user $uid")

      // Step 1: Delete from Storage
      val storageRef =
          storage.reference.child(USERS_STORAGE_PATH).child(uid).child(PROFILE_PHOTO_NAME)

      try {
        storageRef.delete().await()
        Log.d(TAG, "Profile photo deleted from Storage")
      } catch (e: Exception) {
        // File might not exist, log but continue
        Log.w(TAG, "Photo file not found in Storage, continuing to clear Firestore field", e)
      }

      // Step 2: Clear photoUrl in Firestore
      val docRef = profilesCollection.document(uid)
      docRef
          .update(mapOf(F_PHOTO_URL to null, F_UPDATED_AT to FieldValue.serverTimestamp()))
          .await()
      Log.d(TAG, "Profile photoUrl cleared in Firestore")
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

      // Optional list<string>, guard against mixed types
      val interests: List<String> =
          (document.get(F_INTERESTS) as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

      // Timestamps
      val createdAt: Timestamp? = document.getTimestamp(F_CREATED_AT)
      val updatedAt: Timestamp? = document.getTimestamp(F_UPDATED_AT)

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
          updatedAt = updatedAt)
    } catch (e: Exception) {
      Log.e(TAG, "Error converting document to Profile", e)
      null
    }
  }
}
