package com.android.joinme.ui.groups

// Implemented with the help of AI tools, adapted and refactored to follow project pattern.

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.map.Location
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Instrumented tests for the ActivityGroup screen.
 *
 * Tests the UI behavior, group activities display, and user interactions in the ActivityGroup
 * screen.
 */
@RunWith(RobolectricTestRunner::class)
class ActivityGroupScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var groupRepo: FakeGroupRepository
  private lateinit var eventRepo: FakeActivityGroupEventsRepository
  private lateinit var serieRepo: FakeActivityGroupSeriesRepository

  @Before
  fun setUp() {
    groupRepo = FakeGroupRepository()
    eventRepo = FakeActivityGroupEventsRepository()
    serieRepo = FakeActivityGroupSeriesRepository()
  }

  private fun createViewModel(
      groupRepo: GroupRepository = this.groupRepo,
      eventRepo: EventsRepository = this.eventRepo,
      serieRepo: SeriesRepository = this.serieRepo
  ) =
      ActivityGroupViewModel(
          groupRepository = groupRepo, eventsRepository = eventRepo, seriesRepository = serieRepo)

  /** Waits for the UI to settle after async operations. */
  private fun waitForContent() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()
  }

  private fun createTestEvent(eventId: String, title: String, type: EventType): Event {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2)
    return Event(
        eventId = eventId,
        type = type,
        title = title,
        description = "Description for $title",
        location = Location(46.5191, 6.5668, "EPFL"),
        date = Timestamp(calendar.time),
        duration = 60,
        participants = listOf("user1"),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = "owner1")
  }

  private fun createTestSerie(serieId: String, title: String): Serie {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2)
    val serieDate = Timestamp(calendar.time)
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    val serieEndDate = Timestamp(calendar.time)

    return Serie(
        serieId = serieId,
        title = title,
        description = "Description for $title",
        date = serieDate,
        participants = listOf("user1"),
        maxParticipants = 10,
        visibility = Visibility.PUBLIC,
        eventIds = listOf(),
        ownerId = "owner1",
        lastEventEndTime = serieEndDate)
  }

  @Test
  fun activityGroupScreen_displaysTopBarAndBackButton() {
    val viewModel = createViewModel()

    runBlocking { groupRepo.addGroup(Group(id = "1", name = "Test Group")) }

    composeTestRule.setContent {
      ActivityGroupScreen(groupId = "1", activityGroupViewModel = viewModel)
    }

    waitForContent()

    // Group assertions that use the same setup
    composeTestRule.onNodeWithText("Group Activities").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ActivityGroupScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun activityGroupScreen_backButtonTriggersCallback() {
    var backClicked = false
    val viewModel = createViewModel()

    runBlocking { groupRepo.addGroup(Group(id = "1", name = "Test Group")) }

    composeTestRule.setContent {
      ActivityGroupScreen(
          groupId = "1",
          activityGroupViewModel = viewModel,
          onNavigateBack = { backClicked = true })
    }

    waitForContent()

    composeTestRule.onNodeWithTag(ActivityGroupScreenTestTags.BACK_BUTTON).performClick()

    assertTrue(backClicked)
  }

  @Test
  fun activityGroupScreen_emptyState_displaysCorrectMessage() {
    val viewModel = createViewModel()

    runBlocking { groupRepo.addGroup(Group(id = "1", name = "Empty Group")) }

    composeTestRule.setContent {
      ActivityGroupScreen(groupId = "1", activityGroupViewModel = viewModel)
    }

    waitForContent()

    composeTestRule
        .onNodeWithTag(ActivityGroupScreenTestTags.EMPTY_ACTIVITY_LIST_MSG)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("This group has no activities yet").assertIsDisplayed()
  }

  @Test
  fun activityGroupScreen_displaysEventsAndSeriesInSingleList() {
    val event1 = createTestEvent("event1", "Basketball Game", EventType.SPORTS)
    val event2 = createTestEvent("event2", "Study Session", EventType.ACTIVITY)
    val serie1 = createTestSerie("serie1", "Weekly Meetup")

    runBlocking {
      eventRepo.addEvent(event1)
      eventRepo.addEvent(event2)
      serieRepo.addSerie(serie1)
      groupRepo.addGroup(
          Group(
              id = "1",
              name = "Active Group",
              eventIds = listOf("event1", "event2"),
              serieIds = listOf("serie1")))
    }

    val viewModel = createViewModel()

    composeTestRule.setContent {
      ActivityGroupScreen(groupId = "1", activityGroupViewModel = viewModel)
    }

    waitForContent()

    // All items should be displayed in a single list
    composeTestRule.onNodeWithText("Basketball Game").assertIsDisplayed()
    composeTestRule.onNodeWithText("Study Session").assertIsDisplayed()
    composeTestRule.onNodeWithText("Weekly Meetup").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ActivityGroupScreenTestTags.ACTIVITY_LIST).assertIsDisplayed()
  }

  @Test
  fun activityGroupScreen_eventCardClick_triggersCallback() {
    val event = createTestEvent("event1", "Clickable Event", EventType.SPORTS)
    var selectedEventId: String? = null

    runBlocking {
      eventRepo.addEvent(event)
      groupRepo.addGroup(Group(id = "1", name = "Test Group", eventIds = listOf("event1")))
    }

    val viewModel = createViewModel()

    composeTestRule.setContent {
      ActivityGroupScreen(
          groupId = "1",
          activityGroupViewModel = viewModel,
          onSelectedEvent = { selectedEventId = it })
    }

    waitForContent()

    composeTestRule.onNodeWithText("Clickable Event").performClick()

    assertTrue(selectedEventId != null)
    assertTrue(selectedEventId == "event1")
  }

  @Test
  fun activityGroupScreen_serieCardClick_triggersCallback() {
    val serie = createTestSerie("serie1", "Clickable Serie")
    var selectedSerieId: String? = null

    runBlocking {
      serieRepo.addSerie(serie)
      groupRepo.addGroup(Group(id = "1", name = "Test Group", serieIds = listOf("serie1")))
    }

    val viewModel = createViewModel()

    composeTestRule.setContent {
      ActivityGroupScreen(
          groupId = "1",
          activityGroupViewModel = viewModel,
          onSelectedSerie = { selectedSerieId = it })
    }

    waitForContent()

    composeTestRule.onNodeWithText("Clickable Serie").performClick()

    assertTrue(selectedSerieId != null)
    assertTrue(selectedSerieId == "serie1")
  }

  @Test
  fun activityGroupScreen_hasUniqueTestTags() {
    val event1 = createTestEvent("event1", "Event 1", EventType.SPORTS)
    val event2 = createTestEvent("event2", "Event 2", EventType.SOCIAL)
    val serie1 = createTestSerie("serie1", "Serie 1")

    runBlocking {
      eventRepo.addEvent(event1)
      eventRepo.addEvent(event2)
      serieRepo.addSerie(serie1)
      groupRepo.addGroup(
          Group(
              id = "1",
              name = "Test Group",
              eventIds = listOf("event1", "event2"),
              serieIds = listOf("serie1")))
    }

    val viewModel = createViewModel()

    composeTestRule.setContent {
      ActivityGroupScreen(groupId = "1", activityGroupViewModel = viewModel)
    }

    waitForContent()

    // Verify unique test tags for all items
    composeTestRule
        .onNodeWithTag(ActivityGroupScreenTestTags.getTestTagForEvent(event1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ActivityGroupScreenTestTags.getTestTagForEvent(event2))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ActivityGroupScreenTestTags.getTestTagForSerie(serie1))
        .assertIsDisplayed()
  }

  @Test
  fun activityGroupScreen_handlesErrorFromRepository() {
    val errorGroupRepo = FakeGroupRepository(shouldThrowError = true)
    val viewModel =
        createViewModel(groupRepo = errorGroupRepo, eventRepo = eventRepo, serieRepo = serieRepo)

    composeTestRule.setContent {
      ActivityGroupScreen(groupId = "1", activityGroupViewModel = viewModel)
    }

    waitForContent()

    // After error, loading should stop and empty message should show
    composeTestRule
        .onNodeWithTag(ActivityGroupScreenTestTags.LOADING_INDICATOR)
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(ActivityGroupScreenTestTags.EMPTY_ACTIVITY_LIST_MSG)
        .assertIsDisplayed()
  }

  /** Fake group repository for testing */
  private class FakeGroupRepository(private val shouldThrowError: Boolean = false) :
      GroupRepository {
    private val groups: MutableMap<String, Group> = mutableMapOf()

    override suspend fun getGroup(groupId: String): Group {
      if (shouldThrowError) {
        throw Exception("Failed to fetch group")
      }
      return groups[groupId] ?: throw NoSuchElementException("Group not found")
    }

    override suspend fun getAllGroups(): List<Group> = groups.values.toList()

    override suspend fun addGroup(group: Group) {
      groups[group.id] = group
    }

    override suspend fun editGroup(groupId: String, newValue: Group) {
      // No-op for testing
    }

    override suspend fun deleteGroup(groupId: String, userId: String) {
      // No-op for testing
    }

    override suspend fun leaveGroup(groupId: String, userId: String) {
      // No-op for testing
    }

    override suspend fun joinGroup(groupId: String, userId: String) {
      // No-op for testing
    }

    override fun getNewGroupId(): String = (groups.size + 1).toString()
  }

  /** Fake events repository for testing */
  private class FakeActivityGroupEventsRepository : EventsRepository {
    private val events: MutableMap<String, Event> = mutableMapOf()

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
      return eventIds.mapNotNull { events[it] }
    }

    override suspend fun getAllEvents(
        eventFilter: com.android.joinme.model.event.EventFilter
    ): List<Event> {
      return events.values.toList()
    }

    override suspend fun getEvent(eventId: String): Event {
      return events[eventId] ?: throw NoSuchElementException("Event not found")
    }

    override suspend fun addEvent(event: Event) {
      events[event.eventId] = event
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      // No-op for testing
    }

    override suspend fun deleteEvent(eventId: String) {
      // No-op for testing
    }

    override fun getNewEventId(): String {
      return (events.size + 1).toString()
    }
  }

  /** Fake series repository for testing */
  private class FakeActivityGroupSeriesRepository : SeriesRepository {
    private val series: MutableMap<String, Serie> = mutableMapOf()

    override suspend fun getSeriesByIds(seriesIds: List<String>): List<Serie> {
      return seriesIds.mapNotNull { series[it] }
    }

    override fun getNewSerieId(): String {
      return (series.size + 1).toString()
    }

    override suspend fun getAllSeries(
        serieFilter: com.android.joinme.model.serie.SerieFilter
    ): List<Serie> {
      // No-op for testing
      return series.values.toList()
    }

    override suspend fun getSerie(serieId: String): Serie {
      return series[serieId] ?: throw NoSuchElementException("Serie not found")
    }

    override suspend fun addSerie(serie: Serie) {
      series[serie.serieId] = serie
    }

    override suspend fun editSerie(serieId: String, newValue: Serie) {
      // No-op for testing
    }

    override suspend fun deleteSerie(serieId: String) {
      // No-op for testing
    }
  }
}
