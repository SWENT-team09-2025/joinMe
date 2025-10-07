package com.android.joinme.model.event

import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
