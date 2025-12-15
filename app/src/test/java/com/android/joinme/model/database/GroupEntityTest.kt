package com.android.joinme.model.database

// Tests partially written with AI assistance; reviewed for correctness.

import com.android.joinme.model.event.EventType
import com.android.joinme.model.groups.Group
import org.junit.Assert.*
import org.junit.Test

class GroupEntityTest {

  private val testGroup =
      Group(
          id = "test-group-123",
          name = "Test Group",
          category = EventType.SPORTS,
          description = "A test group",
          ownerId = "owner-123",
          memberIds = listOf("member1", "member2", "member3"),
          eventIds = listOf("event1", "event2"),
          serieIds = listOf("serie1", "serie2", "serie3"),
          photoUrl = "https://example.com/photo.jpg")

  // ==================== Group.toEntity() Tests ====================

  @Test
  fun `toEntity converts all fields correctly`() {
    // When
    val entity = testGroup.toEntity()

    // Then
    assertEquals(testGroup.id, entity.id)
    assertEquals(testGroup.name, entity.name)
    assertEquals(testGroup.category.name, entity.category)
    assertEquals(testGroup.description, entity.description)
    assertEquals(testGroup.ownerId, entity.ownerId)
    assertEquals(testGroup.photoUrl, entity.photoUrl)
  }

  @Test
  fun `toEntity converts memberIds to JSON array format`() {
    // When
    val entity = testGroup.toEntity()

    // Then
    assertEquals("[\"member1\",\"member2\",\"member3\"]", entity.memberIdsJson)
  }

  @Test
  fun `toEntity converts empty memberIds list to empty JSON array`() {
    // Given
    val group = testGroup.copy(memberIds = emptyList())

    // When
    val entity = group.toEntity()

    // Then
    assertEquals("[]", entity.memberIdsJson)
  }

  @Test
  fun `toEntity converts single memberId correctly`() {
    // Given
    val group = testGroup.copy(memberIds = listOf("member1"))

    // When
    val entity = group.toEntity()

    // Then
    assertEquals("[\"member1\"]", entity.memberIdsJson)
  }

  @Test
  fun `toEntity converts eventIds to JSON array format`() {
    // When
    val entity = testGroup.toEntity()

    // Then
    assertEquals("[\"event1\",\"event2\"]", entity.eventIdsJson)
  }

  @Test
  fun `toEntity converts empty eventIds list to empty JSON array`() {
    // Given
    val group = testGroup.copy(eventIds = emptyList())

    // When
    val entity = group.toEntity()

    // Then
    assertEquals("[]", entity.eventIdsJson)
  }

  @Test
  fun `toEntity converts serieIds to JSON array format`() {
    // When
    val entity = testGroup.toEntity()

    // Then
    assertEquals("[\"serie1\",\"serie2\",\"serie3\"]", entity.serieIdsJson)
  }

  @Test
  fun `toEntity converts empty serieIds list to empty JSON array`() {
    // Given
    val group = testGroup.copy(serieIds = emptyList())

    // When
    val entity = group.toEntity()

    // Then
    assertEquals("[]", entity.serieIdsJson)
  }

  @Test
  fun `toEntity handles null photoUrl`() {
    // Given
    val group = testGroup.copy(photoUrl = null)

    // When
    val entity = group.toEntity()

    // Then
    assertNull(entity.photoUrl)
  }

  @Test
  fun `toEntity sets cachedAt timestamp`() {
    // Given
    val beforeTime = System.currentTimeMillis()

    // When
    val entity = testGroup.toEntity()

    // Then
    val afterTime = System.currentTimeMillis()
    assertTrue(entity.cachedAt >= beforeTime)
    assertTrue(entity.cachedAt <= afterTime)
  }

  @Test
  fun `toEntity handles all EventType categories`() {
    EventType.values().forEach { eventType ->
      // Given
      val group = testGroup.copy(category = eventType)

      // When
      val entity = group.toEntity()

      // Then
      assertEquals(eventType.name, entity.category)
    }
  }

  // ==================== GroupEntity.toGroup() Tests ====================

