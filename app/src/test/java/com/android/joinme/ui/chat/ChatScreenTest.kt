package com.android.joinme.ui.chat

// Implemented with help of Claude AI

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.android.joinme.R
import com.android.joinme.model.chat.ChatRepository
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
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
  private lateinit var fakeProfileRepository: FakeProfileRepository
  private lateinit var viewModel: ChatViewModel

  @Before
  fun setup() {
    fakeChatRepository = FakeChatRepository()
    fakeProfileRepository = FakeProfileRepository()
    viewModel = ChatViewModel(fakeChatRepository, fakeProfileRepository)
  }

  // ============================================================================
  // Helper functions
  // ============================================================================

  private fun setupChatScreen(
      chatId: String = "chat1",
      chatTitle: String = "Test Chat",
      currentUserId: String = "user1",
      currentUserName: String = "Alice"
  ) {
    composeTestRule.setContent {
      ChatScreen(chatId, chatTitle, currentUserId, currentUserName, viewModel)
    }
  }

  private fun createMessage(
      id: String,
      senderId: String = "user1",
      senderName: String = "Alice",
      content: String,
      timestampOffset: Long = 0
  ) =
      Message(
          id = id,
          conversationId = "chat1",
          senderId = senderId,
          senderName = senderName,
          content = content,
          timestamp = System.currentTimeMillis() - timestampOffset,
          type = MessageType.TEXT)

  // ============================================================================
  // Basic Display Tests
  // ============================================================================

  @Test
  fun chatScreen_displaysAllUIElements() {
    setupChatScreen(chatTitle = "Basketball Game")

    // Verify all major UI elements are displayed
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.LEAVE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Basketball Game").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_LIST).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).assertIsDisplayed()
    // Mic button is shown when text field is empty (send button appears when typing)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MIC_BUTTON).assertIsDisplayed()
  }

  // ============================================================================
  // Message Display Tests
  // ============================================================================

  @Test
  fun chatScreen_displaysEmptyState_whenNoMessages() {
    setupChatScreen()

    composeTestRule.onNodeWithText("No messages yet. Start the conversation!").assertIsDisplayed()
  }

  @Test
  fun chatScreen_displaysMessages_whenMessagesExist() {
    val messages = listOf(createMessage(id = "msg1", content = "Hello everyone!"))
    fakeChatRepository.setMessages(messages)

    setupChatScreen()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Hello everyone!").assertIsDisplayed()
  }

  @Test
  fun chatScreen_displaysMultipleMessages() {
    val messages =
        listOf(
            createMessage(id = "msg1", content = "First message", timestampOffset = 2000),
            createMessage(
                id = "msg2",
                senderId = "user2",
                senderName = "Bob",
                content = "Second message",
                timestampOffset = 1000),
            createMessage(id = "msg3", content = "Third message"))
    fakeChatRepository.setMessages(messages)

    setupChatScreen()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("First message").assertIsDisplayed()
    composeTestRule.onNodeWithText("Second message").assertIsDisplayed()
    composeTestRule.onNodeWithText("Third message").assertIsDisplayed()
  }

  @Test
  fun chatScreen_displaysSenderName_forOtherUsers() {
    val messages =
        listOf(
            createMessage(id = "msg1", senderId = "user2", senderName = "Bob", content = "Hello!"))
    fakeChatRepository.setMessages(messages)

    setupChatScreen()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    composeTestRule.onNodeWithText("Hello!").assertIsDisplayed()
  }

  // ============================================================================
  // Message Input Tests
  // ============================================================================

  @Test
  fun messageInput_sendButtonAppearsAndIsEnabled_whenTextEntered() {
    setupChatScreen()

    // Initially mic button is shown, not send button
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MIC_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertDoesNotExist()

    // When input has text, send button appears and is enabled
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput("Hello!")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertIsEnabled()
  }

  @Test
  fun messageInput_acceptsTextInput() {
    setupChatScreen()

    val testMessage = "This is a test message"
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput(testMessage)

    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).assertTextEquals(testMessage)
  }

  @Test
  fun messageInput_sendButton_sendsMessage() {
    setupChatScreen()

    val testMessage = "Test message"
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput(testMessage)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify message was sent through repository
    assert(fakeChatRepository.sentMessages.any { it.content == testMessage })
  }

  @Test
  fun messageInput_doesNotSend_whenInputIsWhitespace() {
    setupChatScreen()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput("   ")
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify no message was sent
    assert(fakeChatRepository.sentMessages.isEmpty())
  }

  // ============================================================================
  // Test Tags Tests
  // ============================================================================

  @Test
  fun messageItem_hasCorrectTestTag() {
    val messages = listOf(createMessage(id = "msg123", content = "Test message"))
    fakeChatRepository.setMessages(messages)

    setupChatScreen()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("msg123")).assertExists()
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.getTestTagForMessageBubble("msg123"))
        .assertExists()
  }

  // ============================================================================
  // Attachment Button Tests
  // ============================================================================

  @Test
  fun attachmentButton_opensAttachmentMenu_whenClicked() {
    setupChatScreen()

    // Menu should not be visible initially
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertDoesNotExist()

    // Click attachment button
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Menu should now be visible
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertIsDisplayed()
  }

  // ============================================================================
  // Mic/Send Button Toggle Tests
  // ============================================================================

  @Test
  fun buttonToggle_switchesBetweenMicAndSend_basedOnTextInput() {
    setupChatScreen()

    // Initially mic button is shown when text is empty
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MIC_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertDoesNotExist()

    // Type some text - should switch to send button
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput("Hello")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MIC_BUTTON).assertDoesNotExist()

    // Clear the text - should switch back to mic button
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextClearance()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MIC_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertDoesNotExist()
  }

  // ============================================================================
  // Attachment Menu Tests
  // ============================================================================

  @Test
  fun attachmentMenu_displaysOptionsWithLabels() {
    setupChatScreen()

    // Open the attachment menu
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify all options are displayed with their labels
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_GALLERY).assertIsDisplayed()
    composeTestRule.onNodeWithText("Gallery").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_LOCATION).assertIsDisplayed()
    composeTestRule.onNodeWithText("Location").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_POLL).assertIsDisplayed()
    composeTestRule.onNodeWithText("Poll").assertIsDisplayed()
  }

  @Test
  fun attachmentMenu_optionsCloseMenu_whenClicked() {
    setupChatScreen()

    // Test Gallery option closes menu
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_GALLERY).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertDoesNotExist()

    // Test Location option closes menu
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_LOCATION).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertDoesNotExist()

    // Test Poll option closes menu
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_POLL).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertDoesNotExist()
  }

  @Test
  fun attachmentMenu_canBeReopened_afterClosing() {
    setupChatScreen()

    // Open the attachment menu
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertIsDisplayed()

    // Close by clicking an option
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_GALLERY).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertDoesNotExist()

    // Reopen the menu
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Menu should be visible again
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertIsDisplayed()
  }

  @Test
  fun sendMessage_clearsInputAndShowsMicButton() {
    setupChatScreen()

    // Type a message
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput("Test message")
    composeTestRule.waitForIdle()

    // Verify send button is shown
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertIsDisplayed()

    // Send the message
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Input should be cleared and mic button should reappear
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MIC_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertDoesNotExist()
  }

  // ============================================================================
  // Message Interactions Tests
  // ============================================================================

  @Test
  fun editedMessage_displaysEditedIndicator() {
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "Edited message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                readBy = emptyList(),
                isPinned = false,
                isEdited = true))
    fakeChatRepository.setMessages(messages)

    setupChatScreen()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Edited message").assertIsDisplayed()
    val editedText =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.message_edited)
    composeTestRule.onNodeWithText(editedText).assertIsDisplayed()
  }

  @Test
  fun nonEditedMessage_doesNotDisplayEditedIndicator() {
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "Regular message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                readBy = emptyList(),
                isPinned = false,
                isEdited = false))
    fakeChatRepository.setMessages(messages)

    setupChatScreen()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Regular message").assertIsDisplayed()
    val editedText =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.message_edited)
    composeTestRule.onNodeWithText(editedText).assertDoesNotExist()
  }

  @Test
  fun ownMessage_notReadByAll_displaysGreyCheckmarks() {
    // Chat with 3 participants, message only read by sender and one other user
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "My message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                readBy = listOf("user1", "user2"), // Not read by user3
                isPinned = false,
                isEdited = false))
    fakeChatRepository.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel,
          totalParticipants = 3)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("My message").assertIsDisplayed()
    // Checkmarks should appear with contentDescription from string resource (not read by all)
    val sentDescription =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.message_sent)
    composeTestRule.onNodeWithContentDescription(sentDescription).assertExists()
  }

  @Test
  fun ownMessage_readByAll_displaysCheckmarks() {
    // Chat with 3 participants, message read by all
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "My message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                readBy = listOf("user1", "user2", "user3"),
                isPinned = false,
                isEdited = false))
    fakeChatRepository.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = "chat1",
          chatTitle = "Test Chat",
          currentUserId = "user1",
          currentUserName = "Alice",
          viewModel = viewModel,
          totalParticipants = 3)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("My message").assertIsDisplayed()
    // Checkmarks should appear with contentDescription from string resource
    val readByAllDescription =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.read_by_all)
    composeTestRule.onNodeWithContentDescription(readByAllDescription).assertExists()
  }

  @Test
  fun otherUsersMessage_doesNotDisplayCheckmarks() {
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user2",
                senderName = "Bob",
                content = "Bob's message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                readBy = listOf("user1", "user2"),
                isPinned = false,
                isEdited = false))
    fakeChatRepository.setMessages(messages)

    setupChatScreen(currentUserId = "user1")

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Bob's message").assertIsDisplayed()
    // No checkmarks for other users' messages
    val sentDescription =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.message_sent)
    val readByAllDescription =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.read_by_all)
    composeTestRule.onNodeWithContentDescription(sentDescription).assertDoesNotExist()
    composeTestRule.onNodeWithContentDescription(readByAllDescription).assertDoesNotExist()
  }

  // ============================================================================
  // Message Interaction Tests (Long-press, Context Menu, Dialogs)
  // ============================================================================

  @Test
  fun contextMenu_longPressShowsMenuAndCopy() {
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "Test message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                readBy = emptyList(),
                isPinned = false,
                isEdited = false))
    fakeChatRepository.setMessages(messages)

    setupChatScreen()

    composeTestRule.waitForIdle()

    // Long press on message
    composeTestRule.onNodeWithText("Test message").performTouchInput { longClick() }

    composeTestRule.waitForIdle()

    // Context menu options should be visible
    val copyText = ApplicationProvider.getApplicationContext<Context>().getString(R.string.copy)
    val editText = ApplicationProvider.getApplicationContext<Context>().getString(R.string.edit)
    val deleteText = ApplicationProvider.getApplicationContext<Context>().getString(R.string.delete)

    composeTestRule.onNodeWithText(copyText).assertIsDisplayed()
    composeTestRule.onNodeWithText(editText).assertIsDisplayed()
    composeTestRule.onNodeWithText(deleteText).assertIsDisplayed()

    // Test copy functionality - click copy
    composeTestRule.onNodeWithText(copyText).performClick()

    composeTestRule.waitForIdle()

    // Context menu should be dismissed after copy
    composeTestRule.onNodeWithText(copyText).assertDoesNotExist()
  }

  @Test
  fun deleteDialog_opensAndCloses() {
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "Message to delete",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                readBy = emptyList(),
                isPinned = false,
                isEdited = false))
    fakeChatRepository.setMessages(messages)

    setupChatScreen()

    composeTestRule.waitForIdle()

    // Long press on message
    composeTestRule.onNodeWithText("Message to delete").performTouchInput { longClick() }

    composeTestRule.waitForIdle()

    // Click delete from context menu (first one in the list - from context menu)
    val deleteText = ApplicationProvider.getApplicationContext<Context>().getString(R.string.delete)
    composeTestRule.onAllNodesWithText(deleteText)[0].performClick()

    composeTestRule.waitForIdle()

    // Delete dialog should be visible
    val deleteDialogTitle =
        ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.delete_message_title)
    composeTestRule.onNodeWithText(deleteDialogTitle).assertIsDisplayed()

    // Test cancel button
    val cancelText = ApplicationProvider.getApplicationContext<Context>().getString(R.string.cancel)
    composeTestRule.onNodeWithText(cancelText).performClick()

    composeTestRule.waitForIdle()

    // Delete dialog should be dismissed
    composeTestRule.onNodeWithText(deleteDialogTitle).assertDoesNotExist()
  }

  @Test
  fun whoReadDialog_opensAndCloses() {
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = "chat1",
                senderId = "user1",
                senderName = "Alice",
                content = "Test message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                readBy = listOf("user1", "user2"),
                isPinned = false,
                isEdited = false))
    fakeChatRepository.setMessages(messages)

    setupChatScreen()

    composeTestRule.waitForIdle()

    // Long press on message
    composeTestRule.onNodeWithText("Test message").performTouchInput { longClick() }

    composeTestRule.waitForIdle()

    // Click "See who read"
    val seeWhoReadText =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.see_who_read)
    composeTestRule.onNodeWithText(seeWhoReadText).performClick()

    composeTestRule.waitForIdle()

    // Who read dialog should be visible
    val whoReadDialogTitle =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.read_by_title)
    composeTestRule.onNodeWithText(whoReadDialogTitle).assertIsDisplayed()

    // Click close
    val closeText = ApplicationProvider.getApplicationContext<Context>().getString(R.string.close)
    composeTestRule.onNodeWithText(closeText).performClick()

    composeTestRule.waitForIdle()

    // Who read dialog should be dismissed
    composeTestRule.onNodeWithText(whoReadDialogTitle).assertDoesNotExist()
  }

  // ============================================================================
  // Fake Repository for Testing
  // ============================================================================

  private class FakeProfileRepository : ProfileRepository {
    override suspend fun getProfile(uid: String): Profile? = null

    override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? = emptyList()

    override suspend fun createOrUpdateProfile(profile: Profile) {}

    override suspend fun deleteProfile(uid: String) {}

    override suspend fun uploadProfilePhoto(
        context: android.content.Context,
        uid: String,
        imageUri: android.net.Uri
    ): String = ""

    override suspend fun deleteProfilePhoto(uid: String) {}

    // Stub implementations for follow methods
    override suspend fun followUser(followerId: String, followedId: String) {}

    override suspend fun unfollowUser(followerId: String, followedId: String) {}

    override suspend fun isFollowing(followerId: String, followedId: String): Boolean = false

    override suspend fun getFollowing(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getFollowers(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getMutualFollowing(userId1: String, userId2: String): List<Profile> =
        emptyList()
  }

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
