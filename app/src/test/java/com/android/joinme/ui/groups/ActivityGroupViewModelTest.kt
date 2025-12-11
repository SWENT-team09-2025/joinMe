// Tests implemented with help of Claude AI

package com.android.joinme.ui.groups

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Calendar
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
  private val testGroupName = "Test Group"
  private val testEventId1 = "event1"
  private val testEventId2 = "event2"
  private val testEventId3 = "event3"
  private val testSerieId1 = "serie1"

  private fun createFutureTimestamp(daysFromNow: Int = 1): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, daysFromNow)
    return Timestamp(calendar.time)
  }

  private fun createPastTimestamp(daysAgo: Int = 1): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return Timestamp(calendar.time)
  }

  private val testEvent1 =
      Event(
          eventId = testEventId1,
          type = EventType.SPORTS,
          title = "Football Match",
          description = "Weekly football game",
          location = null,
          date = createFutureTimestamp(1),
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
          date = createFutureTimestamp(2),
          duration = 120,
          participants = listOf("user1", "user3"),
          maxParticipants = 10,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner1",
          partOfASerie = false)

  private val testEventInSerie =
      Event(
          eventId = testEventId3,
          type = EventType.SPORTS,
          title = "Serie Event",
          description = "Event part of serie",
          location = null,
          date = createFutureTimestamp(3),
          duration = 60,
          participants = listOf("user1"),
          maxParticipants = 15,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner1",
          partOfASerie = true)

  private val testExpiredEvent =
      Event(
          eventId = "expiredEvent",
          type = EventType.SOCIAL,
          title = "Old Event",
          description = "Already happened",
          location = null,
          date = createPastTimestamp(2),
          duration = 60,
          participants = listOf("user1"),
          maxParticipants = 10,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner1",
          partOfASerie = false)

  private val testSerie =
      Serie(
          serieId = testSerieId1,
          title = "Weekly Football",
          description = "Weekly football series",
          date = createFutureTimestamp(1),
          participants = listOf("user1", "user2"),
          maxParticipants = 20,
          visibility = Visibility.PUBLIC,
          eventIds = listOf(testEventId3),
          ownerId = "owner1")

  private val testExpiredSerie =
      Serie(
          serieId = "expiredSerie",
          title = "Old Serie",
          description = "Expired serie",
          date = createPastTimestamp(5),
          participants = listOf("user1"),
          maxParticipants = 10,
          visibility = Visibility.PUBLIC,
          eventIds = listOf(),
          ownerId = "owner1",
          lastEventEndTime = createPastTimestamp(3))

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
    assertEquals("", state.groupName)
    assertTrue(state.items.isEmpty())
    assertNull(state.error)
  }

  @Test
  fun `load successfully loads standalone events as EventItems`() = runTest {
    // Given
    val group =
        Group(
            id = testGroupId,
            name = testGroupName,
            eventIds = listOf(testEventId1, testEventId2),
            serieIds = emptyList())
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { eventsRepository.getEventsByIds(listOf(testEventId1, testEventId2)) } returns
        listOf(testEvent1, testEvent2)
    coEvery { seriesRepository.getSeriesByIds(emptyList()) } returns emptyList()

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(testGroupName, state.groupName)
    assertEquals(2, state.items.size)
    assertTrue(state.items[0] is EventItem.SingleEvent)
    assertTrue(state.items[1] is EventItem.SingleEvent)
    assertEquals(testEvent1, (state.items[0] as EventItem.SingleEvent).event)
    assertEquals(testEvent2, (state.items[1] as EventItem.SingleEvent).event)
    assertNull(state.error)
  }

  @Test
  fun `load successfully loads series as EventItems`() = runTest {
    // Given
    val group =
        Group(
            id = testGroupId,
            name = testGroupName,
            eventIds = emptyList(),
            serieIds = listOf(testSerieId1))
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { seriesRepository.getSeriesByIds(listOf(testSerieId1)) } returns listOf(testSerie)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(1, state.items.size)
    assertTrue(state.items[0] is EventItem.EventSerie)
    assertEquals(testSerie, (state.items[0] as EventItem.EventSerie).serie)
    assertNull(state.error)
  }

  @Test
  fun `load filters out events that belong to series`() = runTest {
    // Given - event3 belongs to serie1
    val group =
        Group(
            id = testGroupId,
            name = testGroupName,
            eventIds = listOf(testEventId1, testEventId3),
            serieIds = listOf(testSerieId1))
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { eventsRepository.getEventsByIds(listOf(testEventId1, testEventId3)) } returns
        listOf(testEvent1, testEventInSerie)
    coEvery { seriesRepository.getSeriesByIds(listOf(testSerieId1)) } returns listOf(testSerie)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then - only standalone event and serie should be in items
    val state = viewModel.uiState.value
    assertEquals(2, state.items.size)
    assertTrue(state.items[0] is EventItem.SingleEvent)
    assertTrue(state.items[1] is EventItem.EventSerie)
    assertEquals(testEvent1, (state.items[0] as EventItem.SingleEvent).event)
    assertEquals(testSerie, (state.items[1] as EventItem.EventSerie).serie)
  }

  @Test
  fun `load filters out expired events`() = runTest {
    // Given - mix of expired and non-expired events
    val group =
        Group(
            id = testGroupId,
            name = testGroupName,
            eventIds = listOf(testEventId1, "expiredEvent"),
            serieIds = emptyList())
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { eventsRepository.getEventsByIds(listOf(testEventId1, "expiredEvent")) } returns
        listOf(testEvent1, testExpiredEvent)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then - only non-expired event should be in items
    val state = viewModel.uiState.value
    assertEquals(1, state.items.size)
    assertEquals(testEvent1, (state.items[0] as EventItem.SingleEvent).event)
  }

  @Test
  fun `load filters out expired series`() = runTest {
    // Given - mix of expired and non-expired series
    val group =
        Group(
            id = testGroupId,
            name = testGroupName,
            eventIds = emptyList(),
            serieIds = listOf(testSerieId1, "expiredSerie"))
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { seriesRepository.getSeriesByIds(listOf(testSerieId1, "expiredSerie")) } returns
        listOf(testSerie, testExpiredSerie)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then - only non-expired serie should be in items
    val state = viewModel.uiState.value
    assertEquals(1, state.items.size)
    assertEquals(testSerie, (state.items[0] as EventItem.EventSerie).serie)
  }

  @Test
  fun `load sorts items by date`() = runTest {
    // Given - events with different dates (event1 is 1 day from now, event2 is 2 days)
    val group =
        Group(
            id = testGroupId,
            name = testGroupName,
            eventIds = listOf(testEventId2, testEventId1),
            serieIds = emptyList())
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { eventsRepository.getEventsByIds(listOf(testEventId2, testEventId1)) } returns
        listOf(testEvent2, testEvent1)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then - items should be sorted by date (event1 before event2)
    val state = viewModel.uiState.value
    assertEquals(2, state.items.size)
    assertEquals(testEvent1, (state.items[0] as EventItem.SingleEvent).event)
    assertEquals(testEvent2, (state.items[1] as EventItem.SingleEvent).event)
  }

  @Test
  fun `load with empty lists returns empty items`() = runTest {
    // Given
    val group = Group(id = testGroupId, name = testGroupName)
    coEvery { groupRepository.getGroup(testGroupId) } returns group

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(testGroupName, state.groupName)
    assertTrue(state.items.isEmpty())
    assertNull(state.error)
    coVerify(exactly = 0) { eventsRepository.getEventsByIds(any()) }
    coVerify(exactly = 0) { seriesRepository.getSeriesByIds(any()) }
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
    assertEquals("", state.groupName)
    assertTrue(state.items.isEmpty())
    assertEquals("Failed to load group activities: $errorMessage", state.error)
  }

  @Test
  fun `load handles events repository error gracefully`() = runTest {
    // Given
    val group = Group(id = testGroupId, name = testGroupName, eventIds = listOf(testEventId1))
    val errorMessage = "Network error"
    coEvery { groupRepository.getGroup(testGroupId) } returns group
    coEvery { eventsRepository.getEventsByIds(any()) } throws Exception(errorMessage)

    // When
    viewModel.load(testGroupId)
    advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.items.isEmpty())
    assertEquals("Failed to load group activities: $errorMessage", state.error)
  }
}
