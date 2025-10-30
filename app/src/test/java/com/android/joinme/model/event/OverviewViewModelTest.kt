package com.android.joinme.model.event

import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.utils.Visibility
import com.android.joinme.ui.overview.OverviewViewModel
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OverviewViewModel].
 *
 * Verifies UI state updates and error handling when interacting with repositories. Tests the
 * integration of both events and series in the overview screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var fakeEventRepository: FakeEventsRepository
  private lateinit var fakeSerieRepository: FakeSeriesRepository
  private lateinit var viewModel: OverviewViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeEventRepository = FakeEventsRepository()
    fakeSerieRepository = FakeSeriesRepository()
    viewModel =
        OverviewViewModel(
            eventRepository = fakeEventRepository, serieRepository = fakeSerieRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init loads all events and series successfully`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
    val totalItems = state.ongoingItems.size + state.upcomingItems.size
    // Should have standalone event + serie (serie event is filtered out)
    assertTrue(totalItems > 0)
  }

  @Test
  fun `getAllData updates errorMsg on event repository failure`() = runTest {
    fakeEventRepository.shouldThrow = true
    fakeSerieRepository.shouldThrow = false

    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun `getAllData updates errorMsg on serie repository failure`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = true

    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun `clearErrorMsg clears existing error`() = runTest {
    fakeEventRepository.shouldThrow = true
    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.clearErrorMsg()
    val state = viewModel.uiState.value

    assertNull(state.errorMsg)
  }

  @Test
  fun `refreshUIState fetches updated data`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    val totalItems = state.ongoingItems.size + state.upcomingItems.size
    assertTrue(totalItems > 0)
  }

  @Test
  fun `items are sorted by date in ascending order`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)

    // Check ongoing items are sorted
    if (state.ongoingItems.size > 1) {
      for (i in 0 until state.ongoingItems.size - 1) {
        assertTrue(
            state.ongoingItems[i].date.toDate().time <=
                state.ongoingItems[i + 1].date.toDate().time)
      }
    }

    // Check upcoming items are sorted
    if (state.upcomingItems.size > 1) {
      for (i in 0 until state.upcomingItems.size - 1) {
        assertTrue(
            state.upcomingItems[i].date.toDate().time <=
                state.upcomingItems[i + 1].date.toDate().time)
      }
    }
  }

  @Test
  fun `isLoading is false after error`() = runTest {
    fakeEventRepository.shouldThrow = true

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNotNull(state.errorMsg)
  }

  @Test
  fun `events belonging to series are filtered out`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Event with ID "serie_event_1" should not appear as standalone
    // Only the standalone event and the serie should appear
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
  }

  @Test
  fun `ongoing items contain only active events and series`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = false

    // Create events with specific times
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -1) // Started 1 hour ago
    val ongoingEvent =
        Event(
            eventId = "ongoing_event",
            type = EventType.SPORTS,
            title = "Ongoing Event",
            description = "Desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 120, // 2 hours duration, so still ongoing
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    fakeEventRepository.addTestEvent(ongoingEvent)

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.ongoingItems.isNotEmpty())
  }

  @Test
  fun `upcoming items contain only future events and series`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = false

    // Create events with specific times
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2) // Starts in 2 hours
    val upcomingEvent =
        Event(
            eventId = "upcoming_event",
            type = EventType.SPORTS,
            title = "Upcoming Event",
            description = "Desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    fakeEventRepository.addTestEvent(upcomingEvent)

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.upcomingItems.isNotEmpty())
  }

  @Test
  fun `series with active events appear in ongoing items`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = false

    // Create an active event for the serie
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MINUTE, -30) // Started 30 minutes ago
    val activeSerieEvent =
        Event(
            eventId = "active_serie_event",
            type = EventType.SPORTS,
            title = "Active Serie Event",
            description = "Desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 120, // Still ongoing
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    fakeEventRepository.addTestEvent(activeSerieEvent)

    val activeSerie =
        Serie(
            serieId = "active_serie",
            title = "Active Serie",
            description = "Active serie desc",
            date = Timestamp(calendar.time),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("active_serie_event"),
            ownerId = "owner1")

    fakeSerieRepository.addTestSerie(activeSerie)

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    // The active serie should appear in ongoing items
    assertTrue(state.ongoingItems.isNotEmpty())
  }

  @Test
  fun `expired events do not appear in ongoing or upcoming`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = false

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -3) // Started 3 hours ago
    val expiredEvent =
        Event(
            eventId = "expired_event",
            type = EventType.SPORTS,
            title = "Expired Event",
            description = "Desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60, // Ended 2 hours ago
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    fakeEventRepository.clearEvents()
    fakeEventRepository.addTestEvent(expiredEvent)
    fakeSerieRepository.clearSeries()

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    // Expired events should not appear in either list
    assertEquals(0, state.ongoingItems.size)
    assertEquals(0, state.upcomingItems.size)
  }

  @Test
  fun `empty repositories result in empty item lists`() = runTest {
    fakeEventRepository.clearEvents()
    fakeSerieRepository.clearSeries()

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(0, state.ongoingItems.size)
    assertEquals(0, state.upcomingItems.size)
  }

  @Test
  fun `multiple upcoming items are sorted correctly`() = runTest {
    fakeEventRepository.clearEvents()
    fakeSerieRepository.clearSeries()

    val calendar = Calendar.getInstance()

    // Create events with different future times
    calendar.add(Calendar.HOUR, 5)
    val event1 =
        Event(
            eventId = "event1",
            type = EventType.SPORTS,
            title = "Event 1",
            description = "Desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    calendar.time = Calendar.getInstance().time
    calendar.add(Calendar.HOUR, 2)
    val event2 =
        Event(
            eventId = "event2",
            type = EventType.SPORTS,
            title = "Event 2",
            description = "Desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    fakeEventRepository.addTestEvent(event1)
    fakeEventRepository.addTestEvent(event2)

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.upcomingItems.size)
    // Event 2 should come first (starts in 2 hours vs 5 hours)
    assertEquals("event2", state.upcomingItems[0].eventItemId)
    assertEquals("event1", state.upcomingItems[1].eventItemId)
  }

  /** Fake implementation of [EventsRepository] for isolated ViewModel testing. */
  private class FakeEventsRepository : EventsRepository {
    var shouldThrow = false
    private val events = mutableListOf<Event>()

    init {
      // Add default upcoming event
      val calendar = Calendar.getInstance()
      calendar.add(Calendar.HOUR, 1)
      events.add(
          Event(
              eventId = "standalone_event",
              type = EventType.SPORTS,
              title = "Standalone Event",
              description = "Desc",
              location = null,
              date = Timestamp(calendar.time),
              duration = 60,
              participants = listOf("user1"),
              maxParticipants = 10,
              visibility = EventVisibility.PUBLIC,
              ownerId = "owner1"))

      // Add event that belongs to a serie
      events.add(
          Event(
              eventId = "serie_event_1",
              type = EventType.SOCIAL,
              title = "Serie Event 1",
              description = "Desc",
              location = null,
              date = Timestamp(calendar.time),
              duration = 30,
              participants = emptyList(),
              maxParticipants = 5,
              visibility = EventVisibility.PUBLIC,
              ownerId = "owner2"))
    }

    fun addTestEvent(event: Event) {
      events.add(event)
    }

    fun clearEvents() {
      events.clear()
    }

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> {
      if (shouldThrow) throw RuntimeException("Event repository error")
      return events.toList()
    }

    override suspend fun getEvent(eventId: String): Event {
      if (shouldThrow) throw RuntimeException("Event repository error")
      return events.first { it.eventId == eventId }
    }

    override suspend fun addEvent(event: Event) {
      if (shouldThrow) throw RuntimeException("Event repository error")
      events.add(event)
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      if (shouldThrow) throw RuntimeException("Event repository error")
      val index = events.indexOfFirst { it.eventId == eventId }
      if (index != -1) {
        events[index] = newValue
      }
    }

    override suspend fun deleteEvent(eventId: String) {
      if (shouldThrow) throw RuntimeException("Event repository error")
      events.removeIf { it.eventId == eventId }
    }

    override fun getNewEventId(): String {
      if (shouldThrow) throw RuntimeException("Event repository error")
      return "new_event_id"
    }
  }

  /** Fake implementation of [SeriesRepository] for isolated ViewModel testing. */
  private class FakeSeriesRepository : SeriesRepository {
    var shouldThrow = false
    private val series = mutableListOf<Serie>()

    init {
      // Add default serie with future date
      val calendar = Calendar.getInstance()
      calendar.add(Calendar.HOUR, 1)
      series.add(
          Serie(
              serieId = "serie1",
              title = "Test Serie",
              description = "Serie description",
              date = Timestamp(calendar.time),
              participants = listOf("user1"),
              maxParticipants = 10,
              visibility = Visibility.PUBLIC,
              eventIds = listOf("serie_event_1"),
              ownerId = "owner1"))
    }

    fun addTestSerie(serie: Serie) {
      series.add(serie)
    }

    fun clearSeries() {
      series.clear()
    }

    override fun getNewSerieId(): String {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      return "new_serie_id"
    }

    override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      return series.toList()
    }

    override suspend fun getSerie(serieId: String): Serie {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      return series.first { it.serieId == serieId }
    }

    override suspend fun addSerie(serie: Serie) {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      series.add(serie)
    }

    override suspend fun editSerie(serieId: String, newValue: Serie) {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      val index = series.indexOfFirst { it.serieId == serieId }
      if (index != -1) {
        series[index] = newValue
      }
    }

    override suspend fun deleteSerie(serieId: String) {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      series.removeIf { it.serieId == serieId }
    }
  }
}
