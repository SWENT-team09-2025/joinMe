package com.android.joinme.repository

// Implemented with help of Claude AI

import com.android.joinme.model.chat.CONVERSATIONS_COLLECTION_PATH
import com.android.joinme.model.chat.ChatRepositoryFirestore
import com.android.joinme.model.chat.MESSAGES_SUBCOLLECTION_PATH
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for ChatRepositoryFirestore.
 *
 * Tests all CRUD operations, message read tracking, and Firestore integration using MockK.
 */
class ChatRepositoryFirestoreTest {

  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockConversationCollection: CollectionReference
  private lateinit var mockConversationDocument: DocumentReference
  private lateinit var mockMessagesCollection: CollectionReference
  private lateinit var mockMessageDocument: DocumentReference
  private lateinit var repository: ChatRepositoryFirestore

  private val testConversationId = "conversation123"
  private val testMessageId = "test-message-123"
  private val testMessage =
      Message(
          id = testMessageId,
          conversationId = testConversationId,
          senderId = "user1",
          senderName = "Alice",
          content = "Hello, world!",
          timestamp = 1000L,
          type = MessageType.TEXT,
          readBy = emptyList(),
          isPinned = false)

  @Before
  fun setup() {
    mockFirestore = mockk(relaxed = true)
    mockConversationCollection = mockk(relaxed = true)
    mockConversationDocument = mockk(relaxed = true)
    mockMessagesCollection = mockk(relaxed = true)
    mockMessageDocument = mockk(relaxed = true)

    every { mockFirestore.collection(CONVERSATIONS_COLLECTION_PATH) } returns
        mockConversationCollection
    every { mockConversationCollection.document(any()) } returns mockConversationDocument
    every { mockConversationDocument.collection(MESSAGES_SUBCOLLECTION_PATH) } returns
        mockMessagesCollection
    every { mockMessagesCollection.document(any()) } returns mockMessageDocument

    repository = ChatRepositoryFirestore(mockFirestore)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  // ---------------- ADD MESSAGE ----------------

  @Test
  fun addMessage_callsFirestoreSetWithCorrectPath() = runTest {
    // Given
    val mockTask = Tasks.forResult<Void>(null)
    every { mockMessageDocument.set(any()) } returns mockTask

    // When
    repository.addMessage(testMessage)

    // Then
    verify { mockConversationCollection.document(testConversationId) }
    verify { mockConversationDocument.collection(MESSAGES_SUBCOLLECTION_PATH) }
    verify { mockMessagesCollection.document(testMessageId) }
    verify { mockMessageDocument.set(testMessage) }
  }

  // ---------------- EDIT MESSAGE ----------------

  @Test
  fun editMessage_callsFirestoreSetWithMerge() = runTest {
    // Given
    val updatedMessage = testMessage.copy(content = "Updated content")
    val mockTask = Tasks.forResult<Void>(null)
    every { mockMessageDocument.set(any(), any()) } returns mockTask

    // When
    repository.editMessage(testConversationId, testMessageId, updatedMessage)

    // Then
    verify { mockConversationCollection.document(testConversationId) }
    verify { mockConversationDocument.collection(MESSAGES_SUBCOLLECTION_PATH) }
    verify { mockMessagesCollection.document(testMessageId) }
    verify { mockMessageDocument.set(updatedMessage, any()) } // Verify merge option is used
  }

  // ---------------- DELETE MESSAGE ----------------

  @Test
  fun deleteMessage_callsFirestoreDelete() = runTest {
    // Given
    val mockTask = Tasks.forResult<Void>(null)
    every { mockMessageDocument.delete() } returns mockTask

    // When
    repository.deleteMessage(testConversationId, testMessageId)

    // Then
    verify { mockConversationCollection.document(testConversationId) }
    verify { mockConversationDocument.collection(MESSAGES_SUBCOLLECTION_PATH) }
    verify { mockMessagesCollection.document(testMessageId) }
    verify { mockMessageDocument.delete() }
  }

  // ---------------- MARK MESSAGE AS READ ----------------

  @Test
  fun markMessageAsRead_usesAtomicArrayUnion() = runTest {
    // Given
    val userId = "user2"
    val mockUpdateTask = Tasks.forResult<Void>(null)
    every { mockMessageDocument.update("readBy", any<FieldValue>()) } returns mockUpdateTask

    // When
    repository.markMessageAsRead(testConversationId, testMessageId, userId)

    // Then
    verify { mockConversationCollection.document(testConversationId) }
    verify { mockConversationDocument.collection(MESSAGES_SUBCOLLECTION_PATH) }
    verify { mockMessagesCollection.document(testMessageId) }
    verify { mockMessageDocument.update("readBy", any<FieldValue>()) }
    verify(exactly = 0) { mockMessageDocument.get() } // No read operation
  }

  // ---------------- OBSERVE MESSAGES FOR CONVERSATION ----------------

  @Test
  fun observeMessagesForConversation_returnsFlowWithMessages() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDocumentSnapshot1 = mockk<DocumentSnapshot>(relaxed = true)
    val mockDocumentSnapshot2 = mockk<DocumentSnapshot>(relaxed = true)
    val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)

