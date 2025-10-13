package com.android.joinme.model.event

import androidx.compose.ui.graphics.Color
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
  fun `isExpired returns true for past events`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -3) // Started 3 hours ago
    val pastEvent =
        sampleEvent.copy(
            date = Timestamp(calendar.time),
            duration = 60 // 1 hour duration, so it ended 2 hours ago
            )
    assertTrue(pastEvent.isExpired())
  }

  @Test
  fun `isExpired returns false for current events`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MINUTE, -30) // Started 30 minutes ago
    val currentEvent =
        sampleEvent.copy(
            date = Timestamp(calendar.time), duration = 120 // 2 hour duration, so still ongoing
            )
    assertFalse(currentEvent.isExpired())
  }

  @Test
  fun `isExpired returns false for future events`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2) // Starts in 2 hours
    val futureEvent = sampleEvent.copy(date = Timestamp(calendar.time), duration = 60)
    assertFalse(futureEvent.isExpired())
  }

  @Test
  fun `isActive returns true for ongoing events`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MINUTE, -30) // Started 30 minutes ago
    val ongoingEvent =
        sampleEvent.copy(
            date = Timestamp(calendar.time), duration = 120 // 2 hour duration, so still ongoing
            )
    assertTrue(ongoingEvent.isActive())
  }

  @Test
  fun `isActive returns false for past events`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -3) // Started 3 hours ago
    val pastEvent =
        sampleEvent.copy(
            date = Timestamp(calendar.time), duration = 60 // 1 hour duration, so ended 2 hours ago
            )
    assertFalse(pastEvent.isActive())
  }

  @Test
  fun `isActive returns false for future events`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2) // Starts in 2 hours
    val futureEvent = sampleEvent.copy(date = Timestamp(calendar.time), duration = 60)
    assertFalse(futureEvent.isActive())
  }

  @Test
  fun `isActive returns true for event that just started`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MINUTE, -1) // Started 1 minute ago
    val justStartedEvent = sampleEvent.copy(date = Timestamp(calendar.time), duration = 60)
    assertTrue(justStartedEvent.isActive())
  }

  @Test
  fun `isUpcoming returns true for future events`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2) // Starts in 2 hours
    val futureEvent = sampleEvent.copy(date = Timestamp(calendar.time), duration = 60)
    assertTrue(futureEvent.isUpcoming())
  }

  @Test
  fun `isUpcoming returns false for ongoing events`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MINUTE, -30) // Started 30 minutes ago
    val ongoingEvent =
        sampleEvent.copy(
            date = Timestamp(calendar.time), duration = 120 // Still ongoing
            )
    assertFalse(ongoingEvent.isUpcoming())
  }

  @Test
  fun `isUpcoming returns false for past events`() {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, -3) // Started 3 hours ago
    val pastEvent = sampleEvent.copy(date = Timestamp(calendar.time), duration = 60)
    assertFalse(pastEvent.isUpcoming())
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
  fun `getColor returns correct color for SPORTS event type`() {
    val color = EventType.SPORTS.getColor()
    assertEquals(Color(0xFF7E57C2), color)
  }

  @Test
  fun `getColor returns correct color for ACTIVITY event type`() {
    val color = EventType.ACTIVITY.getColor()
    assertEquals(Color(0xFF81C784), color)
  }

  @Test
  fun `getColor returns correct color for SOCIAL event type`() {
    val color = EventType.SOCIAL.getColor()
    assertEquals(Color(0xFFE57373), color)
  }

  @Test
  fun `getColor returns different colors for different event types`() {
    val sportsColor = EventType.SPORTS.getColor()
    val activityColor = EventType.ACTIVITY.getColor()
    val socialColor = EventType.SOCIAL.getColor()

    assertNotEquals(sportsColor, activityColor)
    assertNotEquals(sportsColor, socialColor)
    assertNotEquals(activityColor, socialColor)
  }
}
