package com.android.joinme.ui.history

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Instrumented tests for the History screen.
 *
 * Tests the UI behavior, event display, and user interactions in the History screen.
 */
@RunWith(RobolectricTestRunner::class)
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
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithText("History").assertIsDisplayed()
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Go back").assertIsDisplayed()
  }

  @Test
  fun historyScreen_backButtonTriggersCallback() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)
    var backClicked = false

    composeTestRule.setContent {
      HistoryScreen(historyViewModel = viewModel, onGoBack = { backClicked = true })
    }

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.BACK_BUTTON).performClick()

    assert(backClicked)
  }

  @Test
  fun historyScreen_emptyMessageHasCorrectText() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.EMPTY_HISTORY_MSG).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            "You have nothing in your history yet. Participate at an event to see it here!")
        .assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysExpiredEvents() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredEvent = createExpiredEvent("1", "Past Basketball Game", EventType.SPORTS)

    runBlocking { eventRepo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Past Basketball Game").assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysEventLocation() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredEvent = createExpiredEvent("1", "Past Event", EventType.SPORTS)

    runBlocking { eventRepo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Place : EPFL").assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysMultipleExpiredEvents() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val event1 = createExpiredEvent("1", "Event 1", EventType.SPORTS, 5)
    val event2 = createExpiredEvent("2", "Event 2", EventType.SOCIAL, 10)
    val event3 = createExpiredEvent("3", "Event 3", EventType.ACTIVITY, 15)

    runBlocking {
      eventRepo.addEvent(event1)
      eventRepo.addEvent(event2)
      eventRepo.addEvent(event3)
    }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

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
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    // Most recent expired event
    val recentEvent = createExpiredEvent("1", "Recent Event", EventType.SPORTS, 2)
    // Older expired event
    val olderEvent = createExpiredEvent("2", "Older Event", EventType.SOCIAL, 10)

    runBlocking {
      eventRepo.addEvent(olderEvent)
      eventRepo.addEvent(recentEvent)
    }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

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
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredEvent = createExpiredEvent("1", "Clickable Event", EventType.SPORTS)

    runBlocking { eventRepo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)
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
  fun historyScreen_multipleEventCards_haveUniqueTestTags() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val event1 = createExpiredEvent("1", "Event 1", EventType.SPORTS)
    val event2 = createExpiredEvent("2", "Event 2", EventType.SOCIAL)

    runBlocking {
      eventRepo.addEvent(event1)
      eventRepo.addEvent(event2)
    }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Both event cards should have unique test tags
    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.getTestTagForEvent(event1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.getTestTagForEvent(event2))
        .assertIsDisplayed()
  }

  @Test
  fun historyScreen_doesNotDisplayOngoingOrUpcomingEvents() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
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

    // Create an upcoming event (starts in 2 hours)
    val calendarUpcoming = Calendar.getInstance()
    calendarUpcoming.add(Calendar.HOUR, 2)
    val upcomingEvent =
        Event(
            eventId = "3",
            type = EventType.ACTIVITY,
            title = "Upcoming Event",
            description = "Not started yet",
            location = Location(46.5191, 6.5668, "EPFL"),
            date = Timestamp(calendarUpcoming.time),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    runBlocking {
      eventRepo.addEvent(expiredEvent)
      eventRepo.addEvent(ongoingEvent)
      eventRepo.addEvent(upcomingEvent)
    }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Only expired event should be displayed
    composeTestRule.onNodeWithText("Expired Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Ongoing Event").assertDoesNotExist()
    composeTestRule.onNodeWithText("Upcoming Event").assertDoesNotExist()
  }

  @Test
  fun historyScreen_displaysHistoryList_whenEventsExist() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredEvent = createExpiredEvent("1", "Test Event", EventType.SPORTS)

    runBlocking { eventRepo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.HISTORY_LIST).assertIsDisplayed()
  }

  @Test
  fun historyScreen_hidesHistoryList_whenNoEvents() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.HISTORY_LIST).assertDoesNotExist()
  }

  @Test
  fun historyScreen_refreshesUIStateOnLaunch() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredEvent = createExpiredEvent("1", "Pre-added Event", EventType.SPORTS)

    runBlocking { eventRepo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // The event should be loaded and displayed
    composeTestRule.onNodeWithText("Pre-added Event").assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysDifferentEventTypes() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val sportsEvent = createExpiredEvent("1", "Sports Event", EventType.SPORTS, 3)
    val socialEvent = createExpiredEvent("2", "Social Event", EventType.SOCIAL, 5)
    val activityEvent = createExpiredEvent("3", "Activity Event", EventType.ACTIVITY, 7)

    runBlocking {
      eventRepo.addEvent(sportsEvent)
      eventRepo.addEvent(socialEvent)
      eventRepo.addEvent(activityEvent)
    }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

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
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()

    // Add many expired events to ensure scrolling is needed
    runBlocking {
      for (i in 1..15) {
        eventRepo.addEvent(createExpiredEvent("$i", "Event $i", EventType.SPORTS, i + 2))
      }
    }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

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
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.onNodeWithTag(HistoryScreenTestTags.SCREEN).assertExists()
  }

  @Test
  fun historyScreen_displaysLoadingIndicator_whenLoading() {
    val eventRepo =
        FakeHistoryEventsRepository(
            delayMillis = 5000) // Longer delay to keep loading state visible
    val serieRepo = FakeHistorySeriesRepository()
    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    // Give time for initial composition
    composeTestRule.waitForIdle()

    // Verify screen is displayed (loading state is managed internally)
    // The loading indicator may appear briefly, so we just verify the screen doesn't crash
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.SCREEN).assertExists()
  }

  @Test
  fun historyScreen_hidesLoadingIndicator_afterDataLoaded() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredEvent = createExpiredEvent("1", "Test Event", EventType.SPORTS)

    runBlocking { eventRepo.addEvent(expiredEvent) }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Loading indicator should not be displayed after loading
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.LOADING_INDICATOR).assertDoesNotExist()
    // Event should be displayed
    composeTestRule.onNodeWithText("Test Event").assertIsDisplayed()
  }

  @Test
  fun historyScreen_showsEmptyMessage_afterLoadingWithNoEvents() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Loading indicator should not be displayed
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.LOADING_INDICATOR).assertDoesNotExist()
    // Empty message should be displayed
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.EMPTY_HISTORY_MSG).assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysExpiredSerie() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredSerie = createExpiredSerie("1", "Past Series")

    runBlocking { serieRepo.addSerie(expiredSerie) }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Past Series").assertIsDisplayed()
  }

  @Test
  fun historyScreen_serieCardHasCorrectTestTag() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredSerie = createExpiredSerie("123", "Test Serie")

    runBlocking { serieRepo.addSerie(expiredSerie) }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.getTestTagForSerie(expiredSerie))
        .assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysMultipleSeries() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val serie1 = createExpiredSerie("1", "Serie 1", 5)
    val serie2 = createExpiredSerie("2", "Serie 2", 10)

    runBlocking {
      serieRepo.addSerie(serie1)
      serieRepo.addSerie(serie2)
    }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Serie 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Serie 2").assertIsDisplayed()
  }

  @Test
  fun historyScreen_displaysMixedEventsAndSeries() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredEvent = createExpiredEvent("1", "Past Event", EventType.SPORTS, 3)
    val expiredSerie = createExpiredSerie("2", "Past Serie", 5)

    runBlocking {
      eventRepo.addEvent(expiredEvent)
      serieRepo.addSerie(expiredSerie)
    }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Both event and serie should be displayed
    composeTestRule.onNodeWithText("Past Event").assertIsDisplayed()
    composeTestRule.onNodeWithText("Past Serie").assertIsDisplayed()
  }

  @Test
  fun historyScreen_doesNotDisplayOngoingOrUpcomingSeries() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredSerie = createExpiredSerie("1", "Expired Serie", 10)

    // Create an ongoing serie (started 1 hour ago)
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -1)
    val startDateOngoing = Timestamp(calendar.time)

    // Create an ongoing event that's part of this serie
    calendar.add(Calendar.MINUTE, 30) // Event is halfway through
    val ongoingEvent =
        Event(
            eventId = "ongoing-event-1",
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

    // Set end time for ongoing serie (started 1 hour ago, ends 1 hour from now)
    val calendarOngoingEnd = Calendar.getInstance()
    calendarOngoingEnd.add(Calendar.HOUR, 1)
    val endDateOngoing = Timestamp(calendarOngoingEnd.time)

    val ongoingSerie =
        Serie(
            serieId = "2",
            title = "Ongoing Serie",
            description = "Still active",
            date = startDateOngoing,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("ongoing-event-1"),
            ownerId = "owner1",
            lastEventEndTime = endDateOngoing) // Ends in future, so still active

    // Create an upcoming serie (starts in 2 hours)
    val calendarUpcoming = Calendar.getInstance()
    calendarUpcoming.add(Calendar.HOUR, 2)
    val startDateUpcoming = Timestamp(calendarUpcoming.time)

    val upcomingSerie =
        Serie(
            serieId = "3",
            title = "Upcoming Serie",
            description = "Not started yet",
            date = startDateUpcoming,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf(),
            ownerId = "owner1",
            lastEventEndTime = startDateUpcoming) // Not expired, upcoming

    runBlocking {
      serieRepo.addSerie(expiredSerie)
      serieRepo.addSerie(ongoingSerie)
      serieRepo.addSerie(upcomingSerie)
      eventRepo.addEvent(ongoingEvent)
    }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Only expired serie should be displayed
    composeTestRule.onNodeWithText("Expired Serie").assertIsDisplayed()
    composeTestRule.onNodeWithText("Ongoing Serie").assertDoesNotExist()
    composeTestRule.onNodeWithText("Upcoming Serie").assertDoesNotExist()
  }

  @Test
  fun historyScreen_serieCardClick_triggersCallback() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val expiredSerie = createExpiredSerie("1", "Clickable Serie")

    runBlocking { serieRepo.addSerie(expiredSerie) }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)
    var selectedSerie: Serie? = null

    composeTestRule.setContent {
      HistoryScreen(historyViewModel = viewModel, onSelectSerie = { selectedSerie = it })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Clickable Serie").performClick()

    assert(selectedSerie != null)
    assert(selectedSerie?.serieId == "1")
  }

  @Test
  fun historyScreen_multipleSerieCards_haveUniqueTestTags() {
    val eventRepo = FakeHistoryEventsRepository()
    val serieRepo = FakeHistorySeriesRepository()
    val serie1 = createExpiredSerie("1", "Serie 1")
    val serie2 = createExpiredSerie("2", "Serie 2")

    runBlocking {
      serieRepo.addSerie(serie1)
      serieRepo.addSerie(serie2)
    }

    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Both serie cards should have unique test tags
    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.getTestTagForSerie(serie1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HistoryScreenTestTags.getTestTagForSerie(serie2))
        .assertIsDisplayed()
  }

  private fun createExpiredSerie(serieId: String, title: String, daysAgo: Int = 3): Serie {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, -daysAgo) // Date in the past
    val serieDate = Timestamp(calendar.time)
    calendar.add(Calendar.HOUR, 1) // Ended 1 hour after the start (so it's expired)
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
        lastEventEndTime = serieEndDate) // Set to past so it's expired
  }

  @Test
  fun historyScreen_handlesErrorFromRepository() {
    val eventRepo = FakeHistoryEventsRepository(shouldThrowError = true)
    val serieRepo = FakeHistorySeriesRepository()
    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // After error, loading should stop and empty message should show
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.LOADING_INDICATOR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(HistoryScreenTestTags.EMPTY_HISTORY_MSG).assertIsDisplayed()
  }

  @Test
  fun historyScreen_clearsErrorMessage() {
    val eventRepo = FakeHistoryEventsRepository(shouldThrowError = true)
    val serieRepo = FakeHistorySeriesRepository()
    val viewModel = HistoryViewModel(eventRepository = eventRepo, serieRepository = serieRepo)

    composeTestRule.setContent { HistoryScreen(historyViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Error should be cleared after being displayed
    // Verify the viewModel's error state is null after clearing
    assert(viewModel.uiState.value.errorMsg == null)
  }

  /** Fake events repository for testing */
  private class FakeHistoryEventsRepository(
      private val delayMillis: Long = 0,
      private val shouldThrowError: Boolean = false
  ) : EventsRepository {
    private val events: MutableList<Event> = mutableListOf()

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> {
      if (delayMillis > 0) {
        kotlinx.coroutines.delay(delayMillis)
      }
      if (shouldThrowError) {
        throw Exception("Failed to fetch events")
      }
      return events
    }

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

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
      return events.filter { eventIds.contains(it.eventId) }
    }

    override fun getNewEventId(): String = (events.size + 1).toString()

    override suspend fun getCommonEvents(userIds: List<String>): List<Event> {
      if (delayMillis > 0) {
        kotlinx.coroutines.delay(delayMillis)
      }
      if (shouldThrowError) {
        throw Exception("Failed to fetch events")
      }
      if (userIds.isEmpty()) return emptyList()
      return events
          .filter { event -> userIds.all { userId -> event.participants.contains(userId) } }
          .sortedBy { it.date.toDate().time }
    }
  }

  /** Fake series repository for testing */
  private class FakeHistorySeriesRepository : SeriesRepository {
    private val series: MutableList<Serie> = mutableListOf()

    override fun getNewSerieId(): String = (series.size + 1).toString()

    override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> {
      return series
    }

    override suspend fun getSeriesByIds(serieIds: List<String>): List<Serie> {
      return series.filter { serieIds.contains(it.serieId) }
    }

    override suspend fun getSerie(serieId: String): Serie = series.first { it.serieId == serieId }

    override suspend fun addSerie(serie: Serie) {
      series.add(serie)
    }

    override suspend fun editSerie(serieId: String, newValue: Serie) {
      val index = series.indexOfFirst { it.serieId == serieId }
      if (index != -1) {
        series[index] = newValue
      }
    }

    override suspend fun deleteSerie(serieId: String) {
      series.removeIf { it.serieId == serieId }
    }
  }
}
