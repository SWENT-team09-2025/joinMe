package com.android.joinme.model.serie

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerieTest {

  private val sampleLocation = Location(46.5191, 6.5668, "EPFL")
  private val sampleTimestamp = Timestamp(Date())

  private val sampleSerie =
      Serie(
          serieId = "serie123",
          title = "Weekly Football",
          description = "Weekly football series",
          date = sampleTimestamp,
          participants = listOf("user1", "user2"),
          maxParticipants = 10,
          visibility = Visibility.PUBLIC,
          eventIds = listOf("event1", "event2", "event3"),
          ownerId = "owner123")

  private fun createEvent(eventId: String, duration: Int = 90, hoursOffset: Int = 0): Event {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, hoursOffset)
    return Event(
        eventId = eventId,
        type = EventType.SPORTS,
        title = "Football Game",
        description = "Friendly football match",
        location = sampleLocation,
        date = Timestamp(calendar.time),
        duration = duration,
        participants = listOf("Alice", "Bob"),
        maxParticipants = 10,
        visibility = EventVisibility.PUBLIC,
        ownerId = "owner123")
  }

  @Test
  fun `test serie properties`() {
    assertEquals("serie123", sampleSerie.serieId)
    assertEquals("Weekly Football", sampleSerie.title)
    assertEquals("Weekly football series", sampleSerie.description)
    assertEquals(sampleTimestamp, sampleSerie.date)
    assertEquals(10, sampleSerie.maxParticipants)
    assertEquals(Visibility.PUBLIC, sampleSerie.visibility)
    assertEquals(listOf("event1", "event2", "event3"), sampleSerie.eventIds)
    assertEquals("owner123", sampleSerie.ownerId)
  }

  @Test
  fun `test serie equality and hashCode`() {
    val serieCopy = sampleSerie.copy()
    assertEquals(sampleSerie, serieCopy)
    assertEquals(sampleSerie.hashCode(), serieCopy.hashCode())
  }

  @Test
  fun `test serie inequality`() {
    val differentSerie = sampleSerie.copy(serieId = "serie456")
    assertNotEquals(sampleSerie, differentSerie)
  }

  @Test
  fun `test copy function changes single property`() {
    val newTitleSerie = sampleSerie.copy(title = "Changed Title")
    assertEquals("Changed Title", newTitleSerie.title)
    assertNotEquals(sampleSerie, newTitleSerie)
  }

  @Test
  fun `test toString contains key fields`() {
    val result = sampleSerie.toString()
    assert(result.contains("Weekly Football"))
    assert(result.contains("owner123"))
  }

  @Test
  fun `getTotalDuration returns sum of all event durations`() {
    val events =
        listOf(
            createEvent("event1", duration = 60),
            createEvent("event2", duration = 90),
            createEvent("event3", duration = 120))

    val totalDuration = sampleSerie.getTotalDuration(events)
    assertEquals(270, totalDuration) // 60 + 90 + 120
  }

  @Test
  fun `getTotalDuration ignores events not in serie`() {
    val events =
        listOf(
            createEvent("event1", duration = 60),
            createEvent("event2", duration = 90),
            createEvent("otherEvent", duration = 100))

    val totalDuration = sampleSerie.getTotalDuration(events)
    assertEquals(150, totalDuration) // 60 + 90, excluding otherEvent
  }

  @Test
  fun `getTotalDuration returns zero for empty event list`() {
    val totalDuration = sampleSerie.getTotalDuration(emptyList())
    assertEquals(0, totalDuration)
  }

  @Test
  fun `isActive returns true when one event is ongoing`() {
    val events =
        listOf(
            createEvent("event1", duration = 60, hoursOffset = -3), // Past
            createEvent("event2", duration = 120, hoursOffset = 0), // Ongoing (started now)
            createEvent("event3", duration = 60, hoursOffset = 5)) // Future

    assertTrue(sampleSerie.isActive(events))
  }

  @Test
  fun `isActive returns false when all events are past`() {
    val events =
        listOf(
            createEvent("event1", duration = 60, hoursOffset = -5),
            createEvent("event2", duration = 60, hoursOffset = -3),
            createEvent("event3", duration = 60, hoursOffset = -2))

    assertFalse(sampleSerie.isActive(events))
  }

  @Test
  fun `isActive returns false when all events are future`() {
    val events =
        listOf(
            createEvent("event1", duration = 60, hoursOffset = 2),
            createEvent("event2", duration = 60, hoursOffset = 5),
            createEvent("event3", duration = 60, hoursOffset = 10))

    assertFalse(sampleSerie.isActive(events))
  }

  @Test
  fun `isActive ignores events not in serie`() {
    val activeEventNotInSerie = createEvent("otherEvent", duration = 120, hoursOffset = 0)
    val events =
        listOf(
            createEvent("event1", duration = 60, hoursOffset = -5), // Past
            activeEventNotInSerie,
            createEvent("event3", duration = 60, hoursOffset = 5)) // Future

    assertFalse(sampleSerie.isActive(events))
  }

  @Test
  fun `isExpired returns true when all events are past`() {
    val events =
        listOf(
            createEvent("event1", duration = 60, hoursOffset = -5),
            createEvent("event2", duration = 60, hoursOffset = -3),
            createEvent("event3", duration = 60, hoursOffset = -2))

    assertTrue(sampleSerie.isExpired(events))
  }

  @Test
  fun `isExpired returns false when at least one event is ongoing`() {
    val events =
        listOf(
            createEvent("event1", duration = 60, hoursOffset = -5), // Past
            createEvent("event2", duration = 120, hoursOffset = 0), // Ongoing
            createEvent("event3", duration = 60, hoursOffset = 5)) // Future

    assertFalse(sampleSerie.isExpired(events))
  }

  @Test
  fun `isExpired returns false when at least one event is future`() {
    val events =
        listOf(
            createEvent("event1", duration = 60, hoursOffset = -5),
            createEvent("event2", duration = 60, hoursOffset = -3),
            createEvent("event3", duration = 60, hoursOffset = 2))

    assertFalse(sampleSerie.isExpired(events))
  }

  @Test
  fun `isExpired returns true when serie has no events and serie date is in past`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -5)
    val serie = sampleSerie.copy(eventIds = emptyList(), date = Timestamp(calendar.time))
    val events = listOf(createEvent("event1", duration = 60, hoursOffset = -5))

    assertTrue(serie.isExpired(events))
  }

  @Test
  fun `isExpired returns false when serie has no events but serie date is in future`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 5)
    val serie = sampleSerie.copy(eventIds = emptyList(), date = Timestamp(calendar.time))
    val events = listOf(createEvent("event1", duration = 60, hoursOffset = -5))

    assertFalse(serie.isExpired(events))
  }

  @Test
  fun `isUpcoming returns true when serie date is in future`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2)
    val futureSerie = sampleSerie.copy(date = Timestamp(calendar.time))

    assertTrue(futureSerie.isUpcoming())
  }

  @Test
  fun `isUpcoming returns false when serie date is in past`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -5)
    val pastSerie = sampleSerie.copy(date = Timestamp(calendar.time))

    assertFalse(pastSerie.isUpcoming())
  }

  @Test
  fun `getSerieEvents returns only events in serie sorted by date`() {
    val events =
        listOf(
            createEvent("event3", hoursOffset = 5),
            createEvent("otherEvent", hoursOffset = 2),
            createEvent("event1", hoursOffset = 1),
            createEvent("event2", hoursOffset = 3))

    val serieEvents = sampleSerie.getSerieEvents(events)

    assertEquals(3, serieEvents.size)
    assertEquals("event1", serieEvents[0].eventId)
    assertEquals("event2", serieEvents[1].eventId)
    assertEquals("event3", serieEvents[2].eventId)
  }

  @Test
  fun `getSerieEvents returns empty list when no matching events`() {
    val events = listOf(createEvent("otherEvent1"), createEvent("otherEvent2"))

    val serieEvents = sampleSerie.getSerieEvents(events)

    assertTrue(serieEvents.isEmpty())
  }

  @Test
  fun `getTotalEventsCount returns correct count`() {
    assertEquals(3, sampleSerie.getTotalEventsCount())
  }

  @Test
  fun `getTotalEventsCount returns zero for empty serie`() {
    val emptySerie = sampleSerie.copy(eventIds = emptyList())
    assertEquals(0, emptySerie.getTotalEventsCount())
  }

  @Test
  fun `getFormattedDuration returns hours and minutes when both present`() {
    val events =
        listOf(
            createEvent("event1", duration = 90), // 1h 30min
            createEvent("event2", duration = 120), // 2h
            createEvent("event3", duration = 60)) // 1h

    val formatted = sampleSerie.getFormattedDuration(events)
    assertEquals("4h 30min", formatted) // Total: 270 minutes
  }

  @Test
  fun `getFormattedDuration returns only hours when no remaining minutes`() {
    val events =
        listOf(
            createEvent("event1", duration = 60),
            createEvent("event2", duration = 120),
            createEvent("event3", duration = 60))

    val formatted = sampleSerie.getFormattedDuration(events)
    assertEquals("4h", formatted) // Total: 240 minutes
  }

  @Test
  fun `getFormattedDuration returns only minutes when less than one hour`() {
    val events =
        listOf(
            createEvent("event1", duration = 15),
            createEvent("event2", duration = 20),
            createEvent("event3", duration = 10))

    val formatted = sampleSerie.getFormattedDuration(events)
    assertEquals("45min", formatted)
  }

  @Test
  fun `getFormattedDuration returns 0min for empty events`() {
    val formatted = sampleSerie.getFormattedDuration(emptyList())
    assertEquals("0min", formatted)
  }

  @Test
  fun `SerieWithEvents contains correct serie and events`() {
    val events = listOf(createEvent("event1"), createEvent("event2"))
    val serieWithEvents = SerieWithEvents(serie = sampleSerie, events = events)

    assertEquals(sampleSerie, serieWithEvents.serie)
    assertEquals(events, serieWithEvents.events)
  }

  @Test
  fun `serie with large number of events handles correctly`() {
    val eventIds = (1..20).map { "event$it" }
    val largeSerie = sampleSerie.copy(eventIds = eventIds)
    val events = eventIds.map { id -> createEvent(id, duration = 60, hoursOffset = 1) }

    assertEquals(20, largeSerie.getTotalEventsCount())
    assertEquals(1200, largeSerie.getTotalDuration(events)) // 20 * 60
    assertEquals("20h", largeSerie.getFormattedDuration(events))
    assertEquals(20, largeSerie.getSerieEvents(events).size)
  }

  @Test
  fun `serie visibility can be PRIVATE`() {
    val privateSerie = sampleSerie.copy(visibility = Visibility.PRIVATE)
    assertEquals(Visibility.PRIVATE, privateSerie.visibility)
  }

  @Test
  fun `serie can have empty participants list`() {
    val serieWithNoParticipants = sampleSerie.copy(participants = emptyList())
    assertTrue(serieWithNoParticipants.participants.isEmpty())
    assertEquals(0, serieWithNoParticipants.participants.size)
  }
}
