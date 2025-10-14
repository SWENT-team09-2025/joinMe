package com.android.joinme.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.authentification.AuthRepository
import com.android.joinme.model.authentification.AuthRepositoryProvider
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

  private val _profile = MutableStateFlow<Profile?>(null)
  val profile: StateFlow<Profile?> = _profile.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

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
      _isLoading.value = true
      clearError()
      clearProfile()

      try {
        // Try to fetch the profile (with timeout to prevent infinite waiting)
        val fetched =
            withTimeout(10000L) {
              try {
                repository.getProfile(uid)
              } catch (_: NoSuchElementException) {
                null // Profile doesn't exist, will bootstrap
              }
            }

        if (fetched != null) {
          _profile.value = fetched
        } else {
          // Bootstrap: create a new profile
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

          // Create profile with timeout
          withTimeout(10000L) { repository.createOrUpdateProfile(newProfile) }

          _profile.value = newProfile
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Timeout loading profile", e)
        _profile.value = null
        _error.value = "Connection timeout. Please check your internet connection and try again."
      } catch (e: Exception) {
        Log.e(TAG, "Error loading profile", e)
        _profile.value = null
        _error.value = "Failed to load profile: ${e.message}"
      } finally {
        _isLoading.value = false
      }
    }
  }

  /**
   * Creates or updates a user profile in the repository.
   *
   * This method persists profile changes and updates the local state upon success. The operation
   * has a 10-second timeout to prevent indefinite waiting.
   *
   * @param profile The [Profile] object to create or update.
   */
  fun createOrUpdateProfile(profile: Profile) {
    viewModelScope.launch {
      try {
        _isLoading.value = true
        _error.value = null

        withTimeout(10000L) { repository.createOrUpdateProfile(profile) }

        _profile.value = profile
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Timeout updating profile", e)
        _error.value = "Connection timeout. Please try again."
      } catch (e: Exception) {
        Log.e(TAG, "Error creating/updating profile", e)
        _error.value = "Failed to save profile: ${e.message}"
      } finally {
        _isLoading.value = false
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
  }
}
