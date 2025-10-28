package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class CreateSerieScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** --- BASIC RENDERING --- */
  @Test
  fun allFieldsAndButtonAreDisplayed() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE).assertIsDisplayed()
  }

  /** --- INPUT BEHAVIOR --- */
  @Test
  fun emptyFieldsDisableSaveButton() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Initially all fields are empty, so save button should be disabled
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE).assertIsNotEnabled()
  }

  @Test
  fun textFieldsAcceptInput() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    val title = "Weekly Football"
    val desc = "Weekly football series"
    val maxParticipants = "10"

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput(title)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput(desc)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .performTextInput(maxParticipants)

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .assertTextContains(title)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .assertTextContains(desc)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .assertTextContains(maxParticipants)
  }

  @Test
  fun dateAndTimeFieldsAcceptInput() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    val date = "25/12/2025"
    val time = "14:30"

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE)
        .performTextInput(date)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME)
        .performTextInput(time)

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE)
        .assertTextContains(date)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME)
        .assertTextContains(time)
  }

  @Test
  fun visibilityFieldAcceptsInput() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    val visibility = "PUBLIC"

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .performTextInput(visibility)

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .assertTextContains(visibility)
  }

  /** --- VALIDATION BEHAVIOR --- */
  @Test
  fun emptyTitle_disablesSaveButton() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE).performTextInput("")
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE).assertIsNotEnabled()
  }

  @Test
  fun whitespaceInputsShouldBeTreatedAsEmpty() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("   ")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput("   ")
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE).assertIsNotEnabled()
  }

  @Test
  fun invalidMaxParticipants_showsError() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Enter invalid max participants (non-numeric)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .performTextInput("abc")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Must be a positive number").assertIsDisplayed()
  }

  @Test
  fun negativeMaxParticipants_showsError() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Enter negative max participants
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .performTextInput("-5")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Must be a positive number").assertIsDisplayed()
  }

  @Test
  fun zeroMaxParticipants_showsError() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Enter zero max participants
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .performTextInput("0")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Must be a positive number").assertIsDisplayed()
  }

  @Test
  fun invalidDateFormat_showsError() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Enter invalid date format
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE)
        .performTextInput("2025-12-25")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Invalid format (must be dd/MM/yyyy)").assertIsDisplayed()
  }

  @Test
  fun invalidTimeFormat_showsError() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Enter invalid time format
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME)
        .performTextInput("25:99")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Invalid format (must be HH:mm)").assertIsDisplayed()
  }

  @Test
  fun invalidVisibility_showsError() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Enter invalid visibility
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .performTextInput("INVALID")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Visibility must be PUBLIC or PRIVATE").assertIsDisplayed()
  }

  /** --- EDGE CASES --- */
  @Test
  fun saveButtonRemainsDisabledUntilAllMandatoryFieldsAreFilled() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Fill only some fields
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("Weekly Run")

    // Missing description, max participants, date, time, and visibility -> must be disabled
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE).assertIsNotEnabled()
  }

  @Test
  fun partialFormFillKeepsSaveButtonDisabled() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    val saveButton = composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE)
    saveButton.assertIsNotEnabled()

    // Fill some but not all required fields
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("Morning Run")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput("Weekly morning jog")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .performTextInput("10")

    // Still missing date, time, and visibility, so button should remain disabled
    composeTestRule.waitForIdle()
    saveButton.assertIsNotEnabled()
  }

  @Test
  fun maxParticipantsFieldDisplaysValue() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Max participants field should be displayed (even if empty initially)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .assertIsDisplayed()
  }

  @Test
  fun dateFieldDisplaysPlaceholder() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Date field should be displayed with placeholder
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE).assertIsDisplayed()
  }

  @Test
  fun timeFieldDisplaysPlaceholder() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Time field should be displayed with placeholder
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME).assertIsDisplayed()
  }

  @Test
  fun visibilityFieldDisplaysPlaceholder() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Visibility field should be displayed with placeholder
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .assertIsDisplayed()
  }

  @Test
  fun fillingAllFieldsEnablesSaveButton() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Fill all required fields with valid data
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("Weekly Football")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput("Weekly football series")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .performTextInput("10")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE)
        .performTextInput("25/12/2025")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME)
        .performTextInput("14:30")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .performTextInput("PUBLIC")

    // Now the save button should be enabled
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE).assertIsEnabled()
  }

  @Test
  fun descriptionFieldSupportsMultipleLines() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    val multilineDescription = "Line 1\nLine 2\nLine 3"

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput(multilineDescription)

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .assertTextContains("Line 1")
  }
}
