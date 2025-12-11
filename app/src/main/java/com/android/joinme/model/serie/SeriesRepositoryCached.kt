package com.android.joinme.model.serie

import android.content.Context
import android.util.Log
import com.android.joinme.model.database.AppDatabase
import com.android.joinme.model.database.toEntity
import com.android.joinme.model.database.toSerie
import com.android.joinme.model.event.OfflineException
import com.android.joinme.model.utils.Visibility
import com.android.joinme.network.NetworkMonitor
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.withTimeout

/**
 * Cached implementation of SeriesRepository. Implements offline-first read strategy and online-only
 * write strategy.
 *
 * Read strategy:
 * 1. Try to fetch from Firestore if online (and cache the result)
 * 2. If offline or fetch fails, return cached data from Room
 *
 * Write strategy:
 * 1. Require network connectivity
 * 2. Write to Firestore
 * 3. Update local cache
 *
 * @param context Application context for database access
 * @param firestoreRepo The Firestore repository implementation to delegate online operations to
 * @param networkMonitor Network connectivity monitor
 */
class SeriesRepositoryCached(
    private val context: Context,
    private val firestoreRepo: SeriesRepository,
    private val networkMonitor: NetworkMonitor
) : SeriesRepository {
  private var firestoreErrorMsg = "Failed to fetch from Firestore, falling back to cache"
  private val database = AppDatabase.getDatabase(context)
  private val serieDao = database.serieDao()

  companion object {
    /** Timeout for Firestore operations in milliseconds (3 seconds) */
    private const val FIRESTORE_TIMEOUT_MS = 3000L
  }

  override fun getNewSerieId(): String = firestoreRepo.getNewSerieId() // NOSONAR

  override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> {
    // Try to fetch from Firestore if online
    if (networkMonitor.isOnline()) {
      try {
        val series = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getAllSeries(serieFilter) }
        // Clear cache and update with fresh data to remove any deleted series
        // For OVERVIEW and HISTORY filters, this represents the complete set of user's series
        if (serieFilter == SerieFilter.SERIES_FOR_OVERVIEW_SCREEN ||
            serieFilter == SerieFilter.SERIES_FOR_HISTORY_SCREEN) {
          serieDao.deleteAllSeries()
        }
        // Always update cache, even if empty, to ensure consistency
        if (series.isNotEmpty()) {
          serieDao.insertSeries(series.map { it.toEntity() })
        }
        return series
      } catch (e: Exception) {
        Log.w("SeriesRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline or network error - apply filter to cached series
    val userId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("SeriesRepositoryCached: User not logged in.")
    val allCachedSeries = serieDao.getAllSeries().map { it.toSerie() }

    return applySerieFilter(serieFilter, allCachedSeries, userId)
  }

  /**
   * Applies client-side filtering to cached series when offline. Mimics the behavior of Firestore
   * queries for each filter type.
   */
  private fun applySerieFilter(
      serieFilter: SerieFilter,
      series: List<Serie>,
      userId: String
  ): List<Serie> {
    return when (serieFilter) {
      SerieFilter.SERIES_FOR_OVERVIEW_SCREEN -> {
        // Overview: series where user is a participant
        series.filter { serie -> serie.participants.contains(userId) }
      }
      SerieFilter.SERIES_FOR_HISTORY_SCREEN -> {
        // History: expired series where user is a participant, sorted by date descending
        series
            .filter { serie -> serie.participants.contains(userId) && serie.isExpired() }
            .sortedByDescending { it.date.toDate().time }
      }
      SerieFilter.SERIES_FOR_SEARCH_SCREEN -> {
        // Search: upcoming public series where user is NOT a participant or owner
        series.filter { serie ->
          serie.visibility == Visibility.PUBLIC &&
              serie.isUpcoming() &&
              !serie.participants.contains(userId) &&
              serie.ownerId != userId
        }
      }
      SerieFilter.SERIES_FOR_MAP_SCREEN -> {
        // Map: upcoming or active series
        series.filter { serie ->
          serie.isUpcoming() || (serie.isActive() && serie.participants.contains(userId))
        }
      }
    }
  }

  override suspend fun getSerie(serieId: String): Serie {
    // Try to fetch from Firestore if online
    if (networkMonitor.isOnline()) {
      try {
        val serie = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getSerie(serieId) }
        // Delete from cache first to handle any staleness, then insert fresh data
        serieDao.deleteSerie(serieId)
        serieDao.insertSerie(serie.toEntity())
        return serie
      } catch (e: Exception) {
        Log.w("SeriesRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline or network error - try cached version
    val cached = serieDao.getSerieById(serieId)?.toSerie()
    return cached
        ?: throw OfflineException(
            "Cannot fetch serie while offline and no cached version available")
  }

  override suspend fun addSerie(serie: Serie) {
    requireOnline()
    firestoreRepo.addSerie(serie)
    // Cache the newly created serie
    serieDao.insertSerie(serie.toEntity())
  }

  override suspend fun editSerie(serieId: String, newValue: Serie) {
    requireOnline()
    firestoreRepo.editSerie(serieId, newValue)
    // Update cache
    serieDao.insertSerie(newValue.toEntity())
  }

  override suspend fun deleteSerie(serieId: String) {
    requireOnline()
    firestoreRepo.deleteSerie(serieId)
    // Remove from cache
    serieDao.deleteSerie(serieId)
  }

  override suspend fun getSeriesByIds(seriesIds: List<String>): List<Serie> {
    if (networkMonitor.isOnline()) {
      try {
        val series = withTimeout(FIRESTORE_TIMEOUT_MS) { firestoreRepo.getSeriesByIds(seriesIds) }
        // Delete requested IDs from cache first to handle deleted series
        seriesIds.forEach { serieDao.deleteSerie(it) }
        // Always update cache, even if empty, to ensure consistency
        if (series.isNotEmpty()) {
          serieDao.insertSeries(series.map { it.toEntity() })
        }
        return series
      } catch (e: Exception) {
        Log.w("SeriesRepositoryCached", firestoreErrorMsg, e)
      }
    }

    // Offline or error - get from cache
    return seriesIds.mapNotNull { serieDao.getSerieById(it)?.toSerie() }
  }

  /**
   * Checks if device is online, throws OfflineException if not.
   *
   * @throws OfflineException if device is offline
   */
  private fun requireOnline() {
    if (!networkMonitor.isOnline()) {
      throw OfflineException("This operation requires an internet connection")
    }
  }
}
