package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.HttpClientProvider
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.map.Location
import com.android.joinme.model.map.LocationRepository
import com.android.joinme.model.map.NominatimLocationRepository
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
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
    val time: String = "",
    val visibility: String = "",
    val errorMsg: String? = null,
    val locationQuery: String = "",
    val locationSuggestions: List<Location> = emptyList(),
    val selectedLocation: Location? = null,

    // validation messages
    val invalidTypeMsg: String? = null,
    val invalidTitleMsg: String? = null,
    val invalidDescriptionMsg: String? = null,
    val invalidLocationMsg: String? = null,
    val invalidMaxParticipantsMsg: String? = null,
    val invalidDurationMsg: String? = null,
    val invalidDateMsg: String? = null,
    val invalidTimeMsg: String? = null,
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
            invalidTimeMsg == null &&
            invalidVisibilityMsg == null &&
            type.isNotBlank() &&
            title.isNotBlank() &&
            description.isNotBlank() &&
            selectedLocation != null &&
            maxParticipants.isNotBlank() &&
            duration.isNotBlank() &&
            date.isNotBlank() &&
            time.isNotBlank() &&
            visibility.isNotBlank()
}

/** ViewModel for the CreateEvent screen. */
class CreateEventViewModel(
    private val repository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    private val locationRepository: LocationRepository =
        NominatimLocationRepository(HttpClientProvider.client)
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

  /** Adds a new event to the repository. Suspends until the save is complete. */
  suspend fun createEvent(): Boolean {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return false
    }

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val combinedDateTime = "${state.date} ${state.time}"
    val parsedDate =
        try {
          Timestamp(sdf.parse(combinedDateTime)!!)
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
            location = state.selectedLocation!!,
            date = parsedDate,
            duration = state.duration.toInt(),
            participants = emptyList(),
            maxParticipants = state.maxParticipants.toInt(),
            visibility = EventVisibility.valueOf(state.visibility.uppercase(Locale.ROOT)),
            ownerId = Firebase.auth.currentUser?.uid ?: "unknown")

    return try {
      repository.addEvent(event)
      clearErrorMsg()
      true
    } catch (e: Exception) {
      Log.e("CreateEventViewModel", "Error creating event", e)
      setErrorMsg("Failed to create event: ${e.message}")
      false
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
                    "Type must be one of: SPORT, ACTIVITY, SOCIAL, etc."
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

  fun setLocationQuery(query: String) {
    _uiState.value = _uiState.value.copy(locationQuery = query)

    if (query.isNotEmpty()) {
      viewModelScope.launch {
        try {
          val results = locationRepository.search(query)
          _uiState.value = _uiState.value.copy(locationSuggestions = results)
        } catch (e: Exception) {
          Log.e("CreateEventVM", "Error fetching location suggestions", e)
          _uiState.value = _uiState.value.copy(locationSuggestions = emptyList())
        }
      }
    } else {
      _uiState.value = _uiState.value.copy(locationSuggestions = emptyList())
    }
  }

  fun selectLocation(loc: Location) {
    _uiState.value =
        _uiState.value.copy(
            selectedLocation = loc, locationQuery = loc.name, invalidLocationMsg = null)
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
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val valid =
        try {
          sdf.parse(date) != null
        } catch (_: Exception) {
          false
        }
    _uiState.value =
        _uiState.value.copy(
            date = date,
            invalidDateMsg = if (!valid) "Invalid format (must be dd/MM/yyyy)" else null)
    updateFormValidity()
  }

  fun setTime(time: String) {
    _uiState.value = _uiState.value.copy(time = time)
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
            state.invalidTimeMsg == null &&
            state.type.isNotBlank() &&
            state.title.isNotBlank() &&
            state.description.isNotBlank() &&
            state.selectedLocation != null &&
            state.maxParticipants.isNotBlank() &&
            state.duration.isNotBlank() &&
            state.date.isNotBlank() &&
            state.time.isNotBlank() &&
            state.visibility.isNotBlank()

    _uiState.value = state.copy(errorMsg = null)
    if (state.isValid != isValid) {
      _uiState.value = state.copy()
    }
  }
}
