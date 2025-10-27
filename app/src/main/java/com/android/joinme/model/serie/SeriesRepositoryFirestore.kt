package com.android.joinme.model.serie

import android.util.Log
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Firestore collection path for series documents */
const val SERIES_COLLECTION_PATH = "series"

/**
 * Firestore-backed implementation of [SeriesRepository].
 *
 * Manages CRUD operations for [Serie] objects stored in Firebase Firestore. All operations are
 * performed asynchronously using Kotlin coroutines.
 *
 * @property db The FirebaseFirestore instance used for database operations
 */
class SeriesRepositoryFirestore(private val db: FirebaseFirestore) : SeriesRepository {
  /** Field name for the owner ID in Firestore documents */
  private val ownerAttributeName = "ownerId"

  /**
   * Generates and returns a new unique identifier for a Serie item.
   *
   * Uses Firestore's auto-generated document IDs to ensure uniqueness.
   *
   * @return A new unique Serie ID string
   */
  override fun getNewSerieId(): String {
    return db.collection(SERIES_COLLECTION_PATH).document().id
  }

  /**
   * Retrieves all Serie items owned by the currently authenticated user.
   *
   * @return A list of all Serie items owned by the current user
   * @throws Exception if the user is not logged in
   */
  override suspend fun getAllSeries(): List<Serie> {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("SeriesRepositoryFirestore: User not logged in.")

    val snapshot =
        db.collection(SERIES_COLLECTION_PATH)
            .whereEqualTo(ownerAttributeName, ownerId)
            .get()
            .await()

    return snapshot.mapNotNull { documentToSerie(it) }
  }

  /**
   * Retrieves a specific Serie item by its unique identifier from Firestore.
   *
   * @param serieId The unique identifier of the Serie item to retrieve
   * @return The Serie item with the specified identifier
   * @throws Exception if the Serie item is not found in Firestore
   */
  override suspend fun getSerie(serieId: String): Serie {
    val document = db.collection(SERIES_COLLECTION_PATH).document(serieId).get().await()
    return documentToSerie(document)
        ?: throw Exception("SeriesRepositoryFirestore: Serie not found ($serieId)")
  }

  /**
   * Adds a new Serie item to Firestore.
   *
   * @param serie The Serie item to add
   */
  override suspend fun addSerie(serie: Serie) {
    db.collection(SERIES_COLLECTION_PATH).document(serie.serieId).set(serie).await()
  }

  /**
   * Edits an existing Serie item in Firestore.
   *
   * Replaces the entire Serie document with the new value.
   *
   * @param serieId The unique identifier of the Serie item to edit
   * @param newValue The new value for the Serie item
   */
  override suspend fun editSerie(serieId: String, newValue: Serie) {
    db.collection(SERIES_COLLECTION_PATH).document(serieId).set(newValue).await()
  }

  /**
   * Deletes a Serie item from Firestore.
   *
   * @param serieId The unique identifier of the Serie item to delete
   */
  override suspend fun deleteSerie(serieId: String) {
    db.collection(SERIES_COLLECTION_PATH).document(serieId).delete().await()
  }

  /**
   * Converts a Firestore document to a [Serie] object.
   *
   * Extracts all Serie fields from the Firestore document and constructs a Serie instance. Returns
   * null if any required fields are missing or if conversion fails.
   *
   * @param document The Firestore document to convert
   * @return The [Serie] object, or null if conversion fails
   */
  private fun documentToSerie(document: DocumentSnapshot): Serie? {
    return try {
      val serieId = document.id
      val title = document.getString("title") ?: return null
      val description = document.getString("description") ?: ""
      val date = document.getTimestamp("date") ?: return null
      val maxParticipants = (document.getLong("maxParticipants") ?: 0L).toInt()
      val visibilityString = document.getString("visibility") ?: Visibility.PUBLIC.name
      val eventIds = document.get("eventIds") as? List<String> ?: emptyList()
      val ownerId = document.getString("ownerId") ?: return null

      Serie(
          serieId = serieId,
          title = title,
          description = description,
          date = date,
          maxParticipants = maxParticipants,
          visibility = Visibility.valueOf(visibilityString),
          eventIds = eventIds,
          ownerId = ownerId)
    } catch (e: Exception) {
      Log.e("SeriesRepositoryFirestore", "Error converting document to Serie", e)
      null
    }
  }
}
