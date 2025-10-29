package com.android.joinme.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.isActive
import com.android.joinme.model.event.isUpcoming
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.model.serie.isActive
import com.android.joinme.model.serie.isUpcoming
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the Overview screen.
 *
 * @property ongoingItems Items (events or series) that are currently active or ongoing
 * @property upcomingItems Items (events or series) that haven't started yet
 * @property isLoading Indicates whether the screen is currently loading data
 * @property errorMsg An error message to be shown when fetching data fails
 */
data class OverviewUIState(
    val ongoingItems: List<EventItem> = emptyList(),
    val upcomingItems: List<EventItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
)

/**
 * ViewModel for the Overview screen.
 *
 * Responsible for managing the UI state by fetching and providing Event and Serie items via their
 * respective repositories.
 *
 * @property eventRepository The repository used to fetch and manage Event items
 * @property serieRepository The repository used to fetch and manage Serie items
 */
class OverviewViewModel(
    private val eventRepository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    private val serieRepository: SeriesRepository = SeriesRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(OverviewUIState())
  val uiState: StateFlow<OverviewUIState> = _uiState.asStateFlow()

  init {
    loadAllData()
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Refreshes the UI state by fetching all data from repositories. */
  fun refreshUIState() {
    loadAllData()
  }

  /** Fetches all events and series from repositories and updates the UI state. */
  private fun loadAllData() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        // Load all events and series
        val allEvents = eventRepository.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)
        val allSeries = serieRepository.getAllSeries(SerieFilter.SERIES_TESTS)

        // Identify events that belong to series
        val serieEventIds = allSeries.flatMap { it.eventIds }.toSet()

        // Filter out standalone events (events not in any serie)
        val standaloneEvents = allEvents.filterNot { it.eventId in serieEventIds }

        // Create EventItems
        val eventItems = standaloneEvents.map { EventItem.SingleEvent(it) }
        val serieItems = allSeries.map { EventItem.EventSerie(it) }

        // Combine all items
        val allItems = eventItems + serieItems

        // Filter ongoing items
        val ongoing =
            allItems
                .filter { item ->
                  when (item) {
                    is EventItem.SingleEvent -> item.event.isActive()
                    is EventItem.EventSerie -> item.serie.isActive(allEvents)
                  }
                }
                .sortedBy { it.date.toDate().time }

        // Filter upcoming items
        val upcoming =
            allItems
                .filter { item ->
                  when (item) {
                    is EventItem.SingleEvent -> item.event.isUpcoming()
                    is EventItem.EventSerie -> item.serie.isUpcoming(allEvents)
                  }
                }
                .sortedBy { it.date.toDate().time }

        _uiState.value =
            OverviewUIState(ongoingItems = ongoing, upcomingItems = upcoming, isLoading = false)
      } catch (e: Exception) {
        setErrorMsg("Failed to load data: ${e.message}")
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }
}
