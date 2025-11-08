package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
class CreateEventForSerieScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** --- BASIC RENDERING --- */
  @Test
  fun allFieldsAndButtonAreDisplayed() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DURATION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_LOCATION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT)
        .assertIsDisplayed()
  }

  @Test
  fun titleIsDisplayedCorrectly() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    composeTestRule.onNodeWithText("Create Event for Serie").assertIsDisplayed()
  }

  /** --- INPUT BEHAVIOR --- */
  @Test
  fun emptyFieldsDisableSaveButton() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    // Initially all fields are empty, so save button should be disabled
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT)
        .assertIsNotEnabled()
  }

  @Test
  fun typeDropdownWorks() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    // Click on type dropdown
    composeTestRule.onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE).performClick()

    // Select SPORTS
    composeTestRule.onNodeWithText("SPORTS").assertIsDisplayed()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    // Verify selection
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE)
        .assertTextContains("SPORTS")
  }

  @Test
  fun textFieldsAcceptInput() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    val title = "Weekly Football Match"
    val desc = "Regular weekly game"
    val location = "EPFL Field"

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput(title)
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput(desc)
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_LOCATION)
        .performTextInput(location)

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .assertTextContains(title)
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertTextContains(desc)
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_LOCATION)
        .assertTextContains(location)
  }

  /** --- EDGE CASES --- */
  @Test
  fun emptyTitle_disablesSaveButton() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("")
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT)
        .assertIsNotEnabled()
  }

  @Test
  fun whitespaceInputsShouldBeTreatedAsEmpty() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("   ")
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("   ")
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT)
        .assertIsNotEnabled()
  }

  @Test
  fun switchingTypeRetainsOtherFields() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    val title = "Morning Run"
    val desc = "Light jog near EPFL"
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput(title)
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput(desc)

    // Select type SPORTS
    composeTestRule.onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    // Switch to SOCIAL
    composeTestRule.onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SOCIAL").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .assertTextContains(title)
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertTextContains(desc)
  }

  @Test
  fun saveButtonRemainsDisabledUntilAllMandatoryFieldsAreFilled() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    // Fill only some fields
    composeTestRule.onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Game")
    // Missing description, duration, and location -> must be disabled
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT)
        .assertIsNotEnabled()
  }

  @Test
  fun durationFieldDisplaysValue() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    // Duration field should be displayed (even if empty initially)
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DURATION)
        .assertIsDisplayed()
  }

  @Test
  fun partialFormFillKeepsSaveButtonDisabled() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    val saveButton =
        composeTestRule.onNodeWithTag(CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT)
    saveButton.assertIsNotEnabled()

    // Fill some but not all required fields
    composeTestRule.onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Run")
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("Morning jog")

    // Still missing duration and location, so button should remain disabled
    composeTestRule.waitForIdle()
    saveButton.assertIsNotEnabled()
  }

  @Test
  fun allEventTypesAreAvailable() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    composeTestRule.onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE).performClick()

    // Verify all three event types are available
    composeTestRule.onNodeWithText("SPORTS").assertIsDisplayed()
    composeTestRule.onNodeWithText("ACTIVITY").assertIsDisplayed()
    composeTestRule.onNodeWithText("SOCIAL").assertIsDisplayed()
  }

  @Test
  fun fieldsNotInheritedFromSerieAreNotDisplayed() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    // These fields should NOT be displayed as they are inherited from the serie
    composeTestRule
        .onNodeWithTag("inputEventMaxParticipants", useUnmergedTree = true)
        .assertDoesNotExist()
    composeTestRule.onNodeWithTag("inputEventDate", useUnmergedTree = true).assertDoesNotExist()
    composeTestRule.onNodeWithTag("inputEventTime", useUnmergedTree = true).assertDoesNotExist()
    composeTestRule
        .onNodeWithTag("inputEventVisibility", useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun locationFieldAcceptsInput() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    val location = "Rolex Learning Center"
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_LOCATION)
        .performTextInput(location)

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_LOCATION)
        .assertTextContains(location)
  }

  @Test
  fun durationPickerOpensOnClick() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    // Click on duration field to open picker
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DURATION)
        .performClick()

    // Verify the dialog appears with expected title
    composeTestRule.onNodeWithText("Select Duration (min)").assertIsDisplayed()
  }

  @Test
  fun buttonTextIsCorrect() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    // Verify the button text is "Create Event" and not "Save"
    composeTestRule.onNodeWithText("Create Event").assertIsDisplayed()
  }

  @Test
  fun serieIdIsPassedCorrectly() {
    val testSerieId = "unique-serie-id-123"
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = testSerieId, onDone = {}) }

    // The screen should render without crashing when receiving a serieId
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT)
        .assertExists()
  }

  @Test
  fun emptyDurationDisablesSaveButton() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    // Fill all fields except duration
    composeTestRule.onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SPORTS").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Soccer Game")
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("Weekly soccer match")
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_LOCATION)
        .performTextInput("EPFL Field")

    // Duration is empty (not selected), so button should be disabled
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT)
        .assertIsNotEnabled()
  }

  @Test
  fun formWithOnlyLocationMissingDisablesSaveButton() {
    composeTestRule.setContent { CreateEventForSerieScreen(serieId = "testSerieId", onDone = {}) }

    // Fill all fields except location (which requires selecting from suggestions)
    composeTestRule.onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("ACTIVITY").performClick()

    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Hiking Trip")
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("Morning hike")

    // Select duration via dialog
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.INPUT_EVENT_DURATION)
        .performClick()
    composeTestRule.onNodeWithText("OK").performClick()

    // Location not selected from suggestions, button should be disabled
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateEventForSerieScreenTestTags.BUTTON_SAVE_EVENT)
        .assertIsNotEnabled()
  }
}
