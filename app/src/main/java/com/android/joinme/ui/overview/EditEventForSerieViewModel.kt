package com.android.joinme.ui.overview

import androidx.lifecycle.viewModelScope
import com.android.joinme.HttpClientProvider
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
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

/** Note: This file was co-written using the help of IA (Claude) */

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

  override val _uiState = MutableStateFlow(EventForSerieFormState())
  val uiState: StateFlow<EventForSerieFormState> = _uiState.asStateFlow()

  override fun getState(): EventForSerieFormUIState = _uiState.value

  override fun updateState(transform: (EventForSerieFormUIState) -> EventForSerieFormUIState) {
    _uiState.value = transform(_uiState.value) as EventForSerieFormState
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
            EventForSerieFormState(
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

      // If duration changed, update subsequent events and serie's lastEventEndTime
      if (durationChanged) {
        updateSubsequentEventDates(serie, eventId, oldDuration, newDuration)
        // Always update the serie's lastEventEndTime when duration changes,
        // even if there are no subsequent events (the edited event could be the last one)
        updateSerieLastEventEndTime(serie)
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
   * Updates the serie's lastEventEndTime field based on the actual end time of the last event.
   *
   * This function recalculates the end time of the last event in the serie (date + duration) and
   * updates the serie's lastEventEndTime field in the repository. This ensures that the total
   * duration displayed in SerieDetailsScreen is accurate.
   *
   * @param serie The serie to update
   */
  private suspend fun updateSerieLastEventEndTime(serie: Serie) {
    // Get all events in the serie, sorted by date
    val allEvents = eventRepository.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)
    val serieEvents =
        allEvents.filter { it.eventId in serie.eventIds }.sortedBy { it.date.toDate().time }

    if (serieEvents.isEmpty()) {
      return
    }

    // Calculate the end time of the last event
    val lastEvent = serieEvents.last()
    val millisecondsPerMinute = 60 * 1000L
    val lastEventEndTime =
        lastEvent.date.toDate().time + (lastEvent.duration * millisecondsPerMinute)
    val secondsPerMs = 1000L
    val nanosPerMs = 1000000
    val seconds = lastEventEndTime / secondsPerMs
    val nanos = ((lastEventEndTime % secondsPerMs) * nanosPerMs).toInt()
    val lastEventEndTimestamp = Timestamp(seconds, nanos)

    // Update the serie with the new lastEventEndTime
    val updatedSerie = serie.copy(lastEventEndTime = lastEventEndTimestamp)
    serieRepository.editSerie(serie.serieId, updatedSerie)
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
      val newTimeMs = event.date.toDate().time + timeShiftMs
      val secondsPerMs = 1000L
      val nanosPerMs = 1000000
      val seconds = newTimeMs / secondsPerMs
      val nanos = ((newTimeMs % secondsPerMs) * nanosPerMs).toInt()
      val newDate = Timestamp(seconds, nanos)
      val updatedEvent = event.copy(date = newDate)
      eventRepository.editEvent(event.eventId, updatedEvent)
    }
  }
}
