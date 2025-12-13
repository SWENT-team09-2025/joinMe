package com.android.joinme.ui.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.authentification.AuthRepository
import com.android.joinme.model.authentification.AuthRepositoryProvider
import com.android.joinme.model.invitation.InvitationRepositoryProvider
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * ViewModel for managing profile-related UI state and operations.
 *
 * This ViewModel handles all profile data operations including loading, creating, updating, and
 * deleting user profiles. It coordinates with [ProfileRepository] for data persistence and
 * [AuthRepository] for authentication-related operations. All operations are performed
 * asynchronously using coroutines and expose state via StateFlows.
 *
 * The ViewModel implements profile bootstrapping: if a profile doesn't exist for a user, it
 * automatically creates one with default values derived from authentication data.
 *
 * State management:
 * - [profile]: The current user's profile data, or null if not loaded or doesn't exist
 * - [isLoading]: Indicates whether a profile operation is in progress
 * - [error]: Contains error messages from failed operations, or null if no error
 * - [isUploadingPhoto]: Indicates whether a photo upload is in progress
 * - [photoUploadError]: Contains photo-specific error messages
 *
 * @param repository The [ProfileRepository] for profile data operations. Defaults to the provider
 *   instance.
 * @param authRepository The [AuthRepository] for authentication operations. Defaults to the
 *   provider instance.
 */
