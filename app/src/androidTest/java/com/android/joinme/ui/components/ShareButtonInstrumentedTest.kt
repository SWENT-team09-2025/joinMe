package com.android.joinme.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.joinme.model.invitation.InvitationType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for ShareButton, which will execute on an Android device.
 *
 * This test covers the success path that is difficult to test with unit tests due to Android
 * framework dependencies.
 */
@RunWith(AndroidJUnit4::class)
class ShareButtonInstrumentedTest {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Test that clicking the share button doesn't crash the app. This covers the success path where
   * the button is clicked and the share sheet would normally open (we can't easily test the actual
   * sharing without user interaction).
   */
  @Test
  fun shareButton_click_doesNotCrash() {
    composeTestRule.setContent {
      ShareButton(
          invitationType = InvitationType.GROUP,
          targetId = "test-group-id",
          createdBy = "test-user-id",
          expiresInDays = 7.0)
    }

    // Verify button exists and is clickable
    composeTestRule.onNodeWithTag(ShareButtonTestTags.SHARE_BUTTON).assertExists()

    // Note: We can't easily test the actual share sheet opening in automated tests,
    // but this at least verifies the button click doesn't crash
    // The success path (creating invitation and opening share sheet) is covered
    // by the fact that no exception is thrown
    composeTestRule.onNodeWithTag(ShareButtonTestTags.SHARE_BUTTON).performClick()
  }
}
