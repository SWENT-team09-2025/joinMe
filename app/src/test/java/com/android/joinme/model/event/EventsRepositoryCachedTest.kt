package com.android.joinme.model.event

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.android.joinme.model.database.AppDatabase
import com.android.joinme.model.database.toEntity
import com.android.joinme.model.map.Location
import com.android.joinme.network.NetworkMonitor
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventsRepositoryCachedTest {

  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var firestoreRepo: EventsRepository
  private lateinit var networkMonitor: NetworkMonitor
  private lateinit var cachedRepo: EventsRepositoryCached
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser

  private val sampleLocation = Location(46.5191, 6.5668, "EPFL")
  private val currentTimestamp = Timestamp(Date())

  private fun createEvent(
      id: String,
      title: String = "Event $id",
      type: EventType = EventType.SPORTS,
      visibility: EventVisibility = EventVisibility.PUBLIC,
      ownerId: String = "owner1",
      participants: List<String> = emptyList(),
      date: Timestamp = currentTimestamp,
      duration: Int = 60,
      location: Location? = sampleLocation
  ): Event {
    return Event(
        eventId = id,
        type = type,
        title = title,
        description = "Description for $title",
        location = location,
        date = date,
        duration = duration,
        participants = participants,
        maxParticipants = 10,
        visibility = visibility,
        ownerId = ownerId)
  }

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    AppDatabase.setTestInstance(database)

    firestoreRepo = mockk(relaxed = true)
    networkMonitor = mockk(relaxed = true)

    // Mock Firebase Auth
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "testUser"

    cachedRepo = EventsRepositoryCached(context, firestoreRepo, networkMonitor)
  }

  @After
  fun tearDown() {
    database.close()
    AppDatabase.setTestInstance(null)
    unmockkAll()
  }

  // ========== getNewEventId Tests ==========

  @Test
  fun `getNewEventId delegates to firestore repository`() = runBlocking {
    every { firestoreRepo.getNewEventId() } returns "newId123"

    val result = cachedRepo.getNewEventId()

    assertEquals("newId123", result)
    verify { firestoreRepo.getNewEventId() }
  }

  // ========== getAllEvents Tests ==========

  @Test
  fun `getAllEvents fetches from firestore when online`() = runBlocking {
    val events = listOf(createEvent("1"), createEvent("2"))
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getAllEvents(any()) } returns events

    val result = cachedRepo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)

    assertEquals(2, result.size)
    coVerify { firestoreRepo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN) }
  }

  @Test
  fun `getAllEvents caches events from firestore`() = runBlocking {
    val events = listOf(createEvent("1", participants = listOf("testUser")))
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getAllEvents(any()) } returns events

    cachedRepo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)

    // Verify event was cached
    val cached = database.eventDao().getEventById("1")
    assertNotNull(cached)
    assertEquals("Event 1", cached?.title)
  }

  @Test
  fun `getAllEvents returns cached data when offline`() = runBlocking {
    val event = createEvent("1", participants = listOf("testUser"))
    database.eventDao().insertEvent(event.toEntity())

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].eventId)
  }

  @Test
  fun `getAllEvents applies OVERVIEW filter correctly when offline`() = runBlocking {
    val event1 = createEvent("1", participants = listOf("testUser", "other"))
    val event2 = createEvent("2", participants = listOf("other"))
    database.eventDao().insertEvents(listOf(event1.toEntity(), event2.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].eventId)
  }

  @Test
  fun `getAllEvents applies HISTORY filter correctly when offline`() = runBlocking {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -3)
    val pastDate = Timestamp(calendar.time)

    val expiredEvent =
        createEvent("1", participants = listOf("testUser"), date = pastDate, duration = 60)
    val futureEvent = createEvent("2", participants = listOf("testUser"))

    database.eventDao().insertEvents(listOf(expiredEvent.toEntity(), futureEvent.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getAllEvents(EventFilter.EVENTS_FOR_HISTORY_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].eventId)
  }

  @Test
  fun `getAllEvents applies SEARCH filter correctly when offline`() = runBlocking {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2)
    val futureDate = Timestamp(calendar.time)

    val publicUpcomingEvent =
        createEvent(
            "1",
            visibility = EventVisibility.PUBLIC,
            date = futureDate,
            participants = listOf("other"))
    val privateEvent =
        createEvent(
            "2",
            visibility = EventVisibility.PRIVATE,
            date = futureDate,
            participants = listOf("other"))
    val userParticipatingEvent =
        createEvent(
            "3",
            visibility = EventVisibility.PUBLIC,
            date = futureDate,
            participants = listOf("testUser"))

    database
        .eventDao()
        .insertEvents(
            listOf(
                publicUpcomingEvent.toEntity(),
                privateEvent.toEntity(),
                userParticipatingEvent.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getAllEvents(EventFilter.EVENTS_FOR_SEARCH_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].eventId)
  }

  @Test
  fun `getAllEvents applies MAP filter correctly when offline`() = runBlocking {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2)
    val futureDate = Timestamp(calendar.time)

    val upcomingEventWithLocation = createEvent("1", date = futureDate, location = sampleLocation)
    val upcomingEventWithoutLocation = createEvent("2", date = futureDate, location = null)

    database
        .eventDao()
        .insertEvents(
            listOf(upcomingEventWithLocation.toEntity(), upcomingEventWithoutLocation.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getAllEvents(EventFilter.EVENTS_FOR_MAP_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].eventId)
  }

  @Test
  fun `getAllEvents falls back to cache when firestore throws exception`() = runBlocking {
    val cachedEvent = createEvent("1", participants = listOf("testUser"))
    database.eventDao().insertEvent(cachedEvent.toEntity())

    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getAllEvents(any()) } throws Exception("Network error")

    val result = cachedRepo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)

    assertEquals(1, result.size)
    assertEquals("1", result[0].eventId)
  }

  @Test
  fun `getAllEvents returns empty list when offline and no auth user`() = runBlocking {
    every { networkMonitor.isOnline() } returns false
    every { mockAuth.currentUser } returns null

    val result = cachedRepo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)

    assertTrue(result.isEmpty())
  }

  @Test
  fun `getAllEvents does not cache empty list from firestore`() = runBlocking {
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getAllEvents(any()) } returns emptyList()

    val result = cachedRepo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)

    assertTrue(result.isEmpty())
    assertEquals(0, database.eventDao().getAllEvents().size)
  }

  // ========== getEvent Tests ==========

  @Test
  fun `getEvent fetches from firestore when online and caches result`() = runBlocking {
    val event = createEvent("1")
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getEvent("1") } returns event

    val result = cachedRepo.getEvent("1")

    assertEquals("1", result.eventId)
    assertNotNull(database.eventDao().getEventById("1"))
    coVerify { firestoreRepo.getEvent("1") }
  }

  @Test
  fun `getEvent returns cached version when offline`() = runBlocking {
    val event = createEvent("1")
    database.eventDao().insertEvent(event.toEntity())

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getEvent("1")

    assertEquals("1", result.eventId)
    coVerify(exactly = 0) { firestoreRepo.getEvent(any()) }
  }

  @Test
  fun `getEvent throws OfflineException when offline and no cache`() = runBlocking {
    every { networkMonitor.isOnline() } returns false

    try {
      cachedRepo.getEvent("nonexistent")
      fail("Should have thrown OfflineException")
    } catch (e: OfflineException) {
      assertTrue(e.message!!.contains("offline"))
    }
  }

  @Test
  fun `getEvent falls back to cache when firestore fails`() = runBlocking {
    val event = createEvent("1")
    database.eventDao().insertEvent(event.toEntity())

    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getEvent("1") } throws Exception("Network error")

    val result = cachedRepo.getEvent("1")

    assertEquals("1", result.eventId)
  }

  @Test
  fun `getEvent throws exception when firestore fails and no cache`() = runBlocking {
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getEvent("1") } throws Exception("Network error")

    try {
      cachedRepo.getEvent("1")
      fail("Should have thrown exception")
    } catch (e: Exception) {
      assertEquals("Network error", e.message)
    }
  }

  // ========== addEvent Tests ==========

  @Test
  fun `addEvent adds to firestore and caches when online`() = runBlocking {
    val event = createEvent("1")
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.addEvent(event) } just Runs

    cachedRepo.addEvent(event)

    coVerify { firestoreRepo.addEvent(event) }
    assertNotNull(database.eventDao().getEventById("1"))
  }

  @Test
  fun `addEvent throws OfflineException when offline`() = runBlocking {
    val event = createEvent("1")
    every { networkMonitor.isOnline() } returns false

    try {
      cachedRepo.addEvent(event)
      fail("Should have thrown OfflineException")
    } catch (e: OfflineException) {
      assertTrue(e.message!!.contains("internet connection"))
    }

    coVerify(exactly = 0) { firestoreRepo.addEvent(any()) }
  }

  // ========== editEvent Tests ==========

  @Test
  fun `editEvent updates firestore and cache when online`() = runBlocking {
    val event = createEvent("1", title = "Updated Title")
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.editEvent("1", event) } just Runs

    cachedRepo.editEvent("1", event)

    coVerify { firestoreRepo.editEvent("1", event) }
    val cached = database.eventDao().getEventById("1")
    assertEquals("Updated Title", cached?.title)
  }

  @Test
  fun `editEvent throws OfflineException when offline`() = runBlocking {
    val event = createEvent("1")
    every { networkMonitor.isOnline() } returns false

    try {
      cachedRepo.editEvent("1", event)
      fail("Should have thrown OfflineException")
    } catch (e: OfflineException) {
      assertTrue(e.message!!.contains("internet connection"))
    }

    coVerify(exactly = 0) { firestoreRepo.editEvent(any(), any()) }
  }

  // ========== deleteEvent Tests ==========

  @Test
  fun `deleteEvent removes from firestore and cache when online`() = runBlocking {
    val event = createEvent("1")
    database.eventDao().insertEvent(event.toEntity())

    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.deleteEvent("1") } just Runs

    cachedRepo.deleteEvent("1")

    coVerify { firestoreRepo.deleteEvent("1") }
    assertNull(database.eventDao().getEventById("1"))
  }

  @Test
  fun `deleteEvent throws OfflineException when offline`() = runBlocking {
    every { networkMonitor.isOnline() } returns false

    try {
      cachedRepo.deleteEvent("1")
      fail("Should have thrown OfflineException")
    } catch (e: OfflineException) {
      assertTrue(e.message!!.contains("internet connection"))
    }

    coVerify(exactly = 0) { firestoreRepo.deleteEvent(any()) }
  }

  // ========== getEventsByIds Tests ==========

  @Test
  fun `getEventsByIds fetches from firestore when online and caches`() = runBlocking {
    val events = listOf(createEvent("1"), createEvent("2"))
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getEventsByIds(listOf("1", "2")) } returns events

    val result = cachedRepo.getEventsByIds(listOf("1", "2"))

    assertEquals(2, result.size)
    assertNotNull(database.eventDao().getEventById("1"))
    assertNotNull(database.eventDao().getEventById("2"))
  }

  @Test
  fun `getEventsByIds returns cached events when offline`() = runBlocking {
    val event1 = createEvent("1")
    val event2 = createEvent("2")
    database.eventDao().insertEvents(listOf(event1.toEntity(), event2.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getEventsByIds(listOf("1", "2"))

    assertEquals(2, result.size)
    coVerify(exactly = 0) { firestoreRepo.getEventsByIds(any()) }
  }

  @Test
  fun `getEventsByIds returns only cached events available when offline`() = runBlocking {
    val event1 = createEvent("1")
    database.eventDao().insertEvent(event1.toEntity())

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getEventsByIds(listOf("1", "2", "3"))

    assertEquals(1, result.size)
    assertEquals("1", result[0].eventId)
  }

  @Test
  fun `getEventsByIds falls back to cache when firestore fails`() = runBlocking {
    val event = createEvent("1")
    database.eventDao().insertEvent(event.toEntity())

    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getEventsByIds(any()) } throws Exception("Network error")

    val result = cachedRepo.getEventsByIds(listOf("1"))

    assertEquals(1, result.size)
    assertEquals("1", result[0].eventId)
  }

  // ========== getCommonEvents Tests ==========

  @Test
  fun `getCommonEvents fetches from firestore when online and caches`() = runBlocking {
    val events = listOf(createEvent("1", participants = listOf("user1", "user2")))
    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getCommonEvents(listOf("user1", "user2")) } returns events

    val result = cachedRepo.getCommonEvents(listOf("user1", "user2"))

    assertEquals(1, result.size)
    assertNotNull(database.eventDao().getEventById("1"))
  }

  @Test
  fun `getCommonEvents filters cached events when offline`() = runBlocking {
    val event1 = createEvent("1", participants = listOf("user1", "user2", "user3"))
    val event2 = createEvent("2", participants = listOf("user1", "user2"))
    val event3 = createEvent("3", participants = listOf("user1"))
    database
        .eventDao()
        .insertEvents(listOf(event1.toEntity(), event2.toEntity(), event3.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getCommonEvents(listOf("user1", "user2"))

    assertEquals(2, result.size)
    assertTrue(result.any { it.eventId == "1" })
    assertTrue(result.any { it.eventId == "2" })
  }

  @Test
  fun `getCommonEvents returns empty when no common events offline`() = runBlocking {
    val event1 = createEvent("1", participants = listOf("user1"))
    val event2 = createEvent("2", participants = listOf("user2"))
    database.eventDao().insertEvents(listOf(event1.toEntity(), event2.toEntity()))

    every { networkMonitor.isOnline() } returns false

    val result = cachedRepo.getCommonEvents(listOf("user1", "user2"))

    assertTrue(result.isEmpty())
  }

  @Test
  fun `getCommonEvents falls back to cache when firestore fails`() = runBlocking {
    val event = createEvent("1", participants = listOf("user1", "user2"))
    database.eventDao().insertEvent(event.toEntity())

    every { networkMonitor.isOnline() } returns true
    coEvery { firestoreRepo.getCommonEvents(any()) } throws Exception("Network error")

    val result = cachedRepo.getCommonEvents(listOf("user1", "user2"))

    assertEquals(1, result.size)
    assertEquals("1", result[0].eventId)
  }

  // ========== OfflineException Tests ==========

  @Test
  fun `OfflineException has correct message`() {
    val exception = OfflineException("Test message")

    assertEquals("Test message", exception.message)
  }

  @Test
  fun `OfflineException is an Exception`() {
    val exception = OfflineException("Test")

    assertTrue(exception is Exception)
  }
}
