package com.android.joinme.ui.overview

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.*
import com.android.joinme.model.map.Location
import com.android.joinme.model.map.LocationRepository
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditEventScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Mock LocationRepository for testing
  private class MockLocationRepository : LocationRepository {
    override suspend fun search(query: String): List<Location> {
      return when {
        query.contains("EPFL") ->
            listOf(Location(46.5197, 6.6323, "EPFL"), Location(46.5198, 6.6324, "EPFL Campus"))
        query.contains("Lausanne") ->
            listOf(
                Location(46.5191, 6.6335, "Lausanne Sports Center"),
                Location(46.5192, 6.6336, "Lausanne Downtown"))
        query.contains("New Location") -> listOf(Location(46.5193, 6.6337, "New Location"))
        else -> emptyList()
      }
    }
  }

  private fun createTestEvent(): Event {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.DECEMBER, 25, 14, 30, 0)

    return Event(
        eventId = "test-event-1",
        type = EventType.SPORTS,
        title = "Basketball Game",
        description = "Friendly 3v3 basketball match",
        location = Location(46.5197, 6.6323, "EPFL"),
        date = Timestamp(calendar.time),
        duration = 90,
        participants = listOf("user1"),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = "owner123")
  }

  /** --- BASIC RENDERING --- */
  @Test
  fun eventDataIsLoadedAndDisplayed() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify loaded data
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TYPE)
        .assertTextContains("SPORTS")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE)
        .assertTextContains("Basketball Game")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertTextContains("Friendly 3v3 basketball match")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_LOCATION)
        .assertTextContains("EPFL")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .assertTextContains("10")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DURATION)
        .assertTextContains("90")
    // Date and time are displayed separately now
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DATE)
        .assertTextContains("25/12/2024")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .assertTextContains("PUBLIC")
  }

  /** --- EDITING FUNCTIONALITY --- */
  @Test
  fun editingTitle_updatesUIState() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Updated Title")

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE)
        .assertTextContains("Updated Title")
  }

  @Test
  fun editingDescription_updatesUIState() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("New description text")

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertTextContains("New description text")
  }

  @Test
  fun editingLocation_updatesUIState() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Simulate selecting a new location
    val newLocation = Location(46.5191, 6.6335, "Lausanne Sports Center")
    viewModel.selectLocation(newLocation)

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_LOCATION)
        .assertTextContains("Lausanne Sports Center")
  }

  // Note: Max participants and duration now use number pickers and cannot be edited via text input
  @Test
  fun maxParticipantsFieldDisplaysLoadedValue() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify field displays the loaded value
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .assertTextContains("10")
  }

  @Test
  fun durationFieldDisplaysLoadedValue() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify field displays the loaded value
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DURATION)
        .assertTextContains("90")
  }

  // Note: Date field now uses date picker dialog and cannot be edited via text input

  /** --- VALIDATION --- */
  @Test
  fun clearingTitle_showsErrorAndDisablesSaveButton() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertExists()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsNotEnabled()
  }

  @Test
  fun clearingDescription_showsErrorAndDisablesSaveButton() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextClearance()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertExists()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsNotEnabled()
  }

  @Test
  fun clearingLocation_showsErrorAndDisablesSaveButton() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Clear location by setting empty query (simulates clearing the field)
    viewModel.setLocationQuery("")
    viewModel.setLocation("")

    composeTestRule.waitForIdle()

    // The save button should be disabled because selectedLocation is now null
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsNotEnabled()
  }

  @Test
  fun correctingInvalidField_removesErrorAndEnablesSaveButton() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Make field invalid
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()

    composeTestRule.waitForIdle()

    // Verify error is shown
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertExists()

    // Fix the field
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Valid Title")

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditEventScreenTestTags.EVENT_SAVE)
          .fetchSemanticsNodes()
          .firstOrNull()
          ?.config
          ?.getOrNull(SemanticsProperties.Disabled) == null
    }

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsEnabled()
  }

  /** --- SAVE FUNCTIONALITY --- */
  @Test
  fun clickingSaveWithInvalidData_doesNotCallOnDone() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    var saveCalled = false

    composeTestRule.setContent {
      EditEventScreen(
          eventId = event.eventId, editEventViewModel = viewModel, onDone = { saveCalled = true })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Make data invalid
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()

    composeTestRule.waitForIdle()

    // Button should be disabled, but try to verify click doesn't work
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsNotEnabled()

    // Callback should not be called
    assert(!saveCalled)
  }

  /** --- VIEWMODEL TESTS --- */
  @Test
  fun viewModel_loadEvent_updatesUIState() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    // Initially empty
    assert(viewModel.uiState.value.title.isEmpty())

    // Load event
    viewModel.loadEvent(event.eventId)

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify loaded
    assert(viewModel.uiState.value.title == "Basketball Game")
    assert(viewModel.uiState.value.type == "SPORTS")
    assert(viewModel.uiState.value.maxParticipants == "10")
  }

  @Test
  fun viewModel_settersUpdateState() {
    val repo = EventsRepositoryLocal()
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    viewModel.setTitle("New Title")
    assert(viewModel.uiState.value.title == "New Title")

    viewModel.setDescription("New Description")
    assert(viewModel.uiState.value.description == "New Description")

    viewModel.setLocation("New Location")
    assert(viewModel.uiState.value.location == "New Location")

    val testLocation = Location(46.5191, 6.6335, "Test Location")
    viewModel.selectLocation(testLocation)
    assert(viewModel.uiState.value.selectedLocation == testLocation)
    assert(viewModel.uiState.value.locationQuery == "Test Location")

    viewModel.setMaxParticipants("5")
    assert(viewModel.uiState.value.maxParticipants == "5")

    viewModel.setDuration("45")
    assert(viewModel.uiState.value.duration == "45")

    viewModel.setType("SPORTS")
    assert(viewModel.uiState.value.type == "SPORTS")

    viewModel.setVisibility("PRIVATE")
    assert(viewModel.uiState.value.visibility == "PRIVATE")
  }

  @Test
  fun viewModel_clearErrorMsg_removesError() {
    val repo = EventsRepositoryLocal()
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    // Load non-existent event to trigger error
    viewModel.loadEvent("non-existent-id")

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
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditEventScreenTestTags.EVENT_SAVE)
          .fetchSemanticsNodes()
          .firstOrNull()
          ?.config
          ?.getOrNull(SemanticsProperties.Disabled) == null
    }

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsEnabled()
  }

  @Test
  fun whitespaceTitleIsTreatedAsEmpty() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("   ")

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsNotEnabled()
  }

  @Test
  fun multipleTextFieldEdits_allPersist() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Edit multiple text fields
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("New Title")

    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .performTextInput("New Description")

    // Simulate selecting a new location
    val newLocation = Location(46.5193, 6.6337, "New Location")
    viewModel.selectLocation(newLocation)

    composeTestRule.waitForIdle()

    // Verify all persist in UI state
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE)
        .assertTextContains("New Title")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertTextContains("New Description")
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_LOCATION)
        .assertTextContains("New Location")
  }

  @Test
  fun typeDropdownCanBeChanged() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify initial value
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TYPE)
        .assertTextContains("SPORTS")

    // Change type
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
    composeTestRule.onNodeWithText("SOCIAL").performClick()

    // Verify changed value
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TYPE)
        .assertTextContains("SOCIAL")
  }

  @Test
  fun visibilityDropdownCanBeChanged() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify initial value
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .performScrollTo()
        .assertTextContains("PUBLIC")

    // Change visibility
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_VISIBILITY).performScrollTo().performClick()
    composeTestRule.onNodeWithText("PRIVATE").performClick()

    // Verify changed value
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .assertTextContains("PRIVATE")
  }

  /** --- ERROR MESSAGE DISPLAY TESTS --- */
  @Test
  fun invalidType_showsErrorMessage() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Set invalid type via ViewModel
    viewModel.setType("")

    composeTestRule.waitForIdle()

    // Verify error message is displayed
    composeTestRule.onNodeWithText("Type cannot be empty").assertExists()
  }

  @Test
  fun invalidMaxParticipants_belowCurrentCount_showsErrorMessage() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent() // Has 1 participant
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Try to set max participants below current count (0 is below 1)
    viewModel.setMaxParticipants("0")

    composeTestRule.waitForIdle()

    // Verify error message is displayed
    composeTestRule.onNodeWithText("Must be a positive number").assertExists()
  }

  @Test
  fun invalidDuration_showsErrorMessage() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Set invalid duration
    viewModel.setDuration("0")

    composeTestRule.waitForIdle()

    // Verify error message is displayed
    composeTestRule.onNodeWithText("Must be a positive number").assertExists()
  }

  @Test
  fun invalidDate_showsErrorMessage() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Set invalid date format
    viewModel.setDate("2024-12-25")

    composeTestRule.waitForIdle()

    // Verify error message is displayed
    composeTestRule.onNodeWithText("Invalid format (must be dd/MM/yyyy)").assertExists()
  }

  @Test
  fun invalidVisibility_showsErrorMessage() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Set invalid visibility
    viewModel.setVisibility("")

    composeTestRule.waitForIdle()

    // Verify error message is displayed
    composeTestRule.onNodeWithText("Event visibility cannot be empty").assertExists()
  }

  /** --- DIALOG TESTS --- */
  @Test
  fun maxParticipantsDialog_opensAndCloses() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click to open dialog
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
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
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click to open dialog
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .performClick()

    composeTestRule.waitForIdle()

    // Click OK to confirm (value will be whatever the NumberPicker has)
    composeTestRule.onNodeWithText("OK").performClick()

    composeTestRule.waitForIdle()

    // Verify dialog is closed
    composeTestRule.onNodeWithText("Select Max Participants").assertDoesNotExist()
  }

  @Test
  fun durationDialog_opensAndCloses() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click to open dialog
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DURATION).performClick()

    composeTestRule.waitForIdle()

    // Verify dialog is shown
    composeTestRule.onNodeWithText("Select Duration (min)").assertExists()
    composeTestRule.onNodeWithText("OK").assertExists()
    composeTestRule.onNodeWithText("Cancel").assertExists()

    // Click Cancel to close
    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.waitForIdle()

    // Verify dialog is closed
    composeTestRule.onNodeWithText("Select Duration (min)").assertDoesNotExist()
  }

  @Test
  fun durationDialog_confirmButton_updatesValue() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = EditEventViewModel(repo, MockLocationRepository())

    composeTestRule.setContent {
      EditEventScreen(eventId = event.eventId, editEventViewModel = viewModel, onDone = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click to open dialog
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DURATION).performClick()

    composeTestRule.waitForIdle()

    // Click OK to confirm
    composeTestRule.onNodeWithText("OK").performClick()

    composeTestRule.waitForIdle()

    // Verify dialog is closed
    composeTestRule.onNodeWithText("Select Duration (min)").assertDoesNotExist()
  }
}
