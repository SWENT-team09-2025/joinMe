package com.android.joinme.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
  // Poll Creation Sheet Tests (Combined)
  // ============================================================================

  @Test
  fun pollCreationSheet_initialState_displaysElementsAndCreateButtonDisabled() {
    setupPollCreationSheet()

    // Verify main elements are displayed
    composeTestRule.onNodeWithTag(PollCreationTestTags.BOTTOM_SHEET).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.CLOSE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.QUESTION_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(0)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(1)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PollCreationTestTags.ADD_OPTION_BUTTON).assertIsDisplayed()

    // Create button should be disabled when form is empty
    composeTestRule.onNodeWithTag(PollCreationTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun pollCreationSheet_inputFields_updateState() {
    setupPollCreationSheet()

    // Test question input
    composeTestRule
        .onNodeWithTag(PollCreationTestTags.QUESTION_FIELD)
        .performTextInput("What is your favorite color?")
    assertEquals("What is your favorite color?", pollViewModel.creationState.value.question)

    // Test option inputs
    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(0)).performTextInput("Red")
    composeTestRule
        .onNodeWithTag(PollCreationTestTags.getOptionFieldTag(1))
        .performTextInput("Blue")

    assertEquals("Red", pollViewModel.creationState.value.options[0])
    assertEquals("Blue", pollViewModel.creationState.value.options[1])
  }

  @Test
  fun pollCreationSheet_addOption_addsFieldAndUpdatesCounter() {
    setupPollCreationSheet()

    // Initial remaining count
    composeTestRule
        .onNodeWithTag(PollCreationTestTags.REMAINING_OPTIONS_TEXT)
        .assertTextContains("${Poll.MAX_OPTIONS - 2}", substring = true)

    // Click add option
    composeTestRule.onNodeWithTag(PollCreationTestTags.ADD_OPTION_BUTTON).performClick()

    // Should have 3 options now
    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(2)).assertIsDisplayed()

    // Updated remaining count
    composeTestRule
        .onNodeWithTag(PollCreationTestTags.REMAINING_OPTIONS_TEXT)
        .assertTextContains("${Poll.MAX_OPTIONS - 3}", substring = true)
  }

  @Test
  fun pollCreationSheet_removeOption_removesFieldAndUpdatesCounter() {
    setupPollCreationSheet()

    // Add a third option first
    composeTestRule.onNodeWithTag(PollCreationTestTags.ADD_OPTION_BUTTON).performClick()
    composeTestRule.onNodeWithTag(PollCreationTestTags.getOptionFieldTag(2)).assertIsDisplayed()

    // Remove the third option
    composeTestRule.onNodeWithTag(PollCreationTestTags.getRemoveOptionTag(2)).performClick()

    // Should be back to 2 options
    assertEquals(2, pollViewModel.creationState.value.options.size)
    composeTestRule
        .onNodeWithTag(PollCreationTestTags.REMAINING_OPTIONS_TEXT)
        .assertTextContains("${Poll.MAX_OPTIONS - 2}", substring = true)
  }

  @Test
  fun pollCreationSheet_maxOptions_disablesAddButton() {
    setupPollCreationSheet()

    // Add options until max
    repeat(Poll.MAX_OPTIONS - 2) {
      composeTestRule.onNodeWithTag(PollCreationTestTags.ADD_OPTION_BUTTON).performClick()
    }

    // Add button should be disabled at max
    composeTestRule.onNodeWithTag(PollCreationTestTags.ADD_OPTION_BUTTON).assertIsNotEnabled()

    // Remaining count should be 0
    composeTestRule
        .onNodeWithTag(PollCreationTestTags.REMAINING_OPTIONS_TEXT)
        .assertTextContains("0", substring = true)
  }

  @Test
  fun pollCreationSheet_toggles_updateState() {
    setupPollCreationSheet()

    // Initial state - both off
    assertFalse(pollViewModel.creationState.value.isAnonymous)
    assertFalse(pollViewModel.creationState.value.allowMultipleAnswers)

    // Toggle anonymous
    composeTestRule.onNodeWithTag(PollCreationTestTags.ANONYMOUS_SWITCH).performClick()
    assertTrue(pollViewModel.creationState.value.isAnonymous)

    // Toggle multiple answers
    composeTestRule.onNodeWithTag(PollCreationTestTags.MULTIPLE_ANSWERS_SWITCH).performClick()
    assertTrue(pollViewModel.creationState.value.allowMultipleAnswers)
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
  fun pollCreationSheet_createPoll_success() {
    var pollCreated = false
    composeTestRule.setContent {
      PollCreationSheet(
          viewModel = pollViewModel,
          creatorName = testUserName,
          onDismiss = {},
          onPollCreated = { pollCreated = true })
    }

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

    // Click create
    composeTestRule.onNodeWithTag(PollCreationTestTags.CREATE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify poll was created
    assertTrue(pollCreated)
    assertTrue(fakePollRepository.createdPolls.isNotEmpty())
  }

  @Test
  fun pollCreationSheet_dismiss_resetsState() {
    var dismissed = false
    composeTestRule.setContent {
      PollCreationSheet(
          viewModel = pollViewModel,
          creatorName = testUserName,
          onDismiss = { dismissed = true },
          onPollCreated = {})
    }

    // Enter some data
    composeTestRule
        .onNodeWithTag(PollCreationTestTags.QUESTION_FIELD)
        .performTextInput("Test question")

    // Click close
    composeTestRule.onNodeWithTag(PollCreationTestTags.CLOSE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // State should be reset
    assertTrue(dismissed)
    assertEquals("", pollViewModel.creationState.value.question)
  }

  // ============================================================================
  // Poll Display Card Tests (Combined)
  // ============================================================================

  @Test
  fun pollCard_displaysQuestionAndOptions() {
    val poll = createSamplePoll("poll1", "What is your favorite color?")
    setupPollCard(poll)

    // Verify question
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollQuestionTag("poll1"))
        .assertTextContains("What is your favorite color?")

    // Verify options
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
  fun pollCard_badges_displayCorrectly() {
    // Test closed poll badge
    val closedPoll = createSamplePoll("poll1").copy(isClosed = true)
    setupPollCard(closedPoll)
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollClosedBadgeTag("poll1"))
        .assertIsDisplayed()

    // Test anonymous poll badge
    val anonymousPoll = createSamplePoll("poll2").copy(isAnonymous = true)
    composeTestRule.setContent {
      PollCard(
          poll = anonymousPoll,
          currentUserId = testUserId,
          voterProfiles = emptyMap(),
          onVote = {},
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = {})
    }
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollAnonymousBadgeTag("poll2"))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_creatorMenu_visibleAndFunctional() {
    var closeCalled = false
    val poll = createSamplePoll("poll1").copy(creatorId = testUserId)

    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          voterProfiles = emptyMap(),
          onVote = {},
          onClosePoll = { closeCalled = true },
          onReopenPoll = {},
          onDeletePoll = {})
    }

    // Menu button should be visible for creator
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag("poll1"))
        .assertIsDisplayed()

    // Open menu and click close
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag("poll1")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Close poll", substring = true).performClick()

    assertTrue(closeCalled)
  }

  @Test
  fun pollCard_reopenPoll_fromMenu() {
    var reopenCalled = false
    val poll = createSamplePoll("poll1").copy(creatorId = testUserId, isClosed = true)

    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          voterProfiles = emptyMap(),
          onVote = {},
          onClosePoll = {},
          onReopenPoll = { reopenCalled = true },
          onDeletePoll = {})
    }

    // Open menu and click reopen
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag("poll1")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Reopen poll", substring = true).performClick()

    assertTrue(reopenCalled)
  }

  @Test
  fun pollCard_deletePoll_callsCallback() {
    var deleteCalled = false
    val poll = createSamplePoll("poll1").copy(creatorId = testUserId)

    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          voterProfiles = emptyMap(),
          onVote = {},
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = { deleteCalled = true })
    }

    // Open menu and click delete
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag("poll1")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Delete poll", substring = true).performClick()

    // Click confirm in dialog (if present) or verify callback was called
    composeTestRule.waitForIdle()

    // Try to click confirm button if dialog is shown
    try {
      composeTestRule.onNodeWithText("Delete", substring = true).performClick()
    } catch (_: AssertionError) {
      // Dialog might auto-confirm or not be present
    }

    assertTrue(deleteCalled)
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

    assertEquals(0, votedOptionId)
  }

  @Test
  fun pollCard_closedPoll_votingDisabled() {
    var voteCalled = false
    val poll = createSamplePoll("poll1").copy(isClosed = true)

    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          voterProfiles = emptyMap(),
          onVote = { voteCalled = true },
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = {})
    }

    // Try to click on option - should not trigger vote on closed poll
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollOptionTag("poll1", 0)).performClick()

    // Vote callback should not be called for closed polls
    assertFalse(voteCalled)
  }

  @Test
  fun pollCard_multipleAnswers_allowsMultipleSelections() {
    val votedOptions = mutableListOf<Int>()
    val poll = createSamplePoll("poll1").copy(allowMultipleAnswers = true)

    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          voterProfiles = emptyMap(),
          onVote = { optionId -> votedOptions.add(optionId) },
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = {})
    }

    // Click on both options
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollOptionTag("poll1", 0)).performClick()
    composeTestRule.onNodeWithTag(PollDisplayTestTags.getPollOptionTag("poll1", 1)).performClick()

    // Both votes should be recorded
    assertTrue(votedOptions.contains(0))
    assertTrue(votedOptions.contains(1))
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

    val createdPolls = mutableListOf<Poll>()

    override fun getNewPollId(): String = "poll_${counter++}"

    override fun observePollsForConversation(conversationId: String): Flow<List<Poll>> {
      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      return flow.map { polls -> polls.filter { it.conversationId == conversationId } }
    }

    override suspend fun getPoll(conversationId: String, pollId: String): Poll? {
      return pollsByConversation[conversationId]?.find { it.id == pollId }
    }

    override suspend fun createPoll(poll: Poll) {
      createdPolls.add(poll)
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
      val polls = pollsByConversation[conversationId] ?: return
      val pollIndex = polls.indexOfFirst { it.id == pollId }
      if (pollIndex == -1) return

      val poll = polls[pollIndex]
      val updatedOptions =
          poll.options.map { option ->
            if (option.id == optionId && !option.voterIds.contains(userId)) {
              option.copy(voterIds = option.voterIds + userId)
            } else {
              option
            }
          }
      polls[pollIndex] = poll.copy(options = updatedOptions)

      val flow = pollsFlows[conversationId]
      flow?.value = polls.toList()
    }

    override suspend fun removeVote(
        conversationId: String,
        pollId: String,
        optionId: Int,
        userId: String
    ) {
      val polls = pollsByConversation[conversationId] ?: return
      val pollIndex = polls.indexOfFirst { it.id == pollId }
      if (pollIndex == -1) return

      val poll = polls[pollIndex]
      val updatedOptions =
          poll.options.map { option ->
            if (option.id == optionId) {
              option.copy(voterIds = option.voterIds - userId)
            } else {
              option
            }
          }
      polls[pollIndex] = poll.copy(options = updatedOptions)

      val flow = pollsFlows[conversationId]
      flow?.value = polls.toList()
    }

    override suspend fun closePoll(conversationId: String, pollId: String, userId: String) {
      val polls = pollsByConversation[conversationId] ?: return
      val pollIndex = polls.indexOfFirst { it.id == pollId }
      if (pollIndex == -1) return

      polls[pollIndex] = polls[pollIndex].copy(isClosed = true)
      val flow = pollsFlows[conversationId]
      flow?.value = polls.toList()
    }

    override suspend fun reopenPoll(conversationId: String, pollId: String, userId: String) {
      val polls = pollsByConversation[conversationId] ?: return
      val pollIndex = polls.indexOfFirst { it.id == pollId }
      if (pollIndex == -1) return

      polls[pollIndex] = polls[pollIndex].copy(isClosed = false)
      val flow = pollsFlows[conversationId]
      flow?.value = polls.toList()
    }

    override suspend fun deletePoll(conversationId: String, pollId: String, userId: String) {
      val polls = pollsByConversation[conversationId] ?: return
      polls.removeAll { it.id == pollId }
      val flow = pollsFlows[conversationId]
      flow?.value = polls.toList()
    }
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
