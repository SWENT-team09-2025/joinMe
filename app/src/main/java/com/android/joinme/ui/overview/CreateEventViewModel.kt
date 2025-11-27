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
import com.android.joinme.model.groups.streaks.StreakService
import com.android.joinme.model.map.Location
import com.android.joinme.model.map.LocationRepository
import com.android.joinme.model.map.NominatimLocationRepository
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Note: This file was co-written with the help of AI (Claude) */

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
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val groupRepository: GroupRepository = GroupRepositoryProvider.repository,
    locationRepository: LocationRepository = NominatimLocationRepository(HttpClientProvider.client)
) : BaseEventFormViewModel(locationRepository) {

  override val _uiState = MutableStateFlow(CreateEventUIState())
  val uiState: StateFlow<CreateEventUIState> = _uiState.asStateFlow()

  companion object {
    private const val DEFAULT_GROUP_EVENT_MAX_PARTICIPANTS = 300
  }

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

  /**
   * Adds a new event to the repository. Suspends until the save is complete.
   *
   * This method also increments the owner's eventsJoinedCount since the owner is automatically
   * added as a participant when creating an event.
   *
   * @param userId The user ID of the event owner. Defaults to the current Firebase Auth user.
   */
  suspend fun createEvent(userId: String? = null): Boolean {
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

    val ownerId = userId ?: (Firebase.auth.currentUser?.uid ?: "unknown")
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
            ownerId = ownerId,
            groupId = state.selectedGroupId)

    // Fetch owner profile first to validate it exists
    val ownerProfile =
        try {
          profileRepository.getProfile(ownerId)
        } catch (e: Exception) {
          Log.e("CreateEventViewModel", "Error fetching owner profile", e)
          setErrorMsg("Failed to load your profile. Cannot create event: ${e.message}")
          return false
        }

    if (ownerProfile == null) {
      setErrorMsg("Failed to load your profile. Cannot create event.")
      return false
    }

    // Create the event
    try {
      repository.addEvent(event)
    } catch (e: Exception) {
      Log.e("CreateEventViewModel", "Error creating event", e)
      setErrorMsg("Failed to create event: ${e.message}")
      return false
    }

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

    // Increment owner's eventsJoinedCount since they're automatically added as participant
    try {
      val updatedProfile = ownerProfile.copy(eventsJoinedCount = ownerProfile.eventsJoinedCount + 1)
      profileRepository.createOrUpdateProfile(updatedProfile)
    } catch (e: Exception) {
      // Rollback: delete the event since profile update failed
      try {
        repository.deleteEvent(event.eventId)
      } catch (deleteError: Exception) {
        Log.e("CreateEventViewModel", "Failed to rollback event creation", deleteError)
      }
      setErrorMsg("Failed to update your profile. Cannot create event: ${e.message}")
      return false
    }

    // Update streaks for all participants if this is a group event
    if (state.selectedGroupId != null && selectedGroup != null) {
      for (memberId in selectedGroup.memberIds) {
        try {
          StreakService.onActivityJoined(state.selectedGroupId, memberId, parsedDate)
        } catch (e: Exception) {
          Log.e("CreateEventViewModel", "Error updating streak for user $memberId", e)
          // Non-critical: don't fail event creation if streak update fails
        }
      }
    }

    clearErrorMsg()
    return true
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
      // For group events, auto-set type, maxParticipants (set to accommodate group growth),
      // and visibility
      _uiState.value =
          _uiState.value.copy(
              selectedGroupId = groupId,
              type = selectedGroup.category.name.uppercase(Locale.ROOT),
              maxParticipants = DEFAULT_GROUP_EVENT_MAX_PARTICIPANTS.toString(),
              visibility = EventVisibility.PRIVATE.name,
              invalidTypeMsg = null,
              invalidMaxParticipantsMsg = null,
              invalidVisibilityMsg = null)
    } else {
      // For standalone events, clear the fields
      _uiState.value =
          _uiState.value.copy(
              selectedGroupId = null,
              type = "",
              maxParticipants = "",
              visibility = "",
              invalidTypeMsg = null,
              invalidMaxParticipantsMsg = null,
              invalidVisibilityMsg = null)
    }
  }
}
