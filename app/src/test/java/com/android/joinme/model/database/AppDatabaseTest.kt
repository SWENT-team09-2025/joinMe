package com.android.joinme.model.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {

  private lateinit var database: AppDatabase
  private lateinit var eventDao: EventDao
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    // Create an in-memory database for testing
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    eventDao = database.eventDao()
  }

  @After
  fun tearDown() {
    database.close()
    // Reset singleton instance for next test
    AppDatabase.setTestInstance(null)
  }

  @Test
  fun `database returns EventDao instance`() {
    assertNotNull(eventDao)
  }

  @Test
  fun `getDatabase returns singleton instance`() {
    val db1 = AppDatabase.getDatabase(context)
    val db2 = AppDatabase.getDatabase(context)

    assertSame(db1, db2)
  }

  @Test
  fun `getDatabase creates new instance if not exists`() {
    AppDatabase.setTestInstance(null)
    val db = AppDatabase.getDatabase(context)

    assertNotNull(db)
  }

  @Test
  fun `setTestInstance allows setting custom database`() {
    val customDb =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    AppDatabase.setTestInstance(customDb)
    val retrievedDb = AppDatabase.getDatabase(context)

    assertSame(customDb, retrievedDb)

    customDb.close()
  }

  @Test
  fun `setTestInstance can reset to null`() {
    AppDatabase.setTestInstance(database)
    AppDatabase.setTestInstance(null)

    // Next call should create a new instance
    val newDb = AppDatabase.getDatabase(context)
    assertNotNull(newDb)
    newDb.close()
  }

  @Test
  fun `insertEvent stores event in database`() = runBlocking {
    val event =
        EventEntity(
            eventId = "test1",
            type = "SPORTS",
            title = "Test Event",
            description = "Test Description",
            locationLatitude = 46.5,
            locationLongitude = 6.5,
            locationName = "Test Location",
            dateSeconds = Timestamp(Date()).seconds,
            dateNanoseconds = 0,
            duration = 60,
            participantsJson = "[\"user1\"]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            ownerId = "owner1",
            partOfASerie = false,
            groupId = null)

    eventDao.insertEvent(event)
    val retrieved = eventDao.getEventById("test1")

    assertNotNull(retrieved)
    assertEquals("test1", retrieved?.eventId)
    assertEquals("Test Event", retrieved?.title)
  }

  @Test
  fun `getEventById returns null for non-existent event`() = runBlocking {
    val retrieved = eventDao.getEventById("nonexistent")

    assertNull(retrieved)
  }

  @Test
  fun `getAllEvents returns all events`() = runBlocking {
    val event1 =
        EventEntity(
            eventId = "1",
            type = "SPORTS",
            title = "Event 1",
            description = "Desc 1",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 100,
            dateNanoseconds = 0,
            duration = 60,
            participantsJson = "[]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            ownerId = "owner1",
            partOfASerie = false,
            groupId = null)

    val event2 =
        EventEntity(
            eventId = "2",
            type = "ACTIVITY",
            title = "Event 2",
            description = "Desc 2",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 200,
            dateNanoseconds = 0,
            duration = 90,
            participantsJson = "[]",
            maxParticipants = 5,
            visibility = "PRIVATE",
            ownerId = "owner2",
            partOfASerie = false,
            groupId = null)

    eventDao.insertEvent(event1)
    eventDao.insertEvent(event2)

    val allEvents = eventDao.getAllEvents()

    assertEquals(2, allEvents.size)
    assertTrue(allEvents.any { it.eventId == "1" })
    assertTrue(allEvents.any { it.eventId == "2" })
  }

  @Test
  fun `getEventsByOwner returns only owner's events`() = runBlocking {
    val event1 =
        EventEntity(
            eventId = "1",
            type = "SPORTS",
            title = "Event 1",
            description = "Desc 1",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 100,
            dateNanoseconds = 0,
            duration = 60,
            participantsJson = "[]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            ownerId = "owner1",
            partOfASerie = false,
            groupId = null)

    val event2 =
        EventEntity(
            eventId = "2",
            type = "ACTIVITY",
            title = "Event 2",
            description = "Desc 2",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 200,
            dateNanoseconds = 0,
            duration = 90,
            participantsJson = "[]",
            maxParticipants = 5,
            visibility = "PRIVATE",
            ownerId = "owner2",
            partOfASerie = false,
            groupId = null)

    eventDao.insertEvent(event1)
    eventDao.insertEvent(event2)

    val owner1Events = eventDao.getEventsByOwner("owner1")

    assertEquals(1, owner1Events.size)
    assertEquals("1", owner1Events[0].eventId)
  }

  @Test
  fun `getPublicEvents returns only public events sorted by date`() = runBlocking {
    val publicEvent1 =
        EventEntity(
            eventId = "pub1",
            type = "SPORTS",
            title = "Public Event 1",
            description = "Desc",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 100,
            dateNanoseconds = 0,
            duration = 60,
            participantsJson = "[]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            ownerId = "owner1",
            partOfASerie = false,
            groupId = null)

    val publicEvent2 =
        EventEntity(
            eventId = "pub2",
            type = "ACTIVITY",
            title = "Public Event 2",
            description = "Desc",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 200,
            dateNanoseconds = 0,
            duration = 90,
            participantsJson = "[]",
            maxParticipants = 5,
            visibility = "PUBLIC",
            ownerId = "owner2",
            partOfASerie = false,
            groupId = null)

    val privateEvent =
        EventEntity(
            eventId = "priv1",
            type = "SOCIAL",
            title = "Private Event",
            description = "Desc",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 150,
            dateNanoseconds = 0,
            duration = 120,
            participantsJson = "[]",
            maxParticipants = 3,
            visibility = "PRIVATE",
            ownerId = "owner3",
            partOfASerie = false,
            groupId = null)

    eventDao.insertEvent(publicEvent1)
    eventDao.insertEvent(publicEvent2)
    eventDao.insertEvent(privateEvent)

    val publicEvents = eventDao.getPublicEvents(10)

    assertEquals(2, publicEvents.size)
    // Should be sorted by date descending
    assertEquals("pub2", publicEvents[0].eventId)
    assertEquals("pub1", publicEvents[1].eventId)
  }

  @Test
  fun `getPublicEvents respects limit parameter`() = runBlocking {
    repeat(5) { i ->
      eventDao.insertEvent(
          EventEntity(
              eventId = "pub$i",
              type = "SPORTS",
              title = "Event $i",
              description = "Desc",
              locationLatitude = null,
              locationLongitude = null,
              locationName = null,
              dateSeconds = i.toLong() * 100,
              dateNanoseconds = 0,
              duration = 60,
              participantsJson = "[]",
              maxParticipants = 10,
              visibility = "PUBLIC",
              ownerId = "owner1",
              partOfASerie = false,
              groupId = null))
    }

    val limitedEvents = eventDao.getPublicEvents(3)

    assertEquals(3, limitedEvents.size)
  }

  @Test
  fun `getUpcomingEvents returns events after start time`() = runBlocking {
    val currentTime = System.currentTimeMillis() / 1000

    val pastEvent =
        EventEntity(
            eventId = "past",
            type = "SPORTS",
            title = "Past Event",
            description = "Desc",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = currentTime - 1000,
            dateNanoseconds = 0,
            duration = 60,
            participantsJson = "[]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            ownerId = "owner1",
            partOfASerie = false,
            groupId = null)

    val futureEvent =
        EventEntity(
            eventId = "future",
            type = "ACTIVITY",
            title = "Future Event",
            description = "Desc",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = currentTime + 1000,
            dateNanoseconds = 0,
            duration = 90,
            participantsJson = "[]",
            maxParticipants = 5,
            visibility = "PUBLIC",
            ownerId = "owner2",
            partOfASerie = false,
            groupId = null)

    eventDao.insertEvent(pastEvent)
    eventDao.insertEvent(futureEvent)

    val upcomingEvents = eventDao.getUpcomingEvents(currentTime, 10)

    assertEquals(1, upcomingEvents.size)
    assertEquals("future", upcomingEvents[0].eventId)
  }

  @Test
  fun `insertEvent with REPLACE strategy updates existing event`() = runBlocking {
    val event =
        EventEntity(
            eventId = "test1",
            type = "SPORTS",
            title = "Original Title",
            description = "Desc",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 100,
            dateNanoseconds = 0,
            duration = 60,
            participantsJson = "[]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            ownerId = "owner1",
            partOfASerie = false,
            groupId = null)

    eventDao.insertEvent(event)

    val updatedEvent = event.copy(title = "Updated Title")
    eventDao.insertEvent(updatedEvent)

    val retrieved = eventDao.getEventById("test1")

    assertEquals("Updated Title", retrieved?.title)
    // Should only be one event
    assertEquals(1, eventDao.getAllEvents().size)
  }

  @Test
  fun `insertEvents stores multiple events`() = runBlocking {
    val events =
        listOf(
            EventEntity(
                eventId = "1",
                type = "SPORTS",
                title = "Event 1",
                description = "Desc",
                locationLatitude = null,
                locationLongitude = null,
                locationName = null,
                dateSeconds = 100,
                dateNanoseconds = 0,
                duration = 60,
                participantsJson = "[]",
                maxParticipants = 10,
                visibility = "PUBLIC",
                ownerId = "owner1",
                partOfASerie = false,
                groupId = null),
            EventEntity(
                eventId = "2",
                type = "ACTIVITY",
                title = "Event 2",
                description = "Desc",
                locationLatitude = null,
                locationLongitude = null,
                locationName = null,
                dateSeconds = 200,
                dateNanoseconds = 0,
                duration = 90,
                participantsJson = "[]",
                maxParticipants = 5,
                visibility = "PRIVATE",
                ownerId = "owner2",
                partOfASerie = false,
                groupId = null))

    eventDao.insertEvents(events)

    assertEquals(2, eventDao.getAllEvents().size)
  }

  @Test
  fun `deleteEvent removes event from database`() = runBlocking {
    val event =
        EventEntity(
            eventId = "delete_me",
            type = "SPORTS",
            title = "Event to Delete",
            description = "Desc",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 100,
            dateNanoseconds = 0,
            duration = 60,
            participantsJson = "[]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            ownerId = "owner1",
            partOfASerie = false,
            groupId = null)

    eventDao.insertEvent(event)
    assertEquals(1, eventDao.getAllEvents().size)

    eventDao.deleteEvent("delete_me")

    assertNull(eventDao.getEventById("delete_me"))
    assertEquals(0, eventDao.getAllEvents().size)
  }

  @Test
  fun `deleteAllEvents removes all events`() = runBlocking {
    repeat(3) { i ->
      eventDao.insertEvent(
          EventEntity(
              eventId = "event$i",
              type = "SPORTS",
              title = "Event $i",
              description = "Desc",
              locationLatitude = null,
              locationLongitude = null,
              locationName = null,
              dateSeconds = i.toLong() * 100,
              dateNanoseconds = 0,
              duration = 60,
              participantsJson = "[]",
              maxParticipants = 10,
              visibility = "PUBLIC",
              ownerId = "owner1",
              partOfASerie = false,
              groupId = null))
    }

    assertEquals(3, eventDao.getAllEvents().size)

    eventDao.deleteAllEvents()

    assertEquals(0, eventDao.getAllEvents().size)
  }

  @Test
  fun `deleteOldEvents removes events older than timestamp`() = runBlocking {
    val currentTime = System.currentTimeMillis()

    val oldEvent =
        EventEntity(
            eventId = "old",
            type = "SPORTS",
            title = "Old Event",
            description = "Desc",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 100,
            dateNanoseconds = 0,
            duration = 60,
            participantsJson = "[]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            ownerId = "owner1",
            partOfASerie = false,
            groupId = null,
            cachedAt = currentTime - 10000)

    val recentEvent =
        EventEntity(
            eventId = "recent",
            type = "ACTIVITY",
            title = "Recent Event",
            description = "Desc",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = 200,
            dateNanoseconds = 0,
            duration = 90,
            participantsJson = "[]",
            maxParticipants = 5,
            visibility = "PUBLIC",
            ownerId = "owner2",
            partOfASerie = false,
            groupId = null,
            cachedAt = currentTime)

    eventDao.insertEvent(oldEvent)
    eventDao.insertEvent(recentEvent)

    eventDao.deleteOldEvents(currentTime - 5000)

    val remaining = eventDao.getAllEvents()
    assertEquals(1, remaining.size)
    assertEquals("recent", remaining[0].eventId)
  }
}
