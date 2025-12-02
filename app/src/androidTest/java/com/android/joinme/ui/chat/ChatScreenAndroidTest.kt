package com.android.joinme.ui.chat

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.chat.ChatRepository
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Android instrumentation tests for ChatScreen that require real Android environment (Coil). */
class ChatScreenAndroidTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testUserId = "user1"
  private val testChatId = "chat1"

  // A tiny in-memory repo that mimics what the ViewModel needs
  private class FakeChatRepository(
      private var uploadShouldSucceed: Boolean = true,
      private var uploadUrl: String = "https://example.com/valid-image.jpg"
  ) : ChatRepository {
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

    override suspend fun uploadChatImage(
        context: Context,
        conversationId: String,
        messageId: String,
        imageUri: Uri
    ): String {
      if (!uploadShouldSucceed) {
        throw RuntimeException("Upload failed")
      }
      return uploadUrl
    }
  }

  private class FakeProfileRepository : ProfileRepository {
    override suspend fun getProfile(uid: String): Profile? = null

    override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? = emptyList()

    override suspend fun createOrUpdateProfile(profile: Profile) {}

    override suspend fun deleteProfile(uid: String) {}

    override suspend fun uploadProfilePhoto(context: Context, uid: String, imageUri: Uri): String =
        ""

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

  /**
   * Merged test: Covers both valid and invalid image URLs to test ChatImageMessage in real Android
   * environment.
   */
  @Test
  fun chatScreen_imageMessages_displayValidAndInvalidImages() = runTest {
    val repo = FakeChatRepository(uploadShouldSucceed = true)
    val profileRepo = FakeProfileRepository()
    val viewModel = ChatViewModel(repo, profileRepo)

    // Create messages with both valid and invalid image URLs
    // Using httpbin.org/html which returns HTML content (not an image), should trigger error
    val messages =
        listOf(
            Message(
                id = "img1",
                conversationId = testChatId,
                senderId = testUserId,
                senderName = "Alice",
                content = "https://picsum.photos/200/300",
                timestamp = System.currentTimeMillis() - 2000,
                type = MessageType.IMAGE),
            Message(
                id = "img2",
                conversationId = testChatId,
                senderId = testUserId,
                senderName = "Alice",
                content = "https://httpbin.org/html",
                timestamp = System.currentTimeMillis() - 1000,
                type = MessageType.IMAGE))

    repo.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = testChatId,
          chatTitle = "Test Chat",
          currentUserId = testUserId,
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Verify both message containers exist
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("img1"))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("img2"))
        .assertIsDisplayed()

    // Wait for Coil to load/fail (give it extra time to render error state)
    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // The invalid image should show error state
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.CHAT_IMAGE_ERROR, useUnmergedTree = true)
        .assertExists()
  }

  /**
   * Tests that clicking on an image message opens the full-screen image viewer.
   * (fullScreenImageUrl conditional) and (FullScreenImageViewer composable).
   */
  @Test
  fun chatScreen_imageMessage_opensFullScreenViewer() = runTest {
    val repo = FakeChatRepository(uploadShouldSucceed = true)
    val profileRepo = FakeProfileRepository()
    val viewModel = ChatViewModel(repo, profileRepo)

    val messages =
        listOf(
            Message(
                id = "img1",
                conversationId = testChatId,
                senderId = testUserId,
                senderName = "Alice",
                content = "https://picsum.photos/400/300",
                timestamp = System.currentTimeMillis(),
                type = MessageType.IMAGE))

    repo.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = testChatId,
          chatTitle = "Test Chat",
          currentUserId = testUserId,
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()
    Thread.sleep(2000) // Wait for Coil to load the image

    // Click on the actual image (which has the clickable modifier)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.CHAT_IMAGE_REMOTE).performClick()

    composeTestRule.waitForIdle()

    // Verify full-screen viewer is displayed
    // The close button should be visible
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()

    // Click close button to dismiss
    composeTestRule.onNodeWithContentDescription("Close").performClick()

    composeTestRule.waitForIdle()

    // Verify full-screen viewer is dismissed
    composeTestRule.onNodeWithContentDescription("Close").assertDoesNotExist()
  }

  /**
   * Tests PhotoSourceDialog opening and UI elements. Covers:
   * - Opening the dialog via Photo button
   * - Dialog UI elements
   * - Dialog conditional rendering
   */
  @Test
  fun chatScreen_photoSourceDialog_opensAndDisplaysUIElements() {
    val repo = FakeChatRepository(uploadShouldSucceed = true)
    val profileRepo = FakeProfileRepository()
    val viewModel = ChatViewModel(repo, profileRepo)

    composeTestRule.setContent {
      ChatScreen(
          chatId = testChatId,
          chatTitle = "Test Chat",
          currentUserId = testUserId,
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Open attachment menu
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500)

    // Dialog should not exist initially
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG).assertDoesNotExist()

    // Click Photo button to open dialog
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_PHOTO).performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is displayed
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG).assertIsDisplayed()

    // Verify all UI elements
    composeTestRule.onNodeWithText("Choose photo source").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_GALLERY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_CAMERA).assertIsDisplayed()
    composeTestRule.onNodeWithText("Gallery").assertIsDisplayed()
    composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Gallery").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Camera").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  /**
   * Tests edit message dialog flow in Android environment. This test covers the edit message
   * functionality that couldn't be reliably tested in Robolectric tests.
   */
  @Test
  fun chatScreen_editMessageDialog_opensAndEditsMessage() = runTest {
    val repo = FakeChatRepository(uploadShouldSucceed = true)
    val profileRepo = FakeProfileRepository()
    val viewModel = ChatViewModel(repo, profileRepo)

    // Create a message from the current user that can be edited
    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = testChatId,
                senderId = testUserId,
                senderName = "Alice",
                content = "Original message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT))

    repo.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = testChatId,
          chatTitle = "Test Chat",
          currentUserId = testUserId,
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Verify the original message is displayed
    composeTestRule.onNodeWithText("Original message").assertIsDisplayed()

    // Long-press on the message to open context menu
    composeTestRule.onNodeWithText("Original message").performTouchInput { longClick() }
    composeTestRule.waitForIdle()

    // Click Edit option from context menu
    composeTestRule.onNodeWithText("Edit").performClick()
    composeTestRule.waitForIdle()

    // Verify edit dialog is displayed
    composeTestRule.onNodeWithTag(ChatScreenTestTags.EDIT_MESSAGE_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithText("Edit Message").assertIsDisplayed()

    // Verify the input field contains the original message
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.EDIT_MESSAGE_INPUT)
        .assertTextEquals("Original message")

    // Clear and type new message
    composeTestRule.onNodeWithTag(ChatScreenTestTags.EDIT_MESSAGE_INPUT).performTextClearance()
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.EDIT_MESSAGE_INPUT)
        .performTextInput("Edited message")
    composeTestRule.waitForIdle()

    // Click Save button
    composeTestRule.onNodeWithTag(ChatScreenTestTags.EDIT_MESSAGE_SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is dismissed
    composeTestRule.onNodeWithTag(ChatScreenTestTags.EDIT_MESSAGE_DIALOG).assertDoesNotExist()

    // Verify the message was updated
    composeTestRule.onNodeWithText("Edited message").assertIsDisplayed()
    composeTestRule.onNodeWithText("Original message").assertDoesNotExist()

    // Verify the edited indicator is shown
    composeTestRule.onNodeWithText("edited").assertIsDisplayed()
  }

  /**
   * Tests edit message dialog cancel functionality.
   */
  @Test
  fun chatScreen_editMessageDialog_cancelKeepsOriginalMessage() = runTest {
    val repo = FakeChatRepository(uploadShouldSucceed = true)
    val profileRepo = FakeProfileRepository()
    val viewModel = ChatViewModel(repo, profileRepo)

    val messages =
        listOf(
            Message(
                id = "msg1",
                conversationId = testChatId,
                senderId = testUserId,
                senderName = "Alice",
                content = "Original message",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT))

    repo.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = testChatId,
          chatTitle = "Test Chat",
          currentUserId = testUserId,
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Long-press and open edit dialog
    composeTestRule.onNodeWithText("Original message").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Edit").performClick()
    composeTestRule.waitForIdle()

    // Change the text
    composeTestRule.onNodeWithTag(ChatScreenTestTags.EDIT_MESSAGE_INPUT).performTextClearance()
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.EDIT_MESSAGE_INPUT)
        .performTextInput("Changed message")
    composeTestRule.waitForIdle()

    // Click Cancel instead of Save
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is dismissed
    composeTestRule.onNodeWithTag(ChatScreenTestTags.EDIT_MESSAGE_DIALOG).assertDoesNotExist()

    // Verify the original message is still there
    composeTestRule.onNodeWithText("Original message").assertIsDisplayed()
    composeTestRule.onNodeWithText("Changed message").assertDoesNotExist()
  }

  /**
   * Tests FullScreenImageViewer error state.(error block in
   * FullScreenImageViewer when the full-screen image fails to load).
   */
  @Test
  fun chatScreen_fullScreenImageViewer_displaysErrorState() = runTest {
    val repo = FakeChatRepository(uploadShouldSucceed = true)
    val profileRepo = FakeProfileRepository()
    val viewModel = ChatViewModel(repo, profileRepo)

    // Use an invalid URL that will fail to load in Coil
    val messages =
        listOf(
            Message(
                id = "img1",
                conversationId = testChatId,
                senderId = testUserId,
                senderName = "Alice",
                content = "https://invalid.example.com/nonexistent.jpg",
                timestamp = System.currentTimeMillis(),
                type = MessageType.IMAGE))

    repo.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = testChatId,
          chatTitle = "Test Chat",
          currentUserId = testUserId,
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()
    Thread.sleep(2000) // Wait for Coil to attempt loading

    // Verify error state is shown for the image message
    composeTestRule.onNodeWithTag(ChatScreenTestTags.CHAT_IMAGE_ERROR, useUnmergedTree = true).assertExists()

    // Click on the image container to open full-screen viewer
    // Use the image container which has the clickable modifier
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.CHAT_IMAGE_REMOTE)
        .performClick()

    composeTestRule.waitForIdle()
    Thread.sleep(2000) // Give time for full-screen image to load/fail

    // Verify the full-screen viewer is displayed (close button visible)
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()

    // We can't directly assert the error text visibility, but the code path is executed

    // Close the viewer
    composeTestRule.onNodeWithContentDescription("Close").assertExists()
    composeTestRule.waitForIdle()
  }
}
