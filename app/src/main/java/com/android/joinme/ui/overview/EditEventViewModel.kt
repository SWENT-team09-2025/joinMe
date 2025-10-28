package com.android.joinme.ui.overview

import android.util.Log
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
    override val type: String = "",
    override val title: String = "",
    override val description: String = "",
    override val location: String = "",
    override val maxParticipants: String = "",
    override val duration: String = "",
    override val date: String = "",
    override val time: String = "",
    override val visibility: String = "",
    val ownerId: String = "",
    val participants: List<String> = emptyList(),
    override val errorMsg: String? = null,
    override val locationQuery: String = "",
    override val locationSuggestions: List<Location> = emptyList(),
    override val selectedLocation: Location? = null,

    // validation messages
    override val invalidTypeMsg: String? = null,
    override val invalidTitleMsg: String? = null,
    override val invalidDescriptionMsg: String? = null,
    override val invalidLocationMsg: String? = null,
    override val invalidMaxParticipantsMsg: String? = null,
    override val invalidDurationMsg: String? = null,
    override val invalidDateMsg: String? = null,
    override val invalidVisibilityMsg: String? = null,
    override val invalidTimeMsg: String? = null,
) : EventFormUIState {
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
    locationRepository: LocationRepository = NominatimLocationRepository(HttpClientProvider.client),
    initialState: EditEventUIState = EditEventUIState()
) : BaseEventFormViewModel(locationRepository) {

    override val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<EditEventUIState> = _uiState.asStateFlow()

    override fun getState(): EventFormUIState = _uiState.value

    override fun updateState(transform: (EventFormUIState) -> EventFormUIState) {
        _uiState.value = transform(_uiState.value) as EditEventUIState
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
                        locationQuery = event.location?.name ?: "",
                        selectedLocation = event.location,
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
     * Updates the event location and validates it.
     *
     * Validates that the location is not blank. If the location is cleared (blank), also clears the
     * selectedLocation.
     *
     * @param location The event location to set.
     */
    fun setLocation(location: String) {
        _uiState.value =
            _uiState.value.copy(
                location = location,
                selectedLocation = if (location.isBlank()) null else _uiState.value.selectedLocation,
                invalidLocationMsg = if (location.isBlank()) "Must be a valid Location" else null)
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
}
