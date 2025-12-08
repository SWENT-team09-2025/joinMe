package com.android.joinme.model.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
    database =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    eventDao = database.eventDao()
  }

  @After
  fun tearDown() {
    database.close()
    AppDatabase.setTestInstance(null)
  }

  private fun createTestEvent(
      id: String,
      ownerId: String = "owner1",
      visibility: String = "PUBLIC",
      dateSeconds: Long = 100,
      cachedAt: Long = System.currentTimeMillis()
  ) =
      EventEntity(
          eventId = id,
          type = "SPORTS",
          title = "Event $id",
          description = "Desc",
          locationLatitude = null,
          locationLongitude = null,
          locationName = null,
          dateSeconds = dateSeconds,
          dateNanoseconds = 0,
          duration = 60,
          participantsJson = "[]",
          maxParticipants = 10,
          visibility = visibility,
          ownerId = ownerId,
          partOfASerie = false,
          groupId = null,
          cachedAt = cachedAt)

  @Test
  fun `database singleton behavior works correctly`() {
    // Test singleton pattern
    val db1 = AppDatabase.getDatabase(context)
    val db2 = AppDatabase.getDatabase(context)
    assertSame(db1, db2)

    // Test reset and recreate
    AppDatabase.setTestInstance(null)
    val db3 = AppDatabase.getDatabase(context)
    assertNotNull(db3)

    // Test custom instance
    val customDb =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    AppDatabase.setTestInstance(customDb)
    assertSame(customDb, AppDatabase.getDatabase(context))
    customDb.close()
  }

  @Test
  fun `insertEvent stores and retrieves event correctly`() = runBlocking {
    val event = createTestEvent("test1")
    eventDao.insertEvent(event)

    val retrieved = eventDao.getEventById("test1")
    assertNotNull(retrieved)
    assertEquals("test1", retrieved?.eventId)
  }

  @Test
  fun `getEventById returns null for non-existent event`() = runBlocking {
    assertNull(eventDao.getEventById("nonexistent"))
  }

  @Test
  fun `getAllEvents returns all events`() = runBlocking {
    eventDao.insertEvent(createTestEvent("1"))
    eventDao.insertEvent(createTestEvent("2", ownerId = "owner2", visibility = "PRIVATE"))

    val allEvents = eventDao.getAllEvents()
    assertEquals(2, allEvents.size)
    assertTrue(allEvents.any { it.eventId == "1" })
    assertTrue(allEvents.any { it.eventId == "2" })
  }

  @Test
  fun `getEventsByOwner returns only owner's events`() = runBlocking {
    eventDao.insertEvent(createTestEvent("1", ownerId = "owner1"))
    eventDao.insertEvent(createTestEvent("2", ownerId = "owner2"))

    val owner1Events = eventDao.getEventsByOwner("owner1")
    assertEquals(1, owner1Events.size)
    assertEquals("1", owner1Events[0].eventId)
  }

  @Test
  fun `getPublicEvents returns only public events sorted by date`() = runBlocking {
    eventDao.insertEvent(createTestEvent("pub1", dateSeconds = 100))
    eventDao.insertEvent(createTestEvent("pub2", dateSeconds = 200))
    eventDao.insertEvent(createTestEvent("priv1", visibility = "PRIVATE", dateSeconds = 150))

    val publicEvents = eventDao.getPublicEvents(10)
    assertEquals(2, publicEvents.size)
    assertEquals("pub2", publicEvents[0].eventId) // Sorted by date descending
    assertEquals("pub1", publicEvents[1].eventId)
  }

  @Test
  fun `getPublicEvents respects limit parameter`() = runBlocking {
    repeat(5) { i ->
      eventDao.insertEvent(createTestEvent("pub$i", dateSeconds = i.toLong() * 100))
    }
    assertEquals(3, eventDao.getPublicEvents(3).size)
  }

  @Test
  fun `getUpcomingEvents returns events after start time`() = runBlocking {
    val currentTime = System.currentTimeMillis() / 1000
    eventDao.insertEvent(createTestEvent("past", dateSeconds = currentTime - 1000))
    eventDao.insertEvent(createTestEvent("future", dateSeconds = currentTime + 1000))

    val upcomingEvents = eventDao.getUpcomingEvents(currentTime, 10)
    assertEquals(1, upcomingEvents.size)
    assertEquals("future", upcomingEvents[0].eventId)
  }

  @Test
  fun `insertEvent with REPLACE strategy updates existing event`() = runBlocking {
    val event = createTestEvent("test1")
    eventDao.insertEvent(event)
    eventDao.insertEvent(event.copy(type = "ACTIVITY"))

    assertEquals("ACTIVITY", eventDao.getEventById("test1")?.type)
    assertEquals(1, eventDao.getAllEvents().size)
  }

  @Test
  fun `insertEvents stores multiple events`() = runBlocking {
    val events = listOf(createTestEvent("1"), createTestEvent("2"))
    eventDao.insertEvents(events)
    assertEquals(2, eventDao.getAllEvents().size)
  }

  @Test
  fun `deleteEvent removes event from database`() = runBlocking {
    eventDao.insertEvent(createTestEvent("delete_me"))
    assertEquals(1, eventDao.getAllEvents().size)

    eventDao.deleteEvent("delete_me")
    assertNull(eventDao.getEventById("delete_me"))
    assertEquals(0, eventDao.getAllEvents().size)
  }

  @Test
  fun `deleteAllEvents removes all events`() = runBlocking {
    repeat(3) { i -> eventDao.insertEvent(createTestEvent("event$i")) }
    assertEquals(3, eventDao.getAllEvents().size)

    eventDao.deleteAllEvents()
    assertEquals(0, eventDao.getAllEvents().size)
  }

  @Test
  fun `deleteOldEvents removes events older than timestamp`() = runBlocking {
    val currentTime = System.currentTimeMillis()
    eventDao.insertEvent(createTestEvent("old", cachedAt = currentTime - 10000))
    eventDao.insertEvent(createTestEvent("recent", cachedAt = currentTime))

    eventDao.deleteOldEvents(currentTime - 5000)

    val remaining = eventDao.getAllEvents()
    assertEquals(1, remaining.size)
    assertEquals("recent", remaining[0].eventId)
  }
}
