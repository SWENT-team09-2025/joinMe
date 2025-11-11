package com.android.joinme.model.filter

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FilterRepositoryTest {

  @Before
  fun setup() {
    // Reset FilterRepository before each test
    FilterRepository.reset()
  }

  @After
  fun tearDown() {
    // Clean up after each test
    FilterRepository.reset()
  }

  @Test
  fun `initial state has no filters selected`() = runTest {
    val state = FilterRepository.filterState.first()

    assertFalse(state.isSocialSelected)
    assertFalse(state.isActivitySelected)
    assertFalse(state.isSelectAllChecked)
    assertEquals(0, state.selectedSportsCount)
    assertTrue(state.sportCategories.none { it.isChecked })
  }

  @Test
  fun `reset restores default state`() = runTest {
    // Modify state by selecting some filters
    FilterRepository.toggleSocial()
    FilterRepository.toggleActivity()

    // Reset
    FilterRepository.reset()

    val state = FilterRepository.filterState.first()
    assertFalse(state.isSocialSelected)
    assertFalse(state.isActivitySelected)
    assertFalse(state.isSelectAllChecked)
  }

  @Test
  fun `toggleSocial twice returns to initial state`() = runTest {
    FilterRepository.toggleSocial()

    val firstToggle = FilterRepository.filterState.first()
    assertTrue(firstToggle.isSocialSelected)

    FilterRepository.toggleSocial()

    val secondToggle = FilterRepository.filterState.first()
    assertFalse(secondToggle.isSocialSelected)
  }

  @Test
  fun `toggleActivity twice returns to initial state`() = runTest {
    FilterRepository.toggleActivity()
    val firstToggle = FilterRepository.filterState.first()
    assertTrue(firstToggle.isActivitySelected)

    FilterRepository.toggleActivity()
    val secondToggle = FilterRepository.filterState.first()
    assertFalse(secondToggle.isActivitySelected)
  }

  @Test
  fun `toggleSelectAll selects all sports`() = runTest {
    FilterRepository.toggleSelectAll()

    val state = FilterRepository.filterState.first()
    assertTrue(state.isSelectAllChecked)
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.sportCategories.all { it.isChecked })
  }

  @Test
  fun `toggleSelectAll twice returns sports to initial state`() = runTest {
    FilterRepository.toggleSelectAll()
    FilterRepository.toggleSelectAll()

    val state = FilterRepository.filterState.first()
    assertFalse(state.isSelectAllChecked)
    assertEquals(0, state.selectedSportsCount)
    assertTrue(state.sportCategories.none { it.isChecked })
  }

  @Test
  fun `toggleSport selects specific sport`() = runTest {
    FilterRepository.toggleSport("basket")

    val state = FilterRepository.filterState.first()
    val basketSport = state.sportCategories.find { it.id == "basket" }
    assertNotNull(basketSport)
    assertTrue(basketSport!!.isChecked)
    assertEquals(1, state.selectedSportsCount)
    assertFalse(state.isSelectAllChecked)
  }

  @Test
  fun `toggleSport twice returns sport to initial state`() = runTest {
    FilterRepository.toggleSport("football")
    FilterRepository.toggleSport("football")

    val state = FilterRepository.filterState.first()
    val footballSport = state.sportCategories.find { it.id == "football" }
    assertFalse(footballSport!!.isChecked)
    assertEquals(0, state.selectedSportsCount)
  }

  @Test
  fun `toggleSport with invalid id does nothing`() = runTest {
    FilterRepository.toggleSport("invalid_sport")

    val state = FilterRepository.filterState.first()
    assertEquals(0, state.selectedSportsCount)
  }

  @Test
  fun `sportCategories contains correct sports`() = runTest {
    val state = FilterRepository.filterState.first()
    val sportIds = state.sportCategories.map { it.id }

    assertTrue(sportIds.contains("basket"))
    assertTrue(sportIds.contains("football"))
    assertTrue(sportIds.contains("tennis"))
    assertTrue(sportIds.contains("running"))
  }

  @Test
  fun `selectedSportsCount is computed correctly`() = runTest {
    FilterRepository.toggleSport("basket")
    FilterRepository.toggleSport("tennis")

    val state = FilterRepository.filterState.first()
    assertEquals(2, state.selectedSportsCount)
  }

  @Test
  fun `applyFilters returns all events when no filters are selected`() = runTest {
    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS),
            createTestEvent("2", EventType.SOCIAL),
            createTestEvent("3", EventType.ACTIVITY))

    val filteredEvents = FilterRepository.applyFilters(events)

    assertEquals(3, filteredEvents.size)
  }

  @Test
  fun `applyFilters returns only social events when only social is selected`() = runTest {
    // Select only social
    FilterRepository.toggleSocial()

    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS),
            createTestEvent("2", EventType.SOCIAL),
            createTestEvent("3", EventType.ACTIVITY))

    val filteredEvents = FilterRepository.applyFilters(events)

    assertEquals(1, filteredEvents.size)
    assertEquals(EventType.SOCIAL, filteredEvents[0].type)
  }

  @Test
  fun `applyFilters returns only activity events when only activity is selected`() = runTest {
    // Select only activity
    FilterRepository.toggleActivity()

    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS),
            createTestEvent("2", EventType.SOCIAL),
            createTestEvent("3", EventType.ACTIVITY))

    val filteredEvents = FilterRepository.applyFilters(events)

    assertEquals(1, filteredEvents.size)
    assertEquals(EventType.ACTIVITY, filteredEvents[0].type)
  }

  @Test
  fun `applyFilters returns sports events when any sport is selected`() = runTest {
    // Select only one sport
    FilterRepository.toggleSport("basket")

    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS),
            createTestEvent("2", EventType.SOCIAL),
            createTestEvent("3", EventType.ACTIVITY))

    val filteredEvents = FilterRepository.applyFilters(events)

    assertEquals(1, filteredEvents.size)
    assertEquals(EventType.SPORTS, filteredEvents[0].type)
  }

  @Test
  fun `applyFilters returns multiple types when multiple filters selected`() = runTest {
    // Select social and activity
    FilterRepository.toggleSocial()
    FilterRepository.toggleActivity()

    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS),
            createTestEvent("2", EventType.SOCIAL),
            createTestEvent("3", EventType.ACTIVITY))

    val filteredEvents = FilterRepository.applyFilters(events)

    assertEquals(2, filteredEvents.size)
    assertTrue(filteredEvents.any { it.type == EventType.SOCIAL })
    assertTrue(filteredEvents.any { it.type == EventType.ACTIVITY })
  }

  @Test
  fun `applyFilters returns empty list when input is empty`() = runTest {
    val filteredEvents = FilterRepository.applyFilters(emptyList())

    assertEquals(0, filteredEvents.size)
  }

  @Test
  fun `complex filter scenario works correctly`() = runTest {
    // Select activity and some sports
    FilterRepository.toggleActivity()
    FilterRepository.toggleSport("basket")
    FilterRepository.toggleSport("football")

    val state = FilterRepository.filterState.first()
    assertFalse(state.isSocialSelected)
    assertTrue(state.isActivitySelected)
    assertEquals(2, state.selectedSportsCount)
    assertFalse(state.isSelectAllChecked)
  }

  private fun createTestEvent(id: String, type: EventType): Event {
    return Event(
        eventId = id,
        type = type,
        title = "Test Event $id",
        description = "Test description",
        location = Location(46.5191, 6.5668, "EPFL"),
        date = Timestamp.now(),
        duration = 60,
        participants = emptyList(),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = "owner1")
  }
}
