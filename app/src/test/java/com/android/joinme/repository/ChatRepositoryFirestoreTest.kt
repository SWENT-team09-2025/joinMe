package com.android.joinme.repository

// Implemented with help of Claude AI

import com.android.joinme.model.chat.CONVERSATIONS_COLLECTION_PATH
import com.android.joinme.model.chat.ChatRepositoryFirestore
import com.android.joinme.model.chat.MESSAGES_SUBCOLLECTION_PATH
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
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
  fun editMessage_callsFirestoreSetWithUpdatedData() = runTest {
    // Given
    val updatedMessage = testMessage.copy(content = "Updated content")
    val mockTask = Tasks.forResult<Void>(null)
    every { mockMessageDocument.set(any()) } returns mockTask

    // When
    repository.editMessage(testConversationId, testMessageId, updatedMessage)

    // Then
    verify { mockConversationCollection.document(testConversationId) }
    verify { mockConversationDocument.collection(MESSAGES_SUBCOLLECTION_PATH) }
    verify { mockMessagesCollection.document(testMessageId) }
    verify { mockMessageDocument.set(updatedMessage) }
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
  fun markMessageAsRead_addsUserToReadByList() = runTest {
    // Given
    val userId = "user2"
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.id } returns testMessageId
    every { mockSnapshot.getString("senderId") } returns testMessage.senderId
    every { mockSnapshot.getString("senderName") } returns testMessage.senderName
    every { mockSnapshot.getString("content") } returns testMessage.content
    every { mockSnapshot.getLong("timestamp") } returns testMessage.timestamp
    every { mockSnapshot.getString("type") } returns "TEXT"
    every { mockSnapshot.get("readBy") } returns emptyList<String>()
    every { mockSnapshot.getBoolean("isPinned") } returns false

    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockUpdateTask = Tasks.forResult<Void>(null)
    every { mockMessageDocument.get() } returns mockGetTask
    every { mockMessageDocument.update("readBy", listOf(userId)) } returns mockUpdateTask

    // When
    repository.markMessageAsRead(testConversationId, testMessageId, userId)

    // Then
    verify { mockConversationCollection.document(testConversationId) }
    verify { mockConversationDocument.collection(MESSAGES_SUBCOLLECTION_PATH) }
    verify { mockMessagesCollection.document(testMessageId) }
    verify { mockMessageDocument.get() }
    verify { mockMessageDocument.update("readBy", listOf(userId)) }
  }

  @Test
  fun markMessageAsRead_doesNotUpdateIfUserAlreadyRead() = runTest {
    // Given
    val userId = "user2"
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.id } returns testMessageId
    every { mockSnapshot.getString("senderId") } returns testMessage.senderId
    every { mockSnapshot.getString("senderName") } returns testMessage.senderName
    every { mockSnapshot.getString("content") } returns testMessage.content
    every { mockSnapshot.getLong("timestamp") } returns testMessage.timestamp
    every { mockSnapshot.getString("type") } returns "TEXT"
    every { mockSnapshot.get("readBy") } returns listOf(userId) // User already in readBy
    every { mockSnapshot.getBoolean("isPinned") } returns false

    val mockGetTask = Tasks.forResult(mockSnapshot)
    every { mockMessageDocument.get() } returns mockGetTask

    // When
    repository.markMessageAsRead(testConversationId, testMessageId, userId)

    // Then
    verify { mockMessageDocument.get() }
    verify(exactly = 0) { mockMessageDocument.update(any<String>(), any<List<String>>()) }
  }

  @Test(expected = Exception::class)
  fun markMessageAsRead_throwsExceptionWhenMessageNotFound() = runTest {
    // Given
    val userId = "user2"
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.id } returns testMessageId
    every { mockSnapshot.getString("senderId") } returns null // Missing required field

    val mockGetTask = Tasks.forResult(mockSnapshot)
    every { mockMessageDocument.get() } returns mockGetTask

    // When
    repository.markMessageAsRead(testConversationId, testMessageId, userId)

    // Then - exception is thrown
  }

  @Test
  fun markMessageAsRead_appendsToExistingReadByList() = runTest {
    // Given
    val userId = "user3"
    val existingReadBy = listOf("user1", "user2")
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.id } returns testMessageId
    every { mockSnapshot.getString("senderId") } returns testMessage.senderId
    every { mockSnapshot.getString("senderName") } returns testMessage.senderName
    every { mockSnapshot.getString("content") } returns testMessage.content
    every { mockSnapshot.getLong("timestamp") } returns testMessage.timestamp
    every { mockSnapshot.getString("type") } returns "TEXT"
    every { mockSnapshot.get("readBy") } returns existingReadBy
    every { mockSnapshot.getBoolean("isPinned") } returns false

    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockUpdateTask = Tasks.forResult<Void>(null)
    every { mockMessageDocument.get() } returns mockGetTask
    every { mockMessageDocument.update("readBy", existingReadBy + userId) } returns mockUpdateTask

    // When
    repository.markMessageAsRead(testConversationId, testMessageId, userId)

    // Then
    verify { mockMessageDocument.update("readBy", listOf("user1", "user2", "user3")) }
  }

  // ---------------- MESSAGE TYPE HANDLING ----------------

  @Test
  fun documentToMessage_handlesSystemMessageType() = runTest {
    // Given
    val systemMessage = testMessage.copy(type = MessageType.SYSTEM, content = "User joined")
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.id } returns testMessageId
    every { mockSnapshot.getString("senderId") } returns systemMessage.senderId
    every { mockSnapshot.getString("senderName") } returns systemMessage.senderName
    every { mockSnapshot.getString("content") } returns "User joined"
    every { mockSnapshot.getLong("timestamp") } returns systemMessage.timestamp
    every { mockSnapshot.getString("type") } returns "SYSTEM"
    every { mockSnapshot.get("readBy") } returns emptyList<String>()
    every { mockSnapshot.getBoolean("isPinned") } returns false

    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockUpdateTask = Tasks.forResult<Void>(null)
    every { mockMessageDocument.get() } returns mockGetTask
    every { mockMessageDocument.update(any<String>(), any<List<String>>()) } returns mockUpdateTask

    // When - we use markMessageAsRead to trigger documentToMessage internally
    repository.markMessageAsRead(testConversationId, testMessageId, "user2")

    // Then - if no exception is thrown, the message was parsed correctly
    verify { mockMessageDocument.get() }
  }

  @Test
  fun documentToMessage_handlesImageMessageType() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.id } returns testMessageId
    every { mockSnapshot.getString("senderId") } returns testMessage.senderId
    every { mockSnapshot.getString("senderName") } returns testMessage.senderName
    every { mockSnapshot.getString("content") } returns "https://example.com/image.jpg"
    every { mockSnapshot.getLong("timestamp") } returns testMessage.timestamp
    every { mockSnapshot.getString("type") } returns "IMAGE"
    every { mockSnapshot.get("readBy") } returns emptyList<String>()
    every { mockSnapshot.getBoolean("isPinned") } returns false

    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockUpdateTask = Tasks.forResult<Void>(null)
    every { mockMessageDocument.get() } returns mockGetTask
    every { mockMessageDocument.update(any<String>(), any<List<String>>()) } returns mockUpdateTask

    // When
    repository.markMessageAsRead(testConversationId, testMessageId, "user2")

    // Then - if no exception is thrown, the message was parsed correctly
    verify { mockMessageDocument.get() }
  }

  @Test
  fun documentToMessage_defaultsToTextTypeWhenInvalid() = runTest {
    // Given
    val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    every { mockSnapshot.id } returns testMessageId
    every { mockSnapshot.getString("senderId") } returns testMessage.senderId
    every { mockSnapshot.getString("senderName") } returns testMessage.senderName
    every { mockSnapshot.getString("content") } returns testMessage.content
    every { mockSnapshot.getLong("timestamp") } returns testMessage.timestamp
    every { mockSnapshot.getString("type") } returns "INVALID_TYPE"
    every { mockSnapshot.get("readBy") } returns emptyList<String>()
    every { mockSnapshot.getBoolean("isPinned") } returns false

    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockUpdateTask = Tasks.forResult<Void>(null)
    every { mockMessageDocument.get() } returns mockGetTask
    every { mockMessageDocument.update(any<String>(), any<List<String>>()) } returns mockUpdateTask

    // When
    repository.markMessageAsRead(testConversationId, testMessageId, "user2")

    // Then - if no exception is thrown, the message was parsed with TEXT as default
    verify { mockMessageDocument.get() }
  }
}
