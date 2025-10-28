package com.android.joinme.model.event

import android.util.Log
import com.android.joinme.model.map.Location
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.get
import kotlinx.coroutines.tasks.await

const val EVENTS_COLLECTION_PATH = "events"

/**
 * Firestore-backed implementation of [EventsRepository]. Manages CRUD operations for [Event]
 * objects.
 */
class EventsRepositoryFirestore(private val db: FirebaseFirestore) : EventsRepository {
  private val ownerAttributeName = "ownerId"

  override fun getNewEventId(): String {
    return db.collection(EVENTS_COLLECTION_PATH).document().id
  }

  override suspend fun getAllEvents(): List<Event> {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("EventsRepositoryFirestore: User not logged in.")

    val snapshot =
        db.collection(EVENTS_COLLECTION_PATH)
            .whereEqualTo(ownerAttributeName, ownerId)
            .get()
            .await()

    return snapshot.mapNotNull { documentToEvent(it) }
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
  }

  override suspend fun editEvent(eventId: String, newValue: Event) {
    db.collection(EVENTS_COLLECTION_PATH).document(eventId).set(newValue).await()
  }

  override suspend fun deleteEvent(eventId: String) {
    db.collection(EVENTS_COLLECTION_PATH).document(eventId).delete().await()
  }

  /**
   * Performs a Firestore operation and calls the appropriate callback based on the result.
   *
   * @param task The Firestore task to perform.
   * @param onSuccess The callback to call if the operation is successful.
   * @param onFailure The callback to call if the operation fails.
   */
  private fun performFirestoreOperation(
      task: Task<Void>,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    task.addOnCompleteListener { result ->
      if (result.isSuccessful) {
        onSuccess()
      } else {
        result.exception?.let { e ->
          Log.e("EventsRepositoryFirestore", "Error performing Firestore operation", e)
          onFailure(e)
        }
      }
    }
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
          ownerId = ownerId)
    } catch (e: Exception) {
      Log.e("EventsRepositoryFirestore", "Error converting document to Event", e)
      null
    }
  }
}
