package com.android.joinme.ui.overview

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepositoryLocal
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditSerieScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createTestSerie(): Serie {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.DECEMBER, 25, 14, 30, 0)

    return Serie(
        serieId = "test-serie-1",
        title = "Weekly Football",
        description = "Weekly football series",
        date = Timestamp(calendar.time),
        participants = listOf("owner123"),
        maxParticipants = 10,
        visibility = Visibility.PUBLIC,
        eventIds = emptyList(),
        ownerId = "owner123")
  }

  /** --- BASIC RENDERING --- */
  @Test
  fun allFieldsAreDisplayed() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TIME).assertIsDisplayed()
    // Elements below the fold need to be checked with assertExists()
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_VISIBILITY).assertExists()
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE).assertExists()
  }

  @Test
  fun serieDataIsLoadedAndDisplayed() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify loaded data
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE)
        .assertTextContains("Weekly Football")
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .assertTextContains("Weekly football series")
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .assertTextContains("10")
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DATE)
        .assertTextContains("25/12/2025")
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TIME)
        .assertTextContains("14:30")
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .assertTextContains("PUBLIC")
  }

  /** --- EDITING FUNCTIONALITY --- */
  @Test
  fun editingTitle_updatesUIState() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("Updated Title")

    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE)
        .assertTextContains("Updated Title")
  }

  @Test
  fun editingDescription_updatesUIState() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput("New description text")

    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .assertTextContains("New description text")
  }

  @Test
  fun maxParticipantsFieldDisplaysLoadedValue() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify field displays the loaded value
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .assertTextContains("10")
  }

  /** --- VALIDATION --- */
  @Test
  fun clearingTitle_showsErrorAndDisablesSaveButton() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performTextClearance()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertExists()
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE).assertIsNotEnabled()
  }

  @Test
  fun clearingDescription_showsErrorAndDisablesSaveButton() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextClearance()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertExists()
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE).assertIsNotEnabled()
  }

  @Test
  fun correctingInvalidField_removesErrorAndEnablesSaveButton() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Make field invalid
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performTextClearance()

    composeTestRule.waitForIdle()

    // Verify error is shown
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertExists()

    // Fix the field
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("Valid Title")

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditSerieScreenTestTags.SERIE_SAVE)
          .fetchSemanticsNodes()
          .firstOrNull()
          ?.config
          ?.getOrNull(SemanticsProperties.Disabled) == null
    }

    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE).assertIsEnabled()
  }

  /** --- SAVE FUNCTIONALITY --- */
  @Test
  fun clickingSaveWithValidData_callsOnDone() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    var saveCalled = false

    composeTestRule.setContent {
      EditSerieScreen(
          serieId = serie.serieId, editSerieViewModel = viewModel, onDone = { saveCalled = true })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Wait for button to be enabled
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditSerieScreenTestTags.SERIE_SAVE)
          .fetchSemanticsNodes()
          .firstOrNull()
          ?.config
          ?.getOrNull(SemanticsProperties.Disabled) == null
    }

    // Click save (scroll to it first as it may be below the fold)
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)

    // Assert callback called
    assert(saveCalled)
  }

  @Test
  fun savingEdits_persistsChanges() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Edit title
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("Updated Football Series")

    // Wait for button to be enabled
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditSerieScreenTestTags.SERIE_SAVE)
          .fetchSemanticsNodes()
          .firstOrNull()
          ?.config
          ?.getOrNull(SemanticsProperties.Disabled) == null
    }

    // Save (scroll to button first as it may be below the fold)
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify changes persisted
    runBlocking {
      val updatedSerie = repo.getSerie(serie.serieId)
      assert(updatedSerie.title == "Updated Football Series")
    }
  }

  @Test
  fun clickingSaveWithInvalidData_doesNotCallOnDone() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    var saveCalled = false

    composeTestRule.setContent {
      EditSerieScreen(
          serieId = serie.serieId, editSerieViewModel = viewModel, onDone = { saveCalled = true })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Make data invalid
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performTextClearance()

    composeTestRule.waitForIdle()

    // Button should be disabled, but try to verify click doesn't work
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE).assertIsNotEnabled()

    // Callback should not be called
    assert(!saveCalled)
  }

  /** --- VIEWMODEL TESTS --- */
  @Test
  fun viewModel_loadSerie_updatesUIState() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    // Initially empty
    assert(viewModel.uiState.value.title.isEmpty())

    // Load serie
    runBlocking { viewModel.loadSerie(serie.serieId) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify loaded
    assert(viewModel.uiState.value.title == "Weekly Football")
    assert(viewModel.uiState.value.maxParticipants == "10")
  }

  @Test
  fun viewModel_settersUpdateState() {
    val repo = SeriesRepositoryLocal()
    val viewModel = EditSerieViewModel(repo)

    viewModel.setTitle("New Title")
    assert(viewModel.uiState.value.title == "New Title")

    viewModel.setDescription("New Description")
    assert(viewModel.uiState.value.description == "New Description")

    viewModel.setMaxParticipants("5")
    assert(viewModel.uiState.value.maxParticipants == "5")

    viewModel.setDate("01/01/2026")
    assert(viewModel.uiState.value.date == "01/01/2026")

    viewModel.setTime("10:00")
    assert(viewModel.uiState.value.time == "10:00")

    viewModel.setVisibility("PRIVATE")
    assert(viewModel.uiState.value.visibility == "PRIVATE")
  }

  @Test
  fun viewModel_clearErrorMsg_removesError() {
    val repo = SeriesRepositoryLocal()
    val viewModel = EditSerieViewModel(repo)

    // Load non-existent serie to trigger error
    runBlocking { viewModel.loadSerie("non-existent-id") }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Error should be set
    assert(viewModel.uiState.value.errorMsg != null)

    // Clear error
    viewModel.clearErrorMsg()

    // Error should be cleared
    assert(viewModel.uiState.value.errorMsg == null)
  }

  @Test
  fun saveButton_enabledWhenAllFieldsValid() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditSerieScreenTestTags.SERIE_SAVE)
          .fetchSemanticsNodes()
          .firstOrNull()
          ?.config
          ?.getOrNull(SemanticsProperties.Disabled) == null
    }

    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE).assertIsEnabled()
  }

  @Test
  fun whitespaceTitleIsTreatedAsEmpty() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performTextClearance()
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performTextInput("   ")

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.SERIE_SAVE).assertIsNotEnabled()
  }

  @Test
  fun multipleTextFieldEdits_allPersist() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Edit multiple text fields
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE)
        .performTextInput("New Title")

    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .performTextInput("New Description")

    composeTestRule.waitForIdle()

    // Verify all persist in UI state
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_TITLE)
        .assertTextContains("New Title")
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_DESCRIPTION)
        .assertTextContains("New Description")
  }

  @Test
  fun visibilityDropdownCanBeChanged() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify initial value
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .assertTextContains("PUBLIC")

    // Change visibility
    composeTestRule.onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_VISIBILITY).performClick()
    composeTestRule.onNodeWithText("PRIVATE").performClick()

    // Verify changed value
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_VISIBILITY)
        .assertTextContains("PRIVATE")
  }

  /** --- ERROR MESSAGE DISPLAY TESTS --- */
  @Test
  fun invalidMaxParticipants_showsErrorMessage() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Set invalid max participants
    viewModel.setMaxParticipants("0")

    composeTestRule.waitForIdle()

    // Verify error message is displayed
    composeTestRule.onNodeWithText("Must be a positive number").assertExists()
  }

  @Test
  fun invalidDate_showsErrorMessage() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Set invalid date format
    viewModel.setDate("2025-12-25")

    composeTestRule.waitForIdle()

    // Verify error message is displayed
    composeTestRule.onNodeWithText("Invalid format (must be dd/MM/yyyy)").assertExists()
  }

  @Test
  fun invalidTime_showsErrorMessage() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Set invalid time format
    viewModel.setTime("not a time")

    composeTestRule.waitForIdle()

    // Verify error message is displayed
    composeTestRule.onNodeWithText("Invalid format (must be HH:mm)").assertExists()
  }

  @Test
  fun invalidVisibility_showsErrorMessage() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Set invalid visibility
    viewModel.setVisibility("INVALID")

    composeTestRule.waitForIdle()

    // Verify error message is displayed
    composeTestRule.onNodeWithText("Visibility must be PUBLIC or PRIVATE").assertExists()
  }

  /** --- DIALOG TESTS --- */
  @Test
  fun maxParticipantsDialog_opensAndCloses() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click to open dialog
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .performClick()

    composeTestRule.waitForIdle()

    // Verify dialog is shown
    composeTestRule.onNodeWithText("Select Max Participants").assertExists()
    composeTestRule.onNodeWithText("OK").assertExists()
    composeTestRule.onNodeWithText("Cancel").assertExists()

    // Click Cancel to close
    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.waitForIdle()

    // Verify dialog is closed
    composeTestRule.onNodeWithText("Select Max Participants").assertDoesNotExist()
  }

  @Test
  fun maxParticipantsDialog_confirmButton_updatesValue() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click to open dialog
    composeTestRule
        .onNodeWithTag(EditSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS)
        .performClick()

    composeTestRule.waitForIdle()

    // Click OK to confirm (value will be whatever the NumberPicker has)
    composeTestRule.onNodeWithText("OK").performClick()

    composeTestRule.waitForIdle()

    // Verify dialog is closed
    composeTestRule.onNodeWithText("Select Max Participants").assertDoesNotExist()
  }

  @Test
  fun topAppBarDisplaysTitle() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    composeTestRule.setContent {
      EditSerieScreen(serieId = serie.serieId, editSerieViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()

    // Verify top app bar shows correct title
    composeTestRule.onNodeWithText("Edit Serie").assertIsDisplayed()
  }

  @Test
  fun backButtonTriggersOnGoBack() {
    val repo = SeriesRepositoryLocal()
    val serie = createTestSerie()
    runBlocking { repo.addSerie(serie) }
    val viewModel = EditSerieViewModel(repo)

    var backPressed = false
    composeTestRule.setContent {
      EditSerieScreen(
          serieId = serie.serieId,
          editSerieViewModel = viewModel,
          onGoBack = { backPressed = true })
    }

    composeTestRule.waitForIdle()

    // Find and click back button
    composeTestRule.onNodeWithContentDescription("Back").performClick()

    // Verify callback was triggered
    assert(backPressed)
  }
}
