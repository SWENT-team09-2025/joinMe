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
   * environment. Covers lines 789-803 (error state).
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
   * Tests that clicking on an image message opens the full-screen image viewer. Covers lines
   * 402-405 (fullScreenImageUrl conditional) and 1132-1177 (FullScreenImageViewer composable).
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
   * Tests PhotoSourceDialog Cancel button. Covers:
   * - Cancel button dismissal
   */
  @Test
  fun chatScreen_photoSourceDialog_cancelDismissesDialog() {
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

    // Open attachment menu and photo source dialog
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_PHOTO).performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is displayed
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG).assertIsDisplayed()

    // Click Cancel button
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    // Dialog should be dismissed
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG).assertDoesNotExist()

    // Attachment menu should still be visible
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertIsDisplayed()
  }

  /**
   * Tests PhotoSourceDialog Gallery button click. Covers:
   * - Gallery button click
   */
  @Test
  fun chatScreen_photoSourceDialog_galleryButtonDismissesDialog() {
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

    // Open attachment menu and photo source dialog
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_PHOTO).performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is displayed
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG).assertIsDisplayed()

    // Click Gallery button
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_GALLERY).performClick()
    composeTestRule.waitForIdle()

    // Dialog should be dismissed (imagePickerLauncher would open but can't test that)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG).assertDoesNotExist()
  }

  /**
   * Tests PhotoSourceDialog Camera button click. Covers:
   * - Camera button click (lines 1108-1111, onCameraClick, cameraPermissionLauncher lines
   *   1041-1055, cameraLauncher lines 1014-1038) Note: The actual launcher can't be tested in this
   *   environment, but we verify the dialog dismisses.
   */
  @Test
  fun chatScreen_photoSourceDialog_cameraButtonDismissesDialog() {
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

    // Open attachment menu and photo source dialog
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_PHOTO).performClick()
    composeTestRule.waitForIdle()

    // Verify dialog is displayed
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG).assertIsDisplayed()

    // Click Camera button
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_CAMERA).performClick()
    composeTestRule.waitForIdle()

    // Dialog should be dismissed (cameraPermissionLauncher would trigger but can't test that)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_DIALOG).assertDoesNotExist()
  }
}
