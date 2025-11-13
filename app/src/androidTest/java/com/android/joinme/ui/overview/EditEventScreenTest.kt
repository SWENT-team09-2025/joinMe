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
  fun allFieldsAreDisplayed() {
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

    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TYPE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_LOCATION).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DURATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_DATE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_VISIBILITY)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).assertIsDisplayed()
  }

  /** --- SAVE FUNCTIONALITY --- */
  @Test
  fun clickingSaveWithValidData_callsOnDone() {
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

    // Wait for button to be enabled
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditEventScreenTestTags.EVENT_SAVE)
          .fetchSemanticsNodes()
          .firstOrNull()
          ?.config
          ?.getOrNull(SemanticsProperties.Disabled) == null
    }

    // Click save
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)

    // Assert callback called
    assert(saveCalled)
  }

  @Test
  fun savingEdits_persistsChanges() {
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

    // Edit title
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditEventScreenTestTags.INPUT_EVENT_TITLE)
        .performTextInput("Updated Basketball Game")

    // Wait for button to be enabled
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(EditEventScreenTestTags.EVENT_SAVE)
          .fetchSemanticsNodes()
          .firstOrNull()
          ?.config
          ?.getOrNull(SemanticsProperties.Disabled) == null
    }

    // Save
    composeTestRule.onNodeWithTag(EditEventScreenTestTags.EVENT_SAVE).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify changes persisted
    runBlocking {
      val updatedEvent = repo.getEvent(event.eventId)
      assert(updatedEvent.title == "Updated Basketball Game")
    }
  }
}
