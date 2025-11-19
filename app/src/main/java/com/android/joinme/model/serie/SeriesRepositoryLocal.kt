package com.android.joinme.model.serie

import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryLocal

/**
 * Local in-memory implementation of SeriesRepository.
 *
 * This implementation stores Serie objects in a mutable list and is primarily used for testing and
 * offline functionality. Data is not persisted and will be lost when the application is closed.
 *
 * @property eventsRepository Repository for managing events associated with series
 */
class SeriesRepositoryLocal(
    private val eventsRepository: EventsRepository = EventsRepositoryLocal()
) : SeriesRepository {
  /** In-memory storage for Serie objects */
  private val series: MutableList<Serie> = mutableListOf()

  /** Counter for generating unique Serie IDs */
  private var counter = 0

  /**
   * Generates and returns a new unique identifier for a Serie item.
   *
   * IDs are generated sequentially starting from 0.
   *
   * @return A new unique Serie ID string
   */
  override fun getNewSerieId(): String {
    return (counter++).toString()
  }

  /**
   * Retrieves all Serie items from the local storage.
   *
   * @return A list of all Serie items currently stored
   */
  override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> {
    return series
  }

  /**
   * Retrieves a specific Serie item by its unique identifier.
   *
   * @param serieId The unique identifier of the Serie item to retrieve
   * @return The Serie item with the specified identifier
   * @throws Exception if the Serie item is not found in local storage
   */
  override suspend fun getSerie(serieId: String): Serie {
    return series.find { it.serieId == serieId }
        ?: throw Exception("SeriesRepositoryLocal: Serie not found")
  }

  /**
   * Adds a new Serie item to the local storage.
   *
   * @param serie The Serie item to add
   */
  override suspend fun addSerie(serie: Serie) {
    series.add(serie)
  }

  /**
   * Edits an existing Serie item in the local storage.
   *
   * Replaces the existing Serie with the new value at the same index.
   *
   * @param serieId The unique identifier of the Serie item to edit
   * @param newValue The new value for the Serie item
   * @throws Exception if the Serie item is not found in local storage
   */
  override suspend fun editSerie(serieId: String, newValue: Serie) {
    val index = series.indexOfFirst { it.serieId == serieId }
    if (index != -1) {
      series[index] = newValue
    } else {
      throw Exception("SeriesRepositoryLocal: Serie not found")
    }
  }

  /**
   * Deletes a Serie item from the local storage.
   *
   * Also deletes all events associated with this serie.
   *
   * @param serieId The unique identifier of the Serie item to delete
   * @throws Exception if the Serie item is not found in local storage
   */
  override suspend fun deleteSerie(serieId: String) {
    val serie = getSerie(serieId)
    // Delete all events related to the serie
    serie.eventIds.forEach { eventId -> eventsRepository.deleteEvent(eventId) }
    // Delete serie
    val index = series.indexOfFirst { it.serieId == serieId }
    if (index != -1) {
      series.removeAt(index)
    } else {
      throw Exception("SeriesRepositoryLocal: Serie not found")
    }
  }

  /**
   * Retrieves a list of Serie items by their unique identifiers.
   *
   * @param seriesIds The list of unique identifiers of the Serie items to retrieve
   * @return A list of Serie items with the specified identifiers
   */
  override suspend fun getSeriesByIds(seriesIds: List<String>): List<Serie> {
    return series.filter { seriesIds.contains(it.serieId) }
  }

  /** Clears all series from the repository. Useful for testing. */
  fun clear() {
    series.clear()
    counter = 0
  }
}
