package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.joinme.HttpClientProvider
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.map.Location
import com.android.joinme.model.map.LocationRepository
import com.android.joinme.model.map.NominatimLocationRepository
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for the CreateEventForSerie screen.
 *
 * Only includes fields that the user needs to input when creating an event for an existing serie:
 * - Event type
 * - Event title
 * - Event description
 * - Event duration
 * - Event location
 *
 * Other fields (date, maxParticipants, visibility, participants, ownerId) will be inherited from
 * the serie.
 *
 * @property type The event type (SPORTS, ACTIVITY, or SOCIAL)
 * @property title The event title
 * @property description The event description
 * @property duration The event duration in minutes as a string
 * @property location The event location name
 * @property locationQuery The current location search query
 * @property locationSuggestions List of location suggestions from search
 * @property selectedLocation The selected Location object
 * @property isLoading Indicates whether the event is currently being created
 * @property errorMsg Global error message for the form
 * @property invalidTypeMsg Validation message for the type field
 * @property invalidTitleMsg Validation message for the title field
 * @property invalidDescriptionMsg Validation message for the description field
 * @property invalidDurationMsg Validation message for the duration field
 * @property invalidLocationMsg Validation message for the location field
 */
data class CreateEventForSerieUIState(
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val duration: String = "",
    val location: String = "",
    val locationQuery: String = "",
    val locationSuggestions: List<Location> = emptyList(),
    val selectedLocation: Location? = null,
    val isLoading: Boolean = false,
    val errorMsg: String? = null,

    // validation messages
    val invalidTypeMsg: String? = null,
    val invalidTitleMsg: String? = null,
    val invalidDescriptionMsg: String? = null,
    val invalidDurationMsg: String? = null,
    val invalidLocationMsg: String? = null,
) {
  /**
   * Checks if all form fields are valid and filled.
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
            type.isNotBlank() &&
            title.isNotBlank() &&
            description.isNotBlank() &&
            duration.isNotBlank() &&
            selectedLocation != null
}

/**
 * ViewModel for the CreateEventForSerie screen.
 *
 * Manages the UI state and business logic for creating a new event that belongs to an existing
 * serie. The event inherits certain properties from the serie (maxParticipants, visibility,
 * ownerId) while allowing the user to specify event-specific details (type, title, description,
 * duration, location).
 *
 * The date for the event is calculated automatically based on the serie's existing events - it will
 * be set to start right after the last event in the serie ends.
 *
 * @property eventRepository The EventsRepository used for event data operations
 * @property serieRepository The SeriesRepository used for serie data operations
 * @property locationRepository The LocationRepository used for location search
 */
class CreateEventForSerieViewModel(
    private val eventRepository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    private val serieRepository: SeriesRepository = SeriesRepositoryProvider.repository,
    private val locationRepository: LocationRepository =
        NominatimLocationRepository(HttpClientProvider.client)
) : ViewModel() {

  private val _uiState = MutableStateFlow(CreateEventForSerieUIState())
  val uiState: StateFlow<CreateEventForSerieUIState> = _uiState.asStateFlow()

  /** Clears the global error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Sets a global error message in the UI state.
   *
   * @param msg The error message to display
   */
  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /**
   * Searches for locations matching the given query.
   *
   * @param query The search query for location lookup
   */
  suspend fun searchLocations(query: String) {
    if (query.isBlank()) {
      _uiState.value = _uiState.value.copy(locationSuggestions = emptyList())
      return
    }

    try {
      val suggestions = locationRepository.search(query)
      _uiState.value = _uiState.value.copy(locationSuggestions = suggestions)
    } catch (e: Exception) {
      Log.e("CreateEventForSerieViewModel", "Error searching locations", e)
      _uiState.value = _uiState.value.copy(locationSuggestions = emptyList())
    }
  }

  /**
   * Updates the location query field.
   *
   * @param query The new location query value
   */
  fun setLocationQuery(query: String) {
    _uiState.value = _uiState.value.copy(locationQuery = query)
  }

  /**
   * Selects a location from the suggestions.
   *
   * @param location The selected location
   */
  fun selectLocation(location: Location) {
    _uiState.value =
        _uiState.value.copy(
            selectedLocation = location,
            location = location.name,
            locationQuery = location.name,
            locationSuggestions = emptyList(),
            invalidLocationMsg = null)
  }

  /** Clears the currently selected location. */
  fun clearLocation() {
    _uiState.value =
        _uiState.value.copy(
            selectedLocation = null,
            location = "",
            locationQuery = "",
            invalidLocationMsg = "Must be a valid Location")
  }

  /**
   * Creates a new event for the given serie and adds it to the repository.
   *
   * This function performs the following steps:
   * 1. Validates that all form fields are valid
   * 2. Loads the serie from the repository
   * 3. Calculates the event date (starts after the last event in the serie ends)
   * 4. Creates an Event object, inheriting maxParticipants, visibility, and ownerId from the serie
   * 5. Saves the event to the event repository
   * 6. Updates the serie to include the new event ID in its eventIds list
   *
   * If the serie has no events yet, the new event will use the serie's date as its start time.
   *
   * @param serieId The ID of the serie to add the event to
   * @return True if the event was created successfully, false if validation failed or an error
   *   occurred
   */
  suspend fun createEventForSerie(serieId: String): Boolean {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return false
    }

    _uiState.value = _uiState.value.copy(isLoading = true)

    return try {
      // Load the serie
      val serie = serieRepository.getSerie(serieId)

      // Calculate the event date based on the serie's existing events
      val eventDate = calculateEventDate(serie)

      // Create the event, inheriting properties from the serie
      val newEventId = eventRepository.getNewEventId()
      val event =
          Event(
              eventId = newEventId,
              type = EventType.valueOf(state.type.uppercase()),
              title = state.title,
              description = state.description,
              location = state.selectedLocation!!,
              date = eventDate,
              duration = state.duration.toInt(),
              participants = emptyList(), // Start with empty participants list
              maxParticipants = serie.maxParticipants, // Inherit from serie
              visibility =
                  when (serie.visibility) {
                    com.android.joinme.model.utils.Visibility.PUBLIC -> EventVisibility.PUBLIC
                    com.android.joinme.model.utils.Visibility.PRIVATE -> EventVisibility.PRIVATE
                  },
              ownerId = serie.ownerId // Inherit from serie
              )

      // Add the event to the repository
      eventRepository.addEvent(event)

      // Update the serie to include the new event ID
      val updatedSerie = serie.copy(eventIds = serie.eventIds + newEventId)
      serieRepository.editSerie(serieId, updatedSerie)

      clearErrorMsg()
      _uiState.value = _uiState.value.copy(isLoading = false)
      true
    } catch (e: Exception) {
      Log.e("CreateEventForSerieViewModel", "Error creating event for serie", e)
      setErrorMsg("Failed to create event: ${e.message}")
      _uiState.value = _uiState.value.copy(isLoading = false)
      false
    }
  }

  /**
   * Calculates the appropriate start date for a new event in the serie.
   *
   * If the serie has no events yet, returns the serie's date. Otherwise, fetches all events in the
   * serie and calculates when the last event ends (lastEventDate + lastEventDuration), then returns
   * that as the start time for the new event.
   *
   * @param serie The serie to calculate the event date for
   * @return The calculated Timestamp for the new event
   */
  private suspend fun calculateEventDate(serie: Serie): Timestamp {
    if (serie.eventIds.isEmpty()) {
      // No events yet, use the serie's date
      return serie.date
    }

    // Get all events in the serie and find the last one
    val allEvents =
        eventRepository.getAllEvents(
            com.android.joinme.model.event.EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)
    val serieEvents =
        allEvents.filter { it.eventId in serie.eventIds }.sortedBy { it.date.toDate().time }

    if (serieEvents.isEmpty()) {
      // No events found, use the serie's date
      return serie.date
    }

    // Calculate when the last event ends
    val lastEvent = serieEvents.last()
    val lastEventEndTime = lastEvent.date.toDate().time + (lastEvent.duration * 60 * 1000)

    return Timestamp(java.util.Date(lastEventEndTime))
  }

  // Update functions for all fields

  /**
   * Updates the event type and validates it.
   *
   * @param type The new type value (SPORTS, ACTIVITY, or SOCIAL)
   */
  fun setType(type: String) {
    val validTypes = listOf("SPORTS", "ACTIVITY", "SOCIAL")
    _uiState.value =
        _uiState.value.copy(
            type = type,
            invalidTypeMsg =
                if (type.isBlank()) "Event type cannot be empty"
                else if (type.uppercase() !in validTypes) "Type must be SPORTS, ACTIVITY, or SOCIAL"
                else null)
  }

  /**
   * Updates the event title and validates it.
   *
   * @param title The new title value
   */
  fun setTitle(title: String) {
    _uiState.value =
        _uiState.value.copy(
            title = title, invalidTitleMsg = if (title.isBlank()) "Title cannot be empty" else null)
  }

  /**
   * Updates the event description and validates it.
   *
   * @param description The new description value
   */
  fun setDescription(description: String) {
    _uiState.value =
        _uiState.value.copy(
            description = description,
            invalidDescriptionMsg =
                if (description.isBlank()) "Description cannot be empty" else null)
  }

  /**
   * Updates the event duration and validates it.
   *
   * @param value The new duration value in minutes as a string
   */
  fun setDuration(value: String) {
    val num = value.toIntOrNull()
    _uiState.value =
        _uiState.value.copy(
            duration = value,
            invalidDurationMsg = if (num == null || num <= 0) "Must be a positive number" else null)
  }

  /**
   * Updates the event location and validates it.
   *
   * @param location The new location value
   */
  fun setLocation(location: String) {
    _uiState.value =
        _uiState.value.copy(
            location = location,
            selectedLocation = if (location.isBlank()) null else _uiState.value.selectedLocation,
            invalidLocationMsg = if (location.isBlank()) "Must be a valid Location" else null)
  }
}
