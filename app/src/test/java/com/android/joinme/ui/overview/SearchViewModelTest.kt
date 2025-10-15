package com.android.joinme.ui.overview

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
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    val fakeRepository =
        object : com.android.joinme.model.event.EventsRepository {
          override fun getNewEventId(): String = "fake-id"

          override suspend fun getAllEvents(): List<com.android.joinme.model.event.Event> =
              emptyList()

          override suspend fun getEvent(eventId: String): com.android.joinme.model.event.Event {
            throw Exception("Not implemented in fake repo")
          }

          override suspend fun addEvent(event: com.android.joinme.model.event.Event) {}

          override suspend fun editEvent(
              eventId: String,
              newValue: com.android.joinme.model.event.Event
          ) {}

          override suspend fun deleteEvent(eventId: String) {}
        }

    viewModel = SearchViewModel(fakeRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is correct`() = runTest {
    val state = viewModel.uiState.value

    assertEquals("", state.query)
    assertTrue(state.isAllSelected)
    assertTrue(state.isSocialSelected)
    assertTrue(state.isActivitySelected)
    assertFalse(state.categoryExpanded)
    assertEquals(4, state.sportCategories.size)
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.isSelectAllChecked)
  }

  @Test
  fun `setQuery updates query state`() = runTest {
    val query = "basketball game"
    viewModel.setQuery(query)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(query, viewModel.uiState.value.query)
  }

  @Test
  fun `toggleAll deselects all filters when initially selected`() = runTest {
    viewModel.toggleAll()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isAllSelected)
    assertFalse(state.isSocialSelected)
    assertFalse(state.isActivitySelected)
    assertFalse(state.isSelectAllChecked)
    assertEquals(0, state.selectedSportsCount)
    assertTrue(state.sportCategories.none { it.isChecked })
  }

  @Test
  fun `toggleAll twice returns to initial state`() = runTest {
    viewModel.toggleAll()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleAll()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isAllSelected)
    assertTrue(state.isSocialSelected)
    assertTrue(state.isActivitySelected)
    assertTrue(state.isSelectAllChecked)
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.sportCategories.all { it.isChecked })
  }

  @Test
  fun `toggleSocial updates social selection`() = runTest {
    viewModel.toggleSocial()
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isSocialSelected)
    assertFalse(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleSocial twice returns to initial state`() = runTest {
    viewModel.toggleSocial()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleSocial()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isSocialSelected)
  }

  @Test
  fun `toggleActivity updates activity selection`() = runTest {
    viewModel.toggleActivity()
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isActivitySelected)
    assertFalse(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleActivity twice returns to initial state`() = runTest {
    viewModel.toggleActivity()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleActivity()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isActivitySelected)
  }

  @Test
  fun `toggleAll is already selected when social, activity and all sports are selected bydefault`() =
      runTest {
        assertTrue(viewModel.uiState.value.isAllSelected)
      }

  @Test
  fun `toggleAll deselects when social is deselected`() = runTest {
    viewModel.toggleSocial()
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleAll deselects when activity is deselected`() = runTest {
    viewModel.toggleActivity()
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isAllSelected)
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
  fun `toggleSelectAll deselects all sports when initially selected`() = runTest {
    viewModel.toggleSelectAll()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isSelectAllChecked)
    assertEquals(0, state.selectedSportsCount)
    assertTrue(state.sportCategories.none { it.isChecked })
  }

  @Test
  fun `toggleSelectAll twice returns sports to initial state`() = runTest {
    viewModel.toggleSelectAll()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleSelectAll()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isSelectAllChecked)
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.sportCategories.all { it.isChecked })
  }

  @Test
  fun `toggleSelectAll maintains isAllSelected when social and activity are selected`() = runTest {
    // All filters are already selected by default
    assertTrue(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleSport toggles specific sport by id`() = runTest {
    viewModel.toggleSport("basket")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    val basketSport = state.sportCategories.find { it.id == "basket" }
    assertNotNull(basketSport)
    assertFalse(basketSport!!.isChecked)
    assertEquals(3, state.selectedSportsCount)
  }

  @Test
  fun `toggleSport twice returns sport to initial checked state`() = runTest {
    viewModel.toggleSport("football")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleSport("football")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    val footballSport = state.sportCategories.find { it.id == "football" }
    assertTrue(footballSport!!.isChecked)
    assertEquals(4, state.selectedSportsCount)
  }

  @Test
  fun `isAllSelected remains true when all filters are selected`() = runTest {
    // All filters are already selected by default
    assertTrue(viewModel.uiState.value.isAllSelected)
    assertTrue(viewModel.uiState.value.isSocialSelected)
    assertTrue(viewModel.uiState.value.isActivitySelected)
    assertTrue(viewModel.uiState.value.isSelectAllChecked)
  }

  @Test
  fun `toggleSport deselects isAllSelected when sport is unchecked`() = runTest {
    // All filters are already selected by default
    assertTrue(viewModel.uiState.value.isAllSelected)

    viewModel.toggleSport("basket")
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleSport with invalid id does nothing`() = runTest {
    viewModel.toggleSport("invalid_sport")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(4, state.selectedSportsCount)
  }

  @Test
  fun `sportCategories contains correct initial sports`() = runTest {
    val state = viewModel.uiState.value
    val sportIds = state.sportCategories.map { it.id }

    assertTrue(sportIds.contains("basket"))
    assertTrue(sportIds.contains("football"))
    assertTrue(sportIds.contains("tennis"))
    assertTrue(sportIds.contains("running"))
  }

  @Test
  fun `sportCategories contains correct sport names`() = runTest {
    val state = viewModel.uiState.value
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

    assertEquals(2, viewModel.uiState.value.selectedSportsCount)
  }

  @Test
  fun `isSelectAllChecked is false when not all sports checked`() = runTest {
    viewModel.toggleSport("basket")
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isSelectAllChecked)

    viewModel.toggleSport("basket")
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isSelectAllChecked)
  }

  @Test
  fun `complex scenario - mixed selections`() = runTest {
    // Deselect activity and some sports
    viewModel.toggleActivity()
    viewModel.toggleSport("basket")
    viewModel.toggleSport("football")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isSocialSelected)
    assertFalse(state.isActivitySelected)
    assertEquals(2, state.selectedSportsCount)
    assertFalse(state.isAllSelected)
    assertFalse(state.isSelectAllChecked)
  }

  @Test
  fun `setEvents updates events list and applies filters`() = runTest {
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

    viewModel.setEvents(listOf(sampleEvent))
    testDispatcher.scheduler.advanceUntilIdle()

    // Since all filters are selected by default, event should be visible
    assertEquals(1, viewModel.uiState.value.events.size)
    assertEquals("Basketball", viewModel.uiState.value.events[0].title)
  }

  @Test
  fun `setEvents with empty list clears events`() = runTest {
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

    viewModel.setEvents(listOf(sampleEvent))
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.events.size)

    viewModel.setEvents(emptyList())
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(0, viewModel.uiState.value.events.size)
  }

  @Test
  fun `refreshUIState filters out past events`() = runTest {
    val now = System.currentTimeMillis()
    val pastEvent =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Past Basketball Game",
            description = "This event already happened",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp((now / 1000) - 7200, 0), // 2 hours ago
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    val upcomingEvent =
        com.android.joinme.model.event.Event(
            eventId = "2",
            type = com.android.joinme.model.event.EventType.SOCIAL,
            title = "Future Party",
            description = "This event is upcoming",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp((now / 1000) + 3600, 0), // 1 hour from now
            duration = 120,
            participants = emptyList(),
            maxParticipants = 20,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner2")

    val fakeRepository =
        object : com.android.joinme.model.event.EventsRepository {
          override fun getNewEventId(): String = "fake-id"

          override suspend fun getAllEvents(): List<com.android.joinme.model.event.Event> =
              listOf(pastEvent, upcomingEvent)

          override suspend fun getEvent(eventId: String): com.android.joinme.model.event.Event {
            throw Exception("Not implemented in fake repo")
          }

          override suspend fun addEvent(event: com.android.joinme.model.event.Event) {}

          override suspend fun editEvent(
              eventId: String,
              newValue: com.android.joinme.model.event.Event
          ) {}

          override suspend fun deleteEvent(eventId: String) {}
        }

    val testViewModel = SearchViewModel(fakeRepository)
    testViewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    // Only upcoming event should be present
    val events = testViewModel.uiState.value.events
    assertEquals(1, events.size)
    assertEquals("Future Party", events[0].title)
    assertEquals("2", events[0].eventId)
  }

  @Test
  fun `refreshUIState shows only upcoming events when all are past`() = runTest {
    val now = System.currentTimeMillis()
    val pastEvent1 =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Old Event 1",
            description = "Past event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp((now / 1000) - 86400, 0), // 1 day ago
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    val pastEvent2 =
        com.android.joinme.model.event.Event(
            eventId = "2",
            type = com.android.joinme.model.event.EventType.ACTIVITY,
            title = "Old Event 2",
            description = "Another past event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp((now / 1000) - 3600, 0), // 1 hour ago
            duration = 90,
            participants = emptyList(),
            maxParticipants = 15,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner2")

    val fakeRepository =
        object : com.android.joinme.model.event.EventsRepository {
          override fun getNewEventId(): String = "fake-id"

          override suspend fun getAllEvents(): List<com.android.joinme.model.event.Event> =
              listOf(pastEvent1, pastEvent2)

          override suspend fun getEvent(eventId: String): com.android.joinme.model.event.Event {
            throw Exception("Not implemented in fake repo")
          }

          override suspend fun addEvent(event: com.android.joinme.model.event.Event) {}

          override suspend fun editEvent(
              eventId: String,
              newValue: com.android.joinme.model.event.Event
          ) {}

          override suspend fun deleteEvent(eventId: String) {}
        }

    val testViewModel = SearchViewModel(fakeRepository)
    testViewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    // No events should be present
    assertEquals(0, testViewModel.uiState.value.events.size)
  }

  @Test
  fun `refreshUIState includes upcoming events`() = runTest {
    val now = System.currentTimeMillis()
    val upcomingEvent1 =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Tomorrow's Game",
            description = "Future event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp((now / 1000) + 86400, 0), // 1 day from now
            duration = 60,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    val upcomingEvent2 =
        com.android.joinme.model.event.Event(
            eventId = "2",
            type = com.android.joinme.model.event.EventType.SOCIAL,
            title = "Next Week Party",
            description = "Another future event",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp((now / 1000) + 604800, 0), // 1 week from now
            duration = 120,
            participants = emptyList(),
            maxParticipants = 20,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner2")

    val fakeRepository =
        object : com.android.joinme.model.event.EventsRepository {
          override fun getNewEventId(): String = "fake-id"

          override suspend fun getAllEvents(): List<com.android.joinme.model.event.Event> =
              listOf(upcomingEvent1, upcomingEvent2)

          override suspend fun getEvent(eventId: String): com.android.joinme.model.event.Event {
            throw Exception("Not implemented in fake repo")
          }

          override suspend fun addEvent(event: com.android.joinme.model.event.Event) {}

          override suspend fun editEvent(
              eventId: String,
              newValue: com.android.joinme.model.event.Event
          ) {}

          override suspend fun deleteEvent(eventId: String) {}
        }

    val testViewModel = SearchViewModel(fakeRepository)
    testViewModel.refreshUIState()
    testDispatcher.scheduler.advanceUntilIdle()

    // Both upcoming events should be present
    val events = testViewModel.uiState.value.events
    assertEquals(2, events.size)
    assertTrue(events.any { it.title == "Tomorrow's Game" })
    assertTrue(events.any { it.title == "Next Week Party" })
  }
}
