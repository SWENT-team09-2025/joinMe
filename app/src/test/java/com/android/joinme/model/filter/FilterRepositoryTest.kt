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
  fun `initial state has all filters selected`() = runTest {
    val state = FilterRepository.filterState.first()

    assertTrue(state.isAllSelected)
    assertTrue(state.isSocialSelected)
    assertTrue(state.isActivitySelected)
    assertTrue(state.isSelectAllChecked)
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.sportCategories.all { it.isChecked })
  }

  @Test
  fun `reset restores default state`() = runTest {
    // Modify state
    FilterRepository.toggleAll()

    // Reset
    FilterRepository.reset()

    val state = FilterRepository.filterState.first()
    assertTrue(state.isAllSelected)
    assertTrue(state.isSocialSelected)
    assertTrue(state.isActivitySelected)
    assertTrue(state.isSelectAllChecked)
  }

  @Test
  fun `toggleAll deselects all filters when initially selected`() = runTest {
    FilterRepository.toggleAll()

    val state = FilterRepository.filterState.first()
    assertFalse(state.isAllSelected)
    assertFalse(state.isSocialSelected)
    assertFalse(state.isActivitySelected)
    assertEquals(0, state.selectedSportsCount)
    assertTrue(state.sportCategories.none { it.isChecked })
  }

  @Test
  fun `toggleAll twice returns to initial state`() = runTest {
    FilterRepository.toggleAll()
    FilterRepository.toggleAll()

    val state = FilterRepository.filterState.first()
    assertTrue(state.isAllSelected)
    assertTrue(state.isSocialSelected)
    assertTrue(state.isActivitySelected)
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.sportCategories.all { it.isChecked })
  }

  @Test
  fun `toggleSocial deselects social filter`() = runTest {
    FilterRepository.toggleSocial()

    val state = FilterRepository.filterState.first()
    assertFalse(state.isSocialSelected)
    assertFalse(state.isAllSelected)
  }

  @Test
  fun `toggleSocial twice returns to initial state`() = runTest {
    FilterRepository.toggleSocial()
    FilterRepository.toggleSocial()

    val state = FilterRepository.filterState.first()
    assertTrue(state.isSocialSelected)
    assertTrue(state.isAllSelected)
  }

  @Test
  fun `toggleActivity deselects activity filter`() = runTest {
    FilterRepository.toggleActivity()

    val state = FilterRepository.filterState.first()
    assertFalse(state.isActivitySelected)
    assertFalse(state.isAllSelected)
  }

  @Test
  fun `toggleActivity twice returns to initial state`() = runTest {
    FilterRepository.toggleActivity()
    FilterRepository.toggleActivity()

    val state = FilterRepository.filterState.first()
    assertTrue(state.isActivitySelected)
    assertTrue(state.isAllSelected)
  }

  @Test
  fun `toggleSelectAll deselects all sports`() = runTest {
    FilterRepository.toggleSelectAll()

    val state = FilterRepository.filterState.first()
    assertFalse(state.isSelectAllChecked)
    assertEquals(0, state.selectedSportsCount)
    assertTrue(state.sportCategories.none { it.isChecked })
    assertFalse(state.isAllSelected)
  }

  @Test
  fun `toggleSelectAll twice returns sports to initial state`() = runTest {
    FilterRepository.toggleSelectAll()
    FilterRepository.toggleSelectAll()

    val state = FilterRepository.filterState.first()
    assertTrue(state.isSelectAllChecked)
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.sportCategories.all { it.isChecked })
    assertTrue(state.isAllSelected)
  }

  @Test
  fun `toggleSport deselects specific sport`() = runTest {
    FilterRepository.toggleSport("basket")

    val state = FilterRepository.filterState.first()
    val basketSport = state.sportCategories.find { it.id == "basket" }
    assertNotNull(basketSport)
    assertFalse(basketSport!!.isChecked)
    assertEquals(3, state.selectedSportsCount)
    assertFalse(state.isSelectAllChecked)
    assertFalse(state.isAllSelected)
  }

  @Test
  fun `toggleSport twice returns sport to initial state`() = runTest {
    FilterRepository.toggleSport("football")
    FilterRepository.toggleSport("football")

    val state = FilterRepository.filterState.first()
    val footballSport = state.sportCategories.find { it.id == "football" }
    assertTrue(footballSport!!.isChecked)
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.isAllSelected)
  }

  @Test
  fun `toggleSport with invalid id does nothing`() = runTest {
    FilterRepository.toggleSport("invalid_sport")

    val state = FilterRepository.filterState.first()
    assertEquals(4, state.selectedSportsCount)
    assertTrue(state.isAllSelected)
  }

  @Test
  fun `isAllSelected is false when social is deselected`() = runTest {
    FilterRepository.toggleSocial()

    val state = FilterRepository.filterState.first()
    assertFalse(state.isAllSelected)
  }

  @Test
  fun `isAllSelected is false when activity is deselected`() = runTest {
    FilterRepository.toggleActivity()

    val state = FilterRepository.filterState.first()
    assertFalse(state.isAllSelected)
  }

  @Test
  fun `isAllSelected is false when any sport is deselected`() = runTest {
    FilterRepository.toggleSport("tennis")

    val state = FilterRepository.filterState.first()
    assertFalse(state.isAllSelected)
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
  fun `applyFilters returns all events when isAllSelected is true`() = runTest {
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
    // Deselect all
    FilterRepository.toggleAll()
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
    // Deselect all
    FilterRepository.toggleAll()
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
    // Deselect all
    FilterRepository.toggleAll()
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
  fun `applyFilters returns empty list when no filters are selected`() = runTest {
    // Deselect all filters
    FilterRepository.toggleAll()

    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS),
            createTestEvent("2", EventType.SOCIAL),
            createTestEvent("3", EventType.ACTIVITY))

    val filteredEvents = FilterRepository.applyFilters(events)

    assertEquals(0, filteredEvents.size)
  }

  @Test
  fun `applyFilters returns multiple types when multiple filters selected`() = runTest {
    // Deselect all
    FilterRepository.toggleAll()
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
    // Deselect activity
    FilterRepository.toggleActivity()
    // Deselect some sports
    FilterRepository.toggleSport("basket")
    FilterRepository.toggleSport("football")

    val state = FilterRepository.filterState.first()
    assertTrue(state.isSocialSelected)
    assertFalse(state.isActivitySelected)
    assertEquals(2, state.selectedSportsCount)
    assertFalse(state.isAllSelected)
    assertFalse(state.isSelectAllChecked)
  }

  @Test
  fun `isAllSelected becomes true when all filters are re-enabled`() = runTest {
    // Deselect some filters
    FilterRepository.toggleSocial()
    FilterRepository.toggleActivity()
    FilterRepository.toggleSport("basket")

    assertFalse(FilterRepository.filterState.first().isAllSelected)

    // Re-enable all filters
    FilterRepository.toggleSocial()
    FilterRepository.toggleActivity()
    FilterRepository.toggleSport("basket")

    val state = FilterRepository.filterState.first()
    assertTrue(state.isAllSelected)
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
