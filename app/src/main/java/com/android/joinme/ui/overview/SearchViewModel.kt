package com.android.joinme.ui.overview

import androidx.lifecycle.ViewModel
import com.android.joinme.model.event.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Represents a sport category that can be filtered */
data class SportCategory(val id: String, val name: String, val isChecked: Boolean = false)

/** UI state for the Search screen. */
data class SearchUIState(
    val query: String = "",
    val isAllSelected: Boolean = false,
    val isBarSelected: Boolean = false,
    val isClubSelected: Boolean = false,
    val sportCategories: List<SportCategory> =
        listOf(
            SportCategory("basket", "Basket"),
            SportCategory("football", "Football"),
            SportCategory("tennis", "Tennis"),
            SportCategory("running", "Running")),
    val categoryExpanded: Boolean = false,
    val events: List<Event> = emptyList()
) {
  val selectedSportsCount: Int
    get() = sportCategories.count { it.isChecked }

  val isSelectAllChecked: Boolean
    get() = sportCategories.all { it.isChecked }
}

class SearchViewModel : ViewModel() {
  // Search UI state
  private val _uiState = MutableStateFlow(SearchUIState())
  val uiState: StateFlow<SearchUIState> = _uiState.asStateFlow()

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
            isBarSelected = newAllSelected,
            isClubSelected = newAllSelected,
            sportCategories =
                _uiState.value.sportCategories.map { it.copy(isChecked = newAllSelected) })
  }

  /** Toggles the "Bar" filter. */
  fun toggleBar() {
    val state = _uiState.value
    val newBarSelected = !state.isBarSelected
    _uiState.value =
        state.copy(
            isBarSelected = newBarSelected,
            isAllSelected = newBarSelected && state.isClubSelected && state.isSelectAllChecked)
  }

  /** Toggles the "Club" filter. */
  fun toggleClub() {
    val state = _uiState.value
    val newClubSelected = !state.isClubSelected
    _uiState.value =
        state.copy(
            isClubSelected = newClubSelected,
            isAllSelected = state.isBarSelected && newClubSelected && state.isSelectAllChecked)
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
            isAllSelected = newSelectAllChecked && state.isBarSelected && state.isClubSelected)
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
            isAllSelected = state.isBarSelected && state.isClubSelected && allSportsChecked)
  }

  /** Sets the events list (for testing purposes). */
  fun setEvents(events: List<Event>) {
    _uiState.value = _uiState.value.copy(events = events)
  }
}
