package com.android.joinme.ui.history

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [HistoryViewModel].
 *
 * Verifies UI state updates and error handling when interacting with the repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var fakeEventRepository: FakeEventsRepository
  private lateinit var fakeSerieRepository: FakeSeriesRepository
  private lateinit var viewModel: HistoryViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeEventRepository = FakeEventsRepository()
    fakeSerieRepository = FakeSeriesRepository()
    viewModel =
        HistoryViewModel(
            eventRepository = fakeEventRepository, serieRepository = fakeSerieRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `refreshUIState loads expired items successfully`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertTrue(state.expiredItems.isNotEmpty())
    // Should have at least the expired event and expired serie from defaults
    assertTrue(state.expiredItems.any { it.title == "Expired Event" })
  }

  @Test
  fun `getExpiredItems updates errorMsg on repository failure`() = runTest {
    fakeEventRepository.shouldThrow = true

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertTrue(state.expiredItems.isEmpty())
    assertTrue(state.errorMsg!!.contains("Failed to load history"))
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
  fun `refreshUIState fetches only expired items`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeSerieRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    // Should only contain expired items, not the ongoing or upcoming ones
    assertTrue(state.expiredItems.isNotEmpty())
    // Verify all items are of type EventItem
    state.expiredItems.forEach { item ->
      assertTrue(item is EventItem.SingleEvent || item is EventItem.EventSerie)
    }
  }

  @Test
  fun `expired items are sorted in descending order by date`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeEventRepository.addMultipleExpiredEvents()

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should have multiple expired items
    assertTrue(state.expiredItems.size >= 2)

    // Check that items are sorted by date in descending order (most recent first)
    for (i in 0 until state.expiredItems.size - 1) {
      assertTrue(
          state.expiredItems[i].date.toDate().time >= state.expiredItems[i + 1].date.toDate().time)
    }
  }

  @Test
  fun `uiState starts with empty expiredItems list`() = runTest {
    val state = viewModel.uiState.value
    assertTrue(state.expiredItems.isEmpty())
    assertNull(state.errorMsg)
  }

  @Test
  fun `uiState starts with isLoading false`() = runTest {
    val state = viewModel.uiState.value
    assertEquals(false, state.isLoading)
  }

  @Test
  fun `isLoading is true during data fetch`() = runTest {
    fakeEventRepository.shouldThrow = false
    fakeEventRepository.setDelay(1000) // Add delay to capture loading state

    viewModel.refreshUIState()

    // Advance dispatcher to allow coroutine to start and set isLoading = true
    testDispatcher.scheduler.runCurrent()

    // Check loading state after coroutine has started
    val loadingState = viewModel.uiState.value
    assertEquals(true, loadingState.isLoading)

    // Advance time to complete the operation
    testDispatcher.scheduler.advanceUntilIdle()

    // Loading should be false after completion
    val finalState = viewModel.uiState.value
    assertEquals(false, finalState.isLoading)
  }

  @Test
  fun `isLoading is false after successful fetch`() = runTest {
    fakeEventRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(false, state.isLoading)
    assertTrue(state.expiredItems.isNotEmpty())
  }

  @Test
  fun `isLoading is false after failed fetch`() = runTest {
    fakeEventRepository.shouldThrow = true

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(false, state.isLoading)
    assertNotNull(state.errorMsg)
  }

  @Test
  fun `refreshUIState handles empty repository`() = runTest {
    fakeEventRepository.clearAllEvents()
    fakeSerieRepository.clearAllSeries()

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertTrue(state.expiredItems.isEmpty())
  }

  @Test
  fun `refreshUIState excludes ongoing items`() = runTest {
    fakeEventRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Expired items should not be ongoing
    state.expiredItems.forEach { item ->
      val now = System.currentTimeMillis()
      val startTime = item.date.toDate().time
      // Verify item date is in the past
      assertTrue(startTime <= now)
    }
  }

  @Test
  fun `refreshUIState excludes upcoming items`() = runTest {
    fakeEventRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should not contain any upcoming items
    state.expiredItems.forEach { item ->
      val now = System.currentTimeMillis()
      // Verify item is not upcoming
      assertTrue(item.date.toDate().time <= now)
    }
  }

  @Test
  fun `refreshUIState excludes future serie with no events`() = runTest {
    fakeSerieRepository.addFutureSerie()

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should not contain the future serie
    state.expiredItems.forEach { item ->
      when (item) {
        is EventItem.EventSerie -> {
          assertTrue(item.serie.serieId != "futureSerie1")
        }
        else -> {}
      }
    }
  }

  /** Fake implementation of [EventsRepository] for isolated ViewModel testing. */
  private class FakeEventsRepository : EventsRepository {
    var shouldThrow = false
    private var delayMillis: Long = 0

    private val fakeEvents = mutableListOf<Event>()

    fun setDelay(millis: Long) {
      delayMillis = millis
    }

    init {
      // Add default test events
      val calendar = Calendar.getInstance()

      // Add an expired event (ended 2 hours ago)
      calendar.add(Calendar.HOUR, -3)
      fakeEvents.add(
          Event(
              eventId = "1",
              type = EventType.SPORTS,
              title = "Expired Event",
              description = "Desc 1",
              location = null,
              date = Timestamp(calendar.time),
              duration = 60,
              participants = listOf("user1"),
              maxParticipants = 10,
              visibility = EventVisibility.PUBLIC,
              ownerId = "owner1"))

      // Add an ongoing event (started 30 minutes ago, lasts 2 hours)
      calendar.time = Calendar.getInstance().time
      calendar.add(Calendar.MINUTE, -30)
      fakeEvents.add(
          Event(
              eventId = "2",
              type = EventType.SOCIAL,
              title = "Ongoing Event",
              description = "Desc 2",
              location = null,
              date = Timestamp(calendar.time),
              duration = 120,
              participants = emptyList(),
              maxParticipants = 5,
              visibility = EventVisibility.PRIVATE,
              ownerId = "owner2"))

      // Add an upcoming event (starts in 2 hours)
      calendar.time = Calendar.getInstance().time
      calendar.add(Calendar.HOUR, 2)
      fakeEvents.add(
          Event(
              eventId = "3",
              type = EventType.ACTIVITY,
              title = "Upcoming Event",
              description = "Desc 3",
              location = null,
              date = Timestamp(calendar.time),
              duration = 60,
              participants = emptyList(),
              maxParticipants = 8,
              visibility = EventVisibility.PUBLIC,
              ownerId = "owner3"))
    }

    fun addMultipleExpiredEvents() {
      val calendar = Calendar.getInstance()

      // Add an expired event from yesterday
      calendar.add(Calendar.DAY_OF_MONTH, -1)
      fakeEvents.add(
          Event(
              eventId = "4",
              type = EventType.SPORTS,
              title = "Yesterday Event",
              description = "Desc 4",
              location = null,
              date = Timestamp(calendar.time),
              duration = 60,
              participants = emptyList(),
              maxParticipants = 10,
              visibility = EventVisibility.PUBLIC,
              ownerId = "owner4"))

      // Add an expired event from last week
      calendar.time = Calendar.getInstance().time
      calendar.add(Calendar.DAY_OF_MONTH, -7)
      fakeEvents.add(
          Event(
              eventId = "5",
              type = EventType.SOCIAL,
              title = "Last Week Event",
              description = "Desc 5",
              location = null,
              date = Timestamp(calendar.time),
              duration = 90,
              participants = emptyList(),
              maxParticipants = 15,
              visibility = EventVisibility.PUBLIC,
              ownerId = "owner5"))
    }

    fun clearAllEvents() {
      fakeEvents.clear()
    }

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> {
      if (delayMillis > 0) {
        kotlinx.coroutines.delay(delayMillis)
      }
      if (shouldThrow) throw RuntimeException("Repository error")
      return fakeEvents.toList()
    }

    override suspend fun getEvent(eventId: String): Event {
      if (shouldThrow) throw RuntimeException("Repository error")
      return fakeEvents.first { it.eventId == eventId }
    }

    override suspend fun addEvent(event: Event) {
      if (shouldThrow) throw RuntimeException("Repository error")
      fakeEvents.add(event)
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      if (shouldThrow) throw RuntimeException("Repository error")
    }

    override suspend fun deleteEvent(eventId: String) {
      if (shouldThrow) throw RuntimeException("Repository error")
    }

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
      if (shouldThrow) throw RuntimeException("Repository error")
      return fakeEvents.filter { eventIds.contains(it.eventId) }
    }

    override fun getNewEventId(): String {
      if (shouldThrow) throw RuntimeException("Repository error")
      return "new_event_id"
    }

    override suspend fun getCommonEvents(userIds: List<String>): List<Event> {
      if (shouldThrow) throw RuntimeException("Repository error")
      if (userIds.isEmpty()) return emptyList()
      return fakeEvents
          .filter { event -> userIds.all { userId -> event.participants.contains(userId) } }
          .sortedBy { it.date.toDate().time }
    }
  }

  /** Fake implementation of [SeriesRepository] for isolated ViewModel testing. */
  private class FakeSeriesRepository : SeriesRepository {
    var shouldThrow = false
    private val fakeSeries = mutableListOf<Serie>()

    init {
      // Add an expired serie (ended 2 hours ago)
      val calendar = Calendar.getInstance()
      calendar.add(Calendar.HOUR, -5) // Started 5 hours ago
      val startDate = calendar.time
      calendar.add(Calendar.HOUR, 3) // Ended 2 hours ago
      val endDate = calendar.time

      fakeSeries.add(
          Serie(
              serieId = "serie1",
              title = "Expired Serie",
              description = "Serie desc",
              date = Timestamp(startDate),
              participants = listOf("user1"),
              maxParticipants = 10,
              visibility = Visibility.PUBLIC,
              eventIds = emptyList(),
              ownerId = "owner1",
              lastEventEndTime = Timestamp(endDate))) // Set to past time to make it expired
    }

    fun clearAllSeries() {
      fakeSeries.clear()
    }

    fun addFutureSerie() {
      val calendar = Calendar.getInstance()
      calendar.add(Calendar.DAY_OF_MONTH, 30) // 30 days in the future
      val futureDate = calendar.time

      fakeSeries.add(
          Serie(
              serieId = "futureSerie1",
              title = "Future Serie",
              description = "Future serie desc",
              date = Timestamp(futureDate),
              participants = listOf("user1"),
              maxParticipants = 10,
              visibility = Visibility.PUBLIC,
              eventIds = emptyList(),
              ownerId = "owner1",
              lastEventEndTime =
                  Timestamp(futureDate))) // Set to future time to make it not expired
    }

    override fun getNewSerieId(): String {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      return "new_serie_id"
    }

    override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      return fakeSeries.toList()
    }

    override suspend fun getSeriesByIds(seriesIds: List<String>): List<Serie> {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      return fakeSeries.filter { seriesIds.contains(it.serieId) }
    }

    override suspend fun getSerie(serieId: String): Serie {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      return fakeSeries.first { it.serieId == serieId }
    }

    override suspend fun addSerie(serie: Serie) {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      fakeSeries.add(serie)
    }

    override suspend fun editSerie(serieId: String, newValue: Serie) {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      val index = fakeSeries.indexOfFirst { it.serieId == serieId }
      if (index != -1) {
        fakeSeries[index] = newValue
      }
    }

    override suspend fun deleteSerie(serieId: String) {
      if (shouldThrow) throw RuntimeException("Serie repository error")
      fakeSeries.removeIf { it.serieId == serieId }
    }
  }
}
