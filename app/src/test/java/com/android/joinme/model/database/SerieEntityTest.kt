package com.android.joinme.model.database
/** This file was implemented with the help of AI * */
import com.android.joinme.model.serie.Serie
import com.android.joinme.model.utils.Visibility
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.*
import org.junit.Test

class SerieEntityTest {

  private val sampleTimestamp = Timestamp(Date())
  private val lastEventEndTimestamp = Timestamp(Date(System.currentTimeMillis() + 3600000))

  private val sampleSerie =
      Serie(
              serieId = "serie123",
              title = "Weekly Football",
              description = "Weekly football matches",
              date = sampleTimestamp,
              participants = listOf("user1", "user2", "user3"),
              maxParticipants = 10,
              visibility = Visibility.PUBLIC,
              eventIds = listOf("event1", "event2", "event3"),
              ownerId = "owner123",
              groupId = "group123")
          .copy(lastEventEndTime = lastEventEndTimestamp)

  @Test
  fun `toEntity converts Serie to SerieEntity correctly`() {
    val entity = sampleSerie.toEntity()

    assertEquals("serie123", entity.serieId)
    assertEquals("Weekly Football", entity.title)
    assertEquals("Weekly football matches", entity.description)
    assertEquals(sampleTimestamp.seconds, entity.dateSeconds)
    assertEquals(sampleTimestamp.nanoseconds, entity.dateNanoseconds)
    assertEquals("[\"user1\",\"user2\",\"user3\"]", entity.participantsJson)
    assertEquals(10, entity.maxParticipants)
    assertEquals("PUBLIC", entity.visibility)
    assertEquals("[\"event1\",\"event2\",\"event3\"]", entity.eventIdsJson)
    assertEquals("owner123", entity.ownerId)
    assertEquals(lastEventEndTimestamp.seconds, entity.lastEventEndTimeSeconds)
    assertEquals(lastEventEndTimestamp.nanoseconds, entity.lastEventEndTimeNanoseconds)
    assertEquals("group123", entity.groupId)
    assertTrue(entity.cachedAt > 0)
  }

  @Test
  fun `toEntity handles various field combinations`() {
    // Empty participants
    val emptyParticipants = sampleSerie.copy(participants = emptyList()).toEntity().participantsJson
    assertEquals("[]", emptyParticipants)

    // Single participant
    val singleParticipant =
        sampleSerie.copy(participants = listOf("user1")).toEntity().participantsJson
    assertEquals("[\"user1\"]", singleParticipant)

    // Empty event IDs
    val emptyEventIds = sampleSerie.copy(eventIds = emptyList()).toEntity().eventIdsJson
    assertEquals("[]", emptyEventIds)

    // Single event ID
    val singleEventId = sampleSerie.copy(eventIds = listOf("event1")).toEntity().eventIdsJson
    assertEquals("[\"event1\"]", singleEventId)

    // Null lastEventEndTime
    val noLastEventTime = sampleSerie.copy(lastEventEndTime = null).toEntity()
    assertNull(noLastEventTime.lastEventEndTimeSeconds)
    assertNull(noLastEventTime.lastEventEndTimeNanoseconds)

    // Null groupId
    val noGroupId = sampleSerie.copy(groupId = null).toEntity()
    assertNull(noGroupId.groupId)

    // PRIVATE visibility
    val privateVisibility = sampleSerie.copy(visibility = Visibility.PRIVATE).toEntity()
    assertEquals("PRIVATE", privateVisibility.visibility)
  }

  @Test
  fun `toSerie converts SerieEntity back to Serie correctly`() {
    val entity =
        SerieEntity(
            serieId = "serie456",
            title = "Monthly Hiking",
            description = "Monthly hiking trips",
            dateSeconds = sampleTimestamp.seconds,
            dateNanoseconds = sampleTimestamp.nanoseconds,
            participantsJson = "[\"user1\",\"user2\"]",
            maxParticipants = 5,
            visibility = "PRIVATE",
            eventIdsJson = "[\"event1\",\"event2\"]",
            ownerId = "owner456",
            lastEventEndTimeSeconds = lastEventEndTimestamp.seconds,
            lastEventEndTimeNanoseconds = lastEventEndTimestamp.nanoseconds,
            groupId = "group456",
            cachedAt = System.currentTimeMillis())

    val serie = entity.toSerie()

    assertEquals("serie456", serie.serieId)
    assertEquals("Monthly Hiking", serie.title)
    assertEquals("Monthly hiking trips", serie.description)
    assertEquals(sampleTimestamp.seconds, serie.date.seconds)
    assertEquals(sampleTimestamp.nanoseconds, serie.date.nanoseconds)
    assertEquals(listOf("user1", "user2"), serie.participants)
    assertEquals(5, serie.maxParticipants)
    assertEquals(Visibility.PRIVATE, serie.visibility)
    assertEquals(listOf("event1", "event2"), serie.eventIds)
    assertEquals("owner456", serie.ownerId)
    assertNotNull(serie.lastEventEndTime)
    assertEquals(lastEventEndTimestamp.seconds, serie.lastEventEndTime!!.seconds)
    assertEquals(lastEventEndTimestamp.nanoseconds, serie.lastEventEndTime!!.nanoseconds)
    assertEquals("group456", serie.groupId)
  }

