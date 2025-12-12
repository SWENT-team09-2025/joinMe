package com.android.joinme.ui.chat

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.joinme.model.chat.ChatRepository
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.chat.Poll
import com.android.joinme.model.chat.PollOption
import com.android.joinme.model.chat.PollRepository
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * UI tests for ChatScreenWithPolls.
 *
 * Tests the integrated chat screen with poll functionality including:
 * - Timeline display with interleaved messages and polls
 * - Poll creation flow from attachment menu
 * - Message input and sending
 * - Poll voting integration
 */
@RunWith(RobolectricTestRunner::class)
class ChatScreenWithPollsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakePollRepository: FakePollRepository
  private lateinit var fakeChatRepository: FakeChatRepository
  private lateinit var fakeProfileRepository: FakeProfileRepository
  private lateinit var chatViewModel: ChatViewModel
  private lateinit var pollViewModel: PollViewModel

  private val testChatId = "chat1"
  private val testChatTitle = "Test Chat"
  private val testUserId = "user1"
  private val testUserName = "Alice"

  @Before
  fun setup() {
    fakePollRepository = FakePollRepository()
    fakeChatRepository = FakeChatRepository()
    fakeProfileRepository = FakeProfileRepository()
    chatViewModel = ChatViewModel(fakeChatRepository, fakeProfileRepository)
    pollViewModel = PollViewModel(fakePollRepository, fakeChatRepository, fakeProfileRepository)
  }

  // ============================================================================
  // Basic Display Tests
  // ============================================================================

  @Test
  fun chatScreenWithPolls_displaysAllUIElements() {
    setupChatScreenWithPolls()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.SCREEN).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText(testChatTitle).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.LEAVE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_LIST).assertExists()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun chatScreenWithPolls_displaysEmptyState_whenNoMessagesOrPolls() {
    setupChatScreenWithPolls()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }

  // ============================================================================
  // Message Display Tests
  // ============================================================================

  @Test
  fun chatScreenWithPolls_displaysMessages() {
    val messages =
        listOf(
            createMessage("msg1", "Hello everyone!"),
            createMessage("msg2", "How are you?", senderId = "user2", senderName = "Bob"))
    fakeChatRepository.setMessages(messages)

    setupChatScreenWithPolls()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Hello everyone!").assertIsDisplayed()
    composeTestRule.onNodeWithText("How are you?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
  }

  @Test
  fun chatScreenWithPolls_displaysPollsInTimeline() {
    val poll = createSamplePoll("poll1", "What's for lunch?")
    fakePollRepository.setPolls(testChatId, listOf(poll))

    // Add a POLL type message that references the poll
    val pollMessage = createPollMessage("msg1", poll.id, poll.createdAt)
    fakeChatRepository.setMessages(listOf(pollMessage))

    setupChatScreenWithPolls()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollCardTag("poll1")).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollQuestionTag("poll1"))
        .assertIsDisplayed()
  }

  @Test
  fun chatScreenWithPolls_interleavesMessagesAndPolls() {
    // Create messages and polls with specific timestamps
    val message1 = createMessage("msg1", "Before poll", timestamp = 1000L)
    val poll = createSamplePoll("poll1", "Vote please", createdAt = 2000L)
    val pollMessage = createPollMessage("msg2", poll.id, poll.createdAt)
    val message2 = createMessage("msg3", "After poll", timestamp = 3000L)

    fakeChatRepository.setMessages(listOf(message1, pollMessage, message2))
    fakePollRepository.setPolls(testChatId, listOf(poll))

    setupChatScreenWithPolls()

    composeTestRule.waitForIdle()

    // All items should be displayed
    composeTestRule.onNodeWithText("Before poll").assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollCardTag("poll1")).assertIsDisplayed()
    composeTestRule.onNodeWithText("After poll").assertIsDisplayed()
  }

  @Test
  fun chatScreenWithPolls_filtersPollMessagesWithDeletedPolls() {
    // Create a POLL message without a corresponding poll (simulating deleted poll)
    val orphanPollMessage = createPollMessage("msg1", "deleted_poll_id", 1000L)
    val regularMessage = createMessage("msg2", "Regular message", timestamp = 2000L)

    fakeChatRepository.setMessages(listOf(orphanPollMessage, regularMessage))
    // No polls in repository

    setupChatScreenWithPolls()

    composeTestRule.waitForIdle()

    // Regular message should be displayed, poll message should be filtered out
    composeTestRule.onNodeWithText("Regular message").assertIsDisplayed()
  }

  // ============================================================================
  // Message Input Tests
  // ============================================================================

  @Test
  fun chatScreenWithPolls_messageInput_sendsMessage() {
    setupChatScreenWithPolls()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput("Test message")
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).performClick()

    composeTestRule.waitForIdle()

    assertTrue(fakeChatRepository.sentMessages.any { it.content == "Test message" })
  }

  @Test
  fun chatScreenWithPolls_messageInput_clearsAfterSend() {
    setupChatScreenWithPolls()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput("Test message")
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // After sending, the mic button should be shown (indicating empty input)
    composeTestRule.onNodeWithTag("micButton").assertIsDisplayed()
  }

  @Test
  fun chatScreenWithPolls_sendButton_enabledWhenTextEntered() {
    setupChatScreenWithPolls()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.MESSAGE_INPUT).performTextInput("Hello")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertIsEnabled()
  }

  // ============================================================================
  // Attachment Menu Tests
  // ============================================================================

  @Test
  fun chatScreenWithPolls_attachmentMenu_opensOnClick() {
    setupChatScreenWithPolls()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_MENU).assertIsDisplayed()
  }

  @Test
  fun chatScreenWithPolls_attachmentMenu_showsPollOption() {
    setupChatScreenWithPolls()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_POLL).assertIsDisplayed()
  }

  @Test
  fun chatScreenWithPolls_attachmentMenu_showsGalleryOption() {
    setupChatScreenWithPolls()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.PHOTO_SOURCE_GALLERY).assertIsDisplayed()
  }

  @Test
  fun chatScreenWithPolls_attachmentMenu_showsLocationOption() {
    setupChatScreenWithPolls()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_LOCATION).assertIsDisplayed()
  }

  // ============================================================================
  // Poll Creation Flow Tests
  // ============================================================================

  @Test
  fun chatScreenWithPolls_pollOption_opensPollCreationSheet() {
    setupChatScreenWithPolls()

    // Open attachment menu
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Click poll option
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_POLL).performClick()
    composeTestRule.waitForIdle()

    // Poll creation sheet should be displayed
    composeTestRule.onNodeWithTag(PollCreationTestTags.BOTTOM_SHEET).assertIsDisplayed()
  }

  @Test
  fun chatScreenWithPolls_pollCreationSheet_closesOnDismiss() {
    setupChatScreenWithPolls()

    // Open poll creation sheet
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.ATTACHMENT_POLL).performClick()
    composeTestRule.waitForIdle()

    // Close the sheet
    composeTestRule.onNodeWithTag(PollCreationTestTags.CLOSE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Sheet should be dismissed
    composeTestRule.onNodeWithTag(PollCreationTestTags.BOTTOM_SHEET).assertDoesNotExist()
  }

  // ============================================================================
  // Poll Voting Integration Tests
  // ============================================================================

  @Test
  fun chatScreenWithPolls_votingOnPoll_callsViewModel() {
    val poll = createSamplePoll("poll1", "Vote?")
    fakePollRepository.setPolls(testChatId, listOf(poll))

    val pollMessage = createPollMessage("msg1", poll.id, poll.createdAt)
    fakeChatRepository.setMessages(listOf(pollMessage))

    setupChatScreenWithPolls()

    composeTestRule.waitForIdle()

    // Click on first option to vote
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollOptionTag("poll1", 0)).performClick()

    composeTestRule.waitForIdle()

    // Verify vote was called (check repository)
    assertTrue(fakePollRepository.votesCast.isNotEmpty())
  }

  // ============================================================================
  // Poll Actions Integration Tests
  // ============================================================================

  @Test
  fun chatScreenWithPolls_closePoll_callsViewModel() {
    val poll = createSamplePoll("poll1", "Vote?", creatorId = testUserId)
    fakePollRepository.setPolls(testChatId, listOf(poll))

    val pollMessage = createPollMessage("msg1", poll.id, poll.createdAt)
    fakeChatRepository.setMessages(listOf(pollMessage))

    setupChatScreenWithPolls()

    composeTestRule.waitForIdle()

    // Open poll menu
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag("poll1")).performClick()
    composeTestRule.waitForIdle()

    // Click close poll
    composeTestRule.onNodeWithText("Close poll", substring = true).performClick()
    composeTestRule.waitForIdle()

    assertTrue(fakePollRepository.closedPolls.contains("poll1"))
  }

  // ============================================================================
  // ChatTimelineItem Tests
  // ============================================================================

  @Test
  fun chatTimelineItem_messageItem_hasCorrectTimestamp() {
    val message = createMessage("msg1", "Test", timestamp = 12345L)
    val item = ChatTimelineItem.MessageItem(message)

    assertEquals(12345L, item.timestamp)
  }

  @Test
  fun chatTimelineItem_pollItem_hasCorrectTimestamp() {
    val poll = createSamplePoll("poll1", "Test?", createdAt = 67890L)
    val item = ChatTimelineItem.PollItem(poll)

    assertEquals(67890L, item.timestamp)
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  private fun setupChatScreenWithPolls() {
    composeTestRule.setContent {
      ChatScreenWithPolls(
          chatId = testChatId,
          chatTitle = testChatTitle,
          currentUserId = testUserId,
          currentUserName = testUserName,
          chatViewModel = chatViewModel,
          pollViewModel = pollViewModel,
          onLeaveClick = {},
          totalParticipants = 2)
    }
  }

  private fun createMessage(
      id: String,
      content: String,
      senderId: String = testUserId,
      senderName: String = testUserName,
      timestamp: Long = System.currentTimeMillis()
  ): Message {
    return Message(
        id = id,
        conversationId = testChatId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        timestamp = timestamp,
        type = MessageType.TEXT)
  }

  private fun createPollMessage(id: String, pollId: String, timestamp: Long): Message {
    return Message(
        id = id,
        conversationId = testChatId,
        senderId = testUserId,
        senderName = testUserName,
        content = pollId,
        timestamp = timestamp,
        type = MessageType.POLL)
  }

  private fun createSamplePoll(
      id: String,
      question: String,
      creatorId: String = "creator1",
      createdAt: Long = System.currentTimeMillis()
  ): Poll {
    return Poll(
        id = id,
        conversationId = testChatId,
        creatorId = creatorId,
        creatorName = "Creator",
        question = question,
        options =
            listOf(PollOption(id = 0, text = "Option A"), PollOption(id = 1, text = "Option B")),
        isAnonymous = false,
        allowMultipleAnswers = false,
        isClosed = false,
        createdAt = createdAt)
  }

  // ============================================================================
  // Fake Repositories
  // ============================================================================

  private class FakePollRepository : PollRepository {
    private val pollsByConversation = mutableMapOf<String, MutableList<Poll>>()
    private val pollsFlows = mutableMapOf<String, MutableStateFlow<List<Poll>>>()
    private var counter = 0

    val votesCast = mutableListOf<Triple<String, Int, String>>() // pollId, optionId, userId
    val closedPolls = mutableListOf<String>()
    val reopenedPolls = mutableListOf<String>()
    val deletedPolls = mutableListOf<String>()

    fun setPolls(conversationId: String, polls: List<Poll>) {
      pollsByConversation[conversationId] = polls.toMutableList()
      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      flow.value = polls
    }

    override fun getNewPollId(): String = "poll_${counter++}"

    override fun observePollsForConversation(conversationId: String): Flow<List<Poll>> {
      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      return flow.map { it.filter { poll -> poll.conversationId == conversationId } }
    }

    override suspend fun getPoll(conversationId: String, pollId: String): Poll? {
      return pollsByConversation[conversationId]?.find { it.id == pollId }
    }

    override suspend fun createPoll(poll: Poll) {
      val polls = pollsByConversation.getOrPut(poll.conversationId) { mutableListOf() }
      polls.add(poll)
      val flow = pollsFlows.getOrPut(poll.conversationId) { MutableStateFlow(emptyList()) }
      flow.value = polls.toList()
    }

    override suspend fun vote(
        conversationId: String,
        pollId: String,
        optionId: Int,
        userId: String
    ) {
      votesCast.add(Triple(pollId, optionId, userId))
    }

    override suspend fun removeVote(
        conversationId: String,
        pollId: String,
        optionId: Int,
        userId: String
    ) {}

    override suspend fun closePoll(conversationId: String, pollId: String, userId: String) {
      closedPolls.add(pollId)
    }

    override suspend fun reopenPoll(conversationId: String, pollId: String, userId: String) {
      reopenedPolls.add(pollId)
    }

    override suspend fun deletePoll(conversationId: String, pollId: String, userId: String) {
      deletedPolls.add(pollId)
    }
  }

  private class FakeChatRepository : ChatRepository {
    private val messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    private var messageCounter = 0

    val sentMessages = mutableListOf<Message>()

    fun setMessages(messages: List<Message>) {
      messagesFlow.value = messages
    }

    override fun getNewMessageId(): String = "msg_${messageCounter++}"

    override fun observeMessagesForConversation(conversationId: String): Flow<List<Message>> =
        messagesFlow

    override suspend fun addMessage(message: Message) {
      sentMessages.add(message)
      messagesFlow.value = messagesFlow.value + message
    }

    override suspend fun editMessage(
        conversationId: String,
        messageId: String,
        newValue: Message
    ) {}

    override suspend fun deleteMessage(conversationId: String, messageId: String) {}

    override suspend fun markMessageAsRead(
        conversationId: String,
        messageId: String,
        userId: String
    ) {}

    override suspend fun uploadChatImage(
        context: Context,
        conversationId: String,
        messageId: String,
        imageUri: android.net.Uri
    ): String = ""
  }

  private class FakeProfileRepository : ProfileRepository {
    override suspend fun getProfile(uid: String): Profile? = null

    override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? = emptyList()

    override suspend fun createOrUpdateProfile(profile: Profile) {}

    override suspend fun deleteProfile(uid: String) {}

    override suspend fun uploadProfilePhoto(
        context: Context,
        uid: String,
        imageUri: android.net.Uri
    ): String = ""

    override suspend fun deleteProfilePhoto(uid: String) {}

    override suspend fun followUser(followerId: String, followedId: String) {}

    override suspend fun unfollowUser(followerId: String, followedId: String) {}

    override suspend fun isFollowing(followerId: String, followedId: String): Boolean = false

    override suspend fun getFollowing(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getFollowers(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getMutualFollowing(userId1: String, userId2: String): List<Profile> =
        emptyList()
  }
}
