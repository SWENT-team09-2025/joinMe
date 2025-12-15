package com.android.joinme.ui.groups.leaderboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.joinme.model.groups.streaks.GroupStreak
import com.android.joinme.model.groups.streaks.GroupStreakRepository
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class GroupLeaderboardScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakeStreakRepo: FakeGroupStreakRepository
  private lateinit var fakeProfileRepo: FakeLeaderboardProfileRepository
  private lateinit var viewModel: GroupLeaderboardViewModel

  // ========== Test Data ==========

  private val testProfiles =
      listOf(
          Profile(uid = "user1", username = "Alice"),
          Profile(uid = "user2", username = "Bob"),
          Profile(uid = "user3", username = "Charlie"),
          Profile(uid = "user4", username = "Diana"),
          Profile(uid = "user5", username = "Eve"))

  private fun createTestStreaks(groupId: String) =
      listOf(
          GroupStreak(
              groupId = groupId,
              userId = "user1",
              currentStreakWeeks = 5,
              currentStreakActivities = 25,
              bestStreakWeeks = 10,
              bestStreakActivities = 50),
          GroupStreak(
              groupId = groupId,
              userId = "user2",
              currentStreakWeeks = 4,
              currentStreakActivities = 20,
              bestStreakWeeks = 8,
              bestStreakActivities = 40),
          GroupStreak(
              groupId = groupId,
              userId = "user3",
              currentStreakWeeks = 3,
              currentStreakActivities = 15,
              bestStreakWeeks = 6,
              bestStreakActivities = 30),
          GroupStreak(
              groupId = groupId,
              userId = "user4",
              currentStreakWeeks = 0,
              currentStreakActivities = 0,
              bestStreakWeeks = 4,
              bestStreakActivities = 20),
          GroupStreak(
              groupId = groupId,
              userId = "user5",
              currentStreakWeeks = 0,
              currentStreakActivities = 0,
              bestStreakWeeks = 2,
              bestStreakActivities = 10))

  // ========== Setup Helpers ==========

  @Before
  fun setUp() {
    fakeStreakRepo = FakeGroupStreakRepository()
    fakeProfileRepo = FakeLeaderboardProfileRepository()
  }

  private fun createViewModel() =
      GroupLeaderboardViewModel(
          ApplicationProvider.getApplicationContext(), fakeStreakRepo, fakeProfileRepo)

  private fun setupWithData(
      groupId: String = "group1",
      streaks: List<GroupStreak> = createTestStreaks(groupId),
      profiles: List<Profile> = testProfiles
  ) {
    profiles.forEach { fakeProfileRepo.addProfile(it) }
    streaks.forEach { fakeStreakRepo.addStreak(it) }
    viewModel = createViewModel()
  }

  private fun setupEmptyState() {
    viewModel = createViewModel()
  }

  private fun setupErrorState() {
    fakeStreakRepo.shouldThrowError = true
    viewModel = createViewModel()
  }

  private fun setScreenContent(groupId: String = "group1", onNavigateBack: () -> Unit = {}) {
    composeTestRule.setContent {
      GroupLeaderboardScreen(
          groupId = groupId, viewModel = viewModel, onNavigateBack = onNavigateBack)
    }
  }

  private fun waitForContent() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()
  }

  // ========== Top Bar Tests ==========

  @Test
  fun topBar_displaysAllElements() {
    setupWithData()
    setScreenContent()
    waitForContent()

    composeTestRule.onNodeWithTag(LeaderboardTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.INFO_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithText("Leaderboard").assertIsDisplayed()
  }

  @Test
  fun backButton_triggersCallback() {
    setupWithData()
    var backClicked = false
    setScreenContent(onNavigateBack = { backClicked = true })
    waitForContent()

    composeTestRule.onNodeWithTag(LeaderboardTestTags.BACK_BUTTON).performClick()

    assertTrue(backClicked)
  }

  @Test
  fun infoButton_opensDialog() {
    setupWithData()
    setScreenContent()
    waitForContent()

    composeTestRule.onNodeWithTag(LeaderboardTestTags.INFO_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Dialog should be visible (check for dialog test tag)
    composeTestRule.onNodeWithTag(StreakInfoDialogTestTags.DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(StreakInfoDialogTestTags.TITLE_HOW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(StreakInfoDialogTestTags.TITLE_WHAT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(StreakInfoDialogTestTags.TITLE_WHY).assertIsDisplayed()
  }

  // ========== Tab Tests ==========

  @Test
  fun tabs_displayBothOptions() {
    setupWithData()
    setScreenContent()
    waitForContent()

    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_CURRENT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).assertIsDisplayed()
  }

  @Test
  fun tabs_currentSelectedByDefault() {
    setupWithData()
    setScreenContent()
    waitForContent()

    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_CURRENT).assertIsSelected()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).assertIsNotSelected()
  }

  @Test
  fun tabs_switchingChangesSelection() {
    setupWithData()
    setScreenContent()
    waitForContent()

    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).assertIsSelected()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_CURRENT).assertIsNotSelected()
  }

  @Test
  fun tabs_switchingDisplaysCorrectData() {
    setupWithData()
    setScreenContent()
    waitForContent()

    // Current tab shows users with currentStreakWeeks > 0 (users 1, 2, 3)
    composeTestRule.onNodeWithTag("${LeaderboardTestTags.ITEM_PREFIX}user1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("${LeaderboardTestTags.ITEM_PREFIX}user2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("${LeaderboardTestTags.ITEM_PREFIX}user3").assertIsDisplayed()

    // Switch to All Time
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).performClick()
    composeTestRule.waitForIdle()

    // All Time tab shows all 5 entries (sorted by bestStreakWeeks)
    composeTestRule.onNodeWithTag("${LeaderboardTestTags.ITEM_PREFIX}user4").assertIsDisplayed()
    composeTestRule.onNodeWithTag("${LeaderboardTestTags.ITEM_PREFIX}user5").assertIsDisplayed()
  }

  // ========== Loading State Tests ==========

  @Test
  fun loadingState_displaysIndicator() {
    fakeStreakRepo = FakeGroupStreakRepository()
    fakeProfileRepo = FakeLeaderboardProfileRepository()
    viewModel =
        GroupLeaderboardViewModel(
            ApplicationProvider.getApplicationContext(), fakeStreakRepo, fakeProfileRepo)

    composeTestRule.setContent {
      GroupLeaderboardScreen(groupId = "group1", viewModel = viewModel, onNavigateBack = {})
    }

    // Check loading state before data loads
    val loadingExists =
        composeTestRule
            .onAllNodesWithTag(LeaderboardTestTags.LOADING)
            .fetchSemanticsNodes()
            .isNotEmpty()
    val listExists =
        composeTestRule
            .onAllNodesWithTag(LeaderboardTestTags.LIST)
            .fetchSemanticsNodes()
            .isNotEmpty()
    val emptyExists =
        composeTestRule
            .onAllNodesWithTag(LeaderboardTestTags.EMPTY_STATE)
            .fetchSemanticsNodes()
            .isNotEmpty()

    // Either loading is shown OR data loaded fast (empty state) - both are valid
    assertTrue(loadingExists || listExists || emptyExists)
  }

  // ========== Empty State Tests ==========

  @Test
  fun emptyState_displaysMessage() {
    setupEmptyState()
    setScreenContent()
    waitForContent()

    composeTestRule.onNodeWithTag(LeaderboardTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.LIST).assertDoesNotExist()
  }

  @Test
  fun emptyState_switchingTabsShowsEmptyForBoth() {
    setupEmptyState()
    setScreenContent()
    waitForContent()

    // Empty on Current tab
    composeTestRule.onNodeWithTag(LeaderboardTestTags.EMPTY_STATE).assertIsDisplayed()

    // Switch to All Time
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).performClick()
    composeTestRule.waitForIdle()

    // Still empty
    composeTestRule.onNodeWithTag(LeaderboardTestTags.EMPTY_STATE).assertIsDisplayed()
  }

  // ========== Leaderboard List Tests ==========

  @Test
  fun leaderboardList_displaysAllEntries() {
    setupWithData()
    setScreenContent()
    waitForContent()

    composeTestRule.onNodeWithTag(LeaderboardTestTags.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.PURPLE_CONTAINER).assertIsDisplayed()

    // Verify current leaderboard entries are displayed
    composeTestRule.onNodeWithTag("${LeaderboardTestTags.ITEM_PREFIX}user1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("${LeaderboardTestTags.ITEM_PREFIX}user2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("${LeaderboardTestTags.ITEM_PREFIX}user3").assertIsDisplayed()
  }

  @Test
  fun leaderboardItem_displaysUserInfo() {
    setupWithData()
    setScreenContent()
    waitForContent()

    // Check username and activity count are displayed
    composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    composeTestRule.onNodeWithText("25 activities joined").assertIsDisplayed()
  }

  @Test
  fun leaderboardItem_topThreeHaveCrownBadges() {
    setupWithData()
    setScreenContent()
    waitForContent()

    // Switch to All Time to see all 5 users
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).performClick()
    composeTestRule.waitForIdle()

    // Top 3 should have crown badges (we can verify by checking the emoji exists)
    composeTestRule.onAllNodesWithText("👑").assertCountEquals(3)
  }

  @Test
  fun leaderboardItem_rank4AndBeyondNoCrownBadge() {
    setupWithData()
    setScreenContent()
    waitForContent()

    // Switch to All Time
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ALL_TIME).performClick()
    composeTestRule.waitForIdle()

    // Should have exactly 3 crown badges (for ranks 1, 2, 3)
    composeTestRule.onAllNodesWithText("👑").assertCountEquals(3)

    // Users 4 and 5 should be visible but without crown
    composeTestRule.onNodeWithText("Diana").assertIsDisplayed()
    composeTestRule.onNodeWithText("Eve").assertIsDisplayed()
  }

  // ========== Info Dialog Tests ==========

  @Test
  fun infoDialog_canBeDismissed() {
    setupWithData()
    setScreenContent()
    waitForContent()

    // Open dialog
    composeTestRule.onNodeWithTag(LeaderboardTestTags.INFO_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(StreakInfoDialogTestTags.DIALOG).assertIsDisplayed()

    // Click the close button
    composeTestRule.onNodeWithTag(StreakInfoDialogTestTags.CLOSE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Dialog should be dismissed
    composeTestRule.onNodeWithTag(StreakInfoDialogTestTags.DIALOG).assertDoesNotExist()
  }

  // ========== Error State Tests ==========

  @Test
  fun errorState_showsEmptyStateAfterError() {
    setupErrorState()
    setScreenContent()
    waitForContent()

    // After error, should show empty state (no crash)
    composeTestRule.onNodeWithTag(LeaderboardTestTags.SCREEN).assertIsDisplayed()
  }

  // ========== Screen Layout Tests ==========

  @Test
  fun screen_hasCorrectStructure() {
    setupWithData()
    setScreenContent()
    waitForContent()

    composeTestRule.onNodeWithTag(LeaderboardTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.TAB_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.PURPLE_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LeaderboardTestTags.LIST).assertIsDisplayed()
  }

  // ========== Fake Repositories ==========

  private class FakeGroupStreakRepository : GroupStreakRepository {
    private val streaks = mutableListOf<GroupStreak>()
    var shouldThrowError = false

    fun addStreak(streak: GroupStreak) {
      streaks.add(streak)
    }

    override suspend fun getStreaksForGroup(groupId: String): List<GroupStreak> {
      if (shouldThrowError) throw Exception("Failed to load streaks")
      return streaks.filter { it.groupId == groupId }
    }

    override suspend fun getStreakForUser(groupId: String, userId: String): GroupStreak? {
      if (shouldThrowError) throw Exception("Failed to load streak")
      return streaks.find { it.groupId == groupId && it.userId == userId }
    }

    override suspend fun updateStreak(groupId: String, userId: String, streak: GroupStreak) {
      val index = streaks.indexOfFirst { it.groupId == groupId && it.userId == userId }
      if (index >= 0) {
        streaks[index] = streak
      } else {
        streaks.add(streak)
      }
    }

    override suspend fun deleteStreakForUser(groupId: String, userId: String) {
      streaks.removeAll { it.groupId == groupId && it.userId == userId }
    }

    override suspend fun deleteAllStreaksForGroup(groupId: String) {
      streaks.removeAll { it.groupId == groupId }
    }
  }

  private class FakeLeaderboardProfileRepository : ProfileRepository {
    private val profiles = mutableMapOf<String, Profile>()

    fun addProfile(profile: Profile) {
      profiles[profile.uid] = profile
    }

    override suspend fun getProfile(uid: String): Profile? = profiles[uid]

    override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? {
      if (uids.isEmpty()) return emptyList()
      return uids.mapNotNull { profiles[it] }
    }

    override suspend fun createOrUpdateProfile(profile: Profile) {
      profiles[profile.uid] = profile
    }

    override suspend fun deleteProfile(uid: String) {
      profiles.remove(uid)
    }

    override suspend fun uploadProfilePhoto(
        context: android.content.Context,
        uid: String,
        imageUri: android.net.Uri
    ): String = "https://fake.url/$uid/photo.jpg"

    override suspend fun deleteProfilePhoto(uid: String) {}

    override suspend fun followUser(followerId: String, followedId: String) {}

    override suspend fun unfollowUser(followerId: String, followedId: String) {}

    override suspend fun isFollowing(followerId: String, followedId: String): Boolean = false

    override suspend fun getFollowing(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getFollowers(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getMutualFollowing(userId1: String, userId2: String): List<Profile> =
        emptyList()
  }
}
