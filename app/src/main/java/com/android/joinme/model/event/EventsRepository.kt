package com.android.joinme.model.event

/** Represents a repository that manages Event items. */
interface EventsRepository {

  /** Generates and returns a new unique identifier for an Event item. */
  fun getNewEventId(): String

  /**
   * Retrieves all event items with the corresponding filter from Firestore specified by
   * the viewModel requested items.
   *
   * @return A list of all Event items.
   */
  suspend fun getAllEvents(eventFilter: EventFilter): List<Event>

  /**
   * Retrieves a specific Event item by its unique identifier.
   *
   * @param eventId The unique identifier of the Event item to retrieve.
   * @return The Event item with the specified identifier.
   * @throws Exception if the event item is not found.
   */
  suspend fun getEvent(eventId: String): Event

  /**
   * Adds a new Event item to the repository.
   *
   * @param event The Event item to add.
   */
  suspend fun addEvent(event: Event)

  /**
   * Edits an existing Event item in the repository.
   *
   * @param eventId The unique identifier of the Event item to edit.
   * @param newValue The new value for the Event item.
   * @throws Exception if the Event item is not found.
   */
  suspend fun editEvent(eventId: String, newValue: Event)

  /**
   * Deletes an Event item from the repository.
   *
   * @param eventId The unique identifier of the Event item to delete.
   * @throws Exception if the Event item is not found.
   */
  suspend fun deleteEvent(eventId: String)
}
