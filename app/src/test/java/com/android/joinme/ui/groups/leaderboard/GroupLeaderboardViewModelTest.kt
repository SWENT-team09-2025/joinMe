package com.android.joinme.ui.groups.leaderboard

// Implemented with help of Claude AI

import com.android.joinme.model.groups.streaks.GroupStreak
import com.android.joinme.model.groups.streaks.GroupStreakRepository
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GroupLeaderboardViewModelTest {

  private lateinit var streakRepository: GroupStreakRepository
  private lateinit var profileRepository: ProfileRepository
  private lateinit var viewModel: GroupLeaderboardViewModel
  private val testDispatcher = StandardTestDispatcher()

  private val testGroupId = "group123"

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    streakRepository = mock()
    profileRepository = mock()
    viewModel = GroupLeaderboardViewModel(streakRepository, profileRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createStreak(
      userId: String,
      currentWeeks: Int = 1,
      currentActivities: Int = 1,
      bestWeeks: Int = 1,
      bestActivities: Int = 1
  ) =
      GroupStreak(
          groupId = testGroupId,
          userId = userId,
          currentStreakWeeks = currentWeeks,
          currentStreakActivities = currentActivities,
          currentStreakStartDate = Timestamp.now(),
          lastActiveWeekStart = Timestamp.now(),
          bestStreakWeeks = bestWeeks,
          bestStreakActivities = bestActivities)

  private fun createProfile(uid: String, username: String) =
      Profile(uid = uid, username = username, email = "$uid@example.com")

  /** Tests loading with multiple users, verifying sorting and rank assignment */
  @Test
  fun loadLeaderboard_multipleUsers_sortsByActivitiesThenWeeks() = runTest {
    // User1: 5 activities, 3 weeks (current) | 10 activities, 5 weeks (best)
    // User2: 5 activities, 2 weeks (current) | 8 activities, 6 weeks (best)
    // User3: 3 activities, 3 weeks (current) | 10 activities, 4 weeks (best)
    val streaks =
        listOf(
            createStreak(
                "user1",
                currentWeeks = 3,
                currentActivities = 5,
                bestWeeks = 5,
                bestActivities = 10),
            createStreak(
                "user2",
                currentWeeks = 2,
                currentActivities = 5,
                bestWeeks = 6,
                bestActivities = 8),
            createStreak(
                "user3",
                currentWeeks = 3,
                currentActivities = 3,
                bestWeeks = 4,
                bestActivities = 10))
    val profiles =
        listOf(
            createProfile("user1", "Alice"),
            createProfile("user2", "Bob"),
            createProfile("user3", "Charlie"))

    whenever(streakRepository.getStreaksForGroup(testGroupId)).thenReturn(streaks)
    whenever(profileRepository.getProfilesByIds(listOf("user1", "user2", "user3")))
        .thenReturn(profiles)

    viewModel.loadLeaderboard(testGroupId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()

    // Current leaderboard: sorted by currentActivities desc, then currentWeeks desc
    // user1 (5 act, 3 wk) > user2 (5 act, 2 wk) > user3 (3 act, 3 wk)
    assertEquals(3, state.currentLeaderboard.size)
    assertEquals("Alice", state.currentLeaderboard[0].displayName)
    assertEquals(1, state.currentLeaderboard[0].rank)
    assertEquals(5, state.currentLeaderboard[0].streakActivities)
    assertEquals("Bob", state.currentLeaderboard[1].displayName)
    assertEquals(2, state.currentLeaderboard[1].rank)
    assertEquals("Charlie", state.currentLeaderboard[2].displayName)
    assertEquals(3, state.currentLeaderboard[2].rank)

    // All-time leaderboard: sorted by bestActivities desc, then bestWeeks desc
    // user1 (10 act, 5 wk) > user3 (10 act, 4 wk) > user2 (8 act, 6 wk)
    assertEquals(3, state.allTimeLeaderboard.size)
    assertEquals("Alice", state.allTimeLeaderboard[0].displayName)
    assertEquals(1, state.allTimeLeaderboard[0].rank)
    assertEquals(10, state.allTimeLeaderboard[0].streakActivities)
    assertEquals("Charlie", state.allTimeLeaderboard[1].displayName)
    assertEquals(2, state.allTimeLeaderboard[1].rank)
    assertEquals("Bob", state.allTimeLeaderboard[2].displayName)
    assertEquals(3, state.allTimeLeaderboard[2].rank)

    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
  }

  /** Tests empty streaks scenario */
  @Test
  fun loadLeaderboard_noStreaks_returnsEmptyLeaderboards() = runTest {
    whenever(streakRepository.getStreaksForGroup(testGroupId)).thenReturn(emptyList())

    viewModel.loadLeaderboard(testGroupId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.currentLeaderboard.isEmpty())
    assertTrue(state.allTimeLeaderboard.isEmpty())
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
  }

  /** Tests graceful handling when profile fetch fails */
  @Test
  fun loadLeaderboard_profileFetchFails_usesUnknownDisplayNames() = runTest {
    val streaks = listOf(createStreak("user1", currentActivities = 5, bestActivities = 10))

    whenever(streakRepository.getStreaksForGroup(testGroupId)).thenReturn(streaks)
    whenever(profileRepository.getProfilesByIds(listOf("user1"))).thenReturn(null)

    viewModel.loadLeaderboard(testGroupId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals(1, state.currentLeaderboard.size)
    assertEquals("Unknown", state.currentLeaderboard[0].displayName)
    assertFalse(state.isLoading)
  }

  /** Tests error handling when streak fetch fails */
  @Test
  fun loadLeaderboard_streakFetchFails_setsErrorMsg() = runTest {
    whenever(streakRepository.getStreaksForGroup(testGroupId))
        .thenThrow(RuntimeException("Network error"))

    viewModel.loadLeaderboard(testGroupId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Network error"))
    assertFalse(state.isLoading)
  }

  /** Tests clearErrorMsg functionality */
  @Test
  fun clearErrorMsg_clearsError() = runTest {
    whenever(streakRepository.getStreaksForGroup(testGroupId)).thenThrow(RuntimeException("Error"))

    viewModel.loadLeaderboard(testGroupId)
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.first().errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.first().errorMsg)
  }

  /** Tests initial loading state */
  @Test
  fun initialState_isLoading() {
    val state = viewModel.uiState.value
    assertTrue(state.isLoading)
    assertTrue(state.currentLeaderboard.isEmpty())
    assertTrue(state.allTimeLeaderboard.isEmpty())
    assertNull(state.errorMsg)
  }
}
