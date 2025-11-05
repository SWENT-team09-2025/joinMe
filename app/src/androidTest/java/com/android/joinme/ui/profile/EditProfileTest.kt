package com.android.joinme.ui.profile

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Comprehensive tests for EditProfileScreen using a tiny in-memory FakeProfileRepository. */
class EditProfileScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // --- Test data ---
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
          updatedAt = Timestamp.now(),
      )

  // A tiny in-memory repo that mimics just what the ViewModel needs
  private class FakeProfileRepository(
      private var stored: Profile? = null,
      private var failOnce: Boolean = false,
      private var photoUploadShouldFail: Boolean = false,
      private var simulateUploadDelay: Boolean = false,
      private var simulateDeleteDelay: Boolean = false
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

    override suspend fun uploadProfilePhoto(
        context: android.content.Context,
        uid: String,
        imageUri: Uri
    ): String {
      if (photoUploadShouldFail) {
        throw RuntimeException("Upload failed")
      }
      if (simulateUploadDelay) {
        kotlinx.coroutines.delay(100) // Simulate network delay
      }
      val downloadUrl = "https://example.com/photos/$uid/profile.jpg"
      // Update stored profile with new photo URL
      stored = stored?.copy(photoUrl = downloadUrl)
      return downloadUrl
    }

    override suspend fun deleteProfilePhoto(uid: String) {
      if (simulateDeleteDelay) {
        kotlinx.coroutines.delay(100)
      }
      stored = stored?.copy(photoUrl = null)
    }
  }

  // --- Helper: scroll a child into view, then assert it's visible ---
  private fun ComposeContentTestRule.scrollIntoViewAndAssert(tag: String) {
    onNodeWithTag(tag).performScrollTo()
    onNodeWithTag(tag).assertIsDisplayed()
  }

  // ==================== BASIC UI DISPLAY TESTS ====================

  @Test
  fun editProfile_displaysAllCoreFields_scrollingForOffscreenOnes() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule.onNodeWithTag(EditProfileTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileTestTags.EMAIL_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD).assertIsDisplayed()

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.COUNTRY_FIELD)
    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.INTERESTS_FIELD)
    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.BIO_FIELD)
    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.SAVE_BUTTON)
  }

  @Test
  fun editProfile_titleIsDisplayed() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule.onNodeWithTag(EditProfileTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Edit Profile").assertIsDisplayed()
  }

  @Test
  fun editProfile_profilePictureIsDisplayed() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule.onNodeWithTag(EditProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertIsDisplayed()
  }

  // ==================== INITIAL VALUES TESTS ====================

  @Test
  fun editProfile_populatesInitialValues() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule
        .onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
        .assertTextContains("Max Verstappen", substring = true)
    composeTestRule
        .onNodeWithTag(EditProfileTestTags.EMAIL_FIELD)
        .assertTextContains("speed@f1.com", substring = true)
    composeTestRule
        .onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
        .assertTextContains("30/09/1997", substring = true)

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.COUNTRY_FIELD)
    composeTestRule
        .onNodeWithTag(EditProfileTestTags.COUNTRY_FIELD)
        .assertTextContains("Netherlands", substring = true)

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.INTERESTS_FIELD)
    composeTestRule
        .onNodeWithTag(EditProfileTestTags.INTERESTS_FIELD)
        .assertTextContains("Racing, Cars, Technology", substring = true)

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.BIO_FIELD)
    composeTestRule
        .onNodeWithTag(EditProfileTestTags.BIO_FIELD)
        .assertTextContains("need for speed", substring = true)
  }

  @Test
  fun editProfile_populatesEmptyFieldsCorrectly() = runTest {
    val emptyProfile =
        testProfile.copy(dateOfBirth = null, country = null, interests = emptyList(), bio = null)
    val vm = ProfileViewModel(FakeProfileRepository(emptyProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule
        .onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
        .assertTextContains("", substring = false)

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.COUNTRY_FIELD)
    composeTestRule
        .onNodeWithTag(EditProfileTestTags.COUNTRY_FIELD)
        .assertTextContains("", substring = false)
  }

  // ==================== EDITABILITY TESTS ====================

  @Test
  fun editProfile_username_isEditable() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
    node.performTextClearance()
    node.performTextInput("Checo Pérez")

    node.assertTextContains("Checo Pérez", substring = false)
  }

  @Test
  fun editProfile_email_isNotEditable() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val emailField = composeTestRule.onNodeWithTag(EditProfileTestTags.EMAIL_FIELD)
    emailField.assertIsNotEnabled()
  }

  @Test
  fun editProfile_dateOfBirth_isEditable() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
    node.performTextClearance()
    node.performTextInput("01/01/2000")

    node.assertTextContains("01/01/2000", substring = false)
  }

  @Test
  fun editProfile_country_isEditable() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.COUNTRY_FIELD)
    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.COUNTRY_FIELD)
    node.performTextClearance()
    node.performTextInput("Switzerland")

    node.assertTextContains("Switzerland", substring = false)
  }

  @Test
  fun editProfile_interests_isEditable() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.INTERESTS_FIELD)
    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.INTERESTS_FIELD)
    node.performTextClearance()
    node.performTextInput("Reading, Swimming")

    node.assertTextContains("Reading, Swimming", substring = false)
  }

  @Test
  fun editProfile_bio_isEditable() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.BIO_FIELD)
    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.BIO_FIELD)
    node.performTextClearance()
    node.performTextInput("New bio text")

    node.assertTextContains("New bio text", substring = false)
  }

  // ==================== VALIDATION TESTS ====================

  @Test
  fun editProfile_username_showsErrorForTooShort() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
    node.performTextClearance()
    node.performTextInput("ab")

    composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertIsDisplayed()
  }

  @Test
  fun editProfile_username_showsErrorForTooLong() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
    node.performTextClearance()
    node.performTextInput("a".repeat(31))

    composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertIsDisplayed()
  }

  @Test
  fun editProfile_username_showsErrorForInvalidCharacters() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
    node.performTextClearance()
    node.performTextInput("invalid@name")

    composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertIsDisplayed()
  }

  @Test
  fun editProfile_username_acceptsValidCharacters() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
    node.performTextClearance()
    node.performTextInput("Valid_User 123")

    composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertDoesNotExist()
  }

  @Test
  fun editProfile_dateOfBirth_showsErrorForInvalidFormat() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
    node.performTextClearance()
    node.performTextInput("01-01-2000")

    composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_ERROR).assertIsDisplayed()
  }

  @Test
  fun editProfile_dateOfBirth_showsErrorForInvalidDate() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
    node.performTextClearance()
    node.performTextInput("32/13/2000")

    composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_ERROR).assertIsDisplayed()
  }

  @Test
  fun editProfile_dateOfBirth_acceptsValidDate() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
    node.performTextClearance()
    node.performTextInput("15/06/1995")

    composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_ERROR).assertDoesNotExist()
  }

  @Test
  fun editProfile_dateOfBirth_allowsEmpty() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
    node.performTextClearance()

    composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_ERROR).assertDoesNotExist()
  }

  @Test
  fun editProfile_date_leapYear_isValid() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
    node.performTextClearance()
    node.performTextInput("29/02/2000")

    composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_ERROR).assertDoesNotExist()
  }

  @Test
  fun editProfile_date_nonLeapYear_showsError() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
    node.performTextClearance()
    node.performTextInput("29/02/2001")

    composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_ERROR).assertIsDisplayed()
  }

  // ==================== SAVE BUTTON TESTS ====================

  @Test
  fun editProfile_saveButton_enabledWhenValid() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun editProfile_saveButton_disabledWhenUsernameInvalid() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
    node.performTextClearance()
    node.performTextInput("ab")

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun editProfile_saveButton_disabledWhenDateInvalid() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
    node.performTextClearance()
    node.performTextInput("32/13/2000")

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun editProfile_saveButton_disabledWhenUsernameEmpty() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
    node.performTextClearance()

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).assertIsNotEnabled()
  }

  // ==================== PHOTO EDIT BUTTON TESTS ====================

  @Test
  fun editProfile_editPhotoButton_isDisplayed() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertIsDisplayed()
  }

  @Test
  fun editProfile_editPhotoButton_isEnabled() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Verify button is enabled (can't click as it launches system picker)
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertHasClickAction()
  }

  @Test
  fun editProfile_editPhotoButton_hasClickAction() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertHasClickAction()
  }

  // ==================== PHOTO UPLOAD STATE TESTS ====================

  @Test
  fun editProfile_photoUploadIndicator_notShownInitially() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Upload indicator should not be visible initially
    composeTestRule
        .onNodeWithTag(EditProfileTestTags.PHOTO_UPLOADING_INDICATOR)
        .assertDoesNotExist()
  }

  @Test
  fun editProfile_editPhotoButton_hidesDuringUpload() = runTest {
    val repo = FakeProfileRepository(testProfile, simulateUploadDelay = true)
    val vm = ProfileViewModel(repo)

    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Simulate starting an upload by directly calling the ViewModel
    vm.uploadProfilePhoto(
        context = ApplicationProvider.getApplicationContext(),
        imageUri = Uri.parse("content://test/image.jpg"),
        onSuccess = {},
        onError = {})

    // Wait a moment for state to update
    composeTestRule.waitForIdle()

    // During upload, the button should be hidden
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertDoesNotExist()
  }

  @Test
  fun editProfile_photoUploadIndicator_showsDuringUpload() = runTest {
    val repo = FakeProfileRepository(testProfile, simulateUploadDelay = true)
    val vm = ProfileViewModel(repo)

    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Trigger upload via ViewModel
    vm.uploadProfilePhoto(
        context = ApplicationProvider.getApplicationContext(),
        imageUri = Uri.parse("content://test/image.jpg"),
        onSuccess = {},
        onError = {})

    composeTestRule.waitForIdle()

    // Upload indicator should be visible during upload
    composeTestRule.onNodeWithTag(EditProfileTestTags.PHOTO_UPLOADING_INDICATOR).assertIsDisplayed()
    // Buttons should be hidden
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(EditProfileTestTags.DELETE_PHOTO_BUTTON).assertDoesNotExist()
  }

  @Test
  fun editProfile_profilePicture_displaysWithExistingPhoto() = runTest {
    val profileWithPhoto = testProfile.copy(photoUrl = "https://example.com/photo.jpg")
    val vm = ProfileViewModel(FakeProfileRepository(profileWithPhoto))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Profile picture container should be displayed
    composeTestRule.onNodeWithTag(EditProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
    // Edit button should be available
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertIsDisplayed()
  }

  @Test
  fun editProfile_profilePicture_displaysWithoutPhoto() = runTest {
    val profileWithoutPhoto = testProfile.copy(photoUrl = null)
    val vm = ProfileViewModel(FakeProfileRepository(profileWithoutPhoto))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Should show default avatar when no photo URL
    composeTestRule.onNodeWithTag(EditProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertIsDisplayed()
  }

  @Test
  fun editProfile_profilePicture_handlesEmptyPhotoUrl() = runTest {
    val profileWithEmptyUrl = testProfile.copy(photoUrl = "")
    val vm = ProfileViewModel(FakeProfileRepository(profileWithEmptyUrl))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Should handle empty string gracefully
    composeTestRule.onNodeWithTag(EditProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
  }

  @Test
  fun editProfile_profilePicture_overlayVisible() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // The profile picture section includes a scrim overlay for the edit UI
    composeTestRule.onNodeWithTag(EditProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
    // The overlay makes the edit button visible and accessible
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertIsDisplayed()
  }

  @Test
  fun editProfile_profilePicture_maintainsStateAfterScroll() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Initially visible
    composeTestRule.onNodeWithTag(EditProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()

    // Scroll down to save button
    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.SAVE_BUTTON)

    // Scroll back up to profile picture
    composeTestRule.onNodeWithTag(EditProfileTestTags.PROFILE_PICTURE).performScrollTo()

    // Should still be displayed with edit button
    composeTestRule.onNodeWithTag(EditProfileTestTags.PROFILE_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertIsDisplayed()
  }

  @Test
  fun editProfile_saveButton_worksIndependentlyOfPhotoState() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    var saveClicked = false

    composeTestRule.setContent {
      EditProfileScreen(
          uid = testUid, profileViewModel = vm, onSaveSuccess = { saveClicked = true })
    }

    // Edit username to make form valid
    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
    node.performTextClearance()
    node.performTextInput("New Name")

    // Save button should work regardless of photo state
    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).assertIsEnabled()
  }

  // ==================== NAVIGATION TESTS ====================

  @Test
  fun editProfile_backButton_isClickable() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    var backClicked = false
    composeTestRule.setContent {
      EditProfileScreen(uid = testUid, profileViewModel = vm, onBackClick = { backClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    assert(backClicked)
  }

  @Test
  fun editProfile_profileTabButton_isClickable() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    var profileClicked = false
    composeTestRule.setContent {
      EditProfileScreen(
          uid = testUid, profileViewModel = vm, onProfileClick = { profileClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Profile").performClick()
    assert(profileClicked)
  }

  @Test
  fun editProfile_groupTabButton_isClickable() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    var groupClicked = false
    composeTestRule.setContent {
      EditProfileScreen(
          uid = testUid, profileViewModel = vm, onGroupClick = { groupClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Group").performClick()
    assert(groupClicked)
  }

  // ==================== COMPLEX INTERACTION TESTS ====================

  @Test
  fun editProfile_multipleFieldEdits_maintainState() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Edit username
    val usernameNode = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
    usernameNode.performTextClearance()
    usernameNode.performTextInput("New Username")

    // Edit date
    val dateNode = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
    dateNode.performTextClearance()
    dateNode.performTextInput("01/01/2000")

    // Scroll and edit country
    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.COUNTRY_FIELD)
    val countryNode = composeTestRule.onNodeWithTag(EditProfileTestTags.COUNTRY_FIELD)
    countryNode.performTextClearance()
    countryNode.performTextInput("France")

    // Verify all changes persist
    usernameNode.assertTextContains("New Username")
    dateNode.assertTextContains("01/01/2000")
    countryNode.assertTextContains("France")
  }

  @Test
  fun editProfile_errorThenValid_saveButtonReEnables() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)

    // Make it invalid
    node.performTextClearance()
    node.performTextInput("ab")

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).assertIsNotEnabled()

    // Make it valid again
    node.performTextClearance()
    node.performTextInput("valid username")

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).assertIsEnabled()
  }

  @Test
  fun editProfile_photoButton_doesNotDisableSaveButton() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Verify photo button exists and save button is enabled
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertIsDisplayed()

    composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(EditProfileTestTags.SAVE_BUTTON).assertIsEnabled()

    // Both photo button and save button should be independently functional
    // (We can't click photo button as it launches system picker, blocking tests)
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertHasClickAction()
  }

  @Test
  fun editProfile_editingFields_doesNotAffectPhotoButton() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(testProfile))
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Photo button is enabled initially
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertIsEnabled()

    // Edit some fields
    val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
    node.performTextClearance()
    node.performTextInput("New Name")

    // Photo button should still be enabled
    composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).assertIsEnabled()
  }

  // ==================== PHOTO DELETE TESTS ====================

  @Test
  fun editProfile_deletePhotoButton_notShown_whenNoPhoto() = runTest {
    val profileWithoutPhoto = testProfile.copy(photoUrl = null)
    val repo = FakeProfileRepository(profileWithoutPhoto) // Repo with no photo
    val vm = ProfileViewModel(repo)
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Delete button should not be displayed
    composeTestRule.onNodeWithTag(EditProfileTestTags.DELETE_PHOTO_BUTTON).assertDoesNotExist()
  }

  @Test
  fun editProfile_deletePhotoButton_isShown_whenPhotoExists() = runTest {
    val profileWithPhoto = testProfile.copy(photoUrl = "https://example.com/photo.jpg")
    val repo = FakeProfileRepository(profileWithPhoto) // Repo with photo
    val vm = ProfileViewModel(repo)
    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Delete button should be displayed
    composeTestRule.onNodeWithTag(EditProfileTestTags.DELETE_PHOTO_BUTTON).assertIsDisplayed()
  }

  @Test
  fun editProfile_clickingDeleteButton_triggersDeletion() = runTest {
    val profileWithPhoto = testProfile.copy(photoUrl = "https://example.com/photo.jpg")
    val repo = FakeProfileRepository(profileWithPhoto) // Repo with photo
    val vm = ProfileViewModel(repo)

    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // 1. Verify photo and delete button are visible
    composeTestRule.onNodeWithTag(EditProfileTestTags.DELETE_PHOTO_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.REMOTE_IMAGE).assertExists()
    // FIX: In test env, Coil load fails, so error fallback (DEFAULT_AVATAR) is displayed.
    composeTestRule
        .onNodeWithTag(ProfilePhotoImageTestTags.DEFAULT_AVATAR)
        .assertIsDisplayed() // <-- CORRECTED ASSERTION

    // 2. Click delete button
    composeTestRule.onNodeWithTag(EditProfileTestTags.DELETE_PHOTO_BUTTON).performClick()

    // 3. Wait for UI to update
    composeTestRule.waitForIdle()

    // 4. Verify UI updates: delete button is gone, default avatar is shown
    composeTestRule.onNodeWithTag(EditProfileTestTags.DELETE_PHOTO_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.REMOTE_IMAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ProfilePhotoImageTestTags.DEFAULT_AVATAR).assertIsDisplayed()
  }

  @Test
  fun editProfile_deletePhotoButton_hidesDuringUpload() = runTest {
    val profileWithPhoto = testProfile.copy(photoUrl = "https://example.com/photo.jpg")
    val repo = FakeProfileRepository(profileWithPhoto, simulateUploadDelay = true)
    val vm = ProfileViewModel(repo)

    composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

    // Delete button is visible and enabled
    composeTestRule.onNodeWithTag(EditProfileTestTags.DELETE_PHOTO_BUTTON).assertIsEnabled()

    // Start upload
    vm.uploadProfilePhoto(
        context = ApplicationProvider.getApplicationContext(),
        imageUri = Uri.parse("content://test/image.jpg"),
        onSuccess = {},
        onError = {})

    composeTestRule.waitForIdle()

    // Delete button should now be hidden
    composeTestRule.onNodeWithTag(EditProfileTestTags.DELETE_PHOTO_BUTTON).assertDoesNotExist()
  }
}
