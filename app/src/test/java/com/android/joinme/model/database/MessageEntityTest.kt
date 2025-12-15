package com.android.joinme.model.database

import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.map.Location
import org.junit.Assert.*
import org.junit.Test

class MessageEntityTest {

  private val sampleLocation = Location(46.5191, 6.5668, "EPFL")

  private val sampleMessage =
      Message(
          id = "msg123",
          conversationId = "conv456",
          senderId = "user1",
          senderName = "John Doe",
          content = "Hello, World!",
          timestamp = 1234567890L,
          type = MessageType.TEXT,
          readBy = listOf("user1", "user2", "user3"),
          isPinned = false,
          isEdited = false,
          location = null)

  @Test
  fun `toEntity converts Message to MessageEntity correctly`() {
    val entity = sampleMessage.toEntity()

    assertEquals("msg123", entity.id)
    assertEquals("conv456", entity.conversationId)
    assertEquals("user1", entity.senderId)
    assertEquals("John Doe", entity.senderName)
    assertEquals("Hello, World!", entity.content)
    assertEquals(1234567890L, entity.timestamp)
    assertEquals("TEXT", entity.type)
    assertEquals("[\"user1\",\"user2\",\"user3\"]", entity.readByJson)
    assertFalse(entity.isPinned)
    assertFalse(entity.isEdited)
    assertNull(entity.locationLatitude)
    assertNull(entity.locationLongitude)
    assertNull(entity.locationName)
    assertTrue(entity.cachedAt > 0)
  }

  @Test
  fun `toEntity handles message with location correctly`() {
    val messageWithLocation = sampleMessage.copy(location = sampleLocation)
    val entity = messageWithLocation.toEntity()

    assertEquals(46.5191, entity.locationLatitude!!, 0.0001)
    assertEquals(6.5668, entity.locationLongitude!!, 0.0001)
    assertEquals("EPFL", entity.locationName)
  }

  @Test
  fun `toEntity handles empty readBy list`() {
    val messageWithoutReaders = sampleMessage.copy(readBy = emptyList())
    val entity = messageWithoutReaders.toEntity()

    assertEquals("[]", entity.readByJson)
  }

  @Test
  fun `toEntity handles single user in readBy list`() {
    val messageWithOneReader = sampleMessage.copy(readBy = listOf("user1"))
    val entity = messageWithOneReader.toEntity()

    assertEquals("[\"user1\"]", entity.readByJson)
  }

  @Test
  fun `toEntity handles different message types`() {
    assertEquals("TEXT", sampleMessage.copy(type = MessageType.TEXT).toEntity().type)
    assertEquals("SYSTEM", sampleMessage.copy(type = MessageType.SYSTEM).toEntity().type)
    assertEquals("IMAGE", sampleMessage.copy(type = MessageType.IMAGE).toEntity().type)
    assertEquals("POLL", sampleMessage.copy(type = MessageType.POLL).toEntity().type)
    assertEquals("LOCATION", sampleMessage.copy(type = MessageType.LOCATION).toEntity().type)
  }

  @Test
  fun `toEntity handles pinned and edited flags`() {
    val pinnedMessage = sampleMessage.copy(isPinned = true, isEdited = true)
    val entity = pinnedMessage.toEntity()

    assertTrue(entity.isPinned)
    assertTrue(entity.isEdited)
  }

  @Test
  fun `toMessage converts MessageEntity back to Message correctly`() {
    val entity =
        MessageEntity(
            id = "msg789",
            conversationId = "conv123",
            senderId = "user2",
            senderName = "Jane Smith",
            content = "Test message",
            timestamp = 9876543210L,
            type = "IMAGE",
            readByJson = "[\"user1\",\"user2\"]",
            isPinned = true,
            isEdited = false,
            locationLatitude = 46.0,
            locationLongitude = 7.0,
            locationName = "Alps",
            cachedAt = System.currentTimeMillis())

    val message = entity.toMessage()

    assertEquals("msg789", message.id)
    assertEquals("conv123", message.conversationId)
    assertEquals("user2", message.senderId)
    assertEquals("Jane Smith", message.senderName)
    assertEquals("Test message", message.content)
    assertEquals(9876543210L, message.timestamp)
    assertEquals(MessageType.IMAGE, message.type)
    assertEquals(listOf("user1", "user2"), message.readBy)
    assertTrue(message.isPinned)
    assertFalse(message.isEdited)
    assertNotNull(message.location)
    assertEquals(46.0, message.location!!.latitude, 0.0001)
    assertEquals(7.0, message.location!!.longitude, 0.0001)
    assertEquals("Alps", message.location!!.name)
  }

  @Test
  fun `toMessage handles entity with no location`() {
    val entityWithoutLocation =
        MessageEntity(
            id = "msg999",
            conversationId = "conv999",
            senderId = "user3",
            senderName = "Bob",
            content = "No location message",
            timestamp = 1111111111L,
            type = "TEXT",
            readByJson = "[\"user1\"]",
            isPinned = false,
            isEdited = false,
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            cachedAt = System.currentTimeMillis())

    val message = entityWithoutLocation.toMessage()

    assertNull(message.location)
  }

