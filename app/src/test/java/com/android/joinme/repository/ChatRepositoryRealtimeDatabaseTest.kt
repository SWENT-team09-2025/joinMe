package com.android.joinme.repository

// Implemented with help of Claude AI

import com.android.joinme.model.chat.ChatRepositoryRealtimeDatabase
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test suite for ChatRepositoryRealtimeDatabase.
 *
 * Tests the Firebase Realtime Database implementation of the ChatRepository interface, including
 * message CRUD operations and real-time message observation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChatRepositoryRealtimeDatabaseTest {

  private lateinit var repository: ChatRepositoryRealtimeDatabase
  private lateinit var mockDatabase: FirebaseDatabase
  private lateinit var mockConversationsRef: DatabaseReference
  private lateinit var mockConversationRef: DatabaseReference
  private lateinit var mockMessagesRef: DatabaseReference
  private lateinit var mockMessageRef: DatabaseReference

  private val testConversationId = "test-conversation-123"
  private val testMessageId = "test-message-456"

  @Before
  fun setup() {
    // Mock Firebase Realtime Database components
    mockDatabase = mockk(relaxed = true)
    mockConversationsRef = mockk(relaxed = true)
    mockConversationRef = mockk(relaxed = true)
    mockMessagesRef = mockk(relaxed = true)
    mockMessageRef = mockk(relaxed = true)

    // Setup reference chain
    every { mockDatabase.getReference("conversations") } returns mockConversationsRef
    every { mockConversationsRef.child(any()) } returns mockConversationRef
    every { mockConversationRef.child("messages") } returns mockMessagesRef
    every { mockMessagesRef.child(any()) } returns mockMessageRef
    every { mockMessagesRef.orderByChild("timestamp") } returns mockk<Query>(relaxed = true)

    repository = ChatRepositoryRealtimeDatabase(mockDatabase)
  }

  @After
  fun teardown() {
    clearAllMocks()
  }

  // ============================================================================
  // getNewMessageId Tests
  // ============================================================================

  @Test
  fun getNewMessageId_generatesUniqueId() {
    // Given
    val mockPushRef = mockk<DatabaseReference>(relaxed = true)
    every { mockConversationsRef.push() } returns mockPushRef
    every { mockPushRef.key } returns "generated-id-123"

    // When
    val messageId = repository.getNewMessageId()

    // Then
    assertNotNull(messageId)
    assertEquals("generated-id-123", messageId)
  }

  @Test
  fun getNewMessageId_fallsBackToTimestampWhenPushReturnsNull() {
    // Given
    val mockPushRef = mockk<DatabaseReference>(relaxed = true)
    every { mockConversationsRef.push() } returns mockPushRef
    every { mockPushRef.key } returns null

    // When
    val messageId = repository.getNewMessageId()

    // Then
    assertNotNull(messageId)
    assertTrue(messageId.toLongOrNull() != null) // Should be a timestamp
  }

  // ============================================================================
  // observeMessagesForConversation Tests
  // ============================================================================

  @Test
  fun observeMessagesForConversation_returnsFlowWithMessages() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockMessageSnapshot1 = mockk<DataSnapshot>(relaxed = true)
    val mockMessageSnapshot2 = mockk<DataSnapshot>(relaxed = true)

    // Setup message 1
    every { mockMessageSnapshot1.key } returns "msg1"
    every { mockMessageSnapshot1.child("senderId").getValue(String::class.java) } returns "user1"
    every { mockMessageSnapshot1.child("senderName").getValue(String::class.java) } returns "Alice"
    every { mockMessageSnapshot1.child("content").getValue(String::class.java) } returns "Hello"
    every { mockMessageSnapshot1.child("timestamp").getValue(Long::class.java) } returns 1000L
    every { mockMessageSnapshot1.child("type").getValue(String::class.java) } returns "TEXT"
    every {
      mockMessageSnapshot1
          .child("readBy")
          .getValue(any<com.google.firebase.database.GenericTypeIndicator<List<String>>>())
    } returns emptyList()
    every { mockMessageSnapshot1.child("isPinned").getValue(Boolean::class.java) } returns false

    // Setup message 2
    every { mockMessageSnapshot2.key } returns "msg2"
    every { mockMessageSnapshot2.child("senderId").getValue(String::class.java) } returns "user2"
    every { mockMessageSnapshot2.child("senderName").getValue(String::class.java) } returns "Bob"
    every { mockMessageSnapshot2.child("content").getValue(String::class.java) } returns "Hi there"
    every { mockMessageSnapshot2.child("timestamp").getValue(Long::class.java) } returns 2000L
    every { mockMessageSnapshot2.child("type").getValue(String::class.java) } returns "TEXT"
    every {
      mockMessageSnapshot2
          .child("readBy")
          .getValue(any<com.google.firebase.database.GenericTypeIndicator<List<String>>>())
    } returns emptyList()
    every { mockMessageSnapshot2.child("isPinned").getValue(Boolean::class.java) } returns false

    every { mockSnapshot.children } returns listOf(mockMessageSnapshot1, mockMessageSnapshot2)

    every { mockMessagesRef.orderByChild("timestamp") } returns mockQuery
    every { mockQuery.addValueEventListener(any()) } answers
        {
          val listener = firstArg<ValueEventListener>()
          listener.onDataChange(mockSnapshot)
          mockk(relaxed = true)
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then
    assertNotNull(messages)
    assertEquals(2, messages.size)
    assertEquals("msg1", messages[0].id)
    assertEquals("Hello", messages[0].content)
    assertEquals("msg2", messages[1].id)
    assertEquals("Hi there", messages[1].content)
  }

  @Test
  fun observeMessagesForConversation_returnsEmptyListWhenNoMessages() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)

    every { mockSnapshot.children } returns emptyList()
    every { mockMessagesRef.orderByChild("timestamp") } returns mockQuery
    every { mockQuery.addValueEventListener(any()) } answers
        {
          val listener = firstArg<ValueEventListener>()
          listener.onDataChange(mockSnapshot)
          mockk(relaxed = true)
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then
    assertNotNull(messages)
    assertEquals(0, messages.size)
  }

  @Test
  fun observeMessagesForConversation_handlesError() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockError = mockk<DatabaseError>(relaxed = true)

    every { mockMessagesRef.orderByChild("timestamp") } returns mockQuery
    every { mockQuery.addValueEventListener(any()) } answers
        {
          val listener = firstArg<ValueEventListener>()
          listener.onCancelled(mockError)
          mockk(relaxed = true)
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then - should return empty list on error
    assertNotNull(messages)
    assertEquals(0, messages.size)
  }

  @Test
  fun observeMessagesForConversation_filtersOutInvalidMessages() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockValidMessage = mockk<DataSnapshot>(relaxed = true)
    val mockInvalidMessage = mockk<DataSnapshot>(relaxed = true)

    // Valid message
    every { mockValidMessage.key } returns "msg1"
    every { mockValidMessage.child("senderId").getValue(String::class.java) } returns "user1"
    every { mockValidMessage.child("senderName").getValue(String::class.java) } returns "Alice"
    every { mockValidMessage.child("content").getValue(String::class.java) } returns "Hello"
    every { mockValidMessage.child("timestamp").getValue(Long::class.java) } returns 1000L
    every { mockValidMessage.child("type").getValue(String::class.java) } returns "TEXT"
    every {
      mockValidMessage
          .child("readBy")
          .getValue(any<com.google.firebase.database.GenericTypeIndicator<List<String>>>())
    } returns emptyList()
    every { mockValidMessage.child("isPinned").getValue(Boolean::class.java) } returns false

    // Invalid message (missing senderId)
    every { mockInvalidMessage.key } returns "msg2"
    every { mockInvalidMessage.child("senderId").getValue(String::class.java) } returns null

    every { mockSnapshot.children } returns listOf(mockValidMessage, mockInvalidMessage)
    every { mockMessagesRef.orderByChild("timestamp") } returns mockQuery
    every { mockQuery.addValueEventListener(any()) } answers
        {
          val listener = firstArg<ValueEventListener>()
          listener.onDataChange(mockSnapshot)
          mockk(relaxed = true)
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then - should only return valid message
    assertNotNull(messages)
    assertEquals(1, messages.size)
    assertEquals("msg1", messages[0].id)
  }

  // ============================================================================
  // addMessage Tests
  // ============================================================================

  @Test
  fun addMessage_successfullySavesMessage() = runTest {
    // Given
    val message =
        Message(
            id = testMessageId,
            conversationId = testConversationId,
            senderId = "user1",
            senderName = "Alice",
            content = "Test message",
            timestamp = System.currentTimeMillis(),
            type = MessageType.TEXT)

    val mockTask = Tasks.forResult<Void>(null)
    every { mockMessageRef.setValue(any()) } returns mockTask

    // When
    repository.addMessage(message)

    // Then
    verify { mockMessageRef.setValue(any()) }
  }

  // ============================================================================
  // editMessage Tests
  // ============================================================================

  @Test
  fun editMessage_successfullyUpdatesMessage() = runTest {
    // Given
    val updatedMessage =
        Message(
            id = testMessageId,
            conversationId = testConversationId,
            senderId = "user1",
            senderName = "Alice",
            content = "Updated content",
            timestamp = System.currentTimeMillis(),
            type = MessageType.TEXT)

    val mockTask = Tasks.forResult<Void>(null)
    every { mockMessageRef.updateChildren(any()) } returns mockTask

    // When
    repository.editMessage(testConversationId, testMessageId, updatedMessage)

    // Then
    verify { mockMessageRef.updateChildren(any()) }
  }

  // ============================================================================
  // deleteMessage Tests
  // ============================================================================

  @Test
  fun deleteMessage_successfullyRemovesMessage() = runTest {
    // Given
    val mockTask = Tasks.forResult<Void>(null)
    every { mockMessageRef.removeValue() } returns mockTask

    // When
    repository.deleteMessage(testConversationId, testMessageId)

    // Then
    verify { mockMessageRef.removeValue() }
  }

  // ============================================================================
  // markMessageAsRead Tests
  // ============================================================================

  @Test
  fun markMessageAsRead_addsUserIdToReadByList() = runTest {
    // Given
    val userId = "user123"
    val mockReadByRef = mockk<DatabaseReference>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockTask = Tasks.forResult(mockSnapshot)
    val mockSetTask = Tasks.forResult<Void>(null)

    every { mockMessageRef.get() } returns mockTask
    every {
      mockSnapshot
          .child("readBy")
          .getValue(any<com.google.firebase.database.GenericTypeIndicator<List<String>>>())
    } returns listOf("user1")
    every { mockMessageRef.child("readBy") } returns mockReadByRef
    every { mockReadByRef.setValue(any()) } returns mockSetTask

    // When
    repository.markMessageAsRead(testConversationId, testMessageId, userId)

    // Then
    verify { mockReadByRef.setValue(listOf("user1", userId)) }
  }

  @Test
  fun markMessageAsRead_doesNotAddDuplicateUserId() = runTest {
    // Given
    val userId = "user123"
    val mockReadByRef = mockk<DatabaseReference>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockTask = Tasks.forResult(mockSnapshot)

    every { mockMessageRef.get() } returns mockTask
    every {
      mockSnapshot
          .child("readBy")
          .getValue(any<com.google.firebase.database.GenericTypeIndicator<List<String>>>())
    } returns listOf("user1", userId)
    every { mockMessageRef.child("readBy") } returns mockReadByRef

    // When
    repository.markMessageAsRead(testConversationId, testMessageId, userId)

    // Then - should not call setValue since user is already in the list
    verify(exactly = 0) { mockReadByRef.setValue(any()) }
  }
}
