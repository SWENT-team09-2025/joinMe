package com.android.joinme.ui.overview

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test



@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class CreateEventScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** --- BASIC RENDERING --- */
  @Test
  fun allFieldsAndButtonAreDisplayed() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsDisplayed()
  }

  /** --- INPUT BEHAVIOR --- */
  @Test
  fun emptyFieldsDisableSaveButton() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Initially all fields are empty, so save button should be disabled
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsNotEnabled()
  }

  @Test
  fun typeDropdownWorks() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Click on type dropdown
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()

    // Select SPORTS
    composeTestRule.onNodeWithText("SPORTS").assertIsDisplayed()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    // Verify selection
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE)
        .assertTextContains("SPORTS")
  }

  @Test
  fun visibilityDropdownWorks() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Click on visibility dropdown
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY).performClick()

    // Select PUBLIC
    composeTestRule.onNodeWithText("PUBLIC").assertIsDisplayed()
    composeTestRule.onNodeWithText("PUBLIC").performClick()

    // Verify selection
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .assertTextContains("PUBLIC")
  }

  @Test
  fun textFieldsAcceptInput() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    val title = "Football Match"
    val desc = "Friendly game"
    val location = "EPFL Field"

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput(title)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput(desc)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
        .performTextInput(location)

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .assertTextContains(title)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertTextContains(desc)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
        .assertTextContains(location)
  }

  /** --- EDGE CASES --- */
  @Test
  fun emptyTitle_disablesSaveButton() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("")
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsNotEnabled()
  }

  @Test
  fun whitespaceInputsShouldBeTreatedAsEmpty() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("   ")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("   ")
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsNotEnabled()
  }

  @Test
  fun switchingTypeRetainsOtherFields() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    val title = "Morning Run"
    val desc = "Light jog near EPFL"
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput(title)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput(desc)

    // Select type SPORTS
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    // Switch to SOCIAL
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SOCIAL").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .assertTextContains(title)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertTextContains(desc)
  }

  @Test
  fun saveButtonRemainsDisabledUntilAllMandatoryFieldsAreFilled() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Fill only some fields
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Game")
    // Missing description, location, date, time, and visibility -> must be disabled
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsNotEnabled()
  }

  @Test
  fun switchingVisibilityUpdatesStateCorrectly() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY).performClick()
    composeTestRule.onNodeWithText("PRIVATE").performClick()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .assertTextContains("PRIVATE")

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY).performClick()
    composeTestRule.onNodeWithText("PUBLIC").performClick()
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .assertTextContains("PUBLIC")
  }

  @Test
  fun maxParticipantsFieldDisplaysValue() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Max participants field should be displayed (even if empty initially)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .assertIsDisplayed()
  }

  @Test
  fun durationFieldDisplaysValue() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Duration field should be displayed (even if empty initially)
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
        .assertIsDisplayed()
  }

  @Test
  fun dateFieldDisplaysValue() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    // Date field should be displayed (even if empty initially)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE).assertIsDisplayed()
  }

  @Test
  fun partialFormFillKeepsSaveButtonDisabled() {
    composeTestRule.setContent { CreateEventScreen(onDone = {}) }

    val saveButton = composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
    saveButton.assertIsNotEnabled()

    // Fill some but not all required fields
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Run")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("Morning jog")
    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
        .performTextInput("EPFL Track")

    // Still missing date, time, and visibility, so button should remain disabled
    composeTestRule.waitForIdle()
    saveButton.assertIsNotEnabled()
  }
}
