package com.android.joinme.model.database

import androidx.room.*

/**
 * Data Access Object for Event entities. Provides methods to query, insert, update, and delete
 * events from the local Room database.
 */
@Dao
interface EventDao {
  /**
   * Retrieves a single event by its unique identifier.
   *
   * @param eventId The unique identifier of the event
   * @return The event entity if found, null otherwise
   */
  @Query("SELECT * FROM events WHERE eventId = :eventId")
  suspend fun getEventById(eventId: String): EventEntity?

  /**
   * Retrieves all cached events from the database.
   *
   * @return List of all event entities in the cache
   */
  @Query("SELECT * FROM events") suspend fun getAllEvents(): List<EventEntity>

  /**
   * Retrieves all events owned by a specific user.
   *
   * @param userId The unique identifier of the event owner
   * @return List of events owned by the specified user
   */
  @Query("SELECT * FROM events WHERE ownerId = :userId")
  suspend fun getEventsByOwner(userId: String): List<EventEntity>

  /**
   * Retrieves public events ordered by date in descending order.
   *
   * @param limit Maximum number of events to retrieve
   * @return List of public events, most recent first, up to the specified limit
   */
  @Query("SELECT * FROM events WHERE visibility = 'PUBLIC' ORDER BY dateSeconds DESC LIMIT :limit")
  suspend fun getPublicEvents(limit: Int): List<EventEntity>

  /**
   * Retrieves upcoming events starting from a specified time.
   *
   * @param startSeconds The start time in seconds (Unix timestamp) to filter events from
   * @param limit Maximum number of events to retrieve
   * @return List of upcoming events ordered by date in ascending order, up to the specified limit
   */
  @Query(
      "SELECT * FROM events WHERE dateSeconds >= :startSeconds ORDER BY dateSeconds ASC LIMIT :limit")
  suspend fun getUpcomingEvents(startSeconds: Long, limit: Int): List<EventEntity>

  /**
   * Inserts or updates a single event in the database.
   *
   * If an event with the same ID already exists, it will be replaced with the new data.
   *
   * @param event The event entity to insert or update
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEvent(event: EventEntity)

  /**
   * Inserts or updates multiple events in the database.
   *
   * If events with the same IDs already exist, they will be replaced with the new data.
   *
   * @param events List of event entities to insert or update
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvents(events: List<EventEntity>)

  /**
   * Deletes a specific event from the database.
   *
   * @param eventId The unique identifier of the event to delete
   */
  @Query("DELETE FROM events WHERE eventId = :eventId") suspend fun deleteEvent(eventId: String)

  /**
   * Deletes all events from the database.
   *
   * This operation clears the entire event cache.
   */
  @Query("DELETE FROM events") suspend fun deleteAllEvents()

  /**
   * Deletes events cached before a specific timestamp.
   *
   * This operation is useful for cache maintenance and removing stale data.
   *
   * @param timestamp The timestamp in milliseconds; events cached before this time will be deleted
   */
  @Query("DELETE FROM events WHERE cachedAt < :timestamp")
  suspend fun deleteOldEvents(timestamp: Long)
}
