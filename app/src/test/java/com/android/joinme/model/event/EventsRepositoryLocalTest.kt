package com.android.joinme.model.event

import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class EventsRepositoryLocalTest {

  private lateinit var repo: EventsRepositoryLocal
  private lateinit var sampleEvent: Event

  @Before
  fun setup() {
    repo = EventsRepositoryLocal()
    sampleEvent =
        Event(
            eventId = "1",
            type = EventType.SPORTS,
            title = "Football Match",
            description = "Friendly game at the park",
            location = Location(46.52, 6.63, "EPFL Stadium"),
            date = Timestamp.Companion.now(),
            duration = 90,
            participants = listOf("A", "B"),
            maxParticipants = 10,
            visibility = EventVisibility.PUBLIC,
            ownerId = "user1")
  }

  // ---------------- BASIC CRUD ----------------

  @Test
  fun addAndGetEvent_success() {
    runBlocking {
      repo.addEvent(sampleEvent)
      val event = repo.getEvent("1")
      Assert.assertEquals(sampleEvent.title, event.title)
    }
  }

  @Test(expected = Exception::class)
  fun getEvent_notFound_throwsException() {
    runBlocking { repo.getEvent("unknown") }
  }

  @Test
  fun editEvent_updatesSuccessfully() {
    runBlocking {
      repo.addEvent(sampleEvent)
      val updated = sampleEvent.copy(title = "Updated Title")
      repo.editEvent("1", updated)
      val event = repo.getEvent("1")
      Assert.assertEquals("Updated Title", event.title)
    }
  }

  @Test
  fun deleteEvent_removesSuccessfully() {
    runBlocking {
      repo.addEvent(sampleEvent)
      repo.deleteEvent("1")
      val all = repo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)
      Assert.assertTrue(all.isEmpty())
    }
  }

  // ---------------- ADDITIONAL TESTS ----------------

  @Test
  fun addMultipleEvents_storesAll() {
    runBlocking {
      val e1 = sampleEvent
      val e2 = sampleEvent.copy(eventId = "2", title = "Basketball")
      val e3 = sampleEvent.copy(eventId = "3", title = "Tennis")
      repo.addEvent(e1)
      repo.addEvent(e2)
      repo.addEvent(e3)

      val all = repo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN)
      Assert.assertEquals(3, all.size)
      Assert.assertTrue(all.any { it.title == "Tennis" })
    }
  }

  @Test
  fun getNewEventId_incrementsSequentially() {
    val id1 = repo.getNewEventId()
    val id2 = repo.getNewEventId()
    val id3 = repo.getNewEventId()
    Assert.assertEquals((id1.toInt() + 1).toString(), id2)
    Assert.assertEquals((id2.toInt() + 1).toString(), id3)
  }

  @Test(expected = Exception::class)
  fun editEvent_notFound_throwsException() {
    runBlocking {
      val fake = sampleEvent.copy(eventId = "999", title = "DoesNotExist")
      repo.editEvent(fake.eventId, fake)
    }
  }

  @Test(expected = Exception::class)
  fun deleteEvent_notFound_throwsException() {
    runBlocking {
      repo.addEvent(sampleEvent)
      repo.deleteEvent("nonexistent")
    }
  }

  @Test
  fun addDuplicateId_keepsOriginalOnGet() {
    runBlocking {
      repo.addEvent(sampleEvent)
      val duplicate = sampleEvent.copy(title = "Duplicate Event")
      repo.addEvent(duplicate)

      // getEvent returns the first matching item (original)
      val fetched = repo.getEvent("1")
      Assert.assertEquals("Football Match", fetched.title)

      // and both entries exist with the same ID
      val allWithSameId =
          repo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN).filter { it.eventId == "1" }
      Assert.assertEquals(2, allWithSameId.size)
    }
  }

  @Test
  fun getEventsByIds_returnsMatchingEvents() {
    runBlocking {
      val e1 = sampleEvent.copy(eventId = "1", title = "Event 1")
      val e2 = sampleEvent.copy(eventId = "2", title = "Event 2")
      val e3 = sampleEvent.copy(eventId = "3", title = "Event 3")
      repo.addEvent(e1)
      repo.addEvent(e2)
      repo.addEvent(e3)

      val result = repo.getEventsByIds(listOf("1", "3"))

      Assert.assertEquals(2, result.size)
      Assert.assertTrue(result.any { it.eventId == "1" })
      Assert.assertTrue(result.any { it.eventId == "3" })
      Assert.assertFalse(result.any { it.eventId == "2" })
    }
  }

  @Test
  fun getEventsByIds_returnsEmptyListWhenNoMatches() {
    runBlocking {
      repo.addEvent(sampleEvent.copy(eventId = "1"))

      val result = repo.getEventsByIds(listOf("999", "888"))

      Assert.assertTrue(result.isEmpty())
    }
  }

  @Test
  fun getEventsByIds_returnsEmptyListWhenEmptyInput() {
    runBlocking {
      repo.addEvent(sampleEvent)

      val result = repo.getEventsByIds(emptyList())

      Assert.assertTrue(result.isEmpty())
    }
  }

  @Test
  fun clear_removesAllEventsAndResetsCounter() {
    runBlocking {
      repo.addEvent(sampleEvent)
      repo.addEvent(sampleEvent.copy(eventId = "2"))
      repo.getNewEventId() // increment counter

      repo.clear()

      Assert.assertTrue(repo.getAllEvents(EventFilter.EVENTS_FOR_OVERVIEW_SCREEN).isEmpty())
      Assert.assertEquals("0", repo.getNewEventId())
    }
  }

  // ---------------- GET COMMON EVENTS TESTS ----------------

  @Test
  fun getCommonEvents_returnsEmptyListWhenNoUserIds() {
    runBlocking {
      repo.addEvent(sampleEvent)
      val result = repo.getCommonEvents(emptyList())
      Assert.assertTrue(result.isEmpty())
    }
  }

  @Test
  fun getCommonEvents_returnsEventsWithSingleUser() {
    runBlocking {
      val e1 = sampleEvent.copy(eventId = "1", participants = listOf("user1", "user2"))
      val e2 = sampleEvent.copy(eventId = "2", participants = listOf("user2"))
      val e3 = sampleEvent.copy(eventId = "3", participants = listOf("user1", "user3"))
      repo.clear()
      repo.addEvent(e1)
      repo.addEvent(e2)
      repo.addEvent(e3)

      val result = repo.getCommonEvents(listOf("user2"))
      Assert.assertEquals(2, result.size)
      Assert.assertTrue(result.any { it.eventId == "1" })
      Assert.assertTrue(result.any { it.eventId == "2" })
    }
  }

  @Test
  fun getCommonEvents_returnsEventsWithMultipleUsers() {
    runBlocking {
      val e1 = sampleEvent.copy(eventId = "1", participants = listOf("user1", "user2", "user3"))
      val e2 = sampleEvent.copy(eventId = "2", participants = listOf("user1", "user2"))
      val e3 = sampleEvent.copy(eventId = "3", participants = listOf("user1", "user3"))
      repo.addEvent(e1)
      repo.addEvent(e2)
      repo.addEvent(e3)

      val result = repo.getCommonEvents(listOf("user1", "user2"))

      Assert.assertEquals(2, result.size)
      Assert.assertTrue(result.any { it.eventId == "1" })
      Assert.assertTrue(result.any { it.eventId == "2" })
    }
  }

  @Test
  fun getCommonEvents_returnsEmptyListWhenNoCommonEvents() {
    runBlocking {
      val e1 = sampleEvent.copy(eventId = "1", participants = listOf("user1", "user3"))
      val e2 = sampleEvent.copy(eventId = "2", participants = listOf("user1", "user3"))
      repo.addEvent(e1)
      repo.addEvent(e2)

      val result = repo.getCommonEvents(listOf("user3", "user2"))

      Assert.assertTrue(result.isEmpty())
    }
  }

  @Test
  fun getCommonEvents_sortsByDateAscending() {
    runBlocking {
      val now = System.currentTimeMillis() / 1000
      val e1 =
          sampleEvent.copy(
              eventId = "1", participants = listOf("user1"), date = Timestamp(now + 3600, 0))
      val e2 =
          sampleEvent.copy(
              eventId = "2", participants = listOf("user1"), date = Timestamp(now + 1800, 0))
      val e3 =
          sampleEvent.copy(
              eventId = "3", participants = listOf("user1"), date = Timestamp(now + 7200, 0))
      repo.addEvent(e1)
      repo.addEvent(e2)
      repo.addEvent(e3)

      val result = repo.getCommonEvents(listOf("user1"))

      Assert.assertEquals(3, result.size)
      Assert.assertEquals("2", result[0].eventId) // earliest
      Assert.assertEquals("1", result[1].eventId)
      Assert.assertEquals("3", result[2].eventId) // latest
    }
  }

  @Test
  fun getCommonEvents_requiresAllUsersToBeParticipants() {
    runBlocking {
      val e1 = sampleEvent.copy(eventId = "1", participants = listOf("user1", "user2", "user3"))
      val e2 = sampleEvent.copy(eventId = "2", participants = listOf("user1", "user2"))
      val e3 = sampleEvent.copy(eventId = "3", participants = listOf("user3"))
      repo.addEvent(e1)
      repo.addEvent(e2)
      repo.addEvent(e3)

      val result = repo.getCommonEvents(listOf("user1", "user2", "user3"))

      Assert.assertEquals(1, result.size)
      Assert.assertEquals("1", result[0].eventId)
    }
  }
}
