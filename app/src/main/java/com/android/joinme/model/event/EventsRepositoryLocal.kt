package com.android.joinme.model.event

/** Exception message for when an event is not found in the local repository. */
const val EVENT_NOT_FOUND = "EventsRepositoryLocal: Event not found"

/** Represents a repository that manages a local list of events (for offline mode or testing). */
class EventsRepositoryLocal : EventsRepository {
  private val events: MutableList<Event> = mutableListOf()
  private var counter = 0

  override fun getNewEventId(): String {
    return (counter++).toString()
  }

  override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> {
    return events
  }

  override suspend fun getEvent(eventId: String): Event {
    return events.find { it.eventId == eventId } ?: throw Exception(EVENT_NOT_FOUND)
  }

  override suspend fun addEvent(event: Event) {
    // ensure creator is participant
    val fixedEvent =
        if (!event.participants.contains(event.ownerId))
            event.copy(participants = event.participants + event.ownerId)
        else event

    events.add(fixedEvent)
  }

  override suspend fun editEvent(eventId: String, newValue: Event) {
    val index = events.indexOfFirst { it.eventId == eventId }
    if (index != -1) {
      events[index] = newValue
    } else {
      throw Exception(EVENT_NOT_FOUND)
    }
  }

  override suspend fun deleteEvent(eventId: String) {
    val index = events.indexOfFirst { it.eventId == eventId }
    if (index != -1) {
      events.removeAt(index)
    } else {
      throw Exception(EVENT_NOT_FOUND)
    }
  }

  override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
    return events.filter { eventIds.contains(it.eventId) }
  }

    override suspend fun getCommonEvents(userIds: List<String>): List<Event> {
        if (userIds.isEmpty()) {
            return emptyList()
        }

        // The current buggy implementation is likely using .any or some other logic.
        // Replace it with this .filter using .all
        return events.filter { event ->
            // This ensures an event is included ONLY IF it contains ALL of the userIds.
            userIds.all { userId -> event.participants.contains(userId) }
        }.sortedBy { it.date }
    }



    /** Clears all events from the repository. Useful for testing. */
  fun clear() {
    events.clear()
    counter = 0
  }
}
