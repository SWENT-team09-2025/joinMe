package com.android.joinme.ui.profile

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Tab
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Comprehensive tests for ViewProfileScreen using a simple in-memory FakeProfileRepository. */
class ViewProfileScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testUid = "test-uid"
  private val testProfile =
      Profile(
          uid = testUid,
          username = "Max Verstappen",
          email = "speed@f1.com",
          dateOfBirth = "30/09/1997",
          country = "Netherlands",
          interests = listOf("Racing", "Cars", "Technology"),
          bio = "F1 driver with a need for speed",
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
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_titleDisplaysCorrectText() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysAllFields() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.USERNAME_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.EMAIL_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.DATE_OF_BIRTH_FIELD).assertIsDisplayed()

    scrollAndAssert(ViewProfileTestTags.COUNTRY_FIELD)
    scrollAndAssert(ViewProfileTestTags.INTERESTS_FIELD)
    scrollAndAssert(ViewProfileTestTags.BIO_FIELD)
  }

  @Test
  fun viewProfileScreen_bottomNavigationIsDisplayed() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  // ==================== PROFILE DATA DISPLAY TESTS ====================

  @Test
  fun viewProfileScreen_displaysCorrectUsername() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Max Verstappen").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysCorrectEmail() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("speed@f1.com").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysCorrectDateOfBirth() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("30/09/1997").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysCorrectCountry() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("Netherlands")
  }

  @Test
  fun viewProfileScreen_displaysCorrectInterests() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("Racing, Cars, Technology")
  }

  @Test
  fun viewProfileScreen_displaysCorrectBio() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("F1 driver with a need for speed")
  }

  @Test
  fun viewProfileScreen_displaysAllFieldLabels() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Username").assertIsDisplayed()
    composeTestRule.onNodeWithText("Email").assertIsDisplayed()
    composeTestRule.onNodeWithText("Date of Birth").assertIsDisplayed()

    scrollAndAssertText("Country/Region")
    scrollAndAssertText("Interests")
    scrollAndAssertText("Bio")
  }

  // ==================== NULL/EMPTY VALUES TESTS ====================

  @Test
  fun viewProfileScreen_displaysNotSpecified_forNullDateOfBirth() = runTest {
    val profileWithNullDate = testProfile.copy(dateOfBirth = null)
    val repo = FakeProfileRepository(profileWithNullDate)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Date not specified").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysNotSpecified_forNullCountry() = runTest {
    val profileWithNullCountry = testProfile.copy(country = null)
    val repo = FakeProfileRepository(profileWithNullCountry)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("Country not specified")
  }

  @Test
  fun viewProfileScreen_displaysNoBio_forNullBio() = runTest {
    val profileWithNullBio = testProfile.copy(bio = null)
    val repo = FakeProfileRepository(profileWithNullBio)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("No bio available")
  }

  @Test
  fun viewProfileScreen_displaysNone_forEmptyInterests() = runTest {
    val profileWithEmptyInterests = testProfile.copy(interests = emptyList())
    val repo = FakeProfileRepository(profileWithEmptyInterests)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("None")
  }

  // ==================== LOGOUT BUTTON TESTS ====================

  @Test
  fun viewProfileScreen_logoutButton_isDisplayed() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_logoutButton_isClickable() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.LOGOUT_BUTTON).assertHasClickAction()
  }

  // ==================== NAVIGATION TESTS ====================

  @Test
  fun viewProfileScreen_backButton_isClickable() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)
    var backClicked = false

    composeTestRule.setContent {
      ViewProfileScreen(
          uid = testUid, profileViewModel = viewModel, onBackClick = { backClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    assert(backClicked)
  }

  @Test
  fun viewProfileScreen_groupButton_isClickable() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)
    var groupClicked = false

    composeTestRule.setContent {
      ViewProfileScreen(
          uid = testUid, profileViewModel = viewModel, onGroupClick = { groupClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Group").performClick()
    assert(groupClicked)
  }

  @Test
  fun viewProfileScreen_editButton_isClickable() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)
    var editClicked = false

    composeTestRule.setContent {
      ViewProfileScreen(
          uid = testUid, profileViewModel = viewModel, onEditClick = { editClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Edit").performClick()
    assert(editClicked)
  }

  @Test
  fun viewProfileScreen_topBar_allNavigationButtonsWork() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)
    var back = false
    var group = false
    var edit = false

    composeTestRule.setContent {
      ViewProfileScreen(
          uid = testUid,
          profileViewModel = viewModel,
          onBackClick = { back = true },
          onGroupClick = { group = true },
          onEditClick = { edit = true })
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    assert(back)
    composeTestRule.onNodeWithContentDescription("Group").performClick()
    assert(group)
    composeTestRule.onNodeWithContentDescription("Edit").performClick()
    assert(edit)
  }

  @Test
  fun viewProfileScreen_bottomNavigation_tabsAreClickable() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)
    var selectedTab: Tab? = null

    composeTestRule.setContent {
      ViewProfileScreen(
          uid = testUid, profileViewModel = viewModel, onTabSelected = { selectedTab = it })
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).performClick()
    assert(selectedTab == Tab.Overview)
  }

  @Test
  fun viewProfileScreen_bottomNavigation_allTabsClickable() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)
    val clickedTabs = mutableListOf<Tab>()

    composeTestRule.setContent {
      ViewProfileScreen(
          uid = testUid, profileViewModel = viewModel, onTabSelected = { clickedTabs.add(it) })
    }

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
    val repo = FakeProfileRepository(testProfile, failOnce = true)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ViewProfileTestTags.RETRY_BUTTON).assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_retryButton_reloadsProfile() = runTest {
    val repo = FakeProfileRepository(testProfile, failOnce = true)
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
    val repo = FakeProfileRepository(testProfile, failOnce = true)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    val retryButton = composeTestRule.onNodeWithTag(ViewProfileTestTags.RETRY_BUTTON)
    retryButton.assertIsDisplayed()
    retryButton.assertHasClickAction()
  }

  // ==================== SCROLLING TESTS ====================

  @Test
  fun viewProfileScreen_isScrollable() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.SCROLL_CONTAINER).assertExists()
  }

  @Test
  fun viewProfileScreen_longBio_rendersAndIsScrollable() = runTest {
    val longBio = "This is a very long biography. ".repeat(50)
    val repo = FakeProfileRepository(testProfile.copy(bio = longBio))
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssert(ViewProfileTestTags.BIO_FIELD)
    composeTestRule.onNodeWithTag(ViewProfileTestTags.BIO_FIELD).assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_canScrollToAllFields() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // Test that we can scroll to each field
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
    val profileWithSpecialChars = testProfile.copy(username = "Max_Verstappen 33")
    val repo = FakeProfileRepository(profileWithSpecialChars)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Max_Verstappen 33").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_handlesMultipleInterests() = runTest {
    val profileWithManyInterests =
        testProfile.copy(
            interests = listOf("Racing", "Cars", "Technology", "Gaming", "Travel", "Music"))
    val repo = FakeProfileRepository(profileWithManyInterests)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("Racing, Cars, Technology, Gaming, Travel, Music")
  }

  @Test
  fun viewProfileScreen_handlesSingleInterest() = runTest {
    val profileWithSingleInterest = testProfile.copy(interests = listOf("Racing"))
    val repo = FakeProfileRepository(profileWithSingleInterest)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    scrollAndAssertText("Racing")
  }

  // ==================== PROFILE PICTURE TESTS ====================

  @Test
  fun viewProfileScreen_profilePictureContainer_isDisplayed() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_profilePicture_displaysDefaultAvatar_whenNoPhotoUrl() = runTest {
    val profileWithoutPhoto = testProfile.copy(photoUrl = null)
    val repo = FakeProfileRepository(profileWithoutPhoto)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // Should display the default avatar icon when photoUrl is null
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.DEFAULT_AVATAR).assertIsDisplayed()
    // Should NOT display remote image
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.REMOTE_IMAGE).assertDoesNotExist()
  }

  @Test
  fun viewProfileScreen_profilePicture_rendersRemoteImage_whenPhotoUrlProvided() = runTest {
    val profileWithPhoto = testProfile.copy(photoUrl = "https://example.com/photo.jpg")
    val repo = FakeProfileRepository(profileWithPhoto)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // Should display the remote image component when photoUrl is provided
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.REMOTE_IMAGE).assertExists()
    // Should NOT display default avatar (unless image fails to load)
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.DEFAULT_AVATAR).assertDoesNotExist()
  }

  @Test
  fun viewProfileScreen_profilePicture_isNotClickable() = runTest {
    val repo = FakeProfileRepository(testProfile)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // ViewProfile picture should not have click action (unlike EditProfile)
    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_PICTURE).assert(hasNoClickAction())
  }

  @Test
  fun viewProfileScreen_profilePicture_persistsAcrossScreenRefresh() = runTest {
    val profileWithPhoto = testProfile.copy(photoUrl = "https://example.com/photo.jpg")
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
    val profileWithEmptyUrl = testProfile.copy(photoUrl = "")
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
    val profileWithLongUrl = testProfile.copy(photoUrl = longUrl)
    val repo = FakeProfileRepository(profileWithLongUrl)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    // Should handle long URLs without crashing - remote image should be attempted
    composeTestRule.onNodeWithTag(ViewProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.REMOTE_IMAGE).assertExists()
  }
}
