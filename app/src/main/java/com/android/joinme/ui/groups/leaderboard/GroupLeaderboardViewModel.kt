// Implemented with help of Claude AI
package com.android.joinme.ui.groups.leaderboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.model.groups.streaks.GroupStreak
import com.android.joinme.model.groups.streaks.GroupStreakRepository
import com.android.joinme.model.groups.streaks.GroupStreakRepositoryProvider
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds resolved user profile information for leaderboard display.
 *
 * @property displayName The user's display name.
 * @property photoUrl The user's profile photo URL, or null if not available.
 */
private data class UserProfileInfo(val displayName: String, val photoUrl: String?)

/**
 * Represents a single entry in the leaderboard.
 *
 * @property userId The unique identifier of the user.
 * @property displayName The user's display name.
 * @property photoUrl The user's profile photo URL, or null if not available.
 * @property streakWeeks The number of consecutive weeks (current or all-time).
 * @property streakActivities The number of activities (current or all-time).
 * @property rank The user's position in the leaderboard (1-indexed).
 */
data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val streakWeeks: Int,
    val streakActivities: Int,
    val rank: Int
)

/**
 * Represents the UI state for the Group Leaderboard screen.
 *
 * @property currentLeaderboard The leaderboard sorted by current streak (activities desc, weeks as
 *   tiebreaker).
 * @property allTimeLeaderboard The leaderboard sorted by best streak (activities desc, weeks as
 *   tiebreaker).
 * @property isLoading Indicates whether the screen is currently loading data.
 * @property errorMsg An error message to be shown when fetching data fails.
 */
data class GroupLeaderboardUIState(
    val currentLeaderboard: List<LeaderboardEntry> = emptyList(),
    val allTimeLeaderboard: List<LeaderboardEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMsg: String? = null
)

/**
 * ViewModel for the Group Leaderboard screen.
 *
 * Responsible for fetching streak data for all group members, resolving their display names and
 * photo URLs, and exposing sorted leaderboards for both current and all-time streaks.
 *
 * Sorting criteria: activities count descending, then weeks as tiebreaker (descending).
 *
 * @property streakRepository The repository used to fetch streak data.
 * @property profileRepository The repository used to fetch user profiles.
 */
