package com.android.joinme.ui.profile

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Tab
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Comprehensive tests for ViewProfileScreen using a simple in-memory FakeProfileRepository. */
@RunWith(RobolectricTestRunner::class)
class ViewProfileScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    // Initialize Firebase if not already initialized
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }
  }

  private val testUid = "test-uid"

  private fun createTestProfile() =
      Profile(
          uid = testUid,
          username = "Max Verstappen",
          email = "speed@f1.com",
          dateOfBirth = "30/09/1997",
          country = "Netherlands",
          interests = listOf("Racing", "Cars", "Technology"),
          bio = "F1 driver with a need for speed",
          eventsJoinedCount = 42,
          followersCount = 1250,
          followingCount = 89,
          createdAt = Timestamp.now(),
          updatedAt = Timestamp.now())

  // A tiny in-memory repo that mimics just what the ViewModel needs
  private class FakeProfileRepository(
      private var stored: Profile? = null,
      private var failOnce: Boolean = false
  ) : ProfileRepository {

    override suspend fun getProfile(uid: String): Profile? {
      if (failOnce) {
        failOnce = false
        throw RuntimeException("Network error")
      }
      return stored?.takeIf { it.uid == uid }
    }

    override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? {
      if (uids.isEmpty()) return emptyList()
      val result = uids.mapNotNull { stored?.takeIf { p -> p.uid == it } }
      return if (result.size == uids.size) result else null
    }

    override suspend fun createOrUpdateProfile(profile: Profile) {
      stored = profile
    }

    override suspend fun deleteProfile(uid: String) {
      if (stored?.uid == uid) stored = null
    }

    // Stub implementations for photo methods - not used in ViewProfile tests
    override suspend fun uploadProfilePhoto(
        context: android.content.Context,
        uid: String,
        imageUri: Uri
    ): String {
      return "https://example.com/photo.jpg"
    }

    override suspend fun deleteProfilePhoto(uid: String) {
      stored = stored?.copy(photoUrl = null)
    }

    // Stub implementations for follow methods - not used in ViewProfile tests
    override suspend fun followUser(followerId: String, followedId: String) {}

    override suspend fun unfollowUser(followerId: String, followedId: String) {}

    override suspend fun isFollowing(followerId: String, followedId: String): Boolean = false

    override suspend fun getFollowing(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getFollowers(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getMutualFollowing(userId1: String, userId2: String): List<Profile> =
        emptyList()
  }

  // Helper function to scroll and assert
  private fun scrollAndAssert(tag: String) {
    composeTestRule
        .onNodeWithTag(ViewProfileTestTags.SCROLL_CONTAINER)
        .performScrollToNode(hasTestTag(tag))
    composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
  }

  private fun scrollAndAssertText(text: String) {
    composeTestRule
        .onNodeWithTag(ViewProfileTestTags.SCROLL_CONTAINER)
        .performScrollToNode(hasText(text))
    composeTestRule.onNodeWithText(text).assertIsDisplayed()
  }

  // ==================== BASIC UI DISPLAY TESTS ====================

  @Test
  fun viewProfileScreen_displaysAllComponents() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.STATS_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_titleDisplaysCorrectText() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_bottomNavigationIsDisplayed() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  // ==================== PROFILE DATA DISPLAY TESTS ====================

  @Test
  fun viewProfileScreen_displaysValues() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Max Verstappen").assertIsDisplayed()
    scrollAndAssertText("speed@f1.com")
    scrollAndAssertText("Netherlands")
    scrollAndAssertText("Racing, Cars, Technology")
    scrollAndAssertText("F1 driver with a need for speed")
  }

  @Test
  fun viewProfileScreen_displaysCorrectBio() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }
  }

  // ==================== NULL/EMPTY VALUES TESTS ====================

  @Test
  fun viewProfileScreen_displaysNotSpecified_forNullCountry() = runTest {
    val profileWithNullCountry = createTestProfile().copy(country = null)
    val repo = FakeProfileRepository(profileWithNullCountry)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("Country not specified")
  }

  @Test
  fun viewProfileScreen_displaysNone_forEmptyInterests() = runTest {
    val profileWithNoInterests = createTestProfile().copy(interests = emptyList())
    val repo = FakeProfileRepository(profileWithNoInterests)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("None")
  }

  @Test
  fun viewProfileScreen_displaysNoBioAvailable_forNullBio() = runTest {
    val profileWithNullBio = createTestProfile().copy(bio = null)

    val repo = FakeProfileRepository(profileWithNullBio)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("No bio available")
  }

  // ==================== BUTTON INTERACTION TESTS ====================

  @Test
  fun viewProfileScreen_topBar_allNavigationButtonsWork() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)
    var group = false
    var edit = false
    var logoutClicked = false

    composeTestRule.setContent {
      ViewProfileScreen(
          uid = testUid,
          profileViewModel = viewModel,
          onGroupClick = { group = true },
          onEditClick = { edit = true },
          onSignOutComplete = { logoutClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Group").performClick()
    assert(group)
    composeTestRule.onNodeWithContentDescription("Edit").performClick()
    assert(edit)

    // Click logout button - this should show the confirmation dialog
    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_BUTTON).performClick()

    // Verify dialog is shown
    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_CONFIRM_DIALOG).assertIsDisplayed()

    // Click confirm button in dialog
    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_CONFIRM_BUTTON).performClick()

    // Now logout should be triggered
    assert(logoutClicked)
  }

  @Test
  fun viewProfileScreen_logoutDialog_cancelButton_dismissesDialog() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)
    var logoutClicked = false

    composeTestRule.setContent {
      ViewProfileScreen(
          uid = testUid, profileViewModel = viewModel, onSignOutComplete = { logoutClicked = true })
    }

    // Click logout button to show dialog
    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_BUTTON).performClick()

    // Verify dialog is shown
    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_CONFIRM_DIALOG).assertIsDisplayed()

    // Click cancel button
    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_CANCEL_BUTTON).performClick()

    // Dialog should be dismissed and logout should NOT be triggered
    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_CONFIRM_DIALOG).assertDoesNotExist()
    assert(!logoutClicked)
  }

  @Test
  fun viewProfileScreen_bottomNavigation_allTabsClickable() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)
    val clickedTabs = mutableListOf<Tab>()

    composeTestRule.setContent {
      ViewProfileScreen(
          uid = testUid, profileViewModel = viewModel, onTabSelected = { clickedTabs.add(it) })
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Map")).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).performClick()

    assert(clickedTabs.contains(Tab.Overview))
    assert(clickedTabs.contains(Tab.Search))
    assert(clickedTabs.contains(Tab.Map))
    assert(clickedTabs.contains(Tab.Profile))
  }

  // ==================== ERROR HANDLING TESTS ====================

  @Test
  fun viewProfileScreen_displaysError_whenLoadFails() = runTest {
    val repo = FakeProfileRepository(createTestProfile(), failOnce = true)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.RETRY_BUTTON).assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_retryButton_reloadsProfile() = runTest {
    val repo = FakeProfileRepository(createTestProfile(), failOnce = true)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // First render shows error
    composeTestRule.onNodeWithTag(ViewProfileTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.RETRY_BUTTON).assertIsDisplayed()

    // Retry
    composeTestRule.onNodeWithTag(ViewProfileTestTags.RETRY_BUTTON).performClick()

    // Should now render profile
    composeTestRule.onNodeWithText("Max Verstappen").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_retryButton_isClickable() = runTest {
    val repo = FakeProfileRepository(createTestProfile(), failOnce = true)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    val retryButton = composeTestRule.onNodeWithTag(ViewProfileTestTags.RETRY_BUTTON)
    retryButton.assertIsDisplayed()
    retryButton.assertHasClickAction()
  }

  // ==================== SCROLLING TESTS ====================

  @Test
  fun viewProfileScreen_longBio_rendersAndIsScrollable() = runTest {
    val longBio = "This is a very long biography. ".repeat(50)
    val repo = FakeProfileRepository(createTestProfile().copy(bio = longBio))
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.SCROLL_CONTAINER).assertExists()

    val fieldsToScroll =
        listOf(
            ViewProfileTestTags.USERNAME_FIELD,
            ViewProfileTestTags.EMAIL_FIELD,
            ViewProfileTestTags.DATE_OF_BIRTH_FIELD,
            ViewProfileTestTags.COUNTRY_FIELD,
            ViewProfileTestTags.INTERESTS_FIELD,
            ViewProfileTestTags.BIO_FIELD)

    fieldsToScroll.forEach { tag -> scrollAndAssert(tag) }
  }

  // ==================== SPECIAL CHARACTERS TESTS ====================

  @Test
  fun viewProfileScreen_handlesSpecialCharactersInUsername() = runTest {
    val profileWithSpecialChars = createTestProfile().copy(username = "Max_Verstappen 33")
    val repo = FakeProfileRepository(profileWithSpecialChars)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Max_Verstappen 33").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_handlesMultipleInterests() = runTest {
    val profileWithManyInterests =
        createTestProfile()
            .copy(interests = listOf("Racing", "Cars", "Technology", "Gaming", "Travel", "Music"))
    val repo = FakeProfileRepository(profileWithManyInterests)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("Racing, Cars, Technology, Gaming, Travel, Music")
  }

  @Test
  fun viewProfileScreen_handlesSingleInterest() = runTest {
    val profileWithSingleInterest = createTestProfile().copy(interests = listOf("Racing"))
    val repo = FakeProfileRepository(profileWithSingleInterest)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("Racing")
  }

  // ==================== PROFILE PICTURE TESTS ====================

  @Test
  fun viewProfileScreen_profilePictureContainer_isDisplayed() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_profilePicture_displaysDefaultAvatar_whenNoPhotoUrl() = runTest {
    val profileWithoutPhoto = createTestProfile().copy(photoUrl = null)
    val repo = FakeProfileRepository(profileWithoutPhoto)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // Should display the default avatar icon when photoUrl is null
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.DEFAULT_AVATAR).assertIsDisplayed()
    // Should NOT display remote image
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.REMOTE_IMAGE).assertDoesNotExist()
  }

  @Test
  fun viewProfileScreen_profilePicture_isNotClickable() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // ViewProfile picture should not have click action (unlike EditProfile)
    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_PICTURE).assert(hasNoClickAction())
  }

  @Test
  fun viewProfileScreen_profilePicture_persistsAcrossScreenRefresh() = runTest {
    val profileWithPhoto = createTestProfile().copy(photoUrl = "https://example.com/photo.jpg")
    val repo = FakeProfileRepository(profileWithPhoto)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // Check photo container is displayed
    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
    // Check remote image is being used
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.REMOTE_IMAGE).assertExists()

    // Reload profile
    viewModel.loadProfile(testUid)
    composeTestRule.waitForIdle()

    // Photo should still be displayed with remote image
    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.REMOTE_IMAGE).assertExists()
  }

  @Test
  fun viewProfileScreen_profilePicture_handlesEmptyPhotoUrl() = runTest {
    val profileWithEmptyUrl = createTestProfile().copy(photoUrl = "")
    val repo = FakeProfileRepository(profileWithEmptyUrl)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // Empty string photoUrl should display default avatar (not remote image)
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.DEFAULT_AVATAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.REMOTE_IMAGE).assertDoesNotExist()
  }

  @Test
  fun viewProfileScreen_profilePicture_withLongPhotoUrl_doesNotCrash() = runTest {
    val longUrl = "https://example.com/" + "a".repeat(500) + ".jpg"
    val profileWithLongUrl = createTestProfile().copy(photoUrl = longUrl)
    val repo = FakeProfileRepository(profileWithLongUrl)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // Should handle long URLs without crashing - remote image should be attempted
    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.REMOTE_IMAGE).assertExists()
  }

  // ==================== STATS AND STREAKS TESTS ====================

  @Test
  fun viewProfileScreen_displaysStatsRow() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.STATS_ROW).assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysEventsJoinedStat() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.EVENTS_JOINED_STAT).assertIsDisplayed()
    composeTestRule.onNodeWithText("42").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysFollowersStat_withFormattedCount() = runTest {
    val profile = createTestProfile().copy(followersCount = 1200)
    val repo = FakeProfileRepository(profile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.FOLLOWERS_STAT).assertIsDisplayed()
    // 1200 should be formatted as "1.2k"
    composeTestRule.onNodeWithText("1.2k").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysFollowingStat() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.FOLLOWING_STAT).assertIsDisplayed()
    composeTestRule.onNodeWithText("89").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysEventStreaksSection() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.EVENT_STREAKS_SECTION).assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysCorrectStatsValues() = runTest {
    val profile =
        createTestProfile()
            .copy(eventsJoinedCount = 100, followersCount = 5000, followingCount = 250)
    val repo = FakeProfileRepository(profile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("100").assertIsDisplayed() // events joined
    composeTestRule.onNodeWithText("5.0k").assertIsDisplayed() // followers formatted
    composeTestRule.onNodeWithText("250").assertIsDisplayed() // following
  }

  @Test
  fun viewProfileScreen_handlesLargeFollowerCount() = runTest {
    val profile = createTestProfile().copy(followersCount = 28_800_000)
    val repo = FakeProfileRepository(profile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // 28,800,000 should be formatted as "28.8m"
    composeTestRule.onNodeWithText("28.8m").assertIsDisplayed()
  }
}
