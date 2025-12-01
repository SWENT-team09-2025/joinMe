package com.android.joinme.ui.overview

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.groups.streaks.StreakService
import com.android.joinme.model.map.Location
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ShowEventViewModelTest {

  private lateinit var repository: EventsRepositoryLocal
  private lateinit var profileRepository: ProfileRepository
  private lateinit var viewModel: ShowEventViewModel
  private val testDispatcher = StandardTestDispatcher()

  // Fake repository that can throw exceptions
  private class FakeEventsRepository : EventsRepository {
    val events = mutableListOf<Event>()
    var shouldThrowOnEdit = false
    var shouldThrowOnDelete = false

    override suspend fun addEvent(event: Event) {
      events += event
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      if (shouldThrowOnEdit) throw Exception("Failed to edit event")
      val index = events.indexOfFirst { it.eventId == eventId }
      if (index >= 0) events[index] = newValue
    }

    override suspend fun deleteEvent(eventId: String) {
      if (shouldThrowOnDelete) throw Exception("Failed to delete event")
      events.removeIf { it.eventId == eventId }
    }

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> =
        events.filter { it.eventId in eventIds }

    override suspend fun getEvent(eventId: String): Event =
        events.find { it.eventId == eventId } ?: throw NoSuchElementException("Event not found")

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> = events.toList()

    override fun getNewEventId(): String = "fake-id-${events.size + 1}"

    override suspend fun getCommonEvents(userIds: List<String>): List<Event> {
      if (userIds.isEmpty()) return emptyList()
      return events
          .filter { event -> userIds.all { userId -> event.participants.contains(userId) } }
          .sortedBy { it.date.toDate().time }
    }
  }

  private fun createTestEvent(
      eventId: String = "test-event-1",
      ownerId: String = "owner123",
      participants: List<String> = listOf("user1", "user2"),
      maxParticipants: Int = 10,
      daysFromNow: Int = 7,
      groupId: String? = null
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
        ownerId = ownerId,
        groupId = groupId)
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = EventsRepositoryLocal()
    profileRepository = mock(ProfileRepository::class.java)
    viewModel = ShowEventViewModel(repository, profileRepository)
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

    val mockProfile = Profile(uid = "owner123", username = "JohnDoe", email = "john@example.com")
    whenever(profileRepository.getProfile("owner123")).thenReturn(mockProfile)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("SPORTS", state.type)
    assertEquals("Basketball Game", state.title)
    assertEquals("Friendly 3v3 basketball match", state.description)
    assertEquals("EPFL", state.location)
    assertEquals("10", state.maxParticipants)
    assertEquals("3", state.participantsCount)
    assertEquals("90", state.duration)
    assertTrue(state.date.contains("SPORTS:"))
    assertEquals("PUBLIC", state.visibility)
    assertEquals("owner123", state.ownerId)
    assertEquals("Created by JohnDoe", state.ownerName)
    assertEquals(listOf("user1", "user2", "owner123"), state.participants)
    assertFalse(state.isPastEvent)
    assertNull(state.serieId)
    assertNull(state.errorMsg)
  }

  @Test
  fun loadEvent_validEventIdWithSerieId_updatesUIStateWithSerieId() = runTest {
    val event = createTestEvent()
    repository.addEvent(event)
    val serieId = "test-serie-123"

    viewModel.loadEvent(event.eventId, serieId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("SPORTS", state.type)
    assertEquals("Basketball Game", state.title)
    assertEquals(serieId, state.serieId)
    assertNull(state.errorMsg)
  }

  @Test
  fun loadEvent_validEventIdWithoutSerieId_serieIdIsNull() = runTest {
    val event = createTestEvent()
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId, null)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.serieId)
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
    val pastEvent = createTestEvent(eventId = "past-event", daysFromNow = -7)
    repository.addEvent(pastEvent)

    val mockProfile = Profile(uid = "owner123", username = "TestUser", email = "test@example.com")
    whenever(profileRepository.getProfile("owner123")).thenReturn(mockProfile)

    viewModel.loadEvent(pastEvent.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.isPastEvent)
  }

  @Test
  fun loadEvent_activeEvent_notMarkedAsPast() = runTest {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MINUTE, -10)

    val activeEvent =
        createTestEvent(eventId = "active-event").copy(date = Timestamp(calendar.time))
    repository.addEvent(activeEvent)

    val mockProfile = Profile(uid = "owner123", username = "TestUser", email = "test@example.com")
    whenever(profileRepository.getProfile("owner123")).thenReturn(mockProfile)

    viewModel.loadEvent(activeEvent.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.isPastEvent)
  }

  @Test
  fun loadEvent_profileNotFound_showsUnknown() = runTest {
    val event = createTestEvent(ownerId = "unknown-owner")
    repository.addEvent(event)

    whenever(profileRepository.getProfile("unknown-owner")).thenReturn(null)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("Created by UNKNOWN", state.ownerName)
  }

  @Test
  fun loadEvent_profileRepositoryThrows_showsUnknown() = runTest {
    val event = createTestEvent(ownerId = "error-owner")
    repository.addEvent(event)

    whenever(profileRepository.getProfile("error-owner"))
        .thenThrow(RuntimeException("Network error"))

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("Created by UNKNOWN", state.ownerName)
  }

  @Test
  fun loadEvent_emptyOwnerId_showsUnknown() = runTest {
    val event = createTestEvent(ownerId = "")
    repository.addEvent(event)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("Created by UNKNOWN", state.ownerName)
  }

  /** --- CLEAR ERROR MESSAGE TESTS --- */
  @Test
  fun clearErrorMsg_removesErrorMessage() = runTest {
    viewModel.loadEvent("non-existent-id")
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)

    viewModel.clearErrorMsg()

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

    val mockProfile =
        Profile(uid = "owner123", username = "EventOwner", email = "owner@example.com")
    whenever(profileRepository.getProfile(any())).thenReturn(mockProfile)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    viewModel.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertTrue(state.participants.contains("user2"))
    assertEquals("3", state.participantsCount)
    assertNull(state.errorMsg)
  }

  @Test
  fun toggleParticipation_userJoins_incrementsEventsJoinedCount() = runTest {
    val event = createTestEvent(participants = listOf("user1"))
    repository.addEvent(event)

    val userProfile =
        Profile(
            uid = "user2",
            username = "NewUser",
            email = "newuser@example.com",
            eventsJoinedCount = 5)
    whenever(profileRepository.getProfile("user2")).thenReturn(userProfile)

    viewModel.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    // Verify that createOrUpdateProfile was called with incremented count
    verify(profileRepository)
        .createOrUpdateProfile(
            check { profile ->
              assertEquals("user2", profile.uid)
              assertEquals(6, profile.eventsJoinedCount)
            })
  }

  @Test
  fun toggleParticipation_userIsParticipant_quitsEvent() = runTest {
    val event = createTestEvent(participants = listOf("user1", "user2"))
    repository.addEvent(event)

    val mockProfile =
        Profile(uid = "owner123", username = "EventOwner", email = "owner@example.com")
    whenever(profileRepository.getProfile(any())).thenReturn(mockProfile)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

    viewModel.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.participants.contains("user2"))
    assertEquals("2", state.participantsCount)
  }

  @Test
  fun toggleParticipation_userQuits_decrementsEventsJoinedCount() = runTest {
    val event = createTestEvent(participants = listOf("user1", "user2"))
    repository.addEvent(event)

    val userProfile =
        Profile(
            uid = "user2",
            username = "ExistingUser",
            email = "existing@example.com",
            eventsJoinedCount = 10)
    whenever(profileRepository.getProfile("user2")).thenReturn(userProfile)

    viewModel.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    // Verify that createOrUpdateProfile was called with decremented count
    verify(profileRepository)
        .createOrUpdateProfile(
            check { profile ->
              assertEquals("user2", profile.uid)
              assertEquals(9, profile.eventsJoinedCount)
            })
  }

  @Test
  fun toggleParticipation_userQuitsWithZeroCount_doesNotGoNegative() = runTest {
    val event = createTestEvent(participants = listOf("user1", "user2"))
    repository.addEvent(event)

    val userProfile =
        Profile(
            uid = "user2",
            username = "UserWithZero",
            email = "zero@example.com",
            eventsJoinedCount = 0)
    whenever(profileRepository.getProfile("user2")).thenReturn(userProfile)

    viewModel.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    // Verify count stays at 0 and doesn't go negative
    verify(profileRepository)
        .createOrUpdateProfile(
            check { profile ->
              assertEquals("user2", profile.uid)
              assertEquals(0, profile.eventsJoinedCount)
            })
  }

  @Test
  fun toggleParticipation_profileUpdateFails_doesNotJoinEvent() = runTest {
    val event = createTestEvent(participants = listOf("user1"))
    repository.addEvent(event)

    // Make profileRepository throw exception when trying to update
    whenever(profileRepository.getProfile("user2"))
        .thenThrow(RuntimeException("Database connection failed"))

    viewModel.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    // Verify event in repository was NOT modified
    val eventInRepo = repository.getEvent(event.eventId)
    assertFalse(eventInRepo.participants.contains("user2"))
    assertEquals(2, eventInRepo.participants.size) // Still only owner and user1

    // Error message should be set
    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to update your profile"))
  }

  @Test
  fun toggleParticipation_eventFull_setsErrorMessage() = runTest {
    val event = createTestEvent(participants = listOf("user1", "user2"), maxParticipants = 2)
    repository.addEvent(event)

    val mockProfile =
        Profile(uid = "owner123", username = "EventOwner", email = "owner@example.com")
    whenever(profileRepository.getProfile(any())).thenReturn(mockProfile)

    viewModel.loadEvent(event.eventId)
    advanceUntilIdle()

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

  @Test
  fun toggleParticipation_profileIsNull_doesNotJoinEvent() = runTest {
    val event = createTestEvent(participants = listOf("user1"))
    repository.addEvent(event)

    whenever(profileRepository.getProfile("user2")).thenReturn(null)

    viewModel.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    // Verify event was NOT modified
    val eventInRepo = repository.getEvent(event.eventId)
    assertFalse(eventInRepo.participants.contains("user2"))

    // Error message should be set
    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to load your profile"))
  }

  /** --- TOGGLE PARTICIPATION STREAK TESTS --- */
  @Test
  fun toggleParticipation_joiningGroupEvent_callsStreakServiceOnActivityJoined() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityJoined(any(), any(), any()) } returns Unit

    val fakeRepo = FakeEventsRepository()
    val event = createTestEvent(participants = listOf("user1"), groupId = "group123")
    fakeRepo.addEvent(event)

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    val userProfile =
        Profile(
            uid = "user2", username = "NewUser", email = "new@example.com", eventsJoinedCount = 5)
    whenever(profileRepository.getProfile("user2")).thenReturn(userProfile)

    vm.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    coVerify { StreakService.onActivityJoined("group123", "user2", event.date) }

    unmockkObject(StreakService)
  }

  @Test
  fun toggleParticipation_quittingGroupEvent_callsStreakServiceOnActivityLeft() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityLeft(any(), any(), any()) } returns Unit

    val fakeRepo = FakeEventsRepository()
    val event = createTestEvent(participants = listOf("user1", "user2"), groupId = "group123")
    fakeRepo.addEvent(event)

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    val userProfile =
        Profile(
            uid = "user2",
            username = "ExistingUser",
            email = "existing@example.com",
            eventsJoinedCount = 10)
    whenever(profileRepository.getProfile("user2")).thenReturn(userProfile)

    vm.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    coVerify { StreakService.onActivityLeft("group123", "user2", event.date) }

    unmockkObject(StreakService)
  }

  @Test
  fun toggleParticipation_joiningNonGroupEvent_doesNotCallStreakService() = runTest {
    mockkObject(StreakService)

    val fakeRepo = FakeEventsRepository()
    val event = createTestEvent(participants = listOf("user1"), groupId = null)
    fakeRepo.addEvent(event)

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    val userProfile =
        Profile(
            uid = "user2", username = "NewUser", email = "new@example.com", eventsJoinedCount = 5)
    whenever(profileRepository.getProfile("user2")).thenReturn(userProfile)

    vm.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    coVerify(exactly = 0) { StreakService.onActivityJoined(any(), any(), any()) }
    coVerify(exactly = 0) { StreakService.onActivityLeft(any(), any(), any()) }

    unmockkObject(StreakService)
  }

  @Test
  fun toggleParticipation_streakServiceThrows_doesNotFailParticipation() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityJoined(any(), any(), any()) } throws
        RuntimeException("Streak error")

    val fakeRepo = FakeEventsRepository()
    val event = createTestEvent(participants = listOf("user1"), groupId = "group123")
    fakeRepo.addEvent(event)

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    val userProfile =
        Profile(
            uid = "user2", username = "NewUser", email = "new@example.com", eventsJoinedCount = 5)
    whenever(profileRepository.getProfile("user2")).thenReturn(userProfile)

    vm.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    // Participation should still succeed despite streak error
    val updatedEvent = fakeRepo.getEvent(event.eventId)
    assertTrue(updatedEvent.participants.contains("user2"))

    unmockkObject(StreakService)
  }

  /** --- DELETE EVENT TESTS --- */
  @Test
  fun deleteEvent_validEventId_deletesEvent() = runTest {
    val event = createTestEvent(participants = listOf("user1", "user2"))
    repository.addEvent(event)

    val profile1 =
        Profile(
            uid = "user1", username = "User1", email = "user1@example.com", eventsJoinedCount = 5)
    val profile2 =
        Profile(
            uid = "user2", username = "User2", email = "user2@example.com", eventsJoinedCount = 3)
    val ownerProfile =
        Profile(
            uid = "owner123",
            username = "Owner",
            email = "owner@example.com",
            eventsJoinedCount = 10)
    // Event includes owner in participants
    whenever(profileRepository.getProfilesByIds(listOf("user1", "user2", "owner123")))
        .thenReturn(listOf(profile1, profile2, ownerProfile))

    viewModel.deleteEvent(event.eventId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.errorMsg)

    try {
      repository.getEvent(event.eventId)
      fail("Expected exception when getting deleted event")
    } catch (_: Exception) {
      // Expected
    }
  }

  @Test
  fun deleteEvent_decrementsEventsJoinedCountForAllParticipants() = runTest {
    val event = createTestEvent(participants = listOf("user1", "user2", "user3"))
    repository.addEvent(event)

    val profile1 =
        Profile(
            uid = "user1", username = "User1", email = "user1@example.com", eventsJoinedCount = 5)
    val profile2 =
        Profile(
            uid = "user2", username = "User2", email = "user2@example.com", eventsJoinedCount = 3)
    val profile3 =
        Profile(
            uid = "user3", username = "User3", email = "user3@example.com", eventsJoinedCount = 10)
    val ownerProfile =
        Profile(
            uid = "owner123",
            username = "Owner",
            email = "owner@example.com",
            eventsJoinedCount = 7)

    // Event includes owner in participants
    whenever(profileRepository.getProfilesByIds(listOf("user1", "user2", "user3", "owner123")))
        .thenReturn(listOf(profile1, profile2, profile3, ownerProfile))

    viewModel.deleteEvent(event.eventId)
    advanceUntilIdle()

    // Verify all participants had their count decremented (4 total including owner)
    verify(profileRepository, times(4)).createOrUpdateProfile(any())
  }

  @Test
  fun deleteEvent_noParticipants_deletesEventDirectly() = runTest {
    val fakeRepo = FakeEventsRepository()
    // Create event with no participants (owner will be auto-added, so use empty list)
    val event =
        Event(
            eventId = "empty-event",
            type = EventType.SPORTS,
            title = "Empty Event",
            description = "No participants",
            location = Location(46.5197, 6.6323, "EPFL"),
            date = Timestamp(Date()),
            duration = 90,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner123")
    fakeRepo.addEvent(event)

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    vm.deleteEvent(event.eventId)
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertNull(state.errorMsg)

    // Event should be deleted
    assertNull(fakeRepo.events.find { it.eventId == event.eventId })

    // No profile updates should have happened
    verify(profileRepository, never()).createOrUpdateProfile(any())
  }

  /** --- DELETE EVENT STREAK TESTS --- */
  @Test
  fun deleteEvent_upcomingGroupEvent_callsStreakServiceOnActivityDeleted() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityDeleted(any(), any(), any()) } returns Unit

    val fakeRepo = FakeEventsRepository()
    val event =
        createTestEvent(
            participants = listOf("user1", "user2"), groupId = "group123", daysFromNow = 7)
    fakeRepo.addEvent(event)

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    val profile1 =
        Profile(uid = "user1", username = "User1", email = "u1@example.com", eventsJoinedCount = 5)
    val profile2 =
        Profile(uid = "user2", username = "User2", email = "u2@example.com", eventsJoinedCount = 3)
    // Note: participants list is ["user1", "user2"] - no owner auto-added in test
    whenever(profileRepository.getProfilesByIds(listOf("user1", "user2")))
        .thenReturn(listOf(profile1, profile2))

    vm.deleteEvent(event.eventId)
    advanceUntilIdle()

    coVerify { StreakService.onActivityDeleted("group123", listOf("user1", "user2"), event.date) }

    unmockkObject(StreakService)
  }

  @Test
  fun deleteEvent_pastGroupEvent_doesNotCallStreakService() = runTest {
    mockkObject(StreakService)

    val fakeRepo = FakeEventsRepository()
    val event =
        createTestEvent(
            participants = listOf("user1", "user2"), groupId = "group123", daysFromNow = -7)
    fakeRepo.addEvent(event)

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    val profile1 =
        Profile(uid = "user1", username = "User1", email = "u1@example.com", eventsJoinedCount = 5)
    val profile2 =
        Profile(uid = "user2", username = "User2", email = "u2@example.com", eventsJoinedCount = 3)
    whenever(profileRepository.getProfilesByIds(listOf("user1", "user2")))
        .thenReturn(listOf(profile1, profile2))

    vm.deleteEvent(event.eventId)
    advanceUntilIdle()

    coVerify(exactly = 0) { StreakService.onActivityDeleted(any(), any(), any()) }

    unmockkObject(StreakService)
  }

  @Test
  fun deleteEvent_nonGroupEvent_doesNotCallStreakService() = runTest {
    mockkObject(StreakService)

    val fakeRepo = FakeEventsRepository()
    val event =
        createTestEvent(participants = listOf("user1", "user2"), groupId = null, daysFromNow = 7)
    fakeRepo.addEvent(event)

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    val profile1 =
        Profile(uid = "user1", username = "User1", email = "u1@example.com", eventsJoinedCount = 5)
    val profile2 =
        Profile(uid = "user2", username = "User2", email = "u2@example.com", eventsJoinedCount = 3)
    whenever(profileRepository.getProfilesByIds(listOf("user1", "user2")))
        .thenReturn(listOf(profile1, profile2))

    vm.deleteEvent(event.eventId)
    advanceUntilIdle()

    coVerify(exactly = 0) { StreakService.onActivityDeleted(any(), any(), any()) }

    unmockkObject(StreakService)
  }

  @Test
  fun deleteEvent_streakServiceThrows_doesNotFailDeletion() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityDeleted(any(), any(), any()) } throws
        RuntimeException("Streak error")

    val fakeRepo = FakeEventsRepository()
    val event =
        createTestEvent(
            participants = listOf("user1", "user2"), groupId = "group123", daysFromNow = 7)
    fakeRepo.addEvent(event)

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    val profile1 =
        Profile(uid = "user1", username = "User1", email = "u1@example.com", eventsJoinedCount = 5)
    val profile2 =
        Profile(uid = "user2", username = "User2", email = "u2@example.com", eventsJoinedCount = 3)
    whenever(profileRepository.getProfilesByIds(listOf("user1", "user2")))
        .thenReturn(listOf(profile1, profile2))

    vm.deleteEvent(event.eventId)
    advanceUntilIdle()

    // Event should still be deleted despite streak error
    assertTrue(fakeRepo.events.none { it.eventId == event.eventId })

    unmockkObject(StreakService)
  }

  /** --- ERROR HANDLING TESTS --- */
  @Test
  fun toggleParticipation_editEventFails_rollsBackProfileAndSetsError() = runTest {
    val fakeRepo = FakeEventsRepository()
    val event = createTestEvent(participants = listOf("user1"))
    fakeRepo.addEvent(event)
    fakeRepo.shouldThrowOnEdit = true

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    val userProfile =
        Profile(
            uid = "user2",
            username = "NewUser",
            email = "newuser@example.com",
            eventsJoinedCount = 5)
    whenever(profileRepository.getProfile("user2")).thenReturn(userProfile)

    vm.toggleParticipation(event.eventId, "user2")
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to update participation"))

    // Verify profile was rolled back (createOrUpdateProfile called twice: once to update, once to
    // rollback)
    verify(profileRepository, times(2)).createOrUpdateProfile(any())
  }

  @Test
  fun deleteEvent_deleteEventFails_rollsBackProfilesAndSetsError() = runTest {
    val fakeRepo = FakeEventsRepository()
    val event = createTestEvent(participants = listOf("user1", "user2"))
    fakeRepo.addEvent(event)
    fakeRepo.shouldThrowOnDelete = true

    val vm = ShowEventViewModel(fakeRepo, profileRepository)

    val profile1 =
        Profile(
            uid = "user1", username = "User1", email = "user1@example.com", eventsJoinedCount = 5)
    val profile2 =
        Profile(
            uid = "user2", username = "User2", email = "user2@example.com", eventsJoinedCount = 3)

    // FakeEventsRepository doesn't auto-add owner, so only mock for user1 and user2
    whenever(profileRepository.getProfilesByIds(listOf("user1", "user2")))
        .thenReturn(listOf(profile1, profile2))

    vm.deleteEvent(event.eventId)
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertNotNull(state.errorMsg)

    // Profiles were updated (2) then rolled back (2) = 4 calls
    verify(profileRepository, times(4)).createOrUpdateProfile(any())

    // Event should still exist since delete failed
    assertNotNull(fakeRepo.events.find { it.eventId == event.eventId })
  }
}