  @Test
  fun `toSerie handles entity with null lastEventEndTime`() {
    val entityWithoutLastEventTime =
        SerieEntity(
            serieId = "serie789",
            title = "Running Club",
            description = "Weekly runs",
            dateSeconds = sampleTimestamp.seconds,
            dateNanoseconds = sampleTimestamp.nanoseconds,
            participantsJson = "[\"user1\"]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            eventIdsJson = "[\"event1\"]",
            ownerId = "owner789",
            lastEventEndTimeSeconds = null,
            lastEventEndTimeNanoseconds = null,
            groupId = null)

    val serie = entityWithoutLastEventTime.toSerie()

    assertNull(serie.lastEventEndTime)
  }

  @Test
  fun `toSerie handles entity with null groupId`() {
    val entityWithoutGroup =
        SerieEntity(
            serieId = "serie101",
            title = "Basketball",
            description = "Weekly basketball",
            dateSeconds = sampleTimestamp.seconds,
            dateNanoseconds = sampleTimestamp.nanoseconds,
            participantsJson = "[\"user1\"]",
            maxParticipants = 8,
            visibility = "PUBLIC",
            eventIdsJson = "[\"event1\"]",
            ownerId = "owner101",
            lastEventEndTimeSeconds = lastEventEndTimestamp.seconds,
            lastEventEndTimeNanoseconds = lastEventEndTimestamp.nanoseconds,
            groupId = null)

    val serie = entityWithoutGroup.toSerie()

    assertNull(serie.groupId)
  }

  @Test
  fun `toSerie handles empty participants json`() {
    val entityWithoutParticipants =
        SerieEntity(
            serieId = "serie202",
            title = "Solo Training",
            description = "Personal training sessions",
            dateSeconds = sampleTimestamp.seconds,
            dateNanoseconds = sampleTimestamp.nanoseconds,
            participantsJson = "[]",
            maxParticipants = 1,
            visibility = "PRIVATE",
            eventIdsJson = "[\"event1\"]",
            ownerId = "owner202",
            lastEventEndTimeSeconds = null,
            lastEventEndTimeNanoseconds = null,
            groupId = null)

    val serie = entityWithoutParticipants.toSerie()

    assertTrue(serie.participants.isEmpty())
  }

  @Test
  fun `toSerie handles empty eventIds json`() {
    val entityWithoutEvents =
        SerieEntity(
            serieId = "serie303",
            title = "Future Series",
            description = "Series with no events yet",
            dateSeconds = sampleTimestamp.seconds,
            dateNanoseconds = sampleTimestamp.nanoseconds,
            participantsJson = "[\"user1\"]",
            maxParticipants = 10,
            visibility = "PUBLIC",
            eventIdsJson = "[]",
            ownerId = "owner303",
            lastEventEndTimeSeconds = null,
            lastEventEndTimeNanoseconds = null,
            groupId = null)

    val serie = entityWithoutEvents.toSerie()

    assertTrue(serie.eventIds.isEmpty())
  }

  @Test
  fun `roundtrip conversion preserves all data`() {
    val entity = sampleSerie.toEntity()
    val reconstructedSerie = entity.toSerie()

    assertEquals(sampleSerie.serieId, reconstructedSerie.serieId)
    assertEquals(sampleSerie.title, reconstructedSerie.title)
    assertEquals(sampleSerie.description, reconstructedSerie.description)
    assertEquals(sampleSerie.date.seconds, reconstructedSerie.date.seconds)
    assertEquals(sampleSerie.date.nanoseconds, reconstructedSerie.date.nanoseconds)
    assertEquals(sampleSerie.participants, reconstructedSerie.participants)
    assertEquals(sampleSerie.maxParticipants, reconstructedSerie.maxParticipants)
    assertEquals(sampleSerie.visibility, reconstructedSerie.visibility)
    assertEquals(sampleSerie.eventIds, reconstructedSerie.eventIds)
    assertEquals(sampleSerie.ownerId, reconstructedSerie.ownerId)
    assertEquals(
        sampleSerie.lastEventEndTime?.seconds, reconstructedSerie.lastEventEndTime?.seconds)
    assertEquals(
        sampleSerie.lastEventEndTime?.nanoseconds, reconstructedSerie.lastEventEndTime?.nanoseconds)
    assertEquals(sampleSerie.groupId, reconstructedSerie.groupId)
  }

  @Test
  fun `roundtrip conversion with null lastEventEndTime preserves data`() {
    val serieWithoutLastEventTime = sampleSerie.copy(lastEventEndTime = null)
    val entity = serieWithoutLastEventTime.toEntity()
    val reconstructedSerie = entity.toSerie()

    assertNull(reconstructedSerie.lastEventEndTime)
    assertEquals(serieWithoutLastEventTime.serieId, reconstructedSerie.serieId)
    assertEquals(serieWithoutLastEventTime.title, reconstructedSerie.title)
  }

  @Test
  fun `roundtrip conversion with null groupId preserves data`() {
    val serieWithoutGroup = sampleSerie.copy(groupId = null)
    val entity = serieWithoutGroup.toEntity()
    val reconstructedSerie = entity.toSerie()

    assertNull(reconstructedSerie.groupId)
    assertEquals(serieWithoutGroup.serieId, reconstructedSerie.serieId)
  }

  @Test
  fun `roundtrip conversion with empty lists preserves data`() {
    val serieWithEmptyLists = sampleSerie.copy(participants = emptyList(), eventIds = emptyList())
    val entity = serieWithEmptyLists.toEntity()
    val reconstructedSerie = entity.toSerie()

    assertTrue(reconstructedSerie.participants.isEmpty())
    assertTrue(reconstructedSerie.eventIds.isEmpty())
    assertEquals(serieWithEmptyLists.serieId, reconstructedSerie.serieId)
  }
}
