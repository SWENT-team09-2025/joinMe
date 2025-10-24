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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditEventViewModelTest {

  private lateinit var repository: EventsRepositoryLocal
  private lateinit var viewModel: EditEventViewModel
  private val testDispatcher = StandardTestDispatcher()

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
        participants = listOf("user1", "user2"),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = "owner123")
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = EventsRepositoryLocal()
    viewModel = EditEventViewModel(repository)
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
    assertEquals("90", state.duration)
    assertEquals("25/12/2024", state.date)
    assertEquals("14:30", state.time)
    assertEquals("PUBLIC", state.visibility)
    assertEquals("owner123", state.ownerId)
    assertEquals(listOf("user1", "user2"), state.participants)
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

  /** --- SET TYPE TESTS --- */
  @Test
  fun setType_validType_updatesStateWithoutError() = runBlocking {
    viewModel.setType("SPORTS")

    val state = viewModel.uiState.first()
    assertEquals("SPORTS", state.type)
    assertNull(state.invalidTypeMsg)
  }

  @Test
  fun setType_blankType_setsErrorMessage() = runBlocking {
    viewModel.setType("")

    val state = viewModel.uiState.first()
    assertEquals("", state.type)
    assertEquals("Type cannot be empty", state.invalidTypeMsg)
  }

  @Test
  fun setType_invalidType_setsErrorMessage() = runBlocking {
    viewModel.setType("INVALID")

    val state = viewModel.uiState.first()
    assertEquals("INVALID", state.type)
    assertEquals("Type must be one of: SPORTS, ACTIVITY, SOCIAL", state.invalidTypeMsg)
  }

  /** --- SET TITLE TESTS --- */
  @Test
  fun setTitle_validTitle_updatesStateWithoutError() = runBlocking {
    viewModel.setTitle("My Event")

    val state = viewModel.uiState.first()
    assertEquals("My Event", state.title)
    assertNull(state.invalidTitleMsg)
  }

  @Test
  fun setTitle_blankTitle_setsErrorMessage() = runBlocking {
    viewModel.setTitle("   ")

    val state = viewModel.uiState.first()
    assertEquals("   ", state.title)
    assertEquals("Title cannot be empty", state.invalidTitleMsg)
  }

  /** --- SET DESCRIPTION TESTS --- */
  @Test
  fun setDescription_validDescription_updatesStateWithoutError() = runBlocking {
    viewModel.setDescription("This is a description")

    val state = viewModel.uiState.first()
    assertEquals("This is a description", state.description)
    assertNull(state.invalidDescriptionMsg)
  }

  @Test
  fun setDescription_blankDescription_setsErrorMessage() = runBlocking {
    viewModel.setDescription("")

    val state = viewModel.uiState.first()
    assertEquals("", state.description)
    assertEquals("Description cannot be empty", state.invalidDescriptionMsg)
  }

  /** --- SET LOCATION TESTS --- */
  @Test
  fun setLocation_validLocation_updatesStateWithoutError() = runBlocking {
    viewModel.setLocation("EPFL")

    val state = viewModel.uiState.first()
    assertEquals("EPFL", state.location)
    assertNull(state.invalidLocationMsg)
  }

  @Test
  fun setLocation_blankLocation_setsErrorMessage() = runBlocking {
    viewModel.setLocation("")

    val state = viewModel.uiState.first()
    assertEquals("", state.location)
    assertEquals("Must be a valid Location", state.invalidLocationMsg)
  }

  /** --- SET MAX PARTICIPANTS TESTS --- */
  @Test
  fun setMaxParticipants_validNumber_updatesStateWithoutError() = runBlocking {
    viewModel.setMaxParticipants("10")

    val state = viewModel.uiState.first()
    assertEquals("10", state.maxParticipants)
    assertNull(state.invalidMaxParticipantsMsg)
  }

  @Test
  fun setMaxParticipants_zero_setsErrorMessage() = runBlocking {
    viewModel.setMaxParticipants("0")

    val state = viewModel.uiState.first()
    assertEquals("0", state.maxParticipants)
    assertEquals("Must be a positive number", state.invalidMaxParticipantsMsg)
  }

  @Test
  fun setMaxParticipants_negativeNumber_setsErrorMessage() = runBlocking {
    viewModel.setMaxParticipants("-5")

    val state = viewModel.uiState.first()
    assertEquals("-5", state.maxParticipants)
    assertEquals("Must be a positive number", state.invalidMaxParticipantsMsg)
  }

  @Test
  fun setMaxParticipants_nonNumeric_setsErrorMessage() = runBlocking {
    viewModel.setMaxParticipants("abc")

    val state = viewModel.uiState.first()
    assertEquals("abc", state.maxParticipants)
    assertEquals("Must be a positive number", state.invalidMaxParticipantsMsg)
  }

  @Test
  fun setMaxParticipants_belowCurrentParticipantCount_setsErrorMessage() = runTest {
    val event = createTestEvent() // Has 2 participants: user1, user2
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    // Try to set max participants to 1, which is less than current 2 participants
    viewModel.setMaxParticipants("1")

    val state = viewModel.uiState.first()
    assertEquals("1", state.maxParticipants)
    assertEquals("Cannot be less than current participants (2)", state.invalidMaxParticipantsMsg)
  }

  @Test
  fun setMaxParticipants_equalToCurrentParticipantCount_updatesWithoutError() = runTest {
    val event = createTestEvent() // Has 2 participants
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    // Set max participants equal to current count
    viewModel.setMaxParticipants("2")

    val state = viewModel.uiState.first()
    assertEquals("2", state.maxParticipants)
    assertNull(state.invalidMaxParticipantsMsg)
  }

  @Test
  fun setMaxParticipants_aboveCurrentParticipantCount_updatesWithoutError() = runTest {
    val event = createTestEvent() // Has 2 participants
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    // Set max participants above current count
    viewModel.setMaxParticipants("5")

    val state = viewModel.uiState.first()
    assertEquals("5", state.maxParticipants)
    assertNull(state.invalidMaxParticipantsMsg)
  }

  /** --- SET DURATION TESTS --- */
  @Test
  fun setDuration_validNumber_updatesStateWithoutError() = runBlocking {
    viewModel.setDuration("60")

    val state = viewModel.uiState.first()
    assertEquals("60", state.duration)
    assertNull(state.invalidDurationMsg)
  }

  @Test
  fun setDuration_zero_setsErrorMessage() = runBlocking {
    viewModel.setDuration("0")

    val state = viewModel.uiState.first()
    assertEquals("0", state.duration)
    assertEquals("Must be a positive number", state.invalidDurationMsg)
  }

  @Test
  fun setDuration_negativeNumber_setsErrorMessage() = runBlocking {
    viewModel.setDuration("-10")

    val state = viewModel.uiState.first()
    assertEquals("-10", state.duration)
    assertEquals("Must be a positive number", state.invalidDurationMsg)
  }

  @Test
  fun setDuration_nonNumeric_setsErrorMessage() = runBlocking {
    viewModel.setDuration("xyz")

    val state = viewModel.uiState.first()
    assertEquals("xyz", state.duration)
    assertEquals("Must be a positive number", state.invalidDurationMsg)
  }

  /** --- SET DATE TESTS --- */
  @Test
  fun setDate_validDate_updatesStateWithoutError() = runBlocking {
    viewModel.setDate("25/12/2024")

    val state = viewModel.uiState.first()
    assertEquals("25/12/2024", state.date)
    assertNull(state.invalidDateMsg)
  }

  @Test
  fun setDate_invalidFormat_setsErrorMessage() = runBlocking {
    viewModel.setDate("2024-12-25")

    val state = viewModel.uiState.first()
    assertEquals("2024-12-25", state.date)
    assertEquals("Invalid format (must be dd/MM/yyyy)", state.invalidDateMsg)
  }

  @Test
  fun setDate_invalidDate_setsErrorMessage() = runBlocking {
    viewModel.setDate("not-a-date")

    val state = viewModel.uiState.first()
    assertEquals("not-a-date", state.date)
    assertEquals("Invalid format (must be dd/MM/yyyy)", state.invalidDateMsg)
  }

  /** --- SET VISIBILITY TESTS --- */
  @Test
  fun setVisibility_publicVisibility_updatesStateWithoutError() = runBlocking {
    viewModel.setVisibility("PUBLIC")

    val state = viewModel.uiState.first()
    assertEquals("PUBLIC", state.visibility)
    assertNull(state.invalidVisibilityMsg)
  }

  @Test
  fun setVisibility_privateVisibility_updatesStateWithoutError() = runBlocking {
    viewModel.setVisibility("PRIVATE")

    val state = viewModel.uiState.first()
    assertEquals("PRIVATE", state.visibility)
    assertNull(state.invalidVisibilityMsg)
  }

  @Test
  fun setVisibility_blankVisibility_setsErrorMessage() = runBlocking {
    viewModel.setVisibility("")

    val state = viewModel.uiState.first()
    assertEquals("", state.visibility)
    assertEquals("Event visibility cannot be empty", state.invalidVisibilityMsg)
  }

  @Test
  fun setVisibility_invalidVisibility_setsErrorMessage() = runBlocking {
    viewModel.setVisibility("INVALID")

    val state = viewModel.uiState.first()
    assertEquals("INVALID", state.visibility)
    assertEquals("Visibility must be PUBLIC or PRIVATE", state.invalidVisibilityMsg)
  }

  /** --- IS VALID TESTS --- */
  @Test
  fun isValid_allFieldsValid_returnsTrue() = runBlocking {
    viewModel.setType("SPORTS")
    viewModel.setTitle("Test Event")
    viewModel.setDescription("Test Description")
    viewModel.setLocation("EPFL")
    viewModel.setMaxParticipants("10")
    viewModel.setDuration("60")
    viewModel.setDate("25/12/2024")
    viewModel.setTime("14:30")
    viewModel.setVisibility("PUBLIC")

    val state = viewModel.uiState.first()
    assertTrue(state.isValid)
  }

  @Test
  fun isValid_emptyTitle_returnsFalse() = runBlocking {
    viewModel.setType("SPORTS")
    viewModel.setTitle("")
    viewModel.setDescription("Test Description")
    viewModel.setLocation("EPFL")
    viewModel.setMaxParticipants("10")
    viewModel.setDuration("60")
    viewModel.setDate("25/12/2024 14:30")
    viewModel.setVisibility("PUBLIC")

    val state = viewModel.uiState.first()
    assertFalse(state.isValid)
  }

  @Test
  fun isValid_invalidMaxParticipants_returnsFalse() = runBlocking {
    viewModel.setType("SPORTS")
    viewModel.setTitle("Test Event")
    viewModel.setDescription("Test Description")
    viewModel.setLocation("EPFL")
    viewModel.setMaxParticipants("0")
    viewModel.setDuration("60")
    viewModel.setDate("25/12/2024 14:30")
    viewModel.setVisibility("PUBLIC")

    val state = viewModel.uiState.first()
    assertFalse(state.isValid)
  }

  /** --- EDIT EVENT TESTS --- */
  @Test
  fun editEvent_validData_returnsTrue() = runTest {
    val event = createTestEvent()
    repository.addEvent(event)

    // Load the event first
    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    // Modify some fields
    viewModel.setTitle("Updated Title")

    // Edit the event
    val result = viewModel.editEvent(event.eventId)
    advanceUntilIdle()

    assertTrue(result)

    // Verify the event was updated in the repository
    val updatedEvent = repository.getEvent(event.eventId)
    assertEquals("Updated Title", updatedEvent.title)
  }

  @Test
  fun editEvent_invalidData_returnsFalse() = runTest {
    viewModel.setType("SPORTS")
    viewModel.setTitle("") // Invalid - empty title
    viewModel.setDescription("Test")
    viewModel.setLocation("EPFL")
    viewModel.setMaxParticipants("10")
    viewModel.setDuration("60")
    viewModel.setDate("25/12/2024 14:30")
    viewModel.setVisibility("PUBLIC")

    val result = viewModel.editEvent("test-id")
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("not valid"))
  }

  /** --- DELETE EVENT TESTS --- */
  @Test
  fun deleteEvent_validEventId_deletesEvent() = runTest {
    val event = createTestEvent()
    repository.addEvent(event)

    viewModel.deleteEvent(event.eventId)
    advanceUntilIdle()

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

  // ========== Error Message Display Tests ==========

  @Test
  fun setMaxParticipants_belowCurrentCount_errorMessageDisplayed() = runTest {
    val event = createTestEvent() // Has 2 participants
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    // Try to set max participants below current count
    viewModel.setMaxParticipants("1")

    val state = viewModel.uiState.first()
    // Verify the error message is set and contains the expected text
    assertNotNull(state.invalidMaxParticipantsMsg)
    assertEquals("Cannot be less than current participants (2)", state.invalidMaxParticipantsMsg)
    // Verify the field value is still updated (so UI can show the invalid value)
    assertEquals("1", state.maxParticipants)
    // Verify form is invalid
    assertFalse(state.isValid)
  }

  @Test
  fun setMaxParticipants_equalToCurrentCount_noErrorMessage() = runTest {
    val event = createTestEvent() // Has 2 participants
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    // Set max participants equal to current count
    viewModel.setMaxParticipants("2")

    val state = viewModel.uiState.first()
    // Verify no error message
    assertNull(state.invalidMaxParticipantsMsg)
    assertEquals("2", state.maxParticipants)
  }

  @Test
  fun setType_invalid_errorMessageDisplayed() = runTest {
    viewModel.setType("INVALID_TYPE")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidTypeMsg)
    assertEquals("Type must be one of: SPORTS, ACTIVITY, SOCIAL", state.invalidTypeMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setType_blank_errorMessageDisplayed() = runTest {
    viewModel.setType("")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidTypeMsg)
    assertEquals("Type cannot be empty", state.invalidTypeMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setType_valid_noErrorMessage() = runTest {
    viewModel.setType("SPORTS")

    val state = viewModel.uiState.first()
    assertNull(state.invalidTypeMsg)
    assertEquals("SPORTS", state.type)
  }

  @Test
  fun setDuration_zero_errorMessageDisplayed() = runTest {
    viewModel.setDuration("0")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidDurationMsg)
    assertEquals("Must be a positive number", state.invalidDurationMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setDuration_negative_errorMessageDisplayed() = runTest {
    viewModel.setDuration("-10")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidDurationMsg)
    assertEquals("Must be a positive number", state.invalidDurationMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setDuration_valid_noErrorMessage() = runTest {
    viewModel.setDuration("60")

    val state = viewModel.uiState.first()
    assertNull(state.invalidDurationMsg)
    assertEquals("60", state.duration)
  }

  @Test
  fun setDate_invalidFormat_errorMessageDisplayed() = runTest {
    viewModel.setDate("2024-12-25")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidDateMsg)
    assertEquals("Invalid format (must be dd/MM/yyyy)", state.invalidDateMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setDate_valid_noErrorMessage() = runTest {
    viewModel.setDate("25/12/2024")

    val state = viewModel.uiState.first()
    assertNull(state.invalidDateMsg)
    assertEquals("25/12/2024", state.date)
  }

  @Test
  fun setVisibility_invalid_errorMessageDisplayed() = runTest {
    viewModel.setVisibility("INVALID")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidVisibilityMsg)
    assertEquals("Visibility must be PUBLIC or PRIVATE", state.invalidVisibilityMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setVisibility_blank_errorMessageDisplayed() = runTest {
    viewModel.setVisibility("")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidVisibilityMsg)
    assertEquals("Event visibility cannot be empty", state.invalidVisibilityMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun setVisibility_valid_noErrorMessage() = runTest {
    viewModel.setVisibility("PUBLIC")

    val state = viewModel.uiState.first()
    assertNull(state.invalidVisibilityMsg)
    assertEquals("PUBLIC", state.visibility)
  }

  @Test
  fun multipleValidationErrors_allErrorMessagesDisplayed() = runTest {
    // Set multiple invalid fields
    viewModel.setType("")
    viewModel.setTitle("")
    viewModel.setMaxParticipants("0")
    viewModel.setDuration("-5")
    viewModel.setVisibility("")

    val state = viewModel.uiState.first()

    // Verify all error messages are set
    assertNotNull(state.invalidTypeMsg)
    assertNotNull(state.invalidTitleMsg)
    assertNotNull(state.invalidMaxParticipantsMsg)
    assertNotNull(state.invalidDurationMsg)
    assertNotNull(state.invalidVisibilityMsg)

    // Verify form is invalid
    assertFalse(state.isValid)
  }

  @Test
  fun errorMessageClears_whenFieldBecomesValid() = runTest {
    // First set invalid value
    viewModel.setMaxParticipants("0")
    var state = viewModel.uiState.first()
    assertNotNull(state.invalidMaxParticipantsMsg)

    // Then set valid value
    viewModel.setMaxParticipants("10")
    state = viewModel.uiState.first()
    assertNull(state.invalidMaxParticipantsMsg)
    assertEquals("10", state.maxParticipants)
  }

  @Test
  fun loadEvent_thenSetInvalidMaxParticipants_errorMessageShowsCurrentCount() = runTest {
    // Create event with 5 participants
    val event =
        createTestEvent().copy(participants = listOf("user1", "user2", "user3", "user4", "user5"))
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    // Try to set max to 3 (less than 5 participants)
    viewModel.setMaxParticipants("3")

    val state = viewModel.uiState.first()
    assertNotNull(state.invalidMaxParticipantsMsg)
    // Error message should show the actual current count (5)
    assertEquals("Cannot be less than current participants (5)", state.invalidMaxParticipantsMsg)
  }

  /** --- INITIAL STATE TESTS --- */
  @Test
  fun viewModel_initialState_isEmpty() {
    val state = viewModel.uiState.value
    assertEquals("", state.type)
    assertEquals("", state.title)
    assertEquals("", state.description)
    assertEquals("", state.location)
    assertEquals("", state.maxParticipants)
    assertEquals("", state.duration)
    assertEquals("", state.date)
    assertEquals("", state.visibility)
    assertEquals("", state.ownerId)
    assertTrue(state.participants.isEmpty())
    assertNull(state.errorMsg)
    assertFalse(state.isValid)
  }

  @Test
  fun viewModel_customInitialState_usesProvidedState() {
    val customState =
        EditEventUIState(
            type = "SPORTS",
            title = "Initial Title",
            description = "Initial Description",
            location = "Initial Location",
            maxParticipants = "5",
            duration = "30",
            date = "01/01/2024 10:00",
            visibility = "PRIVATE")

    val customViewModel = EditEventViewModel(repository, customState)

    val state = customViewModel.uiState.value
    assertEquals("SPORTS", state.type)
    assertEquals("Initial Title", state.title)
    assertEquals("Initial Description", state.description)
    assertEquals("Initial Location", state.location)
    assertEquals("5", state.maxParticipants)
    assertEquals("30", state.duration)
    assertEquals("01/01/2024 10:00", state.date)
    assertEquals("PRIVATE", state.visibility)
  }
}
