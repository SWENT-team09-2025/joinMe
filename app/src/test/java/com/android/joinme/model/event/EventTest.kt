package com.android.joinme.model.event

import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventTest {

  private val sampleLocation = Location(46.5191, 6.5668, "EPFL")
  private val sampleTimestamp = Timestamp(Date())

  private val sampleEvent =
      Event(
          eventId = "123",
          type = EventType.SPORTS,
          title = "Football Game",
          description = "Friendly football match",
          location = sampleLocation,
          date = sampleTimestamp,
          duration = 90,
          participants = listOf("Alice", "Bob"),
          maxParticipants = 10,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner123")

  @Test
  fun `displayString returns correct string for all EventType values`() {
    assertEquals("Sports", EventType.SPORTS.displayString())
    assertEquals("Activity", EventType.ACTIVITY.displayString())
    assertEquals("Social", EventType.SOCIAL.displayString())
  }

  @Test
  fun `displayString returns correct string for all EventVisibility values`() {
    assertEquals("Public", EventVisibility.PUBLIC.displayString())
    assertEquals("Private", EventVisibility.PRIVATE.displayString())
  }

  @Test
  fun `test event properties`() {
    assertEquals("123", sampleEvent.eventId)
    assertEquals(EventType.SPORTS, sampleEvent.type)
    assertEquals("Football Game", sampleEvent.title)
    assertEquals("Friendly football match", sampleEvent.description)
    assertEquals(sampleLocation, sampleEvent.location)
    assertEquals(sampleTimestamp, sampleEvent.date)
    assertEquals(90, sampleEvent.duration)
    assertEquals(listOf("Alice", "Bob"), sampleEvent.participants)
    assertEquals(10, sampleEvent.maxParticipants)
    assertEquals(EventVisibility.PUBLIC, sampleEvent.visibility)
    assertEquals("owner123", sampleEvent.ownerId)
  }

  @Test
  fun `test event equality and hashCode`() {
    val eventCopy = sampleEvent.copy()
    assertEquals(sampleEvent, eventCopy)
    assertEquals(sampleEvent.hashCode(), eventCopy.hashCode())
  }

  @Test
  fun `test event inequality`() {
    val differentEvent = sampleEvent.copy(eventId = "456")
    assertNotEquals(sampleEvent, differentEvent)
  }

  @Test
  fun `test copy function changes single property`() {
    val newTitleEvent = sampleEvent.copy(title = "Changed Title")
    assertEquals("Changed Title", newTitleEvent.title)
    assertNotEquals(sampleEvent, newTitleEvent)
  }

  @Test
  fun `test toString contains key fields`() {
    val result = sampleEvent.toString()
    assert(result.contains("Football Game"))
    assert(result.contains("owner123"))
  }

  @Test
  fun `event transitions correctly through lifecycle states`() {
    val calendar = Calendar.getInstance()

    // Future event: should only be upcoming
    calendar.add(Calendar.HOUR, 1)
    val futureEvent = sampleEvent.copy(date = Timestamp(calendar.time), duration = 60)
    assertTrue(futureEvent.isUpcoming())
    assertFalse(futureEvent.isActive())
    assertFalse(futureEvent.isExpired())

    // Ongoing event: should only be active
    calendar.time = Date() // Reset
    calendar.add(Calendar.MINUTE, -30)
    val ongoingEvent = sampleEvent.copy(date = Timestamp(calendar.time), duration = 120)
    assertFalse(ongoingEvent.isUpcoming())
    assertTrue(ongoingEvent.isActive())
    assertFalse(ongoingEvent.isExpired())

    // Past event: should only be expired
    calendar.time = Date() // Reset
    calendar.add(Calendar.HOUR, -2)
    val pastEvent = sampleEvent.copy(date = Timestamp(calendar.time), duration = 60)
    assertFalse(pastEvent.isUpcoming())
    assertFalse(pastEvent.isActive())
    assertTrue(pastEvent.isExpired())
  }

  @Test
  fun `partOfASerie defaults to false`() {
    assertFalse(sampleEvent.partOfASerie)
  }

  @Test
  fun `event can be part of a serie`() {
    val serieEvent = sampleEvent.copy(partOfASerie = true)
    assertTrue(serieEvent.partOfASerie)
  }

  @Test
  fun `partOfASerie property is correctly set`() {
    val standaloneEvent =
        Event(
            eventId = "456",
            type = EventType.SOCIAL,
            title = "Bar Night",
            description = "Casual bar outing",
            location = sampleLocation,
            date = sampleTimestamp,
            duration = 120,
            participants = listOf("Charlie", "Dave"),
            maxParticipants = 15,
            visibility = EventVisibility.PRIVATE,
            ownerId = "owner456",
            partOfASerie = false)

    val serieEvent =
        Event(
            eventId = "789",
            type = EventType.ACTIVITY,
            title = "Weekly Bowling",
            description = "Regular bowling session",
            location = sampleLocation,
            date = sampleTimestamp,
            duration = 90,
            participants = listOf("Eve", "Frank"),
            maxParticipants = 8,
            visibility = EventVisibility.PUBLIC,
            ownerId = "owner789",
            partOfASerie = true)

    assertFalse(standaloneEvent.partOfASerie)
    assertTrue(serieEvent.partOfASerie)
  }
}
