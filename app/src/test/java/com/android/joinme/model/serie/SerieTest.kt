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
          ownerId = "owner123",
          lastEventEndTime = sampleTimestamp)

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
  fun `test serie has lastEventEndTime initialized to date`() {
    // When creating a serie via the public API, lastEventEndTime should be set to date
    assertTrue(sampleSerie.lastEventEndTime != null)
    assertEquals(sampleSerie.date.toDate().time, sampleSerie.lastEventEndTime!!.toDate().time)
  }

  @Test
  fun `test toString contains key fields`() {
    val result = sampleSerie.toString()
    assert(result.contains("Weekly Football"))
    assert(result.contains("owner123"))
  }

  @Test
  fun `getTotalDuration calculates from date to lastEventEndTime`() {
    val calendar = Calendar.getInstance()
    val startDate = calendar.time
    calendar.add(Calendar.MINUTE, 270) // 4.5 hours later
    val endDate = calendar.time

    val serie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1", "event2", "event3"),
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    val totalDuration = serie.getTotalDuration()
    assertEquals(270, totalDuration) // 270 minutes
  }

  @Test
  fun `getTotalDuration returns zero when lastEventEndTime equals date`() {
    val totalDuration = sampleSerie.getTotalDuration()
    assertEquals(0, totalDuration) // date and lastEventEndTime are the same
  }

  @Test
  fun `isActive returns true when current time is between date and lastEventEndTime`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -1) // Started 1 hour ago
    val startDate = calendar.time
    calendar.add(Calendar.HOUR, 3) // Ends 2 hours from now
    val endDate = calendar.time

    val serie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    assertTrue(serie.isActive())
  }

  @Test
  fun `isActive returns false when serie has not started yet`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2) // Starts 2 hours from now
    val startDate = calendar.time
    calendar.add(Calendar.HOUR, 3) // Ends 5 hours from now
    val endDate = calendar.time

    val serie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    assertFalse(serie.isActive())
  }

  @Test
  fun `isActive returns false when serie has already ended`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -5) // Started 5 hours ago
    val startDate = calendar.time
    calendar.add(Calendar.HOUR, 3) // Ended 2 hours ago
    val endDate = calendar.time

    val serie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    assertFalse(serie.isActive())
  }

  @Test
  fun `isExpired returns true when lastEventEndTime is in the past`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -5) // Started 5 hours ago
    val startDate = calendar.time
    calendar.add(Calendar.HOUR, 3) // Ended 2 hours ago
    val endDate = calendar.time

    val serie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    assertTrue(serie.isExpired())
  }

  @Test
  fun `isExpired returns false when lastEventEndTime is in the future`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -1) // Started 1 hour ago
    val startDate = calendar.time
    calendar.add(Calendar.HOUR, 3) // Ends 2 hours from now
    val endDate = calendar.time

    val serie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    assertFalse(serie.isExpired())
  }

  @Test
  fun `isExpired returns false when lastEventEndTime equals date (not started yet)`() {
    assertFalse(sampleSerie.isExpired()) // lastEventEndTime equals date by default
  }

  @Test
  fun `isUpcoming returns true when serie date is in future`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2)

    val futureSerie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(calendar.time),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123")

    assertTrue(futureSerie.isUpcoming())
  }

  @Test
  fun `isUpcoming returns false when serie date is in past`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -5)

    val pastSerie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(calendar.time),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123")

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
    val emptySerie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = sampleTimestamp,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = emptyList(),
            ownerId = "owner123")

    assertEquals(0, emptySerie.getTotalEventsCount())
  }

  @Test
  fun `getFormattedDuration returns hours and minutes when both present`() {
    val calendar = Calendar.getInstance()
    val startDate = calendar.time
    calendar.add(Calendar.MINUTE, 270) // 4.5 hours later
    val endDate = calendar.time

    val serie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    val formatted = serie.getFormattedDuration()
    assertEquals("4h 30min", formatted) // Total: 270 minutes
  }

  @Test
  fun `getFormattedDuration returns only hours when no remaining minutes`() {
    val calendar = Calendar.getInstance()
    val startDate = calendar.time
    calendar.add(Calendar.MINUTE, 240) // 4 hours later
    val endDate = calendar.time

    val serie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    val formatted = serie.getFormattedDuration()
    assertEquals("4h", formatted) // Total: 240 minutes
  }

  @Test
  fun `getFormattedDuration returns only minutes when less than one hour`() {
    val calendar = Calendar.getInstance()
    val startDate = calendar.time
    calendar.add(Calendar.MINUTE, 45)
    val endDate = calendar.time

    val serie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    val formatted = serie.getFormattedDuration()
    assertEquals("45min", formatted)
  }

  @Test
  fun `getFormattedDuration returns 0min when lastEventEndTime equals date`() {
    val formatted = sampleSerie.getFormattedDuration()
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
    val calendar = Calendar.getInstance()
    val startDate = calendar.time
    calendar.add(Calendar.MINUTE, 1200) // 20 hours later
    val endDate = calendar.time

    val eventIds = (1..20).map { "event$it" }
    val largeSerie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = Timestamp(startDate),
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = eventIds,
            ownerId = "owner123",
            lastEventEndTime = Timestamp(endDate))

    val events = eventIds.map { id -> createEvent(id, duration = 60, hoursOffset = 1) }

    assertEquals(20, largeSerie.getTotalEventsCount())
    assertEquals(1200, largeSerie.getTotalDuration()) // 20 * 60
    assertEquals("20h", largeSerie.getFormattedDuration())
    assertEquals(20, largeSerie.getSerieEvents(events).size)
  }

  @Test
  fun `serie visibility can be PRIVATE`() {
    val privateSerie =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = sampleTimestamp,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PRIVATE,
            eventIds = listOf("event1"),
            ownerId = "owner123")

    assertEquals(Visibility.PRIVATE, privateSerie.visibility)
  }

  @Test
  fun `serie can have empty participants list`() {
    val serieWithNoParticipants =
        Serie(
            serieId = "serie123",
            title = "Test Serie",
            description = "Test",
            date = sampleTimestamp,
            participants = emptyList(),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner123")

    assertTrue(serieWithNoParticipants.participants.isEmpty())
    assertEquals(0, serieWithNoParticipants.participants.size)
  }
}
