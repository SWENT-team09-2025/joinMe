package com.android.joinme.ui.groups

// AI-assisted implementation — reviewed and adapted for project standards.

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.util.TestEnvironmentDetector
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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
 * @property pendingPhotoUri URI of the selected photo to upload (not yet uploaded)
 * @property photoError Error message for photo operations
 */
data class CreateGroupUIState(
    override val name: String = "",
    override val category: EventType = EventType.ACTIVITY,
    override val description: String = "",
    override val nameError: String? = null,
    override val descriptionError: String? = null,
    override val isValid: Boolean = false,
    override val isLoading: Boolean = false,
    val createdGroupId: String? = null,
    override val errorMsg: String? = null,
    val pendingPhotoUri: Uri? = null,
    val photoError: String? = null
) : GroupFormUIState

/**
 * ViewModel for Create Group screen
 *
 * Handles validation and creation of new groups through the repository layer. This ViewModel
 * follows clean architecture principles by delegating all data operations to the repository and
 * never directly accessing Firebase or Firestore.
 *
 * Photo handling strategy:
 * - When user selects a photo, the URI is stored locally in pendingPhotoUri (no upload yet)
 * - The UI shows a local preview of the selected image
 * - Photo is only uploaded to Firebase Storage when createGroup() is called
 * - This prevents orphaned files if user abandons the creation screen
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
) : BaseGroupFormViewModel() {

  companion object {
    private const val ERROR_NOT_AUTHENTICATED = "You must be logged in to create a group"
    private const val ERROR_CREATE_FAILED = "Failed to create group"
    private const val ERROR_PHOTO_UPLOAD_FAILED = "Group created but photo upload failed"
    private const val ERROR_UNKNOWN = "Unknown error"
  }

  override val _uiState = MutableStateFlow(CreateGroupUIState())
  val uiState: StateFlow<CreateGroupUIState> = _uiState.asStateFlow()

  override fun getState(): GroupFormUIState = _uiState.value

  override fun updateState(transform: (GroupFormUIState) -> GroupFormUIState) {
    _uiState.value = transform(_uiState.value) as CreateGroupUIState
  }

  /**
   * Sets the pending photo URI for local preview.
   *
   * The photo is NOT uploaded immediately - it's stored locally and displayed as a preview. The
   * actual upload happens only when createGroup() is called. This prevents orphaned files in
   * Storage if the user abandons the creation screen.
   *
   * @param uri The URI of the selected photo, or null to clear
   */
  fun setPendingPhoto(uri: Uri?) {
    _uiState.value = _uiState.value.copy(pendingPhotoUri = uri, photoError = null)
  }

  /**
   * Clears the pending photo selection.
   *
   * Removes the locally stored photo URI. Since the photo hasn't been uploaded yet, there's nothing
   * to clean up in Firebase Storage.
   */
  fun clearPendingPhoto() {
    _uiState.value = _uiState.value.copy(pendingPhotoUri = null, photoError = null)
  }

  /** Clears the photo error message. */
  fun clearPhotoError() {
    _uiState.value = _uiState.value.copy(photoError = null)
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
   * or failure. If a photo was selected, it will be uploaded after the group is created.
   *
   * The photo upload happens AFTER group creation to ensure we have a valid groupId for the storage
   * path. If photo upload fails, the group is still created but user is notified of the photo
   * error.
   *
   * State transitions:
   * - Before: isLoading=false, createdGroupId=null, errorMsg=null
   * - During: isLoading=true
   * - Success: isLoading=false, createdGroupId=<id>
   * - Success with photo error: isLoading=false, createdGroupId=<id>, photoError=<message>
   * - Error: isLoading=false, errorMsg=<message>
   *
   * @param context Android context required for photo upload (image processing)
   */
  fun createGroup(context: Context? = null) {
    viewModelScope.launch {
      val state = _uiState.value

      // Guard: Don't submit if form is invalid
      if (!state.isValid) return@launch

      // Transition to loading state
      _uiState.value =
          state.copy(isLoading = true, errorMsg = null, createdGroupId = null, photoError = null)

      try {
        val uid =
            Firebase.auth.currentUser?.uid
                ?: if (TestEnvironmentDetector.isTestEnvironment())
                    TestEnvironmentDetector.getTestUserId()
                else throw IllegalStateException("User not authenticated")

        val groupId = repository.getNewGroupId()

        val group =
            Group(
                id = groupId,
                name = state.name.trim(),
                category = state.category,
                description = state.description.ifBlank { "" },
                ownerId = uid,
                memberIds = listOf(uid),
                eventIds = emptyList(),
                photoUrl = null)

        repository.addGroup(group)

        // Upload photo if one was selected
        var photoError: String? = null
        if (state.pendingPhotoUri != null && context != null) {
          try {
            repository.uploadGroupPhoto(context, groupId, state.pendingPhotoUri)
          } catch (e: Exception) {
            // Group was created successfully, but photo upload failed
            photoError = "$ERROR_PHOTO_UPLOAD_FAILED: ${e.message ?: ERROR_UNKNOWN}"
          }
        }

        // Transition to success state
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                createdGroupId = groupId,
                pendingPhotoUri = null,
                photoError = photoError)
      } catch (_: IllegalStateException) {
        // User authentication error
        _uiState.value = _uiState.value.copy(isLoading = false, errorMsg = ERROR_NOT_AUTHENTICATED)
      } catch (e: Exception) {
        // Generic error - propagate user-friendly message
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMsg = "$ERROR_CREATE_FAILED: ${e.message ?: ERROR_UNKNOWN}")
      }
    }
  }
}
