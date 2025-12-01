package com.android.joinme.ui.overview

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.groups.streaks.StreakService
import com.android.joinme.model.map.Location
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.utils.Visibility
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
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SerieDetailsViewModelTest {

  // Fake repositories for testing
  private open class FakeSeriesRepository : SeriesRepository {
    private val series = mutableMapOf<String, Serie>()

    override suspend fun addSerie(serie: Serie) {
      series[serie.serieId] = serie
    }

    override suspend fun editSerie(serieId: String, newValue: Serie) {
      series[serieId] = newValue
    }

    override suspend fun deleteSerie(serieId: String) {
      series.remove(serieId)
    }

    override suspend fun getSerie(serieId: String): Serie =
        series[serieId] ?: throw NoSuchElementException("Serie not found")

    override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> =
        series.values.toList()

    override suspend fun getSeriesByIds(seriesIds: List<String>): List<Serie> {
      return series.filter { seriesIds.contains(it.key) }.values.toList()
    }

    override fun getNewSerieId(): String = "new-serie-id"
  }

  private open class FakeEventsRepository : EventsRepository {
    private val events = mutableMapOf<String, Event>()

    override suspend fun addEvent(event: Event) {
      events[event.eventId] = event
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      events[eventId] = newValue
    }

    override suspend fun deleteEvent(eventId: String) {
      events.remove(eventId)
    }

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
      return events.filter { eventIds.contains(it.key) }.values.toList()
    }

    override suspend fun getEvent(eventId: String): Event =
        events[eventId] ?: throw NoSuchElementException("Event not found")

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> =
        events.values.toList()

    override fun getNewEventId(): String = "new-event-id"

    override suspend fun getCommonEvents(userIds: List<String>): List<Event> {
      if (userIds.isEmpty()) return emptyList()
      return events.values
          .filter { event -> userIds.all { userId -> event.participants.contains(userId) } }
          .sortedBy { it.date.toDate().time }
    }
  }

  private lateinit var seriesRepository: FakeSeriesRepository
  private lateinit var eventsRepository: FakeEventsRepository
  private lateinit var profileRepository: ProfileRepository
  private lateinit var viewModel: SerieDetailsViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val testUserId = "test-user-id"

  private fun createTestSerie(
      serieId: String = "test-serie-1",
      title: String = "Weekly Basketball",
      ownerId: String = "owner123",
      participants: List<String> = listOf("user1", "user2", "owner123"),
      maxParticipants: Int = 10,
      eventIds: List<String> = listOf("event1", "event2"),
      groupId: String? = null,
      daysFromNow: Int = 7
  ): Serie {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, daysFromNow)
    calendar.set(Calendar.HOUR_OF_DAY, 18)
    calendar.set(Calendar.MINUTE, 30)
    calendar.set(Calendar.SECOND, 0)

    return Serie(
        serieId = serieId,
        title = title,
        description = "Weekly basketball games every Friday",
        date = Timestamp(calendar.time),
        participants = participants,
        maxParticipants = maxParticipants,
        visibility = Visibility.PUBLIC,
        eventIds = eventIds,
        ownerId = ownerId,
        groupId = groupId)
  }

  private fun createTestEvent(
      eventId: String = "event1",
      ownerId: String = "owner123",
      duration: Int = 90
  ): Event {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 15, 18, 30, 0)

    return Event(
        eventId = eventId,
        type = EventType.SPORTS,
        title = "Basketball Match",
        description = "Friendly basketball match",
        location = Location(46.5197, 6.6323, "EPFL"),
        date = Timestamp(calendar.time),
        duration = duration,
        participants = listOf("user1", "user2"),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = ownerId)
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    seriesRepository = FakeSeriesRepository()
    eventsRepository = FakeEventsRepository()
    profileRepository = mock(ProfileRepository::class.java)
    viewModel = SerieDetailsViewModel(seriesRepository, eventsRepository, profileRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** --- INITIAL STATE TESTS --- */
  @Test
  fun viewModel_initialState_hasCorrectDefaults() {
    val state = viewModel.uiState.value
    assertNull(state.serie)
    assertTrue(state.events.isEmpty())
    assertTrue(state.isLoading)
    assertNull(state.errorMsg)
  }

  /** --- LOAD SERIE DETAILS TESTS --- */
  @Test
  fun loadSerieDetails_validSerieId_updatesState() = runTest {
    val serie = createTestSerie()
    val event1 = createTestEvent(eventId = "event1", duration = 90)
    val event2 = createTestEvent(eventId = "event2", duration = 60)

    seriesRepository.addSerie(serie)
    eventsRepository.addEvent(event1)
    eventsRepository.addEvent(event2)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(state.serie)
    assertEquals("Weekly Basketball", state.serie?.title)
    assertEquals(2, state.events.size)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
  }

  @Test
  fun loadSerieDetails_invalidSerieId_setsError() = runTest {
    viewModel.loadSerieDetails("non-existent-id")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.serie)
    assertFalse(state.isLoading)
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to load serie"))
  }

  /** --- UI STATE COMPUTED PROPERTIES TESTS --- */
  @Test
  fun uiState_isOwner_returnsTrueWhenUserIsOwner() {
    val serie = createTestSerie(ownerId = "user123")
    val state = SerieDetailsUIState(serie = serie, isLoading = false)
    assertTrue(state.isOwner("user123"))
  }

  @Test
  fun uiState_isOwner_returnsFalseWhenUserIsNotOwner() {
    val serie = createTestSerie(ownerId = "owner123")
    val state = SerieDetailsUIState(serie = serie, isLoading = false)
    assertFalse(state.isOwner("user456"))
  }

  @Test
  fun uiState_isParticipant_returnsTrueWhenUserIsParticipant() {
    val serie = createTestSerie(participants = listOf("user1", "user2", "user3"))
    val state = SerieDetailsUIState(serie = serie, isLoading = false)
    assertTrue(state.isParticipant("user2"))
  }

  @Test
  fun uiState_isParticipant_returnsFalseWhenUserIsNotParticipant() {
    val serie = createTestSerie(participants = listOf("user1", "user2"))
    val state = SerieDetailsUIState(serie = serie, isLoading = false)
    assertFalse(state.isParticipant("user3"))
  }

  @Test
  fun uiState_canJoin_returnsTrueWhenUserCanJoin() {
    val serie =
        createTestSerie(
            ownerId = "owner123", participants = listOf("user1", "user2"), maxParticipants = 10)
    val state = SerieDetailsUIState(serie = serie, isLoading = false)
    assertTrue(state.canJoin(testUserId))
  }

  @Test
  fun uiState_canJoin_returnsFalseWhenSerieFull() {
    val serie =
        createTestSerie(
            ownerId = "owner123", participants = listOf("user1", "user2"), maxParticipants = 2)
    val state = SerieDetailsUIState(serie = serie, isLoading = false)
    assertFalse(state.canJoin(testUserId))
  }

  /** --- JOIN SERIE TESTS --- */
  @Test
  fun joinSerie_successfulJoin_returnsTrue() = runTest {
    val serie =
        createTestSerie(
            participants = listOf("user1"), maxParticipants = 10, eventIds = emptyList())
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertTrue(result)

    val state = viewModel.uiState.first()
    assertTrue(state.serie!!.participants.contains(testUserId))
    assertNull(state.errorMsg)
  }

  @Test
  fun joinSerie_serieNotLoaded_returnsFalse() = runTest {
    val result = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Serie not loaded"))
  }

  @Test
  fun joinSerie_userIsOwner_returnsFalse() = runTest {
    val serie = createTestSerie(ownerId = testUserId, participants = listOf("user1"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("owner of this serie"))
  }

  @Test
  fun joinSerie_userAlreadyParticipant_returnsFalse() = runTest {
    val serie = createTestSerie(participants = listOf(testUserId, "user1"), maxParticipants = 10)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("already a participant"))
  }

  @Test
  fun joinSerie_serieIsFull_returnsFalse() = runTest {
    val serie = createTestSerie(participants = listOf("user1", "user2"), maxParticipants = 2)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Serie is full"))
  }

  /** --- JOIN SERIE STREAK TESTS --- */
  @Test
  fun joinSerie_groupSerie_callsStreakServiceOnActivityJoined() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityJoined(any(), any(), any()) } returns Unit

    val serie =
        createTestSerie(
            participants = listOf("user1"),
            maxParticipants = 10,
            eventIds = emptyList(),
            groupId = "group123")
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    coVerify { StreakService.onActivityJoined("group123", testUserId, serie.date) }

    unmockkObject(StreakService)
  }

  @Test
  fun joinSerie_nonGroupSerie_doesNotCallStreakService() = runTest {
    mockkObject(StreakService)

    val serie =
        createTestSerie(
            participants = listOf("user1"),
            maxParticipants = 10,
            eventIds = emptyList(),
            groupId = null)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    coVerify(exactly = 0) { StreakService.onActivityJoined(any(), any(), any()) }

    unmockkObject(StreakService)
  }

  @Test
  fun joinSerie_streakServiceThrows_doesNotFailJoin() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityJoined(any(), any(), any()) } throws
        RuntimeException("Streak error")

    val serie =
        createTestSerie(
            participants = listOf("user1"),
            maxParticipants = 10,
            eventIds = emptyList(),
            groupId = "group123")
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    // Join should still succeed despite streak error
    assertTrue(result)
    val state = viewModel.uiState.first()
    assertTrue(state.serie!!.participants.contains(testUserId))

    unmockkObject(StreakService)
  }

  /** --- QUIT SERIE TESTS --- */
  @Test
  fun quitSerie_regularParticipant_quitsSuccessfully() = runTest {
    val serie =
        createTestSerie(
            ownerId = "owner123",
            participants = listOf(testUserId, "user1", "owner123"),
            eventIds = emptyList())
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    assertTrue(result)

    val state = viewModel.uiState.first()
    assertFalse(state.serie!!.participants.contains(testUserId))
    assertNull(state.errorMsg)
  }

  @Test
  fun quitSerie_serieNotLoaded_returnsFalse() = runTest {
    val result = viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Serie not loaded"))
  }

  @Test
  fun quitSerie_ownerCannotQuit_returnsFalse() = runTest {
    val serie = createTestSerie(ownerId = testUserId, participants = listOf(testUserId, "user1"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("owner of this serie"))
  }

  @Test
  fun quitSerie_notParticipant_returnsFalse() = runTest {
    val serie = createTestSerie(ownerId = "owner123", participants = listOf("user1", "owner123"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("not a participant"))
  }

  /** --- QUIT SERIE STREAK TESTS --- */
  @Test
  fun quitSerie_groupSerie_callsStreakServiceOnActivityLeft() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityLeft(any(), any(), any()) } returns Unit

    val serie =
        createTestSerie(
            ownerId = "owner123",
            participants = listOf(testUserId, "user1", "owner123"),
            eventIds = emptyList(),
            groupId = "group123")
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    coVerify { StreakService.onActivityLeft("group123", testUserId, serie.date) }

    unmockkObject(StreakService)
  }

  @Test
  fun quitSerie_nonGroupSerie_doesNotCallStreakService() = runTest {
    mockkObject(StreakService)

    val serie =
        createTestSerie(
            ownerId = "owner123",
            participants = listOf(testUserId, "user1", "owner123"),
            eventIds = emptyList(),
            groupId = null)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    coVerify(exactly = 0) { StreakService.onActivityLeft(any(), any(), any()) }

    unmockkObject(StreakService)
  }

  @Test
  fun quitSerie_streakServiceThrows_doesNotFailQuit() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityLeft(any(), any(), any()) } throws
        RuntimeException("Streak error")

    val serie =
        createTestSerie(
            ownerId = "owner123",
            participants = listOf(testUserId, "user1", "owner123"),
            eventIds = emptyList(),
            groupId = "group123")
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    // Quit should still succeed despite streak error
    assertTrue(result)
    val state = viewModel.uiState.first()
    assertFalse(state.serie!!.participants.contains(testUserId))

    unmockkObject(StreakService)
  }

  /** --- DELETE SERIE TESTS --- */
  @Test
  fun deleteSerie_validSerieId_deletesSerie() = runTest {
    val serie = createTestSerie()
    seriesRepository.addSerie(serie)

    viewModel.deleteSerie(serie.serieId)
    advanceUntilIdle()

    try {
      seriesRepository.getSerie(serie.serieId)
      fail("Expected exception when getting deleted serie")
    } catch (_: Exception) {
      // Expected
    }
  }

  @Test
  fun deleteSerie_invalidSerieId_setsError() = runTest {
    viewModel.deleteSerie("non-existent-id")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to delete serie"))
  }

  /** --- DELETE SERIE STREAK TESTS --- */
  @Test
  fun deleteSerie_upcomingGroupSerie_callsStreakServiceOnActivityDeleted() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityDeleted(any(), any(), any()) } returns Unit

    val serie =
        createTestSerie(
            participants = listOf("user1", "user2", "owner123"),
            groupId = "group123",
            daysFromNow = 7)
    seriesRepository.addSerie(serie)

    viewModel.deleteSerie(serie.serieId)
    advanceUntilIdle()

    coVerify {
      StreakService.onActivityDeleted("group123", listOf("user1", "user2", "owner123"), serie.date)
    }

    unmockkObject(StreakService)
  }

  @Test
  fun deleteSerie_pastGroupSerie_doesNotCallStreakService() = runTest {
    mockkObject(StreakService)

    val serie =
        createTestSerie(
            participants = listOf("user1", "user2", "owner123"),
            groupId = "group123",
            daysFromNow = -7)
    seriesRepository.addSerie(serie)

    viewModel.deleteSerie(serie.serieId)
    advanceUntilIdle()

    coVerify(exactly = 0) { StreakService.onActivityDeleted(any(), any(), any()) }

    unmockkObject(StreakService)
  }

  @Test
  fun deleteSerie_nonGroupSerie_doesNotCallStreakService() = runTest {
    mockkObject(StreakService)

    val serie =
        createTestSerie(
            participants = listOf("user1", "user2", "owner123"), groupId = null, daysFromNow = 7)
    seriesRepository.addSerie(serie)

    viewModel.deleteSerie(serie.serieId)
    advanceUntilIdle()

    coVerify(exactly = 0) { StreakService.onActivityDeleted(any(), any(), any()) }

    unmockkObject(StreakService)
  }

  @Test
  fun deleteSerie_streakServiceThrows_doesNotFailDeletion() = runTest {
    mockkObject(StreakService)
    coEvery { StreakService.onActivityDeleted(any(), any(), any()) } throws
        RuntimeException("Streak error")

    val serie =
        createTestSerie(
            participants = listOf("user1", "user2", "owner123"),
            groupId = "group123",
            daysFromNow = 7)
    seriesRepository.addSerie(serie)

    viewModel.deleteSerie(serie.serieId)
    advanceUntilIdle()

    // Serie should still be deleted despite streak error
    try {
      seriesRepository.getSerie(serie.serieId)
      fail("Expected exception when getting deleted serie")
    } catch (_: Exception) {
      // Expected
    }

    unmockkObject(StreakService)
  }

  /** --- ERROR MESSAGE HANDLING TESTS --- */
  @Test
  fun setErrorMsg_setsErrorMessageInState() {
    viewModel.setErrorMsg("Test error message")

    val state = viewModel.uiState.value
    assertEquals("Test error message", state.errorMsg)
  }

  @Test
  fun clearErrorMsg_removesErrorMessage() = runTest {
    viewModel.setErrorMsg("Test error")
    var state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)

    viewModel.clearErrorMsg()

    state = viewModel.uiState.first()
    assertNull(state.errorMsg)
  }

  /** --- GET OWNER DISPLAY NAME TESTS --- */
  @Test
  fun getOwnerDisplayName_withValidOwnerId_returnsUsername() = runTest {
    val mockProfile = Profile(uid = "owner123", username = "JohnDoe", email = "john@example.com")
    whenever(profileRepository.getProfile("owner123")).thenReturn(mockProfile)

    val displayName = viewModel.getOwnerDisplayName("owner123")

    assertEquals("JohnDoe", displayName)
  }

  @Test
  fun getOwnerDisplayName_withProfileNotFound_returnsUnknown() = runTest {
    whenever(profileRepository.getProfile("unknown-owner")).thenReturn(null)

    val displayName = viewModel.getOwnerDisplayName("unknown-owner")

    assertEquals("UNKNOWN", displayName)
  }

  @Test
  fun getOwnerDisplayName_withEmptyOwnerId_returnsUnknown() = runTest {
    val displayName = viewModel.getOwnerDisplayName("")

    assertEquals("UNKNOWN", displayName)
  }

  @Test
  fun getOwnerDisplayName_withRepositoryError_returnsUnknown() = runTest {
    whenever(profileRepository.getProfile("error-owner"))
        .thenThrow(RuntimeException("Network error"))

    val displayName = viewModel.getOwnerDisplayName("error-owner")

    assertEquals("UNKNOWN", displayName)
  }
}
