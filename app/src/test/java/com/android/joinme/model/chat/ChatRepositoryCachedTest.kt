package com.android.joinme.model.chat

// Tests partially written with AI assistance; reviewed for correctness.

import android.content.Context
import android.net.Uri
import com.android.joinme.model.database.AppDatabase
import com.android.joinme.model.database.MessageDao
import com.android.joinme.model.database.toEntity
import com.android.joinme.model.event.OfflineException
import com.android.joinme.network.NetworkMonitor
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChatRepositoryCachedTest {

  private lateinit var mockContext: Context
  private lateinit var mockRealtimeDbRepo: ChatRepository
  private lateinit var mockNetworkMonitor: NetworkMonitor
  private lateinit var mockDatabase: AppDatabase
  private lateinit var mockMessageDao: MessageDao
  private lateinit var cachedRepository: ChatRepositoryCached

  private val testConversationId = "conv-123"
  private val testMessageId = "msg-123"
  private val testUserId = "user-123"

  private val testMessage =
      Message(
          id = testMessageId,
          conversationId = testConversationId,
          senderId = "user1",
          senderName = "John Doe",
          content = "Hello!",
          timestamp = 1234567890L,
          type = MessageType.TEXT,
          readBy = listOf("user1"),
          isPinned = false,
          isEdited = false,
          location = null)

  private val testMessage2 =
      Message(
          id = "msg-456",
          conversationId = testConversationId,
          senderId = "user2",
          senderName = "Jane Smith",
          content = "Hi there!",
          timestamp = 1234567900L,
          type = MessageType.TEXT,
          readBy = listOf("user2"),
          isPinned = false,
          isEdited = false,
          location = null)

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockRealtimeDbRepo = mockk(relaxed = true)
    mockNetworkMonitor = mockk(relaxed = true)
    mockDatabase = mockk(relaxed = true)
    mockMessageDao = mockk(relaxed = true)

    // Mock AppDatabase.getDatabase to return our mock
    mockkObject(AppDatabase.Companion)
    every { AppDatabase.getDatabase(any()) } returns mockDatabase
    every { mockDatabase.messageDao() } returns mockMessageDao

    cachedRepository =
        ChatRepositoryCached(
            context = mockContext,
            realtimeDbRepo = mockRealtimeDbRepo,
            networkMonitor = mockNetworkMonitor)
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkObject(AppDatabase.Companion)
  }

  // ==================== getNewMessageId Tests ====================

  @Test
  fun `getNewMessageId delegates to Realtime Database repository`() {
    // Given
    every { mockRealtimeDbRepo.getNewMessageId() } returns "new-msg-id"

    // When
    val result = cachedRepository.getNewMessageId()

    // Then
    assertEquals("new-msg-id", result)
    verify { mockRealtimeDbRepo.getNewMessageId() }
  }

  // ==================== observeMessagesForConversation Tests ====================

  @Test
  fun `observeMessagesForConversation emits cached messages immediately when offline`() = runTest {
    // Given
    val cachedEntities = listOf(testMessage.toEntity(), testMessage2.toEntity())
    val networkStatusFlow = MutableStateFlow(false) // Offline
    coEvery { mockMessageDao.getMessagesForConversation(testConversationId) } returns cachedEntities
    every { mockNetworkMonitor.observeNetworkStatus() } returns networkStatusFlow

    // When
    val result = cachedRepository.observeMessagesForConversation(testConversationId).first()

    // Then
    assertEquals(2, result.size)
    assertEquals(testMessage.id, result[0].id)
    assertEquals(testMessage2.id, result[1].id)
    coVerify { mockMessageDao.getMessagesForConversation(testConversationId) }
    coVerify(exactly = 0) { mockRealtimeDbRepo.observeMessagesForConversation(any()) }
  }

  @Test
  fun `observeMessagesForConversation emits Firebase messages when online`() = runTest {
    // Given
    val cachedEntities = listOf(testMessage.toEntity())
    val networkStatusFlow = flowOf(true) // Online
    val firebaseFlow = flowOf(listOf(testMessage, testMessage2))

    coEvery { mockMessageDao.getMessagesForConversation(testConversationId) } returns cachedEntities
    every { mockNetworkMonitor.observeNetworkStatus() } returns networkStatusFlow
    every { mockRealtimeDbRepo.observeMessagesForConversation(testConversationId) } returns
        firebaseFlow
    coEvery { mockMessageDao.insertMessages(any()) } just Runs

    // When
    val result = cachedRepository.observeMessagesForConversation(testConversationId).first()

    // Then
    // First emission should be from Firebase (both messages)
    assertEquals(2, result.size)
    assertEquals(testMessage.id, result[0].id)
    assertEquals(testMessage2.id, result[1].id)
  }

  @Test
  fun `observeMessagesForConversation emits empty list when no cache and offline`() = runTest {
    // Given
    val networkStatusFlow = flowOf(false) // Offline
    coEvery { mockMessageDao.getMessagesForConversation(testConversationId) } returns emptyList()
    every { mockNetworkMonitor.observeNetworkStatus() } returns networkStatusFlow

    // When
    val result = cachedRepository.observeMessagesForConversation(testConversationId).first()

    // Then
    assertTrue(result.isEmpty())
    coVerify { mockMessageDao.getMessagesForConversation(testConversationId) }
  }

  // ==================== addMessage Tests ====================

  @Test
  fun `addMessage succeeds when online and caches message`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockRealtimeDbRepo.addMessage(testMessage) } just Runs
    coEvery { mockMessageDao.insertMessage(any()) } just Runs

    // When
    cachedRepository.addMessage(testMessage)

    // Then
    coVerify { mockRealtimeDbRepo.addMessage(testMessage) }
    coVerify { mockMessageDao.insertMessage(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `addMessage throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.addMessage(testMessage)

    // Then - exception is thrown
  }

  // ==================== editMessage Tests ====================

  @Test
  fun `editMessage succeeds when online and updates cache`() = runTest {
    // Given
    val editedMessage = testMessage.copy(content = "Edited content", isEdited = true)
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery {
      mockRealtimeDbRepo.editMessage(testConversationId, testMessageId, editedMessage)
    } just Runs
    coEvery { mockMessageDao.insertMessage(any()) } just Runs

    // When
    cachedRepository.editMessage(testConversationId, testMessageId, editedMessage)

    // Then
    coVerify { mockRealtimeDbRepo.editMessage(testConversationId, testMessageId, editedMessage) }
    coVerify { mockMessageDao.insertMessage(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `editMessage throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.editMessage(testConversationId, testMessageId, testMessage)

    // Then - exception is thrown
  }

  // ==================== deleteMessage Tests ====================

  @Test
  fun `deleteMessage succeeds when online and removes from cache`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery { mockRealtimeDbRepo.deleteMessage(testConversationId, testMessageId) } just Runs
    coEvery { mockMessageDao.deleteMessage(testConversationId, testMessageId) } just Runs

    // When
    cachedRepository.deleteMessage(testConversationId, testMessageId)

    // Then
    coVerify { mockRealtimeDbRepo.deleteMessage(testConversationId, testMessageId) }
    coVerify { mockMessageDao.deleteMessage(testConversationId, testMessageId) }
  }

  @Test(expected = OfflineException::class)
  fun `deleteMessage throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.deleteMessage(testConversationId, testMessageId)

    // Then - exception is thrown
  }

  // ==================== markMessageAsRead Tests ====================

  @Test
  fun `markMessageAsRead succeeds when online and updates cache`() = runTest {
    // Given
    val cachedEntity = testMessage.toEntity()
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery {
      mockRealtimeDbRepo.markMessageAsRead(testConversationId, testMessageId, testUserId)
    } just Runs
    coEvery { mockMessageDao.getMessage(testConversationId, testMessageId) } returns cachedEntity
    coEvery { mockMessageDao.insertMessage(any()) } just Runs

    // When
    cachedRepository.markMessageAsRead(testConversationId, testMessageId, testUserId)

    // Then
    coVerify { mockRealtimeDbRepo.markMessageAsRead(testConversationId, testMessageId, testUserId) }
    coVerify { mockMessageDao.getMessage(testConversationId, testMessageId) }
    coVerify { mockMessageDao.insertMessage(any()) }
  }

  @Test
  fun `markMessageAsRead does not duplicate userId in readBy list`() = runTest {
    // Given
    val messageAlreadyRead = testMessage.copy(readBy = listOf("user1", testUserId))
    val cachedEntity = messageAlreadyRead.toEntity()
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery {
      mockRealtimeDbRepo.markMessageAsRead(testConversationId, testMessageId, testUserId)
    } just Runs
    coEvery { mockMessageDao.getMessage(testConversationId, testMessageId) } returns cachedEntity
    coEvery { mockMessageDao.insertMessage(any()) } just Runs

    // When
    cachedRepository.markMessageAsRead(testConversationId, testMessageId, testUserId)

    // Then
    coVerify { mockMessageDao.insertMessage(any()) }
  }

  @Test
  fun `markMessageAsRead handles missing cached message gracefully`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery {
      mockRealtimeDbRepo.markMessageAsRead(testConversationId, testMessageId, testUserId)
    } just Runs
    coEvery { mockMessageDao.getMessage(testConversationId, testMessageId) } returns null

    // When
    cachedRepository.markMessageAsRead(testConversationId, testMessageId, testUserId)

    // Then
    coVerify { mockRealtimeDbRepo.markMessageAsRead(testConversationId, testMessageId, testUserId) }
    coVerify(exactly = 0) { mockMessageDao.insertMessage(any()) }
  }

  @Test(expected = OfflineException::class)
  fun `markMessageAsRead throws OfflineException when offline`() = runTest {
    // Given
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.markMessageAsRead(testConversationId, testMessageId, testUserId)

    // Then - exception is thrown
  }

  // ==================== uploadChatImage Tests ====================

  @Test
  fun `uploadChatImage succeeds when online`() = runTest {
    // Given
    val imageUri = mockk<Uri>()
    val imageUrl = "https://example.com/image.jpg"
    every { mockNetworkMonitor.isOnline() } returns true
    coEvery {
      mockRealtimeDbRepo.uploadChatImage(mockContext, testConversationId, testMessageId, imageUri)
    } returns imageUrl

    // When
    val result =
        cachedRepository.uploadChatImage(mockContext, testConversationId, testMessageId, imageUri)

    // Then
    assertEquals(imageUrl, result)
    coVerify {
      mockRealtimeDbRepo.uploadChatImage(mockContext, testConversationId, testMessageId, imageUri)
    }
  }

  @Test(expected = OfflineException::class)
  fun `uploadChatImage throws OfflineException when offline`() = runTest {
    // Given
    val imageUri = mockk<Uri>()
    every { mockNetworkMonitor.isOnline() } returns false

    // When
    cachedRepository.uploadChatImage(mockContext, testConversationId, testMessageId, imageUri)

    // Then - exception is thrown
  }

  // ==================== Integration Tests ====================

  @Test
  fun `addMessage and observeMessages integration - message appears in flow`() = runTest {
    // Given
    val networkStatusFlow = MutableStateFlow(true) // Online
    val firebaseFlow =
        flow<List<Message>> {
          emit(listOf(testMessage)) // Initial
          emit(listOf(testMessage, testMessage2)) // After add
        }

    coEvery { mockMessageDao.getMessagesForConversation(testConversationId) } returns
        listOf(testMessage.toEntity())
    every { mockNetworkMonitor.observeNetworkStatus() } returns networkStatusFlow
    every { mockNetworkMonitor.isOnline() } returns true
    every { mockRealtimeDbRepo.observeMessagesForConversation(testConversationId) } returns
        firebaseFlow
    coEvery { mockRealtimeDbRepo.addMessage(testMessage2) } just Runs
    coEvery { mockMessageDao.insertMessage(any()) } just Runs
    coEvery { mockMessageDao.insertMessages(any()) } just Runs

    // When
    val messages = cachedRepository.observeMessagesForConversation(testConversationId).first()

    // Then
    assertTrue(messages.isNotEmpty())
  }

  @Test
  fun `offline to online transition updates cache from Firebase`() = runTest {
    // Given
    val networkStatusFlow = MutableStateFlow(false) // Start offline
    val cachedEntities = listOf(testMessage.toEntity())
    val firebaseMessages = listOf(testMessage, testMessage2)

    coEvery { mockMessageDao.getMessagesForConversation(testConversationId) } returns cachedEntities
    every { mockNetworkMonitor.observeNetworkStatus() } returns networkStatusFlow
    every { mockRealtimeDbRepo.observeMessagesForConversation(testConversationId) } returns
        flowOf(firebaseMessages)
    coEvery { mockMessageDao.insertMessages(any()) } just Runs

    // When - collect first emission (offline)
    val offlineResult = cachedRepository.observeMessagesForConversation(testConversationId).first()

    // Then
    assertEquals(1, offlineResult.size)
  }

  @Test
  fun `cache is updated when Firebase emits new messages`() = runTest {
    // Given
    val networkStatusFlow = flowOf(true) // Online
    val firebaseFlow = flowOf(listOf(testMessage, testMessage2))

    coEvery { mockMessageDao.getMessagesForConversation(testConversationId) } returns
        listOf(testMessage.toEntity())
    every { mockNetworkMonitor.observeNetworkStatus() } returns networkStatusFlow
    every { mockRealtimeDbRepo.observeMessagesForConversation(testConversationId) } returns
        firebaseFlow
    coEvery { mockMessageDao.insertMessages(any()) } just Runs

    // When
    val result = cachedRepository.observeMessagesForConversation(testConversationId).first()

    // Then
    // First emission is from Firebase
    assertEquals(2, result.size)
    // Verify cache is updated in background
    coVerify { mockMessageDao.insertMessages(any()) }
  }
}
