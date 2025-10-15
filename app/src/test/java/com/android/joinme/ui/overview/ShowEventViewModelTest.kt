package com.android.joinme.ui.overview

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShowEventViewModelTest {

  private lateinit var repository: EventsRepositoryLocal
  private lateinit var viewModel: ShowEventViewModel
  private val testDispatcher = StandardTestDispatcher()

  private fun createTestEvent(
      eventId: String = "test-event-1",
      ownerId: String = "owner123",
      participants: List<String> = listOf("user1", "user2"),
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

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = EventsRepositoryLocal()
    viewModel = ShowEventViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** --- LOAD EVENT TESTS --- */
  @Test
  fun loadEvent_validEventId_updatesUIState() = runTest {
    val event = createTestEvent()
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("SPORTS", state.type)
    assertEquals("Basketball Game", state.title)
    assertEquals("Friendly 3v3 basketball match", state.description)
    assertEquals("EPFL", state.location)
    assertEquals("10", state.maxParticipants)
    assertEquals("2", state.participantsCount)
    assertEquals("90", state.duration)
    assertTrue(state.date.contains("SPORTS:"))
    assertEquals("PUBLIC", state.visibility)
    assertEquals("owner123", state.ownerId)
    assertTrue(state.ownerName.contains("OWNER123"))
    assertEquals(listOf("user1", "user2"), state.participants)
    assertFalse(state.isPastEvent)
    assertNull(state.errorMsg)
  }

  @Test
  fun loadEvent_invalidEventId_setsErrorMessage() = runTest {
    viewModel.loadEvent("non-existent-id")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to load Event"))
  }

  @Test
  fun loadEvent_pastEvent_marksAsPast() = runTest {
    // Create an event that happened 7 days ago
    val pastEvent = createTestEvent(eventId = "past-event", daysFromNow = -7)
    repository.addEvent(pastEvent)

    viewModel.loadEvent(pastEvent.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.isPastEvent)
  }

  @Test
  fun loadEvent_activeEvent_notMarkedAsPast() = runTest {
    val calendar = Calendar.getInstance()
    // Event started 10 minutes ago and lasts 90 minutes
    calendar.add(Calendar.MINUTE, -10)

    val activeEvent =
        createTestEvent(eventId = "active-event").copy(date = Timestamp(calendar.time))
    repository.addEvent(activeEvent)

    viewModel.loadEvent(activeEvent.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isPastEvent)
  }

  /** --- CLEAR ERROR MESSAGE TESTS --- */
  @Test
  fun clearErrorMsg_removesErrorMessage() = runTest {
    viewModel.loadEvent("non-existent-id")
    advanceUntilIdle()

    // Verify error is set
    var state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)

    // Clear error
    viewModel.clearErrorMsg()

    // Verify error is cleared
    state = viewModel.uiState.first()
    assertNull(state.errorMsg)
  }

  /** --- UI STATE HELPER METHODS TESTS --- */
  @Test
  fun uiState_isOwner_returnsTrue_whenUserIsOwner() {
    val state = ShowEventUIState(ownerId = "user123")
    assertTrue(state.isOwner("user123"))
  }

  @Test
  fun uiState_isOwner_returnsFalse_whenUserIsNotOwner() {
    val state = ShowEventUIState(ownerId = "owner123")
    assertFalse(state.isOwner("user456"))
  }

  @Test
  fun uiState_isParticipant_returnsTrue_whenUserIsParticipant() {
    val state = ShowEventUIState(participants = listOf("user1", "user2", "user3"))
    assertTrue(state.isParticipant("user2"))
  }

  @Test
  fun uiState_isParticipant_returnsFalse_whenUserIsNotParticipant() {
    val state = ShowEventUIState(participants = listOf("user1", "user2", "user3"))
    assertFalse(state.isParticipant("user4"))
  }

  /** --- TOGGLE PARTICIPATION TESTS --- */
  @Test
  fun toggleParticipation_userNotParticipant_joinsEvent() = runTest {
    val event = createTestEvent(participants = listOf("user1"))
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    // User2 joins the event
    viewModel.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.participants.contains("user2"))
    assertEquals("2", state.participantsCount)
    assertNull(state.errorMsg)
  }

  @Test
  fun toggleParticipation_userIsParticipant_quitsEvent() = runTest {
    val event = createTestEvent(participants = listOf("user1", "user2"))
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    // User2 quits the event
    viewModel.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.participants.contains("user2"))
    assertEquals("1", state.participantsCount)
  }

  @Test
  fun toggleParticipation_eventFull_setsErrorMessage() = runTest {
    // Create event with max participants already reached
    val event = createTestEvent(participants = listOf("user1", "user2"), maxParticipants = 2)
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    // User3 tries to join
    viewModel.toggleParticipation(event.eventId, "user3")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Event is full"))
    assertFalse(state.participants.contains("user3"))
  }

  @Test
  fun toggleParticipation_invalidEventId_setsErrorMessage() = runTest {
    viewModel.toggleParticipation("non-existent-id", "user1")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to update participation"))
  }

  /** --- DELETE EVENT TESTS --- */
  @Test
  fun deleteEvent_validEventId_deletesEvent() = runTest {
    val event = createTestEvent()
    repository.addEvent(event)

    viewModel.deleteEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.errorMsg)

    // Verify the event was deleted - should throw exception
    try {
      repository.getEvent(event.eventId)
      fail("Expected exception when getting deleted event")
    } catch (e: Exception) {
      // Expected - event was deleted
    }
  }

  @Test
  fun deleteEvent_invalidEventId_setsErrorMessage() = runTest {
    viewModel.deleteEvent("non-existent-id")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to delete Event"))
  }

  /** --- INITIAL STATE TESTS --- */
  @Test
  fun viewModel_defaultInitialState_isEmpty() {
    val state = viewModel.uiState.value
    assertEquals("", state.type)
    assertEquals("", state.title)
    assertEquals("", state.description)
    assertEquals("", state.location)
    assertEquals("", state.maxParticipants)
    assertEquals("", state.participantsCount)
    assertEquals("", state.duration)
    assertEquals("", state.date)
    assertEquals("", state.visibility)
    assertEquals("", state.ownerId)
    assertEquals("", state.ownerName)
    assertTrue(state.participants.isEmpty())
    assertFalse(state.isPastEvent)
    assertNull(state.errorMsg)
  }

  @Test
  fun viewModel_customInitialState_usesProvidedState() {
    val customState =
        ShowEventUIState(
            type = "SPORTS",
            title = "Custom Event",
            description = "Custom Description",
            location = "Custom Location",
            maxParticipants = "5",
            participantsCount = "3",
            duration = "30",
            date = "01/01/2024 10:00",
            visibility = "PRIVATE",
            ownerId = "custom-owner",
            ownerName = "CREATED BY CUSTOM",
            participants = listOf("user1", "user2", "user3"),
            isPastEvent = true)

    val customViewModel = ShowEventViewModel(repository, customState)

    val state = customViewModel.uiState.value
    assertEquals("SPORTS", state.type)
    assertEquals("Custom Event", state.title)
    assertEquals("Custom Description", state.description)
    assertEquals("Custom Location", state.location)
    assertEquals("5", state.maxParticipants)
    assertEquals("3", state.participantsCount)
    assertEquals("30", state.duration)
    assertEquals("01/01/2024 10:00", state.date)
    assertEquals("PRIVATE", state.visibility)
    assertEquals("custom-owner", state.ownerId)
    assertEquals("CREATED BY CUSTOM", state.ownerName)
    assertEquals(listOf("user1", "user2", "user3"), state.participants)
    assertTrue(state.isPastEvent)
  }

  /** --- OWNER NAME DISPLAY TESTS --- */
  @Test
  fun loadEvent_ownerWithEmailId_extractsNameFromEmail() = runTest {
    val event = createTestEvent(ownerId = "john.doe@example.com")
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.ownerName.contains("JOHN.DOE"))
  }

  @Test
  fun loadEvent_ownerWithEmptyId_showsUnknown() = runTest {
    val event = createTestEvent(ownerId = "")
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.ownerName.contains("UNKNOWN"))
  }

  /** --- DATE FORMATTING TESTS --- */
  @Test
  fun loadEvent_formatsDateCorrectly() = runTest {
    val calendar = Calendar.getInstance()
    calendar.set(2024, Calendar.DECEMBER, 25, 14, 30, 0)

    val event = createTestEvent().copy(date = Timestamp(calendar.time))
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.date.contains("SPORTS:"))
    assertTrue(state.date.contains("25/12/2024"))
    assertTrue(state.date.contains("14:30"))
  }

  /** --- LOCATION TESTS --- */
  @Test
  fun loadEvent_eventWithoutLocation_setsEmptyLocation() = runTest {
    val event = createTestEvent().copy(location = null)
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("", state.location)
  }

  @Test
  fun loadEvent_eventWithLocation_setsLocationName() = runTest {
    val event = createTestEvent()
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("EPFL", state.location)
  }

  /** --- EVENT TYPE TESTS --- */
  @Test
  fun loadEvent_socialEvent_setsCorrectType() = runTest {
    val event = createTestEvent().copy(type = EventType.SOCIAL)
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("SOCIAL", state.type)
  }

  @Test
  fun loadEvent_activityEvent_setsCorrectType() = runTest {
    val event = createTestEvent().copy(type = EventType.ACTIVITY)
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("ACTIVITY", state.type)
  }

  /** --- VISIBILITY TESTS --- */
  @Test
  fun loadEvent_privateEvent_setsCorrectVisibility() = runTest {
    val event = createTestEvent().copy(visibility = EventVisibility.PRIVATE)
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("PRIVATE", state.visibility)
  }
}
