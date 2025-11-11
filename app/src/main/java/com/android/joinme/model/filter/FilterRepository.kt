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
 * @property isSocialSelected Whether the Social event type filter is selected
 * @property isActivitySelected Whether the Activity event type filter is selected
 * @property sportCategories List of sport categories with their selection states
 * @property selectedSportsCount Computed property returning the count of selected sports
 * @property isSelectAllChecked Computed property indicating if all sports are selected
 */
data class FilterState(
    val isSocialSelected: Boolean = false,
    val isActivitySelected: Boolean = false,
    val sportCategories: List<SportCategory> =
        Sports.ALL.map { SportCategory(it.id, it.name, isChecked = false) },
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
 * filter state and provides methods to toggle individual filters. When no filters are selected, all
 * events are shown by default.
 */
object FilterRepository {

  private val _filterState = MutableStateFlow(FilterState())
  val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

  /** Resets all filters to their default state (none selected, showing everything). */
  fun reset() {
    _filterState.value = FilterState()
  }

  /** Toggles the "Social" filter. */
  fun toggleSocial() {
    val state = _filterState.value
    val newSocialSelected = !state.isSocialSelected
    _filterState.value = state.copy(isSocialSelected = newSocialSelected)
  }

  /** Toggles the "Activity" filter. */
  fun toggleActivity() {
    val state = _filterState.value
    val newActivitySelected = !state.isActivitySelected
    _filterState.value = state.copy(isActivitySelected = newActivitySelected)
  }

  /** Toggles the "Select All" sports filter. */
  fun toggleSelectAll() {
    val state = _filterState.value
    val newSelectAllChecked = !state.isSelectAllChecked
    _filterState.value =
        state.copy(
            sportCategories =
                state.sportCategories.map { it.copy(isChecked = newSelectAllChecked) })
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

    _filterState.value = state.copy(sportCategories = updatedSports)
  }

  /**
   * Applies the current filters to a list of events.
   *
   * @param events The list of events to filter
   * @return The filtered list of events based on the current filter state. If no filters are
   *   selected, returns all events (default behavior).
   */
  fun applyFilters(events: List<Event>): List<Event> {
    val state = _filterState.value

    // Build list of allowed event types based on selected filters
    val allowedTypes = mutableListOf<EventType>()
    if (state.isSocialSelected) allowedTypes.add(EventType.SOCIAL)
    if (state.isActivitySelected) allowedTypes.add(EventType.ACTIVITY)

    // Add SPORTS if any sport category is selected
    if (state.sportCategories.any { it.isChecked }) {
      allowedTypes.add(EventType.SPORTS)
    }

    // If no filters are selected, return all events (default behavior)
    if (allowedTypes.isEmpty()) return events

    // Filter by event type
    val filteredEvents = events.filter { it.type in allowedTypes }

    // TODO: Add sport-specific filtering when sport metadata is added to Event model
    // For now, all SPORTS events pass through if any sport is selected

    return filteredEvents
  }
}
