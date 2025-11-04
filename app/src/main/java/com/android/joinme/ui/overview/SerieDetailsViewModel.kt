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
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the SerieDetails screen.
 *
 * Holds all data needed to display the serie details including the serie information, associated
 * events, and loading/error states.
 *
 * @property serie The Serie object to display, null if loading or error
 * @property events List of events associated with this serie
 * @property isLoading Indicates whether data is currently being loaded
 * @property errorMsg Error message to display if something went wrong
 * @property currentUserId The ID of the currently logged-in user
 */
data class SerieDetailsUIState(
    val serie: Serie? = null,
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = true,
    val errorMsg: String? = null,
    val currentUserId: String? = null
) {
  /**
   * Checks if the current user is the owner of the serie. Used to determine if the "Add event"
   * button should be shown.
   */
  val isOwner: Boolean
    get() = serie?.ownerId == currentUserId && currentUserId != null

  /**
   * Checks if the current user is a participant of the serie. Used to determine the value of the
   * button Join/Quit Serie.
   */
  val isParticipant: Boolean
    get() = serie?.participants?.contains(currentUserId) == true

  /**
   * Checks if the current user is not a participant of the serie and if they can actually join.
   * Used to determine the value of the button Join/Quit Serie.
   */
  val canJoin: Boolean
    get() =
        !isOwner &&
            !isParticipant &&
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
      return serie?.getFormattedDuration(events) ?: "0min"
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
    private val eventsRepository: EventsRepository = EventsRepositoryProvider.getRepository(true),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(SerieDetailsUIState())
  val uiState: StateFlow<SerieDetailsUIState> = _uiState.asStateFlow()

  init {
    _uiState.value = _uiState.value.copy(currentUserId = auth.currentUser?.uid)
  }

  /**
   * Loads the serie details and associated events from the repositories.
   *
   * This function performs the following steps:
   * 1. Sets loading state to true
   * 2. Fetches the serie from the series repository using the provided serieId
   * 3. Fetches all events from the events repository
   * 4. Filters events to only include those that belong to this serie
   * 5. Updates the UI state with the loaded data
   *
   * If any error occurs during loading, the error message is set in the UI state.
   *
   * @param serieId The unique identifier of the serie to load
   */
  fun loadSerieDetails(serieId: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)

      try {
        // Fetch the serie
        val serie = seriesRepository.getSerie(serieId)

        // TODO: PERFORMANCE OPTIMIZATION NEEDED
        // Currently fetching ALL events and filtering client-side
        // Solution: Add eventsRepository.getEventsBySerie(serieId) method to fetch only
        // events belonging to this serie from the backend
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
   * This function performs the following steps:
   * 1. Checks if the user is authenticated
   * 2. Validates that the user is not the owner
   * 3. Validates that the user is not already a participant
   * 4. Validates that the serie is not full
   * 5. Adds the user to the participants list
   * 6. Updates the serie in the repository
   *
   * @return True if the user successfully joined the serie, false otherwise
   */
  suspend fun joinSerie(): Boolean {
    val currentState = _uiState.value
    val userId = currentState.currentUserId
    val serie = currentState.serie

    if (userId == null) {
      setErrorMsg("You must be signed in to join a serie")
      return false
    }

    if (serie == null) {
      setErrorMsg("Serie not loaded")
      return false
    }

    // Validation checks
    if (userId == serie.ownerId) {
      setErrorMsg("You are the owner of this serie")
      return false
    }

    if (serie.participants.contains(userId)) {
      setErrorMsg("You are already a participant")
      return false
    }

    if (serie.participants.size >= serie.maxParticipants) {
      setErrorMsg("Serie is full")
      return false
    }

    return try {
      if (_uiState.value.canJoin) {
        // Add user to participants
        val updatedParticipants = serie.participants + userId
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
   * This function performs the following steps:
   * 1. Checks if the user is authenticated
   * 2. Checks if the serie is loaded
   * 3. Validates that the user is not the owner (owner cannot quit their own serie)
   * 4. Checks if the user is a participant
   * 5. Removes the user from the participants list
   * 6. Updates the serie in the repository with the new participants list
   *
   * @return True if the user successfully quit the serie, false otherwise
   */
  suspend fun quitSerie(): Boolean {
    val currentState = _uiState.value
    val userId = currentState.currentUserId
    val serie = currentState.serie

    if (userId == null) {
      setErrorMsg("You must be signed in to quit a serie")
      return false
    }

    if (serie == null) {
      setErrorMsg("Serie not loaded")
      return false
    }

    // Owner cannot quit their own serie
    if (userId == serie.ownerId) {
      setErrorMsg("You are the owner of this serie and cannot quit")
      return false
    }

    // Check if user is in the participants list
    if (userId !in serie.participants) {
      setErrorMsg("You are not a participant of this serie")
      return false
    }

    return try {
      // Remove user from participants
      val updatedParticipants = serie.participants.filter { it != userId }
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
