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
    val name: String = "",
    val category: EventType = EventType.ACTIVITY,
    val description: String = "",
    val nameError: String? = null,
    val descriptionError: String? = null,
    val isValid: Boolean = false,
    val isLoading: Boolean = false,
    val editedGroupId: String? = null,
    val errorMsg: String? = null
)

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
) : ViewModel() {

  companion object {
    // Validation limits
    private const val NAME_MIN_LENGTH = 3
    private const val NAME_MAX_LENGTH = 30
    private const val DESCRIPTION_MAX_LENGTH = 300

    // Error messages
    private const val ERROR_NAME_REQUIRED = "Name is required"
    private const val ERROR_NAME_TOO_SHORT = "Name must be at least 3 characters"
    private const val ERROR_NAME_TOO_LONG = "Name must not exceed 30 characters"
    private const val ERROR_NAME_INVALID_CHARS =
        "Only letters, numbers, spaces, and underscores allowed"
    private const val ERROR_NAME_MULTIPLE_SPACES = "Multiple consecutive spaces not allowed"
    private const val ERROR_DESCRIPTION_TOO_LONG = "Description must not exceed 300 characters"
    private const val ERROR_LOAD_FAILED = "Failed to load group"
    private const val ERROR_UPDATE_FAILED = "Failed to update group"
    private const val ERROR_UNKNOWN = "Unknown error"
  }

  private val _uiState = MutableStateFlow(EditGroupUIState())
  val uiState: StateFlow<EditGroupUIState> = _uiState.asStateFlow()

  /**
   * Sets the group name and validates it
   *
   * @param name The group name to set
   */
  fun setName(name: String) {
    val trimmedName = name.trim()
    val error = validateName(trimmedName)
    _uiState.value = _uiState.value.copy(name = name, nameError = error)
    updateFormValidity()
  }

  /**
   * Sets the group category and validates it
   *
   * @param category The category to set (Social/Activity/Sports)
   */
  fun setCategory(category: EventType) {
    _uiState.value = _uiState.value.copy(category = category)
    updateFormValidity()
  }

  /**
   * Sets the group description and validates it
   *
   * @param description The description to set
   */
  fun setDescription(description: String) {
    val error = validateDescription(description)
    _uiState.value = _uiState.value.copy(description = description, descriptionError = error)
    updateFormValidity()
  }

  /**
   * Clears the error message
   *
   * Useful for dismissing error messages after the user acknowledges them
   */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Clears the success state
   *
   * Useful for resetting after navigation completes
   */
  fun clearSuccessState() {
    _uiState.value = _uiState.value.copy(editedGroupId = null)
  }

  /**
   * Loads an existing group's data from the repository
   *
   * Fetches the group by ID, populates the form fields with its current data, and performs
   * validation. If an error occurs, sets an error message in the UI state.
   *
   * State transitions:
   * - Before: isLoading=false
   * - During: isLoading=true
   * - Success: isLoading=false, form fields populated
   * - Error: isLoading=false, errorMsg=<message>
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
                isLoading = false)

        updateFormValidity()
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
   * memberIds, and eventIds are preserved.
   *
   * State transitions:
   * - Before: isLoading=false, editedGroupId=null, errorMsg=null
   * - During: isLoading=true
   * - Success: isLoading=false, editedGroupId=<id>
   * - Error: isLoading=false, errorMsg=<message>
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

  /**
   * Validates the group name
   *
   * Rules:
   * - Required (not blank)
   * - 3-30 characters
   * - Only letters, numbers, spaces, and underscores
   * - No multiple consecutive spaces
   *
   * @param name The name to validate
   * @return Error message if invalid, null if valid
   */
  private fun validateName(name: String): String? {
    return when {
      name.isBlank() -> ERROR_NAME_REQUIRED
      name.length < NAME_MIN_LENGTH -> ERROR_NAME_TOO_SHORT
      name.length > NAME_MAX_LENGTH -> ERROR_NAME_TOO_LONG
      name.contains(Regex("\\s{2,}")) -> ERROR_NAME_MULTIPLE_SPACES
      !name.matches(Regex("^[a-zA-Z0-9_ ]+$")) -> ERROR_NAME_INVALID_CHARS
      else -> null
    }
  }

  /**
   * Validates the group description
   *
   * Rules:
   * - Optional (can be empty)
   * - Max 300 characters
   *
   * @param description The description to validate
   * @return Error message if invalid, null if valid
   */
  private fun validateDescription(description: String): String? {
    return when {
      description.length > DESCRIPTION_MAX_LENGTH -> ERROR_DESCRIPTION_TOO_LONG
      else -> null
    }
  }

  /**
   * Updates the form validity based on current field values
   *
   * Form is valid when:
   * - Name is not blank
   * - No validation errors exist
   */
  private fun updateFormValidity() {
    val state = _uiState.value
    val isValid =
        state.name.isNotBlank() && state.nameError == null && state.descriptionError == null
    _uiState.value = state.copy(isValid = isValid)
  }
}
