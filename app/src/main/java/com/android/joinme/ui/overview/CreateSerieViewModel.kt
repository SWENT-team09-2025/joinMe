package com.android.joinme.ui.overview

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.groups.streaks.StreakService
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryProvider
import com.android.joinme.model.utils.Visibility
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Note: This file was written with the help of AI (Claude) */

/**
 * UI state for the CreateSerie screen.
 *
 * Holds all form field values and their corresponding validation messages for creating a new serie.
 *
 * @property serieId Empty for CreateSerie (not used during creation)
 * @property title The title of the serie
 * @property description The description of the serie
 * @property maxParticipants String representation of the maximum number of participants
 * @property date The date in dd/MM/yyyy format
 * @property time The time in HH:mm format
 * @property visibility The visibility setting (PUBLIC or PRIVATE)
 * @property isLoading Indicates whether the serie is currently being created
 * @property errorMsg Global error message for the form
 * @property invalidTitleMsg Validation message for the title field
 * @property invalidDescriptionMsg Validation message for the description field
 * @property invalidMaxParticipantsMsg Validation message for the max participants field
 * @property invalidDateMsg Validation message for the date field
 * @property invalidTimeMsg Validation message for the time field
 * @property invalidVisibilityMsg Validation message for the visibility field
 */
data class CreateSerieUIState(
    override val serieId: String = "",
    override val title: String = "",
    override val description: String = "",
    override val maxParticipants: String = "",
    override val date: String = "",
    override val time: String = "",
    override val visibility: String = "",
    val isLoading: Boolean = false,
    override val errorMsg: String? = null,
    val createdSerieId: String? = null, // Stores the ID of the created serie to prevent duplicates
    val selectedGroupId: String? = null, // null means standalone serie
    val availableGroups: List<Group> = emptyList(),

    // validation messages
    override val invalidTitleMsg: String? = null,
    override val invalidDescriptionMsg: String? = null,
    override val invalidMaxParticipantsMsg: String? = null,
    override val invalidDateMsg: String? = null,
    override val invalidTimeMsg: String? = null,
    override val invalidVisibilityMsg: String? = null,
) : SerieFormUIState {
  /**
   * Checks if all form fields are valid and filled.
   *
   * @return True if all validation messages are null and all fields are not blank
   */
  val isValid: Boolean
    get() {
      // When a group is selected, maxParticipants and visibility are auto-filled and always valid
      val selectedGroup = selectedGroupId != null
      return invalidTitleMsg == null &&
          invalidDescriptionMsg == null &&
          invalidMaxParticipantsMsg == null &&
          invalidDateMsg == null &&
          invalidTimeMsg == null &&
          invalidVisibilityMsg == null &&
          title.isNotBlank() &&
          description.isNotBlank() &&
          date.isNotBlank() &&
          time.isNotBlank() &&
          (selectedGroup ||
              maxParticipants.isNotBlank()) && // maxParticipants required only if no group
          (selectedGroup || visibility.isNotBlank()) // visibility required only if no group
    }
}

/**
 * ViewModel for the CreateSerie screen.
 *
 * Manages the UI state and business logic for creating a new serie. Handles form validation,
 * date/time parsing, and repository interactions for saving series.
 *
 * @property repository The SeriesRepository used for data operations
 */