    // Setup message 1
    every { mockDocumentSnapshot1.id } returns "msg1"
    every { mockDocumentSnapshot1.getString("senderId") } returns "user1"
    every { mockDocumentSnapshot1.getString("senderName") } returns "Alice"
    every { mockDocumentSnapshot1.getString("content") } returns "Hello"
    every { mockDocumentSnapshot1.getLong("timestamp") } returns 1000L
    every { mockDocumentSnapshot1.getString("type") } returns "TEXT"
    every { mockDocumentSnapshot1.get("readBy") } returns emptyList<String>()
    every { mockDocumentSnapshot1.getBoolean("isPinned") } returns false

    // Setup message 2
    every { mockDocumentSnapshot2.id } returns "msg2"
    every { mockDocumentSnapshot2.getString("senderId") } returns "user2"
    every { mockDocumentSnapshot2.getString("senderName") } returns "Bob"
    every { mockDocumentSnapshot2.getString("content") } returns "Hi there"
    every { mockDocumentSnapshot2.getLong("timestamp") } returns 2000L
    every { mockDocumentSnapshot2.getString("type") } returns "TEXT"
    every { mockDocumentSnapshot2.get("readBy") } returns emptyList<String>()
    every { mockDocumentSnapshot2.getBoolean("isPinned") } returns false

    every { mockQuerySnapshot.documents } returns
        listOf(mockDocumentSnapshot1, mockDocumentSnapshot2)

    // Setup query and listener
    every { mockMessagesCollection.orderBy("timestamp") } returns mockQuery
    every { mockQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers
        {
          val listener = firstArg<EventListener<QuerySnapshot>>()
          listener.onEvent(mockQuerySnapshot, null)
          mockListenerRegistration
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
    verify { mockMessagesCollection.orderBy("timestamp") }
    verify { mockQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) }
  }

