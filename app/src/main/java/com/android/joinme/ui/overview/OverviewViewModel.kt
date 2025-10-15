package com.android.joinme.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.isActive
import com.android.joinme.model.event.isUpcoming
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the Overview screen.
 *
 * @property ongoingEvents Events that have started (active or ongoing)
 * @property upcomingEvents Events that haven't started yet
 * @property isLoading Indicates whether the screen is currently loading data.
 * @property errorMsg An error message to be shown when fetching events fails
 */
data class OverviewUIState(
    val ongoingEvents: List<Event> = emptyList(),
    val upcomingEvents: List<Event> = emptyList(),
    val isLoading: Boolean = true,
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
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val allEvents = eventRepository.getAllEvents()

        val ongoing = allEvents.filter { it.isActive() }.sortedBy { it.date.toDate().time }

        val upcoming = allEvents.filter { it.isUpcoming() }.sortedBy { it.date.toDate().time }

        _uiState.value =
            OverviewUIState(ongoingEvents = ongoing, upcomingEvents = upcoming, isLoading = false)
      } catch (e: Exception) {
        setErrorMsg("Failed to load events: ${e.message}")
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }
}
