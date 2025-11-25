package com.android.joinme.model.chat

// Implemented with help of Claude AI

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for ChatRepositoryLocal.
 *
 * Tests all CRUD operations, Flow-based real-time updates, message read tracking, and
 * multi-conversation isolation.
 */
class ChatRepositoryLocalTest {

  private lateinit var repo: ChatRepositoryLocal
  private lateinit var sampleMessage: Message
  private val testConversationId = "conversation1"

  @Before
  fun setup() {
    repo = ChatRepositoryLocal()
    sampleMessage =
        Message(
            id = "1",
            conversationId = testConversationId,
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
  fun addMessage_appearsInObservedFlow() = runTest {
    repo.addMessage(sampleMessage)

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertEquals(1, messages.size)
    Assert.assertEquals(sampleMessage.content, messages[0].content)
  }

  @Test
  fun editMessage_updatesSuccessfully() = runTest {
    repo.addMessage(sampleMessage)
    val updated = sampleMessage.copy(content = "Updated content")
    repo.editMessage(testConversationId, "1", updated)

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertEquals("Updated content", messages[0].content)
  }

  @Test(expected = Exception::class)
  fun editMessage_wrongConversation_throwsException() = runTest {
    repo.addMessage(sampleMessage)
    val updated = sampleMessage.copy(content = "Updated content")
    repo.editMessage("wrongConversation", "1", updated)
  }

  @Test
  fun deleteMessage_removesSuccessfully() = runTest {
    repo.addMessage(sampleMessage)
    repo.deleteMessage(testConversationId, "1")

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertTrue(messages.isEmpty())
  }

  @Test(expected = Exception::class)
  fun deleteMessage_wrongConversation_throwsException() = runTest {
    repo.addMessage(sampleMessage)
    repo.deleteMessage("wrongConversation", "1")
  }

  // ---------------- OBSERVE MESSAGES ----------------

  @Test
  fun observeMessages_emitsInitialEmptyList() = runTest {
    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertTrue(messages.isEmpty())
  }

  @Test
  fun observeMessages_returnsAddedMessages() = runTest {
    repo.addMessage(sampleMessage)

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertEquals(1, messages.size)
    Assert.assertEquals("Hello, world!", messages[0].content)
  }

  @Test
  fun observeMessages_filtersCorrectConversation() = runTest {
    val conv1Message = sampleMessage.copy(id = "1", conversationId = "conversation1")
    val conv2Message =
        sampleMessage.copy(
            id = "2", conversationId = "conversation2", content = "Different conversation")

    repo.addMessage(conv1Message)
    repo.addMessage(conv2Message)

    val messages = repo.observeMessagesForConversation("conversation1").first()
    Assert.assertEquals(1, messages.size)
    Assert.assertEquals("conversation1", messages[0].conversationId)
    Assert.assertEquals("Hello, world!", messages[0].content)
  }

  @Test
  fun observeMessages_sortsByTimestamp() = runTest {
    val msg1 =
        sampleMessage.copy(
            id = "1", conversationId = testConversationId, timestamp = 3000L, content = "Third")
    val msg2 =
        sampleMessage.copy(
            id = "2", conversationId = testConversationId, timestamp = 1000L, content = "First")
    val msg3 =
        sampleMessage.copy(
            id = "3", conversationId = testConversationId, timestamp = 2000L, content = "Second")

    repo.addMessage(msg1)
    repo.addMessage(msg2)
    repo.addMessage(msg3)

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertEquals(3, messages.size)
    Assert.assertEquals("First", messages[0].content)
    Assert.assertEquals("Second", messages[1].content)
    Assert.assertEquals("Third", messages[2].content)
  }

  // ---------------- MARK AS READ ----------------

  @Test
  fun markMessageAsRead_addsUserToReadByList() = runTest {
    repo.addMessage(sampleMessage)
    repo.markMessageAsRead(testConversationId, "1", "user2")

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertTrue(messages[0].readBy.contains("user2"))
  }

  @Test
  fun markMessageAsRead_doesNotAddDuplicateUser() = runTest {
    val messageWithReader = sampleMessage.copy(readBy = listOf("user2"))
    repo.addMessage(messageWithReader)

    repo.markMessageAsRead(testConversationId, "1", "user2")

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertEquals(1, messages[0].readBy.size)
    Assert.assertEquals("user2", messages[0].readBy[0])
  }

  @Test
  fun markMessageAsRead_multipleUsers() = runTest {
    repo.addMessage(sampleMessage)
    repo.markMessageAsRead(testConversationId, "1", "user2")
    repo.markMessageAsRead(testConversationId, "1", "user3")
    repo.markMessageAsRead(testConversationId, "1", "user4")

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertEquals(3, messages[0].readBy.size)
    Assert.assertTrue(messages[0].readBy.containsAll(listOf("user2", "user3", "user4")))
  }

  @Test(expected = Exception::class)
  fun markMessageAsRead_notFound_throwsException() = runTest {
    repo.markMessageAsRead(testConversationId, "unknown", "user1")
  }

  @Test(expected = Exception::class)
  fun markMessageAsRead_wrongConversation_throwsException() = runTest {
    repo.addMessage(sampleMessage)
    repo.markMessageAsRead("wrongConversation", "1", "user2")
  }

  @Test
  fun markMessageAsRead_updatesObservableFlow() = runTest {
    repo.addMessage(sampleMessage)
    repo.markMessageAsRead(testConversationId, "1", "user2")

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertTrue(messages[0].readBy.contains("user2"))
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
  fun addMultipleMessages_storesAll() = runTest {
    val msg1 = sampleMessage.copy(id = "1", conversationId = testConversationId)
    val msg2 =
        sampleMessage.copy(id = "2", conversationId = testConversationId, content = "Message 2")
    val msg3 =
        sampleMessage.copy(id = "3", conversationId = testConversationId, content = "Message 3")
    repo.addMessage(msg1)
    repo.addMessage(msg2)
    repo.addMessage(msg3)

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertEquals(3, messages.size)
  }

  @Test
  fun getMessage_preservesAllProperties() = runTest {
    repo.addMessage(sampleMessage)
    val messages = repo.observeMessagesForConversation(testConversationId).first()
    val retrieved = messages[0]

    Assert.assertEquals(sampleMessage.id, retrieved.id)
    Assert.assertEquals(sampleMessage.conversationId, retrieved.conversationId)
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
        sampleMessage.copy(
            id = "2",
            conversationId = testConversationId,
            type = MessageType.SYSTEM,
            content = "User joined")
    repo.addMessage(systemMessage)

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    val retrieved = messages.find { it.id == "2" }!!
    Assert.assertEquals(MessageType.SYSTEM, retrieved.type)
  }

  @Test(expected = Exception::class)
  fun editMessage_notFound_throwsException() = runTest {
    val fake = sampleMessage.copy(id = "999", conversationId = testConversationId)
    repo.editMessage(testConversationId, fake.id, fake)
  }

  @Test(expected = Exception::class)
  fun deleteMessage_notFound_throwsException() = runTest {
    repo.deleteMessage(testConversationId, "nonexistent")
  }

  @Test
  fun editMessage_updatesObservableFlow() = runTest {
    repo.addMessage(sampleMessage)
    val updated = sampleMessage.copy(content = "Edited content")
    repo.editMessage(testConversationId, "1", updated)

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertEquals("Edited content", messages[0].content)
  }

  @Test
  fun deleteMessage_updatesObservableFlow() = runTest {
    repo.addMessage(sampleMessage)
    repo.deleteMessage(testConversationId, "1")

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    Assert.assertTrue(messages.isEmpty())
  }

  @Test
  fun observeMessages_emptyForNonExistentConversation() = runTest {
    repo.addMessage(sampleMessage.copy(conversationId = "conversation1"))

    val messages = repo.observeMessagesForConversation("nonexistent_conversation").first()
    Assert.assertTrue(messages.isEmpty())
  }

  @Test
  fun observeMessages_multipleConversationsIndependent() = runTest {
    val conv1Msg =
        sampleMessage.copy(id = "1", conversationId = "conversation1", content = "Conversation 1")
    val conv2Msg =
        sampleMessage.copy(id = "2", conversationId = "conversation2", content = "Conversation 2")

    repo.addMessage(conv1Msg)
    repo.addMessage(conv2Msg)

    val conv1Messages = repo.observeMessagesForConversation("conversation1").first()
    val conv2Messages = repo.observeMessagesForConversation("conversation2").first()

    Assert.assertEquals(1, conv1Messages.size)
    Assert.assertEquals(1, conv2Messages.size)
    Assert.assertEquals("Conversation 1", conv1Messages[0].content)
    Assert.assertEquals("Conversation 2", conv2Messages[0].content)
  }

  @Test
  fun markMessageAsRead_preservesOtherFields() = runTest {
    repo.addMessage(sampleMessage)
    repo.markMessageAsRead(testConversationId, "1", "user2")

    val messages = repo.observeMessagesForConversation(testConversationId).first()
    val message = messages[0]

    // Verify readBy is updated
    Assert.assertTrue(message.readBy.contains("user2"))

    // Verify other fields are preserved
    Assert.assertEquals("1", message.id)
    Assert.assertEquals(testConversationId, message.conversationId)
    Assert.assertEquals("user1", message.senderId)
    Assert.assertEquals("Alice", message.senderName)
    Assert.assertEquals("Hello, world!", message.content)
    Assert.assertEquals(1000L, message.timestamp)
    Assert.assertEquals(MessageType.TEXT, message.type)
    Assert.assertEquals(false, message.isPinned)
  }
}
