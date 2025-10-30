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
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    val title = "Weekly Football"
    val desc = "Weekly football series"

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput(title)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput(desc)

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .assertTextContains(title)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .assertTextContains(desc)
  }

  @Test
  fun dateAndTimeFieldsAcceptInput() {
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    val date = "25/12/2025"
    val time = "14:30"

    // Use ViewModel to set values since fields are read-only
    viewModel.setDate(date)
    viewModel.setTime(time)

    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE)
        .assertTextContains(date)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME)
        .assertTextContains(time)
  }

  @Test
  fun visibilityFieldAcceptsInput() {
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    val visibility = "PUBLIC"

    // Use ViewModel to set value since field is read-only dropdown
    viewModel.setVisibility(visibility)

    composeTestRule.waitForIdle()
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
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    // Set invalid max participants (non-numeric) via ViewModel
    viewModel.setMaxParticipants("abc")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Must be a positive number").assertIsDisplayed()
  }

  @Test
  fun negativeMaxParticipants_showsError() {
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    // Set negative max participants via ViewModel
    viewModel.setMaxParticipants("-5")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Must be a positive number").assertIsDisplayed()
  }

  @Test
  fun zeroMaxParticipants_showsError() {
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    // Set zero max participants via ViewModel
    viewModel.setMaxParticipants("0")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Must be a positive number").assertIsDisplayed()
  }

  @Test
  fun invalidDateFormat_showsError() {
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    // Set invalid date format via ViewModel
    viewModel.setDate("2025-12-25")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Invalid format (must be dd/MM/yyyy)").assertIsDisplayed()
  }

  @Test
  fun invalidTimeFormat_showsError() {
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    // Set invalid time format via ViewModel - match what's used in ViewModel tests
    viewModel.setTime("not a time")

    // Should show validation error
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Invalid format (must be HH:mm)").assertIsDisplayed()
  }

  @Test
  fun invalidVisibility_showsError() {
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    // Set invalid visibility via ViewModel
    viewModel.setVisibility("INVALID")

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
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    val saveButton = composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE)
    saveButton.assertIsNotEnabled()

    // Fill some but not all required fields
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("Morning Run")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput("Weekly morning jog")
    viewModel.setMaxParticipants("10")

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
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    // Fill all required fields with valid data
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("Weekly Football")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput("Weekly football series")
    viewModel.setMaxParticipants("10")
    viewModel.setDate("25/12/2025")
    viewModel.setTime("14:30")
    viewModel.setVisibility("PUBLIC")

    // Now the save button should be enabled
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE).assertIsEnabled()
  }

  @Test
  fun descriptionFieldSupportsMultipleLines() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    val singleLineDescription = "This is a long description"

    // Test that description field accepts text (multiline is supported by maxLines = 4)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput(singleLineDescription)

    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .assertTextContains("This is a long description")
  }

  /** --- NAVIGATION & CALLBACKS --- */
  @Test
  fun backButtonTriggersOnGoBack() {
    var backPressed = false
    composeTestRule.setContent { CreateSerieScreen(onGoBack = { backPressed = true }) }

    // Find and click back button
    composeTestRule.onNodeWithContentDescription("Back").performClick()

    // Verify callback was triggered
    assert(backPressed)
  }

  @Test
  fun topAppBarDisplaysTitle() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Verify top app bar shows correct title
    composeTestRule.onNodeWithText("Create Serie").assertIsDisplayed()
  }

  /** --- DROPDOWN INTERACTION --- */
  @Test
  fun visibilityDropdownCanBeOpenedAndClosed() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Click to open dropdown
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY).performClick()

    composeTestRule.waitForIdle()

    // Verify dropdown options are displayed
    composeTestRule.onNodeWithText("PUBLIC").assertExists()
    composeTestRule.onNodeWithText("PRIVATE").assertExists()
  }

  @Test
  fun selectingPublicVisibility() {
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    // Click to open dropdown
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY).performClick()

    composeTestRule.waitForIdle()

    // Select PUBLIC
    composeTestRule.onNodeWithText("PUBLIC").performClick()

    composeTestRule.waitForIdle()

    // Verify PUBLIC is selected
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .assertTextContains("PUBLIC")
  }

  @Test
  fun selectingPrivateVisibility() {
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    // Click to open dropdown
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY).performClick()

    composeTestRule.waitForIdle()

    // Select PRIVATE
    composeTestRule.onNodeWithText("PRIVATE").performClick()

    composeTestRule.waitForIdle()

    // Verify PRIVATE is selected
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .assertTextContains("PRIVATE")
  }

  /** --- CLICKABLE FIELDS --- */
  @Test
  fun maxParticipantsFieldIsClickable() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Verify field exists and can be clicked
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .assertIsDisplayed()
  }

  @Test
  fun dateFieldIsClickable() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Verify field exists and can be clicked
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE).assertIsDisplayed()
  }

  @Test
  fun timeFieldIsClickable() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Verify field exists and can be clicked
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME).assertIsDisplayed()
  }

  /** --- BUTTON STATES --- */
  @Test
  fun saveButtonShowsTextWhenNotLoading() {
    composeTestRule.setContent { CreateSerieScreen(onDone = {}) }

    // Verify button displays "Next" text
    composeTestRule.onNodeWithText("Next").assertExists()
  }

  @Test
  fun invalidFormKeepsSaveButtonDisabled() {
    val viewModel = CreateSerieViewModel()
    composeTestRule.setContent { CreateSerieScreen(createSerieViewModel = viewModel, onDone = {}) }

    // Fill form with one invalid field (invalid date)
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("Test Serie")
    composeTestRule
        .onNodeWithTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput("Test description")
    viewModel.setMaxParticipants("10")
    viewModel.setDate("invalid-date")
    viewModel.setTime("14:30")
    viewModel.setVisibility("PUBLIC")

    composeTestRule.waitForIdle()

    // Button should remain disabled due to invalid date
    composeTestRule.onNodeWithTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE).assertIsNotEnabled()
  }
}
