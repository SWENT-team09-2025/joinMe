package com.android.joinme.ui.profile

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Tests for ViewProfileScreen that require Android instrumentation. */
class ViewProfileScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

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

    override suspend fun uploadProfilePhoto(context: Context, uid: String, imageUri: Uri): String {
      TODO("Not yet implemented")
    }

    override suspend fun deleteProfilePhoto(uid: String) {
      TODO("Not yet implemented")
    }

    override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? {
      if (uids.isEmpty()) return emptyList()
      val results = uids.mapNotNull { uid -> stored?.takeIf { it.uid == uid } }
      return if (results.size == uids.size) results else null
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

  @Test
  fun viewProfileScreen_displaysAllFields() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
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
  fun viewProfileScreen_displaysCorrectDateOfBirth() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("30/09/1997").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysAllFieldLabels() = runTest {
    val repo = FakeProfileRepository(createTestProfile())
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Username").assertIsDisplayed()
    composeTestRule.onNodeWithText("Email").assertIsDisplayed()
    composeTestRule.onNodeWithText("Date of Birth").assertIsDisplayed()

    scrollAndAssertText("Country/Region")
    scrollAndAssertText("Interests")
    scrollAndAssertText("Bio")
  }

  @Test
  fun viewProfileScreen_displaysNotSpecified_forNullDateOfBirth() = runTest {
    val profileWithNullDate = createTestProfile().copy(dateOfBirth = null)
    val repo = FakeProfileRepository(profileWithNullDate)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Date not specified").assertIsDisplayed()
  }

  @Test
  fun viewProfileScreen_displaysAllNotSpecified_forAllNullFields() = runTest {
    val profileWithNulls =
        createTestProfile()
            .copy(dateOfBirth = null, country = null, bio = null, interests = emptyList())
    val repo = FakeProfileRepository(profileWithNulls)
    val viewModel = ProfileViewModel(repo)

    composeTestRule.setContent { ViewProfileScreen(uid = testUid, profileViewModel = viewModel) }

    composeTestRule.onNodeWithText("Date not specified").assertIsDisplayed()
    scrollAndAssertText("Country not specified")
    scrollAndAssertText("None")
    scrollAndAssertText("No bio available")
  }
}
