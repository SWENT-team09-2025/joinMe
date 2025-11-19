// Tests implemented with help of Claude AI

package com.android.joinme.ui.groups

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.serie.SeriesRepository
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityGroupViewModelTest {

  private lateinit var groupRepository: GroupRepository
  private lateinit var eventsRepository: EventsRepository
  private lateinit var seriesRepository: SeriesRepository
  private lateinit var viewModel: ActivityGroupViewModel

  private val testDispatcher = StandardTestDispatcher()

  // Test data
  private val testGroupId = "group123"
  private val testEventId1 = "event1"
  private val testEventId2 = "event2"

  private val testEvent1 =
      Event(
          eventId = testEventId1,
          type = EventType.SPORTS,
          title = "Football Match",
          description = "Weekly football game",
          location = null,
          date = Timestamp.now(),
          duration = 90,
          participants = listOf("user1", "user2"),
          maxParticipants = 20,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner1",
          partOfASerie = false)

  private val testEvent2 =
      Event(
          eventId = testEventId2,
          type = EventType.ACTIVITY,
          title = "Bowling Night",
          description = "Fun bowling session",
          location = null,
          date = Timestamp.now(),
          duration = 120,
          participants = listOf("user1", "user3"),
          maxParticipants = 10,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner1",
          partOfASerie = false)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    groupRepository = mockk()
    eventsRepository = mockk()
    seriesRepository = mockk()
    viewModel =
        ActivityGroupViewModel(
            groupRepository = groupRepository,
            eventsRepository = eventsRepository,
            seriesRepository = seriesRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is correct`() {
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.events.isEmpty())
    assertTrue(state.series.isEmpty())
    assertNull(state.error)
  }

  @Test
  fun `load successfully loads events and updates state`() = runTest {
    // Given
    val group = Group(id = testGroupId, eventIds = listOf(testEventId1, testEventId2))
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { eventsRepository.getEventsByIds(listOf(testEventId1, testEventId2)) } returns
        listOf(testEvent1, testEvent2)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then - verify final state
    val finalState = viewModel.uiState.value
    assertFalse(finalState.isLoading)
    assertEquals(2, finalState.events.size)
    assertEquals(testEvent1, finalState.events[0])
    assertEquals(testEvent2, finalState.events[1])
    assertTrue(finalState.series.isEmpty())
    assertNull(finalState.error)

    // Verify repository interactions
    coVerify(exactly = 1) { groupRepository.getGroup(testGroupId) }
    coVerify(exactly = 1) { eventsRepository.getEventsByIds(listOf(testEventId1, testEventId2)) }
  }

  @Test
  fun `load with empty event list returns empty events`() = runTest {
    // Given
    val group = Group(id = testGroupId, eventIds = emptyList())
    coEvery { groupRepository.getGroup(testGroupId) } returns group

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.events.isEmpty())
    assertTrue(state.series.isEmpty())
    assertNull(state.error)

    // Verify getEventsByIds was NOT called
    coVerify(exactly = 0) { eventsRepository.getEventsByIds(any()) }
  }

  @Test
  fun `load handles group repository error gracefully`() = runTest {
    // Given
    val errorMessage = "Group not found"
    coEvery { groupRepository.getGroup(testGroupId) } throws Exception(errorMessage)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.events.isEmpty())
    assertTrue(state.series.isEmpty())
    assertEquals("Failed to load group activities: $errorMessage", state.error)
  }

  @Test
  fun `load handles events repository error gracefully`() = runTest {
    // Given
    val group = Group(id = testGroupId, eventIds = listOf(testEventId1))
    val errorMessage = "Network error"
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { eventsRepository.getEventsByIds(any()) } throws Exception(errorMessage)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.events.isEmpty())
    assertTrue(state.series.isEmpty())
    assertEquals("Failed to load group activities: $errorMessage", state.error)
  }

  @Test
  fun `load with single event works correctly`() = runTest {
    // Given
    val group = Group(id = testGroupId, eventIds = listOf(testEventId1))
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { eventsRepository.getEventsByIds(listOf(testEventId1)) } returns listOf(testEvent1)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(1, state.events.size)
    assertEquals(testEvent1, state.events[0])
    assertNull(state.error)
  }

  @Test
  fun `multiple load calls update state correctly`() = runTest {
    // Given - first group with events
    val group1 = Group(id = "group1", eventIds = listOf(testEventId1))
    coEvery { groupRepository.getGroup("group1") } returns group1
    coEvery { eventsRepository.getEventsByIds(listOf(testEventId1)) } returns listOf(testEvent1)

    // Given - second group with different events
    val group2 = Group(id = "group2", eventIds = listOf(testEventId2))
    coEvery { groupRepository.getGroup("group2") } returns group2
    coEvery { eventsRepository.getEventsByIds(listOf(testEventId2)) } returns listOf(testEvent2)

    // When - load first group
    viewModel.load("group1")
    advanceUntilIdle()

    // Then - verify first load
    assertEquals(1, viewModel.uiState.value.events.size)
    assertEquals(testEvent1, viewModel.uiState.value.events[0])

    // When - load second group
    viewModel.load("group2")
    advanceUntilIdle()

    // Then - verify second load replaced first
    assertEquals(1, viewModel.uiState.value.events.size)
    assertEquals(testEvent2, viewModel.uiState.value.events[0])
  }

  @Test
  fun `load preserves series as empty list for future implementation`() = runTest {
    // Given
    val group = Group(id = testGroupId, eventIds = listOf(testEventId1))
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { eventsRepository.getEventsByIds(any()) } returns listOf(testEvent1)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then - series should always be empty (reserved for future)
    assertTrue(viewModel.uiState.value.series.isEmpty())
  }
}
