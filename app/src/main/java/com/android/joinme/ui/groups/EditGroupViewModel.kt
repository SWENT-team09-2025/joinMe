package com.android.joinme.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for Edit Group screen
 *
 * @property name Group name (3-30 characters, letters/numbers/spaces/underscores)
 * @property category Selected category (Social/Activity/Sports)
 * @property description Optional description (0-300 characters)
 * @property nameError Validation error for name field
 * @property descriptionError Validation error for description field
 * @property isValid Whether the form is valid and can be submitted
 * @property isLoading Whether a load/update operation is in progress
 * @property editedGroupId The ID of the successfully edited group, null otherwise
 * @property errorMsg Error message to display
 */
data class EditGroupUIState(
    override val name: String = "",
    override val category: EventType = EventType.ACTIVITY,
    override val description: String = "",
    override val nameError: String? = null,
    override val descriptionError: String? = null,
    override val isValid: Boolean = false,
    override val isLoading: Boolean = false,
    val editedGroupId: String? = null,
    override val errorMsg: String? = null
) : GroupFormUIState

/**
 * ViewModel for Edit Group screen
 *
 * Handles loading, validation, and updating of existing groups through the repository layer. This
 * ViewModel follows clean architecture principles by delegating all data operations to the
 * repository and never directly accessing Firebase or Firestore.
 *
 * Validation rules:
 * - Name: Required, 3-30 characters, letters/numbers/spaces/underscores only
 * - Category: Required, must be one of: Social, Activity, Sports
 * - Description: Optional, max 300 characters
 *
 * State machine:
 * - Loading: Fetching group data from repository (isLoading=true)
 * - Idle/Editing: User is filling the form, live validation occurs
 * - Submitting: Form is being submitted (isLoading=true)
 * - Success: Group updated successfully (editedGroupId set)
 * - Error: Operation failed (errorMsg set)
 *
 * @param repository Repository for group operations
 */
class EditGroupViewModel(
    private val repository: GroupRepository = GroupRepositoryProvider.repository
) : BaseGroupFormViewModel() {

  companion object {
    private const val ERROR_LOAD_FAILED = "Failed to load group"
    private const val ERROR_UPDATE_FAILED = "Failed to update group"
    private const val ERROR_UNKNOWN = "Unknown error"
  }

  override val _uiState = MutableStateFlow(EditGroupUIState())
  val uiState: StateFlow<EditGroupUIState> = _uiState.asStateFlow()

  override fun getState(): GroupFormUIState = _uiState.value

  override fun updateState(transform: (GroupFormUIState) -> GroupFormUIState) {
    _uiState.value = transform(_uiState.value) as EditGroupUIState
  }

  /**
   * Clears the success state from the UI state
   *
   * Resets the editedGroupId to null. Useful for resetting after navigation completes.
   */
  fun clearSuccessState() {
    _uiState.value = _uiState.value.copy(editedGroupId = null)
  }

  /**
   * Loads an existing group's data from the repository
   *
   * Fetches the group by ID and populates the form fields with its current data. Sets the form
   * validity based on the loaded data. If an error occurs during loading, sets an error message in
   * the UI state.
   *
   * State transitions:
   * - Before: isLoading=false
   * - During: isLoading=true
   * - Success: isLoading=false, form fields populated with group data
   * - Error: isLoading=false, errorMsg set with failure message
   *
   * @param groupId The ID of the group to load
   */
  fun loadGroup(groupId: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)

      try {
        val group = repository.getGroup(groupId)

        _uiState.value =
            _uiState.value.copy(
                name = group.name,
                category = group.category,
                description = group.description,
                nameError = null,
                descriptionError = null,
                isValid = computeValidity(group.name, null, null),
                isLoading = false)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMsg = "$ERROR_LOAD_FAILED: ${e.message ?: ERROR_UNKNOWN}")
      }
    }
  }

  /**
   * Updates the group with the current form data
   *
   * Validates the form, fetches the current group from the repository, applies the changes from the
   * UI state (name, description, category), and saves it back. Other properties like ownerId,
   * memberIds, and eventIds are preserved unchanged.
   *
   * State transitions:
   * - Before: isLoading=false, editedGroupId=null, errorMsg=null
   * - During: isLoading=true
   * - Success: isLoading=false, editedGroupId set to the updated group's ID
   * - Error: isLoading=false, errorMsg set with failure message
   *
   * @param groupId The ID of the group to update
   */
  fun updateGroup(groupId: String) {
    viewModelScope.launch {
      if (!_uiState.value.isValid) return@launch

      val currentState = _uiState.value

      _uiState.value = currentState.copy(isLoading = true, errorMsg = null, editedGroupId = null)

      try {
        val currentGroup = repository.getGroup(groupId)

        val updatedGroup =
            currentGroup.copy(
                name = currentState.name.trim(),
                category = currentState.category,
                description = currentState.description.ifBlank { "" })

        repository.editGroup(groupId, updatedGroup)

        _uiState.value = _uiState.value.copy(isLoading = false, editedGroupId = groupId)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMsg = "$ERROR_UPDATE_FAILED: ${e.message ?: ERROR_UNKNOWN}")
      }
    }
  }
}
