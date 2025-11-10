package com.android.joinme.ui.overview

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

/** Note: This file was refactored using IA (Claude) */

/** Milliseconds per second conversion factor */
private const val MILLIS_PER_SECOND = 1000L

/** Seconds per minute conversion factor */
private const val SECONDS_PER_MINUTE = 60L

/** Milliseconds per minute conversion factor */
private const val MILLIS_PER_MINUTE = SECONDS_PER_MINUTE * MILLIS_PER_SECOND

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
    locationRepository: LocationRepository = NominatimLocationRepository(HttpClientProvider.client)
) : BaseEventForSerieViewModel(locationRepository) {

  override val _uiState = MutableStateFlow(CreateEventForSerieUIState())
  val uiState: StateFlow<CreateEventForSerieUIState> = _uiState.asStateFlow()

  override fun getState(): EventForSerieFormUIState = _uiState.value

  override fun updateState(transform: (EventForSerieFormUIState) -> EventForSerieFormUIState) {
    _uiState.value = transform(_uiState.value) as CreateEventForSerieUIState
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

      // Calculate the end time of the new event
      val newEventEndTime = event.date.toDate().time + (event.duration * MILLIS_PER_MINUTE)
      val newEventEndTimestamp = Timestamp(java.util.Date(newEventEndTime))

      // Update the serie to include the new event ID and update lastEventEndTime
      val updatedSerie =
          serie.copy(
              eventIds = serie.eventIds + newEventId, lastEventEndTime = newEventEndTimestamp)
      serieRepository.editSerie(serieId, updatedSerie)

      clearErrorMsg()
      _uiState.value = _uiState.value.copy(isLoading = false)
      true
    } catch (e: Exception) {
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
    val lastEventEndTime = lastEvent.date.toDate().time + (lastEvent.duration * MILLIS_PER_MINUTE)

    return Timestamp(java.util.Date(lastEventEndTime))
  }
}
