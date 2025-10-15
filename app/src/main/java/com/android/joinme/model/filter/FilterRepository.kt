package com.android.joinme.model.filter

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.sport.Sports
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a sport category that can be filtered.
 *
 * @property id Unique identifier for the sport
 * @property name Display name of the sport
 * @property isChecked Whether the sport category is currently selected
 */
data class SportCategory(val id: String, val name: String, val isChecked: Boolean = false)

/**
 * Represents the current state of event filters.
 *
 * @property isAllSelected Whether all filters (Social, Activity, and all sports) are selected
 * @property isSocialSelected Whether the Social event type filter is selected
 * @property isActivitySelected Whether the Activity event type filter is selected
 * @property sportCategories List of sport categories with their selection states
 * @property selectedSportsCount Computed property returning the count of selected sports
 * @property isSelectAllChecked Computed property indicating if all sports are selected
 */
data class FilterState(
    val isAllSelected: Boolean = true,
    val isSocialSelected: Boolean = true,
    val isActivitySelected: Boolean = true,
    val sportCategories: List<SportCategory> =
        Sports.ALL.map { SportCategory(it.id, it.name, isChecked = true) },
) {
  val selectedSportsCount: Int
    get() = sportCategories.count { it.isChecked }

  val isSelectAllChecked: Boolean
    get() = sportCategories.all { it.isChecked }
}

/**
 * Repository for managing event filter state across the application.
 *
 * This singleton provides a centralized source of truth for event filters (Social, Activity,
 * Sports) that can be shared across multiple screens (e.g., SearchScreen, MapScreen). It maintains
 * filter state and provides methods to toggle individual filters while keeping the "All" filter in
 * sync.
 */
object FilterRepository {

  private val _filterState = MutableStateFlow(FilterState())
  val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

  /** Resets all filters to their default state (all selected). */
  fun reset() {
    _filterState.value = FilterState()
  }

  /** Toggles the "All" filter and updates related filters. */
  fun toggleAll() {
    val newAllSelected = !_filterState.value.isAllSelected
    _filterState.value =
        _filterState.value.copy(
            isAllSelected = newAllSelected,
            isSocialSelected = newAllSelected,
            isActivitySelected = newAllSelected,
            sportCategories =
                _filterState.value.sportCategories.map { it.copy(isChecked = newAllSelected) })
  }

  /** Toggles the "Social" filter. */
  fun toggleSocial() {
    val state = _filterState.value
    val newSocialSelected = !state.isSocialSelected
    _filterState.value =
        state.copy(
            isSocialSelected = newSocialSelected,
            isAllSelected =
                newSocialSelected && state.isActivitySelected && state.isSelectAllChecked)
  }

  /** Toggles the "Activity" filter. */
  fun toggleActivity() {
    val state = _filterState.value
    val newActivitySelected = !state.isActivitySelected
    _filterState.value =
        state.copy(
            isActivitySelected = newActivitySelected,
            isAllSelected =
                state.isSocialSelected && newActivitySelected && state.isSelectAllChecked)
  }

  /** Toggles the "Select All" sports filter. */
  fun toggleSelectAll() {
    val state = _filterState.value
    val newSelectAllChecked = !state.isSelectAllChecked
    _filterState.value =
        state.copy(
            sportCategories =
                state.sportCategories.map { it.copy(isChecked = newSelectAllChecked) },
            isAllSelected =
                newSelectAllChecked && state.isSocialSelected && state.isActivitySelected)
  }

  /**
   * Toggles a specific sport filter by ID.
   *
   * @param sportId The unique identifier of the sport to toggle
   */
  fun toggleSport(sportId: String) {
    val state = _filterState.value
    val updatedSports =
        state.sportCategories.map { sport ->
          if (sport.id == sportId) sport.copy(isChecked = !sport.isChecked) else sport
        }
    val allSportsChecked = updatedSports.all { it.isChecked }

    _filterState.value =
        state.copy(
            sportCategories = updatedSports,
            isAllSelected = state.isSocialSelected && state.isActivitySelected && allSportsChecked)
  }

  /**
   * Applies the current filters to a list of events.
   *
   * @param events The list of events to filter
   * @return The filtered list of events based on the current filter state
   */
  fun applyFilters(events: List<Event>): List<Event> {
    val state = _filterState.value

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
