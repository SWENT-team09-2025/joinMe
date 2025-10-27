package com.android.joinme.model.serie

/**
 * Repository interface for managing Serie (event series) data operations.
 *
 * Provides CRUD operations for series, which are collections of recurring events that share common
 * properties like title, description, and visibility.
 */
interface SeriesRepository {

  /**
   * Generates and returns a new unique identifier for a Serie item.
   *
   * @return A new unique Serie ID string
   */
  fun getNewSerieId(): String

  /**
   * Retrieves all Serie items from the repository.
   *
   * @return A list of all Serie items
   */
  suspend fun getAllSeries(): List<Serie>

  /**
   * Retrieves a specific Serie item by its unique identifier.
   *
   * @param serieId The unique identifier of the Serie item to retrieve
   * @return The Serie item with the specified identifier
   * @throws Exception if the Serie item is not found
   */
  suspend fun getSerie(serieId: String): Serie

  /**
   * Adds a new Serie item to the repository.
   *
   * @param serie The Serie item to add
   */
  suspend fun addSerie(serie: Serie)

  /**
   * Edits an existing Serie item in the repository.
   *
   * @param serieId The unique identifier of the Serie item to edit
   * @param newValue The new value for the Serie item
   * @throws Exception if the Serie item is not found
   */
  suspend fun editSerie(serieId: String, newValue: Serie)

  /**
   * Deletes a Serie item from the repository.
   *
   * @param serieId The unique identifier of the Serie item to delete
   * @throws Exception if the Serie item is not found
   */
  suspend fun deleteSerie(serieId: String)
}
