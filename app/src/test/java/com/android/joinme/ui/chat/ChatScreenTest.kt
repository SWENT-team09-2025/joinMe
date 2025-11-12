package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.joinme.model.chat.ChatRepository
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.ui.theme.SocialColor
import com.android.joinme.ui.theme.SportsColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test suite for ChatScreen UI component
 *
 * Tests the chat interface including message display, input handling, and user interactions
 */
@RunWith(RobolectricTestRunner::class)
class ChatScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakeChatRepository: FakeChatRepository
  private lateinit var viewModel: ChatViewModel

  @Before
  fun setup() {
    fakeChatRepository = FakeChatRepository()
    viewModel = ChatViewModel(fakeChatRepository)
  }

  // ============================================================================
  // Basic Display Tests
  // ============================================================================

  @Test
  fun chatScreen_displaysCorrectTitle() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Basketball Game",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel,
          topBarColor = SportsColor)
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Basketball Game").assertIsDisplayed()
  }

  @Test
  fun chatScreen_displaysTopBar() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.LEAVE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun chatScreen_displaysMessageInput() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertIsDisplayed()
  }

  // ============================================================================
  // Message Display Tests
  // ============================================================================

  @Test
  fun chatScreen_displaysEmptyState_whenNoMessages() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithText("No messages yet. Start the conversation!").assertIsDisplayed()
  }

  @Test
  fun chatScreen_displaysMessages_whenMessagesExist() {
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "Hello everyone!",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT))

    fakeChatRepository.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Hello everyone!").assertIsDisplayed()
  }

  @Test
  fun chatScreen_displaysMultipleMessages() {
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "First message",
                timestamp = System.currentTimeMillis() - 2000,
                type = MessageType.TEXT),
            Message(
                id = "msg2",
                conversationId = "chat1",
                senderId = "user2",
                senderName = "Bob",
                content = "Second message",
                timestamp = System.currentTimeMillis() - 1000,
                type = MessageType.TEXT),
            Message(
                id = "msg3",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "Third message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT))

    fakeChatRepository.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("First message").assertIsDisplayed()
    composeTestRule.onNodeWithText("Second message").assertIsDisplayed()
    composeTestRule.onNodeWithText("Third message").assertIsDisplayed()
  }

  @Test
  fun chatScreen_displaysSenderName_forOtherUsers() {
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user2",
                senderName = "Bob",
                content = "Hello!",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT))

    fakeChatRepository.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    composeTestRule.onNodeWithText("Hello!").assertIsDisplayed()
  }

  // ============================================================================
  // Message Input Tests
  // ============================================================================

  @Test
  fun messageInput_sendButton_isDisabled_whenInputIsEmpty() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun messageInput_sendButton_isEnabled_whenInputHasText() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput("Hello!")

    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertIsEnabled()
  }

  @Test
  fun messageInput_acceptsTextInput() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    val testMessage = "This is a test message"
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput(testMessage)

    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).assertTextEquals(testMessage)
  }

  @Test
  fun messageInput_sendButton_sendsMessage() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    val testMessage = "Test message"
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput(testMessage)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify message was sent through repository
    assert(fakeChatRepository.sentMessages.any { it.content == testMessage })
  }

  @Test
  fun messageInput_doesNotSend_whenInputIsWhitespace() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput("   ")
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify no message was sent
    assert(fakeChatRepository.sentMessages.isEmpty())
  }

  // ============================================================================
  // Navigation Tests
  // ============================================================================

  @Test
  fun backButton_triggersCallback() {
    var backClicked = false

    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel,
          onBackClick = { backClicked = true })
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.BACK_BUTTON).performClick()

    assert(backClicked)
  }

  // ============================================================================
  // Color Theme Tests
  // ============================================================================

  @Test
  fun chatScreen_acceptsDifferentTopBarColors() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Social Event",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel,
          topBarColor = SocialColor)
    }

    // Verify screen displays without errors
    composeTestRule.onNodeWithTag(ChatScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithText("Social Event").assertIsDisplayed()
  }

  // ============================================================================
  // Test Tags Tests
  // ============================================================================

  @Test
  fun chatScreen_hasCorrectTestTags() {
    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    // Verify all major test tags exist
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.TOP_BAR).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.BACK_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.LEAVE_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.TITLE).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_LIST).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertExists()
  }

  @Test
  fun messageItem_hasCorrectTestTag() {
    val messages =
        listOf(
            Message(
                id = "msg123",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "Test message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT))

    fakeChatRepository.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("msg123")).assertExists()
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.getTestTagForMessageBubble("msg123"))
        .assertExists()
  }

  // ============================================================================
  // Fake Repository for Testing
  // ============================================================================

  private class FakeChatRepository : ChatRepository {
    private val messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    private var messageIdCounter = 0
    val sentMessages = mutableListOf<Message>()

    fun setMessages(messages: List<Message>) {
      messagesFlow.value = messages
    }

    override fun getNewMessageId(): String {
      return "msg_${messageIdCounter++}"
    }

    override fun observeMessagesForConversation(conversationId: String): Flow<List<Message>> {
      return messagesFlow
    }

    override suspend fun addMessage(message: Message) {
      sentMessages.add(message)
      val currentMessages = messagesFlow.value.toMutableList()
      currentMessages.add(message)
      messagesFlow.value = currentMessages
    }

    override suspend fun editMessage(conversationId: String, messageId: String, newValue: Message) {
      val currentMessages = messagesFlow.value.toMutableList()
      val index = currentMessages.indexOfFirst { it.id == messageId }
      if (index != -1) {
        currentMessages[index] = newValue
        messagesFlow.value = currentMessages
      }
    }

    override suspend fun deleteMessage(conversationId: String, messageId: String) {
      val currentMessages = messagesFlow.value.toMutableList()
      currentMessages.removeAll { it.id == messageId }
      messagesFlow.value = currentMessages
    }

    override suspend fun markMessageAsRead(
        conversationId: String,
        messageId: String,
        userId: String
    ) {
      // No-op for testing
    }
  }
}
