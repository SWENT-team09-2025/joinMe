package com.android.joinme.model.database

import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.EventVisibility
import com.android.joinme.model.map.Location
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.*
import org.junit.Test

class EventEntityTest {

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
          participants = listOf("user1", "user2", "user3"),
          maxParticipants = 10,
          visibility = EventVisibility.PUBLIC,
          ownerId = "owner123",
          partOfASerie = false,
          groupId = "group123")

  @Test
  fun `toEntity converts Event to EventEntity correctly`() {
    val entity = sampleEvent.toEntity()

    assertEquals("123", entity.eventId)
    assertEquals("SPORTS", entity.type)
    assertEquals("Football Game", entity.title)
    assertEquals("Friendly football match", entity.description)
    assertEquals(46.5191, entity.locationLatitude!!, 0.0001)
    assertEquals(6.5668, entity.locationLongitude!!, 0.0001)
    assertEquals("EPFL", entity.locationName)
    assertEquals(sampleTimestamp.seconds, entity.dateSeconds)
    assertEquals(sampleTimestamp.nanoseconds, entity.dateNanoseconds)
    assertEquals(90, entity.duration)
    assertEquals("[\"user1\",\"user2\",\"user3\"]", entity.participantsJson)
    assertEquals(10, entity.maxParticipants)
    assertEquals("PUBLIC", entity.visibility)
    assertEquals("owner123", entity.ownerId)
    assertFalse(entity.partOfASerie)
    assertEquals("group123", entity.groupId)
    assertTrue(entity.cachedAt > 0)
  }

  @Test
  fun `toEntity handles event with no location`() {
    val eventWithoutLocation = sampleEvent.copy(location = null)
    val entity = eventWithoutLocation.toEntity()

    assertNull(entity.locationLatitude)
    assertNull(entity.locationLongitude)
    assertNull(entity.locationName)
  }

  @Test
  fun `toEntity handles empty participants list`() {
    val eventWithoutParticipants = sampleEvent.copy(participants = emptyList())
    val entity = eventWithoutParticipants.toEntity()

    assertEquals("[]", entity.participantsJson)
  }

  @Test
  fun `toEntity handles single participant`() {
    val eventWithOneParticipant = sampleEvent.copy(participants = listOf("user1"))
    val entity = eventWithOneParticipant.toEntity()

    assertEquals("[\"user1\"]", entity.participantsJson)
  }

  @Test
  fun `toEntity handles null groupId`() {
    val eventWithoutGroup = sampleEvent.copy(groupId = null)
    val entity = eventWithoutGroup.toEntity()

    assertNull(entity.groupId)
  }

  @Test
  fun `toEntity handles partOfASerie true`() {
    val serieEvent = sampleEvent.copy(partOfASerie = true)
    val entity = serieEvent.toEntity()

    assertTrue(entity.partOfASerie)
  }

  @Test
  fun `toEvent converts EventEntity back to Event correctly`() {
    val entity =
        EventEntity(
            eventId = "456",
            type = "ACTIVITY",
            title = "Hiking Trip",
            description = "Mountain hiking adventure",
            locationLatitude = 46.0,
            locationLongitude = 7.0,
            locationName = "Alps",
            dateSeconds = sampleTimestamp.seconds,
            dateNanoseconds = sampleTimestamp.nanoseconds,
            duration = 180,
            participantsJson = "[\"user1\",\"user2\"]",
            maxParticipants = 5,
            visibility = "PRIVATE",
            ownerId = "owner456",
            partOfASerie = true,
            groupId = "group456",
            cachedAt = System.currentTimeMillis())

    val event = entity.toEvent()

    assertEquals("456", event.eventId)
    assertEquals(EventType.ACTIVITY, event.type)
    assertEquals("Hiking Trip", event.title)
    assertEquals("Mountain hiking adventure", event.description)
    assertNotNull(event.location)
    assertEquals(46.0, event.location!!.latitude, 0.0001)
    assertEquals(7.0, event.location!!.longitude, 0.0001)
    assertEquals("Alps", event.location!!.name)
    assertEquals(sampleTimestamp.seconds, event.date.seconds)
    assertEquals(sampleTimestamp.nanoseconds, event.date.nanoseconds)
    assertEquals(180, event.duration)
    assertEquals(listOf("user1", "user2"), event.participants)
    assertEquals(5, event.maxParticipants)
    assertEquals(EventVisibility.PRIVATE, event.visibility)
    assertEquals("owner456", event.ownerId)
    assertTrue(event.partOfASerie)
    assertEquals("group456", event.groupId)
  }

  @Test
  fun `toEvent handles entity with no location`() {
    val entityWithoutLocation =
        EventEntity(
            eventId = "789",
            type = "SOCIAL",
            title = "Coffee Chat",
            description = "Online coffee chat",
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            dateSeconds = sampleTimestamp.seconds,
            dateNanoseconds = sampleTimestamp.nanoseconds,
            duration = 60,
            participantsJson = "[\"user1\"]",
            maxParticipants = 2,
            visibility = "PUBLIC",
            ownerId = "owner789",
            partOfASerie = false,
            groupId = null)

    val event = entityWithoutLocation.toEvent()

    assertNull(event.location)
  }

  @Test
  fun `toEvent handles entity with partial location (missing latitude)`() {
    val entityWithPartialLocation =
        EventEntity(
            eventId = "789",
            type = "SOCIAL",
            title = "Coffee Chat",
            description = "Online coffee chat",
            locationLatitude = null,
            locationLongitude = 7.0,
            locationName = "Alps",
            dateSeconds = sampleTimestamp.seconds,
            dateNanoseconds = sampleTimestamp.nanoseconds,
            duration = 60,
            participantsJson = "[\"user1\"]",
            maxParticipants = 2,
            visibility = "PUBLIC",
            ownerId = "owner789",
            partOfASerie = false,
            groupId = null)

    val event = entityWithPartialLocation.toEvent()

    // Should be null because not all location fields are present
    assertNull(event.location)
  }

  @Test
  fun `toEvent handles empty participants json`() {
    val entityWithoutParticipants =
        EventEntity(
            eventId = "101",
            type = "SPORTS",
            title = "Solo Run",
            description = "Morning jog",
            locationLatitude = 46.5,
            locationLongitude = 6.5,
            locationName = "Park",
            dateSeconds = sampleTimestamp.seconds,
            dateNanoseconds = sampleTimestamp.nanoseconds,
            duration = 30,
            participantsJson = "[]",
            maxParticipants = 1,
            visibility = "PUBLIC",
            ownerId = "owner101",
            partOfASerie = false,
            groupId = null)

    val event = entityWithoutParticipants.toEvent()

    assertTrue(event.participants.isEmpty())
  }

  @Test
  fun `roundtrip conversion preserves all data`() {
    val entity = sampleEvent.toEntity()
    val reconstructedEvent = entity.toEvent()

    assertEquals(sampleEvent.eventId, reconstructedEvent.eventId)
    assertEquals(sampleEvent.type, reconstructedEvent.type)
    assertEquals(sampleEvent.title, reconstructedEvent.title)
    assertEquals(sampleEvent.description, reconstructedEvent.description)
    assertEquals(sampleEvent.location, reconstructedEvent.location)
    assertEquals(sampleEvent.date.seconds, reconstructedEvent.date.seconds)
    assertEquals(sampleEvent.date.nanoseconds, reconstructedEvent.date.nanoseconds)
    assertEquals(sampleEvent.duration, reconstructedEvent.duration)
    assertEquals(sampleEvent.participants, reconstructedEvent.participants)
    assertEquals(sampleEvent.maxParticipants, reconstructedEvent.maxParticipants)
    assertEquals(sampleEvent.visibility, reconstructedEvent.visibility)
    assertEquals(sampleEvent.ownerId, reconstructedEvent.ownerId)
    assertEquals(sampleEvent.partOfASerie, reconstructedEvent.partOfASerie)
    assertEquals(sampleEvent.groupId, reconstructedEvent.groupId)
  }

  @Test
  fun `roundtrip conversion with null location preserves data`() {
    val eventWithoutLocation = sampleEvent.copy(location = null)
    val entity = eventWithoutLocation.toEntity()
    val reconstructedEvent = entity.toEvent()

    assertNull(reconstructedEvent.location)
    assertEquals(eventWithoutLocation.eventId, reconstructedEvent.eventId)
    assertEquals(eventWithoutLocation.title, reconstructedEvent.title)
  }

  @Test
  fun `roundtrip conversion with null groupId preserves data`() {
    val eventWithoutGroup = sampleEvent.copy(groupId = null)
    val entity = eventWithoutGroup.toEntity()
    val reconstructedEvent = entity.toEvent()

    assertNull(reconstructedEvent.groupId)
    assertEquals(eventWithoutGroup.eventId, reconstructedEvent.eventId)
  }

  @Test
  fun `EventEntity data class properties work correctly`() {
    val entity1 =
        EventEntity(
            eventId = "123",
            type = "SPORTS",
            title = "Test",
            description = "Desc",
            locationLatitude = 1.0,
            locationLongitude = 2.0,
            locationName = "Place",
            dateSeconds = 100,
            dateNanoseconds = 200,
            duration = 60,
            participantsJson = "[]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            ownerId = "owner1",
            partOfASerie = false,
            groupId = null,
            cachedAt = 12345)

    val entity2 = entity1.copy()

    assertEquals(entity1, entity2)
    assertEquals(entity1.hashCode(), entity2.hashCode())
  }

  @Test
  fun `EventEntity copy works correctly`() {
    val entity =
        EventEntity(
            eventId = "123",
            type = "SPORTS",
            title = "Test",
            description = "Desc",
            locationLatitude = 1.0,
            locationLongitude = 2.0,
            locationName = "Place",
            dateSeconds = 100,
            dateNanoseconds = 200,
            duration = 60,
            participantsJson = "[]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            ownerId = "owner1",
            partOfASerie = false,
            groupId = null,
            cachedAt = 12345)

    val copiedEntity = entity.copy(title = "New Title")

    assertEquals("New Title", copiedEntity.title)
    assertEquals(entity.eventId, copiedEntity.eventId)
    assertEquals(entity.description, copiedEntity.description)
  }
}
