package com.android.joinme.model.database

import androidx.room.*

/**
 * Data Access Object for Profile entities. Provides methods to query, insert, update, and delete
 * profiles from the local Room database.
 */
@Dao
interface ProfileDao {
  /**
   * Retrieves a single profile by its unique identifier.
   *
   * @param uid The unique identifier of the profile
   * @return The profile entity if found, null otherwise
   */
  @Query("SELECT * FROM profiles WHERE uid = :uid")
  suspend fun getProfileById(uid: String): ProfileEntity?

  /**
   * Retrieves all cached profiles from the database.
   *
   * @return List of all profile entities in the cache
   */
  @Query("SELECT * FROM profiles") suspend fun getAllProfiles(): List<ProfileEntity>

  /**
   * Inserts or updates a single profile in the database.
   *
   * If a profile with the same UID already exists, it will be replaced with the new data.
   *
   * @param profile The profile entity to insert or update
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProfile(profile: ProfileEntity)

  /**
   * Inserts or updates multiple profiles in the database.
   *
   * If profiles with the same UIDs already exist, they will be replaced with the new data.
   *
   * @param profiles List of profile entities to insert or update
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProfiles(profiles: List<ProfileEntity>)

  /**
   * Deletes a specific profile from the database.
   *
   * @param uid The unique identifier of the profile to delete
   */
  @Query("DELETE FROM profiles WHERE uid = :uid") suspend fun deleteProfile(uid: String)

  /**
   * Deletes all profiles from the database.
   *
   * This operation clears the entire profile cache.
   */
  @Query("DELETE FROM profiles") suspend fun deleteAllProfiles()

  /**
   * Deletes profiles cached before a specific timestamp.
   *
   * This operation is useful for cache maintenance and removing stale data.
   *
   * @param timestamp The timestamp in milliseconds; profiles cached before this time will be
   *   deleted
   */
  @Query("DELETE FROM profiles WHERE cachedAt < :timestamp")
  suspend fun deleteOldProfiles(timestamp: Long)
}
