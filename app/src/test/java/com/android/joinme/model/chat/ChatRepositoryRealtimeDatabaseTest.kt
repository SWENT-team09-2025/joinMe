package com.android.joinme.model.chat

// Implemented with help of Claude AI

import android.content.Context
import android.net.Uri
import com.android.joinme.model.map.Location
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
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
  private lateinit var mockStorage: FirebaseStorage
  private lateinit var mockConversationsRef: DatabaseReference
  private lateinit var mockConversationRef: DatabaseReference
  private lateinit var mockMessagesRef: DatabaseReference
  private lateinit var mockMessageRef: DatabaseReference
  private lateinit var mockStorageRef: StorageReference

  private val testConversationId = "test-conversation-123"
  private val testMessageId = "test-message-456"

  @Before
  fun setup() {
    // Mock Firebase Realtime Database components
    mockDatabase = mockk(relaxed = true)
    mockStorage = mockk(relaxed = true)
    mockConversationsRef = mockk(relaxed = true)
    mockConversationRef = mockk(relaxed = true)
    mockMessagesRef = mockk(relaxed = true)
    mockMessageRef = mockk(relaxed = true)
    mockStorageRef = mockk(relaxed = true)

    // Setup reference chain
    every { mockDatabase.getReference("conversations") } returns mockConversationsRef
    every { mockConversationsRef.child(any()) } returns mockConversationRef
    every { mockConversationRef.child("messages") } returns mockMessagesRef
    every { mockMessagesRef.child(any()) } returns mockMessageRef
    every { mockMessagesRef.orderByChild("timestamp") } returns mockk<Query>(relaxed = true)

    // Setup storage reference chain
    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child(any()) } returns mockStorageRef

    repository = ChatRepositoryRealtimeDatabase(mockDatabase, mockStorage)
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

  @Test(expected = IllegalStateException::class)
  fun getNewMessageId_throwsExceptionWhenPushReturnsNull() {
    // Given
    val mockPushRef = mockk<DatabaseReference>(relaxed = true)
    every { mockConversationsRef.push() } returns mockPushRef
    every { mockPushRef.key } returns null

    // When - should throw IllegalStateException
    repository.getNewMessageId()
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
      mockMessageSnapshot1.child("readBy").getValue(any<GenericTypeIndicator<List<String>>>())
    } returns emptyList()
    every { mockMessageSnapshot1.child("isPinned").getValue(Boolean::class.java) } returns false
    every { mockMessageSnapshot1.child("isEdited").getValue(Boolean::class.java) } returns false
    every { mockMessageSnapshot1.child("location").exists() } returns false

    // Setup message 2
    every { mockMessageSnapshot2.key } returns "msg2"
    every { mockMessageSnapshot2.child("senderId").getValue(String::class.java) } returns "user2"
    every { mockMessageSnapshot2.child("senderName").getValue(String::class.java) } returns "Bob"
    every { mockMessageSnapshot2.child("content").getValue(String::class.java) } returns "Hi there"
    every { mockMessageSnapshot2.child("timestamp").getValue(Long::class.java) } returns 2000L
    every { mockMessageSnapshot2.child("type").getValue(String::class.java) } returns "TEXT"
    every {
      mockMessageSnapshot2.child("readBy").getValue(any<GenericTypeIndicator<List<String>>>())
    } returns emptyList()
    every { mockMessageSnapshot2.child("isPinned").getValue(Boolean::class.java) } returns false
    every { mockMessageSnapshot2.child("isEdited").getValue(Boolean::class.java) } returns false
    every { mockMessageSnapshot2.child("location").exists() } returns false

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
      mockValidMessage.child("readBy").getValue(any<GenericTypeIndicator<List<String>>>())
    } returns emptyList()
    every { mockValidMessage.child("isPinned").getValue(Boolean::class.java) } returns false
    every { mockValidMessage.child("isEdited").getValue(Boolean::class.java) } returns false
    every { mockValidMessage.child("location").exists() } returns false

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
      mockSnapshot.child("readBy").getValue(any<GenericTypeIndicator<List<String>>>())
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
      mockSnapshot.child("readBy").getValue(any<GenericTypeIndicator<List<String>>>())
    } returns listOf("user1", userId)
    every { mockMessageRef.child("readBy") } returns mockReadByRef

    // When
    repository.markMessageAsRead(testConversationId, testMessageId, userId)

    // Then - should not call setValue since user is already in the list
    verify(exactly = 0) { mockReadByRef.setValue(any()) }
  }

  // ============================================================================
  // uploadChatImage Tests
  // ============================================================================

  @Test
  fun uploadChatImage_successfullyUploadsImageWithCorrectPathAndReturnsUrl() = runTest {
    // Given
    val mockContext = mockk<Context>(relaxed = true)
    val mockImageUri = mockk<Uri>(relaxed = true)
    val testDownloadUrl = "https://storage.googleapis.com/test-image.jpg"

    // Mock ImageProcessor instance
    mockkConstructor(com.android.joinme.model.utils.ImageProcessor::class)
    every {
      anyConstructed<com.android.joinme.model.utils.ImageProcessor>().processImage(mockImageUri)
    } returns byteArrayOf(1, 2, 3, 4)

    // Mock storage references to track path
    val mockConversationsStorageRef = mockk<StorageReference>(relaxed = true)
    val mockConversationIdRef = mockk<StorageReference>(relaxed = true)
    val mockImagesRef = mockk<StorageReference>(relaxed = true)
    val mockImageFileRef = mockk<StorageReference>(relaxed = true)

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child("conversations") } returns mockConversationsStorageRef
    every { mockConversationsStorageRef.child(testConversationId) } returns mockConversationIdRef
    every { mockConversationIdRef.child("images") } returns mockImagesRef
    every { mockImagesRef.child("$testMessageId.jpg") } returns mockImageFileRef

    // Mock upload - putBytes returns UploadTask which is a Task
    val mockTaskSnapshot = mockk<UploadTask.TaskSnapshot>(relaxed = true)
    // Create a successful UploadTask using mockk with isComplete and result
    val mockUploadTask = mockk<UploadTask>(relaxed = true)
    every { mockUploadTask.isComplete } returns true
    every { mockUploadTask.isSuccessful } returns true
    every { mockUploadTask.isCanceled } returns false
    every { mockUploadTask.result } returns mockTaskSnapshot
    every { mockUploadTask.exception } returns null
    every { mockImageFileRef.putBytes(any<ByteArray>()) } returns mockUploadTask

    // Mock download URL
    val mockUri = mockk<Uri>(relaxed = true)
    every { mockUri.toString() } returns testDownloadUrl
    val mockDownloadUrlTask = Tasks.forResult(mockUri)
    every { mockImageFileRef.downloadUrl } returns mockDownloadUrlTask

    // When
    val result =
        repository.uploadChatImage(mockContext, testConversationId, testMessageId, mockImageUri)

    // Then - verify successful upload and correct return value
    assertEquals(testDownloadUrl, result)

    // Verify image processing
    verify {
      anyConstructed<com.android.joinme.model.utils.ImageProcessor>().processImage(mockImageUri)
    }
    verify { mockImageFileRef.putBytes(byteArrayOf(1, 2, 3, 4)) }

    // Verify correct storage path: conversations/{conversationId}/images/{messageId}.jpg
    verify { mockStorageRef.child("conversations") }
    verify { mockConversationsStorageRef.child(testConversationId) }
    verify { mockConversationIdRef.child("images") }
    verify { mockImagesRef.child("$testMessageId.jpg") }

    // Verify download URL was retrieved
    verify { mockImageFileRef.downloadUrl }

    unmockkConstructor(com.android.joinme.model.utils.ImageProcessor::class)
  }

  @Test(expected = Exception::class)
  fun uploadChatImage_throwsExceptionOnUploadFailure() = runTest {
    // Given
    val mockContext = mockk<Context>(relaxed = true)
    val mockImageUri = mockk<Uri>(relaxed = true)

    // Mock ImageProcessor
    mockkConstructor(com.android.joinme.model.utils.ImageProcessor::class)
    every {
      anyConstructed<com.android.joinme.model.utils.ImageProcessor>().processImage(mockImageUri)
    } returns byteArrayOf(1, 2, 3, 4)

    // Mock storage upload failure
    val uploadException = Exception("Upload failed")
    val mockUploadTask = mockk<UploadTask>(relaxed = true)
    every { mockUploadTask.isComplete } returns true
    every { mockUploadTask.isSuccessful } returns false
    every { mockUploadTask.isCanceled } returns false
    every { mockUploadTask.exception } returns uploadException
    every { mockStorageRef.putBytes(any<ByteArray>()) } returns mockUploadTask

    // When - should throw exception
    try {
      repository.uploadChatImage(mockContext, testConversationId, testMessageId, mockImageUri)
    } finally {
      unmockkConstructor(com.android.joinme.model.utils.ImageProcessor::class)
    }
  }

  // ============================================================================
  // Location Messages Tests
  // ============================================================================

  @Test
  fun observeMessagesForConversation_deserializesLocationMessageCorrectly() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockLocationMessage = mockk<DataSnapshot>(relaxed = true)
    val mockLocationSnapshot = mockk<DataSnapshot>(relaxed = true)

    // Setup location message with location data
    every { mockLocationMessage.key } returns "msg-loc1"
    every { mockLocationMessage.child("senderId").getValue(String::class.java) } returns "user1"
    every { mockLocationMessage.child("senderName").getValue(String::class.java) } returns "Alice"
    every { mockLocationMessage.child("content").getValue(String::class.java) } returns
        "https://maps.googleapis.com/maps/api/staticmap?..."
    every { mockLocationMessage.child("timestamp").getValue(Long::class.java) } returns 3000L
    every { mockLocationMessage.child("type").getValue(String::class.java) } returns "LOCATION"
    every {
      mockLocationMessage.child("readBy").getValue(any<GenericTypeIndicator<List<String>>>())
    } returns emptyList()
    every { mockLocationMessage.child("isPinned").getValue(Boolean::class.java) } returns false
    every { mockLocationMessage.child("isEdited").getValue(Boolean::class.java) } returns false

    // Setup location field
    every { mockLocationMessage.child("location") } returns mockLocationSnapshot
    every { mockLocationSnapshot.exists() } returns true

    val mockLatitudeSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockLongitudeSnapshot = mockk<DataSnapshot>(relaxed = true)
    every { mockLocationSnapshot.child("latitude") } returns mockLatitudeSnapshot
    every { mockLatitudeSnapshot.value } returns 46.5197
    every { mockLocationSnapshot.child("longitude") } returns mockLongitudeSnapshot
    every { mockLongitudeSnapshot.value } returns 6.6323
    every { mockLocationSnapshot.child("name").getValue(String::class.java) } returns
        "EPFL, Lausanne"

    every { mockSnapshot.children } returns listOf(mockLocationMessage)
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
    assertEquals(1, messages.size)

    val locationMessage = messages[0]
    assertEquals("msg-loc1", locationMessage.id)
    assertEquals(MessageType.LOCATION, locationMessage.type)
    assertNotNull(locationMessage.location)
    assertEquals(46.5197, locationMessage.location!!.latitude, 0.0001)
    assertEquals(6.6323, locationMessage.location!!.longitude, 0.0001)
    assertEquals("EPFL, Lausanne", locationMessage.location!!.name)
    assertEquals("https://maps.googleapis.com/maps/api/staticmap?...", locationMessage.content)
  }

  @Test
  fun addMessage_withLocation_savesLocationFieldCorrectly() = runTest {
    // Given
    val location = Location(latitude = 47.3769, longitude = 8.5417, name = "Zurich HB")
    val locationMessage =
        Message(
            id = testMessageId,
            conversationId = testConversationId,
            senderId = "user1",
            senderName = "Alice",
            content = "https://maps.googleapis.com/maps/api/staticmap?...",
            timestamp = System.currentTimeMillis(),
            type = MessageType.LOCATION,
            location = location)

    val capturedData = slot<Map<String, Any?>>()
    val mockTask = Tasks.forResult<Void>(null)
    every { mockMessageRef.setValue(capture(capturedData)) } returns mockTask

    // When
    repository.addMessage(locationMessage)

    // Then
    verify { mockMessageRef.setValue(any()) }

    // Verify location field is properly nested in the saved data
    assertTrue(capturedData.captured.containsKey("location"))
    val locationMap = capturedData.captured["location"] as Map<*, *>
    assertEquals(47.3769, locationMap["latitude"])
    assertEquals(8.5417, locationMap["longitude"])
    assertEquals("Zurich HB", locationMap["name"])
    assertEquals("LOCATION", capturedData.captured["type"])
  }

  // ============================================================================
  // deleteConversation Tests
  // ============================================================================

  @Test
  fun deleteConversation_deletesMessagesAndConversationNode() = runTest {
    // Given
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockRemoveTask = Tasks.forResult<Void>(null)

    every { mockMessagesRef.get() } returns mockGetTask
    every { mockSnapshot.children } returns emptyList() // No messages
    every { mockConversationRef.removeValue() } returns mockRemoveTask

    // When
    repository.deleteConversation(testConversationId)

    // Then
    verify { mockMessagesRef.get() }
    verify { mockConversationRef.removeValue() }
  }

  @Test
  fun deleteConversation_deletesImageMessages() = runTest {
    // Given
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockImageMessage = mockk<DataSnapshot>(relaxed = true)
    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockRemoveTask = Tasks.forResult<Void>(null)
    val mockDeleteTask = Tasks.forResult<Void>(null)
    val mockImageRef = mockk<StorageReference>(relaxed = true)

    // Setup image message
    every { mockImageMessage.key } returns "image-msg-1"
    every { mockImageMessage.child("type").getValue(String::class.java) } returns "IMAGE"
    every { mockSnapshot.children } returns listOf(mockImageMessage)

    // Setup storage references
    every { mockStorageRef.child("conversations") } returns mockStorageRef
    every { mockStorageRef.child(testConversationId) } returns mockStorageRef
    every { mockStorageRef.child("images") } returns mockStorageRef
    every { mockStorageRef.child("image-msg-1.jpg") } returns mockImageRef
    every { mockImageRef.delete() } returns mockDeleteTask

    every { mockMessagesRef.get() } returns mockGetTask
    every { mockConversationRef.removeValue() } returns mockRemoveTask

    // When
    repository.deleteConversation(testConversationId)

    // Then
    verify { mockImageRef.delete() }
    verify { mockConversationRef.removeValue() }
  }

  @Test
  fun deleteConversation_handlesMultipleImageMessages() = runTest {
    // Given
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockImageMsg1 = mockk<DataSnapshot>(relaxed = true)
    val mockImageMsg2 = mockk<DataSnapshot>(relaxed = true)
    val mockTextMsg = mockk<DataSnapshot>(relaxed = true)
    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockRemoveTask = Tasks.forResult<Void>(null)
    val mockDeleteTask = Tasks.forResult<Void>(null)
    val mockImageRef1 = mockk<StorageReference>(relaxed = true)
    val mockImageRef2 = mockk<StorageReference>(relaxed = true)

    // Setup messages
    every { mockImageMsg1.key } returns "img-1"
    every { mockImageMsg1.child("type").getValue(String::class.java) } returns "IMAGE"
    every { mockImageMsg2.key } returns "img-2"
    every { mockImageMsg2.child("type").getValue(String::class.java) } returns "IMAGE"
    every { mockTextMsg.key } returns "text-1"
    every { mockTextMsg.child("type").getValue(String::class.java) } returns "TEXT"
    every { mockSnapshot.children } returns listOf(mockImageMsg1, mockTextMsg, mockImageMsg2)

    // Setup storage references
    every { mockStorageRef.child("conversations") } returns mockStorageRef
    every { mockStorageRef.child(testConversationId) } returns mockStorageRef
    every { mockStorageRef.child("images") } returns mockStorageRef
    every { mockStorageRef.child("img-1.jpg") } returns mockImageRef1
    every { mockStorageRef.child("img-2.jpg") } returns mockImageRef2
    every { mockImageRef1.delete() } returns mockDeleteTask
    every { mockImageRef2.delete() } returns mockDeleteTask

    every { mockMessagesRef.get() } returns mockGetTask
    every { mockConversationRef.removeValue() } returns mockRemoveTask

    // When
    repository.deleteConversation(testConversationId)

    // Then
    verify { mockImageRef1.delete() }
    verify { mockImageRef2.delete() }
    verify(exactly = 0) { mockStorageRef.child("text-1.jpg") } // Text message doesn't delete image
    verify { mockConversationRef.removeValue() }
  }

  @Test
  fun deleteConversation_continuesWhenImageDeleteFails() = runTest {
    // Given
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockImageMessage = mockk<DataSnapshot>(relaxed = true)
    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockRemoveTask = Tasks.forResult<Void>(null)
    val mockDeleteTask = Tasks.forException<Void>(Exception("Image not found"))
    val mockImageRef = mockk<StorageReference>(relaxed = true)

    every { mockImageMessage.key } returns "image-msg-1"
    every { mockImageMessage.child("type").getValue(String::class.java) } returns "IMAGE"
    every { mockSnapshot.children } returns listOf(mockImageMessage)

    every { mockStorageRef.child("conversations") } returns mockStorageRef
    every { mockStorageRef.child(testConversationId) } returns mockStorageRef
    every { mockStorageRef.child("images") } returns mockStorageRef
    every { mockStorageRef.child("image-msg-1.jpg") } returns mockImageRef
    every { mockImageRef.delete() } returns mockDeleteTask

    every { mockMessagesRef.get() } returns mockGetTask
    every { mockConversationRef.removeValue() } returns mockRemoveTask

    // When
    repository.deleteConversation(testConversationId)

    // Then - Should still delete conversation even if image delete fails
    verify { mockImageRef.delete() }
    verify { mockConversationRef.removeValue() }
  }

  // ============================================================================
  // deleteAllUserConversations Tests
  // ============================================================================

  @Test
  fun deleteAllUserConversations_findsAndDeletesUserDMConversations() = runTest {
    // Given
    val userId = "user1"
    val mockAllConversationsSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockConv1 = mockk<DataSnapshot>(relaxed = true)
    val mockConv2 = mockk<DataSnapshot>(relaxed = true)
    val mockConv3 = mockk<DataSnapshot>(relaxed = true)
    val mockConv4 = mockk<DataSnapshot>(relaxed = true)

    // Setup conversation snapshots
    every { mockConv1.key } returns "dm_alice_user1"
    every { mockConv2.key } returns "dm_user1_bob"
    every { mockConv3.key } returns "dm_charlie_david" // Not involving user1
    every { mockConv4.key } returns "event123" // Not a DM

    every { mockAllConversationsSnapshot.children } returns
        listOf(mockConv1, mockConv2, mockConv3, mockConv4)

    val mockGetTask = Tasks.forResult(mockAllConversationsSnapshot)
    every { mockConversationsRef.get() } returns mockGetTask

    // Mock deleteConversation calls
    val mockMessagesSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockMessagesGetTask = Tasks.forResult(mockMessagesSnapshot)
    val mockRemoveTask = Tasks.forResult<Void>(null)
    every { mockMessagesSnapshot.children } returns emptyList()
    every { mockMessagesRef.get() } returns mockMessagesGetTask
    every { mockConversationRef.removeValue() } returns mockRemoveTask

    // When
    repository.deleteAllUserConversations(userId)

    // Then
    verify { mockConversationsRef.get() }
    // Should attempt to delete conversations involving user1
    verify(atLeast = 2) { mockConversationRef.removeValue() }
  }

  @Test
  fun deleteAllUserConversations_handlesUserWithNoDMs() = runTest {
    // Given
    val userId = "user1"
    val mockAllConversationsSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockConv1 = mockk<DataSnapshot>(relaxed = true)
    val mockConv2 = mockk<DataSnapshot>(relaxed = true)

    every { mockConv1.key } returns "dm_alice_bob" // Not involving user1
    every { mockConv2.key } returns "event123" // Not a DM
    every { mockAllConversationsSnapshot.children } returns listOf(mockConv1, mockConv2)

    val mockGetTask = Tasks.forResult(mockAllConversationsSnapshot)
    every { mockConversationsRef.get() } returns mockGetTask

    // When
    repository.deleteAllUserConversations(userId)

    // Then
    verify { mockConversationsRef.get() }
    // Should not attempt to delete any conversations
    verify(exactly = 0) { mockConversationRef.removeValue() }
  }

  @Test
  fun deleteAllUserConversations_ignoresInvalidDMFormat() = runTest {
    // Given
    val userId = "user1"
    val mockAllConversationsSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockConv1 = mockk<DataSnapshot>(relaxed = true)
    val mockConv2 = mockk<DataSnapshot>(relaxed = true)
    val mockConv3 = mockk<DataSnapshot>(relaxed = true)

    every { mockConv1.key } returns "dm_user1" // Invalid (only 2 parts)
    every { mockConv2.key } returns "dm_user1_alice_extra" // Invalid (4 parts)
    every { mockConv3.key } returns "dm_alice_user1" // Valid
    every { mockAllConversationsSnapshot.children } returns listOf(mockConv1, mockConv2, mockConv3)

    val mockGetTask = Tasks.forResult(mockAllConversationsSnapshot)
    every { mockConversationsRef.get() } returns mockGetTask

    // Mock deleteConversation call for valid conversation
    val mockMessagesSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockMessagesGetTask = Tasks.forResult(mockMessagesSnapshot)
    val mockRemoveTask = Tasks.forResult<Void>(null)
    every { mockMessagesSnapshot.children } returns emptyList()
    every { mockMessagesRef.get() } returns mockMessagesGetTask
    every { mockConversationRef.removeValue() } returns mockRemoveTask

    // When
    repository.deleteAllUserConversations(userId)

    // Then
    verify { mockConversationsRef.get() }
    // Should only delete the valid DM conversation
    verify(atLeast = 1) { mockConversationRef.removeValue() }
  }

  @Test
  fun deleteAllUserConversations_deletesConversationsWithImages() = runTest {
    // Given
    val userId = "user1"
    val mockAllConversationsSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockConv = mockk<DataSnapshot>(relaxed = true)

    every { mockConv.key } returns "dm_alice_user1"
    every { mockAllConversationsSnapshot.children } returns listOf(mockConv)

    val mockGetTask = Tasks.forResult(mockAllConversationsSnapshot)
    every { mockConversationsRef.get() } returns mockGetTask

    // Mock conversation with image messages
    val mockMessagesSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockImageMessage = mockk<DataSnapshot>(relaxed = true)
    every { mockImageMessage.key } returns "img-1"
    every { mockImageMessage.child("type").getValue(String::class.java) } returns "IMAGE"
    every { mockMessagesSnapshot.children } returns listOf(mockImageMessage)

    val mockMessagesGetTask = Tasks.forResult(mockMessagesSnapshot)
    val mockRemoveTask = Tasks.forResult<Void>(null)
    val mockDeleteTask = Tasks.forResult<Void>(null)
    val mockImageRef = mockk<StorageReference>(relaxed = true)

    every { mockStorageRef.child("conversations") } returns mockStorageRef
    every { mockStorageRef.child("dm_alice_user1") } returns mockStorageRef
    every { mockStorageRef.child("images") } returns mockStorageRef
    every { mockStorageRef.child("img-1.jpg") } returns mockImageRef
    every { mockImageRef.delete() } returns mockDeleteTask

    every { mockMessagesRef.get() } returns mockMessagesGetTask
    every { mockConversationRef.removeValue() } returns mockRemoveTask

    // When
    repository.deleteAllUserConversations(userId)

    // Then
    verify { mockConversationsRef.get() }
    verify { mockImageRef.delete() }
    verify { mockConversationRef.removeValue() }
  }
}
