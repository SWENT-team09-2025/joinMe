package com.android.joinme.ui.chat

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.joinme.model.chat.ChatRepository
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.Poll
import com.android.joinme.model.chat.PollOption
import com.android.joinme.model.chat.PollRepository
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * UI tests for Poll creation and display components.
 *
 * Tests the poll creation bottom sheet, poll display card, and voting interactions.
 */
@RunWith(RobolectricTestRunner::class)
class PollUITest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakePollRepository: FakePollRepository
  private lateinit var fakeChatRepository: FakeChatRepository
  private lateinit var fakeProfileRepository: FakeProfileRepository
  private lateinit var pollViewModel: PollViewModel

  private val testConversationId = "conv1"
  private val testUserId = "user1"
  private val testUserName = "Alice"

  @Before
  fun setup() {
    fakePollRepository = FakePollRepository()
    fakeChatRepository = FakeChatRepository()
    fakeProfileRepository = FakeProfileRepository()
    pollViewModel = PollViewModel(fakePollRepository, fakeChatRepository, fakeProfileRepository)
    pollViewModel.initialize(testConversationId, testUserId)
  }

  // ============================================================================
  // Poll Creation Sheet Tests
  // ============================================================================

  @Test
  fun pollCreationSheet_displaysAllElements() {
    setupPollCreationSheet()

    // Verify main elements are displayed (top elements should be visible)
    composeTestRule.onNodeWithTag(PollCreationTestTags.BOTTOM_SHEET).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.CLOSE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.QUESTION_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(0)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(1)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.ADD_OPTION_BUTTON).assertIsDisplayed()
    // Note: ANONYMOUS_SWITCH, MULTIPLE_ANSWERS_SWITCH, and CREATE_BUTTON may be scrolled off-screen
    // in small viewport sizes. Their functionality is tested in other tests.
  }

  @Test
  fun pollCreationSheet_createButton_disabledInitially() {
    setupPollCreationSheet()

    // Create button should be disabled when form is empty
    composeTestRule.onNodeWithTag(PollCreationTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun pollCreationSheet_questionInput_updatesState() {
    setupPollCreationSheet()

    composeTestRule
        .onNodeWithTag(PollCreationTestTags.QUESTION_FIELD)
        .performTextInput("What is your favorite color?")

    // Verify state is updated
    assert(pollViewModel.creationState.value.question == "What is your favorite color?")
  }

  @Test
  fun pollCreationSheet_optionInput_updatesState() {
    setupPollCreationSheet()

    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(0)).performTextInput("Red")

    composeTestRule
        .onNodeWithTag(PollCreationTestTags.getOptionFieldTag(1))
        .performTextInput("Blue")

    // Verify state is updated
    assert(pollViewModel.creationState.value.options[0] == "Red")
    assert(pollViewModel.creationState.value.options[1] == "Blue")
  }

  @Test
  fun pollCreationSheet_addOption_addsNewField() {
    setupPollCreationSheet()

    // Initially 2 options
    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(0)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(1)).assertIsDisplayed()

    // Click add option
    composeTestRule.onNodeWithTag(PollCreationTestTags.ADD_OPTION_BUTTON).performClick()

    // Now should have 3 options
    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(2)).assertIsDisplayed()
  }

  @Test
  fun pollCreationSheet_remainingOptionsCounter_updates() {
    setupPollCreationSheet()

    // Initial remaining count
    composeTestRule
        .onNodeWithTag(PollCreationTestTags.REMAINING_OPTIONS_TEXT)
        .assertTextContains("${Poll.MAX_OPTIONS - 2}", substring = true)

    // Add option
    composeTestRule.onNodeWithTag(PollCreationTestTags.ADD_OPTION_BUTTON).performClick()

    // Updated remaining count
    composeTestRule
        .onNodeWithTag(PollCreationTestTags.REMAINING_OPTIONS_TEXT)
        .assertTextContains("${Poll.MAX_OPTIONS - 3}", substring = true)
  }

  @Test
  fun pollCreationSheet_validForm_enablesCreateButton() {
    setupPollCreationSheet()

    // Fill in valid form
    composeTestRule
        .onNodeWithTag(PollCreationTestTags.QUESTION_FIELD)
        .performTextInput("Test question?")

    composeTestRule
        .onNodeWithTag(PollCreationTestTags.getOptionFieldTag(0))
        .performTextInput("Option A")

    composeTestRule
        .onNodeWithTag(PollCreationTestTags.getOptionFieldTag(1))
        .performTextInput("Option B")

    // Create button should now be enabled
    composeTestRule.onNodeWithTag(PollCreationTestTags.CREATE_BUTTON).assertIsEnabled()
  }

  @Test
  fun pollCreationSheet_anonymousToggle_togglesState() {
    setupPollCreationSheet()

    assert(!pollViewModel.creationState.value.isAnonymous)

    composeTestRule.onNodeWithTag(PollCreationTestTags.ANONYMOUS_SWITCH).performClick()

    assert(pollViewModel.creationState.value.isAnonymous)
  }

  @Test
  fun pollCreationSheet_multipleAnswersToggle_togglesState() {
    setupPollCreationSheet()

    assert(!pollViewModel.creationState.value.allowMultipleAnswers)

    composeTestRule.onNodeWithTag(PollCreationTestTags.MULTIPLE_ANSWERS_SWITCH).performClick()

    assert(pollViewModel.creationState.value.allowMultipleAnswers)
  }

  // ============================================================================
  // Poll Display Card Tests
  // ============================================================================

  @Test
  fun pollCard_displaysQuestion() {
    val poll = createSamplePoll("poll1", "What is your favorite color?")
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollQuestionTag("poll1"))
        .assertTextContains("What is your favorite color?")
  }

  @Test
  fun pollCard_displaysOptions() {
    val poll = createSamplePoll("poll1")
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollOptionTag("poll1", 0))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollOptionTag("poll1", 1))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_displaysTotalVotes() {
    val poll =
        createSamplePoll("poll1")
            .copy(
                options =
                    listOf(
                        PollOption(id = 0, text = "A", voterIds = listOf("user1", "user2")),
                        PollOption(id = 1, text = "B", voterIds = listOf("user3"))))
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollTotalVotesTag("poll1"))
        .assertTextContains("3", substring = true)
  }

  @Test
  fun pollCard_closedPoll_showsBadge() {
    val poll = createSamplePoll("poll1").copy(isClosed = true)
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollClosedBadgeTag("poll1"))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_anonymousPoll_showsBadge() {
    val poll = createSamplePoll("poll1").copy(isAnonymous = true)
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollAnonymousBadgeTag("poll1"))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_creatorMenu_visible() {
    val poll = createSamplePoll("poll1").copy(creatorId = testUserId)
    setupPollCard(poll)

    // Menu button should be visible for creator
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag("poll1"))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_vote_updatesSelection() {
    var votedOptionId: Int? = null
    val poll = createSamplePoll("poll1")

    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          voterProfiles = emptyMap(),
          onVote = { optionId -> votedOptionId = optionId },
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = {})
    }

    // Click on first option
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollOptionTag("poll1", 0)).performClick()

    assert(votedOptionId == 0)
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  private fun setupPollCreationSheet() {
    composeTestRule.setContent {
      PollCreationSheet(
          viewModel = pollViewModel, creatorName = testUserName, onDismiss = {}, onPollCreated = {})
    }
  }

  private fun setupPollCard(poll: Poll) {
    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          voterProfiles = emptyMap(),
          onVote = {},
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = {})
    }
  }

  private fun createSamplePoll(id: String, question: String = "Test question?"): Poll {
    return Poll(
        id = id,
        conversationId = testConversationId,
        creatorId = "creator1",
        creatorName = "Creator",
        question = question,
        options =
            listOf(
                PollOption(id = 0, text = "Option A", voterIds = emptyList()),
                PollOption(id = 1, text = "Option B", voterIds = emptyList())),
        isAnonymous = false,
        allowMultipleAnswers = false,
        isClosed = false,
        createdAt = 1000L)
  }

  // ============================================================================
  // Fake Repository Implementations
  // ============================================================================

  private class FakePollRepository : PollRepository {
    private val pollsByConversation = mutableMapOf<String, MutableList<Poll>>()
    private val pollsFlows = mutableMapOf<String, MutableStateFlow<List<Poll>>>()
    private var counter = 0

    override fun getNewPollId(): String = "poll_${counter++}"

    override fun observePollsForConversation(conversationId: String): Flow<List<Poll>> {
      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      return flow.map { polls -> polls.filter { it.conversationId == conversationId } }
    }

    override suspend fun getPoll(conversationId: String, pollId: String): Poll? {
      return pollsByConversation[conversationId]?.find { it.id == pollId }
    }

    override suspend fun createPoll(poll: Poll) {
      val chatPolls = pollsByConversation.getOrPut(poll.conversationId) { mutableListOf() }
      chatPolls.add(poll)
      val flow = pollsFlows.getOrPut(poll.conversationId) { MutableStateFlow(emptyList()) }
      flow.value = chatPolls.sortedBy { it.createdAt }
    }

    override suspend fun vote(
        conversationId: String,
        pollId: String,
        optionId: Int,
        userId: String
    ) {
      // Simplified implementation for tests
    }

    override suspend fun removeVote(
        conversationId: String,
        pollId: String,
        optionId: Int,
        userId: String
    ) {}

    override suspend fun closePoll(conversationId: String, pollId: String, userId: String) {}

    override suspend fun reopenPoll(conversationId: String, pollId: String, userId: String) {}

    override suspend fun deletePoll(conversationId: String, pollId: String, userId: String) {}
  }

  private class FakeChatRepository : ChatRepository {
    private var messageCounter = 0

    override fun getNewMessageId(): String = "msg_${messageCounter++}"

    override fun observeMessagesForConversation(conversationId: String): Flow<List<Message>> =
        MutableStateFlow(emptyList())

    override suspend fun addMessage(message: Message) {
      // No-op for tests
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
        context: android.content.Context,
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
        context: android.content.Context,
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
