package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.map.Location
import com.android.joinme.model.map.LocationRepository
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow

/** Note: This file was created with the help of IA (Claude) */

/** Base interface for event for serie form UI state. */
interface EventForSerieFormUIState {
  val type: String
  val title: String
  val description: String
  val duration: String
  val location: String
  val locationQuery: String
  val locationSuggestions: List<Location>
  val selectedLocation: Location?
  val isLoading: Boolean
  val errorMsg: String?
  val serieHasGroup: Boolean
  val invalidTypeMsg: String?
  val invalidTitleMsg: String?
  val invalidDescriptionMsg: String?
  val invalidDurationMsg: String?
  val invalidLocationMsg: String?
}

/**
 * Concrete UI state implementation for event-for-serie form screens.
 *
 * Used by both CreateEventForSerieViewModel and EditEventForSerieViewModel.
 *
 * @property type The event type (SPORTS, ACTIVITY, or SOCIAL)
 * @property title The event title
 * @property description The event description
 * @property duration The event duration in minutes as a string
 * @property location The event location name
 * @property locationQuery The current location search query
 * @property locationSuggestions List of location suggestions from search
 * @property selectedLocation The selected Location object
 * @property isLoading Indicates whether the event is currently being saved
 * @property errorMsg Global error message for the form
 * @property serieHasGroup Indicates whether the serie is associated with a group
 * @property invalidTypeMsg Validation message for the type field
 * @property invalidTitleMsg Validation message for the title field
 * @property invalidDescriptionMsg Validation message for the description field
 * @property invalidDurationMsg Validation message for the duration field
 * @property invalidLocationMsg Validation message for the location field
 */
data class EventForSerieFormState(
    override val type: String = "",
    override val title: String = "",
    override val description: String = "",
    override val duration: String = "",
    override val location: String = "",
    override val locationQuery: String = "",
    override val locationSuggestions: List<Location> = emptyList(),
    override val selectedLocation: Location? = null,
    override val isLoading: Boolean = false,
    override val errorMsg: String? = null,
    override val serieHasGroup: Boolean = false,

    // validation messages
    override val invalidTypeMsg: String? = null,
    override val invalidTitleMsg: String? = null,
    override val invalidDescriptionMsg: String? = null,
    override val invalidDurationMsg: String? = null,
    override val invalidLocationMsg: String? = null,
) : EventForSerieFormUIState {
  /**
   * Checks if all form fields are valid and filled.
   *
   * When serie has a group, type is auto-filled and doesn't need manual input.
   *
   * @return True if all validation messages are null and all fields are not blank
   */
  val isValid: Boolean
    get() =
        invalidTypeMsg == null &&
            invalidTitleMsg == null &&
            invalidDescriptionMsg == null &&
            invalidDurationMsg == null &&
            invalidLocationMsg == null &&
            (serieHasGroup || type.isNotBlank()) &&
            title.isNotBlank() &&
            description.isNotBlank() &&
            duration.isNotBlank() &&
            selectedLocation != null
}

/**
 * Base ViewModel for event for serie form screens.
 *
 * Contains all shared validation and business logic for creating/editing events within a serie.
 * Subclasses must implement the abstract properties to provide their specific UI state management.
 */
