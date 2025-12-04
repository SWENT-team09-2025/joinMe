package com.android.joinme.ui.overview

import com.android.joinme.HttpClientProvider
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.map.LocationRepository
import com.android.joinme.model.map.NominatimLocationRepository
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.google.firebase.Timestamp
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Note: This file was co-written with AI (Claude) */

/** Milliseconds per second conversion factor */
private const val MILLIS_PER_SECOND = 1000L

/** Seconds per minute conversion factor */
private const val SECONDS_PER_MINUTE = 60L

/** Milliseconds per minute conversion factor */
private const val MILLIS_PER_MINUTE = SECONDS_PER_MINUTE * MILLIS_PER_SECOND

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
    serieRepository: SeriesRepository = SeriesRepositoryProvider.repository,
    groupRepository: GroupRepository = GroupRepositoryProvider.repository,
    locationRepository: LocationRepository = NominatimLocationRepository(HttpClientProvider.client)
) : BaseEventForSerieViewModel(locationRepository, serieRepository, groupRepository) {

  override val _uiState = MutableStateFlow(EventForSerieFormState())
  val uiState: StateFlow<EventForSerieFormState> = _uiState.asStateFlow()

  override fun getState(): EventForSerieFormUIState = _uiState.value

  override fun updateState(transform: (EventForSerieFormUIState) -> EventForSerieFormUIState) {
    _uiState.value = transform(_uiState.value) as EventForSerieFormState
  }

  /**
   * Loads the serie information and updates the UI state.
   *
   * If the serie has a group, sets serieHasGroup to true and loads the group's event type.
   *
   * @param serieId The ID of the serie to load
   */
  suspend fun loadSerie(serieId: String) {
    loadSerieAndCheckGroup(serieId)
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

      // Determine the event type
      // If serie has a group, use the group's category; otherwise use the user's selection
      val eventType =
          try {
            if (serie.groupId != null) {
              determineEventTypeFromGroup(serie)
            } else {
              EventType.valueOf(state.type.uppercase())
            }
          } catch (e: Exception) {
            setErrorMsg("Failed to determine event type: ${e.message}")
            _uiState.value = _uiState.value.copy(isLoading = false)
            return false
          }

      // Calculate the event date based on the serie's existing events
      val eventDate = calculateEventDate(serie)

      // Create the event, inheriting properties from the serie
      val newEventId = eventRepository.getNewEventId()
      val event =
          Event(
              eventId = newEventId,
              type = eventType,
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
              ownerId = serie.ownerId, // Inherit from serie
              partOfASerie = true, // Mark as part of a serie
              groupId = serie.groupId // Inherit groupId from serie
              )

      // Add the event to the repository
      eventRepository.addEvent(event)

      // Calculate the end time of the new event
      val newEventEndTime = event.date.toDate().time + (event.duration * MILLIS_PER_MINUTE)
      val instant = Instant.ofEpochMilli(newEventEndTime)
      val newEventEndTimestamp = Timestamp(instant.epochSecond, instant.nano)

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
    val serieEvents = eventRepository.getEventsByIds(serie.eventIds)

    if (serieEvents.isEmpty()) {
      // No events found, use the serie's date
      return serie.date
    }

    // Calculate when the last event ends
    val lastEvent = serieEvents.last()
    val lastEventEndTime = lastEvent.date.toDate().time + (lastEvent.duration * MILLIS_PER_MINUTE)
    val instant = Instant.ofEpochMilli(lastEventEndTime)

    return Timestamp(instant.epochSecond, instant.nano)
  }
}
