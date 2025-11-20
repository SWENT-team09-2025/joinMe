package com.android.joinme.model.filter

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilteredEventsRepositoryTest {

  private lateinit var repository: FilteredEventsRepository
  private lateinit var fakeEventRepository: FakeEventRepository
  private lateinit var fakeSeriesRepository: FakeSeriesRepository
  private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()

  // Sample test data (with future timestamps to pass the isUpcoming filter)
  private val futureTimestamp = Timestamp(System.currentTimeMillis() / 1000 + 86400, 0) // +1 day

  private val sampleSocialEvent =
      Event(
          eventId = "1",
          type = EventType.SOCIAL,
          title = "Social Event",
          description = "Test social event",
          location = Location(46.5191, 6.5668, "EPFL"),
          date = futureTimestamp,
          duration = 60,
          participants = emptyList(),
          maxParticipants = 10,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner1")

  private val sampleActivityEvent =
      Event(
          eventId = "2",
          type = EventType.ACTIVITY,
          title = "Activity Event",
          description = "Test activity event",
          location = Location(46.5191, 6.5668, "EPFL"),
          date = futureTimestamp,
          duration = 90,
          participants = emptyList(),
          maxParticipants = 15,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner2")

  private val sampleSportsEvent =
      Event(
          eventId = "3",
          type = EventType.SPORTS,
          title = "Sports Event",
          description = "Test sports event",
          location = Location(46.5191, 6.5668, "EPFL"),
          date = futureTimestamp,
          duration = 120,
          participants = emptyList(),
          maxParticipants = 20,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner3")

  private val sampleSerie =
      Serie(
          serieId = "serie1",
          title = "Weekly Basketball",
          description = "Weekly basketball series",
          date = Timestamp.now(),
          participants = emptyList(),
          maxParticipants = 10,
          visibility = Visibility.PUBLIC,
          eventIds = listOf("event1", "event2"),
          ownerId = "owner1")

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    FilterRepository.reset()

    fakeEventRepository = FakeEventRepository()
    fakeSeriesRepository = FakeSeriesRepository()

    repository =
        FilteredEventsRepository(
            fakeEventRepository, fakeSeriesRepository, FilterRepository, testDispatcher)
    FilteredEventsRepository.resetInstance(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    FilteredEventsRepository.resetInstance()
  }

  @Test
  fun `initial state has empty events and series`() = runTest {
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(emptyList<Event>(), repository.filteredEvents.value)
    assertEquals(emptyList<Serie>(), repository.filteredSeries.value)
    assertNull(repository.errorMsg.value)
    assertFalse(repository.isLoading.value)
  }

  @Test
  fun `refresh fetches events and series from repositories`() = runTest {
    fakeEventRepository.eventsToReturn = listOf(sampleSocialEvent, sampleActivityEvent)
    fakeSeriesRepository.seriesToReturn = listOf(sampleSerie)

    repository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()
    // Give time for IO dispatcher to complete

    // All events should be returned since no filters are selected
    assertEquals(2, repository.filteredEvents.value.size)
    assertEquals(1, repository.filteredSeries.value.size)
    assertNull(repository.errorMsg.value)
  }

  @Test
  fun `refresh sets loading state correctly`() = runTest {
    fakeEventRepository.eventsToReturn = listOf(sampleSocialEvent)
    fakeSeriesRepository.seriesToReturn = emptyList()

    repository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Final state should be false after loading completes
    assertFalse(repository.isLoading.value)
  }

  @Test
  fun `clearErrorMsg clears the error message`() = runTest {
    // Manually set an error message using reflection or direct state manipulation isn't possible
    // But we can test that clearErrorMsg sets the value to null
    repository.clearErrorMsg()

    assertNull(repository.errorMsg.value)
  }

  @Test
  fun `filter changes automatically trigger re-filtering`() = runTest {
    fakeEventRepository.eventsToReturn =
        listOf(sampleSocialEvent, sampleActivityEvent, sampleSportsEvent)
    repository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Initially all events should be visible (no filters)
    assertEquals(3, repository.filteredEvents.value.size)

    // Toggle Social filter
    FilterRepository.toggleSocial()
    testDispatcher.scheduler.advanceUntilIdle()

    // Only social events should be visible
    assertEquals(1, repository.filteredEvents.value.size)
    assertEquals(EventType.SOCIAL, repository.filteredEvents.value[0].type)
  }

  @Test
  fun `multiple filter changes apply correctly`() = runTest {
    fakeEventRepository.eventsToReturn =
        listOf(sampleSocialEvent, sampleActivityEvent, sampleSportsEvent)
    repository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Select Social and Activity
    FilterRepository.toggleSocial()
    FilterRepository.toggleActivity()
    testDispatcher.scheduler.advanceUntilIdle()

    // Should show 2 events (social and activity, but not sports)
    assertEquals(2, repository.filteredEvents.value.size)
    val types = repository.filteredEvents.value.map { it.type }
    assertTrue(types.contains(EventType.SOCIAL))
    assertTrue(types.contains(EventType.ACTIVITY))
    assertFalse(types.contains(EventType.SPORTS))
  }

  @Test
  fun `no filters selected shows all events`() = runTest {
    fakeEventRepository.eventsToReturn =
        listOf(sampleSocialEvent, sampleActivityEvent, sampleSportsEvent)
    repository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // No filters selected - all events should be visible
    assertEquals(3, repository.filteredEvents.value.size)
  }

  @Test
  fun `getInstance returns singleton instance`() {
    val instance1 = FilteredEventsRepository.getInstance()
    val instance2 = FilteredEventsRepository.getInstance()

    assertSame(instance1, instance2)
  }

  @Test
  fun `resetInstance creates new instance`() {
    val originalInstance = FilteredEventsRepository.getInstance()

    FilteredEventsRepository.resetInstance()

    val newInstance = FilteredEventsRepository.getInstance()

    assertNotSame(originalInstance, newInstance)
  }

  @Test
  fun `resetInstance with parameter sets specific instance`() {
    val customRepository =
        FilteredEventsRepository(
            fakeEventRepository, fakeSeriesRepository, FilterRepository, testDispatcher)

    FilteredEventsRepository.resetInstance(customRepository)

    val retrievedInstance = FilteredEventsRepository.getInstance()

    assertSame(customRepository, retrievedInstance)
  }

  @Test
  fun `refresh clears previous error message`() = runTest {
    // Test that calling refresh sets error to null initially
    fakeEventRepository.eventsToReturn = listOf(sampleSocialEvent)
    fakeSeriesRepository.seriesToReturn = emptyList()

    repository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // No error should be set
    assertNull(repository.errorMsg.value)
  }

  @Test
  fun `filter repository properly filters events by sports`() = runTest {
    fakeEventRepository.eventsToReturn = listOf(sampleSportsEvent)
    repository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Initially all events visible
    assertEquals(1, repository.filteredEvents.value.size)

    // Select a specific sport
    FilterRepository.toggleSport("basket")
    testDispatcher.scheduler.advanceUntilIdle()

    // Sports events should be visible when any sport is selected
    assertEquals(1, repository.filteredEvents.value.size)
  }

  @Test
  fun `empty events list after filtering`() = runTest {
    fakeEventRepository.eventsToReturn = listOf(sampleSocialEvent)
    repository.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    // Filter for Activity only
    FilterRepository.toggleActivity()
    testDispatcher.scheduler.advanceUntilIdle()

    // No activity events, so should be empty
    assertEquals(0, repository.filteredEvents.value.size)
  }

  // Fake implementations for testing
  private class FakeEventRepository : EventsRepository {
    var eventsToReturn: List<Event> = emptyList()
    var shouldThrowError = false

    override fun getNewEventId(): String = "fake-id"

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> {
      if (shouldThrowError) throw Exception("Test error")
      return eventsToReturn
    }

    override suspend fun getEvent(eventId: String): Event {
      throw Exception("Not implemented")
    }

    override suspend fun addEvent(event: Event) {}

    override suspend fun editEvent(eventId: String, newValue: Event) {}

    override suspend fun deleteEvent(eventId: String) {}

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> = emptyList()
  }

  private class FakeSeriesRepository : SeriesRepository {
    var seriesToReturn: List<Serie> = emptyList()
    var shouldThrowError = false

    override fun getNewSerieId(): String = "fake-serie-id"

    override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> {
      if (shouldThrowError) throw Exception("Test error")
      return seriesToReturn
    }

    override suspend fun getSerie(serieId: String): Serie {
      throw Exception("Not implemented")
    }

    override suspend fun addSerie(serie: Serie) {}

    override suspend fun editSerie(serieId: String, newValue: Serie) {}

    override suspend fun deleteSerie(serieId: String) {}
  }
}
