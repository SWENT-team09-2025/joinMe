package com.android.joinme.ui.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.ui.navigation.NavigationTestTags
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Comprehensive tests for EditProfileScreen using a tiny in-memory FakeProfileRepository. */
class EditProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // --- Test data ---
    private val testUid = "test-uid"
    private val testProfile = Profile(
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
        composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.PASSWORD_SECTION)
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

    @Test
    fun editProfile_bottomNavigationIsDisplayed() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
            .assertIsDisplayed()
    }

    // ==================== INITIAL VALUES TESTS ====================

    @Test
    fun editProfile_populatesInitialValues() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
            .assertTextContains("Max Verstappen", substring = true)
        composeTestRule.onNodeWithTag(EditProfileTestTags.EMAIL_FIELD)
            .assertTextContains("speed@f1.com", substring = true)
        composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
            .assertTextContains("30/09/1997", substring = true)

        composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.COUNTRY_FIELD)
        composeTestRule.onNodeWithTag(EditProfileTestTags.COUNTRY_FIELD)
            .assertTextContains("Netherlands", substring = true)

        composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.INTERESTS_FIELD)
        composeTestRule.onNodeWithTag(EditProfileTestTags.INTERESTS_FIELD)
            .assertTextContains("Racing, Cars, Technology", substring = true)

        composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.BIO_FIELD)
        composeTestRule.onNodeWithTag(EditProfileTestTags.BIO_FIELD)
            .assertTextContains("need for speed", substring = true)
    }

    @Test
    fun editProfile_populatesEmptyFieldsCorrectly() = runTest {
        val emptyProfile = testProfile.copy(
            dateOfBirth = null,
            country = null,
            interests = emptyList(),
            bio = null
        )
        val vm = ProfileViewModel(FakeProfileRepository(emptyProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
            .assertTextContains("", substring = false)

        composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.COUNTRY_FIELD)
        composeTestRule.onNodeWithTag(EditProfileTestTags.COUNTRY_FIELD)
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
        val interests = composeTestRule.onNodeWithTag(EditProfileTestTags.INTERESTS_FIELD)
        interests.performTextClearance()
        interests.performTextInput("Coding, Testing, Android")

        interests.assertTextContains("Coding, Testing, Android", substring = false)
    }

    @Test
    fun editProfile_bio_isEditable() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.BIO_FIELD)
        val bio = composeTestRule.onNodeWithTag(EditProfileTestTags.BIO_FIELD)
        bio.performTextClearance()
        bio.performTextInput("World champion. Loves sim racing and pad thai.")

        bio.assertTextContains("World champion. Loves sim racing and pad thai.", substring = false)
    }

    // ==================== USERNAME VALIDATION TESTS ====================

    @Test
    fun editProfile_username_tooShort_showsError() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
        node.performTextClearance()
        node.performTextInput("ab")

        composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithText("Username must be at least 3 characters")
            .assertIsDisplayed()
    }

    @Test
    fun editProfile_username_tooLong_showsError() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
        node.performTextClearance()
        node.performTextInput("a".repeat(31))

        composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithText("Username must not exceed 30 characters")
            .assertIsDisplayed()
    }

    @Test
    fun editProfile_username_invalidCharacters_showsError() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
        node.performTextClearance()
        node.performTextInput("user@name!")

        composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithText("Only letters, numbers, spaces, and underscores allowed")
            .assertIsDisplayed()
    }

    @Test
    fun editProfile_username_empty_showsError() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
        node.performTextClearance()
        node.performTextInput("")

        composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithText("Username is required").assertIsDisplayed()
    }

    @Test
    fun editProfile_username_validWithSpaces() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
        node.performTextClearance()
        node.performTextInput("Max Verstappen")

        composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertDoesNotExist()
    }

    @Test
    fun editProfile_username_validWithUnderscores() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
        node.performTextClearance()
        node.performTextInput("max_verstappen_33")

        composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertDoesNotExist()
    }

    @Test
    fun editProfile_username_validWithNumbers() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
        node.performTextClearance()
        node.performTextInput("user1234")

        composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertDoesNotExist()
    }

    @Test
    fun editProfile_username_exactly3Characters_isValid() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
        node.performTextClearance()
        node.performTextInput("abc")

        composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertDoesNotExist()
    }

    @Test
    fun editProfile_username_exactly30Characters_isValid() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_FIELD)
        node.performTextClearance()
        node.performTextInput("a".repeat(30))

        composeTestRule.onNodeWithTag(EditProfileTestTags.USERNAME_ERROR).assertDoesNotExist()
    }

    // ==================== DATE VALIDATION TESTS ====================

    @Test
    fun editProfile_date_invalidFormat_showsError() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
        node.performTextClearance()
        node.performTextInput("2000-01-01")

        composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter your date in dd/mm/yyyy format.")
            .assertIsDisplayed()
    }

    @Test
    fun editProfile_date_invalidDay_showsError() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
        node.performTextClearance()
        node.performTextInput("32/01/2000")

        composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithText("Invalid date").assertIsDisplayed()
    }

    @Test
    fun editProfile_date_invalidMonth_showsError() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
        node.performTextClearance()
        node.performTextInput("01/13/2000")

        composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithText("Invalid date").assertIsDisplayed()
    }

    @Test
    fun editProfile_date_validFormat() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        val node = composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_FIELD)
        node.performTextClearance()
        node.performTextInput("15/06/1995")

        composeTestRule.onNodeWithTag(EditProfileTestTags.DATE_OF_BIRTH_ERROR).assertDoesNotExist()
    }

    @Test
    fun editProfile_date_emptyIsValid() = runTest {
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

    // ==================== PASSWORD SECTION TESTS ====================

    @Test
    fun editProfile_passwordSection_isDisplayed() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.PASSWORD_SECTION)
        composeTestRule.onNodeWithTag(EditProfileTestTags.CHANGE_PASSWORD_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun editProfile_changePasswordButton_isClickable() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        var clicked = false
        composeTestRule.setContent {
            EditProfileScreen(
                uid = testUid,
                profileViewModel = vm,
                onChangePasswordClick = { clicked = true }
            )
        }

        composeTestRule.scrollIntoViewAndAssert(EditProfileTestTags.CHANGE_PASSWORD_BUTTON)
        composeTestRule.onNodeWithTag(EditProfileTestTags.CHANGE_PASSWORD_BUTTON).performClick()

        assert(clicked)
    }

    // ==================== PHOTO EDIT BUTTON TESTS ====================

    @Test
    fun editProfile_editPhotoButton_isClickable() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        var clicked = false
        composeTestRule.setContent { EditProfileScreen(uid = testUid, profileViewModel = vm) }

        composeTestRule.onNodeWithTag(EditProfileTestTags.EDIT_PHOTO_BUTTON).performClick()
        // Currently just a placeholder, but we can verify it doesn't crash
        assert(true)
    }

    // ==================== NAVIGATION TESTS ====================

    @Test
    fun editProfile_backButton_isClickable() = runTest {
        val vm = ProfileViewModel(FakeProfileRepository(testProfile))
        var backClicked = false
        composeTestRule.setContent {
            EditProfileScreen(
                uid = testUid,
                profileViewModel = vm,
                onBackClick = { backClicked = true }
            )
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
                uid = testUid,
                profileViewModel = vm,
                onProfileClick = { profileClicked = true }
            )
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
                uid = testUid,
                profileViewModel = vm,
                onGroupClick = { groupClicked = true }
            )
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
}