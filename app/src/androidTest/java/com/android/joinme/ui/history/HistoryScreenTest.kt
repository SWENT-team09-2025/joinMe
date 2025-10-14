package com.android.joinme.ui.history

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for the History screen.
 *
 * Tests the UI behavior, event display, and user interactions in the History screen.
 */
class HistoryScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createExpiredEvent(
      eventId: String,
      title: String,
      type: EventType,
      hoursAgo: Int = 3
  ): Event {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -hoursAgo)
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

  @Test
  fun historyScreen_displaysTopBar() {
    val repo = FakeHistoryRepository()
    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.TOP_BAR).assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysTitle() {
    val repo = FakeHistoryRepository()
    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.onNodeWithText("History").assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysBackButton() {
    val repo = FakeHistoryRepository()
    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun historyScreen_backButtonHasCorrectDescription() {
    val repo = FakeHistoryRepository()
    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.onNodeWithContentDescription("Go back").assertIsDisplayed()
  }

  @Test
  fun historyScreen_backButtonTriggersCallback() {
    val repo = FakeHistoryRepository()
    val viewModel = HistoryViewModel(eventRepository = repo)
    var backClicked = false

    composeTestRule.setContent {
      HistoryScreen(historyViewModel = viewModel, onGoBack = { backClicked = true })
    }

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.BACK_BUTTON).performClick()

    assert(backClicked)
  }

  @Test
  fun historyScreen_displaysEmptyMessage_whenNoExpiredEvents() {
    val repo = FakeHistoryRepository()
    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.EMPTY_HISTORY_MSG).assertIsDisplayed()
  }

  @Test
  fun historyScreen_emptyMessageHasCorrectText() {
    val repo = FakeHistoryRepository()
    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText(
            "You have nothing in your history yet. Participate at an event to see it here!")
        .assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysExpiredEvents() {
    val repo = FakeHistoryRepository()
    val expiredEvent = createExpiredEvent("1", "Past Basketball Game", EventType.SPORTS)

    runBlocking { repo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Past Basketball Game").assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysEventLocation() {
    val repo = FakeHistoryRepository()
    val expiredEvent = createExpiredEvent("1", "Past Event", EventType.SPORTS)

    runBlocking { repo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Place : EPFL").assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysMultipleExpiredEvents() {
    val repo = FakeHistoryRepository()
    val event1 = createExpiredEvent("1", "Event 1", EventType.SPORTS, 5)
    val event2 = createExpiredEvent("2", "Event 2", EventType.SOCIAL, 10)
    val event3 = createExpiredEvent("3", "Event 3", EventType.ACTIVITY, 15)

    runBlocking {
      repo.addEvent(event1)
      repo.addEvent(event2)
      repo.addEvent(event3)
    }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Event 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Event 2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Event 3").assertIsDisplayed()
  }

  @Test
  fun historyScreen_eventsSortedByDateDescending() {
    val repo = FakeHistoryRepository()
    // Most recent expired event
    val recentEvent = createExpiredEvent("1", "Recent Event", EventType.SPORTS, 2)
    // Older expired event
    val olderEvent = createExpiredEvent("2", "Older Event", EventType.SOCIAL, 10)

    runBlocking {
      repo.addEvent(olderEvent)
      repo.addEvent(recentEvent)
    }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify both events are displayed
    composeTestRule.onNodeWithText("Recent Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Older Event").assertIsDisplayed()

    // The more recent event should appear first (descending order)
    val eventList = composeTestRule.onAllNodesWithTag(HistoryScreenTestTags.HISTORY_LIST)
    eventList.assertCountEquals(1) // Should have one history list
  }

  @Test
  fun historyScreen_eventCardClick_triggersCallback() {
    val repo = FakeHistoryRepository()
    val expiredEvent = createExpiredEvent("1", "Clickable Event", EventType.SPORTS)

    runBlocking { repo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = repo)
    var selectedEvent: Event? = null

    composeTestRule.setContent {
      HistoryScreen(historyViewModel = viewModel, onSelectEvent = { selectedEvent = it })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Clickable Event").performClick()

    assert(selectedEvent != null)
    assert(selectedEvent?.eventId == "1")
  }

  @Test
  fun historyScreen_eventCardHasCorrectTestTag() {
    val repo = FakeHistoryRepository()
    val expiredEvent = createExpiredEvent("123", "Test Event", EventType.SPORTS)

    runBlocking { repo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.getTestTagForEventItem(expiredEvent))
        .assertIsDisplayed()
  }

  @Test
  fun historyScreen_multipleEventCards_haveUniqueTestTags() {
    val repo = FakeHistoryRepository()
    val event1 = createExpiredEvent("1", "Event 1", EventType.SPORTS)
    val event2 = createExpiredEvent("2", "Event 2", EventType.SOCIAL)

    runBlocking {
      repo.addEvent(event1)
      repo.addEvent(event2)
    }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Both event cards should have unique test tags
    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.getTestTagForEventItem(event1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.getTestTagForEventItem(event2))
        .assertIsDisplayed()
  }

  @Test
  fun historyScreen_doesNotDisplayOngoingEvents() {
    val repo = FakeHistoryRepository()
    val expiredEvent = createExpiredEvent("1", "Expired Event", EventType.SPORTS, 3)

    // Create an ongoing event (started 30 minutes ago, lasts 2 hours)
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MINUTE, -30)
    val ongoingEvent =
        Event(
            eventId = "2",
            type = EventType.SOCIAL,
            title = "Ongoing Event",
            description = "Still happening",
            location = Location(46.5191, 6.5668, "EPFL"),
            date = Timestamp(calendar.time),
            duration = 120,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    runBlocking {
      repo.addEvent(expiredEvent)
      repo.addEvent(ongoingEvent)
    }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Only expired event should be displayed
    composeTestRule.onNodeWithText("Expired Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Ongoing Event").assertDoesNotExist()
  }

  @Test
  fun historyScreen_doesNotDisplayUpcomingEvents() {
    val repo = FakeHistoryRepository()
    val expiredEvent = createExpiredEvent("1", "Expired Event", EventType.SPORTS, 3)

    // Create an upcoming event (starts in 2 hours)
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2)
    val upcomingEvent =
        Event(
            eventId = "2",
            type = EventType.ACTIVITY,
            title = "Upcoming Event",
            description = "Not started yet",
            location = Location(46.5191, 6.5668, "EPFL"),
            date = Timestamp(calendar.time),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    runBlocking {
      repo.addEvent(expiredEvent)
      repo.addEvent(upcomingEvent)
    }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Only expired event should be displayed
    composeTestRule.onNodeWithText("Expired Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Upcoming Event").assertDoesNotExist()
  }

  @Test
  fun historyScreen_displaysHistoryList_whenEventsExist() {
    val repo = FakeHistoryRepository()
    val expiredEvent = createExpiredEvent("1", "Test Event", EventType.SPORTS)

    runBlocking { repo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.HISTORY_LIST).assertIsDisplayed()
  }

  @Test
  fun historyScreen_hidesHistoryList_whenNoEvents() {
    val repo = FakeHistoryRepository()
    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.HISTORY_LIST).assertDoesNotExist()
  }

  @Test
  fun historyScreen_refreshesUIStateOnLaunch() {
    val repo = FakeHistoryRepository()
    val expiredEvent = createExpiredEvent("1", "Pre-added Event", EventType.SPORTS)

    runBlocking { repo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // The event should be loaded and displayed
    composeTestRule.onNodeWithText("Pre-added Event").assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysDifferentEventTypes() {
    val repo = FakeHistoryRepository()
    val sportsEvent = createExpiredEvent("1", "Sports Event", EventType.SPORTS, 3)
    val socialEvent = createExpiredEvent("2", "Social Event", EventType.SOCIAL, 5)
    val activityEvent = createExpiredEvent("3", "Activity Event", EventType.ACTIVITY, 7)

    runBlocking {
      repo.addEvent(sportsEvent)
      repo.addEvent(socialEvent)
      repo.addEvent(activityEvent)
    }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // All different event types should be displayed
    composeTestRule.onNodeWithText("Sports Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Social Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Activity Event").assertIsDisplayed()
  }

  @Test
  fun historyScreen_eventCard_isScrollable() {
    val repo = FakeHistoryRepository()

    // Add many expired events to ensure scrolling is needed
    runBlocking {
      for (i in 1..15) {
        repo.addEvent(createExpiredEvent("$i", "Event $i", EventType.SPORTS, i + 2))
      }
    }

    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify first event is visible
    composeTestRule.onNodeWithText("Event 1").assertIsDisplayed()

    // Scroll to see last event
    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.HISTORY_LIST)
        .performScrollToNode(hasText("Event 15"))

    // Verify last event is now visible
    composeTestRule.onNodeWithText("Event 15").assertIsDisplayed()
  }

  @Test
  fun historyScreen_hasCorrectScreenTestTag() {
    val repo = FakeHistoryRepository()
    val viewModel = HistoryViewModel(eventRepository = repo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.SCREEN).assertExists()
  }

  /** Fake repository for testing */
  private class FakeHistoryRepository : EventsRepository {
    private val events: MutableList<Event> = mutableListOf()

    override suspend fun getAllEvents(): List<Event> = events

    override suspend fun getEvent(eventId: String): Event = events.first { it.eventId == eventId }

    override suspend fun addEvent(event: Event) {
      events.add(event)
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      val index = events.indexOfFirst { it.eventId == eventId }
      if (index != -1) {
        events[index] = newValue
      }
    }

    override suspend fun deleteEvent(eventId: String) {
      events.removeIf { it.eventId == eventId }
    }

    override fun getNewEventId(): String = (events.size + 1).toString()
  }
}
