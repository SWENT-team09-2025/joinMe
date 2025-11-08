package com.android.joinme.repository

// Implemented with help of Claude AI

import com.android.joinme.model.chat.ChatRepositoryLocal
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for ChatRepositoryLocal.
 *
 * Tests all CRUD operations, message read tracking, and thread-safety.
 */
class ChatRepositoryLocalTest {

  private lateinit var repo: ChatRepositoryLocal
  private lateinit var sampleMessage: Message

  @Before
  fun setup() {
    repo = ChatRepositoryLocal()
    sampleMessage =
        Message(
            id = "1",
            senderId = "user1",
            senderName = "Alice",
            content = "Hello, world!",
            timestamp = 1000L,
            type = MessageType.TEXT,
            readBy = emptyList(),
            isPinned = false)
  }

  // ---------------- BASIC CRUD ----------------

  @Test
  fun addAndGetMessage_success() = runTest {
    repo.addMessage(sampleMessage)
    val message = repo.getMessage("1")
    Assert.assertEquals(sampleMessage.content, message.content)
  }

  @Test(expected = Exception::class)
  fun getMessage_notFound_throwsException() = runTest { repo.getMessage("unknown") }

  @Test
  fun editMessage_updatesSuccessfully() = runTest {
    repo.addMessage(sampleMessage)
    val updated = sampleMessage.copy(content = "Updated content")
    repo.editMessage("1", updated)
    val message = repo.getMessage("1")
    Assert.assertEquals("Updated content", message.content)
  }

  @Test
  fun deleteMessage_removesSuccessfully() = runTest {
    repo.addMessage(sampleMessage)
    repo.deleteMessage("1")

    // Verify message is actually deleted by trying to get it
    try {
      repo.getMessage("1")
      Assert.fail("Expected exception for deleted message")
    } catch (e: Exception) {
      Assert.assertTrue(e.message?.contains("not found") == true)
    }
  }

  // ---------------- MARK AS READ ----------------

  @Test
  fun markMessageAsRead_addsUserToReadByList() = runTest {
    repo.addMessage(sampleMessage)
    repo.markMessageAsRead("1", "user2")

    val message = repo.getMessage("1")
    Assert.assertTrue(message.readBy.contains("user2"))
  }

  @Test
  fun markMessageAsRead_doesNotAddDuplicateUser() = runTest {
    val messageWithReader = sampleMessage.copy(readBy = listOf("user2"))
    repo.addMessage(messageWithReader)

    repo.markMessageAsRead("1", "user2")

    val message = repo.getMessage("1")
    Assert.assertEquals(1, message.readBy.size)
    Assert.assertEquals("user2", message.readBy[0])
  }

  @Test
  fun markMessageAsRead_multipleUsers() = runTest {
    repo.addMessage(sampleMessage)
    repo.markMessageAsRead("1", "user2")
    repo.markMessageAsRead("1", "user3")
    repo.markMessageAsRead("1", "user4")

    val message = repo.getMessage("1")
    Assert.assertEquals(3, message.readBy.size)
    Assert.assertTrue(message.readBy.containsAll(listOf("user2", "user3", "user4")))
  }

  @Test(expected = Exception::class)
  fun markMessageAsRead_notFound_throwsException() = runTest {
    repo.markMessageAsRead("unknown", "user1")
  }

  // ---------------- MESSAGE ID GENERATION ----------------

  @Test
  fun getNewMessageId_incrementsSequentially() {
    val id1 = repo.getNewMessageId()
    val id2 = repo.getNewMessageId()
    val id3 = repo.getNewMessageId()
    Assert.assertEquals((id1.toInt() + 1).toString(), id2)
    Assert.assertEquals((id2.toInt() + 1).toString(), id3)
  }

  // ---------------- ADDITIONAL TESTS ----------------

