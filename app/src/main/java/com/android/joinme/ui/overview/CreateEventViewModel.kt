package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.android.joinme.HttpClientProvider
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
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
    override val type: String = "",
    override val title: String = "",
    override val description: String = "",
    override val location: String = "",
    override val maxParticipants: String = "",
    override val duration: String = "",
    override val date: String = "",
    override val time: String = "",
    override val visibility: String = "",
    override val errorMsg: String? = null,
    override val locationQuery: String = "",
    override val locationSuggestions: List<Location> = emptyList(),
    override val selectedLocation: Location? = null,
    val selectedGroupId: String? = null, // null means standalone event
    val availableGroups: List<Group> = emptyList(),

    // validation messages
    override val invalidTypeMsg: String? = null,
    override val invalidTitleMsg: String? = null,
    override val invalidDescriptionMsg: String? = null,
    override val invalidLocationMsg: String? = null,
    override val invalidMaxParticipantsMsg: String? = null,
    override val invalidDurationMsg: String? = null,
    override val invalidDateMsg: String? = null,
    override val invalidTimeMsg: String? = null,
    override val invalidVisibilityMsg: String? = null,
) : EventFormUIState {
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
    private val groupRepository: GroupRepository = GroupRepositoryProvider.repository,
    locationRepository: LocationRepository = NominatimLocationRepository(HttpClientProvider.client)
) : BaseEventFormViewModel(locationRepository) {

  override val _uiState = MutableStateFlow(CreateEventUIState())
  val uiState: StateFlow<CreateEventUIState> = _uiState.asStateFlow()

  init {
    loadUserGroups()
  }

  override fun getState(): EventFormUIState = _uiState.value

  override fun updateState(transform: (EventFormUIState) -> EventFormUIState) {
    _uiState.value = transform(_uiState.value) as CreateEventUIState
  }

  /** Loads the list of groups the current user belongs to. */
  private fun loadUserGroups() {
    viewModelScope.launch {
      try {
        val groups = groupRepository.getAllGroups()
        _uiState.value = _uiState.value.copy(availableGroups = groups)
      } catch (e: Exception) {
        Log.e("CreateEventViewModel", "Error loading user groups", e)
      }
    }
  }

  /** Adds a new event to the repository. If a group is selected, adds the event to the group. */
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

    // Get group if selected, for both members and event list update
    val selectedGroup =
        state.selectedGroupId?.let { groupId ->
          try {
            groupRepository.getGroup(groupId)
          } catch (e: Exception) {
            Log.e("CreateEventViewModel", "Error getting group", e)
            setErrorMsg("Failed to get group: ${e.message}")
            return false
          }
        }

    val eventId = repository.getNewEventId()
    val event =
        Event(
            eventId = eventId,
            type = EventType.valueOf(state.type.uppercase(Locale.ROOT)),
            title = state.title,
            description = state.description,
            location = state.selectedLocation!!,
            date = parsedDate,
            duration = state.duration.toInt(),
            participants = selectedGroup?.memberIds ?: emptyList(),
            maxParticipants = state.maxParticipants.toInt(),
            visibility = EventVisibility.valueOf(state.visibility.uppercase(Locale.ROOT)),
            ownerId = Firebase.auth.currentUser?.uid ?: "unknown")

    return try {
      repository.addEvent(event)

      // If a group is selected, add the event ID to the group's event list
      selectedGroup?.let { group ->
        try {
          val updatedGroup = group.copy(eventIds = group.eventIds + eventId)
          groupRepository.editGroup(group.id, updatedGroup)
        } catch (e: Exception) {
          Log.e("CreateEventViewModel", "Error adding event to group", e)
          setErrorMsg("Event created but failed to add to group: ${e.message}")
          return false
        }
      }

      clearErrorMsg()
      true
    } catch (e: Exception) {
      Log.e("CreateEventViewModel", "Error creating event", e)
      setErrorMsg("Failed to create event: ${e.message}")
      false
    }
  }

  fun setLocation(location: String) {
    _uiState.value =
        _uiState.value.copy(
            location = location,
            invalidLocationMsg = if (location.isBlank()) "Must be a valid Location" else null)
  }

  fun setMaxParticipants(value: String) {
    val num = value.toIntOrNull()
    val selectedGroup =
        _uiState.value.selectedGroupId?.let { groupId ->
          _uiState.value.availableGroups.find { it.id == groupId }
        }
    val groupMembersCount = selectedGroup?.memberIds?.size ?: 0

    _uiState.value =
        _uiState.value.copy(
            maxParticipants = value,
            invalidMaxParticipantsMsg =
                when {
                  num == null || num <= 0 -> "Must be a positive number"
                  groupMembersCount > 0 && num < groupMembersCount ->
                      "Must be at least $groupMembersCount (group size)"
                  else -> null
                })
  }

  /** Updates the selected group for the event. Pass null for standalone events. */
  fun setSelectedGroup(groupId: String?) {
    val selectedGroup = groupId?.let { id -> _uiState.value.availableGroups.find { it.id == id } }

    if (selectedGroup != null) {
      // For group events, auto-set type, maxParticipants, and visibility
      _uiState.value =
          _uiState.value.copy(
              selectedGroupId = groupId,
              type = selectedGroup.category.name.uppercase(Locale.ROOT),
              maxParticipants = selectedGroup.memberIds.size.toString(),
              visibility = EventVisibility.PRIVATE.name,
              invalidTypeMsg = null,
              invalidMaxParticipantsMsg = null,
              invalidVisibilityMsg = null)
    } else {
      // For standalone events, reset to defaults
      _uiState.value = _uiState.value.copy(selectedGroupId = null)
      // Re-validate maxParticipants when switching back to standalone
      setMaxParticipants(_uiState.value.maxParticipants)
    }
  }
}