  @Test
  fun observeMessagesForConversation_returnsEmptyListWhenNoMessages() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)

    every { mockQuerySnapshot.documents } returns emptyList()

    every { mockMessagesCollection.orderBy("timestamp") } returns mockQuery
    every { mockQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers
        {
          val listener = firstArg<EventListener<QuerySnapshot>>()
          listener.onEvent(mockQuerySnapshot, null)
          mockListenerRegistration
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then
    assertNotNull(messages)
    assertEquals(0, messages.size)
  }

  @Test
  fun observeMessagesForConversation_handlesFirestoreError() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)
    val mockException = mockk<FirebaseFirestoreException>(relaxed = true)

    every { mockMessagesCollection.orderBy("timestamp") } returns mockQuery
    every { mockQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers
        {
          val listener = firstArg<EventListener<QuerySnapshot>>()
          listener.onEvent(null, mockException)
          mockListenerRegistration
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then - should return empty list on error
    assertNotNull(messages)
    assertEquals(0, messages.size)
  }

  @Test
  fun observeMessagesForConversation_filtersOutInvalidDocuments() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)

    // Valid document
    val mockValidDoc = mockk<DocumentSnapshot>(relaxed = true)
    every { mockValidDoc.id } returns "msg1"
    every { mockValidDoc.getString("senderId") } returns "user1"
    every { mockValidDoc.getString("senderName") } returns "Alice"
    every { mockValidDoc.getString("content") } returns "Valid message"
    every { mockValidDoc.getLong("timestamp") } returns 1000L
    every { mockValidDoc.getString("type") } returns "TEXT"
    every { mockValidDoc.get("readBy") } returns emptyList<String>()
    every { mockValidDoc.getBoolean("isPinned") } returns false

    // Invalid document (missing senderId)
    val mockInvalidDoc = mockk<DocumentSnapshot>(relaxed = true)
    every { mockInvalidDoc.id } returns "msg2"
    every { mockInvalidDoc.getString("senderId") } returns null
    every { mockInvalidDoc.getString("senderName") } returns "Bob"
    every { mockInvalidDoc.getString("content") } returns "Invalid message"
    every { mockInvalidDoc.getLong("timestamp") } returns 2000L

    every { mockQuerySnapshot.documents } returns listOf(mockValidDoc, mockInvalidDoc)

    every { mockMessagesCollection.orderBy("timestamp") } returns mockQuery
    every { mockQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers
        {
          val listener = firstArg<EventListener<QuerySnapshot>>()
          listener.onEvent(mockQuerySnapshot, null)
          mockListenerRegistration
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then - should only return valid message
    assertNotNull(messages)
    assertEquals(1, messages.size)
    assertEquals("msg1", messages[0].id)
  }

  @Test
  fun observeMessagesForConversation_handlesFirestoreTimestamp() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDocumentSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)
    val testDate = Date(5000L)
    val testTimestamp = Timestamp(testDate)

    // Setup document with Firestore Timestamp instead of Long
    every { mockDocumentSnapshot.id } returns "msg1"
    every { mockDocumentSnapshot.getString("senderId") } returns "user1"
    every { mockDocumentSnapshot.getString("senderName") } returns "Alice"
    every { mockDocumentSnapshot.getString("content") } returns "Message with Timestamp"
    every { mockDocumentSnapshot.getLong("timestamp") } returns null
    every { mockDocumentSnapshot.getTimestamp("timestamp") } returns testTimestamp
    every { mockDocumentSnapshot.getString("type") } returns "TEXT"
    every { mockDocumentSnapshot.get("readBy") } returns emptyList<String>()
    every { mockDocumentSnapshot.getBoolean("isPinned") } returns false

    every { mockQuerySnapshot.documents } returns listOf(mockDocumentSnapshot)

    every { mockMessagesCollection.orderBy("timestamp") } returns mockQuery
    every { mockQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers
        {
          val listener = firstArg<EventListener<QuerySnapshot>>()
          listener.onEvent(mockQuerySnapshot, null)
          mockListenerRegistration
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then
    assertNotNull(messages)
    assertEquals(1, messages.size)
    assertEquals(5000L, messages[0].timestamp)
  }

  @Test
  fun observeMessagesForConversation_handlesInvalidMessageType() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDocumentSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)

    // Setup document with invalid MessageType
    every { mockDocumentSnapshot.id } returns "msg1"
    every { mockDocumentSnapshot.getString("senderId") } returns "user1"
    every { mockDocumentSnapshot.getString("senderName") } returns "Alice"
    every { mockDocumentSnapshot.getString("content") } returns "Message with invalid type"
    every { mockDocumentSnapshot.getLong("timestamp") } returns 1000L
    every { mockDocumentSnapshot.getString("type") } returns "INVALID_TYPE"
    every { mockDocumentSnapshot.get("readBy") } returns emptyList<String>()
    every { mockDocumentSnapshot.getBoolean("isPinned") } returns false

    every { mockQuerySnapshot.documents } returns listOf(mockDocumentSnapshot)

    every { mockMessagesCollection.orderBy("timestamp") } returns mockQuery
    every { mockQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers
        {
          val listener = firstArg<EventListener<QuerySnapshot>>()
          listener.onEvent(mockQuerySnapshot, null)
          mockListenerRegistration
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then - should default to TEXT type
    assertNotNull(messages)
    assertEquals(1, messages.size)
    assertEquals(MessageType.TEXT, messages[0].type)
  }

  @Test
  fun observeMessagesForConversation_handlesNullTimestamp() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDocumentSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)

    // Setup document with null timestamp
    every { mockDocumentSnapshot.id } returns "msg1"
    every { mockDocumentSnapshot.getString("senderId") } returns "user1"
    every { mockDocumentSnapshot.getString("senderName") } returns "Alice"
    every { mockDocumentSnapshot.getString("content") } returns "Message"
    every { mockDocumentSnapshot.getLong("timestamp") } returns null
    every { mockDocumentSnapshot.getTimestamp("timestamp") } returns null
    every { mockDocumentSnapshot.getString("type") } returns "TEXT"

    every { mockQuerySnapshot.documents } returns listOf(mockDocumentSnapshot)

    every { mockMessagesCollection.orderBy("timestamp") } returns mockQuery
    every { mockQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers
        {
          val listener = firstArg<EventListener<QuerySnapshot>>()
          listener.onEvent(mockQuerySnapshot, null)
          mockListenerRegistration
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then - document with null timestamp should be filtered out
    assertNotNull(messages)
    assertEquals(0, messages.size)
  }

  @Test
  fun observeMessagesForConversation_handlesDocumentConversionException() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockDocumentSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)

    // Setup document that throws exception during conversion
    every { mockDocumentSnapshot.id } returns "msg1"
    every { mockDocumentSnapshot.getString("senderId") } throws RuntimeException("Test exception")

    every { mockQuerySnapshot.documents } returns listOf(mockDocumentSnapshot)

    every { mockMessagesCollection.orderBy("timestamp") } returns mockQuery
    every { mockQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers
        {
          val listener = firstArg<EventListener<QuerySnapshot>>()
          listener.onEvent(mockQuerySnapshot, null)
          mockListenerRegistration
        }

    // When
    val flow = repository.observeMessagesForConversation(testConversationId)
    val messages = withTimeout(1000) { flow.first() }

    // Then - should return empty list when exception occurs
    assertNotNull(messages)
    assertEquals(0, messages.size)
  }

  // ---------------- GET NEW MESSAGE ID ----------------

  @Test
  fun getNewMessageId_returnsUniqueId() {
    // Given
    val mockTempCollection = mockk<CollectionReference>(relaxed = true)
    val mockTempDocument = mockk<DocumentReference>(relaxed = true)
    val expectedId = "generated-unique-id-123"

    every { mockFirestore.collection("temp") } returns mockTempCollection
    every { mockTempCollection.document() } returns mockTempDocument
    every { mockTempDocument.id } returns expectedId

    // When
    val generatedId = repository.getNewMessageId()

    // Then
    assertEquals(expectedId, generatedId)
    verify { mockFirestore.collection("temp") }
    verify { mockTempCollection.document() }
  }
}
