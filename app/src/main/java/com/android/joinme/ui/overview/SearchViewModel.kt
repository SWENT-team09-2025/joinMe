package com.android.joinme.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.isUpcoming
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.filter.FilterRepository
import com.android.joinme.model.filter.FilterState
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Search screen.
 *
 * @property query The current search query text
 * @property categoryExpanded Whether the sport category dropdown menu is expanded
 * @property eventItems List of event items (events and series) to display after applying filters
 * @property errorMsg Error message to display if fetching events fails
 */
data class SearchUIState(
    val query: String = "",
    val categoryExpanded: Boolean = false,
    val eventItems: List<EventItem> = emptyList(),
    val errorMsg: String? = null,
)

/**
 * ViewModel for the Search screen.
 *
 * Manages the search query and delegates filter management to FilterRepository. Fetches events and
 * series from the repositories and applies filters to display relevant results. Filter state is
 * shared across the application through FilterRepository.
 *
 * @property eventRepository Repository for fetching event data
 * @property seriesRepository Repository for fetching series data
 * @property filterRepository Repository for managing filter state (defaults to FilterRepository
 *   singleton)
 */
class SearchViewModel(
    private val eventRepository: EventsRepository? = null,
    private val seriesRepository: SeriesRepository? = null,
    private val filterRepository: FilterRepository = FilterRepository
) : ViewModel() {

  private val eventRepo: EventsRepository by lazy {
    eventRepository ?: EventsRepositoryProvider.getRepository(isOnline = true)
  }

  private val seriesRepo: SeriesRepository by lazy {
    seriesRepository ?: SeriesRepositoryProvider.repository
  }

  // Search UI state
  private val _uiState = MutableStateFlow(SearchUIState())
  val uiState: StateFlow<SearchUIState> = _uiState.asStateFlow()

  // Expose filter state from FilterRepository
  val filterState: StateFlow<FilterState> = filterRepository.filterState

  // Store all events and series fetched from repositories
  private var allEvents: List<Event> = emptyList()
  private var allSeries: List<Serie> = emptyList()

  init {
    // Observe filter changes and apply them to events and series
    viewModelScope.launch { filterRepository.filterState.collect { applyFiltersToUIState() } }
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /**
   * Updates the search query and re-applies filters.
   *
   * @param query The new search query text
   */
  fun setQuery(query: String) {
    _uiState.value = _uiState.value.copy(query = query)
    applyFiltersToUIState()
  }

  /** Toggles the "Social" filter. Delegates to FilterRepository. */
  fun toggleSocial() {
    filterRepository.toggleSocial()
  }

  /** Toggles the "Activity" filter. Delegates to FilterRepository. */
  fun toggleActivity() {
    filterRepository.toggleActivity()
  }

  /**
   * Sets the category dropdown expanded state.
   *
   * @param expanded True to expand the dropdown, false to collapse it
   */
  fun setCategoryExpanded(expanded: Boolean) {
    _uiState.value = _uiState.value.copy(categoryExpanded = expanded)
  }

  /** Toggles the "Select All" sports filter. Delegates to FilterRepository. */
  fun toggleSelectAll() {
    filterRepository.toggleSelectAll()
  }

  /**
   * Toggles a specific sport filter by ID. Delegates to FilterRepository.
   *
   * @param sportId The unique identifier of the sport to toggle
   */
  fun toggleSport(sportId: String) {
    filterRepository.toggleSport(sportId)
  }

  /**
   * Sets the events list (for testing purposes).
   *
   * @param events The list of events to display
   */
  fun setEvents(events: List<Event>) {
    allEvents = events
    applyFiltersToUIState()
  }

  /**
   * Sets the series list (for testing purposes).
   *
   * @param series The list of series to display
   */
  fun setSeries(series: List<Serie>) {
    allSeries = series
    applyFiltersToUIState()
  }

  /**
   * Refreshes the UI state by fetching all events and series from the repositories.
   *
   * This method triggers a fetch of all events and series from the repositories and applies the
   * current filters to update the displayed items list. It should be called when the screen first
   * loads or when the user wants to refresh the data.
   *
   * If fetching data fails, an error message is set in the UI state which can be displayed to the
   * user.
   *
   * @see getAllEventsAndSeries
   * @see applyFiltersToUIState
   */
  fun refreshUIState() {
    getAllEventsAndSeries()
  }

  /**
   * Fetches all events and series from the repositories and updates the UI state.
   *
   * This method fetches both events and series in parallel, filters them, and updates the UI state
   * with the combined results.
   */
  private fun getAllEventsAndSeries() {
    viewModelScope.launch {
      try {
        // Fetch events and series in parallel
        val eventsDeferred = async {
          eventRepo.getAllEvents(EventFilter.EVENTS_FOR_SEARCH_SCREEN).filter { it.isUpcoming() }
        }
        val seriesDeferred = async { seriesRepo.getAllSeries(SerieFilter.SERIES_FOR_SEARCH_SCREEN) }

        allEvents = eventsDeferred.await()
        allSeries = seriesDeferred.await()
        applyFiltersToUIState()
      } catch (e: Exception) {
        setErrorMsg("Failed to load events and series: ${e.message}")
      }
    }
  }

  /**
   * Applies filters and search query to all events and series, then updates the UI state.
   *
   * This method combines events and series into a single list of EventItems, applies category
   * filters, search query filters, and sorts the results by date. Events that belong to series are
   * excluded - only the serie card is shown for those events.
   */
  private fun applyFiltersToUIState() {
    // Apply category filters (Social, Activity, Sports) to events
    val filteredEvents = filterRepository.applyFilters(allEvents)

    // Identify events that belong to series
    val serieEventIds = allSeries.flatMap { it.eventIds }.toSet()

    // Filter out standalone events (events not in any serie)
    val standaloneEvents = filteredEvents.filterNot { it.eventId in serieEventIds }

    // Convert standalone events and all series to EventItems
    val eventItems = mutableListOf<EventItem>()
    eventItems.addAll(standaloneEvents.map { EventItem.SingleEvent(it) })
    eventItems.addAll(allSeries.map { EventItem.EventSerie(it) })

    // Apply search query filter if query is not empty
    val query = _uiState.value.query
    val filteredItems =
        if (query.isNotBlank()) {
          eventItems.filter { item ->
            when (item) {
              is EventItem.SingleEvent ->
                  item.event.title.contains(query, ignoreCase = true) ||
                      item.event.description.contains(query, ignoreCase = true)
              is EventItem.EventSerie ->
                  item.serie.title.contains(query, ignoreCase = true) ||
                      item.serie.description.contains(query, ignoreCase = true)
            }
          }
        } else {
          eventItems
        }

    // Sort by date (most recent first)
    val sortedItems = filteredItems.sortedBy { it.date }

    _uiState.value = _uiState.value.copy(eventItems = sortedItems)
  }
}
