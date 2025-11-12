package com.android.joinme.ui.overview
/*CO-Write with claude AI*/
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.joinme.model.event.*
import com.android.joinme.model.map.Location
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.serie.SeriesRepositoryLocal
import com.android.joinme.model.utils.Visibility
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

  override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> {
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

  override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
    return events.filter { eventIds.contains(it.eventId) }
  }
}

/** Mock series repository for testing */
class SeriesRepositoryMock(private val shouldThrowError: Boolean = false) : SeriesRepository {
  private val series: MutableList<Serie> = mutableListOf()
  private var counter = 0

  override fun getNewSerieId(): String = (counter++).toString()

  override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> {
    if (shouldThrowError) {
      throw Exception("Network error: Failed to fetch series")
    }
    return series
  }

  override suspend fun getSerie(serieId: String): Serie {
    if (shouldThrowError) {
      throw Exception("Network error")
    }
    return series.find { it.serieId == serieId } ?: throw Exception("Serie not found")
  }

  override suspend fun addSerie(serie: Serie) {
    series.add(serie)
  }

  override suspend fun editSerie(serieId: String, newValue: Serie) {
    val index = series.indexOfFirst { it.serieId == serieId }
    if (index != -1) {
      series[index] = newValue
    } else {
      throw Exception("Serie not found")
    }
  }

  override suspend fun deleteSerie(serieId: String) {
    val index = series.indexOfFirst { it.serieId == serieId }
    if (index != -1) {
      series.removeAt(index)
    } else {
      throw Exception("Serie not found")
    }
  }
}

class OverviewScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun createEvent(id: String, title: String, type: EventType): Event {
    return Event(
        eventId = id,
        type = type,
        title = title,
        description = "desc",
        location = null,
        date = Timestamp(Date()),
        duration = 60,
        participants = listOf("owner"),
        maxParticipants = 5,
        visibility = EventVisibility.PUBLIC,
        ownerId = "owner")
  }

  @Test
  fun overviewScreen_displaysBasicUIElementsWithEmptyState() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    // Attends que le LaunchedEffect se termine
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Empty message
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG).assertExists()

    // Create event button (FAB)
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertIsDisplayed()

    // Top bar
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertExists()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Overview").assertExists()

    // Bottom navigation
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertExists()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).assertIsSelected()

    // History button
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("View History").assertExists()

    // Both FABs displayed together
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).assertIsDisplayed()
  }

  @Test
  fun overviewScreen_showsList_whenEventsExist() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    // Ajoute les events AVANT de créer la UI
    runBlocking {
      eventRepo.addEvent(createEvent("1", "Basketball", EventType.SPORTS))
      eventRepo.addEvent(createEvent("2", "Bar", EventType.SOCIAL))
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    // Attends que le LaunchedEffect charge les données
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Vérifie que la liste existe
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EVENT_LIST).assertExists()

    // Vérifie que les événements sont affichés
    composeTestRule
        .onNodeWithTag(
            OverviewScreenTestTags.getTestTagForEvent(
                createEvent("1", "Basketball", EventType.SPORTS)))
        .assertExists()

    composeTestRule
        .onNodeWithTag(
            OverviewScreenTestTags.getTestTagForEvent(createEvent("2", "Bar", EventType.SOCIAL)))
        .assertExists()
  }

  @Test
  fun clickingFab_triggersOnAddEvent() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)
    var clicked = false

    composeTestRule.setContent {
      OverviewScreen(
          overviewViewModel = viewModel,
          onAddEvent = { clicked = true },
          enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    // Click FAB to open bubble menu
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    // Click "Add an event" bubble
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.ADD_EVENT_BUBBLE).performClick()

    assert(clicked)
  }

  @Test
  fun clickingEvent_triggersOnSelectEvent() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    val event = createEvent("1", "Basketball", EventType.SPORTS)

    // Ajoute l'event AVANT de créer la UI
    runBlocking { eventRepo.addEvent(event) }

    var selected: Event? = null

    composeTestRule.setContent {
      OverviewScreen(
          overviewViewModel = viewModel,
          onSelectEvent = { selected = it },
          enableNotificationPermissionRequest = false)
    }

    // Attends que le LaunchedEffect charge les données
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Clique sur l'événement
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.getTestTagForEvent(event)).performClick()

    // Vérifie que l'événement a été sélectionné
    assert(selected == event)
  }

  @Test
  fun eventCard_displaysCorrectInformationAndHandlesClick() {
    var clicked = false
    val event = createEvent("1", "Basketball Match", EventType.SPORTS)

    composeTestRule.setContent {
      EventCard(event = event, onClick = { clicked = true }, testTag = "testCard")
    }

    // Verify information is displayed correctly
    composeTestRule.onNodeWithText("Basketball Match").assertExists()
    composeTestRule.onNodeWithText("Place : Unknown").assertExists()

    // Verify click triggers callback
    composeTestRule.onNodeWithText("Basketball Match").performClick()
    assert(clicked)
  }

  @Test
  fun eventCard_displaysActualLocation() {
    val location = Location(latitude = 46.5197, longitude = 6.6323, name = "EPFL")
    val eventWithLocation =
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

    composeTestRule.setContent {
      EventCard(event = eventWithLocation, onClick = {}, testTag = "testCard")
    }
    composeTestRule.onNodeWithText("Place : EPFL").assertExists()
  }

  @Test
  fun eventCard_displaysUnknownForNullLocation() {
    val eventWithoutLocation = createEvent("2", "Basketball", EventType.SPORTS)
    composeTestRule.setContent {
      EventCard(event = eventWithoutLocation, onClick = {}, testTag = "testCard2")
    }
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
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    // Add 5 events
    runBlocking {
      for (i in 1..5) {
        eventRepo.addEvent(createEvent("$i", "Event $i", EventType.SPORTS))
      }
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify all events are displayed
    for (i in 1..5) {
      composeTestRule
          .onNodeWithTag(
              OverviewScreenTestTags.getTestTagForEvent(
                  createEvent("$i", "Event $i", EventType.SPORTS)))
          .assertExists()
    }
  }

  @Test
  fun overviewScreen_handlesErrorWhenFetchingEvents() {
    val eventRepo = EventsRepositoryMock(shouldThrowError = true)
    val serieRepo = SeriesRepositoryMock()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // The screen should show empty state since error occurred
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG).assertExists()
    // Error should have been cleared from UI state
    assert(viewModel.uiState.value.errorMsg == null)
  }

  @Test
  fun overviewScreen_handlesSeriesRepositoryError() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryMock(shouldThrowError = true)
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // The screen should show empty state since error occurred
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG).assertExists()
    // Error should have been cleared from UI state
    assert(viewModel.uiState.value.errorMsg == null)
  }

  @Test
  fun viewModel_setsErrorMessage_whenRepositoryFails() {
    val eventRepo = EventsRepositoryMock(shouldThrowError = true)
    val serieRepo = SeriesRepositoryMock()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    viewModel.refreshUIState()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Error message should be set
    assert(viewModel.uiState.value.errorMsg != null)
    assert(viewModel.uiState.value.errorMsg?.contains("Failed to load data") == true)
  }

  @Test
  fun viewModel_clearErrorMsg_removesError() {
    val eventRepo = EventsRepositoryMock(shouldThrowError = true)
    val serieRepo = SeriesRepositoryMock()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

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
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    // Initially empty
    assert(viewModel.uiState.value.ongoingItems.isEmpty())
    assert(viewModel.uiState.value.upcomingItems.isEmpty())

    // Add event and refresh
    runBlocking { eventRepo.addEvent(createEvent("1", "Basketball", EventType.SPORTS)) }
    viewModel.refreshUIState()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Events should be updated
    val totalItems =
        viewModel.uiState.value.ongoingItems.size + viewModel.uiState.value.upcomingItems.size
    assert(totalItems == 1)
  }

  @Test
  fun eventCard_displaysMixedEventTypes() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    runBlocking {
      eventRepo.addEvent(createEvent("1", "Football", EventType.SPORTS))
      eventRepo.addEvent(createEvent("2", "Museum Visit", EventType.ACTIVITY))
      eventRepo.addEvent(createEvent("3", "Coffee", EventType.SOCIAL))
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // All three event types should be displayed
    composeTestRule.onNodeWithText("Football").assertExists()
    composeTestRule.onNodeWithText("Museum Visit").assertExists()
    composeTestRule.onNodeWithText("Coffee").assertExists()
  }

  @Test
  fun overviewScreen_refreshesUIStateOnLaunch() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    runBlocking { eventRepo.addEvent(createEvent("1", "Pre-added Event", EventType.SPORTS)) }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // The event should be loaded and displayed
    composeTestRule.onNodeWithText("Pre-added Event").assertExists()
  }

  @Test
  fun overviewScreen_displaysOngoingEventsTitle_whenOngoingEventsExist() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

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

    runBlocking { eventRepo.addEvent(ongoingEvent) }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.ONGOING_EVENTS_TITLE).assertExists()
  }

  @Test
  fun overviewScreen_displaysUpcomingEventsTitle_whenUpcomingEventsExist() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

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

    runBlocking { eventRepo.addEvent(upcomingEvent) }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(OverviewScreenTestTags.UPCOMING_EVENTS_TITLE).assertExists()
  }

  @Test
  fun overviewScreen_displaysBothSections_whenBothEventTypesExist() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

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
      eventRepo.addEvent(ongoingEvent)
      eventRepo.addEvent(upcomingEvent)
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

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
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

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

    runBlocking { eventRepo.addEvent(ongoingEvent) }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display singular form
    composeTestRule.onNodeWithText("Your ongoing activity :").assertExists()
  }

  @Test
  fun overviewScreen_displaysPluralTitle_whenMultipleOngoingEvents() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

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
      eventRepo.addEvent(ongoingEvent1)
      eventRepo.addEvent(ongoingEvent2)
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display plural form
    composeTestRule.onNodeWithText("Your ongoing activities :").assertExists()
  }

  @Test
  fun overviewScreen_displaysSingularTitle_whenOnlyOneUpcomingEvent() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

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

    runBlocking { eventRepo.addEvent(upcomingEvent) }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display singular form
    composeTestRule.onNodeWithText("Your upcoming activity :").assertExists()
  }

  @Test
  fun overviewScreen_displaysPluralTitle_whenMultipleUpcomingEvents() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

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
      eventRepo.addEvent(upcomingEvent1)
      eventRepo.addEvent(upcomingEvent2)
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display plural form
    composeTestRule.onNodeWithText("Your upcoming activities :").assertExists()
  }

  @Test
  fun overviewScreen_displaysSingularAndPluralTitles_whenOneOngoingAndMultipleUpcoming() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

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
      eventRepo.addEvent(ongoingEvent)
      eventRepo.addEvent(upcomingEvent1)
      eventRepo.addEvent(upcomingEvent2)
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display singular for ongoing and plural for upcoming
    composeTestRule.onNodeWithText("Your ongoing activity :").assertExists()
    composeTestRule.onNodeWithText("Your upcoming activities :").assertExists()
  }

  @Test
  fun clickingHistoryButton_triggersOnGoToHistory() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)
    var historyClicked = false

    composeTestRule.setContent {
      OverviewScreen(
          overviewViewModel = viewModel,
          onGoToHistory = { historyClicked = true },
          enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.HISTORY_BUTTON).performClick()

    assert(historyClicked)
  }

  @Test
  fun overviewScreen_hidesLoadingIndicator_afterEventsLoaded() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    runBlocking { eventRepo.addEvent(createEvent("1", "Basketball", EventType.SPORTS)) }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Loading indicator should not exist after loading
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.LOADING_INDICATOR).assertDoesNotExist()
    // Events list should be displayed
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EVENT_LIST).assertExists()
  }

  @Test
  fun overviewScreen_hidesLoadingIndicator_whenNoEvents() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Loading indicator should not exist after loading
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.LOADING_INDICATOR).assertDoesNotExist()
    // Empty message should be displayed
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG).assertExists()
  }

  @Test
  fun overviewScreen_displaysSerieWithBadge() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 1)

    // Create events that belong to the serie
    val event1 =
        Event(
            eventId = "event1",
            type = EventType.SPORTS,
            title = "Basketball Game 1",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    val event2 =
        Event(
            eventId = "event2",
            type = EventType.SPORTS,
            title = "Basketball Game 2",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Serie starts in 1 hour with two 60-minute events
    val serieStartDate = calendar.time
    val serieEndCalendar = calendar.clone() as Calendar
    serieEndCalendar.add(Calendar.MINUTE, 120) // Two 60-min events = 120 min total

    val serie =
        Serie(
            serieId = "serie1",
            title = "Weekly Basketball",
            description = "Weekly basketball games",
            date = Timestamp(serieStartDate),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1",
            lastEventEndTime = Timestamp(serieEndCalendar.time))

    runBlocking {
      eventRepo.addEvent(event1)
      eventRepo.addEvent(event2)
      serieRepo.addSerie(serie)
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Serie should be displayed
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.getTestTagForSerie(serie)).assertExists()
    composeTestRule.onNodeWithText("Weekly Basketball").assertExists()
    // Serie badge should be displayed
    composeTestRule.onNodeWithText("Serie 🔥").assertExists()
  }

  @Test
  fun overviewScreen_displaysBothEventsAndSeries() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 1)

    // Create standalone event with future timestamp
    val standaloneEvent =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Single Event",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    // Create event that belongs to the serie
    val serieEvent =
        Event(
            eventId = "event2",
            type = EventType.SOCIAL,
            title = "Serie Event",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    // Serie starts in 1 hour with one 60-minute event
    val serieStartDate2 = calendar.time
    val serieEndCalendar2 = calendar.clone() as Calendar
    serieEndCalendar2.add(Calendar.MINUTE, 60) // One 60-min event

    val serie =
        Serie(
            serieId = "serie1",
            title = "Event Serie",
            description = "Serie description",
            date = Timestamp(serieStartDate2),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event2"),
            ownerId = "owner1",
            lastEventEndTime = Timestamp(serieEndCalendar2.time))

    runBlocking {
      eventRepo.addEvent(standaloneEvent)
      eventRepo.addEvent(serieEvent)
      serieRepo.addSerie(serie)
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Both standalone event and serie should be displayed
    composeTestRule.onNodeWithText("Single Event").assertExists()
    composeTestRule.onNodeWithText("Event Serie").assertExists()
  }

  @Test
  fun overviewScreen_filtersOutEventsInSeries() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 1)

    // Create events
    val standaloneEvent = createEvent("standalone", "Standalone Event", EventType.SPORTS)
    val serieEvent = createEvent("serie_event", "Serie Event", EventType.SOCIAL)

    // Create serie that contains serie_event (starts in 1 hour, 60-min event)
    val serieStartDate3 = calendar.time
    val serieEndCalendar3 = calendar.clone() as Calendar
    serieEndCalendar3.add(Calendar.MINUTE, 60)

    val serie =
        Serie(
            serieId = "serie1",
            title = "My Serie",
            description = "Serie description",
            date = Timestamp(serieStartDate3),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("serie_event"),
            ownerId = "owner1",
            lastEventEndTime = Timestamp(serieEndCalendar3.time))

    runBlocking {
      eventRepo.addEvent(standaloneEvent)
      eventRepo.addEvent(serieEvent)
      serieRepo.addSerie(serie)
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Standalone event should be displayed
    composeTestRule.onNodeWithText("Standalone Event").assertExists()
    // Serie should be displayed (may appear as multiple nodes in the semantic tree but that's OK)
    // Just verify at least one serie card with the correct tag exists
    composeTestRule
        .onAllNodesWithTag(OverviewScreenTestTags.getTestTagForSerie(serie))[0]
        .assertExists()
    // Verify the serie card contains the title
    composeTestRule
        .onAllNodesWithTag(OverviewScreenTestTags.getTestTagForSerie(serie))[0]
        .assertTextContains("My Serie")
    // Serie event should NOT be displayed as standalone
    composeTestRule.onNodeWithText("Serie Event").assertDoesNotExist()
  }

  @Test
  fun overviewScreen_displaysMultipleSeries() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 1)

    runBlocking {
      for (i in 1..3) {
        // Create event for each serie
        eventRepo.addEvent(
            Event(
                eventId = "event$i",
                type = EventType.SPORTS,
                title = "Event $i",
                description = "desc",
                location = null,
                date = Timestamp(calendar.time),
                duration = 60,
                participants = listOf("user1"),
                maxParticipants = 10,
                visibility = EventVisibility.PUBLIC,
                ownerId = "owner1"))

        // Create serie (starts in 1 hour, 60-min event)
        val serieStartTime = calendar.time
        val serieEndTime = (calendar.clone() as Calendar).apply { add(Calendar.MINUTE, 60) }.time

        serieRepo.addSerie(
            Serie(
                serieId = "serie$i",
                title = "Serie $i",
                description = "Description $i",
                date = Timestamp(serieStartTime),
                participants = listOf("user1"),
                maxParticipants = 10,
                visibility = Visibility.PUBLIC,
                eventIds = listOf("event$i"),
                ownerId = "owner1",
                lastEventEndTime = Timestamp(serieEndTime)))
      }
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // All series should be displayed
    composeTestRule.onNodeWithText("Serie 1").assertExists()
    composeTestRule.onNodeWithText("Serie 2").assertExists()
    composeTestRule.onNodeWithText("Serie 3").assertExists()
  }

  @Test
  fun overviewScreen_displaysOngoingSerie() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MINUTE, -30) // Started 30 minutes ago

    // Create an ongoing event that belongs to the serie
    val ongoingEvent =
        Event(
            eventId = "ongoing_serie_event",
            type = EventType.SPORTS,
            title = "Ongoing Serie Event",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 120, // 2 hours, still ongoing
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Calculate serie end time (started 30 min ago + 120 min duration = ends in 90 min)
    val serieStartTime = calendar.time
    calendar.add(Calendar.MINUTE, 120) // Add event duration
    val serieEndTime = Timestamp(calendar.time)

    val ongoingSerie =
        Serie(
            serieId = "ongoing_serie",
            title = "Ongoing Serie",
            description = "Ongoing serie description",
            date = Timestamp(serieStartTime),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("ongoing_serie_event"),
            ownerId = "owner1",
            lastEventEndTime = serieEndTime) // Set to future so serie is active

    runBlocking {
      eventRepo.addEvent(ongoingEvent)
      serieRepo.addSerie(ongoingSerie)
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Serie should appear in ongoing section
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.ONGOING_EVENTS_TITLE).assertExists()
    composeTestRule.onNodeWithText("Ongoing Serie").assertExists()
  }

  @Test
  fun overviewScreen_titleUsesCorrectPluralityWithMixedItems() {
    val eventRepo = EventsRepositoryLocal()
    val serieRepo = SeriesRepositoryLocal()
    val viewModel = OverviewViewModel(eventRepo, serieRepo)

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 1)

    // Create standalone event with future timestamp to ensure it's upcoming
    val standaloneEvent =
        Event(
            eventId = "standalone1",
            type = EventType.SPORTS,
            title = "Single Event",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    // Create event that belongs to the serie
    val serieEvent =
        Event(
            eventId = "serie_event1",
            type = EventType.SOCIAL,
            title = "Serie Event",
            description = "desc",
            location = null,
            date = Timestamp(calendar.time),
            duration = 60,
            participants = emptyList(),
            maxParticipants = 5,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner")

    // Serie starts in 1 hour with one 60-minute event
    val serieStartDate5 = calendar.time
    val serieEndCalendar5 = calendar.clone() as Calendar
    serieEndCalendar5.add(Calendar.MINUTE, 60)

    val serie =
        Serie(
            serieId = "serie1",
            title = "Event Serie",
            description = "Serie description",
            date = Timestamp(serieStartDate5),
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("serie_event1"),
            ownerId = "owner1",
            lastEventEndTime = Timestamp(serieEndCalendar5.time))

    runBlocking {
      eventRepo.addEvent(standaloneEvent)
      eventRepo.addEvent(serieEvent)
      serieRepo.addSerie(serie)
    }

    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = viewModel, enableNotificationPermissionRequest = false)
    }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Should display plural form (2 items total: 1 standalone event + 1 serie)
    composeTestRule.onNodeWithText("Your upcoming activities :").assertExists()
  }
}
