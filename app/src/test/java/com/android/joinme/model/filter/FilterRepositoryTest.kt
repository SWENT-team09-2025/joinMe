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
  fun `reset restores default state`() = runTest {
    // Modify state by selecting some filters
    FilterRepository.toggleSocial()
    FilterRepository.toggleActivity()

    // Reset
    FilterRepository.reset()

    val state = FilterRepository.filterState.first()
    assertFalse(state.isSocialSelected)
    assertFalse(state.isActivitySelected)
    assertFalse(state.isSportSelected)
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
  fun `toggleSport twice returns to initial state`() = runTest {
    FilterRepository.toggleSport()
    val firstToggle = FilterRepository.filterState.first()
    assertTrue(firstToggle.isSportSelected)

    FilterRepository.toggleSport()
    val secondToggle = FilterRepository.filterState.first()
    assertFalse(secondToggle.isSportSelected)
  }

  @Test
  fun `applyFilters returns all events when no filters are selected`() = runTest {
    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS),
            createTestEvent("2", EventType.SOCIAL),
            createTestEvent("3", EventType.ACTIVITY))

    val filteredEvents = FilterRepository.applyFiltersToEvents(events)

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

    val filteredEvents = FilterRepository.applyFiltersToEvents(events)

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

    val filteredEvents = FilterRepository.applyFiltersToEvents(events)

    assertEquals(1, filteredEvents.size)
    assertEquals(EventType.ACTIVITY, filteredEvents[0].type)
  }

  @Test
  fun `applyFilters returns sports events when sport filter is selected`() = runTest {
    // Select sport filter
    FilterRepository.toggleSport()

    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS),
            createTestEvent("2", EventType.SOCIAL),
            createTestEvent("3", EventType.ACTIVITY))

    val filteredEvents = FilterRepository.applyFiltersToEvents(events)

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

    val filteredEvents = FilterRepository.applyFiltersToEvents(events)

    assertEquals(2, filteredEvents.size)
    assertTrue(filteredEvents.any { it.type == EventType.SOCIAL })
    assertTrue(filteredEvents.any { it.type == EventType.ACTIVITY })
  }

  @Test
  fun `applyFilters returns empty list when input is empty`() = runTest {
    val filteredEvents = FilterRepository.applyFiltersToEvents(emptyList())

    assertEquals(0, filteredEvents.size)
  }

  @Test
  fun `complex filter scenario works correctly`() = runTest {
    // Select activity and sport
    FilterRepository.toggleActivity()
    FilterRepository.toggleSport()

    val state = FilterRepository.filterState.first()
    assertFalse(state.isSocialSelected)
    assertTrue(state.isActivitySelected)
    assertTrue(state.isSportSelected)
  }

  @Test
  fun `toggleMyEvents toggles showMyEvents state`() = runTest {
    // Initially false
    var state = FilterRepository.filterState.first()
    assertFalse(state.showMyEvents)

    // Toggle on
    FilterRepository.toggleMyEvents()
    state = FilterRepository.filterState.first()
    assertTrue(state.showMyEvents)

    // Toggle off
    FilterRepository.toggleMyEvents()
    state = FilterRepository.filterState.first()
    assertFalse(state.showMyEvents)
  }

  @Test
  fun `toggleJoinedEvents toggles showJoinedEvents state`() = runTest {
    // Initially false
    var state = FilterRepository.filterState.first()
    assertFalse(state.showJoinedEvents)

    // Toggle on
    FilterRepository.toggleJoinedEvents()
    state = FilterRepository.filterState.first()
    assertTrue(state.showJoinedEvents)

    // Toggle off
    FilterRepository.toggleJoinedEvents()
    state = FilterRepository.filterState.first()
    assertFalse(state.showJoinedEvents)
  }

  @Test
  fun `toggleOtherEvents toggles showOtherEvents state`() = runTest {
    // Initially false
    var state = FilterRepository.filterState.first()
    assertFalse(state.showOtherEvents)

    // Toggle on
    FilterRepository.toggleOtherEvents()
    state = FilterRepository.filterState.first()
    assertTrue(state.showOtherEvents)

    // Toggle off
    FilterRepository.toggleOtherEvents()
    state = FilterRepository.filterState.first()
    assertFalse(state.showOtherEvents)
  }

  @Test
  fun `applyFilters returns only my events when showMyEvents is selected`() = runTest {
    FilterRepository.toggleMyEvents()

    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS, ownerId = "user1", participants = emptyList()),
            createTestEvent(
                "2", EventType.SOCIAL, ownerId = "user2", participants = listOf("user1")),
            createTestEvent("3", EventType.ACTIVITY, ownerId = "user2", participants = emptyList()))

    val filteredEvents = FilterRepository.applyFiltersToEvents(events, currentUserId = "user1")

    assertEquals(1, filteredEvents.size)
    assertEquals("1", filteredEvents[0].eventId)
  }

  @Test
  fun `applyFilters returns only joined events when showJoinedEvents is selected`() = runTest {
    FilterRepository.toggleJoinedEvents()

    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS, ownerId = "user1", participants = emptyList()),
            createTestEvent(
                "2", EventType.SOCIAL, ownerId = "user2", participants = listOf("user1")),
            createTestEvent("3", EventType.ACTIVITY, ownerId = "user2", participants = emptyList()))

    val filteredEvents = FilterRepository.applyFiltersToEvents(events, currentUserId = "user1")

    assertEquals(1, filteredEvents.size)
    assertEquals("2", filteredEvents[0].eventId)
  }

  @Test
  fun `applyFilters returns only other events when showOtherEvents is selected`() = runTest {
    FilterRepository.toggleOtherEvents()

    val events =
        listOf(
            createTestEvent(
                "1", EventType.SPORTS, ownerId = "user1", participants = listOf("user1")),
            createTestEvent(
                "2", EventType.SOCIAL, ownerId = "user2", participants = listOf("user1")),
            createTestEvent("3", EventType.ACTIVITY, ownerId = "user2", participants = emptyList()))

    val filteredEvents = FilterRepository.applyFiltersToEvents(events, currentUserId = "user1")

    // Should only return event 3 (not owned by user, and user not participating)
    assertEquals(1, filteredEvents.size)
    assertEquals("3", filteredEvents[0].eventId)
  }

  @Test
  fun `applyFilters returns combined results when multiple participation filters selected`() =
      runTest {
        FilterRepository.toggleMyEvents()
        FilterRepository.toggleOtherEvents()

        val events =
            listOf(
                createTestEvent(
                    "1", EventType.SPORTS, ownerId = "user1", participants = emptyList()),
                createTestEvent(
                    "2", EventType.SOCIAL, ownerId = "user2", participants = listOf("user1")),
                createTestEvent(
                    "3", EventType.ACTIVITY, ownerId = "user2", participants = emptyList()))

        val filteredEvents = FilterRepository.applyFiltersToEvents(events, currentUserId = "user1")

        assertEquals(2, filteredEvents.size)
        assertTrue(filteredEvents.any { it.eventId == "1" })
        assertTrue(filteredEvents.any { it.eventId == "3" })
      }

  @Test
  fun `applyFilters returns all events when no participation filters and no userId`() = runTest {
    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS, ownerId = "user1", participants = emptyList()),
            createTestEvent(
                "2", EventType.SOCIAL, ownerId = "user2", participants = listOf("user1")),
            createTestEvent("3", EventType.ACTIVITY, ownerId = "user2", participants = emptyList()))

    val filteredEvents = FilterRepository.applyFiltersToEvents(events, currentUserId = "")

    assertEquals(3, filteredEvents.size)
  }

  @Test
  fun `applyFilters returns all events when participation filters set but userId empty`() =
      runTest {
        FilterRepository.toggleMyEvents()

        val events =
            listOf(
                createTestEvent(
                    "1", EventType.SPORTS, ownerId = "user1", participants = emptyList()),
                createTestEvent(
                    "2", EventType.SOCIAL, ownerId = "user2", participants = emptyList()))

        val filteredEvents = FilterRepository.applyFiltersToEvents(events, currentUserId = "")

        assertEquals(2, filteredEvents.size)
      }

  @Test
  fun `applyFilters combines type and participation filters correctly`() = runTest {
    FilterRepository.toggleSocial()
    FilterRepository.toggleMyEvents()

    val events =
        listOf(
            createTestEvent("1", EventType.SPORTS, ownerId = "user1", participants = emptyList()),
            createTestEvent("2", EventType.SOCIAL, ownerId = "user1", participants = emptyList()),
            createTestEvent("3", EventType.SOCIAL, ownerId = "user2", participants = emptyList()))

    val filteredEvents = FilterRepository.applyFiltersToEvents(events, currentUserId = "user1")

    // Should only return social events owned by user1
    assertEquals(1, filteredEvents.size)
    assertEquals("2", filteredEvents[0].eventId)
  }

  @Test
  fun `applyFiltersToSeries returns all series when no filters selected`() = runTest {
    val events =
        listOf(createTestEvent("e1", EventType.SPORTS), createTestEvent("e2", EventType.SOCIAL))
    val series = listOf(createTestSerie("s1", listOf("e1")), createTestSerie("s2", listOf("e2")))

    val filteredSeries = FilterRepository.applyFiltersToSeries(series, events)

    assertEquals(2, filteredSeries.size)
  }

  @Test
  fun `applyFiltersToSeries filters by event type correctly`() = runTest {
    FilterRepository.toggleSocial()

    val events =
        listOf(
            createTestEvent("e1", EventType.SPORTS),
            createTestEvent("e2", EventType.SOCIAL),
            createTestEvent("e3", EventType.ACTIVITY))
    val series =
        listOf(
            createTestSerie("s1", listOf("e1")), // sports only
            createTestSerie("s2", listOf("e2")), // social only
            createTestSerie("s3", listOf("e1", "e2")) // mixed
            )

    val filteredSeries = FilterRepository.applyFiltersToSeries(series, events)

    // Should return series with at least one social event
    assertEquals(2, filteredSeries.size)
    assertTrue(filteredSeries.any { it.serieId == "s2" })
    assertTrue(filteredSeries.any { it.serieId == "s3" })
  }

  @Test
  fun `applyFiltersToSeries returns empty when no events match filter`() = runTest {
    FilterRepository.toggleActivity()

    val events =
        listOf(createTestEvent("e1", EventType.SPORTS), createTestEvent("e2", EventType.SOCIAL))
    val series = listOf(createTestSerie("s1", listOf("e1")), createTestSerie("s2", listOf("e2")))

    val filteredSeries = FilterRepository.applyFiltersToSeries(series, events)

    assertEquals(0, filteredSeries.size)
  }

  @Test
  fun `applyFiltersToSeries returns only my series when showMyEvents is selected`() = runTest {
    FilterRepository.toggleMyEvents()

    val events = listOf(createTestEvent("e1", EventType.SPORTS))
    val series =
        listOf(
            createTestSerie("s1", listOf("e1"), ownerId = "user1", participants = emptyList()),
            createTestSerie("s2", listOf("e1"), ownerId = "user2", participants = listOf("user1")),
            createTestSerie("s3", listOf("e1"), ownerId = "user2", participants = emptyList()))

    val filteredSeries =
        FilterRepository.applyFiltersToSeries(series, events, currentUserId = "user1")

    assertEquals(1, filteredSeries.size)
    assertEquals("s1", filteredSeries[0].serieId)
  }

  @Test
  fun `applyFiltersToSeries returns only joined series when showJoinedEvents is selected`() =
      runTest {
        FilterRepository.toggleJoinedEvents()

        val events = listOf(createTestEvent("e1", EventType.SPORTS))
        val series =
            listOf(
                createTestSerie("s1", listOf("e1"), ownerId = "user1", participants = emptyList()),
                createTestSerie(
                    "s2", listOf("e1"), ownerId = "user2", participants = listOf("user1")),
                createTestSerie("s3", listOf("e1"), ownerId = "user2", participants = emptyList()))

        val filteredSeries =
            FilterRepository.applyFiltersToSeries(series, events, currentUserId = "user1")

        assertEquals(1, filteredSeries.size)
        assertEquals("s2", filteredSeries[0].serieId)
      }

  @Test
  fun `applyFiltersToSeries returns only other series when showOtherEvents is selected`() =
      runTest {
        FilterRepository.toggleOtherEvents()

        val events = listOf(createTestEvent("e1", EventType.SPORTS))
        val series =
            listOf(
                createTestSerie(
                    "s1", listOf("e1"), ownerId = "user1", participants = listOf("user1")),
                createTestSerie(
                    "s2", listOf("e1"), ownerId = "user2", participants = listOf("user1")),
                createTestSerie("s3", listOf("e1"), ownerId = "user2", participants = emptyList()))

        val filteredSeries =
            FilterRepository.applyFiltersToSeries(series, events, currentUserId = "user1")

        // Should only return s3 (not owned by user, and user not participating)
        assertEquals(1, filteredSeries.size)
        assertEquals("s3", filteredSeries[0].serieId)
      }

  @Test
  fun `applyFiltersToSeries combines type and participation filters correctly`() = runTest {
    FilterRepository.toggleSocial()
    FilterRepository.toggleMyEvents()

    val events =
        listOf(createTestEvent("e1", EventType.SPORTS), createTestEvent("e2", EventType.SOCIAL))
    val series =
        listOf(
            createTestSerie(
                "s1", listOf("e1"), ownerId = "user1", participants = emptyList()), // Sports, owned
            createTestSerie(
                "s2", listOf("e2"), ownerId = "user1", participants = emptyList()), // Social, owned
            createTestSerie(
                "s3",
                listOf("e2"),
                ownerId = "user2",
                participants = emptyList()) // Social, not owned
            )

    val filteredSeries =
        FilterRepository.applyFiltersToSeries(series, events, currentUserId = "user1")

    // Should only return social series owned by user1
    assertEquals(1, filteredSeries.size)
    assertEquals("s2", filteredSeries[0].serieId)
  }

  @Test
  fun `applyFiltersToSeries returns all series when participation filters set but userId empty`() =
      runTest {
        FilterRepository.toggleMyEvents()

        val events = listOf(createTestEvent("e1", EventType.SPORTS))
        val series =
            listOf(
                createTestSerie("s1", listOf("e1"), ownerId = "user1", participants = emptyList()),
                createTestSerie("s2", listOf("e1"), ownerId = "user2", participants = emptyList()))

        val filteredSeries =
            FilterRepository.applyFiltersToSeries(series, events, currentUserId = "")

        assertEquals(2, filteredSeries.size)
      }

  @Test
  fun `applyFiltersToSeries returns combined results when multiple participation filters selected`() =
      runTest {
        FilterRepository.toggleMyEvents()
        FilterRepository.toggleOtherEvents()

        val events = listOf(createTestEvent("e1", EventType.SPORTS))
        val series =
            listOf(
                createTestSerie("s1", listOf("e1"), ownerId = "user1", participants = emptyList()),
                createTestSerie(
                    "s2", listOf("e1"), ownerId = "user2", participants = listOf("user1")),
                createTestSerie("s3", listOf("e1"), ownerId = "user2", participants = emptyList()))

        val filteredSeries =
            FilterRepository.applyFiltersToSeries(series, events, currentUserId = "user1")

        // Should return s1 (my series) and s3 (other series)
        assertEquals(2, filteredSeries.size)
        assertTrue(filteredSeries.any { it.serieId == "s1" })
        assertTrue(filteredSeries.any { it.serieId == "s3" })
      }

  @Test
  fun `applyFiltersToSeries applies participation filters even with no type filters`() = runTest {
    // This is the bug we fixed - participation filters should apply even without type filters
    FilterRepository.toggleMyEvents()

    val events = listOf(createTestEvent("e1", EventType.SPORTS))
    val series =
        listOf(
            createTestSerie("s1", listOf("e1"), ownerId = "user1", participants = emptyList()),
            createTestSerie("s2", listOf("e1"), ownerId = "user2", participants = emptyList()))

    val filteredSeries =
        FilterRepository.applyFiltersToSeries(series, events, currentUserId = "user1")

    // Should only return my series, even though no type filters are selected
    assertEquals(1, filteredSeries.size)
    assertEquals("s1", filteredSeries[0].serieId)
  }

  private fun createTestEvent(
      id: String,
      type: EventType,
      ownerId: String = "owner1",
      participants: List<String> = emptyList()
  ): Event {
    return Event(
        eventId = id,
        type = type,
        title = "Test Event $id",
        description = "Test description",
        location = Location(46.5191, 6.5668, "EPFL"),
        date = Timestamp.now(),
        duration = 60,
        participants = participants,
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = ownerId)
  }

  private fun createTestSerie(
      id: String,
      eventIds: List<String>,
      ownerId: String = "owner1",
      participants: List<String> = emptyList()
  ): com.android.joinme.model.serie.Serie {
    return com.android.joinme.model.serie.Serie(
        serieId = id,
        title = "Test Serie $id",
        description = "Test description",
        date = Timestamp.now(),
        participants = participants,
        maxParticipants = 10,
        visibility = com.android.joinme.model.utils.Visibility.PUBLIC,
        eventIds = eventIds,
        ownerId = ownerId)
  }
}
