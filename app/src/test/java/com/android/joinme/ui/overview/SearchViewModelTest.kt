package com.android.joinme.ui.overview

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.isUpcoming
import com.android.joinme.model.filter.FilterRepository
import com.android.joinme.model.filter.FilteredEventsRepository
import com.android.joinme.model.serie.SerieFilter
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

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

  private lateinit var viewModel: SearchViewModel
  private lateinit var filteredEventsRepository: FilteredEventsRepository
  private lateinit var fakeEventRepository: FakeEventRepository
  private lateinit var fakeSeriesRepository: FakeSeriesRepository
  private val testDispatcher = StandardTestDispatcher()

  // Future timestamp for test events (1 day in the future)
  private val futureTimestamp =
      com.google.firebase.Timestamp(System.currentTimeMillis() / 1000 + 86400, 0)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    FilterRepository.reset()

    fakeEventRepository = FakeEventRepository()
    fakeSeriesRepository = FakeSeriesRepository()

    // Create FilteredEventsRepository with fake repositories and test dispatcher
    filteredEventsRepository =
        FilteredEventsRepository(
            fakeEventRepository, fakeSeriesRepository, FilterRepository, testDispatcher)
    FilteredEventsRepository.resetInstance(filteredEventsRepository)

    viewModel = SearchViewModel(filteredEventsRepository)
    testDispatcher.scheduler.advanceUntilIdle()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    FilteredEventsRepository.resetInstance()
  }

  @Test
  fun `initial state is correct`() = runTest {
    val uiState = viewModel.uiState.value
    val filterState = viewModel.filterState.value

    assertEquals("", uiState.query)
    assertFalse(filterState.isSocialSelected)
    assertFalse(filterState.isActivitySelected)
    assertFalse(filterState.isSportSelected)
    assertTrue(uiState.eventItems.isEmpty())
  }

  @Test
  fun `setQuery updates query in UI state`() = runTest {
    viewModel.setQuery("basketball")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("basketball", viewModel.uiState.value.query)
  }

  @Test
  fun `toggleSocial updates social filter state`() = runTest {
    viewModel.toggleSocial()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.filterState.value.isSocialSelected)
  }

  @Test
  fun `toggleActivity updates activity filter state`() = runTest {
    viewModel.toggleActivity()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.filterState.value.isActivitySelected)
  }

  @Test
  fun `toggleSport updates sport filter state`() = runTest {
    viewModel.toggleSport()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.filterState.value.isSportSelected)
  }

  @Test
  fun `clearErrorMsg clears error message`() = runTest {
    viewModel.clearErrorMsg()
    testDispatcher.scheduler.advanceUntilIdle()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `setQuery filters events by title`() = runTest {
    val event1 =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball Game",
            description = "Test event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    val event2 =
        com.android.joinme.model.event.Event(
            eventId = "2",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Soccer Match",
            description = "Test event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    fakeEventRepository.eventsToReturn = listOf(event1, event2)
    filteredEventsRepository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(2, viewModel.uiState.value.eventItems.size)

    viewModel.setQuery("Basketball")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, viewModel.uiState.value.eventItems.size)
    assertEquals("Basketball Game", viewModel.uiState.value.eventItems[0].title)
  }

  @Test
  fun `setQuery filters events by description`() = runTest {
    val event1 =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Event 1",
            description = "Fun basketball event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    val event2 =
        com.android.joinme.model.event.Event(
            eventId = "2",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Event 2",
            description = "Test event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    fakeEventRepository.eventsToReturn = listOf(event1, event2)
    filteredEventsRepository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.setQuery("basketball")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, viewModel.uiState.value.eventItems.size)
    assertEquals("Event 1", viewModel.uiState.value.eventItems[0].title)
  }

  @Test
  fun `setEvents updates eventItems list and applies filters`() = runTest {
    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball",
            description = "Test event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    fakeEventRepository.eventsToReturn = listOf(sampleEvent)
    filteredEventsRepository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Since no filters are selected by default, all events should be visible
    assertEquals(1, viewModel.uiState.value.eventItems.size)
    val item = viewModel.uiState.value.eventItems[0]
    assertTrue(item is com.android.joinme.model.eventItem.EventItem.SingleEvent)
    assertEquals("Basketball", item.title)
  }

  @Test
  fun `setEvents with empty list clears eventItems`() = runTest {
    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball",
            description = "Test event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    fakeEventRepository.eventsToReturn = listOf(sampleEvent)
    filteredEventsRepository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.eventItems.size)

    fakeEventRepository.eventsToReturn = emptyList()
    filteredEventsRepository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(0, viewModel.uiState.value.eventItems.size)
  }

  @Test
  fun `setSeries updates eventItems list with series`() = runTest {
    val sampleSerie =
        com.android.joinme.model.serie.Serie(
            serieId = "serie1",
            title = "Weekly Basketball",
            description = "Weekly basketball event",
            date = futureTimestamp,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.utils.Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    fakeSeriesRepository.seriesToReturn = listOf(sampleSerie)
    filteredEventsRepository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Series should be visible
    assertEquals(1, viewModel.uiState.value.eventItems.size)
    val item = viewModel.uiState.value.eventItems[0]
    assertTrue(item is com.android.joinme.model.eventItem.EventItem.EventSerie)
    assertEquals("Weekly Basketball", item.title)
  }

  @Test
  fun `eventItems contains both events and series`() = runTest {
    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball Game",
            description = "Test event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    val sampleSerie =
        com.android.joinme.model.serie.Serie(
            serieId = "serie1",
            title = "Weekly Basketball",
            description = "Weekly basketball event",
            date = futureTimestamp,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.utils.Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    fakeEventRepository.eventsToReturn = listOf(sampleEvent)
    fakeSeriesRepository.seriesToReturn = listOf(sampleSerie)
    filteredEventsRepository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Both event and serie should be visible
    assertEquals(2, viewModel.uiState.value.eventItems.size)
    assertTrue(
        viewModel.uiState.value.eventItems.any {
          it is com.android.joinme.model.eventItem.EventItem.SingleEvent
        })
    assertTrue(
        viewModel.uiState.value.eventItems.any {
          it is com.android.joinme.model.eventItem.EventItem.EventSerie
        })
  }

  @Test
  fun `search query filters both events and series`() = runTest {
    val event1 =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball Game",
            description = "Fun game",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    val event2 =
        com.android.joinme.model.event.Event(
            eventId = "2",
            type = com.android.joinme.model.event.EventType.SOCIAL,
            title = "Dinner Party",
            description = "Social event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = futureTimestamp,
            duration = 120,
            participants = emptyList(),
            maxParticipants = 20,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    val serie =
        com.android.joinme.model.serie.Serie(
            serieId = "serie1",
            title = "Weekly Basketball",
            description = "Weekly basketball series",
            date = futureTimestamp,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.utils.Visibility.PUBLIC,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner1")

    fakeEventRepository.eventsToReturn = listOf(event1, event2)
    fakeSeriesRepository.seriesToReturn = listOf(serie)
    filteredEventsRepository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Initially all items should be visible (3 items)
    assertEquals(3, viewModel.uiState.value.eventItems.size)

    // Apply search query for "Basketball"
    viewModel.setQuery("Basketball")
    testDispatcher.scheduler.advanceUntilIdle()

    // Only items containing "Basketball" should be visible (event1 and serie)
    assertEquals(2, viewModel.uiState.value.eventItems.size)
    assertTrue(
        viewModel.uiState.value.eventItems.all {
          it.title.contains("Basketball", ignoreCase = true)
        })
  }

  // Fake implementations for testing
  private class FakeEventRepository : com.android.joinme.model.event.EventsRepository {
    var eventsToReturn: List<com.android.joinme.model.event.Event> = emptyList()

    override fun getNewEventId(): String = "fake-id"

    override suspend fun getAllEvents(
        eventFilter: EventFilter
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

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> = emptyList()

    override suspend fun getCommonEvents(userIds: List<String>): List<Event> {
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
        serieFilter: SerieFilter
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
