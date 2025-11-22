package com.android.joinme.model.event

import android.content.Context
import com.android.joinme.model.map.Location
import com.android.joinme.model.notification.NotificationScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.get
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

const val EVENTS_COLLECTION_PATH = "events"

/**
 * Filter criteria for retrieving events from Firestore based on the target screen.
 *
 * Determines which events to fetch and how to filter them according to the UI context.
 */
enum class EventFilter {
  /**
   * Filter for the overview screen.
   *
   * Retrieves all events where the current user is a participant.
   */
  EVENTS_FOR_OVERVIEW_SCREEN,

  /**
   * Filter for the history screen.
   *
   * Retrieves all events where the current user is a participant, then filters to show only expired
   * events, sorted by date (most recent first).
   */
  EVENTS_FOR_HISTORY_SCREEN,

  /**
   * Filter for the search screen.
   *
   * Retrieves public events that are upcoming, where the current user is neither a participant nor
   * the owner.
   */
  EVENTS_FOR_SEARCH_SCREEN,

  /**
   * Filter for the map screen.
   *
   * Retrieves all upcoming events with a location:
   * - Events owned by the current user
   * - Events joined by the current user
   * - Public events not joined by the current user
   */
  EVENTS_FOR_MAP_SCREEN
}
/**
 * Firestore-backed implementation of [EventsRepository]. Manages CRUD operations for [Event]
 * objects.
 */
class EventsRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val context: Context? = null
) : EventsRepository {

  override fun getNewEventId(): String {
    return db.collection(EVENTS_COLLECTION_PATH).document().id
  }

  override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> {
    val userId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("EventsRepositoryFirestore: User not logged in.")

    val events = databaseFetching(eventFilter, userId)

    return clientSideProcessing(eventFilter, events, userId)
  }

  override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
    if (eventIds.isEmpty()) return emptyList()
    if (eventIds.size > 30) throw Exception("EventsRepositoryFirestore: Too many event IDs")

    val snapshot = db.collection(EVENTS_COLLECTION_PATH).whereIn("eventId", eventIds).get().await()

    return snapshot.mapNotNull { documentToEvent(it) }.sortedBy { it.date.toDate().time }
  }

  override suspend fun getEvent(eventId: String): Event {
    val document = db.collection(EVENTS_COLLECTION_PATH).document(eventId).get().await()
    return documentToEvent(document)
        ?: throw Exception("EventsRepositoryFirestore: Event not found ($eventId)")
  }

  override suspend fun addEvent(event: Event) {
    val ownerId = event.ownerId

    val updatedEvent = event.copy(participants = (event.participants + ownerId).distinct())

    db.collection(EVENTS_COLLECTION_PATH).document(updatedEvent.eventId).set(updatedEvent).await()

    // Schedule notification for upcoming event
    context?.let {
      if (updatedEvent.isUpcoming()) {
        NotificationScheduler.scheduleEventNotification(it, updatedEvent)
      }
    }
  }

  override suspend fun editEvent(eventId: String, newValue: Event) {
    db.collection(EVENTS_COLLECTION_PATH).document(eventId).set(newValue).await()

    // Cancel old notification and reschedule only if current user is a participant
    context?.let {
      NotificationScheduler.cancelEventNotification(it, eventId)

      // Only schedule notification if the current user is a participant of the event
      val currentUserId = Firebase.auth.currentUser?.uid
      if (newValue.isUpcoming() &&
          currentUserId != null &&
          newValue.participants.contains(currentUserId)) {
        NotificationScheduler.scheduleEventNotification(it, newValue)
      }
    }
  }

  override suspend fun deleteEvent(eventId: String) {
    db.collection(EVENTS_COLLECTION_PATH).document(eventId).delete().await()

    // Cancel notification when event is deleted
    context?.let { NotificationScheduler.cancelEventNotification(it, eventId) }
  }

  /**
   * Converts a Firestore document to an [Event] object.
   *
   * @param document The Firestore document to convert.
   * @return The [Event] object, or null if conversion fails.
   */
  private fun documentToEvent(document: DocumentSnapshot): Event? {
    return try {
      val eventId = document.id
      val typeString = document.getString("type") ?: EventType.SOCIAL.name
      val visibilityString = document.getString("visibility") ?: EventVisibility.PUBLIC.name
      val title = document.getString("title") ?: return null
      val description = document.getString("description") ?: ""
      val date = document.getTimestamp("date") ?: return null
      val duration = (document.getLong("duration") ?: 0L).toInt()
      val participants = document.get("participants") as? List<String> ?: emptyList()
      val maxParticipants = (document.getLong("maxParticipants") ?: 0L).toInt()
      val ownerId = document.getString("ownerId") ?: return null

      val locationData = document.get("location") as? Map<*, *>
      val location =
          locationData?.let {
            Location(
                latitude = it["latitude"] as? Double ?: 0.0,
                longitude = it["longitude"] as? Double ?: 0.0,
                name = it["name"] as? String ?: "")
          }

      val partOfASerie = document.getBoolean("partOfASerie") ?: false

      Event(
          eventId = eventId,
          type = EventType.valueOf(typeString),
          title = title,
          description = description,
          location = location,
          date = date,
          duration = duration,
          participants = participants,
          maxParticipants = maxParticipants,
          visibility = EventVisibility.valueOf(visibilityString),
          ownerId = ownerId,
          partOfASerie = partOfASerie)
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Applies client-side filtering and sorting to events based on the specified filter.
   *
   * This method performs in-memory filtering that cannot be efficiently done at the database level,
   * such as filtering by event state (upcoming, active, expired) and excluding events based on user
   * relationships.
   *
   * @param eventFilter The type of filter to apply.
   * @param events The list of events to filter and sort.
   * @param userId The current user's ID.
   * @return A filtered and sorted list of events according to the specified filter criteria.
   */
  private fun clientSideProcessing(
      eventFilter: EventFilter,
      events: List<Event>,
      userId: String
  ): List<Event> {
    return when (eventFilter) {
      EventFilter.EVENTS_FOR_OVERVIEW_SCREEN -> {
        events
      }
      EventFilter.EVENTS_FOR_HISTORY_SCREEN -> {
        events.filter { event -> event.isExpired() }.sortedByDescending { it.date.toDate().time }
      }
      EventFilter.EVENTS_FOR_SEARCH_SCREEN -> {
        events.filter { event ->
          event.isUpcoming() && !event.participants.contains(userId) && event.ownerId != userId
        }
      }
      EventFilter.EVENTS_FOR_MAP_SCREEN -> {
        events.filter { event ->
          (event.isUpcoming() || (event.isActive() && event.participants.contains(userId))) &&
              event.location != null
        }
      }
    }
  }

  /**
   * Fetches events from Firestore with database-level filtering applied.
   *
   * This method performs Firestore queries optimized for each filter type. For the map screen, it
   * combines results from two queries (participant events and public events) to minimize data
   * transfer while ensuring all relevant events are retrieved.
   *
   * @param eventFilter The type of filter determining which Firestore queries to execute.
   * @param userId The current user's ID for filtering events.
   * @return A list of events retrieved from Firestore, converted from document snapshots.
   */
  private suspend fun databaseFetching(eventFilter: EventFilter, userId: String): List<Event> {
    return when (eventFilter) {
      EventFilter.EVENTS_FOR_OVERVIEW_SCREEN,
      EventFilter.EVENTS_FOR_HISTORY_SCREEN -> {
        val snapshot =
            db.collection(EVENTS_COLLECTION_PATH)
                .whereArrayContains("participants", userId)
                .get()
                .await()
        snapshot.mapNotNull { documentToEvent(it) }
      }
      EventFilter.EVENTS_FOR_SEARCH_SCREEN -> {
        val snapshot =
            db.collection(EVENTS_COLLECTION_PATH)
                .whereEqualTo("visibility", EventVisibility.PUBLIC.name)
                .get()
                .await()
        snapshot.mapNotNull { documentToEvent(it) }
      }
      EventFilter.EVENTS_FOR_MAP_SCREEN -> {
        // Execute both Firestore queries in parallel for better performance
        coroutineScope {
          val participantSnapshotDeferred = async {
            db.collection(EVENTS_COLLECTION_PATH)
                .whereArrayContains("participants", userId)
                .get()
                .await()
          }
          val publicSnapshotDeferred = async {
            db.collection(EVENTS_COLLECTION_PATH)
                .whereEqualTo("visibility", EventVisibility.PUBLIC.name)
                .get()
                .await()
          }

          val participantEvents =
              participantSnapshotDeferred.await().mapNotNull { documentToEvent(it) }
          val publicEvents = publicSnapshotDeferred.await().mapNotNull { documentToEvent(it) }
          (participantEvents + publicEvents).distinctBy { it.eventId }
        }
      }
    }
  }
}