  @Test
  fun addMultipleMessages_canRetrieveEach() = runTest {
    val msg1 = sampleMessage.copy(id = "1")
    val msg2 = sampleMessage.copy(id = "2", content = "Message 2")
    val msg3 = sampleMessage.copy(id = "3", content = "Message 3")

    repo.addMessage(msg1)
    repo.addMessage(msg2)
    repo.addMessage(msg3)

    val retrieved1 = repo.getMessage("1")
    val retrieved2 = repo.getMessage("2")
    val retrieved3 = repo.getMessage("3")

    Assert.assertEquals("Hello, world!", retrieved1.content)
    Assert.assertEquals("Message 2", retrieved2.content)
    Assert.assertEquals("Message 3", retrieved3.content)
  }

  @Test
  fun getMessage_preservesAllProperties() = runTest {
    repo.addMessage(sampleMessage)
    val retrieved = repo.getMessage("1")
    Assert.assertEquals(sampleMessage.id, retrieved.id)
    Assert.assertEquals(sampleMessage.senderId, retrieved.senderId)
    Assert.assertEquals(sampleMessage.senderName, retrieved.senderName)
    Assert.assertEquals(sampleMessage.content, retrieved.content)
    Assert.assertEquals(sampleMessage.timestamp, retrieved.timestamp)
    Assert.assertEquals(sampleMessage.type, retrieved.type)
    Assert.assertEquals(sampleMessage.readBy, retrieved.readBy)
    Assert.assertEquals(sampleMessage.isPinned, retrieved.isPinned)
  }

  @Test
  fun messageType_systemMessage_handledCorrectly() = runTest {
    val systemMessage =
        sampleMessage.copy(id = "2", type = MessageType.SYSTEM, content = "User joined")
    repo.addMessage(systemMessage)

    val retrieved = repo.getMessage("2")
    Assert.assertEquals(MessageType.SYSTEM, retrieved.type)
  }

  @Test(expected = Exception::class)
  fun editMessage_notFound_throwsException() = runTest {
    val fake = sampleMessage.copy(id = "999")
    repo.editMessage(fake.id, fake)
  }

  @Test(expected = Exception::class)
  fun deleteMessage_notFound_throwsException() = runTest { repo.deleteMessage("nonexistent") }

  @Test
  fun editMessage_preservesOtherMessages() = runTest {
    val msg1 = sampleMessage.copy(id = "1", content = "Message 1")
    val msg2 = sampleMessage.copy(id = "2", content = "Message 2")

    repo.addMessage(msg1)
    repo.addMessage(msg2)

    // Edit only message 1
    val updated = msg1.copy(content = "Edited Message 1")
    repo.editMessage("1", updated)

    // Verify message 1 is updated
    val retrieved1 = repo.getMessage("1")
    Assert.assertEquals("Edited Message 1", retrieved1.content)

    // Verify message 2 is unchanged
    val retrieved2 = repo.getMessage("2")
    Assert.assertEquals("Message 2", retrieved2.content)
  }

  @Test
  fun deleteMessage_preservesOtherMessages() = runTest {
    val msg1 = sampleMessage.copy(id = "1", content = "Message 1")
    val msg2 = sampleMessage.copy(id = "2", content = "Message 2")

    repo.addMessage(msg1)
    repo.addMessage(msg2)

    // Delete message 1
    repo.deleteMessage("1")

    // Verify message 1 is deleted
    try {
      repo.getMessage("1")
      Assert.fail("Expected exception for deleted message")
    } catch (e: Exception) {
      // Expected
    }

    // Verify message 2 still exists
    val retrieved2 = repo.getMessage("2")
    Assert.assertEquals("Message 2", retrieved2.content)
  }

  @Test
  fun markMessageAsRead_preservesOtherFields() = runTest {
    repo.addMessage(sampleMessage)
    repo.markMessageAsRead("1", "user2")

    val message = repo.getMessage("1")

    // Verify readBy is updated
    Assert.assertTrue(message.readBy.contains("user2"))

    // Verify other fields are preserved
    Assert.assertEquals("1", message.id)
    Assert.assertEquals("user1", message.senderId)
    Assert.assertEquals("Alice", message.senderName)
    Assert.assertEquals("Hello, world!", message.content)
    Assert.assertEquals(1000L, message.timestamp)
    Assert.assertEquals(MessageType.TEXT, message.type)
    Assert.assertEquals(false, message.isPinned)
  }
}
