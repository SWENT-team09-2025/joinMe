package com.android.joinme.ui.groups.leaderboard

// Implemented with help of Claude AI

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
 * Represents a single entry in the leaderboard.
 *
 * @property userId The unique identifier of the user.
 * @property displayName The user's display name.
 * @property streakWeeks The number of consecutive weeks (current or all-time).
 * @property streakActivities The number of activities (current or all-time).
 * @property rank The user's position in the leaderboard (1-indexed).
 */
data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
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
 * Responsible for fetching streak data for all group members, resolving their display names, and
 * exposing sorted leaderboards for both current and all-time streaks.
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
   * Fetches all streaks for the group, resolves user display names via batch fetch, and produces
   * two sorted leaderboards: current and all-time.
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

        // 2. Batch fetch user profiles for display names
        val userIds = streaks.map { it.userId }
        val displayNames = resolveDisplayNames(userIds)

        // 3. Build and sort leaderboards
        val currentLeaderboard = buildLeaderboard(streaks, displayNames, isCurrent = true)
        val allTimeLeaderboard = buildLeaderboard(streaks, displayNames, isCurrent = false)

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
   * Resolves user IDs to display names via batch fetch.
   *
   * @param userIds The list of user IDs to resolve.
   * @return A map of userId to displayName. Users not found will have "Unknown" as their name.
   */
  private suspend fun resolveDisplayNames(userIds: List<String>): Map<String, String> {
    return try {
      val profiles = profileRepository.getProfilesByIds(userIds)
      profiles?.associate { it.uid to it.username } ?: userIds.associateWith { "Unknown" }
    } catch (e: Exception) {
      Log.e("GroupLeaderboardViewModel", "Error fetching profiles", e)
      userIds.associateWith { "Unknown" }
    }
  }

  /**
   * Builds a sorted leaderboard from streaks.
   *
   * @param streaks The list of streaks to process.
   * @param displayNames A map of userId to displayName.
   * @param isCurrent If true, uses current streak values; if false, uses all-time best values.
   * @return A sorted list of LeaderboardEntry with ranks assigned.
   */
  private fun buildLeaderboard(
      streaks: List<GroupStreak>,
      displayNames: Map<String, String>,
      isCurrent: Boolean
  ): List<LeaderboardEntry> {
    // Sort by activities descending, then weeks descending as tiebreaker
    val sorted =
        streaks.sortedWith(
            compareByDescending<GroupStreak> {
                  if (isCurrent) it.currentStreakActivities else it.bestStreakActivities
                }
                .thenByDescending { if (isCurrent) it.currentStreakWeeks else it.bestStreakWeeks })

    // Assign ranks (1-indexed)
    return sorted.mapIndexed { index, streak ->
      LeaderboardEntry(
          userId = streak.userId,
          displayName = displayNames[streak.userId] ?: "Unknown",
          streakWeeks = if (isCurrent) streak.currentStreakWeeks else streak.bestStreakWeeks,
          streakActivities =
              if (isCurrent) streak.currentStreakActivities else streak.bestStreakActivities,
          rank = index + 1)
    }
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
