package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventType
import com.android.joinme.model.map.Location
import com.android.joinme.model.map.LocationRepository
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Base interface for event form UI state. */
interface EventFormUIState {
  val type: String
  val title: String
  val description: String
  val location: String
  val maxParticipants: String
  val duration: String
  val date: String
  val time: String
  val visibility: String
  val errorMsg: String?
  val locationQuery: String
  val locationSuggestions: List<Location>
  val selectedLocation: Location?
  val invalidTypeMsg: String?
  val invalidTitleMsg: String?
  val invalidDescriptionMsg: String?
  val invalidLocationMsg: String?
  val invalidMaxParticipantsMsg: String?
  val invalidDurationMsg: String?
  val invalidDateMsg: String?
  val invalidTimeMsg: String?
  val invalidVisibilityMsg: String?
}

/**
 * Base ViewModel for event form screens.
 *
 * Contains all shared validation and business logic for creating/editing events. Subclasses must
 * implement the abstract properties to provide their specific UI state management.
 */
abstract class BaseEventFormViewModel(protected val locationRepository: LocationRepository) :
    ViewModel() {

  /** The mutable state flow for the UI state. Subclasses must provide their specific state type. */
  protected abstract val _uiState: MutableStateFlow<out EventFormUIState>

  /** Gets the current UI state value. */
  protected abstract fun getState(): EventFormUIState

  /** Updates the UI state. Subclasses must implement to update their specific state type. */
  protected abstract fun updateState(transform: (EventFormUIState) -> EventFormUIState)

  /** Clears the global error message. */
  fun clearErrorMsg() {
    updateState { state ->
      when (state) {
        is CreateEventUIState -> state.copy(errorMsg = null)
        is EditEventUIState -> state.copy(errorMsg = null)
        else -> state
      }
    }
  }

  /** Sets the global error message. */
  protected fun setErrorMsg(msg: String) {
    updateState { state ->
      when (state) {
        is CreateEventUIState -> state.copy(errorMsg = msg)
        is EditEventUIState -> state.copy(errorMsg = msg)
        else -> state
      }
    }
  }

  /** Updates the event type and validates it. */
  fun setType(type: String) {
    val validTypes = EventType.values().map { it.name.uppercase(Locale.ROOT) }
    updateState { state ->
      when (state) {
        is CreateEventUIState ->
            state.copy(
                type = type,
                invalidTypeMsg =
                    if (type.isBlank()) "Type cannot be empty"
                    else if (type.uppercase(Locale.ROOT) !in validTypes)
                        "Type must be one of: SPORT, ACTIVITY, SOCIAL, etc."
                    else null)
        is EditEventUIState ->
            state.copy(
                type = type,
                invalidTypeMsg =
                    if (type.isBlank()) "Type cannot be empty"
                    else if (type.uppercase(Locale.ROOT) !in validTypes)
                        "Type must be one of: SPORTS, ACTIVITY, SOCIAL"
                    else null)
        else -> state
      }
    }
  }

  /** Updates the event title and validates it. */
  fun setTitle(title: String) {
    updateState { state ->
      when (state) {
        is CreateEventUIState ->
            state.copy(
                title = title,
                invalidTitleMsg = if (title.isBlank()) "Title cannot be empty" else null)
        is EditEventUIState ->
            state.copy(
                title = title,
                invalidTitleMsg = if (title.isBlank()) "Title cannot be empty" else null)
        else -> state
      }
    }
  }

  /** Updates the event description and validates it. */
  fun setDescription(description: String) {
    updateState { state ->
      when (state) {
        is CreateEventUIState ->
            state.copy(
                description = description,
                invalidDescriptionMsg =
                    if (description.isBlank()) "Description cannot be empty" else null)
        is EditEventUIState ->
            state.copy(
                description = description,
                invalidDescriptionMsg =
                    if (description.isBlank()) "Description cannot be empty" else null)
        else -> state
      }
    }
  }

  /** Updates the location query and fetches suggestions. */
  fun setLocationQuery(query: String) {
    updateState { state ->
      when (state) {
        is CreateEventUIState -> state.copy(locationQuery = query)
        is EditEventUIState -> state.copy(locationQuery = query)
        else -> state
      }
    }

    if (query.isNotEmpty()) {
      viewModelScope.launch {
        try {
          val results = locationRepository.search(query)
          updateState { state ->
            when (state) {
              is CreateEventUIState -> state.copy(locationSuggestions = results)
              is EditEventUIState -> state.copy(locationSuggestions = results)
              else -> state
            }
          }
        } catch (e: Exception) {
          Log.e("BaseEventFormVM", "Error fetching location suggestions", e)
          updateState { state ->
            when (state) {
              is CreateEventUIState -> state.copy(locationSuggestions = emptyList())
              is EditEventUIState -> state.copy(locationSuggestions = emptyList())
              else -> state
            }
          }
        }
      }
    } else {
      updateState { state ->
        when (state) {
          is CreateEventUIState -> state.copy(locationSuggestions = emptyList())
          is EditEventUIState -> state.copy(locationSuggestions = emptyList())
          else -> state
        }
      }
    }
  }

  /** Selects a location from suggestions. */
  fun selectLocation(loc: Location) {
    updateState { state ->
      when (state) {
        is CreateEventUIState ->
            state.copy(
                location = loc.name,
                selectedLocation = loc,
                locationQuery = loc.name,
                invalidLocationMsg = null)
        is EditEventUIState ->
            state.copy(
                location = loc.name,
                selectedLocation = loc,
                locationQuery = loc.name,
                invalidLocationMsg = null)
        else -> state
      }
    }
  }

  /** Updates the event duration and validates it. */
  fun setDuration(value: String) {
    val num = value.toIntOrNull()
    updateState { state ->
      when (state) {
        is CreateEventUIState ->
            state.copy(
                duration = value,
                invalidDurationMsg =
                    if (num == null || num <= 0) "Must be a positive number" else null)
        is EditEventUIState ->
            state.copy(
                duration = value,
                invalidDurationMsg =
                    if (num == null || num <= 0) "Must be a positive number" else null)
        else -> state
      }
    }
  }

  /** Updates the event date and validates it. */
  fun setDate(date: String) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val valid =
        try {
          sdf.parse(date) != null
        } catch (_: Exception) {
          false
        }
    updateState { state ->
      when (state) {
        is CreateEventUIState ->
            state.copy(
                date = date,
                invalidDateMsg = if (!valid) "Invalid format (must be dd/MM/yyyy)" else null)
        is EditEventUIState ->
            state.copy(
                date = date,
                invalidDateMsg =
                    if (!valid && date.isNotBlank()) "Invalid format (must be dd/MM/yyyy)"
                    else null)
        else -> state
      }
    }
  }

  /** Updates the event time. */
  fun setTime(time: String) {
    updateState { state ->
      when (state) {
        is CreateEventUIState -> state.copy(time = time)
        is EditEventUIState -> state.copy(time = time)
        else -> state
      }
    }
  }

  /** Updates the event visibility and validates it. */
  fun setVisibility(visibility: String) {
    val validVisibilities = listOf("PUBLIC", "PRIVATE")
    updateState { state ->
      when (state) {
        is CreateEventUIState ->
            state.copy(
                visibility = visibility,
                invalidVisibilityMsg =
                    if (visibility.isBlank()) "Event visibility cannot be empty"
                    else if (visibility.uppercase(Locale.ROOT) !in validVisibilities)
                        "Visibility must be PUBLIC or PRIVATE"
                    else null)
        is EditEventUIState ->
            state.copy(
                visibility = visibility,
                invalidVisibilityMsg =
                    if (visibility.isBlank()) "Event visibility cannot be empty"
                    else if (visibility.uppercase(Locale.ROOT) !in validVisibilities)
                        "Visibility must be PUBLIC or PRIVATE"
                    else null)
        else -> state
      }
    }
  }
}