  @Test
  fun `toGroup converts all fields correctly`() {
    // Given
    val entity =
        GroupEntity(
            id = "test-group-123",
            name = "Test Group",
            category = "SPORTS",
            description = "A test group",
            ownerId = "owner-123",
            memberIdsJson = "[\"member1\",\"member2\",\"member3\"]",
            eventIdsJson = "[\"event1\",\"event2\"]",
            serieIdsJson = "[\"serie1\",\"serie2\",\"serie3\"]",
            photoUrl = "https://example.com/photo.jpg",
            cachedAt = System.currentTimeMillis())

    // When
    val group = entity.toGroup()

    // Then
    assertEquals(entity.id, group.id)
    assertEquals(entity.name, group.name)
    assertEquals(EventType.SPORTS, group.category)
    assertEquals(entity.description, group.description)
    assertEquals(entity.ownerId, group.ownerId)
    assertEquals(entity.photoUrl, group.photoUrl)
  }

  @Test
  fun `toGroup parses JSON memberIds correctly`() {
    // Given
    val entity =
        GroupEntity(
            id = "test-group",
            name = "Test",
            category = "ACTIVITY",
            description = "Desc",
            ownerId = "owner",
            memberIdsJson = "[\"member1\",\"member2\",\"member3\"]",
            eventIdsJson = "[]",
            serieIdsJson = "[]",
            photoUrl = null)

    // When
    val group = entity.toGroup()

    // Then
    assertEquals(listOf("member1", "member2", "member3"), group.memberIds)
  }

  @Test
  fun `toGroup parses empty JSON memberIds array`() {
    // Given
    val entity =
        GroupEntity(
            id = "test-group",
            name = "Test",
            category = "ACTIVITY",
            description = "Desc",
            ownerId = "owner",
            memberIdsJson = "[]",
            eventIdsJson = "[]",
            serieIdsJson = "[]",
            photoUrl = null)

    // When
    val group = entity.toGroup()

    // Then
    assertTrue(group.memberIds.isEmpty())
  }

  @Test
  fun `toGroup parses single memberId correctly`() {
    // Given
    val entity =
        GroupEntity(
            id = "test-group",
            name = "Test",
            category = "ACTIVITY",
            description = "Desc",
            ownerId = "owner",
            memberIdsJson = "[\"member1\"]",
            eventIdsJson = "[]",
            serieIdsJson = "[]",
            photoUrl = null)

    // When
    val group = entity.toGroup()

    // Then
    assertEquals(listOf("member1"), group.memberIds)
  }

  @Test
  fun `toGroup parses JSON eventIds correctly`() {
    // Given
    val entity =
        GroupEntity(
            id = "test-group",
            name = "Test",
            category = "ACTIVITY",
            description = "Desc",
            ownerId = "owner",
            memberIdsJson = "[]",
            eventIdsJson = "[\"event1\",\"event2\"]",
            serieIdsJson = "[]",
            photoUrl = null)

    // When
    val group = entity.toGroup()

    // Then
    assertEquals(listOf("event1", "event2"), group.eventIds)
  }

  @Test
  fun `toGroup parses JSON serieIds correctly`() {
    // Given
    val entity =
        GroupEntity(
            id = "test-group",
            name = "Test",
            category = "ACTIVITY",
            description = "Desc",
            ownerId = "owner",
            memberIdsJson = "[]",
            eventIdsJson = "[]",
            serieIdsJson = "[\"serie1\",\"serie2\",\"serie3\"]",
            photoUrl = null)

    // When
    val group = entity.toGroup()

    // Then
    assertEquals(listOf("serie1", "serie2", "serie3"), group.serieIds)
  }

  @Test
  fun `toGroup parses JSON arrays with spaces correctly`() {
    // Given
    val entity =
        GroupEntity(
            id = "test-group",
            name = "Test",
            category = "ACTIVITY",
            description = "Desc",
            ownerId = "owner",
            memberIdsJson = "[\"member1\", \"member2\", \"member3\"]",
            eventIdsJson = "[\"event1\", \"event2\"]",
            serieIdsJson = "[\"serie1\", \"serie2\"]",
            photoUrl = null)

    // When
    val group = entity.toGroup()

    // Then
    assertEquals(listOf("member1", "member2", "member3"), group.memberIds)
    assertEquals(listOf("event1", "event2"), group.eventIds)
    assertEquals(listOf("serie1", "serie2"), group.serieIds)
  }

