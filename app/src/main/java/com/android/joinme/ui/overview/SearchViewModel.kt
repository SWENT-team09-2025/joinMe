package com.android.joinme.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.filter.FilterRepository
import com.android.joinme.model.filter.FilterState
import com.android.joinme.model.filter.FilteredEventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * UI state for the Search screen.
 *
 * @property query The current search query text
 * @property eventItems List of event items (events and series) to display after applying filters
 * @property errorMsg Error message to display if fetching events fails
 */
data class SearchUIState(
    val query: String = "",
    val eventItems: List<EventItem> = emptyList(),
    val errorMsg: String? = null,
)

/**
 * ViewModel for the Search screen.
 *
 * Manages the search query and delegates filter management to FilterRepository. Uses
 * FilteredEventsRepository to access centrally managed filtered events and series data. Filter
 * state is shared across the application through FilterRepository.
 *
 * @property filteredEventsRepository Repository for accessing filtered events and series data
 * @property filterRepository Repository for managing filter state (defaults to FilterRepository
 *   singleton)
 */
class SearchViewModel(
    private val filteredEventsRepository: FilteredEventsRepository =
        FilteredEventsRepository.getInstance(),
    private val filterRepository: FilterRepository = FilterRepository
) : ViewModel() {

  // Search UI state
  private val _uiState = MutableStateFlow(SearchUIState())
  val uiState: StateFlow<SearchUIState> = _uiState.asStateFlow()

  // Expose filter state from FilterRepository
  val filterState: StateFlow<FilterState> = filterRepository.filterState

  init {
    // Observe filtered events and series from repository and apply search query
    viewModelScope.launch {
      combine(filteredEventsRepository.filteredEvents, filteredEventsRepository.filteredSeries) {
              _,
              _ ->
            Unit
          }
          .collect { applySearchQueryToUIState() }
    }
    // Observe errors from repository
    viewModelScope.launch {
      filteredEventsRepository.errorMsg.collect { error ->
        _uiState.value = _uiState.value.copy(errorMsg = error)
      }
    }
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    filteredEventsRepository.clearErrorMsg()
  }

  /**
   * Updates the search query and re-applies it to the filtered events.
   *
   * @param query The new search query text
   */
  fun setQuery(query: String) {
    _uiState.value = _uiState.value.copy(query = query)
    applySearchQueryToUIState()
  }

  /** Toggles the "Social" filter. Delegates to FilterRepository. */
  fun toggleSocial() {
    filterRepository.toggleSocial()
  }

  /** Toggles the "Activity" filter. Delegates to FilterRepository. */
  fun toggleActivity() {
    filterRepository.toggleActivity()
  }

  /** Toggles the "Sport" filter. Delegates to FilterRepository. */
  fun toggleSport() {
    filterRepository.toggleSport()
  }

  /**
   * Refreshes the UI state by fetching all events and series from the repositories.
   *
   * This method triggers a fetch of all events and series from FilteredEventsRepository and applies
   * the current search query to update the displayed items list. It should be called when the
   * screen first loads or when the user wants to refresh the data.
   *
   * If fetching data fails, an error message is set in the UI state which can be displayed to the
   * user.
   */
  fun refreshUIState() {
    filteredEventsRepository.refresh()
  }

  /**
   * Applies search query to filtered events and series, then updates the UI state.
   *
   * This method combines filtered events and series into a single list of EventItems, applies
   * search query filters, and sorts the results by date. Events that belong to series are excluded
   * - only the serie card is shown for those events.
   *
   * Note: Category filters (Social, Activity, Sports) are already applied by
   * FilteredEventsRepository.
   */
  private fun applySearchQueryToUIState() {
    // Get already filtered events and series from repository
    val filteredEvents = filteredEventsRepository.filteredEvents.value
    val filteredSeries = filteredEventsRepository.filteredSeries.value

    // Identify events that belong to series
    val serieEventIds = filteredSeries.flatMap { it.eventIds }.toSet()

    // Filter out standalone events (events not in any serie)
    val standaloneEvents = filteredEvents.filterNot { it.eventId in serieEventIds }

    // Convert standalone events and filtered series to EventItems
    val eventItems = mutableListOf<EventItem>()
    eventItems.addAll(standaloneEvents.map { EventItem.SingleEvent(it) })
    eventItems.addAll(filteredSeries.map { EventItem.EventSerie(it) })

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
