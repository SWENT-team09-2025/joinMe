package com.android.joinme.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.isActive
import com.android.joinme.model.event.isUpcoming
import com.android.joinme.model.sport.Sports
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents a sport category that can be filtered.
 *
 * @property id Unique identifier for the sport
 * @property name Display name of the sport
 * @property isChecked Whether the sport category is currently selected
 */
data class SportCategory(val id: String, val name: String, val isChecked: Boolean = false)

/**
 * UI state for the Search screen.
 *
 * @property query The current search query text
 * @property isAllSelected Whether all filters (Social, Activity, and all sports) are selected
 * @property isSocialSelected Whether the Social event type filter is selected
 * @property isActivitySelected Whether the Activity event type filter is selected
 * @property sportCategories List of sport categories with their selection states
 * @property categoryExpanded Whether the sport category dropdown menu is expanded
 * @property events List of events to display after applying filters
 * @property errorMsg Error message to display if fetching events fails
 * @property selectedSportsCount Computed property returning the count of selected sports
 * @property isSelectAllChecked Computed property indicating if all sports are selected
 */
data class SearchUIState(
    val query: String = "",
    val isAllSelected: Boolean = true,
    val isSocialSelected: Boolean = true,
    val isActivitySelected: Boolean = true,
    val sportCategories: List<SportCategory> =
        Sports.ALL.map { SportCategory(it.id, it.name, isChecked = true) },
    val categoryExpanded: Boolean = false,
    val events: List<Event> = emptyList(),
    val errorMsg: String? = null,
) {
  val selectedSportsCount: Int
    get() = sportCategories.count { it.isChecked }

  val isSelectAllChecked: Boolean
    get() = sportCategories.all { it.isChecked }
}

/**
 * ViewModel for the Search screen.
 *
 * Manages the search query and filter states (Social, Activity, Sports) for event searching.
 * Handles filter toggling logic and maintains the relationship between individual filters and the
 * "All" filter. Fetches events from the repository and applies filters to display relevant results.
 *
 * @property eventRepository Repository for fetching event data
 */
class SearchViewModel(private val eventRepository: EventsRepository? = null) : ViewModel() {

  private val repo: EventsRepository by lazy {
    eventRepository ?: EventsRepositoryProvider.getRepository(isOnline = true)
  }
  // Search UI state
  private val _uiState = MutableStateFlow(SearchUIState())
  val uiState: StateFlow<SearchUIState> = _uiState.asStateFlow()

  // Store all events fetched from repository
  private var allEvents: List<Event> = emptyList()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /**
   * Updates the search query.
   *
   * @param query The new search query text
   */
  fun setQuery(query: String) {
    _uiState.value = _uiState.value.copy(query = query)
  }

  /** Toggles the "All" filter and updates related filters. */
  fun toggleAll() {
    val newAllSelected = !_uiState.value.isAllSelected
    _uiState.value =
        _uiState.value.copy(
            isAllSelected = newAllSelected,
            isSocialSelected = newAllSelected,
            isActivitySelected = newAllSelected,
            sportCategories =
                _uiState.value.sportCategories.map { it.copy(isChecked = newAllSelected) })
    applyFiltersToUIState()
  }

  /** Toggles the "Social" filter. */
  fun toggleSocial() {
    val state = _uiState.value
    val newSocialSelected = !state.isSocialSelected
    _uiState.value =
        state.copy(
            isSocialSelected = newSocialSelected,
            isAllSelected =
                newSocialSelected && state.isActivitySelected && state.isSelectAllChecked)
    applyFiltersToUIState()
  }

  /** Toggles the "Activity" filter. */
  fun toggleActivity() {
    val state = _uiState.value
    val newActivitySelected = !state.isActivitySelected
    _uiState.value =
        state.copy(
            isActivitySelected = newActivitySelected,
            isAllSelected =
                state.isSocialSelected && newActivitySelected && state.isSelectAllChecked)
    applyFiltersToUIState()
  }

  /**
   * Sets the category dropdown expanded state.
   *
   * @param expanded True to expand the dropdown, false to collapse it
   */
  fun setCategoryExpanded(expanded: Boolean) {
    _uiState.value = _uiState.value.copy(categoryExpanded = expanded)
  }

  /** Toggles the "Select All" sports filter. */
  fun toggleSelectAll() {
    val state = _uiState.value
    val newSelectAllChecked = !state.isSelectAllChecked
    _uiState.value =
        state.copy(
            sportCategories =
                state.sportCategories.map { it.copy(isChecked = newSelectAllChecked) },
            isAllSelected =
                newSelectAllChecked && state.isSocialSelected && state.isActivitySelected)
    applyFiltersToUIState()
  }

  /**
   * Toggles a specific sport filter by ID.
   *
   * @param sportId The unique identifier of the sport to toggle
   */
  fun toggleSport(sportId: String) {
    val state = _uiState.value
    val updatedSports =
        state.sportCategories.map { sport ->
          if (sport.id == sportId) sport.copy(isChecked = !sport.isChecked) else sport
        }
    val allSportsChecked = updatedSports.all { it.isChecked }

    _uiState.value =
        state.copy(
            sportCategories = updatedSports,
            isAllSelected = state.isSocialSelected && state.isActivitySelected && allSportsChecked)
    applyFiltersToUIState()
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

  /** Applies filters to all events and updates the UI state. */
  private fun applyFiltersToUIState() {
    val filteredEvents = applyFilters(allEvents)
    _uiState.value = _uiState.value.copy(events = filteredEvents)
  }

  /** Applies the current filters to the list of events. */
  private fun applyFilters(events: List<Event>): List<Event> {
    val state = uiState.value

    // If "All" is selected, return all events
    if (state.isAllSelected) return events

    var filteredEvents = events

    // Filter by event type (Social, Activity)
    val allowedTypes = mutableListOf<EventType>()
    if (state.isSocialSelected) allowedTypes.add(EventType.SOCIAL)
    if (state.isActivitySelected) allowedTypes.add(EventType.ACTIVITY)

    // Add SPORTS if any sport category is selected
    if (state.sportCategories.any { it.isChecked }) {
      allowedTypes.add(EventType.SPORTS)
    }

    // If no filters are selected, return empty list
    if (allowedTypes.isEmpty()) return emptyList()

    // Filter by event type
    filteredEvents = filteredEvents.filter { it.type in allowedTypes }

    // TODO: Add sport-specific filtering when sport metadata is added to Event model
    // For now, all SPORTS events pass through if any sport is selected

    return filteredEvents
  }
}
