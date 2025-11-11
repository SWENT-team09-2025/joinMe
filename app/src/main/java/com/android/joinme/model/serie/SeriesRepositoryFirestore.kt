package com.android.joinme.model.serie

import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.isActive
import com.android.joinme.model.event.isExpired
import com.android.joinme.model.event.isUpcoming
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/** Firestore collection path for series documents */
const val SERIES_COLLECTION_PATH = "series"

/**
 * Filter criteria for retrieving series from Firestore based on the target screen.
 *
 * Determines which series to fetch and how to filter them according to the UI context.
 */
enum class SerieFilter {
  /**
   * Filter for the overview screen.
   *
   * Retrieves all series where the current user is a participant.
   */
  SERIES_FOR_OVERVIEW_SCREEN,

  /**
   * Filter for the history screen.
   *
   * Retrieves all series where the current user is a participant. TODO: Should filter to show only
   * expired series.
   */
  SERIES_FOR_HISTORY_SCREEN,

  /**
   * Filter for the search screen.
   *
   * Retrieves all series. TODO: Should filter to show only public series that are upcoming, where
   * the current user is neither a participant nor the owner.
   */
  SERIES_FOR_SEARCH_SCREEN,
  SERIES_FOR_MAP_SCREEN
}
/**
 * Firestore-backed implementation of [SeriesRepository].
 *
 * Manages CRUD operations for [Serie] objects stored in Firebase Firestore. All operations are
 * performed asynchronously using Kotlin coroutines.
 *
 * @property db The FirebaseFirestore instance used for database operations
 */
class SeriesRepositoryFirestore(private val db: FirebaseFirestore) : SeriesRepository {
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
   * Retrieves all Serie items with the corresponding filter from Firestore specified by the
   * viewModel requested items.
   *
   * @return A list of all Serie items owned by the current user
   * @throws Exception if the user is not logged in
   */
  override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> {
    val userId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("SeriesRepositoryFirestore: User not logged in.")

    val series = databaseFetching(serieFilter, userId)

    return clientSideProcessing(serieFilter, series, userId)
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
      val participants = document.get("participants") as? List<String> ?: emptyList()
      val maxParticipants = (document.getLong("maxParticipants") ?: 0L).toInt()
      val visibilityString = document.getString("visibility") ?: Visibility.PUBLIC.name
      val eventIds = document.get("eventIds") as? List<String> ?: emptyList()
      val ownerId = document.getString("ownerId") ?: return null
      val lastEventEndTime = document.getTimestamp("lastEventEndTime") ?: date

      Serie(
          serieId = serieId,
          title = title,
          description = description,
          date = date,
          participants = participants,
          maxParticipants = maxParticipants,
          visibility = Visibility.valueOf(visibilityString),
          eventIds = eventIds,
          ownerId = ownerId,
          lastEventEndTime = lastEventEndTime)
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Applies client-side filtering and sorting to series based on the specified filter.
   *
   * This method performs in-memory filtering that cannot be efficiently done at the database level,
   * such as filtering by series state (upcoming, active, expired) and excluding events based on
   * user relationships.
   *
   * @param serieFilter The type of filter to apply.
   * @param series The list of series to filter and sort.
   * @param userId The current user's ID.
   * @return A filtered and sorted list of series according to the specified filter criteria.
   */
  private fun clientSideProcessing(
      serieFilter: SerieFilter,
      series: List<Serie>,
      userId: String
  ): List<Serie> {
    return when (serieFilter) {
      SerieFilter.SERIES_FOR_OVERVIEW_SCREEN -> {
        series
      }
      SerieFilter.SERIES_FOR_HISTORY_SCREEN -> {
        series.filter { serie -> serie.isExpired() }.sortedByDescending { it.date.toDate().time }
      }
      SerieFilter.SERIES_FOR_SEARCH_SCREEN -> {
        // ToDo review search algorithm for series because type are on events not on series
        series.filter { serie ->
          serie.isUpcoming() && !serie.participants.contains(userId) && serie.ownerId != userId
        }
      }
      SerieFilter.SERIES_FOR_MAP_SCREEN -> {
        series.filter { serie ->
          (serie.isUpcoming() || (serie.isActive() && serie.participants.contains(userId)))
        }
      }
    }
  }

  /**
   * Fetches series from Firestore with database-level filtering applied.
   *
   * This method performs Firestore queries optimized for each filter type.
   *
   * @param serieFilter The type of filter determining which Firestore queries to execute.
   * @param userId The current user's ID for filtering series.
   * @return A list of events retrieved from Firestore, converted from document snapshots.
   */
  private suspend fun databaseFetching(serieFilter: SerieFilter, userId: String): List<Serie> {
    return when (serieFilter) {
      SerieFilter.SERIES_FOR_OVERVIEW_SCREEN,
      SerieFilter.SERIES_FOR_HISTORY_SCREEN -> {
        val snapshot =
            db.collection(SERIES_COLLECTION_PATH)
                .whereArrayContains("participants", userId)
                .get()
                .await()
        snapshot.mapNotNull { documentToSerie(it) }
      }
      SerieFilter.SERIES_FOR_SEARCH_SCREEN -> {
        val snapshot =
            db.collection(SERIES_COLLECTION_PATH)
                .whereEqualTo("visibility", EventVisibility.PUBLIC.name)
                .get()
                .await()
        snapshot.mapNotNull { documentToSerie(it) }
      }
      SerieFilter.SERIES_FOR_MAP_SCREEN -> {
        // Execute both Firestore queries in parallel for better performance
        coroutineScope {
          val participantSnapshotDeferred = async {
            db.collection(SERIES_COLLECTION_PATH)
                .whereArrayContains("participants", userId)
                .get()
                .await()
          }
          val publicSnapshotDeferred = async {
            db.collection(SERIES_COLLECTION_PATH)
                .whereEqualTo("visibility", EventVisibility.PUBLIC.name)
                .get()
                .await()
          }

          val participantEvents =
              participantSnapshotDeferred.await().mapNotNull { documentToSerie(it) }
          val publicEvents = publicSnapshotDeferred.await().mapNotNull { documentToSerie(it) }

          (participantEvents + publicEvents).distinctBy { it.serieId }
        }
      }
    }
  }
}
