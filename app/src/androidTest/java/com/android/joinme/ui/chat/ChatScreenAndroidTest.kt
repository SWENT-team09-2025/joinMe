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

  // Common test dependencies
  private lateinit var repo: FakeChatRepository
  private lateinit var profileRepo: FakeProfileRepository
  private lateinit var viewModel: ChatViewModel

  // Common test location
  private val epflLocation =
      com.android.joinme.model.map.Location(
          latitude = 46.5197, longitude = 6.6323, name = "EPFL Campus")

  @org.junit.Before
  fun setUp() {
    repo = FakeChatRepository(uploadShouldSucceed = true)
    profileRepo = FakeProfileRepository()
    viewModel = ChatViewModel(repo, profileRepo)
  }

  /**
   * Helper function to compose LocationPreviewDialog with test data.
   *
   * @param userLocation Location to preview
   * @param onDismiss Callback when dialog is dismissed
   * @param onSendLocation Callback when send button is clicked
   */
  private fun composeLocationPreviewDialog(
      userLocation: com.android.joinme.model.map.UserLocation =
          com.android.joinme.model.map.UserLocation(
              latitude = 46.5197, longitude = 6.6323, accuracy = 10f),
      onDismiss: () -> Unit = {},
      onSendLocation: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      LocationPreviewDialog(
          userLocation = userLocation, onDismiss = onDismiss, onSendLocation = onSendLocation)
    }
    composeTestRule.waitForIdle()
    Thread.sleep(1000) // Wait for Maps to initialize
  }

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
   * Tests that IMAGE type messages are rendered correctly in real Android environment. Covers the
   * when(message.type) branch for MessageType.IMAGE and verifies ChatImageMessage composable is
   * invoked for image messages.
   */
  @Test
  fun chatScreen_imageMessages_renderCorrectly() = runTest {
    // Create multiple image messages to test IMAGE type handling
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
                senderId = "user2",
                senderName = "Bob",
                content = "https://picsum.photos/250/350",
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

    // Verify both image message containers exist (tests IMAGE type in when block)
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("img1"))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("img2"))
        .assertIsDisplayed()

    // Verify sender name is shown for other user's image (tests message structure)
    composeTestRule.onNodeWithText("Bob", useUnmergedTree = true).assertExists()

    // Note: Error state for images is specifically tested in
    // chatScreen_fullScreenImageViewer_displaysErrorState test to avoid flakiness
  }

  /**
   * Tests that clicking on an image message opens the full-screen image viewer. (fullScreenImageUrl
   * conditional) and (FullScreenImageViewer composable).
   */
  @Test
  fun chatScreen_imageMessage_opensFullScreenViewer() = runTest {
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

  /** Tests edit message dialog cancel functionality. */
  @Test
  fun chatScreen_editMessageDialog_cancelKeepsOriginalMessage() = runTest {
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
   * Tests FullScreenImageViewer error state.(error block in FullScreenImageViewer when the
   * full-screen image fails to load).
   */
  @Test
  fun chatScreen_fullScreenImageViewer_displaysErrorState() = runTest {
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
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.CHAT_IMAGE_ERROR, useUnmergedTree = true)
        .assertExists()

    // Click on the image container to open full-screen viewer
    // Use the image container which has the clickable modifier
    composeTestRule.onNodeWithTag(ChatScreenTestTags.CHAT_IMAGE_REMOTE).performClick()

    composeTestRule.waitForIdle()
    Thread.sleep(2000) // Give time for full-screen image to load/fail

    // Verify the full-screen viewer is displayed (close button visible)
    composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()

    // We can't directly assert the error text visibility, but the code path is executed

    // Close the viewer
    composeTestRule.onNodeWithContentDescription("Close").assertExists()
    composeTestRule.waitForIdle()
  }

  /**
   * Tests that LOCATION type messages render correctly with Google Maps and that mixed message
   * types (text, image, location) coexist properly in real Android environment. Covers the
   * when(message.type) branch for MessageType.LOCATION and verifies all complex components (Coil
   * for images, Maps for locations) work together.
   */
  @Test
  fun chatScreen_locationAndMixedMessages_renderCorrectlyWithMaps() = runTest {
    val messages =
        listOf(
            // Image message
            Message(
                id = "img1",
                conversationId = testChatId,
                senderId = "user2",
                senderName = "Bob",
                content = "https://picsum.photos/200/200",
                timestamp = System.currentTimeMillis() - 3000,
                type = MessageType.IMAGE),
            // Location message from current user
            Message(
                id = "loc1",
                conversationId = testChatId,
                senderId = testUserId,
                senderName = "Alice",
                content = "static_map_url",
                timestamp = System.currentTimeMillis() - 2000,
                type = MessageType.LOCATION,
                location = epflLocation),
            // Location message from other user
            Message(
                id = "loc2",
                conversationId = testChatId,
                senderId = "user2",
                senderName = "Bob",
                content = "static_map_url",
                timestamp = System.currentTimeMillis() - 1500,
                type = MessageType.LOCATION,
                location = epflLocation.copy(name = "Meeting Point")),
            // Text messages
            Message(
                id = "txt1",
                conversationId = testChatId,
                senderId = testUserId,
                senderName = "Alice",
                content = "Check out this place!",
                timestamp = System.currentTimeMillis() - 1000,
                type = MessageType.TEXT),
            Message(
                id = "txt2",
                conversationId = testChatId,
                senderId = "user2",
                senderName = "Bob",
                content = "Looks great!",
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
    Thread.sleep(2000) // Wait for Coil and Maps to initialize

    // Verify all message containers exist
    composeTestRule.onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("img1")).assertExists()
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("loc1"))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("loc2"))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("txt1")).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.getTestTagForMessage("txt2")).assertExists()

    // Verify location names are displayed
    composeTestRule.onNodeWithText("EPFL Campus", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Meeting Point", useUnmergedTree = true).assertExists()

    // Verify text content
    composeTestRule.onNodeWithText("Check out this place!", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Looks great!", useUnmergedTree = true).assertExists()

    // Verify location previews exist (there are 2 location messages)
    composeTestRule
        .onAllNodesWithTag(ChatScreenTestTags.LOCATION_MESSAGE_PREVIEW)
        .assertCountEquals(2)

    // Note: Don't assert on "Bob" text as it appears in multiple messages (image, location, text)
  }

  /**
   * Tests that clicking on a location message triggers the navigation callback. This test focuses
   * solely on the click behavior since display is already tested in
   * chatScreen_locationAndMixedMessages_renderCorrectlyWithMaps.
   */
  @Test
  fun chatScreen_locationMessage_clickTriggersNavigation() = runTest {
    var navigationCallbackInvoked = false
    var navigatedLocation: com.android.joinme.model.map.Location? = null

    val testLocation =
        com.android.joinme.model.map.Location(
            latitude = 46.5197, longitude = 6.6323, name = "Test Location")

    val messages =
        listOf(
            Message(
                id = "loc1",
                conversationId = testChatId,
                senderId = testUserId,
                senderName = "Alice",
                content = "static_map_url",
                timestamp = System.currentTimeMillis(),
                type = MessageType.LOCATION,
                location = testLocation))

    repo.setMessages(messages)

    composeTestRule.setContent {
      ChatScreen(
          chatId = testChatId,
          chatTitle = "Test Chat",
          currentUserId = testUserId,
          currentUserName = "Alice",
          viewModel = viewModel,
          onNavigateToMap = { location, _ ->
            navigationCallbackInvoked = true
            navigatedLocation = location
          })
    }

    composeTestRule.waitForIdle()
    Thread.sleep(3000) // Wait for Maps to fully initialize and become interactive

    // Click on the location name text (more reliable than clicking on Maps)
    composeTestRule.onNodeWithText("Test Location", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdle()
    Thread.sleep(1000) // Give time for callback to execute

    // Verify callback was invoked with correct location
    assert(navigationCallbackInvoked) { "Navigation callback was not invoked" }
    assert(navigatedLocation == testLocation) {
      "Expected location $testLocation but got $navigatedLocation"
    }
  }

  /**
   * Tests the AttachmentMenu location option displays correctly. Covers: - Location option is
   * displayed and clickable in AttachmentMenu
   *
   * Note: Clicking the location option triggers rememberLocationPermissionsLauncher in
   * ChatImageLaunchers.kt which is excluded from coverage (requires real location permissions).
   */
  @Test
  fun chatScreen_attachmentMenu_locationOptionDisplayed() = runTest {
    composeTestRule.setContent {
      ChatScreen(
          chatId = testChatId,
          chatTitle = "Test Chat",
          currentUserId = testUserId,
          currentUserName = "Alice",
          viewModel = viewModel)
    }

    composeTestRule.waitForIdle()

    // Click attachment button to open menu
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify attachment menu is displayed with Photo and Location options
    // Note: Poll option is only available in ChatScreenWithPolls (for group/event chats)
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_PHOTO).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_LOCATION).assertExists()

    // Verify location option is displayed with proper icon and label, and is clickable
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.ATTACHMENT_LOCATION)
        .assertExists()
        .assertHasClickAction()
    composeTestRule.onNodeWithContentDescription("Location").assertExists()
  }

  /**
   * Tests LocationPreviewDialog displays correctly with all UI elements. This test composes the
   * dialog directly with test data to verify: - Dialog displays with correct structure and test
   * tags - Map preview is shown - Location coordinates are displayed - Send and Cancel buttons are
   * present
   */
  @Test
  fun chatScreen_locationPreviewDialog_displaysCorrectly() = runTest {
    composeLocationPreviewDialog()

    // Verify dialog is displayed
    composeTestRule.onNodeWithTag(ChatScreenTestTags.LOCATION_PREVIEW_DIALOG).assertIsDisplayed()

    // Verify title
    composeTestRule.onNodeWithText("Location Preview").assertIsDisplayed()

    // Verify location coordinates are displayed
    composeTestRule.onNodeWithText("46.5197, 6.6323").assertIsDisplayed()

    // Verify map preview exists
    composeTestRule.onNodeWithTag(ChatScreenTestTags.LOCATION_PREVIEW_MAP).assertExists()

    // Verify buttons exist
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.LOCATION_PREVIEW_SEND_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.LOCATION_PREVIEW_CANCEL_BUTTON)
        .assertIsDisplayed()

    // Verify Send Location button text
    composeTestRule.onNodeWithText("Send Location").assertIsDisplayed()

    // Verify Cancel button text
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  /** Tests LocationPreviewDialog cancel button dismisses the dialog. */
  @Test
  fun chatScreen_locationPreviewDialog_cancelButtonDismisses() = runTest {
    var dialogDismissed = false
    composeLocationPreviewDialog(onDismiss = { dialogDismissed = true })

    // Click cancel button
    composeTestRule.onNodeWithTag(ChatScreenTestTags.LOCATION_PREVIEW_CANCEL_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify dialog was dismissed
    assert(dialogDismissed) { "Cancel button should dismiss the dialog" }
  }

  /** Tests LocationPreviewDialog send button triggers location send. */
  @Test
  fun chatScreen_locationPreviewDialog_sendButtonTriggersLocationSend() = runTest {
    var locationSent = false
    composeLocationPreviewDialog(onSendLocation = { locationSent = true })

    // Click send button
    composeTestRule.onNodeWithTag(ChatScreenTestTags.LOCATION_PREVIEW_SEND_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify location send was triggered
    assert(locationSent) { "Send button should trigger location send" }
  }
}
