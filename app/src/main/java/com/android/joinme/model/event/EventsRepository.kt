package com.android.joinme.model.event

/** Represents a repository that manages Event items. */
interface EventsRepository {

    /** Generates and returns a new unique identifier for an Event item. */
    fun getNewUid(): String

    /**
     * Retrieves all Event items from the repository.
     *
     * @return A list of all Event items.
     */
    suspend fun getAllEvents(): List<Event>

    /**
     * Retrieves a specific Event item by its unique identifier.
     *
     * @param eventID The unique identifier of the Event item to retrieve.
     * @return The Event item with the specified identifier.
     * @throws Exception if the event item is not found.
     */
    suspend fun getEvent(eventID: String): Event

    /**
     * Adds a new Event item to the repository.
     *
     * @param event The Event item to add.
     */
    suspend fun addEvent(event: Event)

    /**
     * Edits an existing Event item in the repository.
     *
     * @param eventID The unique identifier of the Event item to edit.
     * @param newValue The new value for the Event item.
     * @throws Exception if the Event item is not found.
     */
    suspend fun editEvent(eventID: String, newValue: Event)

    /**
     * Deletes an Event item from the repository.
     *
     * @param eventID The unique identifier of the Event item to delete.
     * @throws Exception if the Event item is not found.
     */
    suspend fun deleteEvent(eventID: String)
}