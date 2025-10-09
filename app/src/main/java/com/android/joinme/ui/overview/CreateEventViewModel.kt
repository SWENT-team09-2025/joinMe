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
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the CreateEvent screen. */
data class CreateEventUIState(
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val maxParticipants: String = "",
    val duration: String = "",
    val date: String = "",
    val visibility: String = "",
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

/** ViewModel for the CreateEvent screen. */
class CreateEventViewModel(
    private val repository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = false)
) : ViewModel() {

  private val _uiState = MutableStateFlow(CreateEventUIState())
  val uiState: StateFlow<CreateEventUIState> = _uiState.asStateFlow()

  /** Clears the global error message. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /** Adds a new event to the repository. */
  fun createEvent(): Boolean {
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
            eventId = repository.getNewEventId(),
            type = EventType.valueOf(state.type.uppercase(Locale.ROOT)),
            title = state.title,
            description = state.description,
            location = Location(0.0, 0.0, state.location),
            date = parsedDate,
            duration = state.duration.toInt(),
            participants = emptyList(),
            maxParticipants = state.maxParticipants.toInt(),
            visibility = EventVisibility.valueOf(state.visibility.uppercase(Locale.ROOT)),
            ownerId = "placeholderUser")

    viewModelScope.launch {
      try {
        repository.addEvent(event)
        clearErrorMsg()
      } catch (e: Exception) {
        Log.e("CreateEventViewModel", "Error creating event", e)
        setErrorMsg("Failed to create event: ${e.message}")
      }
    }

    return true
  }

  // Update functions for all fields

  fun setType(type: String) {
    val validTypes = listOf("SPORTS", "SOCIAL", "OTHER")
    _uiState.value =
        _uiState.value.copy(
            type = type,
            invalidTypeMsg =
                if (type.isBlank()) "Type cannot be empty"
                else if (type.uppercase(Locale.ROOT) !in validTypes)
                    "Type must be one of: SPORT, SOCIAL, OTHER"
                else null)
    updateFormValidity()
  }

  fun setTitle(title: String) {
    _uiState.value =
        _uiState.value.copy(
            title = title, invalidTitleMsg = if (title.isBlank()) "Title cannot be empty" else null)
    updateFormValidity()
  }

  fun setDescription(description: String) {
    _uiState.value =
        _uiState.value.copy(
            description = description,
            invalidDescriptionMsg =
                if (description.isBlank()) "Description cannot be empty" else null)
    updateFormValidity()
  }

  fun setLocation(location: String) {
    _uiState.value =
        _uiState.value.copy(
            location = location,
            invalidLocationMsg = if (location.isBlank()) "Must be a valid Location" else null)
    updateFormValidity()
  }

  fun setMaxParticipants(value: String) {
    val num = value.toIntOrNull()
    _uiState.value =
        _uiState.value.copy(
            maxParticipants = value,
            invalidMaxParticipantsMsg =
                if (num == null || num <= 0) "Must be a positive number" else null)
    updateFormValidity()
  }

  fun setDuration(value: String) {
    val num = value.toIntOrNull()
    _uiState.value =
        _uiState.value.copy(
            duration = value,
            invalidDurationMsg = if (num == null || num <= 0) "Must be a positive number" else null)
    updateFormValidity()
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
    updateFormValidity()
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
    updateFormValidity()
  }

  private fun updateFormValidity() {
    val state = _uiState.value
    val isValid =
        state.invalidTypeMsg == null &&
            state.invalidTitleMsg == null &&
            state.invalidDescriptionMsg == null &&
            state.invalidLocationMsg == null &&
            state.invalidMaxParticipantsMsg == null &&
            state.invalidDurationMsg == null &&
            state.invalidDateMsg == null &&
            state.invalidVisibilityMsg == null &&
            state.type.isNotBlank() &&
            state.title.isNotBlank() &&
            state.description.isNotBlank() &&
            state.location.isNotBlank() &&
            state.maxParticipants.isNotBlank() &&
            state.duration.isNotBlank() &&
            state.date.isNotBlank() &&
            state.visibility.isNotBlank()

    _uiState.value = state.copy(errorMsg = null) // clear any old error
    if (state.isValid != isValid) {
      _uiState.value = state.copy() // trigger recomposition
    }
  }
}
