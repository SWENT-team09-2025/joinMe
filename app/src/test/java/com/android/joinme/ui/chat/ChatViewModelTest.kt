package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import com.android.joinme.model.chat.ChatRepository
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ChatViewModel.
 *
 * These tests use a fake repository to verify ViewModel behavior without Firebase dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

  private class FakeProfileRepository : ProfileRepository {
    private val profiles = mutableMapOf<String, Profile>()
    var shouldThrowError = false

    fun addProfile(profile: Profile) {
      profiles[profile.uid] = profile
    }

    override suspend fun getProfile(uid: String): Profile? {
      if (shouldThrowError) throw Exception("Profile fetch error")
      return profiles[uid]
    }

    override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? {
      if (shouldThrowError) throw Exception("Profiles fetch error")
      return uids.mapNotNull { profiles[it] }
    }

    override suspend fun createOrUpdateProfile(profile: Profile) {
      profiles[profile.uid] = profile
    }

    override suspend fun deleteProfile(uid: String) {
      profiles.remove(uid)
    }

    override suspend fun uploadProfilePhoto(
        context: android.content.Context,
        uid: String,
        imageUri: android.net.Uri
    ): String {
      return "https://example.com/photo.jpg"
    }

    override suspend fun deleteProfilePhoto(uid: String) {
      profiles[uid]?.let { profiles[uid] = it.copy(photoUrl = null) }
    }
  }

  private class FakeChatRepository : ChatRepository {
    var shouldThrowError = false
    var errorMessage = "Test error"
    private val messagesByChat = mutableMapOf<String, MutableList<Message>>()
    private val messagesFlows = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    private var counter = 0

    fun setMessages(chatId: String, newMessages: List<Message>) {
      val chatMessages = messagesByChat.getOrPut(chatId) { mutableListOf() }
      chatMessages.clear()
      chatMessages.addAll(newMessages)

      val flow = messagesFlows.getOrPut(chatId) { MutableStateFlow(emptyList()) }
      flow.value = chatMessages.sortedBy { it.timestamp }
    }

    override fun getNewMessageId(): String {
      return (counter++).toString()
    }

    override fun observeMessagesForConversation(conversationId: String): Flow<List<Message>> {
      return messagesFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
    }

    override suspend fun addMessage(message: Message) {
      if (shouldThrowError) {
        throw Exception(errorMessage)
      }
      // Add to the specific conversation based on conversationId
      val conversationId = message.conversationId
      val messages = messagesByChat.getOrPut(conversationId) { mutableListOf() }
      messages.add(message)
      messagesFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }.value =
          messages.sortedBy { it.timestamp }
    }

    override suspend fun editMessage(conversationId: String, messageId: String, newValue: Message) {
      if (shouldThrowError) {
        throw Exception(errorMessage)
      }
      val messages = messagesByChat[conversationId]
      if (messages != null) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
          messages[index] = newValue
          messagesFlows[conversationId]?.value = messages.sortedBy { it.timestamp }
          return
        }
      }
      throw Exception("Message not found")
    }

    override suspend fun deleteMessage(conversationId: String, messageId: String) {
      if (shouldThrowError) {
        throw Exception(errorMessage)
      }
      val messages = messagesByChat[conversationId]
      if (messages != null) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
          messages.removeAt(index)
          messagesFlows[conversationId]?.value = messages.sortedBy { it.timestamp }
          return
        }
      }
      throw Exception("Message not found")
    }

    override suspend fun markMessageAsRead(
        conversationId: String,
        messageId: String,
        userId: String
    ) {
      if (shouldThrowError && errorMessage != "Read receipt error") {
        throw Exception(errorMessage)
      }
      val messages = messagesByChat[conversationId]
      if (messages != null) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
          val message = messages[index]
          if (userId !in message.readBy) {
            messages[index] = message.copy(readBy = message.readBy + userId)
            messagesFlows[conversationId]?.value = messages.sortedBy { it.timestamp }
          }
          return
        }
      }
    }
  }

  private lateinit var fakeRepo: FakeChatRepository
  private lateinit var fakeProfileRepo: FakeProfileRepository
  private lateinit var viewModel: ChatViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val testChatId = "chat123"
  private val testUserId = "user123"

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeRepo = FakeChatRepository()
    fakeProfileRepo = FakeProfileRepository()
    viewModel = ChatViewModel(fakeRepo, fakeProfileRepo)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initializeChat_loadsMessagesAutomatically() = runTest {
    val testMessages =
        listOf(
            Message(
                id = "1",
                conversationId = testChatId,
                senderId = "user1",
                senderName = "Alice",
                content = "Hello",
                timestamp = 1000L),
            Message(
                id = "2",
                conversationId = testChatId,
                senderId = "user2",
                senderName = "Bob",
                content = "Hi there",
                timestamp = 2000L))
    fakeRepo.setMessages(testChatId, testMessages)

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.messages.size)
    assertEquals("Hello", state.messages[0].content)
    assertEquals("Hi there", state.messages[1].content)
    assertEquals(testUserId, state.currentUserId)
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun initializeChat_withEmptyChat_returnsEmptyList() = runTest {
    fakeRepo.setMessages(testChatId, emptyList())

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.messages.isEmpty())
    assertEquals(testUserId, state.currentUserId)
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun initializeChat_filtersByChatId() = runTest {
    val chat1Messages =
        listOf(
            Message(
                id = "1",
                conversationId = testChatId,
                senderId = "user1",
                senderName = "Alice",
                content = "Message in test chat",
                timestamp = 1000L))
    val chat2Messages =
        listOf(
            Message(
                id = "2",
                conversationId = "otherChat",
                senderId = "user2",
                senderName = "Bob",
                content = "Message in other chat",
                timestamp = 2000L))
    fakeRepo.setMessages(testChatId, chat1Messages)
    fakeRepo.setMessages("otherChat", chat2Messages)

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.messages.size)
    assertEquals("Message in test chat", state.messages[0].content)
  }

  @Test
  fun sendMessage_addsMessageToChat() = runTest {
    fakeRepo.setMessages(testChatId, emptyList())
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.sendMessage("Hello World", "Alice")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.messages.size)
    assertEquals("Hello World", state.messages[0].content)
    assertEquals(testUserId, state.messages[0].senderId)
    assertEquals("Alice", state.messages[0].senderName)
    assertEquals(MessageType.TEXT, state.messages[0].type)
  }

  @Test
  fun sendMessage_withEmptyContent_setsErrorMessage() = runTest {
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.sendMessage("", "Alice")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Message cannot be empty", state.errorMsg)
    assertTrue(state.messages.isEmpty())
  }

  @Test
  fun sendMessage_withBlankContent_setsErrorMessage() = runTest {
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.sendMessage("   ", "Alice")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Message cannot be empty", state.errorMsg)
    assertTrue(state.messages.isEmpty())
  }

  @Test
  fun sendMessage_withSystemType_allowsEmptyContent() = runTest {
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.sendMessage("User joined", "System", MessageType.SYSTEM)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.messages.size)
    assertEquals(MessageType.SYSTEM, state.messages[0].type)
  }

  @Test
  fun sendMessage_withRepositoryError_setsErrorMessage() = runTest {
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    fakeRepo.shouldThrowError = true
    fakeRepo.errorMessage = "Network error"
    viewModel.sendMessage("Test message", "Alice")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to send message: Network error", state.errorMsg)
  }

  @Test
  fun editMessage_updatesMessageContent() = runTest {
    val originalMessage =
        Message(
            id = "1",
            conversationId = testChatId,
            senderId = testUserId,
            senderName = "Alice",
            content = "Original",
            timestamp = 1000L)
    fakeRepo.setMessages(testChatId, listOf(originalMessage))

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.editMessage("1", "Updated")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.messages.size)
    assertEquals("Updated", state.messages[0].content)
  }

  @Test
  fun editMessage_withEmptyContent_setsErrorMessage() = runTest {
    val originalMessage =
        Message(
            id = "1",
            conversationId = testChatId,
            senderId = testUserId,
            senderName = "Alice",
            content = "Original",
            timestamp = 1000L)
    fakeRepo.setMessages(testChatId, listOf(originalMessage))

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.editMessage("1", "")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Message cannot be empty", state.errorMsg)
    assertEquals("Original", state.messages[0].content)
  }

  @Test
  fun editMessage_byNonOwner_setsErrorMessage() = runTest {
    val originalMessage =
        Message(
            id = "1",
            conversationId = testChatId,
            senderId = "otherUser",
            senderName = "Bob",
            content = "Original",
            timestamp = 1000L)
    fakeRepo.setMessages(testChatId, listOf(originalMessage))

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.editMessage("1", "Hacked!")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("You can only edit your own messages", state.errorMsg)
    assertEquals("Original", state.messages[0].content)
  }

  @Test
  fun editMessage_notFound_setsErrorMessage() = runTest {
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.editMessage("999", "Updated")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Message not found", state.errorMsg)
  }

  @Test
  fun deleteMessage_removesMessage() = runTest {
    val message =
        Message(
            id = "1",
            conversationId = testChatId,
            senderId = testUserId,
            senderName = "Alice",
            content = "To be deleted",
            timestamp = 1000L)
    fakeRepo.setMessages(testChatId, listOf(message))

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()
    assertEquals(1, viewModel.uiState.value.messages.size)

    viewModel.deleteMessage("1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.messages.isEmpty())
  }

  @Test
  fun deleteMessage_byNonOwner_setsErrorMessage() = runTest {
    val message =
        Message(
            id = "1",
            conversationId = testChatId,
            senderId = "otherUser",
            senderName = "Bob",
            content = "Cannot delete",
            timestamp = 1000L)
    fakeRepo.setMessages(testChatId, listOf(message))

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.deleteMessage("1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("You can only delete your own messages", state.errorMsg)
    assertEquals(1, state.messages.size)
  }

  @Test
  fun deleteMessage_notFound_setsErrorMessage() = runTest {
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.deleteMessage("999")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Message not found", state.errorMsg)
  }

  @Test
  fun markMessageAsRead_updatesReadByList() = runTest {
    val message =
        Message(
            id = "1",
            conversationId = testChatId,
            senderId = "otherUser",
            senderName = "Alice",
            content = "Read me",
            timestamp = 1000L,
            readBy = emptyList())
    fakeRepo.setMessages(testChatId, listOf(message))

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.markMessageAsRead("1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.messages[0].readBy.contains(testUserId))
  }

  @Test
  fun markMessageAsRead_withError_doesNotDisruptUI() = runTest {
    val message =
        Message(
            id = "1",
            conversationId = testChatId,
            senderId = "otherUser",
            senderName = "Alice",
            content = "Read me",
            timestamp = 1000L)
    fakeRepo.setMessages(testChatId, listOf(message))

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    fakeRepo.shouldThrowError = true
    fakeRepo.errorMessage = "Read receipt error"
    viewModel.markMessageAsRead("1")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    // Should not set error message for read receipts
    assertNull(state.errorMsg)
  }

  @Test
  fun markAllMessagesAsRead_marksAllUnreadMessages() = runTest {
    val messages =
        listOf(
            Message(
                id = "1",
                conversationId = testChatId,
                senderId = "user1",
                senderName = "Alice",
                content = "Message 1",
                timestamp = 1000L,
                readBy = emptyList()),
            Message(
                id = "2",
                conversationId = testChatId,
                senderId = "user2",
                senderName = "Bob",
                content = "Message 2",
                timestamp = 2000L,
                readBy = emptyList()),
            Message(
                id = "3",
                conversationId = testChatId,
                senderId = "user3",
                senderName = "Charlie",
                content = "Message 3",
                timestamp = 3000L,
                readBy = listOf(testUserId)))
    fakeRepo.setMessages(testChatId, messages)

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.markAllMessagesAsRead()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.messages[0].readBy.contains(testUserId))
    assertTrue(state.messages[1].readBy.contains(testUserId))
    assertTrue(state.messages[2].readBy.contains(testUserId))
  }

  @Test
  fun getUnreadCount_returnsCorrectCount() = runTest {
    val messages =
        listOf(
            Message(
                id = "1",
                conversationId = testChatId,
                senderId = "user1",
                senderName = "Alice",
                content = "Unread 1",
                timestamp = 1000L,
                readBy = emptyList()),
            Message(
                id = "2",
                conversationId = testChatId,
                senderId = "user2",
                senderName = "Bob",
                content = "Unread 2",
                timestamp = 2000L,
                readBy = emptyList()),
            Message(
                id = "3",
                conversationId = testChatId,
                senderId = "user3",
                senderName = "Charlie",
                content = "Read",
                timestamp = 3000L,
                readBy = listOf(testUserId)),
            Message(
                id = "4",
                conversationId = testChatId,
                senderId = testUserId,
                senderName = "Me",
                content = "My message",
                timestamp = 4000L,
                readBy = emptyList()))
    fakeRepo.setMessages(testChatId, messages)

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    val unreadCount = viewModel.getUnreadCount()
    assertEquals(2, unreadCount)
  }

  @Test
  fun getUnreadCount_withNoUnreadMessages_returnsZero() = runTest {
    val messages =
        listOf(
            Message(
                id = "1",
                conversationId = testChatId,
                senderId = "user1",
                senderName = "Alice",
                content = "Read",
                timestamp = 1000L,
                readBy = listOf(testUserId)))
    fakeRepo.setMessages(testChatId, messages)

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    val unreadCount = viewModel.getUnreadCount()
    assertEquals(0, unreadCount)
  }

  @Test
  fun clearErrorMsg_removesErrorMessage() = runTest {
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.sendMessage("", "Alice")
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun clearErrorMsg_doesNotAffectOtherState() = runTest {
    val message =
        Message(
            id = "1",
            conversationId = testChatId,
            senderId = testUserId,
            senderName = "Alice",
            content = "Test",
            timestamp = 1000L)
    fakeRepo.setMessages(testChatId, listOf(message))

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.sendMessage("", "Alice")
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    val state = viewModel.uiState.value
    assertNull(state.errorMsg)
    assertEquals(1, state.messages.size)
    assertEquals(testUserId, state.currentUserId)
  }

  @Test
  fun observeMessages_receivesRealTimeUpdates() = runTest {
    fakeRepo.setMessages(testChatId, emptyList())
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.messages.isEmpty())

    val newMessage =
        Message(
            id = "1",
            conversationId = testChatId,
            senderId = "user1",
            senderName = "Alice",
            content = "New message",
            timestamp = 1000L)
    fakeRepo.setMessages(testChatId, listOf(newMessage))
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.messages.size)
    assertEquals("New message", state.messages[0].content)
  }

  @Test
  fun observeMessages_sortsByTimestamp() = runTest {
    val messages =
        listOf(
            Message(
                id = "1",
                conversationId = testChatId,
                senderId = "user1",
                senderName = "Alice",
                content = "Third",
                timestamp = 3000L),
            Message(
                id = "2",
                conversationId = testChatId,
                senderId = "user2",
                senderName = "Bob",
                content = "First",
                timestamp = 1000L),
            Message(
                id = "3",
                conversationId = testChatId,
                senderId = "user3",
                senderName = "Charlie",
                content = "Second",
                timestamp = 2000L))
    fakeRepo.setMessages(testChatId, messages)

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(3, state.messages.size)
    assertEquals("First", state.messages[0].content)
    assertEquals("Second", state.messages[1].content)
    assertEquals("Third", state.messages[2].content)
  }

  @Test
  fun sendMessage_setsCorrectTimestamp() = runTest {
    fakeRepo.setMessages(testChatId, emptyList())
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    val beforeTime = System.currentTimeMillis()
    viewModel.sendMessage("Test", "Alice")
    advanceUntilIdle()
    val afterTime = System.currentTimeMillis()

    val state = viewModel.uiState.value
    val messageTimestamp = state.messages[0].timestamp
    assertTrue(messageTimestamp >= beforeTime)
    assertTrue(messageTimestamp <= afterTime)
  }

  @Test
  fun sendMessage_generatesUniqueIds() = runTest {
    fakeRepo.setMessages(testChatId, emptyList())
    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    viewModel.sendMessage("Message 1", "Alice")
    advanceUntilIdle()
    viewModel.sendMessage("Message 2", "Alice")
    advanceUntilIdle()
    viewModel.sendMessage("Message 3", "Alice")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(3, state.messages.size)
    val ids = state.messages.map { it.id }.toSet()
    assertEquals(3, ids.size) // All IDs should be unique
  }

  @Test
  fun multipleChats_areIsolated() = runTest {
    val chat1Messages =
        listOf(
            Message(
                id = "1",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "Chat 1 message",
                timestamp = 1000L))
    val chat2Messages =
        listOf(
            Message(
                id = "2",
                conversationId = "chat2",
                senderId = "user2",
                senderName = "Bob",
                content = "Chat 2 message",
                timestamp = 2000L))
    fakeRepo.setMessages("chat1", chat1Messages)
    fakeRepo.setMessages("chat2", chat2Messages)

    viewModel.initializeChat("chat1", testUserId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(1, state.messages.size)
    assertEquals("Chat 1 message", state.messages[0].content)
  }

  @Test
  fun fetchSenderProfiles_loadsProfilesCorrectlyAndHandlesErrors() = runTest {
    // Test 1: Load profiles for multiple senders (including one with no photo)
    fakeProfileRepo.addProfile(
        Profile(uid = "user1", photoUrl = "https://example.com/user1.jpg", username = "Alice"))
    fakeProfileRepo.addProfile(Profile(uid = "user2", photoUrl = null, username = "Bob"))

    val testMessages =
        listOf(
            Message(
                id = "1",
                conversationId = testChatId,
                senderId = "user1",
                senderName = "Alice",
                content = "Hello",
                timestamp = 1000L),
            Message(
                id = "2",
                conversationId = testChatId,
                senderId = "user2",
                senderName = "Bob",
                content = "Hi",
                timestamp = 2000L),
            Message(
                id = "3",
                conversationId = testChatId,
                senderId = "user3", // This user doesn't have a profile
                senderName = "Charlie",
                content = "Hey",
                timestamp = 3000L))
    fakeRepo.setMessages(testChatId, testMessages)

    viewModel.initializeChat(testChatId, testUserId)
    advanceUntilIdle()

    var state = viewModel.uiState.value
    // Should have profiles for user1 and user2 (user3 not found is handled gracefully)
    assertEquals(2, state.senderProfiles.size)
    assertEquals("https://example.com/user1.jpg", state.senderProfiles["user1"]?.photoUrl)
    assertNull(state.senderProfiles["user2"]?.photoUrl) // Bob has no photo
    assertNull(state.senderProfiles["user3"]) // Charlie not found
    assertNull(state.errorMsg) // No error shown for missing profiles

    // Test 2: Verify deduplication - same sender with multiple messages
    val moreMessages =
        listOf(
            Message(
                id = "4",
                conversationId = testChatId,
                senderId = "user1",
                senderName = "Alice",
                content = "Another message",
                timestamp = 4000L))
    fakeRepo.setMessages(testChatId, testMessages + moreMessages)
    advanceUntilIdle()

    state = viewModel.uiState.value
    // Should still have only 2 profiles (user1 profile not duplicated)
    assertEquals(2, state.senderProfiles.size)

    // Test 3: Handles repository error gracefully
    fakeProfileRepo.shouldThrowError = true
    fakeRepo.setMessages(testChatId, emptyList())
    val newMessages =
        listOf(
            Message(
                id = "5",
                conversationId = testChatId,
                senderId = "user4",
                senderName = "Dave",
                content = "Test",
                timestamp = 5000L))
    fakeRepo.setMessages(testChatId, newMessages)
    advanceUntilIdle()

    state = viewModel.uiState.value
    // Should not crash, profiles may be empty or unchanged
    assertNull(state.errorMsg) // Profile errors should not be shown to user
  }
}
