package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.*
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ShowEventScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createTestEvent(
      eventId: String = "test-event-1",
      ownerId: String = "owner123",
      participants: List<String> = listOf("user1", "user2", "owner123"),
      maxParticipants: Int = 10,
      daysFromNow: Int = 7 // Future event by default
  ): Event {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, daysFromNow)
    calendar.set(Calendar.HOUR_OF_DAY, 14)
    calendar.set(Calendar.MINUTE, 30)
    calendar.set(Calendar.SECOND, 0)

    return Event(
        eventId = eventId,
        type = EventType.SPORTS,
        title = "Basketball Game",
        description = "Friendly 3v3 basketball match",
        location = Location(46.5197, 6.6323, "EPFL"),
        date = Timestamp(calendar.time),
        duration = 90,
        participants = participants,
        maxParticipants = maxParticipants,
        visibility = EventVisibility.PUBLIC,
        ownerId = ownerId)
  }

  /** --- BASIC RENDERING --- */
  @Test
  fun allElementsAreDisplayed() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user3",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TYPE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_VISIBILITY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_DESCRIPTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_LOCATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_DURATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_OWNER).assertIsDisplayed()
  }

  @Test
  fun eventDataIsDisplayedCorrectly() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user3",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_TITLE)
        .assertTextContains("Basketball Game")
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TYPE).assertTextContains("SPORTS")
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_DATE).assertExists()
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_VISIBILITY)
        .assertTextContains("PUBLIC")
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_DESCRIPTION)
        .assertTextContains("Friendly 3v3 basketball match")
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_LOCATION).assertTextContains("EPFL")
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 3/10")
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_DURATION)
        .assertTextContains("90min")
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_OWNER).assertExists()
  }

  /** --- OWNER VIEW --- */
  @Test
  fun ownerSeesEditAndDeleteButtons() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123")
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "owner123", // Current user is the owner
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertDoesNotExist()
  }

  @Test
  fun clickingEditButton_callsOnEditEvent() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123")
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    var editEventCalled = false
    var editEventId = ""

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "owner123",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = { id ->
            editEventCalled = true
            editEventId = id
          })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).performClick()

    assert(editEventCalled)
    assert(editEventId == event.eventId)
  }

  @Test
  fun clickingDeleteButton_showsConfirmationDialog() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123")
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "owner123",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).performClick()

    // Check for dialog elements
    composeTestRule.onNodeWithText("Delete Event").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Are you sure you want to delete this event? This action cannot be undone.")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun confirmingDelete_deletesEventAndCallsOnGoBack() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123")
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    var goBackCalled = false

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "owner123",
          showEventViewModel = viewModel,
          onGoBack = { goBackCalled = true },
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click delete
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Confirm delete
    composeTestRule.onNodeWithText("Delete").performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify callback was called
    assert(goBackCalled)

    // Verify event was deleted
    runBlocking {
      try {
        repo.getEvent(event.eventId)
        assert(false) { "Event should have been deleted" }
      } catch (e: Exception) {
        // Expected - event was deleted
      }
    }
  }

  @Test
  fun cancelingDelete_doesNotDeleteEvent() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123")
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "owner123",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click delete
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Cancel delete
    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.waitForIdle()

    // Verify dialog is closed
    composeTestRule.onNodeWithText("Delete Event").assertDoesNotExist()

    // Verify event still exists
    runBlocking {
      val existingEvent = repo.getEvent(event.eventId)
      assert(existingEvent.eventId == event.eventId)
    }
  }

  /** --- NON-OWNER VIEW --- */
  @Test
  fun nonOwnerSeesJoinQuitButton() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123", participants = listOf("user1", "owner123"))
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user2", // Not the owner
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun nonParticipantSeesJoinButton() {
    val repo = EventsRepositoryLocal()
    val event =
        createTestEvent(ownerId = "owner123", participants = listOf("user1", "user2", "owner123"))
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user3", // Not a participant
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("JOIN EVENT")
  }

  @Test
  fun participantSeesQuitButton() {
    val repo = EventsRepositoryLocal()
    val event =
        createTestEvent(ownerId = "owner123", participants = listOf("user1", "user2", "owner123"))
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user2", // Is a participant
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("QUIT EVENT")
  }

  @Test
  fun clickingJoinButton_addsUserToParticipants() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123", participants = listOf("user1", "owner123"))
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user2",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify initial state
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 2/10")
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("JOIN EVENT")

    // Click join
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify user was added
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 3/10")
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("QUIT EVENT")
  }

  @Test
  fun clickingQuitButton_removesUserFromParticipants() {
    val repo = EventsRepositoryLocal()
    val event =
        createTestEvent(ownerId = "owner123", participants = listOf("user1", "user2", "owner123"))
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user2",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify initial state
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 3/10")
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("QUIT EVENT")

    // Click quit
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify user was removed
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 2/10")
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("JOIN EVENT")
  }

  /** --- PAST EVENT TESTS --- */
  @Test
  fun pastEvent_doesNotShowAnyButtons() {
    val repo = EventsRepositoryLocal()
    // Create an event that happened 7 days ago
    val pastEvent = createTestEvent(ownerId = "owner123", daysFromNow = -7)
    runBlocking { repo.addEvent(pastEvent) }
    val viewModel = ShowEventViewModel(repo)

    // Test for owner view
    composeTestRule.setContent {
      ShowEventScreen(
          eventId = pastEvent.eventId,
          currentUserId = "owner123",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertDoesNotExist()
  }

  /** --- NAVIGATION TESTS --- */
  @Test
  fun clickingBackButton_callsOnGoBack() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent()
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    var goBackCalled = false

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user1",
          showEventViewModel = viewModel,
          onGoBack = { goBackCalled = true },
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Click back button (it's in the TopAppBar, find by content description)
    composeTestRule.onNodeWithContentDescription("Back").performClick()

    assert(goBackCalled)
  }

  /** --- EVENT TYPE TESTS --- */
  @Test
  fun socialEvent_displaysCorrectType() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent().copy(type = EventType.SOCIAL)
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user1",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TYPE).assertTextContains("SOCIAL")
  }

  @Test
  fun activityEvent_displaysCorrectType() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent().copy(type = EventType.ACTIVITY)
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user1",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_TYPE).assertTextContains("ACTIVITY")
  }

  /** --- VISIBILITY TESTS --- */
  @Test
  fun privateEvent_displaysCorrectVisibility() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent().copy(visibility = EventVisibility.PRIVATE)
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user1",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_VISIBILITY)
        .assertTextContains("PRIVATE")
  }

  /** --- FULL EVENT TESTS --- */
  @Test
  fun joiningFullEvent_showsErrorToast() {
    val repo = EventsRepositoryLocal()
    // Create event with max participants already reached
    val event =
        createTestEvent(
            ownerId = "owner123", participants = listOf("user1", "owner123"), maxParticipants = 2)
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user3",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify event is full
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 2/2")

    // Try to join
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify user was NOT added (still 2 participants)
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 2/2")
    // Still shows JOIN EVENT (not QUIT)
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("JOIN EVENT")
  }

  /** --- LOCATION TESTS --- */
  @Test
  fun eventWithoutLocation_showsEmptyLocation() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent().copy(location = null)
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user1",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // The location tag should still exist but be empty or show empty text
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EVENT_LOCATION).assertExists()
  }
}
