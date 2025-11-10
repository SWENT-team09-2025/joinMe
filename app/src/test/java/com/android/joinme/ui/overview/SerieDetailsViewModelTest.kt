package com.android.joinme.ui.overview

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.map.Location
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.utils.Visibility
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

    override suspend fun getEvent(eventId: String): Event =
        events[eventId] ?: throw NoSuchElementException("Event not found")

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> =
        events.values.toList()

    override fun getNewEventId(): String = "new-event-id"
  }

  private lateinit var seriesRepository: FakeSeriesRepository
  private lateinit var eventsRepository: FakeEventsRepository
  private lateinit var viewModel: SerieDetailsViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val testUserId = "test-user-id"

  private fun createTestSerie(
      serieId: String = "test-serie-1",
      title: String = "Weekly Basketball",
      ownerId: String = "owner123",
      participants: List<String> = listOf("user1", "user2", "owner123"),
      maxParticipants: Int = 10,
      eventIds: List<String> = listOf("event1", "event2")
  ): Serie {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 15, 18, 30, 0)

    return Serie(
        serieId = serieId,
        title = title,
        description = "Weekly basketball games every Friday",
        date = Timestamp(calendar.time),
        participants = participants,
        maxParticipants = maxParticipants,
        visibility = Visibility.PUBLIC,
        eventIds = eventIds,
        ownerId = ownerId)
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
    viewModel = SerieDetailsViewModel(seriesRepository, eventsRepository)
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

  /** --- SERIE DETAILS UI STATE COMPUTED PROPERTIES TESTS --- */
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
  fun uiState_isOwner_returnsFalseWhenCurrentUserIdIsNull() {
    val serie = createTestSerie(ownerId = "owner123")
    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    assertFalse(state.isOwner(null))
  }

  @Test
  fun uiState_isOwner_returnsFalseWhenSerieIsNull() {
    val state = SerieDetailsUIState(serie = null, isLoading = false)

    assertFalse(state.isOwner("user123"))
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
  fun uiState_isParticipant_returnsFalseWhenSerieIsNull() {
    val state = SerieDetailsUIState(serie = null, isLoading = false)

    assertFalse(state.isParticipant("user1"))
  }

  @Test
  fun uiState_isParticipant_returnsFalseWhenCurrentUserIdIsNull() {
    val serie = createTestSerie(participants = listOf("user1", "user2"))
    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    assertFalse(state.isParticipant(null))
  }

  @Test
  fun uiState_canJoin_returnsTrueWhenUserCanJoin() {
    val serie =
        createTestSerie(
            ownerId = "owner123", participants = listOf("user1", "user2"), maxParticipants = 10)
    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    assertTrue(state.canJoin("user3"))
  }

  @Test
  fun uiState_canJoin_returnsFalseWhenUserIsOwner() {
    val serie =
        createTestSerie(ownerId = "owner123", participants = listOf("user1"), maxParticipants = 10)
    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    assertFalse(state.canJoin("owner123"))
  }

  @Test
  fun uiState_canJoin_returnsFalseWhenUserIsAlreadyParticipant() {
    val serie =
        createTestSerie(
            ownerId = "owner123", participants = listOf("user1", "user2"), maxParticipants = 10)
    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    assertFalse(state.canJoin("user1"))
  }

  @Test
  fun uiState_canJoin_returnsFalseWhenSerieIsFull() {
    val serie =
        createTestSerie(
            ownerId = "owner123", participants = listOf("user1", "user2"), maxParticipants = 2)
    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    assertFalse(state.canJoin("user3"))
  }

  @Test
  fun uiState_canJoin_returnsFalseWhenSerieIsNull() {
    val state = SerieDetailsUIState(serie = null, isLoading = false)

    assertFalse(state.canJoin("user1"))
  }

  @Test
  fun uiState_formattedDateTime_returnsFormattedDate() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 15, 18, 30, 0)
    val serie = createTestSerie(serieId = "test-serie-1")
    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    val formatted = state.formattedDateTime
    assertTrue(formatted.contains("15/01/2025"))
    assertTrue(formatted.contains("18:30"))
  }

  @Test
  fun uiState_formattedDateTime_returnsEmptyWhenSerieIsNull() {
    val state = SerieDetailsUIState(serie = null, isLoading = false)

    assertEquals("", state.formattedDateTime)
  }

  @Test
  fun uiState_formattedDuration_calculatesTotalDuration() {
    val calendar = Calendar.getInstance()
    val startDate = calendar.time
    calendar.add(Calendar.MINUTE, 210) // 3.5 hours = 210 minutes
    val endDate = calendar.time

    val serie =
        Serie(
            serieId = "test-serie-1",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = listOf("user1", "user2"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    val formatted = state.formattedDuration
    // Total: 210 minutes = 3h 30min
    assertTrue(formatted.contains("3h") && formatted.contains("30min"))
  }

  @Test
  fun uiState_formattedDuration_returnsZeroWhenSerieIsNull() {
    val state = SerieDetailsUIState(serie = null, isLoading = false)

    assertEquals("0min", state.formattedDuration)
  }

  @Test
  fun uiState_formattedDuration_returnsZeroWhenLastEventEndTimeEqualsDate() {
    val serie = createTestSerie()
    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    assertEquals("0min", state.formattedDuration)
  }

  @Test
  fun uiState_participantsCount_returnsCorrectFormat() {
    val serie =
        createTestSerie(participants = listOf("user1", "user2", "user3"), maxParticipants = 10)
    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    assertEquals("3/10", state.participantsCount)
  }

  @Test
  fun uiState_participantsCount_returnsZeroWhenSerieIsNull() {
    val state = SerieDetailsUIState(serie = null, isLoading = false)

    assertEquals("0/0", state.participantsCount)
  }

  @Test
  fun uiState_visibilityDisplay_returnsPublic() {
    val serie = createTestSerie()
    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    assertEquals("PUBLIC", state.visibilityDisplay)
  }

  @Test
  fun uiState_visibilityDisplay_returnsPrivate() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 15, 18, 30, 0)

    val serie =
        Serie(
            serieId = "test-serie-1",
            title = "Private Serie",
            description = "Private test",
            date = Timestamp(calendar.time),
            participants = listOf("user1", "user2"),
            maxParticipants = 10,
            visibility = Visibility.PRIVATE,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner123")

    val state = SerieDetailsUIState(serie = serie, isLoading = false)

    assertEquals("PRIVATE", state.visibilityDisplay)
  }

  @Test
  fun uiState_visibilityDisplay_returnsPublicWhenSerieIsNull() {
    val state = SerieDetailsUIState(serie = null, isLoading = false)

    assertEquals("PUBLIC", state.visibilityDisplay)
  }

  /** --- LOAD SERIE DETAILS TESTS --- */
  @Test
  fun loadSerieDetails_validSerieId_updatesUIState() = runTest {
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
  fun loadSerieDetails_invalidSerieId_setsErrorMessage() = runTest {
    viewModel.loadSerieDetails("non-existent-id")
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNull(state.serie)
    assertTrue(state.events.isEmpty())
    assertFalse(state.isLoading)
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to load serie"))
  }

  @Test
  fun loadSerieDetails_repositoryError_setsErrorMessage() = runTest {
    val errorRepository =
        object : FakeSeriesRepository() {
          override suspend fun getSerie(serieId: String): Serie {
            throw Exception("Database error")
          }
        }

    val errorViewModel = SerieDetailsViewModel(errorRepository, eventsRepository)

    errorViewModel.loadSerieDetails("test-serie-1")
    advanceUntilIdle()

    val state = errorViewModel.uiState.first()
    assertNull(state.serie)
    assertFalse(state.isLoading)
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to load serie"))
    assertTrue(state.errorMsg!!.contains("Database error"))
  }

  @Test
  fun loadSerieDetails_setsLoadingStateDuringLoad() = runTest {
    val serie = createTestSerie()
    seriesRepository.addSerie(serie)

    // Initial loading state should be true
    var state = viewModel.uiState.value
    assertTrue(state.isLoading)

    viewModel.loadSerieDetails(serie.serieId)

    // After loading completes, should be false
    advanceUntilIdle()

    state = viewModel.uiState.first()
    assertFalse(state.isLoading)
  }

  @Test
  fun loadSerieDetails_filtersEventsCorrectly() = runTest {
    val serie = createTestSerie(eventIds = listOf("event1", "event2"))
    val event1 = createTestEvent(eventId = "event1")
    val event2 = createTestEvent(eventId = "event2")
    val event3 = createTestEvent(eventId = "event3") // Not in serie

    seriesRepository.addSerie(serie)
    eventsRepository.addEvent(event1)
    eventsRepository.addEvent(event2)
    eventsRepository.addEvent(event3)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals(2, state.events.size)
    assertTrue(state.events.any { it.eventId == "event1" })
    assertTrue(state.events.any { it.eventId == "event2" })
    assertFalse(state.events.any { it.eventId == "event3" })
  }

  @Test
  fun loadSerieDetails_withNoEvents_returnsEmptyEventList() = runTest {
    val serie = createTestSerie(eventIds = emptyList())
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(state.serie)
    assertTrue(state.events.isEmpty())
    assertNull(state.errorMsg)
  }

  @Test
  fun loadSerieDetails_eventsRepositoryError_setsErrorMessage() = runTest {
    val serie = createTestSerie()
    seriesRepository.addSerie(serie)

    val errorEventsRepository =
        object : FakeEventsRepository() {
          override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> {
            throw Exception("Events fetch error")
          }
        }

    val errorViewModel = SerieDetailsViewModel(seriesRepository, errorEventsRepository)

    errorViewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val state = errorViewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to load serie"))
  }

  /** --- JOIN SERIE TESTS --- */
  @Test
  fun joinSerie_validUser_joinsSuccessfully() = runTest {
    val serie = createTestSerie(participants = listOf("user1", "owner123"), maxParticipants = 10)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertTrue(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.serie)
    assertTrue(state.serie!!.participants.contains(testUserId))
    assertEquals(3, state.serie!!.participants.size)
    assertNull(state.errorMsg)

    // Verify repository was updated
    val updatedSerie = seriesRepository.getSerie(serie.serieId)
    assertTrue(updatedSerie.participants.contains(testUserId))
  }

  @Test
  fun joinSerie_serieNotLoaded_returnsFalse() = runTest {
    // Don't load any serie

    val result = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Serie not loaded"))
  }

  @Test
  fun joinSerie_userIsOwner_returnsFalse() = runTest {
    val serie =
        createTestSerie(ownerId = testUserId, participants = listOf("user1"), maxParticipants = 10)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("owner of this serie"))

    // Verify repository was NOT updated (user1 still only participant)
    val unchangedSerie = seriesRepository.getSerie(serie.serieId)
    assertEquals(1, unchangedSerie.participants.size)
    assertTrue(unchangedSerie.participants.contains("user1"))
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

    // Verify repository was NOT updated
    val unchangedSerie = seriesRepository.getSerie(serie.serieId)
    assertFalse(unchangedSerie.participants.contains(testUserId))
  }

  @Test
  fun joinSerie_repositoryError_returnsFalse() = runTest {
    val serie = createTestSerie(participants = listOf("user1"), maxParticipants = 10)

    val errorRepository =
        object : FakeSeriesRepository() {
          override suspend fun getSerie(serieId: String): Serie = serie

          override suspend fun editSerie(serieId: String, newValue: Serie) {
            throw Exception("Repository update failed")
          }
        }

    val errorViewModel = SerieDetailsViewModel(errorRepository, eventsRepository)

    errorViewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = errorViewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = errorViewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to join serie"))
    assertTrue(state.errorMsg!!.contains("Repository update failed"))
  }

  @Test
  fun joinSerie_whenCanJoinIsFalse_returnsFalse() = runTest {
    // Create a situation where canJoin is false (serie is full)
    val serie = createTestSerie(participants = listOf("user1", "user2"), maxParticipants = 2)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse(state.canJoin(testUserId))

    val result = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)
  }

  @Test
  fun joinSerie_participantCountUpdatesCorrectly() = runTest {
    val serie = createTestSerie(participants = listOf("user1"), maxParticipants = 10)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertEquals("1/10", state.participantsCount) // Only user1 is in participants

    viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    state = viewModel.uiState.first()
    assertEquals("2/10", state.participantsCount) // user1 + test-user-id
  }

  /** --- QUIT SERIE TESTS --- */
  @Test
  fun quitSerie_regularParticipant_quitsSuccessfully() = runTest {
    val serie = createTestSerie(participants = listOf(testUserId, "user1", "owner123"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    assertTrue(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.serie)
    assertFalse(state.serie!!.participants.contains(testUserId))
    assertTrue(state.serie!!.participants.contains("user1"))
    assertTrue(state.serie!!.participants.contains("owner123"))
    assertEquals(2, state.serie!!.participants.size)
    assertNull(state.errorMsg)

    // Verify repository was updated
    val updatedSerie = seriesRepository.getSerie(serie.serieId)
    assertFalse(updatedSerie.participants.contains(testUserId))
  }

  @Test
  fun quitSerie_serieNotLoaded_returnsFalse() = runTest {
    // Don't load any serie

    val result = viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Serie not loaded"))
  }

  @Test
  fun quitSerie_userIsOwner_returnsFalse() = runTest {
    // Create serie where test-user-id is the owner
    val serie = createTestSerie(ownerId = testUserId, participants = listOf(testUserId, "user1"))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("You are the owner of this serie and cannot quit"))

    // Verify repository was NOT updated
    val unchangedSerie = seriesRepository.getSerie(serie.serieId)
    assertTrue(unchangedSerie.participants.contains(testUserId))
  }

  @Test
  fun quitSerie_userNotInParticipants_returnsFalse() = runTest {
    val serie =
        createTestSerie(participants = listOf("user1", "owner123")) // test-user-id not in list
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

  @Test
  fun quitSerie_repositoryError_returnsFalse() = runTest {
    val serie = createTestSerie(participants = listOf(testUserId, "user1", "owner123"))

    val errorRepository =
        object : FakeSeriesRepository() {
          override suspend fun getSerie(serieId: String): Serie = serie

          override suspend fun editSerie(serieId: String, newValue: Serie) {
            throw Exception("Repository update failed")
          }
        }

    val errorViewModel = SerieDetailsViewModel(errorRepository, eventsRepository)

    errorViewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = errorViewModel.quitSerie(testUserId)
    advanceUntilIdle()

    assertFalse(result)

    val state = errorViewModel.uiState.first()
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Failed to quit serie"))
    assertTrue(state.errorMsg!!.contains("Repository update failed"))
  }

  @Test
  fun quitSerie_lastParticipant_removesSuccessfully() = runTest {
    // Serie with only one participant (not the owner)
    val serie = createTestSerie(ownerId = "owner123", participants = listOf(testUserId))
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val result = viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    assertTrue(result)

    val state = viewModel.uiState.first()
    assertNotNull(state.serie)
    assertTrue(state.serie!!.participants.isEmpty())
    assertNull(state.errorMsg)
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
    // First set an error
    viewModel.setErrorMsg("Test error")
    var state = viewModel.uiState.first()
    assertNotNull(state.errorMsg)

    // Clear error
    viewModel.clearErrorMsg()

    state = viewModel.uiState.first()
    assertNull(state.errorMsg)
  }

  @Test
  fun clearErrorMsg_afterLoadingError_removesError() = runTest {
    viewModel.loadSerieDetails("non-existent-id")
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

  /** --- EDGE CASES AND INTEGRATION TESTS --- */
  @Test
  fun loadSerieDetails_multipleTimes_updatesStateCorrectly() = runTest {
    val serie1 = createTestSerie(serieId = "serie1", title = "Serie 1")
    val serie2 = createTestSerie(serieId = "serie2", title = "Serie 2")

    seriesRepository.addSerie(serie1)
    seriesRepository.addSerie(serie2)

    // Load first serie
    viewModel.loadSerieDetails(serie1.serieId)
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertEquals("Serie 1", state.serie?.title)

    // Load second serie
    viewModel.loadSerieDetails(serie2.serieId)
    advanceUntilIdle()

    state = viewModel.uiState.first()
    assertEquals("Serie 2", state.serie?.title)
  }

  @Test
  fun joinAndQuitSerie_workflow_worksCorrectly() = runTest {
    val serie = createTestSerie(participants = listOf("user1", "owner123"), maxParticipants = 10)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertFalse(state.isParticipant(testUserId))
    assertTrue(state.canJoin(testUserId))

    // Join the serie
    val joinResult = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertTrue(joinResult)
    state = viewModel.uiState.first()
    assertTrue(state.isParticipant(testUserId))
    assertFalse(state.canJoin(testUserId))

    // Quit the serie
    val quitResult = viewModel.quitSerie(testUserId)
    advanceUntilIdle()

    assertTrue(quitResult)
    state = viewModel.uiState.first()
    assertFalse(state.isParticipant(testUserId))
    assertTrue(state.canJoin(testUserId))
  }

  @Test
  fun viewModel_withLargeParticipantList_handlesCorrectly() = runTest {
    val largeParticipantList = (1..100).map { "user$it" }.toMutableList()

    val serie = createTestSerie(participants = largeParticipantList, maxParticipants = 150)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("100/150", state.participantsCount)
    assertTrue(state.canJoin(testUserId))

    // Join serie
    val joinResult = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertTrue(joinResult)

    val updatedState = viewModel.uiState.first()
    assertEquals("101/150", updatedState.participantsCount)
  }

  @Test
  fun viewModel_withSpecialCharactersInTitle_handlesCorrectly() = runTest {
    val serie = createTestSerie(title = "Test ✓ Serie \"with\" special <chars>")
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertEquals("Test ✓ Serie \"with\" special <chars>", state.serie?.title)
  }

  @Test
  fun joinSerie_exactlyAtMaxCapacity_preventsFurtherJoins() = runTest {
    // Create serie with 2 participants, max 3
    val serie = createTestSerie(participants = listOf("user1", "user2"), maxParticipants = 3)
    seriesRepository.addSerie(serie)

    viewModel.loadSerieDetails(serie.serieId)
    advanceUntilIdle()

    // test-user-id joins (should succeed - now 3/3)
    val joinResult = viewModel.joinSerie(testUserId)
    advanceUntilIdle()

    assertTrue(joinResult)

    val state = viewModel.uiState.first()
    assertEquals("3/3", state.participantsCount)
    assertFalse(state.canJoin(testUserId)) // Serie is now full
  }
}
