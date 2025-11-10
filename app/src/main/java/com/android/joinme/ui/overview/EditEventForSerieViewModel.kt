package com.android.joinme.ui.overview

import androidx.lifecycle.viewModelScope
import com.android.joinme.HttpClientProvider
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
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
import kotlinx.coroutines.launch

/** Note: This file was written using the help of IA (Claude) */

/**
 * UI state for the EditEventForSerie screen.
 *
 * Includes fields that can be edited for an event within a serie:
 * - Event type
 * - Event title
 * - Event description
 * - Event duration
 * - Event location
 *
 * Note: Unlike standalone events, events within a serie don't have editable date, maxParticipants,
 * or visibility fields - these are inherited from the serie. The date is calculated automatically
 * based on the position within the serie.
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
 * @property invalidTypeMsg Validation message for the type field
 * @property invalidTitleMsg Validation message for the title field
 * @property invalidDescriptionMsg Validation message for the description field
 * @property invalidDurationMsg Validation message for the duration field
 * @property invalidLocationMsg Validation message for the location field
 */
data class EditEventForSerieUIState(
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
 * ViewModel for the EditEventForSerie screen.
 *
 * Manages the UI state and business logic for editing an event that belongs to a serie. When the
 * event's duration is changed, all subsequent events in the serie must have their dates
 * recalculated to maintain proper scheduling.
 *
 * The critical logic handles duration changes:
 * 1. When an event's duration changes, it affects the start time of the next event
 * 2. This cascades through all subsequent events in the serie
 * 3. All affected events must be updated in the repository
 *
 * @property eventRepository The EventsRepository used for event data operations
 * @property serieRepository The SeriesRepository used for serie data operations
 * @property locationRepository The LocationRepository used for location search
 */
class EditEventForSerieViewModel(
    private val eventRepository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    private val serieRepository: SeriesRepository = SeriesRepositoryProvider.repository,
    locationRepository: LocationRepository = NominatimLocationRepository(HttpClientProvider.client)
) : BaseEventForSerieViewModel(locationRepository) {

  override val _uiState = MutableStateFlow(EditEventForSerieUIState())
  val uiState: StateFlow<EditEventForSerieUIState> = _uiState.asStateFlow()

  override fun getState(): EventForSerieFormUIState = _uiState.value

  override fun updateState(transform: (EventForSerieFormUIState) -> EventForSerieFormUIState) {
    _uiState.value = transform(_uiState.value) as EditEventForSerieUIState
  }

  /**
   * Loads an event by its ID and populates the UI state with its data.
   *
   * @param eventId The ID of the event to load
   */
  fun loadEvent(eventId: String) {
    viewModelScope.launch {
      try {
        val event = eventRepository.getEvent(eventId)

        _uiState.value =
            EditEventForSerieUIState(
                type = event.type.name,
                title = event.title,
                description = event.description,
                location = event.location?.name ?: "",
                locationQuery = event.location?.name ?: "",
                selectedLocation = event.location,
                duration = event.duration.toString())
      } catch (e: Exception) {
        setErrorMsg("Failed to load event: ${e.message}")
      }
    }
  }

  /**
   * Edits an event within a serie and recalculates subsequent event dates if duration changed.
   *
   * This function performs the following steps:
   * 1. Validates that all form fields are valid
   * 2. Loads the current event from the repository
   * 3. Checks if the duration has changed
   * 4. Updates the event with new values
   * 5. If duration changed, recalculates and updates all subsequent events in the serie
   *
   * @param serieId The ID of the serie containing this event
   * @param eventId The ID of the event to edit
   * @return True if the event was edited successfully, false if validation failed or an error
   *   occurred
   */
  suspend fun editEventForSerie(serieId: String, eventId: String): Boolean {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return false
    }

    _uiState.value = _uiState.value.copy(isLoading = true)

    return try {
      // Load the serie and the current event
      val serie = serieRepository.getSerie(serieId)
      val currentEvent = eventRepository.getEvent(eventId)

      // Check if duration has changed
      val oldDuration = currentEvent.duration
      val newDuration = state.duration.toInt()
      val durationChanged = oldDuration != newDuration

      // Update the event
      val updatedEvent =
          currentEvent.copy(
              type = EventType.valueOf(state.type.uppercase()),
              title = state.title,
              description = state.description,
              location = state.selectedLocation!!,
              duration = newDuration)

      eventRepository.editEvent(eventId, updatedEvent)

      // If duration changed, recalculate and update all subsequent events
      if (durationChanged) {
        updateSubsequentEventDates(serie, eventId, oldDuration, newDuration)
      }

      clearErrorMsg()
      _uiState.value = _uiState.value.copy(isLoading = false)
      true
    } catch (e: Exception) {
      setErrorMsg("Failed to edit event: ${e.message}")
      _uiState.value = _uiState.value.copy(isLoading = false)
      false
    }
  }

  /**
   * Recalculates and updates the dates of all events that come after the edited event.
   *
   * When an event's duration changes, all subsequent events need their start times adjusted. The
   * time difference (newDuration - oldDuration) is propagated through all following events.
   *
   * For example, if event #2 in a serie has its duration increased by 30 minutes, then
   * events #3, #4, #5, etc. all need to start 30 minutes later.
   *
   * @param serie The serie containing the events
   * @param editedEventId The ID of the event that was edited
   * @param oldDuration The previous duration in minutes
   * @param newDuration The new duration in minutes
   */
  private suspend fun updateSubsequentEventDates(
      serie: Serie,
      editedEventId: String,
      oldDuration: Int,
      newDuration: Int
  ) {
    // Get all events in the serie, sorted by date
    val allEvents = eventRepository.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)
    val serieEvents =
        allEvents.filter { it.eventId in serie.eventIds }.sortedBy { it.date.toDate().time }

    // Find the index of the edited event
    val editedEventIndex = serieEvents.indexOfFirst { it.eventId == editedEventId }
    if (editedEventIndex == -1 || editedEventIndex == serieEvents.size - 1) {
      // Event not found or it's the last event (no subsequent events to update)
      return
    }

    // Calculate the time shift in milliseconds
    val durationDiff = newDuration - oldDuration
    val millisecondsPerMinute = 60 * 1000L
    val timeShiftMs = durationDiff * millisecondsPerMinute

    // Update all subsequent events
    for (i in (editedEventIndex + 1) until serieEvents.size) {
      val event = serieEvents[i]
      val newDate = Timestamp(java.util.Date(event.date.toDate().time + timeShiftMs))
      val updatedEvent = event.copy(date = newDate)
      eventRepository.editEvent(event.eventId, updatedEvent)
    }
  }
}
