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
    viewModel = SearchViewModel()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is correct`() = runTest {
    val state = viewModel.uiState.value

    assertEquals("", state.query)
    assertFalse(state.isAllSelected)
    assertFalse(state.isBarSelected)
    assertFalse(state.isClubSelected)
    assertFalse(state.categoryExpanded)
    assertEquals(4, state.sportCategories.size)
    assertEquals(0, state.selectedSportsCount)
    assertFalse(state.isSelectAllChecked)
  }

  @Test
  fun `setQuery updates query state`() = runTest {
    val query = "basketball game"
    viewModel.setQuery(query)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(query, viewModel.uiState.value.query)
  }

  @Test
  fun `toggleAll selects all filters`() = runTest {
    viewModel.toggleAll()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isAllSelected)
    assertTrue(state.isBarSelected)
    assertTrue(state.isClubSelected)
    assertTrue(state.isSelectAllChecked)
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.sportCategories.all { it.isChecked })
  }

  @Test
  fun `toggleAll twice returns to initial state`() = runTest {
    viewModel.toggleAll()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleAll()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isAllSelected)
    assertFalse(state.isBarSelected)
    assertFalse(state.isClubSelected)
    assertFalse(state.isSelectAllChecked)
    assertEquals(0, state.selectedSportsCount)
    assertTrue(state.sportCategories.none { it.isChecked })
  }

  @Test
  fun `toggleBar updates bar selection`() = runTest {
    viewModel.toggleBar()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isBarSelected)
    assertFalse(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleBar twice returns to initial state`() = runTest {
    viewModel.toggleBar()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleBar()
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isBarSelected)
  }

  @Test
  fun `toggleClub updates club selection`() = runTest {
    viewModel.toggleClub()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isClubSelected)
    assertFalse(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleClub twice returns to initial state`() = runTest {
    viewModel.toggleClub()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleClub()
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isClubSelected)
  }

  @Test
  fun `toggleAll is selected when bar, club and all sports are selected`() = runTest {
    viewModel.toggleBar()
    viewModel.toggleClub()
    viewModel.toggleSelectAll()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleAll deselects when bar is deselected`() = runTest {
    viewModel.toggleAll()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleBar()
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleAll deselects when club is deselected`() = runTest {
    viewModel.toggleAll()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleClub()
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
  fun `toggleSelectAll selects all sports`() = runTest {
    viewModel.toggleSelectAll()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
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

    val state = viewModel.uiState.value
    assertFalse(state.isSelectAllChecked)
    assertEquals(0, state.selectedSportsCount)
    assertTrue(state.sportCategories.none { it.isChecked })
  }

  @Test
  fun `toggleSelectAll updates isAllSelected when bar and club are selected`() = runTest {
    viewModel.toggleBar()
    viewModel.toggleClub()
    viewModel.toggleSelectAll()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleSport toggles specific sport by id`() = runTest {
    viewModel.toggleSport("basket")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    val basketSport = state.sportCategories.find { it.id == "basket" }
    assertNotNull(basketSport)
    assertTrue(basketSport!!.isChecked)
    assertEquals(1, state.selectedSportsCount)
  }

  @Test
  fun `toggleSport twice returns sport to unchecked`() = runTest {
    viewModel.toggleSport("football")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.toggleSport("football")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    val footballSport = state.sportCategories.find { it.id == "football" }
    assertFalse(footballSport!!.isChecked)
    assertEquals(0, state.selectedSportsCount)
  }

  @Test
  fun `toggleSport updates isAllSelected when all conditions met`() = runTest {
    viewModel.toggleBar()
    viewModel.toggleClub()
    testDispatcher.scheduler.advanceUntilIdle()

    // Toggle all sports individually
    viewModel.toggleSport("basket")
    viewModel.toggleSport("football")
    viewModel.toggleSport("tennis")
    viewModel.toggleSport("running")
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleSport deselects isAllSelected when sport is unchecked`() = runTest {
    viewModel.toggleAll()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleSport("basket")
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun `toggleSport with invalid id does nothing`() = runTest {
    viewModel.toggleSport("invalid_sport")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(0, state.selectedSportsCount)
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
    viewModel.toggleSport("basket")
    viewModel.toggleSport("tennis")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(2, viewModel.uiState.value.selectedSportsCount)
  }

  @Test
  fun `isSelectAllChecked is true only when all sports checked`() = runTest {
    viewModel.toggleSport("basket")
    viewModel.toggleSport("football")
    viewModel.toggleSport("tennis")
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isSelectAllChecked)

    viewModel.toggleSport("running")
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isSelectAllChecked)
  }

  @Test
  fun `complex scenario - mixed selections`() = runTest {
    // Select bar and some sports
    viewModel.toggleBar()
    viewModel.toggleSport("basket")
    viewModel.toggleSport("football")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isBarSelected)
    assertFalse(state.isClubSelected)
    assertEquals(2, state.selectedSportsCount)
    assertFalse(state.isAllSelected)
    assertFalse(state.isSelectAllChecked)
  }

  @Test
  fun `setEvents updates events list`() = runTest {
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
}
