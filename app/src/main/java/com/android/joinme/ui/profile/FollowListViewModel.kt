package com.android.joinme.ui.profile

/** This file was implemented with the help of AI */
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Enum representing the available tabs in the follow list screen.
 *
 * @property FOLLOWERS Tab showing users who follow the profile
 * @property FOLLOWING Tab showing users that the profile follows
 */
enum class FollowTab {
  FOLLOWERS,
  FOLLOWING
}

/**
 * ViewModel for managing the follower/following list screen state.
 *
 * This ViewModel handles displaying a user's followers and following lists with tab switching. It
 * uses lazy loading - only fetching data for a tab when it's first selected.
 *
 * State management:
 * - [selectedTab]: Currently active tab (FOLLOWERS or FOLLOWING)
 * - [followers]: List of users following the profile
 * - [following]: List of users the profile follows
 * - [isLoading]: Indicates whether data is being loaded
 * - [error]: Contains error messages from failed operations, or null if no error
 *
 * @param profileRepository The [ProfileRepository] for fetching follower/following data
 */
class FollowListViewModel(
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {
  companion object {
    /** Tag for logging purposes. */
    private const val TAG = "FollowListViewModel"

    /** Number of items to load per page. */
    private const val PAGE_SIZE = 25
  }

  private val _selectedTab = MutableStateFlow(FollowTab.FOLLOWERS)
  val selectedTab: StateFlow<FollowTab> = _selectedTab.asStateFlow()

  private val _profileUsername = MutableStateFlow("")
  val profileUsername: StateFlow<String> = _profileUsername.asStateFlow()

  private val _followers = MutableStateFlow<List<Profile>>(emptyList())
  val followers: StateFlow<List<Profile>> = _followers.asStateFlow()

  private val _following = MutableStateFlow<List<Profile>>(emptyList())
  val following: StateFlow<List<Profile>> = _following.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _isLoadingMore = MutableStateFlow(false)
  val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

  private val _hasMoreFollowers = MutableStateFlow(false)
  val hasMoreFollowers: StateFlow<Boolean> = _hasMoreFollowers.asStateFlow()

  private val _hasMoreFollowing = MutableStateFlow(false)
  val hasMoreFollowing: StateFlow<Boolean> = _hasMoreFollowing.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  /** Tracks which tabs have already been loaded to avoid redundant fetches. */
  private val loadedTabs = mutableSetOf<FollowTab>()

  /** The user ID whose followers/following are being displayed. */
  private var currentUserId: String? = null

  /** Current page number for followers (0-indexed). */
  private var followersPage = 0

  /** Current page number for following (0-indexed). */
  private var followingPage = 0

  /**
   * Initializes the screen with a specific user's data and tab.
   *
   * This method should be called when the screen is first opened. It sets up the initial tab and
   * loads the corresponding data. If called multiple times (e.g., with different users), it resets
   * the loaded tabs cache to ensure fresh data is fetched.
   *
   * Exception handling: Any exceptions during profile or follower/following data fetching are
   * caught internally and exposed through the [error] StateFlow. The loading will complete
   * gracefully with the error state set.
   *
   * @param userId The unique identifier of the user whose followers/following should be displayed
   * @param initialTab The tab to show initially (defaults to FOLLOWERS)
   */
  fun initialize(userId: String, initialTab: FollowTab = FollowTab.FOLLOWERS) {
    // Reset state when initializing with a new user
    if (currentUserId != userId) {
      loadedTabs.clear()
      _followers.value = emptyList()
      _following.value = emptyList()
      followersPage = 0
      followingPage = 0
      _hasMoreFollowers.value = false
      _hasMoreFollowing.value = false
    }

    currentUserId = userId
    _selectedTab.value = initialTab
    loadCurrentTab()
    loadProfileUsername(userId)
  }

  /** Loads the username of the profile being viewed. */
  private fun loadProfileUsername(userId: String) {
    viewModelScope.launch {
      try {
        val profile = profileRepository.getProfile(userId)
        _profileUsername.value = profile?.username ?: ""
      } catch (e: Exception) {
        Log.e(TAG, "Error loading profile username", e)
        _error.value = "Failed to load Profile username: ${e.message}"
        _profileUsername.value = ""
      }
    }
  }

  /**
   * Switches to a different tab and loads its data if not already loaded.
   *
   * Uses lazy loading - data is only fetched the first time a tab is selected.
   *
   * Exception handling: Any exceptions during data fetching are caught internally and exposed
   * through the [error] StateFlow. The tab switch will complete regardless of loading errors.
   *
   * @param tab The tab to switch to
   */
  fun selectTab(tab: FollowTab) {
    _selectedTab.value = tab
    loadCurrentTab()
  }

  /** Loads data for the currently selected tab if it hasn't been loaded yet. */
  private fun loadCurrentTab() {
    val tab = _selectedTab.value
    val userId = currentUserId

    if (userId == null) {
      _error.value = "User ID not set"
      return
    }

    // Skip if already loaded (lazy loading)
    if (loadedTabs.contains(tab)) {
      return
    }

    viewModelScope.launch {
      _isLoading.value = true
      clearError()

      try {
        when (tab) {
          FollowTab.FOLLOWERS -> loadFollowers(userId)
          FollowTab.FOLLOWING -> loadFollowing(userId)
        }
        loadedTabs.add(tab)
      } catch (e: Exception) {
        Log.e(TAG, "Error loading ${tab.name.lowercase()}", e)
        _error.value = "Failed to load ${tab.name.lowercase()}: ${e.message}"
      } finally {
        _isLoading.value = false
      }
    }
  }

  /**
   * Fetches the list of users following the specified user.
   *
   * @param userId The user whose followers should be fetched
   */
  private suspend fun loadFollowers(userId: String) {
    val limit = (followersPage + 1) * PAGE_SIZE
    val result = profileRepository.getFollowers(userId, limit)
    _followers.value = result
    _hasMoreFollowers.value = result.size >= limit
  }

  /**
   * Fetches the list of users that the specified user follows.
   *
   * @param userId The user whose following list should be fetched
   */
  private suspend fun loadFollowing(userId: String) {
    val limit = (followingPage + 1) * PAGE_SIZE
    val result = profileRepository.getFollowing(userId, limit)
    _following.value = result
    _hasMoreFollowing.value = result.size >= limit
  }

  /**
   * Forces a refresh of the current tab's data.
   *
   * This can be used for pull-to-refresh functionality or when data may have changed. Resets
   * pagination state and fetches the first page again.
   *
   * Exception handling: Any exceptions during refresh are caught internally and exposed through the
   * [error] StateFlow. Previous data will remain visible if the refresh fails.
   */
  fun refresh() {
    val tab = _selectedTab.value
    // Reset pagination for the current tab
    when (tab) {
      FollowTab.FOLLOWERS -> {
        followersPage = 0
        _hasMoreFollowers.value = false
      }
      FollowTab.FOLLOWING -> {
        followingPage = 0
        _hasMoreFollowing.value = false
      }
    }
    loadedTabs.remove(tab)
    loadCurrentTab()
  }

  /**
   * Loads the next page of data for the current tab.
   *
   * This should be called when the user scrolls near the end of the list to load more items. Uses
   * the [isLoadingMore] state to indicate loading status.
   *
   * Exception handling: Any exceptions during load are caught internally and exposed through the
   * [error] StateFlow. The pagination state remains unchanged if loading fails.
   */
  fun loadMore() {
    val tab = _selectedTab.value
    val userId = currentUserId
    val hasMore =
        when (tab) {
          FollowTab.FOLLOWERS -> _hasMoreFollowers.value
          FollowTab.FOLLOWING -> _hasMoreFollowing.value
        }

    // Don't load if already loading, no user ID, or no more data
    if (_isLoadingMore.value || userId == null || !hasMore) {
      return
    }

    viewModelScope.launch {
      _isLoadingMore.value = true

      try {
        when (tab) {
          FollowTab.FOLLOWERS -> {
            followersPage++
            loadFollowers(userId)
          }
          FollowTab.FOLLOWING -> {
            followingPage++
            loadFollowing(userId)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error loading more ${tab.name.lowercase()}", e)
        _error.value = "Failed to load more ${tab.name.lowercase()}: ${e.message}"
        // Revert page increment on error
        when (tab) {
          FollowTab.FOLLOWERS -> followersPage--
          FollowTab.FOLLOWING -> followingPage--
        }
      } finally {
        _isLoadingMore.value = false
      }
    }
  }

  /** Clears the current error state. */
  fun clearError() {
    _error.value = null
  }
}
