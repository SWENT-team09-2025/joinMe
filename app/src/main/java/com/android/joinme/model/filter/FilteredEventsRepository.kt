package com.android.joinme.model.filter

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.isUpcoming
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository for managing filtered events and series data across the application.
 *
 * This singleton provides a centralized source of truth for filtered event data. It handles:
 * - Fetching events and series from their respective repositories
 * - Applying filters from FilterRepository
 * - Managing the filtered data state
 * - Automatically updating when filters change
 *
 * This eliminates the need for each ViewModel to duplicate fetch-filter logic.
 *
 * @property eventRepository Repository for fetching event data (injectable for testing)
 * @property seriesRepository Repository for fetching series data (injectable for testing)
 * @property filterRepository Repository managing filter state
 */
class FilteredEventsRepository(
    private val eventRepository: EventsRepository? = null,
    private val seriesRepository: SeriesRepository? = null,
    private val filterRepository: FilterRepository = FilterRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
  private val eventRepo: EventsRepository by lazy {
    eventRepository ?: EventsRepositoryProvider.getRepository(isOnline = true)
  }

  private val seriesRepo: SeriesRepository by lazy {
    seriesRepository ?: SeriesRepositoryProvider.repository
  }

  // Coroutine scope for repository operations
  private val repositoryScope = CoroutineScope(SupervisorJob() + dispatcher)

  // Store all events and series fetched from repositories
  private var allEvents: List<Event> = emptyList()
  private var allSeries: List<Serie> = emptyList()

  // Filtered events state
  private val _filteredEvents = MutableStateFlow<List<Event>>(emptyList())
  val filteredEvents: StateFlow<List<Event>> = _filteredEvents.asStateFlow()

  // Filtered series state
  private val _filteredSeries = MutableStateFlow<List<Serie>>(emptyList())
  val filteredSeries: StateFlow<List<Serie>> = _filteredSeries.asStateFlow()

  // Error state
  private val _errorMsg = MutableStateFlow<String?>(null)
  val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

  // Loading state
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  init {
    // Observe filter changes and re-apply filters automatically
    repositoryScope.launch { filterRepository.filterState.collect { applyFilters() } }
  }

  /** Clears the error message. */
  fun clearErrorMsg() {
    _errorMsg.value = null
  }

  /**
   * Refreshes data by fetching all events and series from repositories.
   *
   * Fetches events and series sequentially, applies current filters, and updates the state. If
   * fetching fails, sets an error message.
   */
  fun refresh() {
    repositoryScope.launch {
      try {
        _isLoading.value = true
        _errorMsg.value = null

        allEvents =
            eventRepo.getAllEvents(EventFilter.EVENTS_FOR_SEARCH_SCREEN).filter { it.isUpcoming() }
        allSeries = seriesRepo.getAllSeries(SerieFilter.SERIES_FOR_SEARCH_SCREEN)

        applyFilters()
      } catch (e: Exception) {
        _errorMsg.value = "Failed to load events and series: ${e.message}"
      } finally {
        _isLoading.value = false
      }
    }
  }

  /**
   * Applies current filters to all events and series, and updates their filtered states.
   *
   * This is called automatically when filters change or when new data is fetched.
   */
  private fun applyFilters() {
    // Get current user ID for participation filtering
    // Wrap in try-catch to handle Firebase not being initialized in tests
    val currentUserId =
        try {
          Firebase.auth.currentUser?.uid ?: ""
        } catch (e: IllegalStateException) {
          ""
        }

    val filteredEvents = filterRepository.applyFilters(allEvents, currentUserId)
    val filteredSeries = filterRepository.applyFiltersToSeries(allSeries, allEvents, currentUserId)

    _filteredEvents.value = filteredEvents
    _filteredSeries.value = filteredSeries
  }

  /**
   * Closes the repository and cancels all ongoing coroutines.
   *
   * This should be called when the repository is no longer needed to prevent memory leaks.
   */
  fun close() {
    repositoryScope.cancel()
  }

  companion object {
    /** Singleton instance of FilteredEventsRepository. */
    @Volatile private var INSTANCE: FilteredEventsRepository? = null

    /** Gets the singleton instance of FilteredEventsRepository. */
    fun getInstance(): FilteredEventsRepository {
      return INSTANCE
          ?: synchronized(this) { INSTANCE ?: FilteredEventsRepository().also { INSTANCE = it } }
    }

    /**
     * Resets the singleton instance (for testing purposes).
     *
     * @param instance Optional instance to set (for dependency injection in tests)
     */
    fun resetInstance(instance: FilteredEventsRepository? = null) {
      synchronized(this) { INSTANCE = instance }
    }
  }
}
