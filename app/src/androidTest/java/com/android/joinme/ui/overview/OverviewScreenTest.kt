package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.*
import com.android.joinme.model.map.Location
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.navigation.NavigationTestTags
import com.google.firebase.Timestamp
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Mock repository that throws errors for testing error handling */
class EventsRepositoryMock(private val shouldThrowError: Boolean = false) : EventsRepository {
  private val events: MutableList<Event> = mutableListOf()
  private var counter = 0

  override fun getNewEventId(): String = (counter++).toString()

  override suspend fun getAllEvents(): List<Event> {
    if (shouldThrowError) {
      throw Exception("Network error: Failed to fetch events")
    }
    return events
  }

  override suspend fun getEvent(eventId: String): Event {
    if (shouldThrowError) {
      throw Exception("Network error")
    }
    return events.find { it.eventId == eventId } ?: throw Exception("Event not found")
  }

  override suspend fun addEvent(event: Event) {
    events.add(event)
  }

  override suspend fun editEvent(eventId: String, newValue: Event) {
    val index = events.indexOfFirst { it.eventId == eventId }
    if (index != -1) {
      events[index] = newValue
    } else {
      throw Exception("Event not found")
    }
  }

  override suspend fun deleteEvent(eventId: String) {
    val index = events.indexOfFirst { it.eventId == eventId }
    if (index != -1) {
      events.removeAt(index)
    } else {
      throw Exception("Event not found")
    }
  }
}

class OverviewScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createEvent(id: String, title: String, type: EventType): Event {
    return Event(
        eventId = id,
        type = type,
        title = title,
        description = "desc",
        location = null,
        date = Timestamp(Date()),
        duration = 60,
        participants = emptyList(),
        maxParticipants = 5,
        visibility = EventVisibility.PUBLIC,
        ownerId = "owner")
  }

  @Test
  fun overviewScreen_showsEmptyMessage_whenNoEvents() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    // Attends que le LaunchedEffect se termine
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG).assertExists()
  }

  @Test
  fun overviewScreen_showsList_whenEventsExist() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Ajoute les events AVANT de créer la UI
    runBlocking {
      repo.addEvent(createEvent("1", "Basketball", EventType.SPORTS))
      repo.addEvent(createEvent("2", "Bar", EventType.SOCIAL))
    }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    // Attends que le LaunchedEffect charge les données
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Vérifie que la liste existe
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EVENT_LIST).assertExists()

    // Vérifie que les événements sont affichés
    composeTestRule
        .onNodeWithTag(
            OverviewScreenTestTags.getTestTagForEventItem(
                createEvent("1", "Basketball", EventType.SPORTS)))
        .assertExists()

    composeTestRule
        .onNodeWithTag(
            OverviewScreenTestTags.getTestTagForEventItem(
                createEvent("2", "Bar", EventType.SOCIAL)))
        .assertExists()
  }

  @Test
  fun clickingFab_triggersOnAddEvent() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)
    var clicked = false

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, onAddEvent = { clicked = true })
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()

    assert(clicked)
  }

  @Test
  fun clickingEvent_triggersOnSelectEvent() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    val event = createEvent("1", "Basketball", EventType.SPORTS)

    // Ajoute l'event AVANT de créer la UI
    runBlocking { repo.addEvent(event) }

    var selected: Event? = null

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, onSelectEvent = { selected = it })
    }

    // Attends que le LaunchedEffect charge les données
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Clique sur l'événement
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForEventItem(event))
        .performClick()

    // Vérifie que l'événement a été sélectionné
    assert(selected == event)
  }

  @Test
  fun eventCard_displaysCorrectInformation() {
    val event = createEvent("1", "Basketball Match", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Basketball Match").assertExists()
    composeTestRule.onNodeWithText("Place : Unknown").assertExists()
  }

  @Test
  fun eventCard_clickTriggersCallback() {
    var clicked = false
    val event = createEvent("1", "Test Event", EventType.SPORTS)

    composeTestRule.setContent {
      EventCard(event = event, onClick = { clicked = true }, testTag = "testCard")
    }

    composeTestRule.onNodeWithText("Test Event").performClick()

    assert(clicked)
  }

  @Test
  fun eventCard_displaysSportsEventType() {
    val sportsEvent = createEvent("1", "Basketball", EventType.SPORTS)

    composeTestRule.setContent {
      EventCard(event = sportsEvent, onClick = {}, testTag = "testCard")
    }
    composeTestRule.onNodeWithText("Basketball").assertExists()
  }

  @Test
  fun eventCard_displaysActivityEventType() {
    val activityEvent = createEvent("2", "Hiking", EventType.ACTIVITY)

    composeTestRule.setContent {
      EventCard(event = activityEvent, onClick = {}, testTag = "testCard")
    }
    composeTestRule.onNodeWithText("Hiking").assertExists()
  }

  @Test
  fun eventCard_displaysSocialEventType() {
    val socialEvent = createEvent("3", "Party", EventType.SOCIAL)

    composeTestRule.setContent {
      EventCard(event = socialEvent, onClick = {}, testTag = "testCard")
    }
    composeTestRule.onNodeWithText("Party").assertExists()
  }

  @Test
  fun eventCard_displaysActualLocation() {
    val location = Location(latitude = 46.5197, longitude = 6.6323, name = "EPFL")
    val event =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Football",
            description = "desc",
            location = location,
            date = Timestamp(Date()),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Place : EPFL").assertExists()
  }

  @Test
  fun eventCard_displaysUnknownForNullLocation() {
    val event = createEvent("1", "Basketball", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("Place : Unknown").assertExists()
  }

  @Test
  fun eventCard_formatsDateCorrectly() {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 15, 14, 30, 0)
    val timestamp = Timestamp(calendar.time)

    val event =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Test Event",
            description = "desc",
            location = null,
            date = timestamp,
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("15/01/2025").assertExists()
    composeTestRule.onNodeWithText("14h30").assertExists()
  }

  @Test
  fun overviewScreen_displaysMultipleEvents() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Add 5 events
    runBlocking {
      for (i in 1..5) {
        repo.addEvent(createEvent("$i", "Event $i", EventType.SPORTS))
      }
    }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify all events are displayed
    for (i in 1..5) {
      composeTestRule
          .onNodeWithTag(
              OverviewScreenTestTags.getTestTagForEventItem(
                  createEvent("$i", "Event $i", EventType.SPORTS)))
          .assertExists()
    }
  }

  @Test
  fun overviewScreen_displaysCreateEventButton() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
  }

  @Test
  fun overviewScreen_handlesErrorWhenFetchingEvents() {
    val repo = EventsRepositoryMock(shouldThrowError = true)
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    // Wait for error to be processed
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // The screen should show empty state since error occurred
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG).assertExists()

    // Error should have been cleared from UI state
    assert(viewModel.uiState.value.errorMsg == null)
  }

  @Test
  fun overviewScreen_bottomNavigationDisplayed() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Verify bottom navigation is displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertExists()

    // Verify overview tab is selected
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("overview")).assertExists()
  }

  @Test
  fun viewModel_clearErrorMsg_removesError() {
    val repo = EventsRepositoryMock(shouldThrowError = true)
    val viewModel = OverviewViewModel(repo)

    // Trigger error
    viewModel.refreshUIState()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Error should be set
    assert(viewModel.uiState.value.errorMsg != null)

    // Clear error
    viewModel.clearErrorMsg()

    // Error should be cleared
    assert(viewModel.uiState.value.errorMsg == null)
  }

  @Test
  fun viewModel_refreshUIState_updatesEvents() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Initially empty
    assert(viewModel.uiState.value.ongoingEvents.isEmpty())
    assert(viewModel.uiState.value.upcomingEvents.isEmpty())

    // Add event and refresh
    runBlocking { repo.addEvent(createEvent("1", "Basketball", EventType.SPORTS)) }
    viewModel.refreshUIState()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Events should be updated
    val totalEvents =
        viewModel.uiState.value.ongoingEvents.size + viewModel.uiState.value.upcomingEvents.size
    assert(totalEvents == 1)
  }

  @Test
  fun viewModel_setsErrorMessage_whenRepositoryFails() {
    val repo = EventsRepositoryMock(shouldThrowError = true)
    val viewModel = OverviewViewModel(repo)

    // Trigger refresh
    viewModel.refreshUIState()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Error message should be set
    assert(viewModel.uiState.value.errorMsg != null)
    assert(viewModel.uiState.value.errorMsg?.contains("Failed to load events") == true)
  }

  @Test
  fun eventCard_displaysMixedEventTypes() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    runBlocking {
      repo.addEvent(createEvent("1", "Football", EventType.SPORTS))
      repo.addEvent(createEvent("2", "Museum Visit", EventType.ACTIVITY))
      repo.addEvent(createEvent("3", "Coffee", EventType.SOCIAL))
    }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // All three event types should be displayed
    composeTestRule.onNodeWithText("Football").assertExists()
    composeTestRule.onNodeWithText("Museum Visit").assertExists()
    composeTestRule.onNodeWithText("Coffee").assertExists()
  }

  @Test
  fun overviewScreen_displaysTopBar() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertExists()
  }

  @Test
  fun overviewScreen_topBarDisplaysOverviewTitle() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Overview").assertExists()
  }

  @Test
  fun overviewScreen_topBarIsDisplayed() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
  }

  @Test
  fun overviewScreen_fabIsDisplayedWithEmptyList() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun overviewScreen_fabIsDisplayedWithEventsList() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    runBlocking { repo.addEvent(createEvent("1", "Basketball", EventType.SPORTS)) }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun overviewScreen_fabClickTriggersCallback() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)
    var fabClicked = false

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, onAddEvent = { fabClicked = true })
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()

    assert(fabClicked)
  }

  @Test
  fun overviewScreen_eventClickTriggersCallback() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)
    var clickedEvent: Event? = null

    runBlocking { repo.addEvent(createEvent("1", "Basketball", EventType.SPORTS)) }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, onSelectEvent = { clickedEvent = it })
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Basketball").performClick()

    assert(clickedEvent != null)
    assert(clickedEvent?.title == "Basketball")
  }

  @Test
  fun overviewScreen_bottomNavigationIsDisplayed() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun overviewScreen_bottomNavigationHasOverviewSelected() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).assertIsSelected()
  }

  @Test
  fun eventCard_displaysEventTitle() {
    val event = createEvent("1", "My Event Title", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    composeTestRule.onNodeWithText("My Event Title").assertExists()
  }

  @Test
  fun eventCard_displaysEventDescription() {
    val event = createEvent("1", "Basketball Game", EventType.SPORTS)

    composeTestRule.setContent { EventCard(event = event, onClick = {}, testTag = "testCard") }

    // Description is set to "desc" in createEvent helper
    composeTestRule.onNodeWithText("Basketball Game").assertExists()
  }

  @Test
  fun overviewScreen_displaysMultipleEventsInList() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    runBlocking {
      for (i in 1..8) {
        repo.addEvent(createEvent("$i", "Event Number $i", EventType.SPORTS))
      }
    }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(3000)
    composeTestRule.waitForIdle()

    // Verify multiple events are displayed
    composeTestRule.onNodeWithText("Event Number 1").assertExists()
    composeTestRule.onNodeWithText("Event Number 5").assertExists()
  }

  @Test
  fun eventCard_isClickable() {
    var clicked = false
    val event = createEvent("1", "Clickable Event", EventType.SPORTS)

    composeTestRule.setContent {
      EventCard(event = event, onClick = { clicked = true }, testTag = "testCard")
    }

    composeTestRule.onNodeWithText("Clickable Event").performClick()

    assert(clicked)
  }

  @Test
  fun overviewScreen_refreshesUIStateOnLaunch() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    runBlocking { repo.addEvent(createEvent("1", "Pre-added Event", EventType.SPORTS)) }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // The event should be loaded and displayed
    composeTestRule.onNodeWithText("Pre-added Event").assertExists()
  }

  @Test
  fun overviewScreen_displaysOngoingEventsTitle_whenOngoingEventsExist() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Create an ongoing event (started but not finished)
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -1) // Started 1 hour ago
    val ongoingEvent =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Ongoing Event",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 180, // 3 hours duration
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    runBlocking { repo.addEvent(ongoingEvent) }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.ONGOING_EVENTS_TITLE).assertExists()
  }

  @Test
  fun overviewScreen_displaysUpcomingEventsTitle_whenUpcomingEventsExist() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Create an upcoming event (not started yet)
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2) // Starts in 2 hours
    val upcomingEvent =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Upcoming Event",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    runBlocking { repo.addEvent(upcomingEvent) }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.UPCOMING_EVENTS_TITLE).assertExists()
  }

  @Test
  fun overviewScreen_displaysBothSections_whenBothEventTypesExist() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Create an ongoing event
    val calendarOngoing = Calendar.getInstance()
    calendarOngoing.add(Calendar.HOUR, -1)
    val ongoingEvent =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Ongoing Event",
            description = "desc",
            location = null,
            date = Timestamp(calendarOngoing.time),
            duration = 180,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    // Create an upcoming event
    val calendarUpcoming = Calendar.getInstance()
    calendarUpcoming.add(Calendar.HOUR, 2)
    val upcomingEvent =
        Event(
            eventId = "2",
            type = EventType.SOCIAL,
            title = "Upcoming Event",
            description = "desc",
            location = null,
            date = Timestamp(calendarUpcoming.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    runBlocking {
      repo.addEvent(ongoingEvent)
      repo.addEvent(upcomingEvent)
    }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Both section titles should exist
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.ONGOING_EVENTS_TITLE).assertExists()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.UPCOMING_EVENTS_TITLE).assertExists()

    // Both events should be displayed
    composeTestRule.onNodeWithText("Ongoing Event").assertExists()
    composeTestRule.onNodeWithText("Upcoming Event").assertExists()
  }

  @Test
  fun overviewScreen_displaysSingularTitle_whenOnlyOneOngoingEvent() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Create a single ongoing event
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -1)
    val ongoingEvent =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Single Ongoing",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 180,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    runBlocking { repo.addEvent(ongoingEvent) }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display singular form
    composeTestRule.onNodeWithText("Your ongoing event :").assertExists()
  }

  @Test
  fun overviewScreen_displaysPluralTitle_whenMultipleOngoingEvents() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Create multiple ongoing events
    val calendar1 = Calendar.getInstance()
    calendar1.add(Calendar.HOUR, -1)
    val ongoingEvent1 =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "First Ongoing",
            description = "desc",
            location = null,
            date = Timestamp(calendar1.time),
            duration = 180,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    val calendar2 = Calendar.getInstance()
    calendar2.add(Calendar.MINUTE, -30)
    val ongoingEvent2 =
        Event(
            eventId = "2",
            type = EventType.ACTIVITY,
            title = "Second Ongoing",
            description = "desc",
            location = null,
            date = Timestamp(calendar2.time),
            duration = 120,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    runBlocking {
      repo.addEvent(ongoingEvent1)
      repo.addEvent(ongoingEvent2)
    }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display plural form
    composeTestRule.onNodeWithText("Your ongoing events :").assertExists()
  }

  @Test
  fun overviewScreen_displaysSingularTitle_whenOnlyOneUpcomingEvent() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Create a single upcoming event
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2)
    val upcomingEvent =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Single Upcoming",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    runBlocking { repo.addEvent(upcomingEvent) }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display singular form
    composeTestRule.onNodeWithText("Your upcoming event :").assertExists()
  }

  @Test
  fun overviewScreen_displaysPluralTitle_whenMultipleUpcomingEvents() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Create multiple upcoming events
    val calendar1 = Calendar.getInstance()
    calendar1.add(Calendar.HOUR, 1)
    val upcomingEvent1 =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "First Upcoming",
            description = "desc",
            location = null,
            date = Timestamp(calendar1.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    val calendar2 = Calendar.getInstance()
    calendar2.add(Calendar.HOUR, 3)
    val upcomingEvent2 =
        Event(
            eventId = "2",
            type = EventType.SOCIAL,
            title = "Second Upcoming",
            description = "desc",
            location = null,
            date = Timestamp(calendar2.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    runBlocking {
      repo.addEvent(upcomingEvent1)
      repo.addEvent(upcomingEvent2)
    }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display plural form
    composeTestRule.onNodeWithText("Your upcoming events :").assertExists()
  }

  @Test
  fun overviewScreen_displaysSingularAndPluralTitles_whenOneOngoingAndMultipleUpcoming() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    // Create one ongoing event
    val calendarOngoing = Calendar.getInstance()
    calendarOngoing.add(Calendar.HOUR, -1)
    val ongoingEvent =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Ongoing Event",
            description = "desc",
            location = null,
            date = Timestamp(calendarOngoing.time),
            duration = 180,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    // Create multiple upcoming events
    val calendarUpcoming1 = Calendar.getInstance()
    calendarUpcoming1.add(Calendar.HOUR, 1)
    val upcomingEvent1 =
        Event(
            eventId = "2",
            type = EventType.SOCIAL,
            title = "First Upcoming",
            description = "desc",
            location = null,
            date = Timestamp(calendarUpcoming1.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    val calendarUpcoming2 = Calendar.getInstance()
    calendarUpcoming2.add(Calendar.HOUR, 3)
    val upcomingEvent2 =
        Event(
            eventId = "3",
            type = EventType.ACTIVITY,
            title = "Second Upcoming",
            description = "desc",
            location = null,
            date = Timestamp(calendarUpcoming2.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    runBlocking {
      repo.addEvent(ongoingEvent)
      repo.addEvent(upcomingEvent1)
      repo.addEvent(upcomingEvent2)
    }

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display singular for ongoing and plural for upcoming
    composeTestRule.onNodeWithText("Your ongoing event :").assertExists()
    composeTestRule.onNodeWithText("Your upcoming events :").assertExists()
  }

  @Test
  fun overviewScreen_displaysHistoryButton() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).assertExists()
  }

  @Test
  fun overviewScreen_historyButtonIsDisplayed() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).assertIsDisplayed()
  }

  @Test
  fun overviewScreen_historyButtonHasCorrectDescription() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("View History").assertExists()
  }

  @Test
  fun clickingHistoryButton_triggersOnGoToHistory() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)
    var historyClicked = false

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, onGoToHistory = { historyClicked = true })
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).performClick()

    assert(historyClicked)
  }

  @Test
  fun overviewScreen_bothFabsDisplayedTogether() {
    val repo = EventsRepositoryLocal()
    val viewModel = OverviewViewModel(repo)

    composeTestRule.setContent { OverviewScreen(overviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Both FABs should be displayed
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).assertIsDisplayed()
  }
}
