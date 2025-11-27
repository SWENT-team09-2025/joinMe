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
  fun `test Serie invoke operator initializes lastEventEndTime correctly`() {
    // Explicitly test the companion object's invoke operator function
    val testDate = Timestamp(Date())
    val serie =
        Serie.invoke(
            serieId = "test123",
            title = "Test Title",
            description = "Test Description",
            date = testDate,
            participants = listOf("user1", "user2"),
            maxParticipants = 5,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner456")

    // Verify all parameters are set correctly
    assertEquals("test123", serie.serieId)
    assertEquals("Test Title", serie.title)
    assertEquals("Test Description", serie.description)
    assertEquals(testDate, serie.date)
    assertEquals(listOf("user1", "user2"), serie.participants)
    assertEquals(5, serie.maxParticipants)
    assertEquals(Visibility.PUBLIC, serie.visibility)
    assertEquals(listOf("event1"), serie.eventIds)
    assertEquals("owner456", serie.ownerId)
    // Verify lastEventEndTime is initialized to date (compare timestamps)
    assertEquals(testDate.toDate().time, serie.lastEventEndTime?.toDate()?.time)
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

  @Test
  fun `groupId property works correctly for all cases`() {
    // Default to null
    assertEquals(null, sampleSerie.groupId)

    // Can be set via copy
    val copiedGroupSerie = sampleSerie.copy(groupId = "group123")
    assertEquals("group123", copiedGroupSerie.groupId)

    // Standalone serie with null groupId
    val standaloneSerie =
        Serie(
            serieId = "serie456",
            title = "Standalone Serie",
            description = "Individual serie",
            date = sampleTimestamp,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner456",
            groupId = null)

    // Serie with explicit groupId
    val groupSerie =
        Serie(
            serieId = "serie789",
            title = "Group Serie",
            description = "Team serie",
            date = sampleTimestamp,
            participants = listOf("user1", "user2", "user3"),
            maxParticipants = 20,
            visibility = Visibility.PRIVATE,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner789",
            groupId = "group456")

    // Test invoke operator with groupId
    val testDate = Timestamp(Date())
    val invokedGroupSerie =
        Serie.invoke(
            serieId = "test999",
            title = "Test Group Serie",
            description = "Test Description",
            date = testDate,
            participants = listOf("user1", "user2"),
            maxParticipants = 15,
            visibility = Visibility.PRIVATE,
            eventIds = listOf("event1", "event2"),
            ownerId = "owner999",
            groupId = "group999")

    // Test invoke operator without groupId (defaults to null)
    val invokedStandaloneSerie =
        Serie.invoke(
            serieId = "test888",
            title = "Test Standalone Serie",
            description = "Test Description",
            date = testDate,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = Visibility.PUBLIC,
            eventIds = listOf("event1"),
            ownerId = "owner888")

    // Assertions
    assertEquals(null, standaloneSerie.groupId)
    assertEquals("group456", groupSerie.groupId)
    assertEquals("group999", invokedGroupSerie.groupId)
    assertEquals(null, invokedStandaloneSerie.groupId)
  }
}
