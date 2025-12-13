package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.displayString
import com.android.joinme.model.event.isActive
import com.android.joinme.model.event.isUpcoming
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.groups.streaks.StreakService
import com.android.joinme.model.map.Location
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the ShowEvent screen. */
data class ShowEventUIState(
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val locationObject: Location? = null,
    val maxParticipants: String = "",
    val participantsCount: String = "",
    val duration: String = "",
    val date: String = "",
    val dateTimestamp: Timestamp? = null,
    val visibility: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val participants: List<String> = emptyList(),
    val isPastEvent: Boolean = false,
    val partOfASerie: Boolean = false,
    val serieId: String? = null,
    val groupId: String? = null,
    val groupName: String? = null,
    val errorMsg: String? = null,
) {
  /**
   * Checks if the given user ID is the owner of this event.
   *
   * @param userId The user ID to check.
   * @return True if the user is the owner, false otherwise.
   */
  fun isOwner(userId: String): Boolean = ownerId == userId

  /**
   * Checks if the given user ID is a participant in this event.
   *
   * @param userId The user ID to check.
   * @return True if the user is a participant, false otherwise.
   */
  fun isParticipant(userId: String): Boolean = participants.contains(userId)
}

/** ViewModel for the ShowEvent screen. */
class ShowEventViewModel(
    private val repository: EventsRepository =
        EventsRepositoryProvider.getRepository(isOnline = true),
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val groupRepository: GroupRepository = GroupRepositoryProvider.repository,
    initialState: ShowEventUIState = ShowEventUIState()
) : ViewModel() {

  private val _uiState = MutableStateFlow(initialState)
  val uiState: StateFlow<ShowEventUIState> = _uiState.asStateFlow()

  /** Clears the global error message. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  private fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /**
   * Loads an Event by its ID and updates the UI state.
   *
   * @param eventId The ID of the Event to be loaded.
   * @param serieId Optional serie ID if the event belongs to a serie.
   */
  fun loadEvent(eventId: String, serieId: String? = null) {
    viewModelScope.launch {
      try {
        val event = repository.getEvent(eventId)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Format the date with event type prefix
        val formattedDate =
            "${event.type.displayString().uppercase()}: ${dateFormat.format(event.date.toDate())}"

        // Get owner name
        val ownerDisplayName = "Created by ${getOwnerDisplayName(event.ownerId)}"

        // Get group name if event belongs to a group
        val groupName = event.groupId?.let { getGroupName(it) }

        // Check if the event is past (not active and not upcoming)
        val isPast = !event.isActive() && !event.isUpcoming()

        _uiState.value =
            ShowEventUIState(
                type = event.type.displayString().uppercase(),
                title = event.title,
                description = event.description,
                location = event.location?.name ?: "",
                locationObject = event.location,
                maxParticipants = event.maxParticipants.toString(),
                participantsCount = event.participants.size.toString(),
                duration = event.duration.toString(),
                date = formattedDate,
                dateTimestamp = event.date,
                visibility = event.visibility.displayString().uppercase(),
                ownerId = event.ownerId,
                ownerName = ownerDisplayName,
                participants = event.participants,
                isPastEvent = isPast,
                partOfASerie = event.partOfASerie,
                serieId = serieId,
                groupId = event.groupId,
                groupName = groupName)
      } catch (e: Exception) {
        setErrorMsg("Failed to load Event: ${e.message}")
      }
    }
  }

  /**
   * Fetches the display name of the serie owner given their user ID.
   *
   * @param ownerId The user ID of the serie owner
   * @return The display name of the owner, or "UNKNOWN" if not found or if an error occurs
   */
  private suspend fun getOwnerDisplayName(ownerId: String): String {
    if (ownerId.isEmpty()) {
      return "UNKNOWN"
    }
    return try {
      val profile = profileRepository.getProfile(ownerId)
      profile?.username ?: "UNKNOWN"
    } catch (_: Exception) {
      "UNKNOWN"
    }
  }

  /**
   * Fetches the name of the group given its ID.
   *
   * @param groupId The ID of the group
   * @return The name of the group, or null if not found or if an error occurs
   */
  private suspend fun getGroupName(groupId: String): String? {
    return try {
      val group = groupRepository.getGroup(groupId)
      group.name
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Toggles the current user's participation in the event (join or quit).
   *
   * Refactored to reduce cognitive complexity. Orchestrates validation, profile update, event
   * update, and streaks.
   */
  suspend fun toggleParticipation(eventId: String, userId: String) {
    try {
      val event = repository.getEvent(eventId)
      val isJoining = !event.participants.contains(userId)

      // 1. Calculate new participant list
      val updatedParticipants = getUpdatedParticipantsOrError(event, userId) ?: return

      // 2. Update Profile Stats (returns original profile for rollback)
      val originalProfile = updateProfileStatsOrError(userId, isJoining) ?: return

      // 3. Update Event (rolls back profile if fails)
      val eventUpdateSuccess =
          updateEventOrRollback(
              eventId, event.copy(participants = updatedParticipants), originalProfile)

      if (!eventUpdateSuccess) return

      // 4. Update Streak (rolls back event and profile if fails)
      if (!updateStreakOrRollback(event, userId, isJoining, eventId, originalProfile)) return

      // 5. Refresh UI
      loadEvent(eventId)
      clearErrorMsg()
    } catch (e: Exception) {
      setErrorMsg("Failed to update participation: ${e.message}")
    }
  }

  /**
   * Deletes an Event document by its ID.
   *
   * Refactored to reduce cognitive complexity. Handles participant profile updates and event
   * deletion with rollback support.
   */
  suspend fun deleteEvent(eventId: String) {
    try {
      val event = repository.getEvent(eventId)

      if (event.participants.isNotEmpty()) {
        // 1. Update all participant profiles (returns originals for rollback)
        val originalProfiles = updateParticipantProfilesOrError(event.participants) ?: return

        // 2. Delete event (rolls back profiles if fails)
        if (!deleteEventOrRollback(eventId, originalProfiles)) return

        // 3. Revert streaks (Non-critical)
        revertStreaksIfUpcomingGroupEvent(event)
      } else {
        // No participants, simple delete
        repository.deleteEvent(eventId)
      }

      clearErrorMsg()
    } catch (e: Exception) {
      setErrorMsg("Failed to delete Event: ${e.message}")
    }
  }

  // region toggleParticipation Helpers

  /**
   * Calculates the new list of participants based on the current user's status. Checks if the event
   * is full before joining.
   *
   * Side effect: Sets error message if event is full.
   *
   * @return The updated list of user IDs, or null if the action is invalid (e.g., event full).
   */
  private fun getUpdatedParticipantsOrError(event: Event, userId: String): List<String>? {
    return if (event.participants.contains(userId)) {
      event.participants.filter { it != userId }
    } else {
      if (event.participants.size >= event.maxParticipants) {
        setErrorMsg("Event is full. Cannot join.")
        null
      } else {
        event.participants + userId
      }
    }
  }

  /**
   * Updates the user's `eventsJoinedCount` in Firestore.
   *
   * Side effect: Sets error message if profile fetch or update fails.
   *
   * @return The *original* profile state before the update (for rollback purposes), or null if the
   *   operation failed.
   */
  private suspend fun updateProfileStatsOrError(userId: String, isJoining: Boolean): Profile? {
    try {
      val userProfile = profileRepository.getProfile(userId)
      if (userProfile == null) {
        setErrorMsg("Failed to load your profile. Cannot complete operation.")
        return null
      }
      val newCount =
          if (isJoining) {
            userProfile.eventsJoinedCount + 1
          } else {
            maxOf(0, userProfile.eventsJoinedCount - 1)
          }
      val updatedProfile = userProfile.copy(eventsJoinedCount = newCount)
      profileRepository.createOrUpdateProfile(updatedProfile)
      return userProfile // Return original for potential rollback
    } catch (e: Exception) {
      setErrorMsg("Failed to update your profile. Cannot complete operation: ${e.message}")
      return null
    }
  }

  /**
   * Attempts to update the Event document. If it fails, it rolls back the profile update using the
   * [originalProfile].
   *
   * Side effect: Sets error message on failure.
   *
   * @return True if successful, False if failed (and rolled back).
   */
  private suspend fun updateEventOrRollback(
      eventId: String,
      updatedEvent: Event,
      originalProfile: Profile
  ): Boolean {
    return try {
      repository.editEvent(eventId, updatedEvent)
      true
    } catch (e: Exception) {
      // Rollback profile update
      try {
        profileRepository.createOrUpdateProfile(originalProfile)
      } catch (rollbackEx: Exception) {
        Log.e(
            "ShowEventViewModel", "Rollback failed for profile ${originalProfile.uid}", rollbackEx)
      }
      setErrorMsg("Failed to update participation: ${e.message}")
      false
    }
  }

  /**
   * Updates the StreakService. If it fails, rolls back the event and profile changes.
   *
   * @return True if successful or no streak update needed, False if failed (and rolled back).
   */
  private suspend fun updateStreakOrRollback(
      event: Event,
      userId: String,
      isJoining: Boolean,
      eventId: String,
      originalProfile: Profile
  ): Boolean {
    if (event.groupId == null) return true

    return try {
      if (isJoining) {
        StreakService.onActivityJoined(event.groupId, userId, event.date)
      } else {
        StreakService.onActivityLeft(event.groupId, userId, event.date)
      }
      true
    } catch (e: Exception) {
      Log.e("ShowEventViewModel", "Error updating streak for user $userId, rolling back", e)
      // Rollback event
      try {
        repository.editEvent(eventId, event)
      } catch (rollbackEx: Exception) {
        Log.e("ShowEventViewModel", "Failed to rollback event $eventId", rollbackEx)
      }
      // Rollback profile
      try {
        profileRepository.createOrUpdateProfile(originalProfile)
      } catch (rollbackEx: Exception) {
        Log.e("ShowEventViewModel", "Failed to rollback profile ${originalProfile.uid}", rollbackEx)
      }
      setErrorMsg("Failed to update streak: ${e.message}")
      false
    }
  }

  // endregion

  // region deleteEvent Helpers

  /**
   * Decrements `eventsJoinedCount` for all participants. If any update fails, it triggers a
   * restoration of all profiles modified so far.
   *
   * Side effect: Sets error message on failure.
   *
   * @return A list of the *original* profiles (for rollback) if all updates succeeded, or null if
   *   any failed.
   */
  private suspend fun updateParticipantProfilesOrError(participants: List<String>): List<Profile>? {
    val profiles = profileRepository.getProfilesByIds(participants)
    if (profiles == null) {
      setErrorMsg("Failed to load all participant profiles. Cannot delete event.")
      return null
    }

    // Try to update all profiles
    for (profile in profiles) {
      try {
        val newCount = maxOf(0, profile.eventsJoinedCount - 1)
        val updatedProfile = profile.copy(eventsJoinedCount = newCount)
        profileRepository.createOrUpdateProfile(updatedProfile)
      } catch (e: Exception) {
        // Restore all profiles if any update fails
        restoreProfiles(profiles)
        setErrorMsg(
            "Failed to update profile for participant ${profile.uid}. Cannot delete event: ${e.message}")
        return null
      }
    }
    return profiles
  }

  /**
   * Attempts to delete the event from the repository. If it fails, it restores all participant
   * profiles to their original state.
   *
   * Side effect: Sets error message on failure.
   */
  private suspend fun deleteEventOrRollback(
      eventId: String,
      originalProfiles: List<Profile>
  ): Boolean {
    return try {
      repository.deleteEvent(eventId)
      true
    } catch (e: Exception) {
      restoreProfiles(originalProfiles)
      setErrorMsg("Failed to delete Event: ${e.message}")
      false
    }
  }

  /**
   * Best-effort restoration of a list of profiles to the repository. Used for rolling back changes.
   */
  private suspend fun restoreProfiles(profiles: List<Profile>) {
    profiles.forEach { profile ->
      try {
        profileRepository.createOrUpdateProfile(profile)
      } catch (e: Exception) {
        // Best-effort restoration: log failures for observability
        Log.e("ShowEventViewModel", "Failed to restore profile ${profile.uid}", e)
      }
    }
  }

  /**
   * Reverts streaks if the deleted event was an upcoming group event. Failures are logged silently.
   */
  private suspend fun revertStreaksIfUpcomingGroupEvent(event: Event) {
    if (event.groupId != null && event.isUpcoming()) {
      try {
        StreakService.onActivityDeleted(event.groupId, event.participants, event.date)
      } catch (e: Exception) {
        Log.e("ShowEventViewModel", "Error reverting streaks for deleted event", e)
      }
    }
  }

  // endregion
}
