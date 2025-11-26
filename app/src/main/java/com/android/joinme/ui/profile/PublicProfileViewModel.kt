package com.android.joinme.ui.profile
/** This file was implemented with the help of AI */
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryProvider
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.groups.GroupRepositoryProvider
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing public profile view state.
 *
 * This ViewModel handles displaying another user's profile along with common events and groups
 * shared between the viewer and the profile owner. It coordinates with multiple repositories to
 * fetch profile data, common events, and common groups.
 *
 * State management:
 * - [profile]: The viewed user's profile data, or null if not loaded
 * - [commonEvents]: List of events shared between viewer and profile owner
 * - [commonGroups]: List of groups shared between viewer and profile owner
 * - [isLoading]: Indicates whether data is being loaded
 * - [error]: Contains error messages from failed operations, or null if no error
 *
 * @param profileRepository The [ProfileRepository] for profile data operations
 * @param eventsRepository The [EventsRepository] for event data operations
 * @param groupRepository The [GroupRepository] for group data operations
 */
class PublicProfileViewModel(
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val eventsRepository: EventsRepository = EventsRepositoryProvider.getRepository(true),
    private val groupRepository: GroupRepository = GroupRepositoryProvider.repository
) : ViewModel() {
  companion object {
    /** Tag for logging purposes. */
    private const val TAG = "PublicProfileViewModel"
  }

  private val _profile = MutableStateFlow<Profile?>(null)
  val profile: StateFlow<Profile?> = _profile.asStateFlow()

  private val _commonEvents = MutableStateFlow<List<Event>>(emptyList())
  val commonEvents: StateFlow<List<Event>> = _commonEvents.asStateFlow()

  private val _commonGroups = MutableStateFlow<List<Group>>(emptyList())
  val commonGroups: StateFlow<List<Group>> = _commonGroups.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _isFollowing = MutableStateFlow(false)
  val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

  private val _isFollowLoading = MutableStateFlow(false)
  val isFollowLoading: StateFlow<Boolean> = _isFollowLoading.asStateFlow()

  /**
   * Loads the public profile for a specific user along with common events and groups.
   *
   * This method fetches:
   * 1. The profile of the specified user
   * 2. Events that both the current user and specified user participate in
   * 3. Groups that both the current user and specified user are members of
   *
   * @param userId The unique identifier of the user whose profile should be loaded
   * @param currentUserId The unique identifier of the current logged-in user
   */
  fun loadPublicProfile(userId: String, currentUserId: String?) {
    viewModelScope.launch {
      _isLoading.value = true
      clearError()

      // Validate userId is not empty
      if (userId.isBlank()) {
        _isLoading.value = false
        _error.value = "Invalid user ID"
        return@launch
      }

      // Validate currentUserId
      if (currentUserId.isNullOrBlank()) {
        _error.value = "Not authenticated. Please sign in."
        _isLoading.value = false
        return@launch
      }

      // Don't allow viewing your own profile with this screen
      if (currentUserId == userId) {
        _error.value = "Cannot view your own profile here"
        _isLoading.value = false
        return@launch
      }

      // Fetch profile
      val fetchedProfile =
          try {
            profileRepository.getProfile(userId)
          } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile", e)
            null
          }

      if (fetchedProfile == null) {
        _error.value = "Profile not found"
        _isLoading.value = false
        return@launch
      }

      _profile.value = fetchedProfile

      // Check if current user follows this profile
      try {
        _isFollowing.value = profileRepository.isFollowing(currentUserId, userId)
      } catch (e: Exception) {
        Log.e(TAG, "Error checking follow status", e)
        _isFollowing.value = false
      }

      // Fetch common events
      try {
        val events = eventsRepository.getCommonEvents(listOf(currentUserId, userId))
        _commonEvents.value = events
      } catch (e: Exception) {
        Log.e(TAG, "Error loading common events", e)
        _commonEvents.value = emptyList()
      }

      // Fetch common groups
      try {
        val groups = groupRepository.getCommonGroups(listOf(currentUserId, userId))
        _commonGroups.value = groups
      } catch (e: Exception) {
        Log.e(TAG, "Error loading common groups", e)
        _commonGroups.value = emptyList()
      }

      _isLoading.value = false
    }
  }

  /** Clears the current error state. */
  fun clearError() {
    _error.value = null
  }

  /**
   * Toggles the follow status for the current profile.
   *
   * If currently following, unfollows the user. If not following, follows the user. Updates local
   * state immediately for responsive UI.
   *
   * @param currentUserId The current user's ID (the one performing the action)
   * @param profileUserId The profile user's ID (the one being followed/unfollowed)
   */
  fun toggleFollow(currentUserId: String, profileUserId: String) {
    viewModelScope.launch {
      _isFollowLoading.value = true

      try {
        if (_isFollowing.value) {
          // Unfollow
          profileRepository.unfollowUser(currentUserId, profileUserId)
          _isFollowing.value = false

          // Update local follower count
          _profile.value?.let { profile ->
            _profile.value = profile.copy(followersCount = profile.followersCount - 1)
          }
        } else {
          // Follow
          profileRepository.followUser(currentUserId, profileUserId)
          _isFollowing.value = true

          // Update local follower count
          _profile.value?.let { profile ->
            _profile.value = profile.copy(followersCount = profile.followersCount + 1)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error toggling follow", e)
        _error.value = "Failed to update follow status: ${e.message}"
      } finally {
        _isFollowLoading.value = false
      }
    }
  }
}
