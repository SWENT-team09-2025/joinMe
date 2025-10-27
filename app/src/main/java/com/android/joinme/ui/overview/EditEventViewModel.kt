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
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the EditEvent screen.
 *
 * @property type The event type (e.g., SPORTS, ACTIVITY, SOCIAL).
 * @property title The event title.
 * @property description The event description.
 * @property location The event location name.
 * @property maxParticipants The maximum number of participants as a string.
 * @property duration The event duration in minutes as a string.
 * @property date The event date in dd/MM/yyyy format.
 * @property time The event time in HH:mm format.
 * @property visibility The event visibility (PUBLIC or PRIVATE).
 * @property ownerId The ID of the event owner.
 * @property participants List of participant user IDs.
 * @property errorMsg Global error message for the form.
 * @property invalidTypeMsg Validation error message for the type field.
 * @property invalidTitleMsg Validation error message for the title field.
 * @property invalidDescriptionMsg Validation error message for the description field.
 * @property invalidLocationMsg Validation error message for the location field.
 * @property invalidMaxParticipantsMsg Validation error message for the max participants field.
 * @property invalidDurationMsg Validation error message for the duration field.
 * @property invalidDateMsg Validation error message for the date field.
 * @property invalidVisibilityMsg Validation error message for the visibility field.
 */
data class EditEventUIState(
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val maxParticipants: String = "",
    val duration: String = "",
    val date: String = "",
    val time: String = "",
    val visibility: String = "",
    val ownerId: String = "",
    val participants: List<String> = emptyList(),
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
    val invalidVisibilityMsg: String? = null,
) {
  /**
   * Indicates whether all form fields are valid.
   *
   * @return true if all validation messages are null and all required fields are non-blank.
   */
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
            selectedLocation != null &&
            maxParticipants.isNotBlank() &&
            duration.isNotBlank() &&
            date.isNotBlank() &&
            time.isNotBlank() &&
            visibility.isNotBlank()
}

/**
 * ViewModel for the EditEvent screen.
 *
 * Manages the state and business logic for editing existing events. Handles loading event data,
 * validating input fields, and persisting changes to the repository.
 *
 * @property repository The repository for event data operations.
 * @property initialState Optional initial state for the UI (primarily used for testing).
 */
class EditEventViewModel(
    private val repository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    private val locationRepository: LocationRepository =
        NominatimLocationRepository(HttpClientProvider.client),
    initialState: EditEventUIState = EditEventUIState()
) : ViewModel() {

  private val _uiState = MutableStateFlow(initialState)
  val uiState: StateFlow<EditEventUIState> = _uiState.asStateFlow()

  /** Clears the global error message. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Sets the global error message.
   *
   * @param msg The error message to display.
   */
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
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        _uiState.value =
            EditEventUIState(
                type = event.type.name,
                title = event.title,
                description = event.description,
                location = event.location?.name ?: "",
                maxParticipants = event.maxParticipants.toString(),
                duration = event.duration.toString(),
                date = dateFormat.format(event.date.toDate()),
                time = timeFormat.format(event.date.toDate()),
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
    val combinedDateTime = "${state.date} ${state.time}"
    val parsedDate =
        try {
          Timestamp(sdf.parse(combinedDateTime)!!)
        } catch (_: Exception) {
          null
        }

    if (parsedDate == null) {
      setErrorMsg("Invalid date or time format")
      return false
    }

    val event =
        Event(
            eventId = eventId,
            type = EventType.valueOf(state.type.uppercase(Locale.ROOT)),
            title = state.title,
            description = state.description,
            location = state.selectedLocation!!,
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

  /**
   * Updates the event type and validates it.
   *
   * Validates that the type is not blank and is one of the valid EventType values (SPORTS,
   * ACTIVITY, SOCIAL).
   *
   * @param type The event type to set.
   */
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

  /**
   * Updates the event title and validates it.
   *
   * Validates that the title is not blank.
   *
   * @param title The event title to set.
   */
  fun setTitle(title: String) {
    _uiState.value =
        _uiState.value.copy(
            title = title, invalidTitleMsg = if (title.isBlank()) "Title cannot be empty" else null)
  }

  /**
   * Updates the event description and validates it.
   *
   * Validates that the description is not blank.
   *
   * @param description The event description to set.
   */
  fun setDescription(description: String) {
    _uiState.value =
        _uiState.value.copy(
            description = description,
            invalidDescriptionMsg =
                if (description.isBlank()) "Description cannot be empty" else null)
  }

  /**
   * Updates the event location and validates it.
   *
   * Validates that the location is not blank.
   *
   * @param location The event location to set.
   */
  fun setLocation(location: String) {
    _uiState.value =
        _uiState.value.copy(
            location = location,
            invalidLocationMsg = if (location.isBlank()) "Must be a valid Location" else null)
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
  }

  /**
   * Updates the maximum number of participants and validates it.
   *
   * Validates that:
   * - The value is a positive number
   * - The value is greater than or equal to the current number of participants
   *
   * @param value The maximum number of participants as a string.
   */
  fun setMaxParticipants(value: String) {
    val num = value.toIntOrNull()
    val currentParticipantsCount = _uiState.value.participants.size
    _uiState.value =
        _uiState.value.copy(
            maxParticipants = value,
            invalidMaxParticipantsMsg =
                when {
                  num == null || num <= 0 -> "Must be a positive number"
                  num < currentParticipantsCount ->
                      "Cannot be less than current participants ($currentParticipantsCount)"
                  else -> null
                })
  }

  /**
   * Updates the event duration and validates it.
   *
   * Validates that the duration is a positive number.
   *
   * @param value The event duration in minutes as a string.
   */
  fun setDuration(value: String) {
    val num = value.toIntOrNull()
    _uiState.value =
        _uiState.value.copy(
            duration = value,
            invalidDurationMsg = if (num == null || num <= 0) "Must be a positive number" else null)
  }

  /**
   * Updates the event date and validates it.
   *
   * Validates that the date is in the correct format (dd/MM/yyyy).
   *
   * @param date The event date to set.
   */
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
            invalidDateMsg =
                if (!valid && date.isNotBlank()) "Invalid format (must be dd/MM/yyyy)" else null)
  }

  /**
   * Updates the event time.
   *
   * @param time The event time in HH:mm format.
   */
  fun setTime(time: String) {
    _uiState.value = _uiState.value.copy(time = time)
  }

  /**
   * Updates the event visibility and validates it.
   *
   * Validates that the visibility is not blank and is either PUBLIC or PRIVATE.
   *
   * @param visibility The event visibility to set.
   */
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