class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val authRepository: AuthRepository = AuthRepositoryProvider.repository,
) : ViewModel() {

  val invitationalRepo = InvitationRepositoryProvider.repository

  private val _profile = MutableStateFlow<Profile?>(null)
  val profile: StateFlow<Profile?> = _profile.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  // Photo upload specific states
  private val _isUploadingPhoto = MutableStateFlow(false)
  val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

  private val _photoUploadError = MutableStateFlow<String?>(null)
  val photoUploadError: StateFlow<String?> = _photoUploadError.asStateFlow()

  // Pending photo changes (deferred until save)
  private val _pendingPhotoUri = MutableStateFlow<Uri?>(null)
  val pendingPhotoUri: StateFlow<Uri?> = _pendingPhotoUri.asStateFlow()

  private val _pendingPhotoDelete = MutableStateFlow(false)
  val pendingPhotoDelete: StateFlow<Boolean> = _pendingPhotoDelete.asStateFlow()

  /**
   * Loads a user profile by UID, with automatic profile creation if it doesn't exist.
   *
   * This method first attempts to fetch an existing profile. If no profile exists, it bootstraps a
   * new profile using the user's email from authentication and a derived username. The operation
   * has a 10-second timeout to prevent indefinite waiting.
   *
   * @param uid The unique identifier of the user whose profile should be loaded.
   */
  fun loadProfile(uid: String) {
    viewModelScope.launch(Dispatchers.Main) {
      // Skip if already loaded for this UID
      if (_profile.value?.uid == uid) {
        return@launch
      }

      _isLoading.value = true
      clearError()
      clearProfile()

      // Validate uid is not empty
      if (uid.isBlank()) {
        handleInvalidUid()
        return@launch
      }

      try {
        val fetched = fetchProfileWithTimeout(uid)
        _profile.value = fetched ?: bootstrapNewProfile(uid)
      } catch (e: TimeoutCancellationException) {
        handleLoadTimeout(e)
      } catch (e: Exception) {
        handleLoadError(e)
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Fetches a profile with timeout, returning null if profile doesn't exist. */
  private suspend fun fetchProfileWithTimeout(uid: String): Profile? {
    return withTimeout(10000L) {
      try {
        repository.getProfile(uid)
      } catch (_: NoSuchElementException) {
        null // Profile doesn't exist, will bootstrap
      }
    }
  }

  /** Creates and persists a new profile for the given UID. */
  private suspend fun bootstrapNewProfile(uid: String): Profile {
    val email =
        try {
          authRepository.getCurrentUserEmail() ?: ""
        } catch (e: Exception) {
          Log.e(TAG, "Error getting email", e)
          ""
        }

    val username = deriveDefaultUsername(email, uid)

    val newProfile =
        Profile(
            uid = uid,
            email = email,
            username = username,
            dateOfBirth = null,
            country = null,
            interests = emptyList(),
            bio = null,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now())

    withTimeout(10000L) { repository.createOrUpdateProfile(newProfile) }
    return newProfile
  }

  /** Handles invalid UID error. */
  private fun handleInvalidUid() {
    _isLoading.value = false
    _error.value = "User not authenticated. Please sign in again."
  }

  /** Handles timeout during profile loading. */
  private fun handleLoadTimeout(e: TimeoutCancellationException) {
    _profile.value = null
    _error.value = "Connection timeout. Please check your internet connection and try again."
  }

  /** Handles general errors during profile loading. */
  private fun handleLoadError(e: Exception) {
    _profile.value = null
    _error.value = "Failed to load profile: ${e.message}"
  }

  /**
   * Creates or updates a user profile in the repository.
   *
   * This method persists profile changes and updates the local state upon success. The operation
   * has a 10-second timeout to prevent indefinite waiting.
   *
   * If there are pending photo changes (upload or delete), they are applied here after the profile
   * update. This ensures photo operations only persist when the user clicks "Save Changes".
   *
   * @param profile The [Profile] object to create or update.
   * @param context Android context needed for photo upload (image processing). Required if there's
   *   a pending photo upload.
   * @param onSuccess Callback invoked after successful profile update.
   * @param onError Callback invoked if update fails, receives error message.
   */
  fun createOrUpdateProfile(
      profile: Profile,
      context: Context? = null,
      onSuccess: () -> Unit = {},
      onError: (String) -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        _isLoading.value = true
        _error.value = null

        withTimeout(10000L) { repository.createOrUpdateProfile(profile) }

        // Handle pending photo changes
        var photoError: String? = null
        var updatedProfile = profile
        when {
          // Upload pending photo if selected
          _pendingPhotoUri.value != null && context != null -> {
            try {
              _isUploadingPhoto.value = true

              val downloadUrl =
                  withTimeout(30000L) {
                    repository.uploadProfilePhoto(context, profile.uid, _pendingPhotoUri.value!!)
                  }

              updatedProfile = profile.copy(photoUrl = downloadUrl)
              _isUploadingPhoto.value = false
            } catch (e: Exception) {
              photoError = "Profile updated but photo upload failed: ${e.message}"
              _isUploadingPhoto.value = false
            }
          }
          // Delete photo if marked for deletion
          _pendingPhotoDelete.value -> {
            try {
              _isUploadingPhoto.value = true

              withTimeout(10000L) { repository.deleteProfilePhoto(profile.uid) }

              updatedProfile = profile.copy(photoUrl = null)
              _isUploadingPhoto.value = false
            } catch (e: Exception) {
              photoError = "Profile updated but photo deletion failed: ${e.message}"
              _isUploadingPhoto.value = false
            }
          }
        }

        _profile.value = updatedProfile
        _pendingPhotoUri.value = null
        _pendingPhotoDelete.value = false
        _photoUploadError.value = photoError

        onSuccess()
      } catch (e: TimeoutCancellationException) {
        val errorMsg = ERROR_CONNECTION_TIMEOUT
        _error.value = errorMsg
        onError(errorMsg)
      } catch (e: Exception) {
        val errorMsg = ERROR_SAVE_PROFILE_FAILED.format(e.message)
        _error.value = errorMsg
        onError(errorMsg)
      } finally {
        _isLoading.value = false
      }
    }
  }

  /**
   * Sets the pending photo URI for local preview.
   *
   * The photo is NOT uploaded immediately - it's stored locally and displayed as a preview. The
   * actual upload happens only when createOrUpdateProfile() is called with the context parameter.
   * This prevents persisting changes until the user clicks "Save Changes".
   *
   * @param uri The URI of the selected photo
   */
  fun setPendingPhoto(uri: Uri) {
    _pendingPhotoUri.value = uri
    _pendingPhotoDelete.value = false
    _photoUploadError.value = null
  }

  /**
   * Marks the photo for deletion (deferred until save).
   *
   * This method does NOT immediately delete the photo from Firebase Storage. Instead, it marks the
   * photo for deletion and clears any pending upload. The actual deletion happens only when
   * createOrUpdateProfile() is called. This allows users to cancel the deletion by navigating back
   * without saving.
   */
  fun markPhotoForDeletion() {
    _pendingPhotoDelete.value = true
    _pendingPhotoUri.value = null
    _photoUploadError.value = null
  }

  /**
   * Clears any pending photo changes (upload or delete).
   *
   * This method is called when the user navigates back without saving, discarding any pending photo
   * changes.
   */
  fun clearPendingPhotoChanges() {
    _pendingPhotoUri.value = null
    _pendingPhotoDelete.value = false
    _photoUploadError.value = null
  }

  /**
   * Deletes a user profile by UID.
   *
   * This method removes the profile from the repository and clears the local state. The operation
   * has a 10-second timeout to prevent indefinite waiting.
   *
   * @param uid The unique identifier of the profile to delete.
   */
  fun deleteProfile(uid: String) {
    viewModelScope.launch {
      try {
        _isLoading.value = true
        _error.value = null

        withTimeout(10000L) { repository.deleteProfile(uid) }
        val invitationsId = invitationalRepo.getInvitationsByUser(userId = uid).getOrNull()
        invitationsId?.forEach { invitationalRepo.revokeInvitation(it.token) }
        _profile.value = null
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Timeout deleting profile", e)
        _error.value = "Connection timeout. Please try again."
      } catch (e: Exception) {
        Log.e(TAG, "Error deleting profile", e)
        _error.value = "Failed to delete profile: ${e.message}"
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Clears the current error state. */
  fun clearError() {
    _error.value = null
  }

  /** Sets an error message. */
  fun setError(message: String) {
    _error.value = message
  }

  /** Clears the photo upload error state. */
  fun clearPhotoUploadError() {
    _photoUploadError.value = null
  }

  /** Clears the current profile state. */
  fun clearProfile() {
    _profile.value = null
  }

  /**
   * Validates a username according to app requirements.
   *
   * Username requirements:
   * - Not empty
   * - At least 3 characters long
   * - At most 30 characters long
   * - Only contains letters, numbers, spaces, and underscores
   *
   * @param username The username to validate.
   * @return An error message if validation fails, or null if the username is valid.
   */
  fun getUsernameError(username: String): String? {
    return when {
      username.isEmpty() -> "Username is required"
      username.length < 3 -> "Username must be at least 3 characters"
      username.length > 30 -> "Username must not exceed 30 characters"
      !username.matches(Regex("^[a-zA-Z0-9_ ]+$")) ->
          "Only letters, numbers, spaces, and underscores allowed"
      else -> null
    }
  }

  /**
   * Validates a date of birth string in dd/MM/yyyy format.
   *
   * @param dateOfBirth The date string to validate. Empty strings are considered valid.
   * @return An error message if validation fails, or null if the date is valid or empty.
   */
  fun getDateOfBirthError(dateOfBirth: String): String? {
    if (dateOfBirth.isBlank()) return null
    if (!dateOfBirth.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$"))) {
      return "Enter your date in dd/mm/yyyy format."
    }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
    return try {
      dateFormat.parse(dateOfBirth)
      null
    } catch (_: Exception) {
      "Invalid date"
    }
  }

  /**
   * Signs out the current user via the authentication repository.
   *
   * @param onComplete Callback invoked after successful sign-out.
   * @param onError Callback invoked if sign-out fails, receiving the error message.
   */
  fun signOut(onComplete: () -> Unit, onError: (String) -> Unit) {
    viewModelScope.launch {
      try {
        authRepository.signOut()
        onComplete()
      } catch (e: Exception) {
        Log.e(TAG, "Error signing out", e)
        onError("Failed to sign out: ${e.message}")
      }
    }
  }

  /**
   * Derives a default username from an email address or UID.
   *
   * This method extracts the local part of an email (before @) and sanitizes it, or falls back to
   * "user_" + last 6 characters of UID if the email is invalid.
   *
   * @param email The user's email address.
   * @param uid The user's unique identifier, used as fallback.
   * @return A sanitized username suitable for the app, max 30 characters.
   */
  private fun deriveDefaultUsername(email: String, uid: String): String {
    val base = email.substringBefore('@').ifBlank { "user_${uid.takeLast(6)}" }
    return base.replace(Regex("[^A-Za-z0-9_ ]"), "_").take(30).ifBlank { "user_${uid.takeLast(6)}" }
  }

  companion object {
    private const val TAG = "ProfileViewModel"
    private const val ERROR_CONNECTION_TIMEOUT = "Connection timeout. Please try again."
    private const val ERROR_SAVE_PROFILE_FAILED = "Failed to save profile: %s"
  }
}
