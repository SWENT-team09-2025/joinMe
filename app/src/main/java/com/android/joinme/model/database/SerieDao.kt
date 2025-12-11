package com.android.joinme.model.database

import androidx.room.*

/**
 * Data Access Object for Serie entities. Provides methods to query, insert, update, and delete
 * series from the local Room database.
 */
@Dao
interface SerieDao {
  /**
   * Retrieves a single serie by its unique identifier.
   *
   * @param serieId The unique identifier of the serie
   * @return The serie entity if found, null otherwise
   */
  @Query("SELECT * FROM series WHERE serieId = :serieId")
  suspend fun getSerieById(serieId: String): SerieEntity?

  /**
   * Retrieves all cached series from the database.
   *
   * @return List of all serie entities in the cache
   */
  @Query("SELECT * FROM series") suspend fun getAllSeries(): List<SerieEntity>

  /**
   * Retrieves all series owned by a specific user.
   *
   * @param userId The unique identifier of the serie owner
   * @return List of series owned by the specified user
   */
  @Query("SELECT * FROM series WHERE ownerId = :userId")
  suspend fun getSeriesByOwner(userId: String): List<SerieEntity>

  /**
   * Inserts or updates a single serie in the database.
   *
   * If a serie with the same ID already exists, it will be replaced with the new data.
   *
   * @param serie The serie entity to insert or update
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSerie(serie: SerieEntity)

  /**
   * Inserts or updates multiple series in the database.
   *
   * If series with the same IDs already exist, they will be replaced with the new data.
   *
   * @param series List of serie entities to insert or update
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSeries(series: List<SerieEntity>)

  /**
   * Deletes a specific serie from the database.
   *
   * @param serieId The unique identifier of the serie to delete
   */
  @Query("DELETE FROM series WHERE serieId = :serieId") suspend fun deleteSerie(serieId: String)

  /**
   * Deletes all series from the database.
   *
   * This operation clears the entire serie cache.
   */
  @Query("DELETE FROM series") suspend fun deleteAllSeries()

  /**
   * Deletes series cached before a specific timestamp.
   *
   * This operation is useful for cache maintenance and removing stale data.
   *
   * @param timestamp The timestamp in milliseconds; series cached before this time will be deleted
   */
  @Query("DELETE FROM series WHERE cachedAt < :timestamp")
  suspend fun deleteOldSeries(timestamp: Long)
}
