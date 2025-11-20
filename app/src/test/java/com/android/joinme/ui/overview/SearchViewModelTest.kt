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

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    FilterRepository.reset()

    fakeEventRepository = FakeEventRepository()
    fakeSeriesRepository = FakeSeriesRepository()

    // Create FilteredEventsRepository with fake repositories
    filteredEventsRepository = FilteredEventsRepository(fakeEventRepository, fakeSeriesRepository)
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
    assertFalse(uiState.categoryExpanded)
    assertEquals(4, filterState.sportCategories.size)
    assertEquals(0, filterState.selectedSportsCount)
    assertFalse(filterState.isSelectAllChecked)
  }

  @Test
  fun `setQuery updates query state`() = runTest {
    val query = "basketball game"
    viewModel.setQuery(query)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(query, viewModel.uiState.value.query)
  }

  @Test
  fun `toggleSocial updates social selection`() = runTest {
    viewModel.toggleSocial()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.filterState.value.isSocialSelected)
  }

  @Test
  fun `toggleSocial twice returns to initial state`() = runTest {
    viewModel.toggleSocial()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleSocial()
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.filterState.value.isSocialSelected)
  }

  @Test
  fun `toggleActivity updates activity selection`() = runTest {
    viewModel.toggleActivity()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.filterState.value.isActivitySelected)
  }

  @Test
  fun `toggleActivity twice returns to initial state`() = runTest {
    viewModel.toggleActivity()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleActivity()
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.filterState.value.isActivitySelected)
  }

  @Test
  fun `setCategoryExpanded updates expanded state`() = runTest {
    viewModel.setCategoryExpanded(true)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.categoryExpanded)

    viewModel.setCategoryExpanded(false)
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.categoryExpanded)
  }

  @Test
  fun `toggleSelectAll selects all sports when initially unselected`() = runTest {
    viewModel.toggleSelectAll()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.filterState.value
    assertTrue(state.isSelectAllChecked)
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.sportCategories.all { it.isChecked })
  }

  @Test
  fun `toggleSelectAll twice returns sports to initial state`() = runTest {
    viewModel.toggleSelectAll()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleSelectAll()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.filterState.value
    assertFalse(state.isSelectAllChecked)
    assertEquals(0, state.selectedSportsCount)
    assertTrue(state.sportCategories.none { it.isChecked })
  }

  @Test
  fun `toggleSport toggles specific sport by id`() = runTest {
    viewModel.toggleSport("basket")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.filterState.value
    val basketSport = state.sportCategories.find { it.id == "basket" }
    assertNotNull(basketSport)
    assertTrue(basketSport!!.isChecked)
    assertEquals(1, state.selectedSportsCount)
  }

  @Test
  fun `toggleSport twice returns sport to initial unchecked state`() = runTest {
    viewModel.toggleSport("football")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleSport("football")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.filterState.value
    val footballSport = state.sportCategories.find { it.id == "football" }
    assertFalse(footballSport!!.isChecked)
    assertEquals(0, state.selectedSportsCount)
  }

  @Test
  fun `toggleSport with invalid id does nothing`() = runTest {
    viewModel.toggleSport("invalid_sport")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.filterState.value
    assertEquals(0, state.selectedSportsCount)
  }

  @Test
  fun `sportCategories contains correct initial sports`() = runTest {
    val state = viewModel.filterState.value
    val sportIds = state.sportCategories.map { it.id }

    assertTrue(sportIds.contains("basket"))
    assertTrue(sportIds.contains("football"))
    assertTrue(sportIds.contains("tennis"))
    assertTrue(sportIds.contains("running"))
  }

  @Test
  fun `sportCategories contains correct sport names`() = runTest {
    val state = viewModel.filterState.value
    val sportNames = state.sportCategories.map { it.name }

    assertTrue(sportNames.contains("Basket"))
    assertTrue(sportNames.contains("Football"))
    assertTrue(sportNames.contains("Tennis"))
    assertTrue(sportNames.contains("Running"))
  }

  @Test
  fun `selectedSportsCount is computed correctly`() = runTest {
    // Start with all sports selected (4)
    // Toggle off basket and tennis
    viewModel.toggleSport("basket")
    viewModel.toggleSport("tennis")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(2, viewModel.filterState.value.selectedSportsCount)
  }

  @Test
  fun `isSelectAllChecked becomes true when all sports checked`() = runTest {
    viewModel.toggleSelectAll()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.filterState.value.isSelectAllChecked)

    viewModel.toggleSport("basket")
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.filterState.value.isSelectAllChecked)
  }

  @Test
  fun `complex scenario - mixed selections`() = runTest {
    // Select activity and some sports
    viewModel.toggleActivity()
    viewModel.toggleSport("basket")
    viewModel.toggleSport("football")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.filterState.value
    assertFalse(state.isSocialSelected)
    assertTrue(state.isActivitySelected)
    assertEquals(2, state.selectedSportsCount)
    assertFalse(state.isSelectAllChecked)
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
            date = com.google.firebase.Timestamp.now(),
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
            date = com.google.firebase.Timestamp.now(),
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
            date = com.google.firebase.Timestamp.now(),
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
            date = com.google.firebase.Timestamp.now(),
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
            date = com.google.firebase.Timestamp.now(),
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
            description = "Test event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp.now(),
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
            date = com.google.firebase.Timestamp.now(),
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
            date = com.google.firebase.Timestamp.now(),
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
  }

  private class FakeSeriesRepository : com.android.joinme.model.serie.SeriesRepository {
    var seriesToReturn: List<com.android.joinme.model.serie.Serie> = emptyList()

    override fun getNewSerieId(): String = "fake-serie-id"

    override suspend fun getAllSeries(
        serieFilter: SerieFilter
    ): List<com.android.joinme.model.serie.Serie> = seriesToReturn

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
