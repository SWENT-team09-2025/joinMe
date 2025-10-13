package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Represents a sport category that can be filtered */
data class SportCategory(val id: String, val name: String, val isChecked: Boolean = false)

/** UI state for the Search screen. */
data class SearchUIState(
    val query: String = "",
    val isAllSelected: Boolean = true,
    val isSocialSelected: Boolean = true,
    val isActivitySelected: Boolean = true,
    val sportCategories: List<SportCategory> =
        listOf(
            SportCategory("basket", "Basket", isChecked = true),
            SportCategory("football", "Football", isChecked = true),
            SportCategory("tennis", "Tennis", isChecked = true),
            SportCategory("running", "Running", isChecked = true)),
    val categoryExpanded: Boolean = false,
    val events: List<Event> = emptyList(),
    val errorMsg: String? = null,
) {
  val selectedSportsCount: Int
    get() = sportCategories.count { it.isChecked }

  val isSelectAllChecked: Boolean
    get() = sportCategories.all { it.isChecked }
}

class SearchViewModel(
    private val eventRepository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true)
) : ViewModel() {
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

  /** Updates the search query. */
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

  /** Sets the category dropdown expanded state. */
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

  /** Toggles a specific sport filter by ID. */
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

  /** Sets the events list (for testing purposes). */
  fun setEvents(events: List<Event>) {
    Log.d("SearchViewModel", "setEvents: Setting ${events.size} events")
    allEvents = events
    applyFiltersToUIState()
  }

  fun refreshUIState() {
    getAllEvents()
  }

  /** Fetches all events from the repository and updates the UI state. */
  private fun getAllEvents() {
    viewModelScope.launch {
      try {
        allEvents = eventRepository.getAllEvents()
        applyFiltersToUIState()
      } catch (e: Exception) {
        Log.e("OverviewViewModel", "Error fetching events", e)
        setErrorMsg("Failed to load events: ${e.message}")
      }
    }
  }

  /** Applies filters to all events and updates the UI state. */
  private fun applyFiltersToUIState() {
    Log.d("SearchViewModel", "applyFiltersToUIState: allEvents size = ${allEvents.size}")
    val filteredEvents = applyFilters(allEvents)
    Log.d("SearchViewModel", "applyFiltersToUIState: filteredEvents size = ${filteredEvents.size}")
    _uiState.value = _uiState.value.copy(events = filteredEvents)
    Log.d("SearchViewModel", "applyFiltersToUIState: uiState.events size = ${_uiState.value.events.size}")
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