  @Test
  fun `toMessage handles entity with partial location (missing latitude)`() {
    val entityWithPartialLocation =
        MessageEntity(
            id = "msg888",
            conversationId = "conv888",
            senderId = "user4",
            senderName = "Alice",
            content = "Partial location",
            timestamp = 2222222222L,
            type = "LOCATION",
            readByJson = "[]",
            isPinned = false,
            isEdited = false,
            locationLatitude = null,
            locationLongitude = 7.0,
            locationName = "Alps",
            cachedAt = System.currentTimeMillis())

    val message = entityWithPartialLocation.toMessage()

    // Should be null because not all location fields are present
    assertNull(message.location)
  }

  @Test
  fun `toMessage handles entity with partial location (missing longitude)`() {
    val entityWithPartialLocation =
        MessageEntity(
            id = "msg777",
            conversationId = "conv777",
            senderId = "user5",
            senderName = "Charlie",
            content = "Partial location 2",
            timestamp = 3333333333L,
            type = "LOCATION",
            readByJson = "[]",
            isPinned = false,
            isEdited = false,
            locationLatitude = 46.0,
            locationLongitude = null,
            locationName = "Alps",
            cachedAt = System.currentTimeMillis())

    val message = entityWithPartialLocation.toMessage()

    assertNull(message.location)
  }

  @Test
  fun `toMessage handles entity with partial location (missing name)`() {
    val entityWithPartialLocation =
        MessageEntity(
            id = "msg666",
            conversationId = "conv666",
            senderId = "user6",
            senderName = "Diana",
            content = "Partial location 3",
            timestamp = 4444444444L,
            type = "LOCATION",
            readByJson = "[]",
            isPinned = false,
            isEdited = false,
            locationLatitude = 46.0,
            locationLongitude = 7.0,
            locationName = null,
            cachedAt = System.currentTimeMillis())

    val message = entityWithPartialLocation.toMessage()

    assertNull(message.location)
  }

  @Test
  fun `toMessage handles empty readBy json`() {
    val entityWithoutReaders =
        MessageEntity(
            id = "msg555",
            conversationId = "conv555",
            senderId = "user7",
            senderName = "Eve",
            content = "Unread message",
            timestamp = 5555555555L,
            type = "TEXT",
            readByJson = "[]",
            isPinned = false,
            isEdited = false,
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            cachedAt = System.currentTimeMillis())

    val message = entityWithoutReaders.toMessage()

    assertTrue(message.readBy.isEmpty())
  }

  @Test
  fun `toMessage handles invalid MessageType gracefully`() {
    val entityWithInvalidType =
        MessageEntity(
            id = "msg444",
            conversationId = "conv444",
            senderId = "user8",
            senderName = "Frank",
            content = "Invalid type message",
            timestamp = 6666666666L,
            type = "INVALID_TYPE", // Invalid type
            readByJson = "[\"user1\"]",
            isPinned = false,
            isEdited = false,
            locationLatitude = null,
            locationLongitude = null,
            locationName = null,
            cachedAt = System.currentTimeMillis())

    val message = entityWithInvalidType.toMessage()

    // Should default to TEXT when type is invalid
    assertEquals(MessageType.TEXT, message.type)
  }

  @Test
  fun `roundtrip conversion preserves all data`() {
    val entity = sampleMessage.toEntity()
    val reconstructedMessage = entity.toMessage()

    assertEquals(sampleMessage.id, reconstructedMessage.id)
    assertEquals(sampleMessage.conversationId, reconstructedMessage.conversationId)
    assertEquals(sampleMessage.senderId, reconstructedMessage.senderId)
    assertEquals(sampleMessage.senderName, reconstructedMessage.senderName)
    assertEquals(sampleMessage.content, reconstructedMessage.content)
    assertEquals(sampleMessage.timestamp, reconstructedMessage.timestamp)
    assertEquals(sampleMessage.type, reconstructedMessage.type)
    assertEquals(sampleMessage.readBy, reconstructedMessage.readBy)
    assertEquals(sampleMessage.isPinned, reconstructedMessage.isPinned)
    assertEquals(sampleMessage.isEdited, reconstructedMessage.isEdited)
    assertEquals(sampleMessage.location, reconstructedMessage.location)
  }

  @Test
  fun `roundtrip conversion with location preserves data`() {
    val messageWithLocation = sampleMessage.copy(location = sampleLocation)
    val entity = messageWithLocation.toEntity()
    val reconstructedMessage = entity.toMessage()

    assertNotNull(reconstructedMessage.location)
    assertEquals(sampleLocation.latitude, reconstructedMessage.location!!.latitude, 0.0001)
    assertEquals(sampleLocation.longitude, reconstructedMessage.location!!.longitude, 0.0001)
    assertEquals(sampleLocation.name, reconstructedMessage.location!!.name)
    assertEquals(messageWithLocation.id, reconstructedMessage.id)
    assertEquals(messageWithLocation.content, reconstructedMessage.content)
  }

  @Test
  fun `roundtrip conversion with empty readBy preserves data`() {
    val messageWithoutReaders = sampleMessage.copy(readBy = emptyList())
    val entity = messageWithoutReaders.toEntity()
    val reconstructedMessage = entity.toMessage()

    assertTrue(reconstructedMessage.readBy.isEmpty())
    assertEquals(messageWithoutReaders.id, reconstructedMessage.id)
  }

  @Test
  fun `roundtrip conversion with different message types preserves data`() {
    for (type in MessageType.values()) {
      val messageWithType = sampleMessage.copy(type = type)
      val entity = messageWithType.toEntity()
      val reconstructedMessage = entity.toMessage()

      assertEquals(type, reconstructedMessage.type)
    }
  }
}
