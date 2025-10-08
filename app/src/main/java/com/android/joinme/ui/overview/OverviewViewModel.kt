package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the Overview screen.
 *
 * @property events A list of `Event` items to be displayed in the Overview screen. Defaults to an
 *   empty list if no items are available.
 * @property errorMsg An error message to be shown when fetching todos fails. `null` if no error is
 *   present.
 */
data class OverviewUIState(
    val events: List<Event> = emptyList(),
    val errorMsg: String? = null,
)

/**
 * ViewModel for the Overview screen.
 *
 * Responsible for managing the UI state, by fetching and providing Event items via the
 * [EventsRepository].
 *
 * @property eventRepository The repository used to fetch and manage Event items.
 */
class OverviewViewModel(
    private val eventRepository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true)
) : ViewModel() {

  private val _uiState = MutableStateFlow(OverviewUIState())
  val uiState: StateFlow<OverviewUIState> = _uiState.asStateFlow()

  /*init {
    //    Firebase.auth.addAuthStateListener {
    //      if (it.currentUser != null) {
    getAllEvents()
    //      }
    //    }
  }*/

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Refreshes the UI state by fetching all Event items from the repository. */
  fun refreshUIState() {
    getAllEvents()
  }

  /** Fetches all events from the repository and updates the UI state. */
  private fun getAllEvents() {
    viewModelScope.launch {
      try {
        val events = eventRepository.getAllEvents()
        _uiState.value = OverviewUIState(events = events)
      } catch (e: Exception) {
        Log.e("OverviewViewModel", "Error fetching events", e)
        setErrorMsg("Failed to load events: ${e.message}")
      }
    }
  }
}
