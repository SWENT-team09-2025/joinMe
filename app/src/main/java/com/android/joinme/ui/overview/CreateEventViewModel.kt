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
import com.android.joinme.model.profile.Profile
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

/**
 * UI state for the CreateEvent screen. Holds all form data, validation flags, and available group
 * data.
 */
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

    // Validation messages
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

/**
 * ViewModel for the CreateEvent screen. Handles form validation, event creation orchestration, and
 * post-creation updates (groups, streaks).
 */
class CreateEventViewModel(
    private val repository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
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

  /**
   * Asynchronously loads the list of groups the current user belongs to and updates the
   * availableGroups in the UI state.
   */
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
   * Orchestrates the event creation process.
   * * 1. Validates the form.
   * 2. Fetches necessary data (Group and Profile).
   * 3. Creates and saves the Event.
   * 4. Updates related data (Group lists, Profile stats, Streaks).
   * 5. Handles rollback if critical updates fail.
   *
   * @param userId The user ID of the event owner. Defaults to the current Firebase Auth user.
   * @return `true` if the event was created and all critical updates succeeded; `false` otherwise.
   */
  suspend fun createEvent(userId: String? = null): Boolean {
    if (!_uiState.value.isValid) {
      setErrorMsg("At least one field is not valid")
      return false
    }

    val parsedDate = parseDateOrError() ?: return false
    val ownerId = userId ?: (Firebase.auth.currentUser?.uid ?: "unknown")

    // 1. Fetch Prerequisites (Group & Profile)
    // Uses Result wrapper to distinguish between "No Group Selected" (Success(null)) vs "Fetch
    // Failed" (Failure)
    val groupResult = fetchGroupOrError()
    if (groupResult.isFailure) return false
    val selectedGroup = groupResult.getOrNull()

    val ownerProfile = fetchProfileOrError(ownerId) ?: return false

    // 2. Build and Save Event
    val event = buildEvent(ownerId, parsedDate)
    if (!saveEventOrError(event)) return false

    // 3. Post-Creation Updates (Group, Profile, Streaks)
    // If these fail, we must rollback the event creation to maintain data consistency.
    if (!processPostCreationUpdates(event, selectedGroup, ownerProfile, parsedDate)) {
      performRollback(event.eventId)
      return false
    }

    clearErrorMsg()
    return true
  }

  // region Helper Functions for createEvent

  /**
   * Parses the date and time strings from the UI state. Side effect: Sets the error message in UI
   * state if parsing fails.
   *
   * @return [Timestamp] if successful, null otherwise.
   */
  private fun parseDateOrError(): Timestamp? {
    val state = _uiState.value
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val combinedDateTime = "${state.date} ${state.time}"
    return try {
      Timestamp(sdf.parse(combinedDateTime)!!)
    } catch (_: Exception) {
      setErrorMsg("Invalid date format (must be dd/MM/yyyy HH:mm)")
      null
    }
  }

  /**
   * Fetches the selected group if a groupId is present in the state.
   * * @return [Result]
   * - Success(Group?): The group object, or null if no group was selected (standalone event).
   * - Failure: If a database error occurred (UI error message is set internally).
   */
  private suspend fun fetchGroupOrError(): Result<Group?> {
    val groupId = _uiState.value.selectedGroupId ?: return Result.success(null)
    return try {
      Result.success(groupRepository.getGroup(groupId))
    } catch (e: Exception) {
      Log.e("CreateEventViewModel", "Error getting group", e)
      setErrorMsg("Failed to get group: ${e.message}")
      Result.failure(e)
    }
  }

  /**
   * Fetches the user's profile. Side effect: Sets error message if profile is null or fetch fails.
   */
  private suspend fun fetchProfileOrError(ownerId: String): Profile? {
    return try {
      val profile = profileRepository.getProfile(ownerId)
      if (profile == null) {
        setErrorMsg("Failed to load your profile. Cannot create event.")
      }
      profile
    } catch (e: Exception) {
      Log.e("CreateEventViewModel", "Error fetching owner profile", e)
      setErrorMsg("Failed to load your profile. Cannot create event: ${e.message}")
      null
    }
  }

  /**
   * Constructs the [Event] domain object from the current UI state.
   *
   * @param ownerId The user ID of the event owner.
   * @param date The event date as a [Timestamp].
   * @return The constructed [Event] object.
   */
  private fun buildEvent(ownerId: String, date: Timestamp): Event {
    val state = _uiState.value
    return Event(
        eventId = repository.getNewEventId(),
        type = EventType.valueOf(state.type.uppercase(Locale.ROOT)),
        title = state.title,
        description = state.description,
        location = state.selectedLocation!!,
        date = date,
        duration = state.duration.toInt(),
        participants = listOf(ownerId),
        maxParticipants = state.maxParticipants.toInt(),
        visibility = EventVisibility.valueOf(state.visibility.uppercase(Locale.ROOT)),
        ownerId = ownerId,
        partOfASerie = false,
        groupId = state.selectedGroupId)
  }

  /**
   * Persists the event to the repository. Side effect: Logs error and sets UI error message on
   * failure.
   */
  private suspend fun saveEventOrError(event: Event): Boolean {
    return try {
      repository.addEvent(event)
      true
    } catch (e: Exception) {
      Log.e("CreateEventViewModel", "Error creating event", e)
      setErrorMsg("Failed to create event: ${e.message}")
      false
    }
  }

  /**
   * Manages secondary updates after the event is successfully saved.
   * 1. Adds event ID to the Group.
   * 2. Increments user's 'eventsJoinedCount'.
   * 3. Updates streaks.
   *
   * @return `true` if all updates succeeded; `false` if any failed.
   */
  private suspend fun processPostCreationUpdates(
      event: Event,
      group: Group?,
      profile: Profile,
      date: Timestamp
  ): Boolean {
    // A. Update Group (if applicable)
    if (group != null && !updateGroupList(group, event.eventId)) {
      // Return false to trigger rollback in the orchestrator
      return false
    }

    // B. Update Profile (Increment count)
    if (!updateProfileStats(profile)) {
      // Return false to trigger rollback in the orchestrator
      return false
    }

    // C. Update Streaks (for group events)
    if (group != null && !updateStreaks(group, date)) {
      // Return false to trigger rollback in the orchestrator
      return false
    }

    return true
  }

  /** Updates the Group document to include the new event ID. */
  private suspend fun updateGroupList(group: Group, eventId: String): Boolean {
    return try {
      val updatedGroup = group.copy(eventIds = group.eventIds + eventId)
      groupRepository.editGroup(group.id, updatedGroup)
      true
    } catch (e: Exception) {
      Log.e("CreateEventViewModel", "Error adding event to group", e)
      setErrorMsg("Event created but failed to add to group: ${e.message}")
      false
    }
  }

  /**
   * Increments the user's `eventsJoinedCount`. This is required because the creator is
   * automatically a participant.
   */
  private suspend fun updateProfileStats(profile: Profile): Boolean {
    return try {
      val updatedProfile = profile.copy(eventsJoinedCount = profile.eventsJoinedCount + 1)
      profileRepository.createOrUpdateProfile(updatedProfile)
      true
    } catch (e: Exception) {
      // Note: We set the specific error message here so the user knows WHY it failed,
      // even though the rollback will happen afterward.
      setErrorMsg("Failed to update your profile. Cannot create event: ${e.message}")
      false
    }
  }

  /**
   * Updates streaks for all group members.
   *
   * @return `true` if all streak updates succeeded; `false` if any failed.
   */
  private suspend fun updateStreaks(group: Group, date: Timestamp): Boolean {
    return try {
      for (memberId in group.memberIds) {
        StreakService.onActivityJoined(group.id, memberId, date)
      }
      true
    } catch (e: Exception) {
      Log.e("CreateEventViewModel", "Error updating streaks", e)
      setErrorMsg("Failed to update streaks. Cannot create event: ${e.message}")
      false
    }
  }

  /**
   * Deletes the event to rollback changes after a critical failure in post-processing. This ensures
   * the database doesn't contain an event that isn't linked to a group or profile stats.
   */
  private suspend fun performRollback(eventId: String) {
    try {
      repository.deleteEvent(eventId)
    } catch (deleteError: Exception) {
      Log.e("CreateEventViewModel", "CRITICAL: Failed to rollback event creation", deleteError)
    }
  }

  // region UI Field Setters

  /** Updates the location field and validates it. */
  fun setLocation(location: String) {
    _uiState.value =
        _uiState.value.copy(
            location = location,
            invalidLocationMsg = if (location.isBlank()) "Must be a valid Location" else null)
  }

  /** Updates and validates maxParticipants. */
  fun setMaxParticipants(value: String) {
    val num = value.toIntOrNull()

    _uiState.value =
        _uiState.value.copy(
            maxParticipants = value,
            invalidMaxParticipantsMsg =
                if (num == null || num <= 0) "Must be a positive number" else null)
  }

  /**
   * Updates the selected group for the event.
   * - If [groupId] is NOT null: Auto-fills type, visibility, and defaults maxParticipants.
   * - If [groupId] IS null: Resets fields for a standalone event.
   */
  fun setSelectedGroup(groupId: String?) {
    val selectedGroup = groupId?.let { id -> _uiState.value.availableGroups.find { it.id == id } }

    if (selectedGroup != null) {
      // For group events: auto-set properties and ensure visibility is PRIVATE
      _uiState.value =
          _uiState.value.copy(
              selectedGroupId = groupId,
              type = selectedGroup.category.name.uppercase(Locale.ROOT),
              visibility = EventVisibility.PRIVATE.name,
              invalidTypeMsg = null,
              invalidVisibilityMsg = null)
    } else {
      // For standalone events: clear fields for manual entry
      _uiState.value =
          _uiState.value.copy(
              selectedGroupId = null,
              type = "",
              visibility = "",
              invalidTypeMsg = null,
              invalidVisibilityMsg = null)
    }
  }
}
