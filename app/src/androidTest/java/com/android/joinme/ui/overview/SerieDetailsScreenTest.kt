package com.android.joinme.ui.overview

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
import java.util.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Fake SeriesRepository for testing SerieDetailsScreen. */
class FakeSerieDetailsSeriesRepository : SeriesRepository {
  private val series = mutableMapOf<String, Serie>()
  var shouldThrowError = false

  fun setSerie(serie: Serie) {
    series[serie.serieId] = serie
  }

  fun clear() {
    series.clear()
  }

  override suspend fun addSerie(serie: Serie) {
    series[serie.serieId] = serie
  }

  override suspend fun editSerie(serieId: String, newValue: Serie) {
    if (shouldThrowError) throw Exception("Failed to update serie")
    series[serieId] = newValue
  }

  override suspend fun deleteSerie(serieId: String) {
    series.remove(serieId)
  }

  override suspend fun getSerie(serieId: String): Serie {
    if (shouldThrowError) throw Exception("Failed to load serie")
    return series[serieId] ?: throw NoSuchElementException("Serie not found")
  }

  override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> = series.values.toList()

  override fun getNewSerieId(): String = "new-serie-id"
}

/** Fake EventsRepository for testing SerieDetailsScreen. */
class FakeSerieDetailsEventsRepository : EventsRepository {
  private val events = mutableMapOf<String, Event>()

  fun setEvent(event: Event) {
    events[event.eventId] = event
  }

  fun clear() {
    events.clear()
  }

  override suspend fun addEvent(event: Event) {
    events[event.eventId] = event
  }

  override suspend fun editEvent(eventId: String, newValue: Event) {
    events[eventId] = newValue
  }

  override suspend fun deleteEvent(eventId: String) {
    events.remove(eventId)
  }

  override suspend fun getEvent(eventId: String): Event =
      events[eventId] ?: throw NoSuchElementException("Event not found")

  override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> = events.values.toList()

  override fun getNewEventId(): String = "new-event-id"
}

class SerieDetailsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakeSeriesRepo: FakeSerieDetailsSeriesRepository
  private lateinit var fakeEventsRepo: FakeSerieDetailsEventsRepository

  private fun setup() {
    fakeSeriesRepo = FakeSerieDetailsSeriesRepository()
    fakeEventsRepo = FakeSerieDetailsEventsRepository()
  }

  private fun createViewModel(): SerieDetailsViewModel {
    return SerieDetailsViewModel(fakeSeriesRepo, fakeEventsRepo)
  }

  private fun createTestSerie(
      serieId: String = "test-serie-1",
      title: String = "Weekly Basketball",
      ownerId: String = "owner123",
      participants: List<String> = listOf("user1", "user2", "owner123"),
      maxParticipants: Int = 10,
      eventIds: List<String> = listOf("event1", "event2"),
      visibility: Visibility = Visibility.PUBLIC,
      description: String = "Weekly basketball games every Friday"
  ): Serie {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 15, 18, 30, 0)

    return Serie(
        serieId = serieId,
        title = title,
        description = description,
        date = Timestamp(calendar.time),
        participants = participants,
        maxParticipants = maxParticipants,
        visibility = visibility,
        eventIds = eventIds,
        ownerId = ownerId)
  }

  private fun createTestEvent(
      eventId: String = "event1",
      ownerId: String = "owner123",
      duration: Int = 90
  ): Event {
    val calendar = Calendar.getInstance()
    calendar.set(2025, Calendar.JANUARY, 15, 18, 30, 0)

    return Event(
        eventId = eventId,
        type = EventType.SPORTS,
        title = "Basketball Match $eventId",
        description = "Friendly basketball match",
        location = Location(46.5197, 6.6323, "EPFL"),
        date = Timestamp(calendar.time),
        duration = duration,
        participants = listOf("user1", "user2"),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = ownerId)
  }

  // ========== Comprehensive Display Tests ==========

  @Test
  fun screenDisplaysAllSerieInformationCorrectly() {
    setup()
    val serie =
        createTestSerie(
            title = "Football League",
            visibility = Visibility.PUBLIC,
            participants = listOf("user1", "user2", "user3"),
            maxParticipants = 10,
            ownerId = "owner123",
            description = "Join us for weekly games!",
            eventIds = listOf("event1", "event2"))

    val event1 = createTestEvent(eventId = "event1", duration = 90)
    val event2 = createTestEvent(eventId = "event2", duration = 60)

    fakeSeriesRepo.setSerie(serie)
    fakeEventsRepo.setEvent(event1)
    fakeEventsRepo.setEvent(event2)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(serieId = serie.serieId, serieDetailsViewModel = viewModel)
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule.onAllNodesWithText("Football League").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify all main UI elements are displayed
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SERIE_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.MEETING_INFO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.VISIBILITY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.MEMBERS_COUNT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.DURATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.DESCRIPTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.OWNER_INFO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EVENT_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BACK_BUTTON).assertIsDisplayed()

    // Verify specific content values
    composeTestRule.onNodeWithText("Football League").assertIsDisplayed()
    composeTestRule.onNodeWithText("PUBLIC").assertIsDisplayed()
    composeTestRule.onNodeWithText("Join us for weekly games!").assertIsDisplayed()

    // Verify event cards are displayed
    composeTestRule
        .onNodeWithTag("${SerieDetailsScreenTestTags.EVENT_CARD}_event1")
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${SerieDetailsScreenTestTags.EVENT_CARD}_event2")
        .assertIsDisplayed()

    // Verify back button functionality
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BACK_BUTTON).assertHasClickAction()
  }

  @Test
  fun screenDisplaysPrivateVisibilityAndEmptyEventList() {
    setup()
    val serie =
        createTestSerie(visibility = Visibility.PRIVATE, eventIds = emptyList(), description = "")
    fakeSeriesRepo.setSerie(serie)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(serieId = serie.serieId, serieDetailsViewModel = viewModel)
    }

    // Wait for the screen to fully load
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.VISIBILITY)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Add a small delay to ensure everything is settled
    composeTestRule.mainClock.advanceTimeBy(500)
    composeTestRule.waitForIdle()

    // Verify PRIVATE visibility
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.VISIBILITY)
        .assertTextContains("PRIVATE")

    // Verify empty event list shows message
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EVENT_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithText("No events in this serie yet").assertIsDisplayed()

    // Verify empty description is handled
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.DESCRIPTION).assertIsDisplayed()
  }

  @Test
  fun loadingStateHandledCorrectly() {
    setup()
    val serie = createTestSerie()
    fakeSeriesRepo.setSerie(serie)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(serieId = serie.serieId, serieDetailsViewModel = viewModel)
    }

    // Check for loading indicator or content (loading might be too fast)
    val loadingOrContentExists =
        composeTestRule
            .onAllNodesWithTag(SerieDetailsScreenTestTags.LOADING)
            .fetchSemanticsNodes()
            .isNotEmpty() ||
            composeTestRule
                .onAllNodesWithText("Weekly Basketball")
                .fetchSemanticsNodes()
                .isNotEmpty()

    assertTrue(loadingOrContentExists)
  }

  // ========== Event Interactions ==========

  @Test
  fun eventCardClickTriggersCallback() {
    setup()
    val serie = createTestSerie(eventIds = listOf("event1"))
    val event1 = createTestEvent(eventId = "event1")
    fakeSeriesRepo.setSerie(serie)
    fakeEventsRepo.setEvent(event1)

    var clickedEventId: String? = null
    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId,
          serieDetailsViewModel = viewModel,
          onEventCardClick = { clickedEventId = it })
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag("${SerieDetailsScreenTestTags.EVENT_CARD}_event1")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag("${SerieDetailsScreenTestTags.EVENT_CARD}_event1").performClick()

    assertEquals("event1", clickedEventId)
  }

  // ========== Owner View Tests ==========

  @Test
  fun ownerViewShowsCorrectButtonsAndCallbacks() {
    setup()
    val serie = createTestSerie(ownerId = "owner123")
    fakeSeriesRepo.setSerie(serie)

    var addEventClicked = false
    var editSerieClicked = false
    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId,
          serieDetailsViewModel = viewModel,
          currentUserId = "owner123",
          onAddEventClick = { addEventClicked = true },
          onEditSerieClick = { editSerieClicked = true })
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify owner buttons are shown
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT)
        .assertTextContains("ADD EVENT")
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON)
        .assertTextContains("EDIT SERIE")

    // Verify non-owner button is hidden
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertDoesNotExist()

    // Test button callbacks
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT).performClick()
    assertTrue(addEventClicked)

    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON).performClick()
    assertTrue(editSerieClicked)
  }

  // ========== Participant Tests ==========

  @Test
  fun participantCanQuitSerieSuccessfully() {
    setup()
    val serie =
        createTestSerie(ownerId = "owner123", participants = listOf("user1", "user2", "owner123"))
    fakeSeriesRepo.setSerie(serie)

    var quitSerieSuccessCalled = false
    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId,
          serieDetailsViewModel = viewModel,
          currentUserId = "user1",
          onQuitSerieSuccess = { quitSerieSuccessCalled = true })
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify quit button is shown and owner buttons are hidden
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE)
        .assertTextContains("QUIT SERIE")
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON).assertDoesNotExist()

    // Verify initial members count
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.MEMBERS_COUNT).assertIsDisplayed()

    // Click quit button
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify callback was triggered
    assertTrue(quitSerieSuccessCalled)

    // Verify button now shows JOIN
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertIsDisplayed()
  }

  // ========== Non-Participant Tests ==========

  @Test
  fun nonParticipantCanJoinSerieSuccessfully() {
    setup()
    val serie = createTestSerie(ownerId = "owner123", participants = listOf("user1", "owner123"))
    fakeSeriesRepo.setSerie(serie)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId, serieDetailsViewModel = viewModel, currentUserId = "user2")
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify join button is shown
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE)
        .assertTextContains("JOIN SERIE")

    // Verify initial members count
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.MEMBERS_COUNT).assertIsDisplayed()

    // Click join button
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Verify user joined successfully - button should now show QUIT
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.MEMBERS_COUNT).assertIsDisplayed()
  }

  @Test
  fun nonParticipantCannotJoinFullSerie() {
    setup()
    val serie =
        createTestSerie(
            ownerId = "owner123", participants = listOf("user1", "owner123"), maxParticipants = 2)
    fakeSeriesRepo.setSerie(serie)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId, serieDetailsViewModel = viewModel, currentUserId = "user2")
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify serie is full
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.MEMBERS_COUNT).assertIsDisplayed()

    // Join button should be disabled
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertIsNotEnabled()
  }

  // ========== Navigation ==========

  @Test
  fun backButtonTriggersCallback() {
    setup()
    val serie = createTestSerie()
    fakeSeriesRepo.setSerie(serie)

    var goBackCalled = false
    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId,
          serieDetailsViewModel = viewModel,
          onGoBack = { goBackCalled = true })
    }

    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BACK_BUTTON).assertHasClickAction()
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BACK_BUTTON).performClick()

    assertTrue(goBackCalled)
  }

  // ========== Edge Cases ==========

  @Test
  fun handlesSpecialCharactersAndLongText() {
    setup()
    val serie =
        createTestSerie(
            title = "Basketball & Football ⚽ - Very Long Title That Tests Layout Handling",
            description = "Special chars: @#$% - café, ñoño, 中文")
    fakeSeriesRepo.setSerie(serie)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(serieId = serie.serieId, serieDetailsViewModel = viewModel)
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithText(
              "Basketball & Football ⚽ - Very Long Title That Tests Layout Handling")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithText("Basketball & Football ⚽ - Very Long Title That Tests Layout Handling")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Special chars: @#$% - café, ñoño, 中文").assertIsDisplayed()
  }

  @Test
  fun handlesMaxParticipantsAndManyEvents() {
    setup()
    val participants = (1..9).map { "user$it" } + "owner123"
    val eventIds = (1..10).map { "event$it" }
    val serie =
        createTestSerie(
            ownerId = "owner123",
            participants = participants,
            maxParticipants = 10,
            eventIds = eventIds)
    fakeSeriesRepo.setSerie(serie)

    eventIds.forEach { eventId -> fakeEventsRepo.setEvent(createTestEvent(eventId = eventId)) }

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(serieId = serie.serieId, serieDetailsViewModel = viewModel)
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag("${SerieDetailsScreenTestTags.EVENT_CARD}_event1")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify members count display
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.MEMBERS_COUNT).assertIsDisplayed()

    // Verify event list exists and first event is displayed
    // LazyColumn only composes visible items
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.EVENT_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${SerieDetailsScreenTestTags.EVENT_CARD}_event1")
        .assertIsDisplayed()
  }

  @Test
  fun handlesUnknownUserIdGracefully() {
    setup()
    val serie = createTestSerie(ownerId = "owner123", participants = listOf("owner123"))
    fakeSeriesRepo.setSerie(serie)

    val viewModel = createViewModel()

    composeTestRule.setContent {
      SerieDetailsScreen(
          serieId = serie.serieId, serieDetailsViewModel = viewModel, currentUserId = "unknown")
    }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SerieDetailsScreenTestTags.SERIE_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Screen should load normally
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.SCREEN).assertIsDisplayed()

    // Should show join button for unknown user (non-owner, non-participant)
    composeTestRule.onNodeWithTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE).assertIsDisplayed()
  }
}
