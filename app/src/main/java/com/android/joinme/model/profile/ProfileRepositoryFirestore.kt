package com.android.joinme.model.profile

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

class ProfileRepositoryFirestore(private val db: FirebaseFirestore) : ProfileRepository {

  private val profilesCollection = db.collection(PROFILES_COLLECTION_PATH)

  override suspend fun getProfile(uid: String): Profile {
    val document = profilesCollection.document(uid).get().await()
    return documentToProfile(document)
        ?: throw NoSuchElementException(
            "ProfileRepositoryFirestore: Profile with UID $uid not found")
  }

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
      Log.e("ProfileRepositoryFirestore", "Error converting document to Profile", e)
      null
    }
  }
}
