package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.streaks.StreakService
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.model.serie.getFormattedDuration
import com.android.joinme.model.serie.isExpired
import com.android.joinme.model.serie.isUpcoming
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
   * Gets the title of the serie.
   *
   * @return The title of the serie, or an empty string if the serie is null
   */
  fun getTitle(): String = serie?.title ?: ""

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
        } catch (_: Exception) {
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

  /** Checks if the serie is expired (last event has ended) */
  val isPastSerie: Boolean
    get() = serie?.isExpired() ?: false
}

/**
 * ViewModel for the SerieDetails screen.
 *
 * Manages loading and displaying serie details along with its associated events. Provides functions
 * for quitting the serie and handles data fetching from repositories.
 *
 * @property seriesRepository Repository for accessing serie data
 * @property eventsRepository Repository for accessing event data
 * @property profileRepository Repository for accessing user profile data
 */
class SerieDetailsViewModel(
    private val seriesRepository: SeriesRepository = SeriesRepositoryProvider.repository,
    private val eventsRepository: EventsRepository = EventsRepositoryProvider.getRepository(true),
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
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
        val serie = seriesRepository.getSerie(serieId)
        val serieEvents = eventsRepository.getEventsByIds(serie.eventIds)

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
   * Fetches the display name of the serie owner given their user ID.
   *
   * @param ownerId The user ID of the serie owner
   * @return The display name of the owner, or "UNKNOWN" if not found or if an error occurs
   */
  suspend fun getOwnerDisplayName(ownerId: String): String {
    if (ownerId.isEmpty()) {
      return "UNKNOWN"
    }
    return try {
      val profile = profileRepository.getProfile(ownerId)
      profile?.username ?: "UNKNOWN"
    } catch (_: Exception) {
      "UNKNOWN"
    }
  }

  /**
   * Adds the current user to the serie's participants list.
   *
   * For group series, also updates the user's streak via StreakService.
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
        val events = eventsRepository.getEventsByIds(serie.eventIds)
        events.forEach { event ->
          val updatedEventParticipants = (event.participants + currentUserId).distinct()
          val updatedEvent = event.copy(participants = updatedEventParticipants)
          eventsRepository.editEvent(event.eventId, updatedEvent)
        }
        val updatedSerie = serie.copy(participants = updatedParticipants)

        // Update in repository
        seriesRepository.editSerie(serie.serieId, updatedSerie)

        // Update streak for group series
        if (serie.groupId != null) {
          try {
            StreakService.onActivityJoined(serie.groupId, currentUserId, serie.date)
          } catch (e: Exception) {
            Log.e("SerieDetailsViewModel", "Error updating streak, rolling back", e)
            // Rollback serie
            seriesRepository.editSerie(serie.serieId, serie)
            // Rollback all events
            events.forEach { event ->
              try {
                eventsRepository.editEvent(event.eventId, event)
              } catch (rollbackEx: Exception) {
                Log.e(
                    "SerieDetailsViewModel",
                    "Failed to rollback event ${event.eventId}",
                    rollbackEx)
              }
            }
            setErrorMsg("Failed to update streak: ${e.message}")
            return false
          }
        }

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
   * For group series, also updates the user's streak via StreakService.
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
      val events = eventsRepository.getEventsByIds(serie.eventIds)
      events.forEach { event ->
        val updatedEventParticipants = event.participants.filter { it != currentUserId }
        val updatedEvent = event.copy(participants = updatedEventParticipants)
        eventsRepository.editEvent(event.eventId, updatedEvent)
      }
      val updatedSerie = serie.copy(participants = updatedParticipants)

      // Update in repository
      seriesRepository.editSerie(serie.serieId, updatedSerie)

      // Update streak for group series
      if (serie.groupId != null) {
        try {
          StreakService.onActivityLeft(serie.groupId, currentUserId, serie.date)
        } catch (e: Exception) {
          Log.e("SerieDetailsViewModel", "Error updating streak, rolling back", e)
          // Rollback serie
          seriesRepository.editSerie(serie.serieId, serie)
          // Rollback all events
          events.forEach { event ->
            try {
              eventsRepository.editEvent(event.eventId, event)
            } catch (rollbackEx: Exception) {
              Log.e(
                  "SerieDetailsViewModel", "Failed to rollback event ${event.eventId}", rollbackEx)
            }
          }
          setErrorMsg("Failed to update streak: ${e.message}")
          return false
        }
      }

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

  /**
   * Deletes the serie from the repository.
   *
   * For upcoming group series, also reverts streaks for all participants via StreakService.
   *
   * @param serieId The unique identifier of the serie to delete
   */
  suspend fun deleteSerie(serieId: String) {
    try {
      val serie = seriesRepository.getSerie(serieId)

      // Check if this is an upcoming group serie (for streak reversion)
      val isUpcomingGroupSerie = serie.groupId != null && serie.isUpcoming()

      seriesRepository.deleteSerie(serieId)

      // Revert streaks for upcoming group series
      if (isUpcomingGroupSerie) {
        try {
          StreakService.onActivityDeleted(serie.groupId, serie.participants, serie.date)
        } catch (e: Exception) {
          Log.e("SerieDetailsViewModel", "Error reverting streaks for deleted serie", e)
          // Non-critical: serie is already deleted, just log the error
        }
      }
    } catch (e: Exception) {
      setErrorMsg("Failed to delete serie: ${e.message}")
    }
  }
}
