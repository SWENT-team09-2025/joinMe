package com.android.joinme.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.isExpired
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.model.serie.isExpired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the History screen.
 *
 * @property expiredItems A list of expired items (events or series) to be displayed in the History
 *   screen. Defaults to an empty list if no items are available.
 * @property isLoading Indicates whether the screen is currently loading data.
 * @property errorMsg An error message to be shown when fetching data fails. `null` if no error is
 *   present.
 */
data class HistoryUIState(
    val expiredItems: List<EventItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
)

/**
 * ViewModel for the History screen.
 *
 * Responsible for managing the UI state, by fetching and providing expired Event and Serie items
 * via their respective repositories.
 *
 * @property eventRepository The repository used to fetch and manage Event items.
 * @property serieRepository The repository used to fetch and manage Serie items.
 */
class HistoryViewModel(
    private val eventRepository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    private val serieRepository: SeriesRepository = SeriesRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(HistoryUIState())
  val uiState: StateFlow<HistoryUIState> = _uiState.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Refreshes the UI state by fetching all expired items from the repositories. */
  fun refreshUIState() {
    getExpiredItems()
  }

  /** Fetches all expired events and series from the repositories and updates the UI state. */
  private fun getExpiredItems() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        // Load all events and series
        val allEvents = eventRepository.getAllEvents(EventFilter.EVENTS_FOR_HISTORY_SCREEN)
        val allSeries = serieRepository.getAllSeries(SerieFilter.SERIES_FOR_HISTORY_SCREEN)

        // Identify events that belong to series
        val serieEventIds = allSeries.flatMap { it.eventIds }.toSet()

        // Filter out standalone events (events not in any serie)
        val standaloneEvents = allEvents.filterNot { it.eventId in serieEventIds }

        // Create EventItems
        val eventItems = standaloneEvents.map { EventItem.SingleEvent(it) }
        val serieItems = allSeries.map { EventItem.EventSerie(it) }

        // Combine all items
        val allItems = eventItems + serieItems

        // Filter expired items
        val expired =
            allItems
                .filter { item ->
                  when (item) {
                    is EventItem.SingleEvent -> item.event.isExpired()
                    is EventItem.EventSerie -> item.serie.isExpired(allEvents)
                  }
                }
                .sortedByDescending { it.date.toDate().time }

        _uiState.value = HistoryUIState(expiredItems = expired, isLoading = false)
      } catch (e: Exception) {
        Log.e("HistoryViewModel", "Error fetching expired items", e)
        setErrorMsg("Failed to load history: ${e.message}")
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }
}
