package com.android.joinme.model.database

import androidx.room.*

/**
 * Data Access Object for Group entities. Provides methods to query, insert, update, and delete
 * groups from the local Room database.
 */
@Dao
interface GroupDao {
  /**
   * Retrieves a single group by its unique identifier.
   *
   * @param groupId The unique identifier of the group
   * @return The group entity if found, null otherwise
   */
  @Query("SELECT * FROM groups WHERE id = :groupId")
  suspend fun getGroupById(groupId: String): GroupEntity?

  /**
   * Retrieves all cached groups from the database.
   *
   * @return List of all group entities in the cache
   */
  @Query("SELECT * FROM groups") suspend fun getAllGroups(): List<GroupEntity>

  /**
   * Inserts or updates a single group in the database.
   *
   * If a group with the same ID already exists, it will be replaced with the new data.
   *
   * @param group The group entity to insert or update
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGroup(group: GroupEntity)

  /**
   * Inserts or updates multiple groups in the database.
   *
   * If groups with the same IDs already exist, they will be replaced with the new data.
   *
   * @param groups List of group entities to insert or update
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGroups(groups: List<GroupEntity>)

  /**
   * Deletes a specific group from the database.
   *
   * @param groupId The unique identifier of the group to delete
   */
  @Query("DELETE FROM groups WHERE id = :groupId") suspend fun deleteGroup(groupId: String)

  /**
   * Deletes all groups from the database.
   *
   * This operation clears the entire group cache.
   */
  @Query("DELETE FROM groups") suspend fun deleteAllGroups()

  /**
   * Deletes groups cached before a specific timestamp.
   *
   * This operation is useful for cache maintenance and removing stale data.
   *
   * @param timestamp The timestamp in milliseconds; groups cached before this time will be deleted
   */
  @Query("DELETE FROM groups WHERE cachedAt < :timestamp")
  suspend fun deleteOldGroups(timestamp: Long)
}
