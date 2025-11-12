package com.android.joinme.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.displayString
import com.android.joinme.model.event.isActive
import com.android.joinme.model.event.isUpcoming
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the ShowEvent screen. */
data class ShowEventUIState(
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val maxParticipants: String = "",
    val participantsCount: String = "",
    val duration: String = "",
    val date: String = "",
    val visibility: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val participants: List<String> = emptyList(),
    val isPastEvent: Boolean = false,
    val isPartOfASerie: Boolean = false,
    val serieId: String? = null,
    val errorMsg: String? = null,
) {
  /**
   * Checks if the given user ID is the owner of this event.
   *
   * @param userId The user ID to check.
   * @return True if the user is the owner, false otherwise.
   */
  fun isOwner(userId: String): Boolean = ownerId == userId

  /**
   * Checks if the given user ID is a participant in this event.
   *
   * @param userId The user ID to check.
   * @return True if the user is a participant, false otherwise.
   */
  fun isParticipant(userId: String): Boolean = participants.contains(userId)
}

/** ViewModel for the ShowEvent screen. */
class ShowEventViewModel(
    private val repository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    initialState: ShowEventUIState = ShowEventUIState()
) : ViewModel() {

  private val _uiState = MutableStateFlow(initialState)
  val uiState: StateFlow<ShowEventUIState> = _uiState.asStateFlow()

  /** Clears the global error message. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /**
   * Loads an Event by its ID and updates the UI state.
   *
   * @param eventId The ID of the Event to be loaded.
   * @param serieId Optional serie ID if the event belongs to a serie.
   */
  fun loadEvent(eventId: String, serieId: String? = null) {
    viewModelScope.launch {
      try {
        val event = repository.getEvent(eventId)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Format the date with event type prefix
        val formattedDate =
            "${event.type.displayString().uppercase()}: ${dateFormat.format(event.date.toDate())}"

        // Get owner name (you might want to fetch this from a user repository)
        val ownerDisplayName = "CREATED BY ${getOwnerDisplayName(event.ownerId)}"

        // Check if the event is past (not active and not upcoming)
        val isPast = !event.isActive() && !event.isUpcoming()

        _uiState.value =
            ShowEventUIState(
                type = event.type.displayString().uppercase(),
                title = event.title,
                description = event.description,
                location = event.location?.name ?: "",
                maxParticipants = event.maxParticipants.toString(),
                participantsCount = event.participants.size.toString(),
                duration = event.duration.toString(),
                date = formattedDate,
                visibility = event.visibility.displayString().uppercase(),
                ownerId = event.ownerId,
                ownerName = ownerDisplayName,
                participants = event.participants,
                isPastEvent = isPast,
                isPartOfASerie = event.isPartOfASerie,
                serieId = serieId)
      } catch (e: Exception) {
        setErrorMsg("Failed to load Event: ${e.message}")
      }
    }
  }

  /**
   * Gets the display name for the event owner. This is a placeholder that can be replaced with
   * actual user repository calls.
   *
   * @param ownerId The ID of the owner.
   * @return The display name for the owner.
   */
  private fun getOwnerDisplayName(ownerId: String): String {
    // TODO: Replace with actual user repository call to get user name
    // For now, return "YOU" if it's the current user, or extract name from ID
    return if (ownerId.isNotEmpty()) {
      ownerId.substringBefore("@").uppercase()
    } else {
      "UNKNOWN"
    }
  }

  /**
   * Toggles the current user's participation in the event (join or quit).
   *
   * @param eventId The ID of the event.
   * @param userId The ID of the current user.
   */
  suspend fun toggleParticipation(eventId: String, userId: String) {
    try {
      val event = repository.getEvent(eventId)
      val updatedParticipants =
          if (event.participants.contains(userId)) {
            // User is already a participant, remove them (quit)
            event.participants.filter { it != userId }
          } else {
            // User is not a participant, add them (join)
            if (event.participants.size >= event.maxParticipants) {
              setErrorMsg("Event is full. Cannot join.")
              return
            }
            event.participants + userId
          }

      // Update the event with new participants list
      val updatedEvent = event.copy(participants = updatedParticipants)
      repository.editEvent(eventId, updatedEvent)

      // Reload the event to update UI
      loadEvent(eventId)
      clearErrorMsg()
    } catch (e: Exception) {
      setErrorMsg("Failed to update participation: ${e.message}")
    }
  }

  /**
   * Deletes an Event document by its ID.
   *
   * @param eventId The ID of the Event document to be deleted.
   */
  suspend fun deleteEvent(eventId: String) {
    try {
      repository.deleteEvent(eventId)
      clearErrorMsg()
    } catch (e: Exception) {
      setErrorMsg("Failed to delete Event: ${e.message}")
    }
  }
}
