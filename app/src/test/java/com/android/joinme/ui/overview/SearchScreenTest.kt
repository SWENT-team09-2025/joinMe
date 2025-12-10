package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.isUpcoming
import com.android.joinme.model.filter.FilterRepository
import com.android.joinme.model.filter.FilteredEventsRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var filteredEventsRepository: FilteredEventsRepository
  private lateinit var fakeEventRepository: FakeEventRepository
  private lateinit var fakeSeriesRepository: FakeSeriesRepository

  // Future timestamp for test events (1 day in the future)
  private val futureTimestamp =
      com.google.firebase.Timestamp(System.currentTimeMillis() / 1000 + 86400, 0)

  @Before
  fun setup() {
    // Reset FilterRepository before each test to ensure clean state
    FilterRepository.reset()

    fakeEventRepository = FakeEventRepository()
    fakeSeriesRepository = FakeSeriesRepository()

    // Create FilteredEventsRepository with fake repositories for testing
    filteredEventsRepository =
        FilteredEventsRepository(
            fakeEventRepository,
            fakeSeriesRepository,
            FilterRepository,
            kotlinx.coroutines.Dispatchers.Unconfined)
    FilteredEventsRepository.resetInstance(filteredEventsRepository)
  }

  @After
  fun tearDown() {
    FilteredEventsRepository.resetInstance()
  }

  private fun setupScreen(viewModel: SearchViewModel = SearchViewModel(filteredEventsRepository)) {
    composeTestRule.setContent { SearchScreen(searchViewModel = viewModel) }
    composeTestRule.waitForIdle()
  }

  @Test
  fun searchScreen_Components_display() {
    setupScreen()

    composeTestRule.onNodeWithText("Search").assertIsDisplayed()
    composeTestRule.onNodeWithText("Search an event").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    composeTestRule.onNodeWithText("Social").assertIsDisplayed()
    composeTestRule.onNodeWithText("Activity").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sport").assertIsDisplayed()
  }

  @Test
  fun searchScreen_canEnterSearchQuery() {
    setupScreen()

    val searchQuery = "basketball"
    composeTestRule.onNodeWithText("Search an event").performTextInput(searchQuery)

    composeTestRule.onNodeWithText(searchQuery).assertIsDisplayed()
  }

  @Test
  fun searchScreen_socialFilterChipToggles() {
    setupScreen()

    val socialChip = composeTestRule.onNodeWithText("Social")

    socialChip.assertIsDisplayed()
    socialChip.performClick()
    socialChip.performClick()
    socialChip.assertIsDisplayed()
  }

  @Test
  fun searchScreen_activityFilterChipToggles() {
    setupScreen()

    val activityChip = composeTestRule.onNodeWithText("Activity")

    activityChip.assertIsDisplayed()
    activityChip.performClick()
    activityChip.performClick()
    activityChip.assertIsDisplayed()
  }

  @Test
  fun searchScreen_sportFilterChipToggles() {
    setupScreen()

    val sportChip = composeTestRule.onNodeWithText("Sport")

    sportChip.assertIsDisplayed()
    sportChip.performClick()
    sportChip.performClick()
    sportChip.assertIsDisplayed()
  }

  @Test
  fun searchScreen_multipleFiltersCanBeSelected() {
    setupScreen()

    // Select Activity filter
    composeTestRule.onNodeWithText("Activity").performClick()

    // Select Social filter
    composeTestRule.onNodeWithText("Social").performClick()

    // Both should still be displayed
    composeTestRule.onNodeWithText("Activity").assertIsDisplayed()
    composeTestRule.onNodeWithText("Social").assertIsDisplayed()
  }


  @Test
  fun searchScreen_searchFieldClearsText() {
    setupScreen()

    composeTestRule.onNodeWithText("Search an event").performTextInput("test query")

    // Verify text was entered
    composeTestRule.onNodeWithText("test query").assertExists()

    // Clear button should be visible
    composeTestRule.onNodeWithContentDescription("Clear").assertIsDisplayed()

    // Click clear button
    composeTestRule.onNodeWithContentDescription("Clear").performClick()

    composeTestRule.waitForIdle()

    // Text should be cleared
    composeTestRule.onNodeWithText("test query").assertDoesNotExist()
  }

  @Test
  fun searchScreen_longSearchQueryIsAccepted() {
    setupScreen()

    val longQuery = "a".repeat(100)
    composeTestRule.onNodeWithText("Search an event").performTextInput(longQuery)

    composeTestRule.onNodeWithText(longQuery).assertIsDisplayed()
  }

  @Test
  fun searchScreen_allFiltersDisplayedSimultaneously() {
    setupScreen()

    // All filter chips should be visible at the same time
    composeTestRule.onNodeWithText("Social").assertIsDisplayed()
    composeTestRule.onNodeWithText("Activity").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sport").assertIsDisplayed()
  }


  @Test
  fun searchScreen_viewModelIntegration_queryUpdates() {
    val viewModel = SearchViewModel(filteredEventsRepository)
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("Search an event").performTextInput("test")

    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.query == "test")
  }

  @Test
  fun searchScreen_viewModelIntegration_socialFilterUpdates() {
    val viewModel = SearchViewModel(filteredEventsRepository)
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("Social").performClick()

    composeTestRule.waitForIdle()

    assert(viewModel.filterState.value.isSocialSelected)
  }

  @Test
  fun searchScreen_viewModelIntegration_activityFilterUpdates() {
    val viewModel = SearchViewModel(filteredEventsRepository)
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("Activity").performClick()

    composeTestRule.waitForIdle()

    assert(viewModel.filterState.value.isActivitySelected)
  }

  @Test
  fun searchScreen_viewModelIntegration_sportFilterUpdates() {
    val viewModel = SearchViewModel(filteredEventsRepository)
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("Sport").performClick()

    composeTestRule.waitForIdle()

    assert(viewModel.filterState.value.isSportSelected)
  }

  @Test
  fun searchScreen_displaysEmptyMessage_whenNoEvents() {
    val viewModel = SearchViewModel(filteredEventsRepository)
    setupScreen(viewModel)

    // Ensure events are empty
    fakeEventRepository.eventsToReturn = emptyList()
    filteredEventsRepository.refresh()

    composeTestRule.waitForIdle()

    // Empty events should show message using SearchScreenTestTags
    composeTestRule.onNodeWithTag(SearchScreenTestTags.EMPTY_EVENT_LIST_MSG).assertIsDisplayed()
  }

  @Test
  fun searchScreen_searchIconClick_dismissesKeyboard() {
    setupScreen()

    // Enter text
    composeTestRule.onNodeWithText("Search an event").performTextInput("test")

    composeTestRule.waitForIdle()

    // Click search icon (which is an IconButton)
    composeTestRule.onAllNodesWithContentDescription("Search")[0].performClick()

    composeTestRule.waitForIdle()

    // Verify text is still there after clicking search
    composeTestRule.onNodeWithText("test").assertExists()
  }

  @Test
  fun searchScreen_enterKeyPress_dismissesKeyboard() {
    setupScreen()

    // Find and interact with the search field
    composeTestRule.onNodeWithText("Search an event").performTextInput("test")

    composeTestRule.waitForIdle()

    // Now find the field by the text that was entered and perform IME action
    composeTestRule.onNode(hasSetTextAction()).performImeAction()

    composeTestRule.waitForIdle()

    // Text should still be there
    composeTestRule.onNodeWithText("test").assertExists()
  }

  @Test
  fun searchScreen_searchIcon_notClickable_whenQueryEmpty() {
    setupScreen()

    // Search icon should exist
    composeTestRule.onAllNodesWithContentDescription("Search")[0].assertIsDisplayed()

    // But clicking when empty should not crash
    composeTestRule.onAllNodesWithContentDescription("Search")[0].performClick()

    composeTestRule.waitForIdle()
  }

  @Test
  fun searchScreen_clearButton_onlyAppearsWithText() {
    setupScreen()

    // Clear button should not exist initially
    composeTestRule.onNodeWithContentDescription("Clear").assertDoesNotExist()

    // Enter text
    composeTestRule.onNodeWithText("Search an event").performTextInput("test")

    composeTestRule.waitForIdle()

    // Now clear button should appear
    composeTestRule.onNodeWithContentDescription("Clear").assertIsDisplayed()

    // Clear the text
    composeTestRule.onNodeWithContentDescription("Clear").performClick()

    composeTestRule.waitForIdle()

    // Clear button should disappear again
    composeTestRule.onNodeWithContentDescription("Clear").assertDoesNotExist()
  }

  @Test
  fun searchScreen_filterChips_displayedInRow() {
    setupScreen()

    // All filter chips in same row
    composeTestRule.onNodeWithText("Social").assertIsDisplayed()
    composeTestRule.onNodeWithText("Activity").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sport").assertIsDisplayed()
  }

  @Test
  fun searchScreen_complexFilterScenario() {
    setupScreen()

    // Select multiple filters
    composeTestRule.onNodeWithText("Activity").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Social").performClick()
    composeTestRule.waitForIdle()

    // Select sport filter
    composeTestRule.onNodeWithText("Sport").performClick()
    composeTestRule.waitForIdle()

    // Activity should still be displayed
    composeTestRule.onNodeWithText("Activity").assertIsDisplayed()
  }

  @Test
  fun searchScreen_multipleSearchQueries() {
    setupScreen()

    // First query
    composeTestRule.onNodeWithText("Search an event").performTextInput("basketball")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("basketball").assertExists()

    // Clear
    composeTestRule.onNodeWithContentDescription("Clear").performClick()
    composeTestRule.waitForIdle()

    // Second query
    composeTestRule.onNodeWithText("Search an event").performTextInput("football")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("football").assertExists()
  }

  @Test
  fun searchScreen_displaysEventCards_whenEventsExist() {
    val viewModel = SearchViewModel(filteredEventsRepository)
    setupScreen(viewModel)

    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball Game",
            description = "Fun basketball game",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Set events after screen is setup
    fakeEventRepository.eventsToReturn = listOf(sampleEvent)
    filteredEventsRepository.refresh()

    composeTestRule.waitForIdle()

    // Event card should be displayed
    composeTestRule.onNodeWithText("Basketball Game").assertIsDisplayed()
    composeTestRule.onNodeWithText("Place : EPFL").assertIsDisplayed()
  }

  @Test
  fun searchScreen_eventCardClick_triggersCallback() {
    val viewModel = SearchViewModel(filteredEventsRepository)

    var eventClicked = false
    composeTestRule.setContent {
      SearchScreen(searchViewModel = viewModel, onSelectEvent = { eventClicked = true })
    }
    composeTestRule.waitForIdle()

    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball Game",
            description = "Fun basketball game",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Set events after screen is setup
    fakeEventRepository.eventsToReturn = listOf(sampleEvent)
    filteredEventsRepository.refresh()

    composeTestRule.waitForIdle()

    // Click on event card
    composeTestRule.onNodeWithText("Basketball Game").performClick()

    assert(eventClicked)
  }

  @Test
  fun searchScreen_eventList_displaysWithTestTag() {
    val viewModel = SearchViewModel(filteredEventsRepository)
    setupScreen(viewModel)

    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball Game",
            description = "Fun basketball game",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Set events
    fakeEventRepository.eventsToReturn = listOf(sampleEvent)
    filteredEventsRepository.refresh()

    composeTestRule.waitForIdle()

    // Event list should have proper test tag
    composeTestRule.onNodeWithTag(SearchScreenTestTags.EVENT_LIST).assertIsDisplayed()
  }

  @Test
  fun searchScreen_eventCard_hasCorrectTestTag() {
    val viewModel = SearchViewModel(filteredEventsRepository)
    setupScreen(viewModel)

    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "123",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Test Event",
            description = "Test description",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Set events
    fakeEventRepository.eventsToReturn = listOf(sampleEvent)
    filteredEventsRepository.refresh()

    composeTestRule.waitForIdle()

    // Event card should have correct test tag based on eventId
    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.getTestTagForEventItem(sampleEvent))
        .assertIsDisplayed()
  }

  @Test
  fun searchScreen_multipleEventCards_haveUniqueTestTags() {
    val viewModel = SearchViewModel(filteredEventsRepository)
    setupScreen(viewModel)

    val event1 =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Event 1",
            description = "Description 1",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    val event2 =
        com.android.joinme.model.event.Event(
            eventId = "2",
            type = com.android.joinme.model.event.EventType.SOCIAL,
            title = "Event 2",
            description = "Description 2",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 90,
            participants = listOf("user2"),
            maxParticipants = 15,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner2")

    // Set events
    fakeEventRepository.eventsToReturn = listOf(event1, event2)
    filteredEventsRepository.refresh()

    composeTestRule.waitForIdle()

    // Both event cards should have unique test tags
    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.getTestTagForEventItem(event1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.getTestTagForEventItem(event2))
        .assertIsDisplayed()
  }

  @Test
  fun searchScreen_searchTextField_hasCorrectTestTag() {
    setupScreen()

    // Search text field should have proper test tag
    composeTestRule.onNodeWithTag(SearchScreenTestTags.SEARCH_TEXT_FIELD).assertIsDisplayed()
  }


  // Fake implementations for testing
  private class FakeEventRepository : com.android.joinme.model.event.EventsRepository {
    var eventsToReturn: List<com.android.joinme.model.event.Event> = emptyList()

    override fun getNewEventId(): String = "fake-id"

    override suspend fun getAllEvents(
        eventFilter: com.android.joinme.model.event.EventFilter
    ): List<com.android.joinme.model.event.Event> = eventsToReturn.filter { it.isUpcoming() }

    override suspend fun getEvent(eventId: String): com.android.joinme.model.event.Event {
      throw Exception("Not implemented in fake repo")
    }

    override suspend fun addEvent(event: com.android.joinme.model.event.Event) {}

    override suspend fun editEvent(
        eventId: String,
        newValue: com.android.joinme.model.event.Event
    ) {}

    override suspend fun deleteEvent(eventId: String) {}

    override suspend fun getEventsByIds(
        eventIds: List<String>
    ): List<com.android.joinme.model.event.Event> = emptyList()

    override suspend fun getCommonEvents(
        userIds: List<String>
    ): List<com.android.joinme.model.event.Event> {
      if (userIds.isEmpty()) return emptyList()
      return eventsToReturn
          .filter { event -> userIds.all { userId -> event.participants.contains(userId) } }
          .sortedBy { it.date.toDate().time }
    }
  }

  private class FakeSeriesRepository : com.android.joinme.model.serie.SeriesRepository {
    var seriesToReturn: List<com.android.joinme.model.serie.Serie> = emptyList()

    override fun getNewSerieId(): String = "fake-serie-id"

    override suspend fun getAllSeries(
        serieFilter: com.android.joinme.model.serie.SerieFilter
    ): List<com.android.joinme.model.serie.Serie> = seriesToReturn

    override suspend fun getSeriesByIds(
        seriesIds: List<String>
    ): List<com.android.joinme.model.serie.Serie> = emptyList()

    override suspend fun getSerie(serieId: String): com.android.joinme.model.serie.Serie {
      throw Exception("Not implemented in fake repo")
    }

    override suspend fun addSerie(serie: com.android.joinme.model.serie.Serie) {}

    override suspend fun editSerie(
        serieId: String,
        newValue: com.android.joinme.model.serie.Serie
    ) {}

    override suspend fun deleteSerie(serieId: String) {}
  }
}