class CreateSerieViewModel(
    private val repository: SeriesRepository = SeriesRepositoryProvider.repository,
    private val groupRepository: GroupRepository = GroupRepositoryProvider.repository
) : BaseSerieFormViewModel() {

  override val _uiState = MutableStateFlow(CreateSerieUIState())
  val uiState: StateFlow<CreateSerieUIState> = _uiState.asStateFlow()

  companion object {
    private const val DEFAULT_GROUP_SERIE_MAX_PARTICIPANTS = 300
  }

  override fun getState(): SerieFormUIState = _uiState.value

  override fun updateState(transform: (SerieFormUIState) -> SerieFormUIState) {
    _uiState.value = transform(_uiState.value) as CreateSerieUIState
  }

  /**
   * Loads the list of groups the current user belongs to.
   *
   * This should be called when the screen is displayed to ensure the group list is up-to-date,
   * especially if the user has joined new groups since the last visit.
   */
  fun loadUserGroups() {
    viewModelScope.launch {
      try {
        val groups = groupRepository.getAllGroups()
        _uiState.value = _uiState.value.copy(availableGroups = groups)
      } catch (e: Exception) {
        Log.e("CreateSerieViewModel", "Error loading user groups", e)
      }
    }
  }

  /** Updates the selected group for the serie. Pass null for standalone series. */
  fun setSelectedGroup(groupId: String?) {
    val selectedGroup = groupId?.let { id -> _uiState.value.availableGroups.find { it.id == id } }

    if (selectedGroup != null) {
      // For group series, auto-set maxParticipants and visibility
      _uiState.value =
          _uiState.value.copy(
              selectedGroupId = groupId,
              maxParticipants = DEFAULT_GROUP_SERIE_MAX_PARTICIPANTS.toString(),
              visibility = Visibility.PRIVATE.name,
              invalidMaxParticipantsMsg = null,
              invalidVisibilityMsg = null)
    } else {
      // For standalone series, clear the fields
      _uiState.value =
          _uiState.value.copy(
              selectedGroupId = null,
              maxParticipants = "",
              visibility = "",
              invalidMaxParticipantsMsg = null,
              invalidVisibilityMsg = null)
    }
  }

  /**
   * Creates a new serie and adds it to the repository.
   *
   * This function performs the following steps:
   * 1. Returns the existing serie ID if already created (prevents duplicates)
   * 2. Validates that all form fields are valid
   * 3. Checks that the user is authenticated (must be signed in)
   * 4. Parses the date and time into a single Timestamp
   * 5. Creates a Serie object with the current user as owner
   * 6. Saves the serie to the repository
   * 7. Stores the serie ID in state to prevent duplicate creation
   * 8. Updates streaks for all group members if this is a group serie
   *
   * The loading state is set to true at the start and false upon completion. If any error occurs
   * during the process (validation failure, authentication check failure, date parsing error, or
   * repository error), an appropriate error message is set and the function returns null.
   *
   * @return The serie ID if the serie was created successfully, null if validation failed, user is
   *   not authenticated, date parsing failed, or repository save failed
   */
  suspend fun createSerie(): String? {
    val state = _uiState.value

    // If serie was already created, return the existing ID
    state.createdSerieId?.let {
      return it
    }

    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return null
    }

    // Check if user is authenticated
    val currentUserId = getCurrentUserId()
    if (currentUserId == null) {
      setErrorMsg("You must be signed in to create a serie")
      return null
    }

    setLoadingState(true)

    val parsedDate = parseDateTime(state.date, state.time)
    if (parsedDate == null) {
      setErrorMsg("Invalid date format (must be dd/MM/yyyy HH:mm)")
      setLoadingState(false)
      return null
    }

    // Get group if selected
    val selectedGroup =
        state.selectedGroupId?.let { groupId ->
          try {
            groupRepository.getGroup(groupId)
          } catch (e: Exception) {
            setErrorMsg("Failed to get group: ${e.message}")
            setLoadingState(false)
            return null
          }
        }

    val serieId = repository.getNewSerieId()
    val serie =
        Serie(
            serieId = serieId,
            title = state.title,
            description = state.description,
            date = parsedDate,
            participants = selectedGroup?.memberIds ?: listOf(currentUserId),
            maxParticipants = state.maxParticipants.toInt(),
            visibility = Visibility.valueOf(state.visibility.uppercase(Locale.ROOT)),
            eventIds = emptyList(),
            ownerId = currentUserId,
            groupId = state.selectedGroupId)

    return try {
      // If a group is selected, add the serie ID to the group's serie list first
      selectedGroup?.let { group ->
        try {
          val updatedGroup = group.copy(serieIds = group.serieIds + serieId)
          groupRepository.editGroup(group.id, updatedGroup)
        } catch (e: Exception) {
          setErrorMsg("Failed to add serie to group: ${e.message}")
          setLoadingState(false)
          return null
        }
      }

      // Only add the serie to the repository after successfully adding to the group
      try {
        repository.addSerie(serie)
      } catch (e: Exception) {
        // Roll back the group update if adding the serie fails
        selectedGroup?.let { group ->
          try {
            val revertedGroup = group.copy(serieIds = group.serieIds - serieId)
            groupRepository.editGroup(group.id, revertedGroup)
          } catch (rollbackException: Exception) {
            Log.e("CreateSerieViewModel", "Error rolling back group update", rollbackException)
          }
        }
        throw e
      }

      // Update streaks for all participants if this is a group serie
      if (state.selectedGroupId != null && selectedGroup != null) {
        for (memberId in selectedGroup.memberIds) {
          try {
            StreakService.onActivityJoined(state.selectedGroupId, memberId, parsedDate)
          } catch (e: Exception) {
            Log.e("CreateSerieViewModel", "Error updating streak for user $memberId", e)
            // Non-critical: don't fail serie creation if streak update fails
          }
        }
      }

      clearErrorMsg()
      setLoadingState(false)
      // Store the created serie ID to prevent duplicates
      _uiState.value = _uiState.value.copy(createdSerieId = serieId)
      serieId
    } catch (e: Exception) {
      setErrorMsg("Failed to create serie: ${e.message}")
      setLoadingState(false)
      null
    }
  }

  /**
   * Deletes the created serie if the user goes back without completing the flow.
   *
   * This should be called when the user navigates back from CreateSerieScreen after creating a
   * serie but before completing the event creation. If the serie was associated with a group, it
   * also removes the serie ID from the group's serie list and reverts streaks for all participants.
   */
  suspend fun deleteCreatedSerieIfExists() {
    val serieId = _uiState.value.createdSerieId
    val groupId = _uiState.value.selectedGroupId
    if (serieId != null) {
      try {
        val serie = repository.getSerie(serieId)
        // Only delete if the serie has no events
        if (serie.eventIds.isEmpty()) {
          repository.deleteSerie(serieId)

          // Remove the serie from the group's serie list if it was associated with a group
          if (groupId != null) {
            try {
              val group = groupRepository.getGroup(groupId)
              val updatedGroup = group.copy(serieIds = group.serieIds - serieId)
              groupRepository.editGroup(group.id, updatedGroup)
            } catch (_: Exception) {
              // Error removing serie from group, ignore
            }

            // Revert streaks for all participants
            try {
              StreakService.onActivityDeleted(groupId, serie.participants, serie.date)
            } catch (e: Exception) {
              Log.e("CreateSerieViewModel", "Error reverting streaks for deleted serie", e)
              // Non-critical: serie is already deleted, just log the error
            }
          }
        }
      } catch (_: Exception) {
        // Serie doesn't exist or error occurred, ignore
      }
    }
  }
}
