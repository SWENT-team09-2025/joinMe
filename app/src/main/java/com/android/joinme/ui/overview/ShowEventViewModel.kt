package com.android.joinme.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.event.displayString
import com.android.joinme.model.event.isActive
import com.android.joinme.model.event.isUpcoming
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider
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
    val maxParticipants: String = "",
    val participantsCount: String = "",
    val duration: String = "",
    val date: String = "",
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
                maxParticipants = event.maxParticipants.toString(),
                participantsCount = event.participants.size.toString(),
                duration = event.duration.toString(),
                date = formattedDate,
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
    if (groupId.isEmpty()) {
      return null
    }
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
   * This method also updates the user's eventsJoinedCount in their profile:
   * - When joining: increment eventsJoinedCount by 1
   * - When quitting: decrement eventsJoinedCount by 1
   *
   * @param eventId The ID of the event.
   * @param userId The ID of the current user.
   */
  suspend fun toggleParticipation(eventId: String, userId: String) {
    try {
      val event = repository.getEvent(eventId)
      val isJoining = !event.participants.contains(userId)

      val updatedParticipants =
          if (event.participants.contains(userId)) {
            // User is already a participant, remove them (quit)
            event.participants.filter { it != userId }
          } else {
            // User is not a participant, add them (join)
            if (event.participants.size >= event.maxParticipants) {
              setErrorMsg("Event is full. Cannot join.")
              return
            }
            event.participants + userId
          }

      // Update user's eventsJoinedCount in their profile FIRST
      try {
        val userProfile = profileRepository.getProfile(userId)
        if (userProfile == null) {
          setErrorMsg("Failed to load your profile. Cannot complete operation.")
          return
        }
        val newCount =
            if (isJoining) {
              userProfile.eventsJoinedCount + 1
            } else {
              // When quitting, decrement but don't go below 0
              maxOf(0, userProfile.eventsJoinedCount - 1)
            }
        val updatedProfile = userProfile.copy(eventsJoinedCount = newCount)
        profileRepository.createOrUpdateProfile(updatedProfile)

        // Only update the event if profile update succeeded
        val updatedEvent = event.copy(participants = updatedParticipants)
        try {
          repository.editEvent(eventId, updatedEvent)
        } catch (e: Exception) {
          profileRepository.createOrUpdateProfile(userProfile)
          setErrorMsg("Failed to update participation: ${e.message}")
          return
        }
      } catch (e: Exception) {
        // Profile update failed - don't proceed with event update to maintain consistency
        setErrorMsg("Failed to update your profile. Cannot complete operation: ${e.message}")
        return
      }

      // Reload the event to update UI
      loadEvent(eventId)
      clearErrorMsg()
    } catch (e: Exception) {
      setErrorMsg("Failed to update participation: ${e.message}")
    }
  }

  /**
   * Deletes an Event document by its ID.
   *
   * This method also decrements eventsJoinedCount for all participants of the deleted event.
   *
   * @param eventId The ID of the Event document to be deleted.
   */
  suspend fun deleteEvent(eventId: String) {
    try {
      // Get the event before deleting to access participants list
      val event = repository.getEvent(eventId)

      // Fetch all participant profiles first to validate they all exist
      if (event.participants.isNotEmpty()) {
        val participantProfiles = profileRepository.getProfilesByIds(event.participants)
        if (participantProfiles == null) {
          setErrorMsg("Failed to load all participant profiles. Cannot delete event.")
          return
        }

        // Decrement eventsJoinedCount for all participants
        participantProfiles.forEach { profile ->
          try {
            val newCount = maxOf(0, profile.eventsJoinedCount - 1)
            val updatedProfile = profile.copy(eventsJoinedCount = newCount)
            profileRepository.createOrUpdateProfile(updatedProfile)
          } catch (e: Exception) {
            // Restore all eventsJoinedCount if profile update fails
            participantProfiles.forEach { p -> profileRepository.createOrUpdateProfile(p) }
            setErrorMsg(
                "Failed to update profile for participant ${profile.uid}. Cannot delete event: ${e.message}")
            return
          }
        }

        try {
          // Only delete the event if all profile updates succeeded
          repository.deleteEvent(eventId)
        } catch (e: Exception) {
          // Restore all eventsJoinedCount if profile update fails
          participantProfiles.forEach { profile ->
            profileRepository.createOrUpdateProfile(profile)
          }
          setErrorMsg("Failed to delete Event: ${e.message}")
          return
        }
      } else {
        // No participants, just delete the event
        repository.deleteEvent(eventId)
      }

      clearErrorMsg()
    } catch (e: Exception) {
      setErrorMsg("Failed to delete Event: ${e.message}")
    }
  }
}
