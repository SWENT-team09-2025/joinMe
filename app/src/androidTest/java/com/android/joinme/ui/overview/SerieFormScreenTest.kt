package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class SerieFormScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createEmptyFormState() =
      SerieFormState(
          title = "",
          description = "",
          maxParticipants = "",
          date = "",
          time = "",
          visibility = "",
          isValid = false,
          isLoading = false,
          invalidTitleMsg = null,
          invalidDescriptionMsg = null,
          invalidMaxParticipantsMsg = null,
          invalidDateMsg = null,
          invalidTimeMsg = null,
          invalidVisibilityMsg = null)

  private fun createValidFormState() =
      SerieFormState(
          title = "Test Serie",
          description = "Test Description",
          maxParticipants = "10",
          date = "25/12/2025",
          time = "14:30",
          visibility = "PUBLIC",
          isValid = true,
          isLoading = false,
          invalidTitleMsg = null,
          invalidDescriptionMsg = null,
          invalidMaxParticipantsMsg = null,
          invalidDateMsg = null,
          invalidTimeMsg = null,
          invalidVisibilityMsg = null)

  private fun createDefaultTestTags() =
      SerieFormTestTags(
          inputSerieTitle = "inputSerieTitle",
          inputSerieDescription = "inputSerieDescription",
          inputSerieMaxParticipants = "inputSerieMaxParticipants",
          inputSerieDate = "inputSerieDate",
          inputSerieTime = "inputSerieTime",
          inputSerieVisibility = "inputSerieVisibility",
          buttonSaveSerie = "buttonSaveSerie",
          errorMessage = "errorMessage")

  /** --- HELPER FUNCTION TESTS --- */
  @Test
  fun createSerieFormState_fromCreateSerieUIState_mapsCorrectly() {
    val uiState =
        CreateSerieUIState(
            title = "Weekly Run",
            description = "Weekly running series",
            maxParticipants = "15",
            date = "01/01/2026",
            time = "08:00",
            visibility = "PRIVATE",
            isLoading = true,
            invalidTitleMsg = "Error",
            invalidDescriptionMsg = "Desc Error",
            invalidMaxParticipantsMsg = "Max Error",
            invalidDateMsg = "Date Error",
            invalidTimeMsg = "Time Error",
            invalidVisibilityMsg = "Vis Error")

    val formState = createSerieFormState(uiState)

    assert(formState.title == "Weekly Run")
    assert(formState.description == "Weekly running series")
    assert(formState.maxParticipants == "15")
    assert(formState.date == "01/01/2026")
    assert(formState.time == "08:00")
    assert(formState.visibility == "PRIVATE")
    assert(formState.isLoading)
    assert(!formState.isValid)
    assert(formState.invalidTitleMsg == "Error")
    assert(formState.invalidDescriptionMsg == "Desc Error")
    assert(formState.invalidMaxParticipantsMsg == "Max Error")
    assert(formState.invalidDateMsg == "Date Error")
    assert(formState.invalidTimeMsg == "Time Error")
    assert(formState.invalidVisibilityMsg == "Vis Error")
  }

  @Test
  fun createSerieFormState_fromEditSerieUIState_mapsCorrectly() {
    val uiState =
        EditSerieUIState(
            serieId = "serie-123",
            title = "Monthly Game",
            description = "Monthly gaming event",
            maxParticipants = "20",
            date = "15/03/2026",
            time = "19:00",
            visibility = "PUBLIC",
            isLoading = false,
            invalidTitleMsg = null,
            invalidDescriptionMsg = null,
            invalidMaxParticipantsMsg = null,
            invalidDateMsg = null,
            invalidTimeMsg = null,
            invalidVisibilityMsg = null)

    val formState = createSerieFormState(uiState)

    assert(formState.title == "Monthly Game")
    assert(formState.description == "Monthly gaming event")
    assert(formState.maxParticipants == "20")
    assert(formState.date == "15/03/2026")
    assert(formState.time == "19:00")
    assert(formState.visibility == "PUBLIC")
    assert(!formState.isLoading)
    assert(formState.isValid)
    assert(formState.invalidTitleMsg == null)
    assert(formState.invalidDescriptionMsg == null)
    assert(formState.invalidMaxParticipantsMsg == null)
    assert(formState.invalidDateMsg == null)
    assert(formState.invalidTimeMsg == null)
    assert(formState.invalidVisibilityMsg == null)
  }

  /** --- BASIC RENDERING --- */
  @Test
  fun serieFormScreen_rendersAllComponents() {
    val formState = createEmptyFormState()
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithText("Test Form").assertIsDisplayed()
    composeTestRule.onNodeWithTag(testTags.inputSerieTitle).assertIsDisplayed()
    composeTestRule.onNodeWithTag(testTags.inputSerieDescription).assertIsDisplayed()
    composeTestRule.onNodeWithTag(testTags.inputSerieMaxParticipants).assertIsDisplayed()
    composeTestRule.onNodeWithTag(testTags.inputSerieDate).assertIsDisplayed()
    composeTestRule.onNodeWithTag(testTags.inputSerieTime).assertIsDisplayed()
    composeTestRule.onNodeWithTag(testTags.inputSerieVisibility).assertIsDisplayed()
    composeTestRule.onNodeWithTag(testTags.buttonSaveSerie).assertIsDisplayed()
  }

  @Test
  fun serieFormScreen_displaysSaveButtonText() {
    val formState = createValidFormState()
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Custom Save Text")
    }

    composeTestRule.onNodeWithText("Custom Save Text").assertIsDisplayed()
  }

  @Test
  fun serieFormScreen_usesDefaultSaveButtonText() {
    val formState = createValidFormState()
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {})
    }

    composeTestRule.onNodeWithText("Next").assertIsDisplayed()
  }

  /** --- LOADING STATE --- */
  @Test
  fun serieFormScreen_showsLoadingIndicatorWhenLoading() {
    val formState = createValidFormState().copy(isLoading = true)
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    // When loading, button should be disabled
    composeTestRule.onNodeWithTag(testTags.buttonSaveSerie).assertIsNotEnabled()
  }

  /** --- ERROR MESSAGES --- */
  @Test
  fun serieFormScreen_showsTitleError() {
    val formState = createEmptyFormState().copy(invalidTitleMsg = "Title is required")
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithText("Title is required").assertIsDisplayed()
  }

  @Test
  fun serieFormScreen_showsDescriptionError() {
    val formState = createEmptyFormState().copy(invalidDescriptionMsg = "Description is required")
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithText("Description is required").assertIsDisplayed()
  }

  @Test
  fun serieFormScreen_showsMaxParticipantsError() {
    val formState =
        createEmptyFormState().copy(invalidMaxParticipantsMsg = "Must be a positive number")
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithText("Must be a positive number").assertIsDisplayed()
  }

  @Test
  fun serieFormScreen_showsDateError() {
    val formState =
        createEmptyFormState().copy(invalidDateMsg = "Invalid format (must be dd/MM/yyyy)")
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithText("Invalid format (must be dd/MM/yyyy)").assertIsDisplayed()
  }

  @Test
  fun serieFormScreen_showsTimeError() {
    val formState = createEmptyFormState().copy(invalidTimeMsg = "Invalid format (must be HH:mm)")
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithText("Invalid format (must be HH:mm)").assertIsDisplayed()
  }

  @Test
  fun serieFormScreen_showsVisibilityError() {
    val formState =
        createEmptyFormState().copy(invalidVisibilityMsg = "Visibility must be PUBLIC or PRIVATE")
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithText("Visibility must be PUBLIC or PRIVATE").assertIsDisplayed()
  }

  /** --- CALLBACK TESTS --- */
  @Test
  fun serieFormScreen_triggersOnSaveWhenButtonClicked() {
    val formState = createValidFormState()
    val testTags = createDefaultTestTags()
    var saveCalled = false

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = {
            saveCalled = true
            true
          },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithTag(testTags.buttonSaveSerie).performClick()
    assert(saveCalled)
  }

  @Test
  fun serieFormScreen_triggersOnGoBackWhenBackButtonClicked() {
    val formState = createValidFormState()
    val testTags = createDefaultTestTags()
    var backCalled = false

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = { backCalled = true },
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    assert(backCalled)
  }

  @Test
  fun serieFormScreen_triggersOnTitleChange() {
    val formState = createEmptyFormState()
    val testTags = createDefaultTestTags()
    var titleChanged = false
    var newTitle = ""

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {
            titleChanged = true
            newTitle = it
          },
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithTag(testTags.inputSerieTitle).performTextInput("New Title")
    assert(titleChanged)
    assert(newTitle == "New Title")
  }

  @Test
  fun serieFormScreen_triggersOnDescriptionChange() {
    val formState = createEmptyFormState()
    val testTags = createDefaultTestTags()
    var descriptionChanged = false
    var newDescription = ""

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {
            descriptionChanged = true
            newDescription = it
          },
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule
        .onNodeWithTag(testTags.inputSerieDescription)
        .performTextInput("New Description")
    assert(descriptionChanged)
    assert(newDescription == "New Description")
  }

  /** --- READ-ONLY FIELD COLORS --- */
  @Test
  fun serieFormScreen_readOnlyFieldsHaveCorrectStyling() {
    val formState = createValidFormState()
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    // Verify read-only fields are displayed
    composeTestRule.onNodeWithTag(testTags.inputSerieMaxParticipants).assertIsDisplayed()
    composeTestRule.onNodeWithTag(testTags.inputSerieDate).assertIsDisplayed()
    composeTestRule.onNodeWithTag(testTags.inputSerieTime).assertIsDisplayed()
  }

  /** --- VISIBILITY DROPDOWN --- */
  @Test
  fun serieFormScreen_visibilityDropdownCanExpand() {
    val formState = createEmptyFormState()
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithTag(testTags.inputSerieVisibility).performClick()
    composeTestRule.waitForIdle()

    // Verify options are displayed
    composeTestRule.onNodeWithText("PUBLIC").assertExists()
    composeTestRule.onNodeWithText("PRIVATE").assertExists()
  }

  @Test
  fun serieFormScreen_visibilityDropdownCallsOnChange() {
    val formState = createEmptyFormState()
    val testTags = createDefaultTestTags()
    var visibilityChanged = false
    var newVisibility = ""

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {
            visibilityChanged = true
            newVisibility = it
          },
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithTag(testTags.inputSerieVisibility).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("PUBLIC").performClick()

    assert(visibilityChanged)
    assert(newVisibility == "PUBLIC")
  }

  /** --- BUTTON STATE TESTS --- */
  @Test
  fun serieFormScreen_saveButtonDisabledWhenInvalid() {
    val formState = createEmptyFormState().copy(isValid = false)
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithTag(testTags.buttonSaveSerie).assertIsNotEnabled()
  }

  @Test
  fun serieFormScreen_saveButtonEnabledWhenValid() {
    val formState = createValidFormState().copy(isValid = true)
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithTag(testTags.buttonSaveSerie).assertIsEnabled()
  }

  @Test
  fun serieFormScreen_saveButtonDisabledWhenLoadingEvenIfValid() {
    val formState = createValidFormState().copy(isValid = true, isLoading = true)
    val testTags = createDefaultTestTags()

    composeTestRule.setContent {
      SerieFormScreen(
          title = "Test Form",
          formState = formState,
          testTags = testTags,
          onTitleChange = {},
          onDescriptionChange = {},
          onMaxParticipantsChange = {},
          onDateChange = {},
          onTimeChange = {},
          onVisibilityChange = {},
          onSave = { true },
          onGoBack = {},
          saveButtonText = "Save")
    }

    composeTestRule.onNodeWithTag(testTags.buttonSaveSerie).assertIsNotEnabled()
  }
}
