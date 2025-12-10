package com.android.joinme.model.event

import android.content.Context
import android.util.Log
import com.android.joinme.model.database.AppDatabase
import com.android.joinme.model.database.toEntity
import com.android.joinme.model.database.toEvent
import com.android.joinme.network.NetworkMonitor
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Cached implementation of EventsRepository. Implements offline-first read strategy and online-only
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
class EventsRepositoryCached(
    private val context: Context,
    private val firestoreRepo: EventsRepository,
    private val networkMonitor: NetworkMonitor
) : EventsRepository {

  private val database = AppDatabase.getDatabase(context)
  private val eventDao = database.eventDao()

  override fun getNewEventId(): String {
    return firestoreRepo.getNewEventId()
  }

  override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> {
    // Try to fetch from Firestore if online
    if (networkMonitor.isOnline()) {
      try {
        val events = firestoreRepo.getAllEvents(eventFilter)
        if (events.isNotEmpty()) {
          // Cache the fetched events
          eventDao.insertEvents(events.map { it.toEntity() })
        }
        return events
      } catch (e: Exception) {
        Log.w("EventsRepositoryCached", "Failed to fetch from Firestore, falling back to cache", e)
      }
    }

    // Offline or network error - apply filter to cached events
    val userId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("EventsRepositoryCached: User not logged in.")
    val allCachedEvents = eventDao.getAllEvents().map { it.toEvent() }

    return applyEventFilter(eventFilter, allCachedEvents, userId)
  }

  /**
   * Applies client-side filtering to cached events when offline. Mimics the behavior of Firestore
   * queries for each filter type.
   */
  private fun applyEventFilter(
      eventFilter: EventFilter,
      events: List<Event>,
      userId: String
  ): List<Event> {
    return when (eventFilter) {
      EventFilter.EVENTS_FOR_OVERVIEW_SCREEN -> {
        // Overview: events where user is a participant
        events.filter { event -> event.participants.contains(userId) }
      }
      EventFilter.EVENTS_FOR_HISTORY_SCREEN -> {
        // History: expired events where user is a participant, sorted by date descending
        events
            .filter { event -> event.participants.contains(userId) && event.isExpired() }
            .sortedByDescending { it.date.toDate().time }
      }
      EventFilter.EVENTS_FOR_SEARCH_SCREEN -> {
        // Search: upcoming public events where user is NOT a participant or owner
        events.filter { event ->
          event.visibility == EventVisibility.PUBLIC &&
              event.isUpcoming() &&
              !event.participants.contains(userId) &&
              event.ownerId != userId
        }
      }
      EventFilter.EVENTS_FOR_MAP_SCREEN -> {
        // Map: upcoming or active events with location
        events.filter { event ->
          (event.isUpcoming() || (event.isActive() && event.participants.contains(userId))) &&
              event.location != null
        }
      }
    }
  }

  override suspend fun getEvent(eventId: String): Event {
    val cached = eventDao.getEventById(eventId)?.toEvent()

    if (!networkMonitor.isOnline()) {
      return cached
          ?: throw OfflineException(
              "Cannot fetch event while offline and no cached version available")
    }

    return try {
      firestoreRepo.getEvent(eventId).also { event -> eventDao.insertEvent(event.toEntity()) }
    } catch (e: Exception) {
      cached ?: throw e
    }
  }

  override suspend fun addEvent(event: Event) {
    requireOnline()
    firestoreRepo.addEvent(event)
    // Cache the newly created event
    eventDao.insertEvent(event.toEntity())
  }

  override suspend fun editEvent(eventId: String, newValue: Event) {
    requireOnline()
    firestoreRepo.editEvent(eventId, newValue)
    // Update cache
    eventDao.insertEvent(newValue.toEntity())
  }

  override suspend fun deleteEvent(eventId: String) {
    requireOnline()
    firestoreRepo.deleteEvent(eventId)
    // Remove from cache
    eventDao.deleteEvent(eventId)
  }

  override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
    if (networkMonitor.isOnline()) {
      try {
        val events = firestoreRepo.getEventsByIds(eventIds)
        if (events.isNotEmpty()) {
          eventDao.insertEvents(events.map { it.toEntity() })
        }
        return events
      } catch (e: Exception) {
        Log.w("EventsRepositoryCached", "Failed to fetch from Firestore, falling back to cache", e)
      }
    }

    // Offline or error - get from cache
    return eventIds.mapNotNull { eventDao.getEventById(it)?.toEvent() }
  }

  override suspend fun getCommonEvents(userIds: List<String>): List<Event> {
    if (networkMonitor.isOnline()) {
      try {
        val events = firestoreRepo.getCommonEvents(userIds)
        if (events.isNotEmpty()) {
          eventDao.insertEvents(events.map { it.toEntity() })
        }
        return events
      } catch (e: Exception) {
        Log.w("EventsRepositoryCached", "Failed to fetch from Firestore, falling back to cache", e)
      }
    }

    // Offline - filter cached events locally
    // This is a best-effort implementation - may not be perfectly accurate
    return eventDao
        .getAllEvents()
        .map { it.toEvent() }
        .filter { event -> userIds.all { userId -> event.participants.contains(userId) } }
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

/** Exception thrown when attempting write operations while offline. */
class OfflineException(message: String) : Exception(message)
