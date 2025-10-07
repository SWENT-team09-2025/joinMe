package com.android.joinme.model.event

/** Represents a repository that manages a local list of events (for offline mode or testing). */
class EventsRepositoryLocal : EventsRepository {
  private val events: MutableList<Event> = mutableListOf()
  private var counter = 0

  override fun getNewEventId(): String {
    return (counter++).toString()
  }

  override suspend fun getAllEvents(): List<Event> {
    return events
  }

  override suspend fun getEvent(eventId: String): Event {
    return events.find { it.eventId == eventId }
        ?: throw Exception("EventsRepositoryLocal: Event not found")
  }

  override suspend fun addEvent(event: Event) {
    events.add(event)
  }

  override suspend fun editEvent(eventId: String, newValue: Event) {
    val index = events.indexOfFirst { it.eventId == eventId }
    if (index != -1) {
      events[index] = newValue
    } else {
      throw Exception("EventsRepositoryLocal: Event not found")
    }
  }

  override suspend fun deleteEvent(eventId: String) {
    val index = events.indexOfFirst { it.eventId == eventId }
    if (index != -1) {
      events.removeAt(index)
    } else {
      throw Exception("EventsRepositoryLocal: Event not found")
    }
  }
}
