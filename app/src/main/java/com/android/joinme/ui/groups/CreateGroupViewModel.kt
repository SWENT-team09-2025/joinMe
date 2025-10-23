package com.android.joinme.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventType
import com.android.joinme.model.group.GroupRepository
import com.android.joinme.model.group.GroupRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for Create Group screen
 *
 * @property name Group name (3-30 characters, letters/numbers/spaces/underscores)
 * @property category Selected category (Social/Activity/Sports)
 * @property description Optional description (0-300 characters)
 * @property nameError Validation error for name field
 * @property descriptionError Validation error for description field
 * @property isValid Whether the form is valid and can be submitted
 * @property isLoading Whether a create operation is in progress
 * @property createdGroupId The ID of the successfully created group, null otherwise
 * @property errorMsg Error message to display
 */
data class CreateGroupUIState(
    val name: String = "",
    val category: EventType = EventType.ACTIVITY,
    val description: String = "",
    val nameError: String? = null,
    val descriptionError: String? = null,
    val isValid: Boolean = false,
    val isLoading: Boolean = false,
    val createdGroupId: String? = null,
    val errorMsg: String? = null
)

/**
 * ViewModel for Create Group screen
 *
 * Handles validation and creation of new groups through the repository layer. This ViewModel
 * follows clean architecture principles by delegating all data operations to the repository and
 * never directly accessing Firebase or Firestore.
 *
 * Validation rules:
 * - Name: Required, 3-30 characters, letters/numbers/spaces/underscores only
 * - Category: Required, must be one of: Social, Activity, Sports
 * - Description: Optional, max 300 characters
 *
 * State machine:
 * - Idle/Editing: User is filling the form, live validation occurs
 * - Submitting: Form is being submitted (isLoading=true)
 * - Success: Group created successfully (createdGroupId set)
 * - Error: Creation failed (errorMsg set)
 *
 * @param repository Repository for group operations
 */
class CreateGroupViewModel(
    private val repository: GroupRepository = GroupRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(CreateGroupUIState())
  val uiState: StateFlow<CreateGroupUIState> = _uiState.asStateFlow()

  /**
   * Sets the group name and validates it
   *
   * @param name The group name to set
   */
  fun setName(name: String) {
    val error = validateName(name)
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
    _uiState.value = _uiState.value.copy(createdGroupId = null)
  }

  /**
   * Creates a new group through the repository
   *
   * Validates the form, delegates creation to the repository, and updates UI state based on success
   * or failure. The repository handles all Firestore operations including:
   * - Creating the group document
   * - Adding the creator as a member with admin role
   *
   * State transitions:
   * - Before: isLoading=false, createdGroupId=null, errorMsg=null
   * - During: isLoading=true
   * - Success: isLoading=false, createdGroupId=<id>
   * - Error: isLoading=false, errorMsg=<message>
   */
  fun createGroup() {
    viewModelScope.launch {
      val state = _uiState.value

      // Guard: Don't submit if form is invalid
      if (!state.isValid) return@launch

      // Transition to loading state
      _uiState.value = state.copy(isLoading = true, errorMsg = null, createdGroupId = null)

      try {
        // Delegate to repository - it handles all Firestore operations
        val groupId =
            repository.createGroup(
                name = state.name,
                category = state.category,
                description = state.description.ifBlank { "" })

        // Transition to success state
        _uiState.value = state.copy(isLoading = false, createdGroupId = groupId)
      } catch (_: IllegalStateException) {
        // User authentication error
        _uiState.value =
            state.copy(isLoading = false, errorMsg = "You must be logged in to create a group")
      } catch (e: Exception) {
        // Generic error - propagate user-friendly message
        _uiState.value =
            state.copy(
                isLoading = false,
                errorMsg = "Failed to create group: ${e.message ?: "Unknown error"}")
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
   *
   * @param name The name to validate
   * @return Error message if invalid, null if valid
   */
  private fun validateName(name: String): String? {
    return when {
      name.isBlank() -> "Name is required"
      name.length < 3 -> "Name must be at least 3 characters"
      name.length > 30 -> "Name must not exceed 30 characters"
      !name.matches(Regex("^[a-zA-Z0-9_ ]+$")) ->
          "Only letters, numbers, spaces, and underscores allowed"
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
      description.length > 300 -> "Description must not exceed 300 characters"
      else -> null
    }
  }

  /**
   * Updates the form validity based on current field values
   *
   * Form is valid when:
   * - Name is not blank
   * - Category is not blank
   * - No validation errors exist
   */
  private fun updateFormValidity() {
    val state = _uiState.value
    val isValid =
        state.name.isNotBlank() && state.nameError == null && state.descriptionError == null
    _uiState.value = state.copy(isValid = isValid)
  }
}