class GroupLeaderboardViewModel(
    private val streakRepository: GroupStreakRepository =
        GroupStreakRepositoryProvider.getRepository(),
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(GroupLeaderboardUIState())
  val uiState: StateFlow<GroupLeaderboardUIState> = _uiState.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /**
   * Loads the leaderboard data for a specific group.
   *
   * Fetches all streaks for the group, resolves user profile info via batch fetch, and produces two
   * sorted leaderboards: current and all-time.
   *
   * @param groupId The unique identifier of the group.
   */
  fun loadLeaderboard(groupId: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)

      try {
        // 1. Fetch all streaks for the group
        val streaks = streakRepository.getStreaksForGroup(groupId)

        if (streaks.isEmpty()) {
          _uiState.value = GroupLeaderboardUIState(isLoading = false)
          return@launch
        }

        // 2. Batch fetch user profiles for display names and photo URLs
        val userIds = streaks.map { it.userId }
        val userProfiles = resolveUserProfiles(userIds)

        // 3. Build and sort leaderboards
        val currentLeaderboard = buildLeaderboard(streaks, userProfiles, isCurrent = true)
        val allTimeLeaderboard = buildLeaderboard(streaks, userProfiles, isCurrent = false)

        _uiState.value =
            GroupLeaderboardUIState(
                currentLeaderboard = currentLeaderboard,
                allTimeLeaderboard = allTimeLeaderboard,
                isLoading = false)
      } catch (e: Exception) {
        Log.e("GroupLeaderboardViewModel", "Error loading leaderboard", e)
        setErrorMsg("Failed to load leaderboard: ${e.message}")
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  /**
   * Resolves user IDs to profile information (display name and photo URL) via batch fetch.
   *
   * @param userIds The list of user IDs to resolve.
   * @return A map of userId to UserProfileInfo. Users not found will have "Unknown" as their name
   *   and null as their photo URL.
   */
  private suspend fun resolveUserProfiles(userIds: List<String>): Map<String, UserProfileInfo> {
    return try {
      val profiles = profileRepository.getProfilesByIds(userIds)
      profiles?.associate {
        it.uid to UserProfileInfo(displayName = it.username, photoUrl = it.photoUrl)
      } ?: userIds.associateWith { UserProfileInfo(displayName = "Unknown", photoUrl = null) }
    } catch (e: Exception) {
      Log.e("GroupLeaderboardViewModel", "Error fetching profiles", e)
      userIds.associateWith { UserProfileInfo(displayName = "Unknown", photoUrl = null) }
    }
  }

  /**
   * Builds a sorted leaderboard from streaks.
   *
   * @param streaks The list of streaks to process.
   * @param userProfiles A map of userId to UserProfileInfo.
   * @param isCurrent If true, uses current streak values; if false, uses all-time best values.
   * @return A sorted list of LeaderboardEntry with ranks assigned (ties share the same rank).
   */
  private fun buildLeaderboard(
      streaks: List<GroupStreak>,
      userProfiles: Map<String, UserProfileInfo>,
      isCurrent: Boolean
  ): List<LeaderboardEntry> {
    val sorted = sortStreaks(streaks, isCurrent)
    return assignRanks(sorted, userProfiles, isCurrent)
  }

  /** Sorts streaks by activities descending, then weeks descending as tiebreaker. */
  private fun sortStreaks(streaks: List<GroupStreak>, isCurrent: Boolean): List<GroupStreak> {
    return streaks.sortedWith(
        compareByDescending<GroupStreak> { getActivities(it, isCurrent) }
            .thenByDescending { getWeeks(it, isCurrent) })
  }

  /** Assigns ranks with tie handling (standard competition ranking). */
  private fun assignRanks(
      sorted: List<GroupStreak>,
      userProfiles: Map<String, UserProfileInfo>,
      isCurrent: Boolean
  ): List<LeaderboardEntry> {
    val result = mutableListOf<LeaderboardEntry>()
    var currentRank = 1

    sorted.forEachIndexed { index, streak ->
      if (index > 0 && !isTied(sorted[index - 1], streak, isCurrent)) {
        currentRank = index + 1
      }

      result.add(createEntry(streak, userProfiles, isCurrent, currentRank))
    }

    return result
  }

  /** Checks if two streaks have identical stats (tied). */
  private fun isTied(prev: GroupStreak, current: GroupStreak, isCurrent: Boolean): Boolean {
    return getActivities(prev, isCurrent) == getActivities(current, isCurrent) &&
        getWeeks(prev, isCurrent) == getWeeks(current, isCurrent)
  }

  /** Creates a LeaderboardEntry from a streak. */
  private fun createEntry(
      streak: GroupStreak,
      userProfiles: Map<String, UserProfileInfo>,
      isCurrent: Boolean,
      rank: Int
  ): LeaderboardEntry {
    val profileInfo =
        userProfiles[streak.userId] ?: UserProfileInfo(displayName = "Unknown", photoUrl = null)

    return LeaderboardEntry(
        userId = streak.userId,
        displayName = profileInfo.displayName,
        photoUrl = profileInfo.photoUrl,
        streakWeeks = getWeeks(streak, isCurrent),
        streakActivities = getActivities(streak, isCurrent),
        rank = rank)
  }

  /** Gets activities count based on leaderboard type. */
  private fun getActivities(streak: GroupStreak, isCurrent: Boolean): Int {
    return if (isCurrent) streak.currentStreakActivities else streak.bestStreakActivities
  }

  /** Gets weeks count based on leaderboard type. */
  private fun getWeeks(streak: GroupStreak, isCurrent: Boolean): Int {
    return if (isCurrent) streak.currentStreakWeeks else streak.bestStreakWeeks
  }

  /**
   * Refreshes the leaderboard data for a specific group.
   *
   * @param groupId The unique identifier of the group.
   */
  fun refresh(groupId: String) {
    loadLeaderboard(groupId)
  }
}
