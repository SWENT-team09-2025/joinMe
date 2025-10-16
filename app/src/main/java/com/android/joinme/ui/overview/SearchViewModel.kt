package com.android.joinme.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.isUpcoming
import com.android.joinme.model.filter.FilterRepository
import com.android.joinme.model.filter.FilterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Search screen.
 *
 * @property query The current search query text
 * @property categoryExpanded Whether the sport category dropdown menu is expanded
 * @property events List of events to display after applying filters
 * @property errorMsg Error message to display if fetching events fails
 */
data class SearchUIState(
    val query: String = "",
    val categoryExpanded: Boolean = false,
    val events: List<Event> = emptyList(),
    val errorMsg: String? = null,
)

/**
 * ViewModel for the Search screen.
 *
 * Manages the search query and delegates filter management to FilterRepository. Fetches events from
 * the repository and applies filters to display relevant results. Filter state is shared across the
 * application through FilterRepository.
 *
 * @property eventRepository Repository for fetching event data
 * @property filterRepository Repository for managing filter state (defaults to FilterRepository
 *   singleton)
 */
class SearchViewModel(
    private val eventRepository: EventsRepository? = null,
    private val filterRepository: FilterRepository = FilterRepository
) : ViewModel() {

  private val repo: EventsRepository by lazy {
    eventRepository ?: EventsRepositoryProvider.getRepository(isOnline = true)
  }
  // Search UI state
  private val _uiState = MutableStateFlow(SearchUIState())
  val uiState: StateFlow<SearchUIState> = _uiState.asStateFlow()

  // Expose filter state from FilterRepository
  val filterState: StateFlow<FilterState> = filterRepository.filterState

  // Store all events fetched from repository
  private var allEvents: List<Event> = emptyList()

  init {
    // Observe filter changes and apply them to events
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

  /** Toggles the "All" filter. Delegates to FilterRepository. */
  fun toggleAll() {
    filterRepository.toggleAll()
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
   * Refreshes the UI state by fetching all events from the repository.
   *
   * This method triggers a fetch of all events from the repository and applies the current filters
   * to update the displayed events list. It should be called when the screen first loads or when
   * the user wants to refresh the event data.
   *
   * If fetching events fails, an error message is set in the UI state which can be displayed to the
   * user.
   *
   * @see getAllEvents
   * @see applyFiltersToUIState
   */
  fun refreshUIState() {
    getAllEvents()
  }

  /** Fetches all events from the repository and updates the UI state. */
  private fun getAllEvents() {
    viewModelScope.launch {
      try {
        allEvents = repo.getAllEvents().filter { it.isUpcoming() }
        applyFiltersToUIState()
      } catch (e: Exception) {
        setErrorMsg("Failed to load events: ${e.message}")
      }
    }
  }

  /** Applies filters and search query to all events and updates the UI state. */
  private fun applyFiltersToUIState() {
    // First apply category filters (Social, Activity, Sports)
    var filteredEvents = filterRepository.applyFilters(allEvents)

    // Then apply search query filter if query is not empty
    val query = _uiState.value.query
    if (query.isNotBlank()) {
      filteredEvents =
          filteredEvents.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
          }
    }

    _uiState.value = _uiState.value.copy(events = filteredEvents)
  }
}
