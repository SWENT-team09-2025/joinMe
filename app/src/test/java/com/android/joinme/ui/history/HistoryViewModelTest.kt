package com.android.joinme.ui.history

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
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

  private lateinit var fakeRepository: FakeEventsRepository
  private lateinit var viewModel: HistoryViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeRepository = FakeEventsRepository()
    viewModel = HistoryViewModel(eventRepository = fakeRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `refreshUIState loads expired events successfully`() = runTest {
    fakeRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertEquals(1, state.expiredEvents.size)
    assertEquals("Expired Event", state.expiredEvents[0].title)
  }

  @Test
  fun `getExpiredEvents updates errorMsg on repository failure`() = runTest {
    fakeRepository.shouldThrow = true

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertTrue(state.expiredEvents.isEmpty())
    assertTrue(state.errorMsg!!.contains("Failed to load history"))
  }

  @Test
  fun `clearErrorMsg clears existing error`() = runTest {
    fakeRepository.shouldThrow = true
    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.clearErrorMsg()
    val state = viewModel.uiState.value

    assertNull(state.errorMsg)
  }

  @Test
  fun `refreshUIState fetches only expired events`() = runTest {
    fakeRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    // Should only contain the expired event, not the ongoing or upcoming ones
    assertEquals(1, state.expiredEvents.size)
    assertEquals("Expired Event", state.expiredEvents[0].title)
  }

  @Test
  fun `expired events are sorted in descending order by date`() = runTest {
    fakeRepository.shouldThrow = false
    fakeRepository.addMultipleExpiredEvents()

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should have 3 expired events (1 from default + 2 added)
    assertTrue(state.expiredEvents.size >= 2)

    // Check that events are sorted by date in descending order (most recent first)
    for (i in 0 until state.expiredEvents.size - 1) {
      assertTrue(
          state.expiredEvents[i].date.toDate().time >=
              state.expiredEvents[i + 1].date.toDate().time)
    }
  }

  @Test
  fun `uiState starts with empty expiredEvents list`() = runTest {
    val state = viewModel.uiState.value
    assertTrue(state.expiredEvents.isEmpty())
    assertNull(state.errorMsg)
  }

  @Test
  fun `refreshUIState handles empty repository`() = runTest {
    fakeRepository.clearAllEvents()

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertTrue(state.expiredEvents.isEmpty())
  }

  @Test
  fun `refreshUIState excludes ongoing events`() = runTest {
    fakeRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should not contain any ongoing events
    state.expiredEvents.forEach { event ->
      val now = System.currentTimeMillis()
      val startTime = event.date.toDate().time
      val endTime = startTime + (event.duration * 60 * 1000)
      // Verify event is not ongoing
      assertTrue(endTime <= now)
    }
  }

  @Test
  fun `refreshUIState excludes upcoming events`() = runTest {
    fakeRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should not contain any upcoming events
    state.expiredEvents.forEach { event ->
      val now = System.currentTimeMillis()
      // Verify event is not upcoming
      assertTrue(event.date.toDate().time <= now)
    }
  }

  /** Fake implementation of [EventsRepository] for isolated ViewModel testing. */
  private class FakeEventsRepository : EventsRepository {
    var shouldThrow = false

    private val fakeEvents = mutableListOf<Event>()

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

    override suspend fun getAllEvents(): List<Event> {
      if (shouldThrow) throw RuntimeException("Repository error")
      return fakeEvents
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

    override fun getNewEventId(): String {
      if (shouldThrow) throw RuntimeException("Repository error")
      return "new_event_id"
    }
  }
}
