package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.*
import com.android.joinme.model.map.Location
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

  /** --- FULL EVENT TEST --- */
  @Test
  fun joinButtonIsDisabledWhenEventIsFullForNotParticipants() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(maxParticipants = 1)
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
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.FULL_EVENT_MESSAGE).assertExists()
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

  /** --- OWNER DISPLAY NAME TESTS --- */
  @Test
  fun ownerDisplayName_showsUsernameFromProfile() {
    val repo = EventsRepositoryLocal()
    val profileRepo = mock(ProfileRepository::class.java)
    val event = createTestEvent(ownerId = "owner123")
    runBlocking {
      repo.addEvent(event)
      val mockProfile = Profile(uid = "owner123", username = "JohnDoe", email = "john@example.com")
      whenever(profileRepo.getProfile("owner123")).thenReturn(mockProfile)
    }
    val viewModel = ShowEventViewModel(repo, profileRepo)

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
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_OWNER)
        .assertTextContains("Created by JohnDoe")
  }

  @Test
  fun ownerDisplayName_showsUnknownWhenProfileNotFound() {
    val repo = EventsRepositoryLocal()
    val profileRepo = mock(ProfileRepository::class.java)
    val event = createTestEvent(ownerId = "unknown-owner")
    runBlocking {
      repo.addEvent(event)
      whenever(profileRepo.getProfile("unknown-owner")).thenReturn(null)
    }
    val viewModel = ShowEventViewModel(repo, profileRepo)

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
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_OWNER)
        .assertTextContains("Created by UNKNOWN")
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
  fun clickingEditButton_callsOnEditEvent_whenNoSerieId() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123")
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    var editEventCalled = false
    var editEventId = ""

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          serieId = null,
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
  fun clickingEditButton_callsOnEditEventForSerie_whenSerieIdExists() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123")
    runBlocking { repo.addEvent(event) }
    val serieId = "test-serie-123"
    val viewModel = ShowEventViewModel(repo)

    var editEventForSerieCalled = false
    var capturedSerieId = ""
    var capturedEventId = ""

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          serieId = serieId,
          currentUserId = "owner123",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {},
          onEditEventForSerie = { sId, eId ->
            editEventForSerieCalled = true
            capturedSerieId = sId
            capturedEventId = eId
          })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).performClick()

    assert(editEventForSerieCalled)
    assert(capturedSerieId == serieId)
    assert(capturedEventId == event.eventId)
  }

  /** --- PARTICIPANT VIEW --- */
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
          currentUserId = "user1", // user1 is a participant
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("QUIT EVENT")
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun participantClickingQuitButton_quitsEvent() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123", participants = listOf("user1", "owner123"))
    runBlocking { repo.addEvent(event) }

    // Mock profileRepository for user1
    val profileRepository = mock(ProfileRepository::class.java)
    val user1Profile = Profile(uid = "user1", username = "User1", email = "user1@test.com", eventsJoinedCount = 5)
    runBlocking {
      whenever(profileRepository.getProfile("user1")).thenReturn(user1Profile)
    }

    val viewModel = ShowEventViewModel(repo, profileRepository)

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

    // Verify initial state
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 2/10")
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("QUIT EVENT")

    // Click quit button
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify participant count decreased
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 1/10")
  }

  /** --- NON-PARTICIPANT VIEW --- */
  @Test
  fun nonParticipantSeesJoinButton() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123", participants = listOf("owner123"))
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          currentUserId = "user3", // user3 is not a participant
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {})
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("JOIN EVENT")
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun nonParticipantClickingJoinButton_joinsEvent() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123", participants = listOf("owner123"))
    runBlocking { repo.addEvent(event) }

    // Mock profileRepository for user3
    val profileRepository = mock(ProfileRepository::class.java)
    val user3Profile = Profile(uid = "user3", username = "User3", email = "user3@test.com", eventsJoinedCount = 0)
    runBlocking {
      whenever(profileRepository.getProfile("user3")).thenReturn(user3Profile)
    }

    val viewModel = ShowEventViewModel(repo, profileRepository)

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

    // Verify initial state
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 1/10")
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON)
        .assertTextContains("JOIN EVENT")

    // Click join button
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify participant count increased
    composeTestRule
        .onNodeWithTag(ShowEventScreenTestTags.EVENT_MEMBERS)
        .assertTextContains("MEMBERS : 2/10")
  }

  /** --- DELETE EVENT TESTS --- */
  @Test
  fun ownerClickingDeleteButton_showsConfirmationDialog() {
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

    // Click delete button
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).performClick()

    // Verify confirmation dialog appears
    composeTestRule.onNodeWithText("Delete Event").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Are you sure you want to delete this event? This action cannot be undone.")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun ownerConfirmingDelete_callsOnGoBack() {
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

    // Click delete button
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).performClick()

    // Confirm deletion
    composeTestRule.onNodeWithText("Delete").performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    assert(goBackCalled)
  }

  @Test
  fun ownerCancellingDelete_closesDialog() {
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

    // Click delete button
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).performClick()

    // Cancel deletion
    composeTestRule.onNodeWithText("Cancel").performClick()

    // Dialog should be closed - verify by checking if event screen is still displayed
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete Event").assertDoesNotExist()
  }

  /** --- PAST EVENT TESTS --- */
  @Test
  fun pastEvent_hidesAllActionButtons() {
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

  /** --- SERIE ID TESTS --- */
  @Test
  fun ownerSeesEditButton_withSerieId() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123")
    runBlocking { repo.addEvent(event) }
    val serieId = "test-serie-456"
    val viewModel = ShowEventViewModel(repo)

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          serieId = serieId,
          currentUserId = "owner123",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = {},
          onEditEventForSerie = { _, _ -> })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.DELETE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun editButton_doesNotCallOnEditEvent_whenSerieIdIsProvided() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123")
    runBlocking { repo.addEvent(event) }
    val serieId = "test-serie-789"
    val viewModel = ShowEventViewModel(repo)

    var editEventCalled = false

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          serieId = serieId,
          currentUserId = "owner123",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = { editEventCalled = true },
          onEditEventForSerie = { _, _ -> })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).performClick()

    // onEditEvent should NOT be called when serieId is provided
    assert(!editEventCalled)
  }

  @Test
  fun editButton_callsCorrectCallback_basedOnSerieIdPresence() {
    val repo = EventsRepositoryLocal()
    val event = createTestEvent(ownerId = "owner123")
    runBlocking { repo.addEvent(event) }
    val viewModel = ShowEventViewModel(repo)

    // Test without serieId - should call onEditEvent
    var editEventCalled = false
    var editEventForSerieCalled = false

    composeTestRule.setContent {
      ShowEventScreen(
          eventId = event.eventId,
          serieId = null,
          currentUserId = "owner123",
          showEventViewModel = viewModel,
          onGoBack = {},
          onEditEvent = { editEventCalled = true },
          onEditEventForSerie = { _, _ -> editEventForSerieCalled = true })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ShowEventScreenTestTags.EDIT_BUTTON).performClick()

    assert(editEventCalled)
    assert(!editEventForSerieCalled)
  }
}
