package com.android.joinme.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing profile-related UI state and operations.
 *
 * @property repository The repository used for profile data operations, provided by
 *   [ProfileRepositoryProvider].
 */
class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

  private val _profile = MutableStateFlow<Profile?>(null)
  val profile: StateFlow<Profile?> = _profile.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  /**
   * Loads a user profile by UID.
   *
   * @param uid The unique identifier of the user profile to load.
   */
  fun loadProfile(uid: String) {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null
      try {
        val fetchedProfile = repository.getProfile(uid)
        _profile.value = fetchedProfile
      } catch (e: NoSuchElementException) {
        Log.e(TAG, "Profile not found: ${e.message}")
        _error.value = "Profile not found"
        _profile.value = null
      } catch (e: Exception) {
        Log.e(TAG, "Error loading profile", e)
        _error.value = "Failed to load profile: ${e.message}"
        _profile.value = null
      } finally {
        _isLoading.value = false
      }
    }
  }

  /**
   * Creates or updates a user profile.
   *
   * @param profile The profile to create or update.
   */
  fun createOrUpdateProfile(profile: Profile) {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null
      try {
        repository.createOrUpdateProfile(profile)
        _profile.value = profile
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
   * @param uid The unique identifier of the user profile to delete.
   */
  fun deleteProfile(uid: String) {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null
      try {
        repository.deleteProfile(uid)
        _profile.value = null
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

  /** Clears the current profile state. */
  fun clearProfile() {
    _profile.value = null
  }
  
  /** Gets the validation error message for username. */
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

  /** Gets the validation error message for date of birth. */
  fun getDateOfBirthError(dateOfBirth: String): String? {
    if (dateOfBirth.isBlank()) return null // Optional field
    if (!dateOfBirth.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$"))) {
      return "Enter your date in dd/mm/yyyy format."
    }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    dateFormat.isLenient = false
    return try {
      dateFormat.parse(dateOfBirth)
      null
    } catch (e: Exception) {
      "Invalid date"
    }
  }

  companion object {
    private const val TAG = "ProfileViewModel"
  }
}
