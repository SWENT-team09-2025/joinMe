package com.android.joinme.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.isExpired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the History screen.
 *
 * @property expiredEvents A list of expired `Event` items to be displayed in the History screen.
 *   Defaults to an empty list if no items are available.
 * @property errorMsg An error message to be shown when fetching events fails. `null` if no error is
 *   present.
 */
data class HistoryUIState(
    val expiredEvents: List<Event> = emptyList(),
    val errorMsg: String? = null,
)

/**
 * ViewModel for the History screen.
 *
 * Responsible for managing the UI state, by fetching and providing expired Event items via the
 * [EventsRepository].
 *
 * @property eventRepository The repository used to fetch and manage Event items.
 */
class HistoryViewModel(
    private val eventRepository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true)
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

  /** Refreshes the UI state by fetching all expired Event items from the repository. */
  fun refreshUIState() {
    getExpiredEvents()
  }

  /** Fetches all expired events from the repository and updates the UI state. */
  private fun getExpiredEvents() {
    viewModelScope.launch {
      try {
        val allEvents = eventRepository.getAllEvents()

        val expired =
            allEvents.filter { it.isExpired() }.sortedByDescending { it.date.toDate().time }

        _uiState.value = HistoryUIState(expiredEvents = expired)
      } catch (e: Exception) {
        Log.e("HistoryViewModel", "Error fetching expired events", e)
        setErrorMsg("Failed to load history: ${e.message}")
      }
    }
  }
}
