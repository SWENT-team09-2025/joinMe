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
    Log.e(TAG, "Timeout loading profile", e)
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
   * @param profile The [Profile] object to create or update.
   * @param onSuccess Callback invoked after successful profile update.
   * @param onError Callback invoked if update fails, receives error message.
   */
  fun createOrUpdateProfile(
      profile: Profile,
      onSuccess: () -> Unit = {},
      onError: (String) -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        _isLoading.value = true
        _error.value = null

        withTimeout(10000L) { repository.createOrUpdateProfile(profile) }

        _profile.value = profile
        onSuccess()
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Timeout updating profile", e)
        val errorMsg = ERROR_CONNECTION_TIMEOUT
        _error.value = errorMsg
        onError(errorMsg)
      } catch (e: Exception) {
        Log.e(TAG, "Error creating/updating profile", e)
        val errorMsg = ERROR_SAVE_PROFILE_FAILED.format(e.message)
        _error.value = errorMsg
        onError(errorMsg)
      } finally {
        _isLoading.value = false
      }
    }
  }

  /**
   * Uploads a profile photo for the current user.
   *
   * This method handles the complete photo upload flow:
   * 1. Validates that a profile is loaded
   * 2. Uploads the photo to Firebase Storage (with compression and orientation correction)
   * 3. Updates the profile's photoUrl in Firestore
   * 4. Refreshes the local profile state
   *
   * @param context Android context needed for image processing
   * @param imageUri The URI of the image to upload
   * @param onSuccess Callback invoked after successful upload
   * @param onError Callback invoked if upload fails, receives error message
   */
  fun uploadProfilePhoto(
      context: Context,
      imageUri: Uri,
      onSuccess: () -> Unit = {},
      onError: (String) -> Unit = {}
  ) {
    val currentProfile = _profile.value
    if (currentProfile == null) {
      val errorMsg = "No profile loaded"
      _photoUploadError.value = errorMsg
      onError(errorMsg)
      return
    }

    viewModelScope.launch {
      try {
        _isUploadingPhoto.value = true
        _photoUploadError.value = null

        // Upload photo and get download URL
        // The repository handles image processing, upload, and Firestore update
        val downloadUrl =
            withTimeout(30000L) { // 30 second timeout for upload
              repository.uploadProfilePhoto(context, currentProfile.uid, imageUri)
            }

        // Update local profile state with new photo URL
        val updatedProfile = currentProfile.copy(photoUrl = downloadUrl)
        _profile.value = updatedProfile

        onSuccess()
      } catch (e: TimeoutCancellationException) {
        val errorMsg = "Upload timeout. Please check your connection and try again."
        Log.e(TAG, "Timeout uploading photo", e)
        _photoUploadError.value = errorMsg
        onError(errorMsg)
      } catch (e: Exception) {
        val errorMsg = "Failed to upload photo: ${e.message}"
        Log.e(TAG, "Error uploading photo", e)
        _photoUploadError.value = errorMsg
        onError(errorMsg)
      } finally {
        _isUploadingPhoto.value = false
      }
    }
  }

  /**
   * Deletes the current user's profile photo.
   *
   * This method:
   * 1. Deletes the photo from Firebase Storage
   * 2. Clears the photoUrl field in Firestore
   * 3. Updates the local profile state
   *
   * @param onSuccess Callback invoked after successful deletion
   * @param onError Callback invoked if deletion fails, receives error message
   */
  fun deleteProfilePhoto(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
    val currentProfile = _profile.value
    if (currentProfile == null) {
      val errorMsg = "No profile loaded"
      _photoUploadError.value = errorMsg
      onError(errorMsg)
      return
    }

    viewModelScope.launch {
      try {
        _isUploadingPhoto.value = true
        _photoUploadError.value = null

        withTimeout(10000L) { repository.deleteProfilePhoto(currentProfile.uid) }

        // Update local profile state to remove photo URL
        val updatedProfile = currentProfile.copy(photoUrl = null)
        _profile.value = updatedProfile

        onSuccess()
      } catch (e: TimeoutCancellationException) {
        val errorMsg = "Delete timeout. Please try again."
        Log.e(TAG, "Timeout deleting photo", e)
        _photoUploadError.value = errorMsg
        onError(errorMsg)
      } catch (e: Exception) {
        val errorMsg = "Failed to delete photo: ${e.message}"
        Log.e(TAG, "Error deleting photo", e)
        _photoUploadError.value = errorMsg
        onError(errorMsg)
      } finally {
        _isUploadingPhoto.value = false
      }
    }
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
