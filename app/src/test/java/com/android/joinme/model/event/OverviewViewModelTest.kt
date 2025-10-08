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
    assertEquals(2, state.events.size)
    assertEquals("Event 1", state.events[0].title)
  }

  @Test
  fun `getAllEvents updates errorMsg on repository failure`() = runTest {
    fakeRepository.shouldThrow = true

    // Laisse le ViewModel terminer son init (et ignorer son 1er fetch)
    testDispatcher.scheduler.advanceUntilIdle()

    // Puis simule la nouvelle tentative
    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertTrue(state.events.isEmpty())
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
  fun `refreshUIState fetches updated events`() = runTest {
    fakeRepository.shouldThrow = false

    viewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.events.size)
    assertEquals("Event 2", state.events[1].title)
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