  @Test
  fun `toGroup handles null photoUrl`() {
    // Given
    val entity =
        GroupEntity(
            id = "test-group",
            name = "Test",
            category = "ACTIVITY",
            description = "Desc",
            ownerId = "owner",
            memberIdsJson = "[]",
            eventIdsJson = "[]",
            serieIdsJson = "[]",
            photoUrl = null)

    // When
    val group = entity.toGroup()

    // Then
    assertNull(group.photoUrl)
  }

  @Test
  fun `toGroup converts all EventType categories correctly`() {
    EventType.values().forEach { eventType ->
      // Given
      val entity =
          GroupEntity(
              id = "test-group",
              name = "Test",
              category = eventType.name,
              description = "Desc",
              ownerId = "owner",
              memberIdsJson = "[]",
              eventIdsJson = "[]",
              serieIdsJson = "[]",
              photoUrl = null)

      // When
      val group = entity.toGroup()

      // Then
      assertEquals(eventType, group.category)
    }
  }

  // ==================== Round-trip Conversion Tests ====================

  @Test
  fun `round-trip conversion preserves all data`() {
    // Given
    val originalGroup = testGroup

    // When
    val entity = originalGroup.toEntity()
    val convertedGroup = entity.toGroup()

    // Then
    assertEquals(originalGroup.id, convertedGroup.id)
    assertEquals(originalGroup.name, convertedGroup.name)
    assertEquals(originalGroup.category, convertedGroup.category)
    assertEquals(originalGroup.description, convertedGroup.description)
    assertEquals(originalGroup.ownerId, convertedGroup.ownerId)
    assertEquals(originalGroup.memberIds, convertedGroup.memberIds)
    assertEquals(originalGroup.eventIds, convertedGroup.eventIds)
    assertEquals(originalGroup.serieIds, convertedGroup.serieIds)
    assertEquals(originalGroup.photoUrl, convertedGroup.photoUrl)
  }

  @Test
  fun `round-trip conversion with minimal group data`() {
    // Given
    val originalGroup =
        Group(
            id = "test-group",
            name = "Test",
            category = EventType.ACTIVITY,
            description = "Desc",
            ownerId = "owner",
            memberIds = emptyList(),
            eventIds = emptyList(),
            serieIds = emptyList(),
            photoUrl = null)

    // When
    val entity = originalGroup.toEntity()
    val convertedGroup = entity.toGroup()

    // Then
    assertEquals(originalGroup.id, convertedGroup.id)
    assertEquals(originalGroup.name, convertedGroup.name)
    assertEquals(originalGroup.category, convertedGroup.category)
    assertEquals(originalGroup.description, convertedGroup.description)
    assertEquals(originalGroup.ownerId, convertedGroup.ownerId)
    assertTrue(convertedGroup.memberIds.isEmpty())
    assertTrue(convertedGroup.eventIds.isEmpty())
    assertTrue(convertedGroup.serieIds.isEmpty())
    assertNull(convertedGroup.photoUrl)
  }

  @Test
  fun `round-trip conversion with empty lists`() {
    // Given
    val originalGroup =
        testGroup.copy(memberIds = emptyList(), eventIds = emptyList(), serieIds = emptyList())

    // When
    val entity = originalGroup.toEntity()
    val convertedGroup = entity.toGroup()

    // Then
    assertTrue(convertedGroup.memberIds.isEmpty())
    assertTrue(convertedGroup.eventIds.isEmpty())
    assertTrue(convertedGroup.serieIds.isEmpty())
  }

  @Test
  fun `round-trip conversion with many memberIds`() {
    // Given
    val manyMembers = (1..20).map { "member$it" }
    val originalGroup = testGroup.copy(memberIds = manyMembers)

    // When
    val entity = originalGroup.toEntity()
    val convertedGroup = entity.toGroup()

    // Then
    assertEquals(manyMembers, convertedGroup.memberIds)
  }

  @Test
  fun `round-trip conversion with many eventIds and serieIds`() {
    // Given
    val manyEvents = (1..15).map { "event$it" }
    val manySeries = (1..10).map { "serie$it" }
    val originalGroup = testGroup.copy(eventIds = manyEvents, serieIds = manySeries)

    // When
    val entity = originalGroup.toEntity()
    val convertedGroup = entity.toGroup()

    // Then
    assertEquals(manyEvents, convertedGroup.eventIds)
    assertEquals(manySeries, convertedGroup.serieIds)
  }
}
