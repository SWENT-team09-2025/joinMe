package com.android.joinme.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.joinme.model.chat.Poll
import com.android.joinme.model.chat.PollOption
import com.android.joinme.model.profile.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PollDisplayTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testUserId = "user1"
  private val testPollId = "poll1"

  // ============================================================================
  // PollCard Basic Display Tests
  // ============================================================================

  @Test
  fun pollCard_displaysQuestionOptionsAndVotes() {
    val poll =
        createPoll(
            question = "Favorite color?",
            options =
                listOf(
                    PollOption(0, "Red", listOf("u1", "u2")), PollOption(1, "Blue", listOf("u3"))))
    setupPollCard(poll)

    // Question displayed
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollQuestionTag(testPollId))
        .assertTextContains("Favorite color?")
    // Options displayed
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollOptionTag(testPollId, 0))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollOptionTag(testPollId, 1))
        .assertIsDisplayed()
    // Total votes displayed
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollTotalVotesTag(testPollId))
        .assertTextContains("3", substring = true)
    // Creator name displayed
    composeTestRule.onNodeWithText("Creator", substring = true).assertIsDisplayed()
  }

  @Test
  fun pollCard_displaysVotePercentage() {
    val poll =
        createPoll(
            options =
                listOf(
                    PollOption(0, "Option A", listOf("u1", "u2", "u3")),
                    PollOption(1, "Option B", listOf("u4"))))
    setupPollCard(poll)

    // 75% for option A (3 out of 4)
    composeTestRule.onNodeWithText("75%").assertIsDisplayed()
    // 25% for option B (1 out of 4)
    composeTestRule.onNodeWithText("25%").assertIsDisplayed()
  }

  // ============================================================================
  // Badge Tests
  // ============================================================================

  @Test
  fun pollCard_showsAllBadgesWhenApplicable() {
    val poll = createPoll(isClosed = true, isAnonymous = true, allowMultipleAnswers = true)
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollClosedBadgeTag(testPollId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollAnonymousBadgeTag(testPollId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMultipleSelectionBadgeTag(testPollId))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_noBadges_whenRegularPoll() {
    val poll = createPoll(isClosed = false, isAnonymous = false, allowMultipleAnswers = false)
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollClosedBadgeTag(testPollId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollAnonymousBadgeTag(testPollId))
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMultipleSelectionBadgeTag(testPollId))
        .assertDoesNotExist()
  }

  // ============================================================================
  // Creator Menu Tests
  // ============================================================================

  @Test
  fun pollCard_creatorSeesMenuButton() {
    val poll = createPoll(creatorId = testUserId)
    setupPollCard(poll)
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_nonCreatorDoesNotSeeMenuButton() {
    val poll = createPoll(creatorId = "other_user")
    setupPollCard(poll)
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .assertDoesNotExist()
  }

  @Test
  fun pollCard_menuShowsCloseOption_whenOpen() {
    val poll = createPoll(creatorId = testUserId, isClosed = false)
    setupPollCard(poll)
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .performClick()
    composeTestRule.onNodeWithText("Close poll", substring = true).assertIsDisplayed()
  }

  @Test
  fun pollCard_menuShowsReopenOption_whenClosed() {
    val poll = createPoll(creatorId = testUserId, isClosed = true)
    setupPollCard(poll)
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .performClick()
    composeTestRule.onNodeWithText("Reopen poll", substring = true).assertIsDisplayed()
  }

  // ============================================================================
  // Voting Tests
  // ============================================================================

  @Test
  fun pollCard_votingCallsOnVote() {
    var votedOptionId: Int? = null
    val poll = createPoll(isClosed = false)
    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          onVote = { votedOptionId = it },
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = {})
    }
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollOptionTag(testPollId, 0))
        .performClick()
    assertEquals(0, votedOptionId)
  }

  @Test
  fun pollCard_closedPoll_votingDisabled() {
    var voteCalled = false
    val poll = createPoll(isClosed = true)
    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          onVote = { voteCalled = true },
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = {})
    }
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollOptionTag(testPollId, 0))
        .performClick()
    assertFalse("Vote callback should not be triggered on closed poll", voteCalled)
  }

  // ============================================================================
  // Callback Tests
  // ============================================================================

  @Test
  fun pollCard_closePollCallback_triggered() {
    var closeCalled = false
    val poll = createPoll(creatorId = testUserId, isClosed = false)
    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          onVote = {},
          onClosePoll = { closeCalled = true },
          onReopenPoll = {},
          onDeletePoll = {})
    }

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .performClick()
    composeTestRule.onNodeWithText("Close poll", substring = true).performClick()
    assertTrue(closeCalled)
  }

  @Test
  fun pollCard_reopenPollCallback_triggered() {
    var reopenCalled = false
    val poll = createPoll(creatorId = testUserId, isClosed = true)
    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          onVote = {},
          onClosePoll = {},
          onReopenPoll = { reopenCalled = true },
          onDeletePoll = {})
    }

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .performClick()
    composeTestRule.onNodeWithText("Reopen poll", substring = true).performClick()
    assertTrue(reopenCalled)
  }

  // ============================================================================
  // Delete Dialog Tests
  // ============================================================================

  @Test
  fun pollCard_deleteDialog_showsConfirmationAndCallsCallback() {
    var deleteCalled = false
    val poll = createPoll(creatorId = testUserId)
    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          onVote = {},
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = { deleteCalled = true })
    }

    // Open menu and click delete
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .performClick()
    composeTestRule.onNodeWithText("Delete", substring = true).performClick()

    // Confirmation dialog appears
    composeTestRule.onNodeWithText("Delete Poll", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Are you sure", substring = true).assertIsDisplayed()

    // Confirm deletion
    composeTestRule.onNodeWithText("Delete").performClick()
    assertTrue(deleteCalled)
  }

  @Test
  fun pollCard_deleteDialog_cancelDismisses() {
    val poll = createPoll(creatorId = testUserId)
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .performClick()
    composeTestRule.onNodeWithText("Delete", substring = true).performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.onNodeWithText("Delete Poll").assertDoesNotExist()
  }

  // ============================================================================
  // VotersPreview Tests
  // ============================================================================

  @Test
  fun pollCard_nonAnonymous_showsVotersPreview() {
    val voterProfiles =
        mapOf(
            "v1" to Profile(uid = "v1", username = "Voter1"),
            "v2" to Profile(uid = "v2", username = "Voter2"))

    val poll =
        createPoll(
            isAnonymous = false,
            options =
                listOf(
                    PollOption(0, "Option A", listOf("v1", "v2")),
                    PollOption(1, "Option B", emptyList())))
    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          voterProfiles = voterProfiles,
          onVote = {},
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = {})
    }
    // Card renders successfully with voters
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollCardTag(testPollId))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_votersPreview_showsPlusNBadge_whenMoreThan3Voters() {
    val voters = (1..5).map { "voter$it" }
    val voterProfiles = voters.associateWith { Profile(uid = it, username = "User $it") }

    val poll =
        createPoll(
            isAnonymous = false,
            options =
                listOf(PollOption(0, "Option A", voters), PollOption(1, "Option B", emptyList())))

    composeTestRule.setContent {
      PollCard(
          poll = poll,
          currentUserId = testUserId,
          voterProfiles = voterProfiles,
          onVote = {},
          onClosePoll = {},
          onReopenPoll = {},
          onDeletePoll = {})
    }

    // Should show "+2" badge (5 voters - 3 displayed = 2 remaining)
    composeTestRule.onNodeWithText("+2").assertIsDisplayed()
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

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

  private fun createPoll(
      id: String = testPollId,
      question: String = "Test question?",
      creatorId: String = "creator1",
      options: List<PollOption> =
          listOf(PollOption(0, "Option A", emptyList()), PollOption(1, "Option B", emptyList())),
      isAnonymous: Boolean = false,
      allowMultipleAnswers: Boolean = false,
      isClosed: Boolean = false
  ): Poll {
    return Poll(
        id = id,
        conversationId = "conv1",
        creatorId = creatorId,
        creatorName = "Creator",
        question = question,
        options = options,
        isAnonymous = isAnonymous,
        allowMultipleAnswers = allowMultipleAnswers,
        isClosed = isClosed,
        createdAt = System.currentTimeMillis())
  }
}
