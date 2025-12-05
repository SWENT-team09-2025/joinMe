package com.android.joinme.ui.groups

import android.content.Context
import android.net.Uri
import android.util.Log
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
 * @property photoUrl Current photo URL of the group
 * @property isUploadingPhoto Whether a photo upload/delete operation is in progress
 * @property photoError Error message for photo operations
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
    override val errorMsg: String? = null,
    val photoUrl: String? = null,
    val isUploadingPhoto: Boolean = false,
    val photoError: String? = null
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
    private const val TAG = "EditGroupViewModel"
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

  /** Clears the photo error state. */
  fun clearPhotoError() {
    _uiState.value = _uiState.value.copy(photoError = null)
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
                photoUrl = group.photoUrl,
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

  /**
   * Uploads a photo for the group.
   *
   * This method handles the complete photo upload flow:
   * 1. Uploads the photo to Firebase Storage (with compression and orientation correction)
   * 2. Updates the group's photoUrl in Firestore
   * 3. Updates the local UI state with the new photo URL
   *
   * @param context Android context needed for image processing
   * @param groupId The ID of the group to upload the photo for
   * @param imageUri The URI of the image to upload
   * @param onSuccess Callback invoked after successful upload
   * @param onError Callback invoked if upload fails, receives error message
   */
  fun uploadGroupPhoto(
      context: Context,
      groupId: String,
      imageUri: Uri,
      onSuccess: () -> Unit = {},
      onError: (String) -> Unit = {}
  ) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isUploadingPhoto = true, photoError = null)

      try {
        Log.d(TAG, "Starting photo upload for group $groupId")

        val downloadUrl = repository.uploadGroupPhoto(context, groupId, imageUri)

        _uiState.value = _uiState.value.copy(isUploadingPhoto = false, photoUrl = downloadUrl)

        onSuccess()
      } catch (e: Exception) {
        val errorMsg = "Failed to upload photo: ${e.message ?: ERROR_UNKNOWN}"
        Log.e(TAG, "Error uploading photo", e)
        _uiState.value = _uiState.value.copy(isUploadingPhoto = false, photoError = errorMsg)
        onError(errorMsg)
      }
    }
  }

  /**
   * Deletes the group's photo.
   *
   * This method:
   * 1. Deletes the photo from Firebase Storage
   * 2. Clears the photoUrl field in Firestore
   * 3. Updates the local UI state to remove the photo URL
   *
   * @param groupId The ID of the group to delete the photo for
   * @param onSuccess Callback invoked after successful deletion
   * @param onError Callback invoked if deletion fails, receives error message
   */
  fun deleteGroupPhoto(
      groupId: String,
      onSuccess: () -> Unit = {},
      onError: (String) -> Unit = {}
  ) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isUploadingPhoto = true, photoError = null)

      try {
        repository.deleteGroupPhoto(groupId)

        _uiState.value = _uiState.value.copy(isUploadingPhoto = false, photoUrl = null)

        onSuccess()
      } catch (e: Exception) {
        val errorMsg = "Failed to delete photo: ${e.message ?: ERROR_UNKNOWN}"
        Log.e(TAG, "Error deleting photo", e)
        _uiState.value = _uiState.value.copy(isUploadingPhoto = false, photoError = errorMsg)
        onError(errorMsg)
      }
    }
  }
}
