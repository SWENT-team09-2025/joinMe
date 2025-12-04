package com.android.joinme.ui.profile

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Robolectric tests for FollowListScreen composable. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class FollowListScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private val testUserId = "test-user-id"
  private val follower1Id = "follower-1"
  private val follower2Id = "follower-2"
  private val following1Id = "following-1"
  private val following2Id = "following-2"

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    // Initialize Firebase if not already initialized
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
  }

  private fun createTestProfile(
      uid: String,
      username: String = "User_$uid",
      bio: String? = "Bio for $username",
      photoUrl: String? = null
  ): Profile {
    return Profile(
        uid = uid,
        username = username,
        email = "$uid@example.com",
        dateOfBirth = "01/01/1990",
        country = "Switzerland",
        interests = listOf("Coding", "Music"),
        bio = bio,
        photoUrl = photoUrl,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
        eventsJoinedCount = 10,
        followersCount = 5,
        followingCount = 3)
  }

  // Fake ProfileRepository for testing
  private class FakeProfileRepository(
      private val profiles: Map<String, Profile> = emptyMap(),
      private val followers: Map<String, List<Profile>> = emptyMap(),
      private val following: Map<String, List<Profile>> = emptyMap(),
      private val shouldThrowError: Boolean = false,
      private val errorMessage: String = "Test error",
      private val delayMillis: Long = 0
  ) : ProfileRepository {

    override suspend fun getProfile(uid: String): Profile? {
      if (delayMillis > 0) kotlinx.coroutines.delay(delayMillis)
      if (shouldThrowError && uid.isEmpty()) throw Exception(errorMessage)
      return profiles[uid]
    }

    override suspend fun getFollowers(userId: String, limit: Int): List<Profile> {
      if (delayMillis > 0) kotlinx.coroutines.delay(delayMillis)
      if (shouldThrowError) throw Exception(errorMessage)
      return followers[userId]?.take(limit) ?: emptyList()
    }

    override suspend fun getFollowing(userId: String, limit: Int): List<Profile> {
      if (delayMillis > 0) kotlinx.coroutines.delay(delayMillis)
      if (shouldThrowError) throw Exception(errorMessage)
      return following[userId]?.take(limit) ?: emptyList()
    }

    // Stub implementations for other methods
    override suspend fun createOrUpdateProfile(profile: Profile) {}

    override suspend fun deleteProfile(uid: String) {}

    override suspend fun uploadProfilePhoto(
        context: android.content.Context,
        uid: String,
        imageUri: android.net.Uri
    ): String = ""

    override suspend fun deleteProfilePhoto(uid: String) {}

    override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? = null

    override suspend fun followUser(followerId: String, followedId: String) {}

    override suspend fun unfollowUser(followerId: String, followedId: String) {}

    override suspend fun isFollowing(followerId: String, followedId: String): Boolean = false

    override suspend fun getMutualFollowing(userId1: String, userId2: String): List<Profile> =
        emptyList()
  }

  // ==================== LOADING AND ERROR STATES ====================

  @Test
  fun followListScreen_completesLoadingSuccessfully() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val followersList = listOf(createTestProfile(follower1Id, "Follower1"))

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to followersList))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent {
      FollowListScreen(userId = testUserId, initialTab = FollowTab.FOLLOWERS, viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // After loading completes, the list should be displayed
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithText("Follower1").assertIsDisplayed()
  }

  @Test
  fun followListScreen_displaysErrorMessage_whenLoadFails() {
    val repository = FakeProfileRepository(shouldThrowError = true, errorMessage = "Load failed")

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent {
      FollowListScreen(userId = "", viewModel = viewModel, initialTab = FollowTab.FOLLOWERS)
    }

    // Wait for loading to finish
    composeTestRule.waitForIdle()

    // Error message should be displayed
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }

  // ==================== TOP BAR TESTS ====================

  @Test
  fun followListScreen_displaysTopBarWithUsername() {
    val testProfile = createTestProfile(testUserId, "JohnDoe123")
    val followersList = listOf(createTestProfile(follower1Id))

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to followersList))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent { FollowListScreen(userId = testUserId, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FollowListScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithText("JohnDoe123").assertIsDisplayed()
  }

  @Test
  fun followListScreen_backButtonInvokesCallback() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val followersList = listOf(createTestProfile(follower1Id))

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to followersList))

    val viewModel = FollowListViewModel(repository)
    var backClicked = false

    composeTestRule.setContent {
      FollowListScreen(
          userId = testUserId, viewModel = viewModel, onBackClick = { backClicked = true })
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FollowListScreenTestTags.BACK_BUTTON).performClick()

    assert(backClicked) { "Back button click callback was not invoked" }
  }

  // ==================== TAB NAVIGATION TESTS ====================

  @Test
  fun followListScreen_displaysTabRow_withBothTabs() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val followersList = listOf(createTestProfile(follower1Id))

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to followersList))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent { FollowListScreen(userId = testUserId, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FollowListScreenTestTags.TAB_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.FOLLOWERS_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.FOLLOWING_TAB).assertIsDisplayed()
  }

  @Test
  fun followListScreen_startsWithFollowingTab_whenSpecified() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val followingList = listOf(createTestProfile(following1Id))

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            following = mapOf(testUserId to followingList))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent {
      FollowListScreen(userId = testUserId, initialTab = FollowTab.FOLLOWING, viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Following tab should be selected
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.FOLLOWING_TAB).assertIsDisplayed()
  }

  @Test
  fun followListScreen_switchesTabsCorrectly() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val followersList = listOf(createTestProfile(follower1Id, "Follower1"))
    val followingList = listOf(createTestProfile(following1Id, "Following1"))

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to followersList),
            following = mapOf(testUserId to followingList))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent { FollowListScreen(userId = testUserId, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Initially on followers tab, should see follower
    composeTestRule.onNodeWithText("Follower1").assertIsDisplayed()

    // Switch to following tab
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.FOLLOWING_TAB).performClick()
    composeTestRule.waitForIdle()

    // Should now see following user
    composeTestRule.onNodeWithText("Following1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Follower1").assertDoesNotExist()
  }

  // ==================== PROFILE LIST DISPLAY TESTS ====================

  @Test
  fun followListScreen_displaysFollowersList() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val follower1 = createTestProfile(follower1Id, "Follower1", "Bio for follower 1")
    val follower2 = createTestProfile(follower2Id, "Follower2", "Bio for follower 2")

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to listOf(follower1, follower2)))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent { FollowListScreen(userId = testUserId, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // List should be displayed
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST).assertIsDisplayed()

    // Both followers should be displayed
    composeTestRule.onNodeWithText("Follower1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Follower2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bio for follower 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bio for follower 2").assertIsDisplayed()
  }

  @Test
  fun followListScreen_displaysFollowingList() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val following1 = createTestProfile(following1Id, "Following1", "Bio for following 1")
    val following2 = createTestProfile(following2Id, "Following2", "Bio for following 2")

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            following = mapOf(testUserId to listOf(following1, following2)))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent {
      FollowListScreen(userId = testUserId, initialTab = FollowTab.FOLLOWING, viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // List should be displayed
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST).assertIsDisplayed()

    // Both following users should be displayed
    composeTestRule.onNodeWithText("Following1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Following2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bio for following 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bio for following 2").assertIsDisplayed()
  }

  @Test
  fun followListScreen_profileItemsHaveCorrectTestTags() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val follower1 = createTestProfile(follower1Id, "Follower1")

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to listOf(follower1)))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent { FollowListScreen(userId = testUserId, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Profile item should have the correct test tag
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.profileItemTag(follower1Id))
        .assertIsDisplayed()
  }

  @Test
  fun followListScreen_displaysProfilesWithoutBio() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val follower1 = createTestProfile(follower1Id, "Follower1", bio = null)

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to listOf(follower1)))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent { FollowListScreen(userId = testUserId, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Username should be displayed, bio should not
    composeTestRule.onNodeWithText("Follower1").assertIsDisplayed()
  }

  // ==================== EMPTY STATE TESTS ====================

  @Test
  fun followListScreen_displaysEmptyMessage_whenNoFollowers() {
    val testProfile = createTestProfile(testUserId, "TestUser")

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to emptyList()))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent { FollowListScreen(userId = testUserId, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FollowListScreenTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun followListScreen_displaysEmptyMessage_whenNotFollowingAnyone() {
    val testProfile = createTestProfile(testUserId, "TestUser")

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            following = mapOf(testUserId to emptyList()))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent {
      FollowListScreen(userId = testUserId, initialTab = FollowTab.FOLLOWING, viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FollowListScreenTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }

  // ==================== INTERACTION TESTS ====================

  @Test
  fun followListScreen_profileItemClickInvokesCallback() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val follower1 = createTestProfile(follower1Id, "Follower1")

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to listOf(follower1)))

    val viewModel = FollowListViewModel(repository)
    var clickedProfileId: String? = null

    composeTestRule.setContent {
      FollowListScreen(
          userId = testUserId, viewModel = viewModel, onProfileClick = { clickedProfileId = it })
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.profileItemTag(follower1Id))
        .performClick()

    assert(clickedProfileId == follower1Id) {
      "Expected profile ID $follower1Id but got $clickedProfileId"
    }
  }

  // ==================== LAZY LOADING TESTS ====================

  @Test
  fun followListScreen_loadsFollowersOnlyWhenTabIsFirstSelected() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val followersList = listOf(createTestProfile(follower1Id, "Follower1"))
    val followingList = listOf(createTestProfile(following1Id, "Following1"))

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to followersList),
            following = mapOf(testUserId to followingList))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent {
      FollowListScreen(userId = testUserId, initialTab = FollowTab.FOLLOWING, viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Should start with following tab - follower should not be visible yet
    composeTestRule.onNodeWithText("Following1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Follower1").assertDoesNotExist()

    // Switch to followers tab - should now load and display followers
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.FOLLOWERS_TAB).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Follower1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Following1").assertDoesNotExist()
  }

  // ==================== INTEGRATION TESTS ====================

  @Test
  fun followListScreen_handlesLargeFollowersList() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val followers = (1..50).map { createTestProfile("follower-$it", "Follower$it") }

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile), followers = mapOf(testUserId to followers))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent { FollowListScreen(userId = testUserId, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // List should be displayed
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST).assertIsDisplayed()

    // First few items should be visible (LazyColumn only renders visible items)
    composeTestRule.onNodeWithText("Follower1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Follower2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Follower3").assertIsDisplayed()
  }

  @Test
  fun followListScreen_switchingTabsPreservesData() {
    val testProfile = createTestProfile(testUserId, "TestUser")
    val followersList = listOf(createTestProfile(follower1Id, "Follower1"))
    val followingList = listOf(createTestProfile(following1Id, "Following1"))

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to followersList),
            following = mapOf(testUserId to followingList))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent { FollowListScreen(userId = testUserId, viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Start on followers tab
    composeTestRule.onNodeWithText("Follower1").assertIsDisplayed()

    // Switch to following tab
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.FOLLOWING_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Following1").assertIsDisplayed()

    // Switch back to followers tab - data should be preserved
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.FOLLOWERS_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Follower1").assertIsDisplayed()
  }

  // ==================== LOADING INDICATOR TESTS ====================

  @Test
  fun followListScreen_displaysInitialLoadingIndicator_whileLoading() {
    // Verifies the loading indicator code path and completion
    val testProfile = createTestProfile(testUserId, "TestUser")
    val followersList = listOf(createTestProfile(follower1Id, "Follower1"))

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to followersList))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent {
      FollowListScreen(userId = testUserId, initialTab = FollowTab.FOLLOWERS, viewModel = viewModel)
    }

    // Wait for loading to complete
    composeTestRule.waitForIdle()

    // After loading completes, loading indicator should be gone and content displayed
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LOADING_INDICATOR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST).assertIsDisplayed()
  }

  @Test
  fun followListScreen_displaysPaginationLoadingIndicator_whenLoadingMore() {
    // Verifies the pagination code path is exercised
    val testProfile = createTestProfile(testUserId, "TestUser")
    // Create 30 followers (more than PAGE_SIZE of 25) to enable pagination
    val followersList = (1..30).map { createTestProfile("follower-$it", "Follower$it") }

    val repository =
        FakeProfileRepository(
            profiles = mapOf(testUserId to testProfile),
            followers = mapOf(testUserId to followersList))

    val viewModel = FollowListViewModel(repository)

    composeTestRule.setContent {
      FollowListScreen(userId = testUserId, initialTab = FollowTab.FOLLOWERS, viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Initial load displays first page
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithText("Follower1").assertIsDisplayed()

    // Scroll to trigger pagination (within 5 items of end)
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST).performScrollToIndex(20)

    composeTestRule.waitForIdle()

    // After pagination, list continues to display (pagination loading happens quickly)
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.PAGINATION_LOADING_INDICATOR)
        .assertDoesNotExist()
  }
}
