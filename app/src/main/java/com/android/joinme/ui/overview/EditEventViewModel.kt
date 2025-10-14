package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the EditEvent screen. */
data class EditEventUIState(
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val maxParticipants: String = "",
    val duration: String = "",
    val date: String = "",
    val visibility: String = "",
    val ownerId: String = "",
    val participants: List<String> = emptyList(),
    val errorMsg: String? = null,

    // validation messages
    val invalidTypeMsg: String? = null,
    val invalidTitleMsg: String? = null,
    val invalidDescriptionMsg: String? = null,
    val invalidLocationMsg: String? = null,
    val invalidMaxParticipantsMsg: String? = null,
    val invalidDurationMsg: String? = null,
    val invalidDateMsg: String? = null,
    val invalidVisibilityMsg: String? = null,
) {
  val isValid: Boolean
    get() =
        invalidTypeMsg == null &&
            invalidTitleMsg == null &&
            invalidDescriptionMsg == null &&
            invalidLocationMsg == null &&
            invalidMaxParticipantsMsg == null &&
            invalidDurationMsg == null &&
            invalidDateMsg == null &&
            invalidVisibilityMsg == null &&
            type.isNotBlank() &&
            title.isNotBlank() &&
            description.isNotBlank() &&
            location.isNotBlank() &&
            maxParticipants.isNotBlank() &&
            duration.isNotBlank() &&
            date.isNotBlank() &&
            visibility.isNotBlank()
}

/** ViewModel for the EditEvent screen. */
class EditEventViewModel(
    private val repository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    initialState: EditEventUIState = EditEventUIState()
) : ViewModel() {

  private val _uiState = MutableStateFlow(initialState)
  val uiState: StateFlow<EditEventUIState> = _uiState.asStateFlow()

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
   */
  fun loadEvent(eventId: String) {
    viewModelScope.launch {
      try {
        val event = repository.getEvent(eventId)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        _uiState.value =
            EditEventUIState(
                type = event.type.name,
                title = event.title,
                description = event.description,
                location = event.location?.name ?: "",
                maxParticipants = event.maxParticipants.toString(),
                duration = event.duration.toString(),
                date = dateFormat.format(event.date.toDate()),
                visibility = event.visibility.name,
                ownerId = event.ownerId,
                participants = event.participants)
      } catch (e: Exception) {
        Log.e("EditEventViewModel", "Error loading Event by ID: $eventId", e)
        setErrorMsg("Failed to load Event: ${e.message}")
      }
    }
  }

  /**
   * Edits an Event document.
   *
   * @param eventId The ID of the event to edit.
   * @return Boolean indicating success.
   */
  suspend fun editEvent(eventId: String): Boolean {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return false
    }

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val parsedDate =
        try {
          Timestamp(sdf.parse(state.date)!!)
        } catch (_: Exception) {
          null
        }

    if (parsedDate == null) {
      setErrorMsg("Invalid date format (must be dd/MM/yyyy HH:mm)")
      return false
    }

    val event =
        Event(
            eventId = eventId,
            type = EventType.valueOf(state.type.uppercase(Locale.ROOT)),
            title = state.title,
            description = state.description,
            location = Location(0.0, 0.0, state.location),
            date = parsedDate,
            duration = state.duration.toInt(),
            participants = state.participants,
            maxParticipants = state.maxParticipants.toInt(),
            visibility = EventVisibility.valueOf(state.visibility.uppercase(Locale.ROOT)),
            ownerId = state.ownerId)

    return try {
      repository.editEvent(eventId, event)
      clearErrorMsg()
      true
    } catch (e: Exception) {
      Log.e("EditEventViewModel", "Error editing event", e)
      setErrorMsg("Failed to edit event: ${e.message}")
      false
    }
  }

  /**
   * Deletes an Event document by its ID.
   *
   * @param eventId The ID of the Event document to be deleted.
   */
  fun deleteEvent(eventId: String) {
    viewModelScope.launch {
      try {
        repository.deleteEvent(eventId)
      } catch (e: Exception) {
        Log.e("EditEventViewModel", "Error deleting Event", e)
        setErrorMsg("Failed to delete Event: ${e.message}")
      }
    }
  }

  // Update functions for all fields

  fun setType(type: String) {
    val validTypes = EventType.values().map { it.name.uppercase(Locale.ROOT) }
    _uiState.value =
        _uiState.value.copy(
            type = type,
            invalidTypeMsg =
                if (type.isBlank()) "Type cannot be empty"
                else if (type.uppercase(Locale.ROOT) !in validTypes)
                    "Type must be one of: SPORTS, ACTIVITY, SOCIAL"
                else null)
  }

  fun setTitle(title: String) {
    _uiState.value =
        _uiState.value.copy(
            title = title, invalidTitleMsg = if (title.isBlank()) "Title cannot be empty" else null)
  }

  fun setDescription(description: String) {
    _uiState.value =
        _uiState.value.copy(
            description = description,
            invalidDescriptionMsg =
                if (description.isBlank()) "Description cannot be empty" else null)
  }

  fun setLocation(location: String) {
    _uiState.value =
        _uiState.value.copy(
            location = location,
            invalidLocationMsg = if (location.isBlank()) "Must be a valid Location" else null)
  }

  fun setMaxParticipants(value: String) {
    val num = value.toIntOrNull()
    _uiState.value =
        _uiState.value.copy(
            maxParticipants = value,
            invalidMaxParticipantsMsg =
                if (num == null || num <= 0) "Must be a positive number" else null)
  }

  fun setDuration(value: String) {
    val num = value.toIntOrNull()
    _uiState.value =
        _uiState.value.copy(
            duration = value,
            invalidDurationMsg = if (num == null || num <= 0) "Must be a positive number" else null)
  }

  fun setDate(date: String) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val valid =
        try {
          sdf.parse(date) != null
        } catch (_: Exception) {
          false
        }
    _uiState.value =
        _uiState.value.copy(
            date = date,
            invalidDateMsg = if (!valid) "Invalid format (must be dd/MM/yyyy HH:mm)" else null)
  }

  fun setVisibility(visibility: String) {
    val validVisibilities = listOf("PUBLIC", "PRIVATE")
    _uiState.value =
        _uiState.value.copy(
            visibility = visibility,
            invalidVisibilityMsg =
                if (visibility.isBlank()) "Event visibility cannot be empty"
                else if (visibility.uppercase(Locale.ROOT) !in validVisibilities)
                    "Visibility must be PUBLIC or PRIVATE"
                else null)
  }
}
