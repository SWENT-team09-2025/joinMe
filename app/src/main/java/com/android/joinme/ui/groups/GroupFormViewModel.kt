package com.android.joinme.ui.groups

import androidx.lifecycle.ViewModel
import com.android.joinme.model.event.EventType
import kotlinx.coroutines.flow.MutableStateFlow

/** Base interface for group form UI state. */
interface GroupFormUIState {
  val name: String
  val category: EventType
  val description: String
  val nameError: String?
  val descriptionError: String?
  val isValid: Boolean
  val isLoading: Boolean
  val errorMsg: String?
}

/**
 * Base ViewModel for group form screens.
 *
 * Contains all shared validation and business logic for creating/editing groups. Subclasses must
 * implement the abstract properties to provide their specific UI state management.
 */
abstract class BaseGroupFormViewModel : ViewModel() {

  companion object {
    // Validation limits
    const val NAME_MIN_LENGTH = 3
    const val NAME_MAX_LENGTH = 30
    const val DESCRIPTION_MAX_LENGTH = 300

    // Error messages
    const val ERROR_NAME_REQUIRED = "Name is required"
    const val ERROR_NAME_TOO_SHORT = "Name must be at least 3 characters"
    const val ERROR_NAME_TOO_LONG = "Name must not exceed 30 characters"
    const val ERROR_NAME_INVALID_CHARS = "Only letters, numbers, spaces, and underscores allowed"
    const val ERROR_NAME_MULTIPLE_SPACES = "Multiple consecutive spaces not allowed"
    const val ERROR_DESCRIPTION_TOO_LONG = "Description must not exceed 300 characters"
  }

  /** The mutable state flow for the UI state. Subclasses must provide their specific state type. */
  protected abstract val _uiState: MutableStateFlow<out GroupFormUIState>

  /** Gets the current UI state value. */
  protected abstract fun getState(): GroupFormUIState

  /** Updates the UI state. Subclasses must implement to update their specific state type. */
  protected abstract fun updateState(transform: (GroupFormUIState) -> GroupFormUIState)

  /** Clears the global error message. */
  fun clearErrorMsg() {
    updateState { state ->
      when (state) {
        is CreateGroupUIState -> state.copy(errorMsg = null)
        is EditGroupUIState -> state.copy(errorMsg = null)
        else -> state
      }
    }
  }

  /** Sets the global error message. */
  protected fun setErrorMsg(msg: String) {
    updateState { state ->
      when (state) {
        is CreateGroupUIState -> state.copy(errorMsg = msg)
        is EditGroupUIState -> state.copy(errorMsg = msg)
        else -> state
      }
    }
  }

  /**
   * Sets the group name and validates it
   *
   * Validates against the following rules:
   * - Required (not blank)
   * - 3-30 characters
   * - Only letters, numbers, spaces, and underscores
   * - No multiple consecutive spaces
   *
   * @param name The group name to set
   */
  fun setName(name: String) {
    val trimmedName = name.trim()
    val error = validateName(trimmedName)
    updateState { state ->
      when (state) {
        is CreateGroupUIState ->
            state.copy(
                name = name,
                nameError = error,
                isValid = computeValidity(trimmedName, error, state.descriptionError))
        is EditGroupUIState ->
            state.copy(
                name = name,
                nameError = error,
                isValid = computeValidity(trimmedName, error, state.descriptionError))
        else -> state
      }
    }
  }

  /**
   * Sets the group category
   *
   * @param category The category to set (Social/Activity/Sports)
   */
  fun setCategory(category: EventType) {
    updateState { state ->
      when (state) {
        is CreateGroupUIState -> state.copy(category = category)
        is EditGroupUIState -> state.copy(category = category)
        else -> state
      }
    }
  }

  /**
   * Sets the group description and validates it
   *
   * Validates that the description does not exceed 300 characters.
   *
   * @param description The description to set
   */
  fun setDescription(description: String) {
    val error = validateDescription(description)
    updateState { state ->
      when (state) {
        is CreateGroupUIState ->
            state.copy(
                description = description,
                descriptionError = error,
                isValid = computeValidity(state.name.trim(), state.nameError, error))
        is EditGroupUIState ->
            state.copy(
                description = description,
                descriptionError = error,
                isValid = computeValidity(state.name, state.nameError, error))
        else -> state
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
   * @param name The name to validate (should be trimmed)
   * @return Error message if invalid, null if valid
   */
  protected fun validateName(name: String): String? {
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
  protected fun validateDescription(description: String): String? {
    return when {
      description.length > DESCRIPTION_MAX_LENGTH -> ERROR_DESCRIPTION_TOO_LONG
      else -> null
    }
  }

  /**
   * Computes whether the form is valid based on field values and validation errors
   *
   * Form is valid when:
   * - Name is not blank
   * - No validation errors exist for name or description
   *
   * @param name The group name
   * @param nameError Validation error for name field
   * @param descriptionError Validation error for description field
   * @return true if form is valid, false otherwise
   */
  protected fun computeValidity(
      name: String,
      nameError: String?,
      descriptionError: String?
  ): Boolean {
    return name.isNotBlank() && nameError == null && descriptionError == null
  }
}
