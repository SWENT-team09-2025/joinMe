package com.android.joinme.model.event

import com.android.joinme.ui.overview.OverviewViewModel
import com.google.firebase.Timestamp
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
 * Verifies UI state updates and error handling when interacting with the repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var fakeRepository: FakeEventsRepository
  private lateinit var viewModel: OverviewViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeRepository = FakeEventsRepository()
    viewModel = OverviewViewModel(eventRepository = fakeRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init loads all events successfully`() = runTest {
    fakeRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
    val totalEvents = state.ongoingEvents.size + state.upcomingEvents.size
    assertEquals(2, totalEvents)
  }

  /*@Test
  fun `getAllEvents updates errorMsg on repository failure`() = runTest {
    fakeRepository.shouldThrow = true

    // Laisse le ViewModel terminer son init (et ignorer son 1er fetch)
    testDispatcher.scheduler.advanceUntilIdle()

    // Puis simule la nouvelle tentative
    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertFalse(state.isLoading)
    assertTrue(state.ongoingEvents.isEmpty())
    assertTrue(state.upcomingEvents.isEmpty())
  }*/

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
  fun `refreshUIState fetches updated events`() = runTest {
    fakeRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    val totalEvents = state.ongoingEvents.size + state.upcomingEvents.size
    assertEquals(2, totalEvents)
  }

  @Test
  fun `events are sorted by date in ascending order`() = runTest {
    fakeRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)

    // Check ongoing events are sorted
    if (state.ongoingEvents.size > 1) {
      for (i in 0 until state.ongoingEvents.size - 1) {
        assertTrue(
            state.ongoingEvents[i].date.toDate().time <=
                state.ongoingEvents[i + 1].date.toDate().time)
      }
    }

    // Check upcoming events are sorted
    if (state.upcomingEvents.size > 1) {
      for (i in 0 until state.upcomingEvents.size - 1) {
        assertTrue(
            state.upcomingEvents[i].date.toDate().time <=
                state.upcomingEvents[i + 1].date.toDate().time)
      }
    }
  }

  /*@Test
  fun `initial state has isLoading true`() = runTest {
    val state = viewModel.uiState.value
    assertTrue(state.isLoading)
  }*/

  /*@Test
  fun `isLoading is true during fetch and false after completion`() = runTest {
    fakeRepository.shouldThrow = false

    viewModel.refreshUIState()

    // Should be loading at start
    assertTrue(viewModel.uiState.value.isLoading)

    testDispatcher.scheduler.advanceUntilIdle()

    // Should not be loading after completion
    assertFalse(viewModel.uiState.value.isLoading)
  }*/

  @Test
  fun `isLoading is false after error`() = runTest {
    fakeRepository.shouldThrow = true

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNotNull(state.errorMsg)
  }

  /** Fake implementation of [EventsRepository] for isolated ViewModel testing. */
  private class FakeEventsRepository : EventsRepository {
    var shouldThrow = false

    private val fakeEvents =
        listOf(
            Event(
                eventId = "1",
                type = EventType.SPORTS,
                title = "Event 1",
                description = "Desc 1",
                location = null,
                date = Timestamp.now(),
                duration = 60,
                participants = listOf("user1"),
                maxParticipants = 10,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner1"),
            Event(
                eventId = "2",
                type = EventType.SOCIAL,
                title = "Event 2",
                description = "Desc 2",
                location = null,
                date = Timestamp.now(),
                duration = 30,
                participants = emptyList(),
                maxParticipants = 5,
                visibility = EventVisibility.PRIVATE,
                ownerId = "owner2"))

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
