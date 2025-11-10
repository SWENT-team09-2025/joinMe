package com.android.joinme.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.model.serie.getFormattedDuration
import com.android.joinme.model.serie.getSerieEvents
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the SerieDetails screen. ...
 *
 * @property serie The Serie object to display, null if loading or error
 * @property events List of events associated with this serie
 * @property isLoading Indicates whether data is currently being loaded
 * @property errorMsg Error message to display if something went wrong // REMOVED: @property
 *   currentUserId The ID of the currently logged-in user
 */
data class SerieDetailsUIState(
    val serie: Serie? = null,
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = true,
    val errorMsg: String? = null
) {
  /**
   * Checks if the current user is the owner of the serie.
   *
   * @param currentUserId The ID of the user to check.
   */
  fun isOwner(currentUserId: String?): Boolean =
      serie?.ownerId == currentUserId && currentUserId != null

  /**
   * Checks if the current user is a participant of the serie.
   *
   * @param currentUserId The ID of the user to check.
   */
  fun isParticipant(currentUserId: String?): Boolean =
      serie?.participants?.contains(currentUserId) == true

  /**
   * Checks if the current user is not a participant of the serie and if they can actually join.
   *
   * @param currentUserId The ID of the user to check.
   */
  fun canJoin(currentUserId: String?): Boolean =
      !isOwner(currentUserId) &&
          !isParticipant(currentUserId) &&
          (serie?.participants?.size ?: 0) < (serie?.maxParticipants ?: 0)

  /**
   * Formats the serie date and time for display. Returns a string in format "dd/MM/yyyy at HH:mm"
   */
  val formattedDateTime: String
    get() {
      return serie?.date?.let { timestamp ->
        try {
          val dateFormat = SimpleDateFormat("dd/MM/yyyy 'at' HH:mm", Locale.getDefault())
          dateFormat.format(timestamp.toDate())
        } catch (e: Exception) {
          ""
        }
      } ?: ""
    }

  /**
   * Gets the formatted total duration of all events in the serie. Returns a string like "5h 30min",
   * "5h", or "90min"
   */
  val formattedDuration: String
    get() {
      return serie?.getFormattedDuration() ?: "0min"
    }

  /** Gets the participant count string in format "current/max" */
  val participantsCount: String
    get() {
      return serie?.let { "${it.participants.size}/${it.maxParticipants}" } ?: "0/0"
    }

  /** Gets the visibility display string (PUBLIC or PRIVATE) */
  val visibilityDisplay: String
    get() = serie?.visibility?.name ?: "PUBLIC"
}

/**
 * ViewModel for the SerieDetails screen.
 *
 * Manages loading and displaying serie details along with its associated events. Provides functions
 * for quitting the serie and handles data fetching from repositories.
 *
 * @property seriesRepository Repository for accessing serie data
 * @property eventsRepository Repository for accessing event data
 * @property auth Firebase authentication instance for user identification
 */
class SerieDetailsViewModel(
    private val seriesRepository: SeriesRepository = SeriesRepositoryProvider.repository,
    private val eventsRepository: EventsRepository = EventsRepositoryProvider.getRepository(true)
) : ViewModel() {

  private val _uiState = MutableStateFlow(SerieDetailsUIState())
  val uiState: StateFlow<SerieDetailsUIState> = _uiState.asStateFlow()

  /**
   * Loads the serie details and associated events from the repositories. ...
   *
   * @param serieId The unique identifier of the serie to load
   */
  fun loadSerieDetails(serieId: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)

      try {
        // ... (this logic is unchanged)
        val serie = seriesRepository.getSerie(serieId)
        val allEvents = eventsRepository.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)
        val serieEvents = serie.getSerieEvents(allEvents)

        _uiState.value =
            _uiState.value.copy(
                serie = serie, events = serieEvents, isLoading = false, errorMsg = null)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(isLoading = false, errorMsg = "Failed to load serie: ${e.message}")
      }
    }
  }

  /**
   * Adds the current user to the serie's participants list.
   *
   * @param currentUserId The ID of the user trying to join.
   * @return True if the user successfully joined the serie, false otherwise
   */
  suspend fun joinSerie(currentUserId: String): Boolean {
    val currentState = _uiState.value
    val serie = currentState.serie

    if (serie == null) {
      setErrorMsg("Serie not loaded")
      return false
    }

    // Validation checks
    if (currentUserId == serie.ownerId) {
      setErrorMsg("You are the owner of this serie")
      return false
    }

    if (serie.participants.contains(currentUserId)) {
      setErrorMsg("You are already a participant")
      return false
    }

    if (serie.participants.size >= serie.maxParticipants) {
      setErrorMsg("Serie is full")
      return false
    }

    return try {
      // Add user to participants
      if (_uiState.value.canJoin(currentUserId)) {
        val updatedParticipants = serie.participants + currentUserId
        val updatedSerie = serie.copy(participants = updatedParticipants)

        // Update in repository
        seriesRepository.editSerie(serie.serieId, updatedSerie)

        // Update local state
        _uiState.value = _uiState.value.copy(serie = updatedSerie, errorMsg = null)
        true
      } else false
    } catch (e: Exception) {
      setErrorMsg("Failed to join serie: ${e.message}")
      false
    }
  }

  /**
   * Removes the current user from the serie's participants list.
   *
   * @param currentUserId The ID of the user trying to quit.
   * @return True if the user successfully quit the serie, false otherwise
   */
  suspend fun quitSerie(currentUserId: String): Boolean {
    val currentState = _uiState.value
    val serie = currentState.serie

    if (serie == null) {
      setErrorMsg("Serie not loaded")
      return false
    }

    // Owner cannot quit their own serie
    if (currentUserId == serie.ownerId) {
      setErrorMsg("You are the owner of this serie and cannot quit")
      return false
    }

    // Check if user is in the participants list
    if (currentUserId !in serie.participants) {
      setErrorMsg("You are not a participant of this serie")
      return false
    }

    return try {
      // Remove user from participants
      val updatedParticipants = serie.participants.filter { it != currentUserId }
      val updatedSerie = serie.copy(participants = updatedParticipants)

      // Update in repository
      seriesRepository.editSerie(serie.serieId, updatedSerie)

      // Update local state
      _uiState.value = _uiState.value.copy(serie = updatedSerie, errorMsg = null)
      true
    } catch (e: Exception) {
      setErrorMsg("Failed to quit serie: ${e.message}")
      false
    }
  }

  /**
   * Sets an error message in the UI state.
   *
   * @param msg The error message to display
   */
  fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /**
   * Clears the error message from the UI state. Typically called after displaying an error to the
   * user.
   */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }
}
