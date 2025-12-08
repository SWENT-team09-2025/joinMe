package com.android.joinme.model.database

import androidx.room.*

/**
 * Data Access Object for Event entities. Provides methods to query, insert, update, and delete
 * events from the local Room database.
 */
@Dao
interface EventDao {
  @Query("SELECT * FROM events WHERE eventId = :eventId")
  suspend fun getEventById(eventId: String): EventEntity?

  @Query("SELECT * FROM events") suspend fun getAllEvents(): List<EventEntity>

  @Query("SELECT * FROM events WHERE ownerId = :userId")
  suspend fun getEventsByOwner(userId: String): List<EventEntity>

  @Query("SELECT * FROM events WHERE visibility = 'PUBLIC' ORDER BY dateSeconds DESC LIMIT :limit")
  suspend fun getPublicEvents(limit: Int): List<EventEntity>

  @Query(
      "SELECT * FROM events WHERE dateSeconds >= :startSeconds ORDER BY dateSeconds ASC LIMIT :limit")
  suspend fun getUpcomingEvents(startSeconds: Long, limit: Int): List<EventEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEvent(event: EventEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvents(events: List<EventEntity>)

  @Query("DELETE FROM events WHERE eventId = :eventId") suspend fun deleteEvent(eventId: String)

  @Query("DELETE FROM events") suspend fun deleteAllEvents()

  @Query("DELETE FROM events WHERE cachedAt < :timestamp")
  suspend fun deleteOldEvents(timestamp: Long)
}
