package com.android.joinme.ui.overview

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.map.Location
import com.android.joinme.model.map.LocationRepository
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.serie.SerieFilter
import com.android.joinme.model.serie.SeriesRepository
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CreateEventForSerieViewModel.
 *
 * Tests the creation of events that belong to an existing serie, including:
 * - Form validation
 * - Date calculation based on existing events in the serie
 * - Inheriting properties from the parent serie
 * - Updating the serie's eventIds list
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateEventForSerieViewModelTest {

  // ---- Fake repositories ----
  private class FakeEventsRepository : EventsRepository {
    val added = mutableListOf<Event>()
    private var idCounter = 1

    override suspend fun addEvent(event: Event) {
      added += event
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      /* no-op */
    }

    override suspend fun deleteEvent(eventId: String) {
      /* no-op */
    }

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
      return added.filter { it.eventId in eventIds }.sortedBy { it.date.toDate().time }
    }

    override suspend fun getEvent(eventId: String): Event =
        added.find { it.eventId == eventId } ?: throw NoSuchElementException("Event not found")

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> = added.toList()

    override fun getNewEventId(): String = "fake-event-id-${idCounter++}"

    override suspend fun getCommonEvents(userIds: List<String>): List<Event> {
      if (userIds.isEmpty()) return emptyList()
      return added
          .filter { event -> userIds.all { userId -> event.participants.contains(userId) } }
          .sortedBy { it.date.toDate().time }
    }
  }

  private class FakeSeriesRepository : SeriesRepository {
    val series = mutableMapOf<String, Serie>()

    override suspend fun addSerie(serie: Serie) {
      series[serie.serieId] = serie
    }

    override suspend fun editSerie(serieId: String, newValue: Serie) {
      series[serieId] = newValue
    }

    override suspend fun deleteSerie(serieId: String) {
      series.remove(serieId)
    }

    override suspend fun getSerie(serieId: String): Serie =
        series[serieId] ?: throw NoSuchElementException("Serie not found")

    override suspend fun getAllSeries(serieFilter: SerieFilter): List<Serie> =
        series.values.toList()

    override suspend fun getSeriesByIds(seriesIds: List<String>): List<Serie> {
      return seriesIds.mapNotNull { series[it] }
    }

    override fun getNewSerieId(): String = "fake-serie-id"
  }

  private class FakeLocationRepository : LocationRepository {
    override suspend fun search(query: String): List<Location> {
      return if (query.isBlank()) {
        emptyList()
      } else {
        listOf(
            Location(latitude = 1.0, longitude = 1.0, name = "Test Location 1"),
            Location(latitude = 2.0, longitude = 2.0, name = "Test Location 2"))
      }
    }
  }

  private class FakeGroupRepository : GroupRepository {
    val groups = mutableListOf<Group>()

    override fun getNewGroupId(): String = "group-id"

    override suspend fun getAllGroups(): List<Group> = groups.toList()

    override suspend fun getGroup(groupId: String): Group =
        groups.find { it.id == groupId } ?: throw NoSuchElementException("Group not found")

    override suspend fun addGroup(group: Group) {
      groups += group
    }

    override suspend fun editGroup(groupId: String, newValue: Group) {
      val index = groups.indexOfFirst { it.id == groupId }
      if (index != -1) {
        groups[index] = newValue
      }
    }

    override suspend fun deleteGroup(groupId: String, userId: String) {
      groups.removeIf { it.id == groupId }
    }

    override suspend fun leaveGroup(groupId: String, userId: String) {}

    override suspend fun joinGroup(groupId: String, userId: String) {}

    override suspend fun getCommonGroups(userIds: List<String>): List<Group> {
      if (userIds.isEmpty()) return emptyList()
      return groups.filter { group -> userIds.all { userId -> group.memberIds.contains(userId) } }
    }
  }

  private lateinit var eventRepo: FakeEventsRepository
  private lateinit var serieRepo: FakeSeriesRepository
  private lateinit var groupRepo: FakeGroupRepository
  private lateinit var locationRepo: FakeLocationRepository
  private lateinit var vm: CreateEventForSerieViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    eventRepo = FakeEventsRepository()
    serieRepo = FakeSeriesRepository()
    groupRepo = FakeGroupRepository()
    locationRepo = FakeLocationRepository()
    vm = CreateEventForSerieViewModel(eventRepo, serieRepo, groupRepo, locationRepo)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ---------- Basic validity ----------

  @Test
  fun initialState_isInvalid() {
    val s = vm.uiState.value
    assertFalse(s.isValid)
    assertFalse(s.isLoading)
  }

  @Test
  fun fillingAllFields_makesFormValid() = runTest {
    vm.setType("SPORTS")
    vm.setTitle("Weekly Soccer Match")
    vm.setDescription("Weekly soccer match for serie")
    vm.setDuration("90")
    vm.setLocationQuery("Test Location")
    vm.searchLocations("Test Location")
    advanceUntilIdle()

    val location = vm.uiState.value.locationSuggestions.firstOrNull()
    assertNotNull(location)
    vm.selectLocation(location!!)

    assertTrue(vm.uiState.value.isValid)
  }

  // ---------- Field validation ----------

  @Test
  fun setType_blank_marksInvalid() {
    vm.setType("")
    val s = vm.uiState.value
    assertNotNull(s.invalidTypeMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setType_invalid_marksInvalid() {
    vm.setType("INVALID_TYPE")
    val s = vm.uiState.value
    assertNotNull(s.invalidTypeMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setType_valid_marksValid() {
    vm.setType("SPORTS")
    val s = vm.uiState.value
    assertNull(s.invalidTypeMsg)
    assertEquals("SPORTS", s.type)
  }

  @Test
  fun setTitle_blank_marksInvalid() {
    vm.setTitle("")
    val s = vm.uiState.value
    assertNotNull(s.invalidTitleMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setTitle_nonBlank_marksValid() {
    vm.setTitle("Match 1")
    val s = vm.uiState.value
    assertNull(s.invalidTitleMsg)
    assertEquals("Match 1", s.title)
  }

  @Test
  fun setDescription_blank_marksInvalid() {
    vm.setDescription("")
    val s = vm.uiState.value
    assertNotNull(s.invalidDescriptionMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setDescription_nonBlank_marksValid() {
    vm.setDescription("First match of the serie")
    val s = vm.uiState.value
    assertNull(s.invalidDescriptionMsg)
    assertEquals("First match of the serie", s.description)
  }

  @Test
  fun setDuration_nonNumeric_marksInvalid() {
    vm.setDuration("ninety")
    val s = vm.uiState.value
    assertNotNull(s.invalidDurationMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setDuration_negative_marksInvalid() {
    vm.setDuration("-30")
    val s = vm.uiState.value
    assertNotNull(s.invalidDurationMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setDuration_zero_marksInvalid() {
    vm.setDuration("0")
    val s = vm.uiState.value
    assertNotNull(s.invalidDurationMsg)
    assertFalse(s.isValid)
  }

  @Test
  fun setDuration_positive_marksValid() {
    vm.setDuration("60")
    val s = vm.uiState.value
    assertNull(s.invalidDurationMsg)
    assertEquals("60", s.duration)
  }

  @Test
  fun setLocation_blank_marksInvalid() {
    vm.setLocation("")
    val s = vm.uiState.value
    assertNotNull(s.invalidLocationMsg)
    assertFalse(s.isValid)
  }

  // ---------- Location search and selection ----------

  @Test
  fun searchLocations_withBlankQuery_returnEmpty() = runTest {
    vm.searchLocations("")
    advanceUntilIdle()

    assertTrue(vm.uiState.value.locationSuggestions.isEmpty())
  }

  @Test
  fun searchLocations_withQuery_returnsResults() = runTest {
    vm.setLocationQuery("Stadium")
    vm.searchLocations("Stadium")
    advanceUntilIdle()

    assertTrue(vm.uiState.value.locationSuggestions.isNotEmpty())
  }

  @Test
  fun selectLocation_updatesSelectedLocation() = runTest {
    val location = Location(latitude = 3.0, longitude = 4.0, name = "Stadium")
    vm.selectLocation(location)

    val s = vm.uiState.value
    assertEquals(location, s.selectedLocation)
    assertEquals("Stadium", s.location)
    assertEquals("Stadium", s.locationQuery)
    assertTrue(s.locationSuggestions.isEmpty())
    assertNull(s.invalidLocationMsg)
  }

  @Test
  fun clearLocation_resetsLocation() = runTest {
    val location = Location(latitude = 3.0, longitude = 4.0, name = "Stadium")
    vm.selectLocation(location)
    vm.clearLocation()

    val s = vm.uiState.value
    assertNull(s.selectedLocation)
    assertEquals("", s.location)
    assertEquals("", s.locationQuery)
    assertNotNull(s.invalidLocationMsg)
  }

  // ---------- createEventForSerie() behavior ----------

  @Test
  fun createEventForSerie_withInvalidForm_returnsFalse() = runTest {
    val ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()

    assertFalse(ok)
    assertTrue(eventRepo.added.isEmpty())
    assertNotNull(vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)
  }

  @Test
  fun createEventForSerie_withNonexistentSerie_returnsFalse() = runTest {
    // Fill form
    vm.setType("SPORTS")
    vm.setTitle("Match 1")
    vm.setDescription("First match")
    vm.setDuration("90")
    vm.selectLocation(Location(latitude = 1.0, longitude = 1.0, name = "Stadium"))

    assertTrue(vm.uiState.value.isValid)

    val ok = vm.createEventForSerie("nonexistent-serie")
    advanceUntilIdle()

    assertFalse(ok)
    assertTrue(eventRepo.added.isEmpty())
    assertNotNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun createEventForSerie_withEmptySerie_usesSerieDate() = runTest {
    // Create a serie with no events
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val serieDate = Timestamp(calendar.time)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Soccer League",
            description = "Weekly soccer",
            date = serieDate,
            participants = listOf("owner-1"),
            maxParticipants = 20,
            visibility = Visibility.PUBLIC,
            eventIds = emptyList(),
            ownerId = "owner-1")

    serieRepo.addSerie(serie)

    // Fill form
    vm.setType("SPORTS")
    vm.setTitle("Match 1")
    vm.setDescription("First match")
    vm.setDuration("90")
    vm.selectLocation(Location(latitude = 1.0, longitude = 1.0, name = "Stadium"))

    assertTrue(vm.uiState.value.isValid)

    val ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()

    assertTrue(ok)
    assertEquals(1, eventRepo.added.size)

    val event = eventRepo.added.first()
    assertEquals(serieDate.toDate().time, event.date.toDate().time)
    assertEquals(EventType.SPORTS, event.type)
    assertEquals("Match 1", event.title)
    assertEquals("First match", event.description)
    assertEquals(90, event.duration)
    assertEquals(20, event.maxParticipants) // Inherited from serie
    assertEquals(EventVisibility.PUBLIC, event.visibility) // Inherited from serie
    assertEquals("owner-1", event.ownerId) // Inherited from serie
    assertTrue(event.participants.isEmpty()) // Starts with empty list

    // Verify serie was updated
    val updatedSerie = serieRepo.getSerie("serie-1")
    assertEquals(1, updatedSerie.eventIds.size)
    assertEquals(event.eventId, updatedSerie.eventIds.first())

    // Verify lastEventEndTime was updated to the end of the new event
    val expectedEndTime = event.date.toDate().time + (event.duration * 60 * 1000)
    assertEquals(expectedEndTime, updatedSerie.lastEventEndTime?.toDate()?.time)
  }

  @Test
  fun createEventForSerie_withExistingEvents_calculatesCorrectDate() = runTest {
    // Create a serie with one existing event
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val serieDate = Timestamp(calendar.time)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Soccer League",
            description = "Weekly soccer",
            date = serieDate,
            participants = listOf("owner-1"),
            maxParticipants = 20,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event-1"),
            ownerId = "owner-1")

    serieRepo.addSerie(serie)

    // Add an existing event that lasts 90 minutes
    val existingEvent =
        Event(
            eventId = "event-1",
            type = EventType.SPORTS,
            title = "Match 1",
            description = "First match",
            location = Location(latitude = 1.0, longitude = 1.0, name = "Stadium"),
            date = serieDate,
            duration = 90,
            participants = emptyList(),
            maxParticipants = 20,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner-1")

    eventRepo.added.add(existingEvent)

    // Fill form for second event
    vm.setType("ACTIVITY")
    vm.setTitle("Match 2")
    vm.setDescription("Second match")
    vm.setDuration("60")
    vm.selectLocation(Location(latitude = 2.0, longitude = 2.0, name = "Stadium 2"))

    assertTrue(vm.uiState.value.isValid)

    val ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()

    assertTrue(ok)
    assertEquals(2, eventRepo.added.size)

    val newEvent = eventRepo.added.last()
    // New event should start when the previous event ends (serieDate + 90 minutes)
    val expectedStartTime = serieDate.toDate().time + (90 * 60 * 1000)
    assertEquals(expectedStartTime, newEvent.date.toDate().time)
    assertEquals(EventType.ACTIVITY, newEvent.type)
    assertEquals("Match 2", newEvent.title)
    assertEquals(60, newEvent.duration)

    // Verify serie was updated
    val updatedSerie = serieRepo.getSerie("serie-1")
    assertEquals(2, updatedSerie.eventIds.size)
    assertTrue(updatedSerie.eventIds.contains("event-1"))
    assertTrue(updatedSerie.eventIds.contains(newEvent.eventId))

    // Verify lastEventEndTime was updated to the end of the new (second) event
    val expectedEndTime = newEvent.date.toDate().time + (newEvent.duration * 60 * 1000)
    assertEquals(expectedEndTime, updatedSerie.lastEventEndTime?.toDate()?.time)
  }

  @Test
  fun createEventForSerie_inheritsVisibilityPrivate() = runTest {
    // Create a PRIVATE serie
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Private League",
            description = "Private games",
            date = Timestamp(calendar.time),
            participants = listOf("owner-1"),
            maxParticipants = 10,
            visibility = Visibility.PRIVATE,
            eventIds = emptyList(),
            ownerId = "owner-1")

    serieRepo.addSerie(serie)

    // Fill form
    vm.setType("SOCIAL")
    vm.setTitle("Private Match")
    vm.setDescription("Private event")
    vm.setDuration("120")
    vm.selectLocation(Location(latitude = 1.0, longitude = 1.0, name = "Private Venue"))

    val ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()

    assertTrue(ok)
    val event = eventRepo.added.first()
    assertEquals(EventVisibility.PRIVATE, event.visibility)
  }

  @Test
  fun createEventForSerie_setsLoadingState() = runTest {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(calendar.time),
            participants = listOf("owner-1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = emptyList(),
            ownerId = "owner-1")

    serieRepo.addSerie(serie)

    vm.setType("SPORTS")
    vm.setTitle("Test Event")
    vm.setDescription("Test description")
    vm.setDuration("60")
    vm.selectLocation(Location(latitude = 1.0, longitude = 1.0, name = "Test Location"))

    assertFalse(vm.uiState.value.isLoading)

    val ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()

    assertTrue(ok)
    assertFalse(vm.uiState.value.isLoading)
  }

  // ---------- Error clearing ----------

  @Test
  fun clearErrorMsg_resetsErrorField() = runTest {
    val ok = vm.createEventForSerie("nonexistent")
    advanceUntilIdle()

    assertFalse(ok)
    assertNotNull(vm.uiState.value.errorMsg)

    vm.clearErrorMsg()
    assertNull(vm.uiState.value.errorMsg)
  }

  // ---------- Repository error handling ----------

  @Test
  fun createEventForSerie_withRepositoryError_returnsFalse() = runTest {
    val errorEventRepo =
        object : EventsRepository {
          override suspend fun addEvent(event: Event) {
            throw RuntimeException("Network error")
          }

          override suspend fun editEvent(eventId: String, newValue: Event) {}

          override suspend fun deleteEvent(eventId: String) {}

          override suspend fun getEventsByIds(eventIds: List<String>): List<Event> = emptyList()

          override suspend fun getCommonEvents(userIds: List<String>): List<Event> = emptyList()

          override suspend fun getEvent(eventId: String): Event {
            throw NoSuchElementException()
          }

          override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> = emptyList()

          override fun getNewEventId(): String = "fake-id"
        }

    val errorVm = CreateEventForSerieViewModel(errorEventRepo, serieRepo, groupRepo, locationRepo)

    // Add a serie
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(calendar.time),
            participants = listOf("owner-1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = emptyList(),
            ownerId = "owner-1")

    serieRepo.addSerie(serie)

    // Fill form
    errorVm.setType("SPORTS")
    errorVm.setTitle("Test Event")
    errorVm.setDescription("Test description")
    errorVm.setDuration("60")
    errorVm.selectLocation(Location(latitude = 1.0, longitude = 1.0, name = "Test Location"))

    assertTrue(errorVm.uiState.value.isValid)

    val ok = errorVm.createEventForSerie("serie-1")
    advanceUntilIdle()

    assertFalse(ok)
    assertNotNull(errorVm.uiState.value.errorMsg)
    assertFalse(errorVm.uiState.value.isLoading)
  }

  // ---------- Multiple events in sequence ----------

  @Test
  fun createEventForSerie_multipleEvents_calculatesCorrectSequentialDates() = runTest {
    // Create a serie
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val serieDate = Timestamp(calendar.time)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Tournament",
            description = "Multi-game tournament",
            date = serieDate,
            participants = listOf("owner-1"),
            maxParticipants = 20,
            visibility = Visibility.PUBLIC,
            eventIds = emptyList(),
            ownerId = "owner-1")

    serieRepo.addSerie(serie)

    // Create first event (90 minutes)
    vm.setType("SPORTS")
    vm.setTitle("Game 1")
    vm.setDescription("First game")
    vm.setDuration("90")
    vm.selectLocation(Location(latitude = 1.0, longitude = 1.0, name = "Venue 1"))

    var ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()
    assertTrue(ok)

    val event1 = eventRepo.added.last()
    assertEquals(serieDate.toDate().time, event1.date.toDate().time)

    // Create second event (60 minutes)
    vm.setType("ACTIVITY")
    vm.setTitle("Game 2")
    vm.setDescription("Second game")
    vm.setDuration("60")
    vm.selectLocation(Location(latitude = 2.0, longitude = 2.0, name = "Venue 2"))

    ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()
    assertTrue(ok)

    val event2 = eventRepo.added.last()
    val expectedDate2 = serieDate.toDate().time + (90 * 60 * 1000)
    assertEquals(expectedDate2, event2.date.toDate().time)

    // Create third event (120 minutes)
    vm.setType("SOCIAL")
    vm.setTitle("Game 3")
    vm.setDescription("Third game")
    vm.setDuration("120")
    vm.selectLocation(Location(latitude = 3.0, longitude = 3.0, name = "Venue 3"))

    ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()
    assertTrue(ok)

    val event3 = eventRepo.added.last()
    val expectedDate3 = expectedDate2 + (60 * 60 * 1000)
    assertEquals(expectedDate3, event3.date.toDate().time)

    // Verify serie has all three events
    val updatedSerie = serieRepo.getSerie("serie-1")
    assertEquals(3, updatedSerie.eventIds.size)

    // Verify lastEventEndTime was updated to the end of the third (last) event
    val expectedEndTime = event3.date.toDate().time + (event3.duration * 60 * 1000)
    assertEquals(expectedEndTime, updatedSerie.lastEventEndTime?.toDate()?.time)
  }

  @Test
  fun createEventForSerie_updatesLastEventEndTime() = runTest {
    // Create a serie with initial lastEventEndTime
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val serieDate = Timestamp(calendar.time)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Test Serie",
            description = "Test",
            date = serieDate,
            participants = listOf("owner-1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = emptyList(),
            ownerId = "owner-1",
            lastEventEndTime = serieDate) // Initially equals serie date

    serieRepo.addSerie(serie)

    // Verify initial lastEventEndTime
    assertEquals(serieDate.toDate().time, serie.lastEventEndTime?.toDate()?.time)

    // Create an event with 120 minute duration
    vm.setType("SPORTS")
    vm.setTitle("Long Event")
    vm.setDescription("A long event")
    vm.setDuration("120")
    vm.selectLocation(Location(latitude = 1.0, longitude = 1.0, name = "Stadium"))

    val ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()

    assertTrue(ok)

    // Verify lastEventEndTime was updated
    val updatedSerie = serieRepo.getSerie("serie-1")
    val event = eventRepo.added.first()
    val expectedEndTime = event.date.toDate().time + (120 * 60 * 1000)

    assertEquals(expectedEndTime, updatedSerie.lastEventEndTime?.toDate()?.time)
    // Verify it's different from the initial serie date
    assertTrue(updatedSerie.lastEventEndTime!!.toDate().time > serieDate.toDate().time)
  }

  // ---------- Group-related tests ----------

  @Test
  fun createEventForSerie_withGroupSerie_autoFillsTypeFromGroup_inheritsGroupId() = runTest {
    // Setup: Create a group with SPORTS category
    val sportsGroup =
        Group(
            id = "group-1",
            name = "Basketball Club",
            category = EventType.SPORTS,
            description = "Weekly basketball games",
            ownerId = "owner-1",
            memberIds = listOf("owner-1", "user-2", "user-3"))
    groupRepo.groups.add(sportsGroup)

    // Create a serie with the group
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val serieDate = Timestamp(calendar.time)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Basketball Tournament",
            description = "Weekly games",
            date = serieDate,
            participants = listOf("owner-1", "user-2", "user-3"),
            maxParticipants = 300,
            visibility = Visibility.PRIVATE,
            eventIds = emptyList(),
            ownerId = "owner-1",
            groupId = "group-1")

    serieRepo.addSerie(serie)

    // Fill form WITHOUT setting type (should be auto-determined from group)
    vm.setTitle("Game 1")
    vm.setDescription("First basketball game")
    vm.setDuration("90")
    vm.selectLocation(Location(latitude = 1.0, longitude = 1.0, name = "Court 1"))

    // Type field can be left empty or set - it will be overridden by group category
    vm.setType("ACTIVITY") // This will be ignored

    val ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()

    assertTrue(ok)
    assertEquals(1, eventRepo.added.size)

    val event = eventRepo.added.first()
    // Verify event type is from group (SPORTS), not what we set (ACTIVITY)
    assertEquals(EventType.SPORTS, event.type)
    // Verify groupId is inherited from serie
    assertEquals("group-1", event.groupId)
    // Verify other inherited properties
    assertEquals(300, event.maxParticipants)
    assertEquals(EventVisibility.PRIVATE, event.visibility)
    assertEquals("owner-1", event.ownerId)
  }

  @Test
  fun createEventForSerie_withStandaloneSerie_usesUserSelectedType_noGroupId() = runTest {
    // Create a standalone serie (no group)
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val serieDate = Timestamp(calendar.time)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Standalone Serie",
            description = "No group",
            date = serieDate,
            participants = listOf("owner-1"),
            maxParticipants = 20,
            visibility = Visibility.PUBLIC,
            eventIds = emptyList(),
            ownerId = "owner-1",
            groupId = null) // No group

    serieRepo.addSerie(serie)

    // Fill form WITH type selection (required for standalone)
    vm.setType("SOCIAL")
    vm.setTitle("Social Event")
    vm.setDescription("A social gathering")
    vm.setDuration("120")
    vm.selectLocation(Location(latitude = 1.0, longitude = 1.0, name = "Bar"))

    assertTrue(vm.uiState.value.isValid)

    val ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()

    assertTrue(ok)
    val event = eventRepo.added.first()
    // Verify event type is the user's selection
    assertEquals(EventType.SOCIAL, event.type)
    // Verify no groupId
    assertNull(event.groupId)
    assertEquals(20, event.maxParticipants)
    assertEquals(EventVisibility.PUBLIC, event.visibility)
  }

  @Test
  fun createEventForSerie_withMissingGroup_failsGracefully() = runTest {
    // Create a serie with a groupId that doesn't exist in the repository
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Orphaned Serie",
            description = "Group doesn't exist",
            date = Timestamp(calendar.time),
            participants = listOf("owner-1"),
            maxParticipants = 20,
            visibility = Visibility.PUBLIC,
            eventIds = emptyList(),
            ownerId = "owner-1",
            groupId = "nonexistent-group") // Group doesn't exist

    serieRepo.addSerie(serie)

    vm.setType("SPORTS")
    vm.setTitle("Test Event")
    vm.setDescription("Test description")
    vm.setDuration("60")
    vm.selectLocation(Location(1.0, 1.0, "Test Location"))

    assertTrue(vm.uiState.value.isValid)

    val ok = vm.createEventForSerie("serie-1")
    advanceUntilIdle()

    // Should fail with error message
    assertFalse(ok)
    assertNotNull(vm.uiState.value.errorMsg)
    assertTrue(vm.uiState.value.errorMsg!!.contains("Failed to determine event type"))
    assertTrue(eventRepo.added.isEmpty())
  }

  // ---------- loadSerie() tests ----------

  @Test
  fun loadSerie_withGroupSerie_setsSerieHasGroupTrue_autoFillsTypeFromGroup() = runTest {
    // Setup: Create a group with SOCIAL category
    val socialGroup =
        Group(
            id = "group-1",
            name = "Social Club",
            category = EventType.SOCIAL,
            description = "Social gatherings",
            ownerId = "owner-1",
            memberIds = listOf("owner-1", "user-2"))
    groupRepo.groups.add(socialGroup)

    // Create a serie with the group
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val serieDate = Timestamp(calendar.time)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Social Serie",
            description = "Weekly social events",
            date = serieDate,
            participants = listOf("owner-1", "user-2"),
            maxParticipants = 300,
            visibility = Visibility.PRIVATE,
            eventIds = emptyList(),
            ownerId = "owner-1",
            groupId = "group-1")

    serieRepo.addSerie(serie)

    // Initially, serieHasGroup should be false
    assertFalse(vm.uiState.value.serieHasGroup)
    assertEquals("", vm.uiState.value.type)

    // Load the serie
    vm.loadSerie("serie-1")
    advanceUntilIdle()

    // Verify serieHasGroup is set to true and type is auto-filled
    val state = vm.uiState.value
    assertTrue(state.serieHasGroup)
    assertEquals("SOCIAL", state.type)
    assertNull(state.invalidTypeMsg)
  }

  @Test
  fun loadSerie_withStandaloneSerie_setsSerieHasGroupFalse_doesNotFillType() = runTest {
    // Create a standalone serie (no group)
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val serieDate = Timestamp(calendar.time)

    val serie =
        Serie(
            serieId = "serie-1",
            title = "Standalone Serie",
            description = "No group",
            date = serieDate,
            participants = listOf("owner-1"),
            maxParticipants = 20,
            visibility = Visibility.PUBLIC,
            eventIds = emptyList(),
            ownerId = "owner-1",
            groupId = null) // No group

    serieRepo.addSerie(serie)

    // Initially, serieHasGroup should be false
    assertFalse(vm.uiState.value.serieHasGroup)

    // Load the serie
    vm.loadSerie("serie-1")
    advanceUntilIdle()

    // Verify serieHasGroup remains false and type is not auto-filled
    val state = vm.uiState.value
    assertFalse(state.serieHasGroup)
    assertEquals("", state.type) // Type should remain empty, user must select
  }

  @Test
  fun loadSerie_withNonexistentSerie_setsErrorMsg() = runTest {
    // Try to load a serie that doesn't exist
    vm.loadSerie("nonexistent-serie")
    advanceUntilIdle()

    // Verify error message is set
    assertNotNull(vm.uiState.value.errorMsg)
    assertTrue(vm.uiState.value.errorMsg!!.contains("Failed to load serie"))
  }

  @Test
  fun loadSerie_withDifferentGroupCategories_setsCorrectType() = runTest {
    // Test with ACTIVITY group
    val activityGroup =
        Group(
            id = "activity-group",
            name = "Activity Group",
            category = EventType.ACTIVITY,
            description = "Various activities",
            ownerId = "owner-1",
            memberIds = listOf("owner-1"))
    groupRepo.groups.add(activityGroup)

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 7)

    val activitySerie =
        Serie(
            serieId = "activity-serie",
            title = "Activity Serie",
            description = "Weekly activities",
            date = Timestamp(calendar.time),
            participants = listOf("owner-1"),
            maxParticipants = 300,
            visibility = Visibility.PRIVATE,
            eventIds = emptyList(),
            ownerId = "owner-1",
            groupId = "activity-group")

    serieRepo.addSerie(activitySerie)

    vm.loadSerie("activity-serie")
    advanceUntilIdle()

    assertEquals("ACTIVITY", vm.uiState.value.type)
    assertTrue(vm.uiState.value.serieHasGroup)

    // Test with SPORTS group
    val sportsGroup =
        Group(
            id = "sports-group",
            name = "Sports Group",
            category = EventType.SPORTS,
            description = "Sports events",
            ownerId = "owner-1",
            memberIds = listOf("owner-1"))
    groupRepo.groups.add(sportsGroup)

    val sportsSerie =
        Serie(
            serieId = "sports-serie",
            title = "Sports Serie",
            description = "Weekly sports",
            date = Timestamp(calendar.time),
            participants = listOf("owner-1"),
            maxParticipants = 300,
            visibility = Visibility.PRIVATE,
            eventIds = emptyList(),
            ownerId = "owner-1",
            groupId = "sports-group")

    serieRepo.addSerie(sportsSerie)

    // Create new VM to reset state
    val newVm = CreateEventForSerieViewModel(eventRepo, serieRepo, groupRepo, locationRepo)
    newVm.loadSerie("sports-serie")
    advanceUntilIdle()

    assertEquals("SPORTS", newVm.uiState.value.type)
    assertTrue(newVm.uiState.value.serieHasGroup)
  }
}
