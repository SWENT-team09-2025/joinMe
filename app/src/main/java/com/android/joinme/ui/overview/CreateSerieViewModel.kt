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
import com.google.firebase.Timestamp
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
      // When a group is selected, visibility is auto-filled
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
          maxParticipants.isNotBlank() && // maxParticipants always required
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

  /**
   * Updates the selected group for the serie. Pass null for standalone series.
   *
   * Deletes any previously created serie since changing the group requires creating a new serie.
   */
  fun setSelectedGroup(groupId: String?) {
    // Delete any existing serie before changing the group
    viewModelScope.launch {
      deleteCreatedSerieIfExists()

      // Update state after deletion to ensure createdSerieId is available during deletion
      val selectedGroup = groupId?.let { id -> _uiState.value.availableGroups.find { it.id == id } }

      if (selectedGroup != null) {
        // For group series, auto-set visibility
        _uiState.value =
            _uiState.value.copy(
                selectedGroupId = groupId,
                visibility = Visibility.PRIVATE.name,
                invalidVisibilityMsg = null,
                createdSerieId = null) // Clear any existing serie
      } else {
        // For standalone series, clear the fields
        _uiState.value =
            _uiState.value.copy(
                selectedGroupId = null,
                visibility = "",
                invalidVisibilityMsg = null,
                createdSerieId = null) // Clear any existing serie
      }
    }
  }

  /**
   * Creates a new serie and adds it to the repository.
   *
   * Orchestrates the creation process: validation, auth check, date parsing, group fetching, saving
   * to repository, linking to group, and updating streaks.
   *
   * @return The serie ID if the serie was created successfully, null otherwise.
   */
  suspend fun createSerie(): String? {
    // 1. Check existing
    _uiState.value.createdSerieId?.let {
      return it
    }

    // 2. Validate Form
    if (!_uiState.value.isValid) {
      setErrorMsg("At least one field is not valid")
      return null
    }

    // 3. Auth Check
    val userId = authenticateUserOrError() ?: return null

    setLoadingState(true)

    // 4. Parse Date
    val parsedDate = parseDateOrError() ?: return stopLoadingAndReturn()

    // 5. Fetch Group (if selected)
    val groupResult = fetchGroupSafe()
    if (groupResult.isFailure) return stopLoadingAndReturn()
    val group = groupResult.getOrNull()

    // 6. Build Object
    val serie = buildSerie(userId, parsedDate, group)

    // 7. Persist (Repo + Group Link)
    if (!tryPersistSerie(serie, group)) {
      return stopLoadingAndReturn()
    }

    // 8. Update Streaks
    if (group != null && !updateStreaks(group, parsedDate)) {
      // Rollback serie and group association
      tryRollbackSerie(serie.serieId, group)
      return stopLoadingAndReturn()
    }

    onCreationSuccess(serie.serieId)
    return serie.serieId
  }

  /**
   * Deletes the created serie if the user goes back without completing the flow.
   *
   * This is a cleanup operation that handles:
   * 1. Removing the serie from the repository.
   * 2. Removing the serie ID from the group (if applicable).
   * 3. Reverting streaks (if applicable).
   */
  suspend fun deleteCreatedSerieIfExists() {
    val serieId = _uiState.value.createdSerieId ?: return
    val groupId = _uiState.value.selectedGroupId

    try {
      val serie = repository.getSerie(serieId)
      // Only delete if the serie has no events
      if (serie.eventIds.isNotEmpty()) return

      repository.deleteSerie(serieId)

      if (groupId != null) {
        cleanupGroupAssociations(groupId, serieId, serie.participants, serie.date)
      }
    } catch (_: Exception) {
      // Serie doesn't exist or error occurred, ignore
    }
  }

  // region CreateSerie Helpers

  /**
   * Verifies that the user is currently signed in.
   *
   * Side effect: Sets the error message if the user is not authenticated.
   *
   * @return The current user's ID, or null if not authenticated.
   */
  private fun authenticateUserOrError(): String? {
    val currentUserId = getCurrentUserId()
    if (currentUserId == null) {
      setErrorMsg("You must be signed in to create a serie")
    }
    return currentUserId
  }

  /**
   * Parses the date and time strings from the UI state.
   *
   * Side effect: Sets the error message if parsing fails.
   *
   * @return A [Timestamp] object if successful, or null if the format is invalid.
   */
  private fun parseDateOrError(): Timestamp? {
    val state = _uiState.value
    val date = parseDateTime(state.date, state.time)
    if (date == null) {
      setErrorMsg("Invalid date format (must be dd/MM/yyyy HH:mm)")
    }
    return date
  }

  /**
   * Helper to reset the loading state to false and return null. Used when a step in [createSerie]
   * fails.
   */
  private fun stopLoadingAndReturn(): String? {
    setLoadingState(false)
    return null
  }

  /**
   * Safely attempts to fetch the selected group.
   *
   * @return A [Result] containing the [Group] if found (or null if no group was selected), or a
   *   Failure if a repository error occurred (error message is automatically set).
   */
  private suspend fun fetchGroupSafe(): Result<Group?> {
    val groupId = _uiState.value.selectedGroupId ?: return Result.success(null)
    return try {
      Result.success(groupRepository.getGroup(groupId))
    } catch (e: Exception) {
      setErrorMsg("Failed to get group: ${e.message}")
      Result.failure(e)
    }
  }

  /** Constructs the [Serie] object based on the current form state and context. */
  private fun buildSerie(userId: String, date: Timestamp, group: Group?): Serie {
    val state = _uiState.value
    val serieId = repository.getNewSerieId()
    return Serie(
        serieId = serieId,
        title = state.title,
        description = state.description,
        date = date,
        participants = listOf(userId),
        maxParticipants = state.maxParticipants.toInt(),
        visibility = Visibility.valueOf(state.visibility.uppercase(Locale.ROOT)),
        eventIds = emptyList(),
        ownerId = userId,
        groupId = state.selectedGroupId)
  }

  /**
   * Persists the serie to the repository. If the serie is part of a group, it handles the
   * distributed transaction:
   * 1. Add serie ID to Group.
   * 2. Save Serie to DB.
   * 3. Rollback Group update if saving Serie fails.
   *
   * @return True if persistence was successful, False otherwise.
   */
  private suspend fun tryPersistSerie(serie: Serie, group: Group?): Boolean {
    return try {
      // 1. Add to Group first
      if (group != null) {
        try {
          val updatedGroup = group.copy(serieIds = group.serieIds + serie.serieId)
          groupRepository.editGroup(group.id, updatedGroup)
        } catch (e: Exception) {
          setErrorMsg("Failed to add serie to group: ${e.message}")
          return false
        }
      }

      // 2. Add to Repository
      try {
        repository.addSerie(serie)
      } catch (e: Exception) {
        // Rollback group if needed
        if (group != null) {
          rollbackGroupUpdate(group, serie.serieId)
        }
        throw e // Re-throw to be caught by outer catch for generic error msg
      }
      true
    } catch (e: Exception) {
      setErrorMsg("Failed to create serie: ${e.message}")
      false
    }
  }

  /**
   * Reverts the modification made to the group (removing the serie ID) if the main serie
   * persistence failed.
   */
  private suspend fun rollbackGroupUpdate(group: Group, serieId: String) {
    try {
      val revertedGroup = group.copy(serieIds = group.serieIds - serieId)
      groupRepository.editGroup(group.id, revertedGroup)
    } catch (rollbackException: Exception) {
      Log.e("CreateSerieViewModel", "Error rolling back group update", rollbackException)
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
      Log.e("CreateSerieViewModel", "Error updating streaks", e)
      setErrorMsg("Failed to update streaks. Cannot create serie: ${e.message}")
      false
    }
  }

  /** Rolls back a created serie by deleting it and cleaning up group associations. */
  private suspend fun tryRollbackSerie(serieId: String, group: Group) {
    try {
      repository.deleteSerie(serieId)
      rollbackGroupUpdate(group, serieId)
    } catch (e: Exception) {
      Log.e("CreateSerieViewModel", "CRITICAL: Failed to rollback serie creation", e)
    }
  }

  /**
   * Finalizes the creation process by clearing errors, stopping loading, and updating the state
   * with the new ID.
   */
  private fun onCreationSuccess(serieId: String) {
    clearErrorMsg()
    setLoadingState(false)
    _uiState.value = _uiState.value.copy(createdSerieId = serieId)
  }

  // region DeleteSerie Helpers

  /** Cleans up external associations (Group link, Streaks) when a created serie is deleted. */
  private suspend fun cleanupGroupAssociations(
      groupId: String,
      serieId: String,
      participants: List<String>,
      date: Timestamp
  ) {
    // 1. Remove from group list
    try {
      val group = groupRepository.getGroup(groupId)
      val updatedGroup = group.copy(serieIds = group.serieIds - serieId)
      groupRepository.editGroup(group.id, updatedGroup)
    } catch (_: Exception) {
      // Error removing serie from group, ignore
    }

    // 2. Revert streaks
    try {
      StreakService.onActivityDeleted(groupId, participants, date)
    } catch (e: Exception) {
      Log.e("CreateSerieViewModel", "Error reverting streaks for deleted serie", e)
      // Non-critical
    }
  }
}