abstract class BaseEventForSerieViewModel(
    protected val locationRepository: LocationRepository,
    protected val serieRepository: SeriesRepository,
    protected val groupRepository: GroupRepository
) : ViewModel() {

  /** The mutable state flow for the UI state. Subclasses must provide their specific state type. */
  protected abstract val _uiState: MutableStateFlow<out EventForSerieFormUIState>

  /** Gets the current UI state value. */
  protected abstract fun getState(): EventForSerieFormUIState

  /** Updates the UI state. Subclasses must implement to update their specific state type. */
  protected abstract fun updateState(
      transform: (EventForSerieFormUIState) -> EventForSerieFormUIState
  )

  /** Clears the global error message. */
  fun clearErrorMsg() {
    updateState { state ->
      when (state) {
        is EventForSerieFormState -> state.copy(errorMsg = null)
        else -> state
      }
    }
  }

  /** Sets the global error message. */
  protected fun setErrorMsg(msg: String) {
    updateState { state ->
      when (state) {
        is EventForSerieFormState -> state.copy(errorMsg = msg)
        else -> state
      }
    }
  }

  /**
   * Updates the event type and validates it.
   *
   * @param type The new type value (SPORTS, ACTIVITY, or SOCIAL)
   */
  fun setType(type: String) {
    val validTypes = EventType.values().map { it.name.uppercase(Locale.ROOT) }
    val invalidMsg =
        if (type.isBlank()) "Event type cannot be empty"
        else if (type.uppercase(Locale.ROOT) !in validTypes)
            "Type must be one of: SPORTS, ACTIVITY, SOCIAL, etc."
        else null
    updateState { state ->
      when (state) {
        is EventForSerieFormState -> state.copy(type = type, invalidTypeMsg = invalidMsg)
        else -> state
      }
    }
  }

  /**
   * Updates the event title and validates it.
   *
   * @param title The new title value
   */
  fun setTitle(title: String) {
    val invalidMsg = if (title.isBlank()) "Title cannot be empty" else null
    updateState { state ->
      when (state) {
        is EventForSerieFormState -> state.copy(title = title, invalidTitleMsg = invalidMsg)
        else -> state
      }
    }
  }

  /**
   * Updates the event description and validates it.
   *
   * @param description The new description value
   */
  fun setDescription(description: String) {
    val invalidMsg = if (description.isBlank()) "Description cannot be empty" else null
    updateState { state ->
      when (state) {
        is EventForSerieFormState ->
            state.copy(description = description, invalidDescriptionMsg = invalidMsg)
        else -> state
      }
    }
  }

  /**
   * Updates the event duration and validates it.
   *
   * @param value The new duration value in minutes as a string
   */
  fun setDuration(value: String) {
    val num = value.toIntOrNull()
    val invalidMsg = if (num == null || num <= 0) "Must be a positive number" else null
    updateState { state ->
      when (state) {
        is EventForSerieFormState -> state.copy(duration = value, invalidDurationMsg = invalidMsg)
        else -> state
      }
    }
  }

  /**
   * Updates the location query field.
   *
   * @param query The new location query value
   */
  fun setLocationQuery(query: String) {
    updateState { state ->
      when (state) {
        is EventForSerieFormState -> state.copy(locationQuery = query)
        else -> state
      }
    }
  }

  /**
   * Selects a location from the suggestions.
   *
   * @param location The selected location
   */
  fun selectLocation(location: Location) {
    updateState { state ->
      when (state) {
        is EventForSerieFormState ->
            state.copy(
                selectedLocation = location,
                location = location.name,
                locationQuery = location.name,
                locationSuggestions = emptyList(),
                invalidLocationMsg = null)
        else -> state
      }
    }
  }

  /** Clears the currently selected location. */
  fun clearLocation() {
    updateState { state ->
      when (state) {
        is EventForSerieFormState ->
            state.copy(
                selectedLocation = null,
                location = "",
                locationQuery = "",
                invalidLocationMsg = "Must be a valid Location")
        else -> state
      }
    }
  }

  /**
   * Updates the event location and validates it.
   *
   * @param location The new location value
   */
  fun setLocation(location: String) {
    updateState { state ->
      when (state) {
        is EventForSerieFormState ->
            state.copy(
                location = location,
                selectedLocation = if (location.isBlank()) null else state.selectedLocation,
                invalidLocationMsg = if (location.isBlank()) "Must be a valid Location" else null)
        else -> state
      }
    }
  }

  /**
   * Searches for locations matching the given query.
   *
   * @param query The search query for location lookup
   */
  suspend fun searchLocations(query: String) {
    if (query.isBlank()) {
      updateState { state ->
        when (state) {
          is EventForSerieFormState -> state.copy(locationSuggestions = emptyList())
          else -> state
        }
      }
      return
    }

    try {
      val suggestions = locationRepository.search(query)
      updateState { state ->
        when (state) {
          is EventForSerieFormState -> state.copy(locationSuggestions = suggestions)
          else -> state
        }
      }
    } catch (e: Exception) {
      updateState { state ->
        when (state) {
          is EventForSerieFormState -> state.copy(locationSuggestions = emptyList())
          else -> state
        }
      }
    }
  }

  /**
   * Loads the serie and checks if it has a group.
   *
   * If the serie has a group, sets serieHasGroup to true and loads the group's event type.
   *
   * @param serieId The ID of the serie to load
   */
  protected suspend fun loadSerieAndCheckGroup(serieId: String) {
    try {
      val serie = serieRepository.getSerie(serieId)
      if (serie.groupId != null) {
        try {
          val eventType = determineEventTypeFromGroup(serie)
          updateState { state ->
            when (state) {
              is EventForSerieFormState ->
                  state.copy(serieHasGroup = true, type = eventType.name, invalidTypeMsg = null)
              else -> state
            }
          }
        } catch (e: NoSuchElementException) {
          Log.e("BaseEventForSerieVM", "Group not found for serie $serieId: ${e.message}", e)
          setErrorMsg("Group not found for this serie")
        }
      } else {
        updateState { state ->
          when (state) {
            is EventForSerieFormState -> state.copy(serieHasGroup = false)
            else -> state
          }
        }
      }
    } catch (e: NoSuchElementException) {
      Log.e("BaseEventForSerieVM", "Serie not found: $serieId", e)
      setErrorMsg("Failed to load serie: ${e.message}")
    } catch (e: IllegalStateException) {
      Log.e("BaseEventForSerieVM", "Invalid state when loading serie group", e)
      setErrorMsg("Serie configuration error: ${e.message}")
    } catch (e: Exception) {
      Log.e("BaseEventForSerieVM", "Failed to load serie $serieId", e)
      setErrorMsg("Failed to load serie: ${e.message}")
    }
  }

  /**
   * Determines the event type from the serie's group.
   *
   * Fetches the group and returns its category as the event type. Throws an exception if the group
   * cannot be fetched.
   *
   * @param serie The serie with a groupId
   * @return The EventType from the group's category
   * @throws IllegalStateException if the serie has no groupId
   * @throws Exception if the group cannot be fetched
   */
  protected suspend fun determineEventTypeFromGroup(serie: Serie): EventType {
    val groupId =
        serie.groupId
            ?: throw IllegalStateException(
                "Serie ${serie.serieId} has no groupId but was expected to have one")
    val group = groupRepository.getGroup(groupId)
    return group.category
  }

  /**
   * Populates the UI state with event data.
   *
   * This helper function extracts common event fields and updates the state, optionally preserving
   * group-specific type information.
   *
   * @param event The event to populate from
   * @param serieHasGroup Whether the serie has a group
   * @param preservedType The type to preserve if serie has a group
   */
  protected fun populateEventData(
      event: Event,
      serieHasGroup: Boolean = false,
      preservedType: String? = null
  ) {
    val eventType = if (serieHasGroup && preservedType != null) preservedType else event.type.name

    updateState { state ->
      when (state) {
        is EventForSerieFormState ->
            state.copy(
                type = eventType,
                title = event.title,
                description = event.description,
                location = event.location?.name ?: "",
                locationQuery = event.location?.name ?: "",
                selectedLocation = event.location,
                duration = event.duration.toString())
        else -> state
      }
    }
  }
}
