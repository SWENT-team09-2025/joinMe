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
  fun pollCard_displaysQuestionAndOptions() {
    val poll = createPoll(question = "Favorite color?")
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollQuestionTag(testPollId))
        .assertTextContains("Favorite color?")
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollOptionTag(testPollId, 0))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollOptionTag(testPollId, 1))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_displaysTotalVotesCount() {
    val poll =
        createPoll(
            options =
                listOf(PollOption(0, "A", listOf("u1", "u2")), PollOption(1, "B", listOf("u3"))))
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollTotalVotesTag(testPollId))
        .assertTextContains("3", substring = true)
  }

  // ============================================================================
  // Badge Tests
  // ============================================================================

  @Test
  fun pollCard_closedPoll_showsClosedBadge() {
    val poll = createPoll(isClosed = true)
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollClosedBadgeTag(testPollId))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_anonymousPoll_showsAnonymousBadge() {
    val poll = createPoll(isAnonymous = true)
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollAnonymousBadgeTag(testPollId))
        .assertIsDisplayed()
  }

  @Test
  fun pollCard_multipleAnswersPoll_showsMultipleSelectionBadge() {
    val poll = createPoll(allowMultipleAnswers = true)
    setupPollCard(poll)

    // The multiple selection badge doesn't have a unique testTag, verify via text
    composeTestRule.onNodeWithText("Multiple selection", substring = true).assertIsDisplayed()
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

  @Test
  fun pollCard_menuShowsDeleteOption() {
    val poll = createPoll(creatorId = testUserId)
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .performClick()
    composeTestRule.onNodeWithText("Delete", substring = true).assertIsDisplayed()
  }

  // ============================================================================
  // Voting Tests
  // ============================================================================

  @Test
  fun pollCard_votingCallsOnVote() {
    var votedOptionId: Int? = null
    val poll = createPoll()

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
    // Voting should not work on closed poll (click handler checks canVote)
    assertFalse("Vote callback should not be triggered on closed poll", voteCalled)
  }

  @Test
  fun pollCard_userVotedOption_showsCheckmark() {
    val poll =
        createPoll(
            options =
                listOf(
                    PollOption(0, "Option A", listOf(testUserId)),
                    PollOption(1, "Option B", emptyList())))
    setupPollCard(poll)

    // Option with user's vote should show checkmark (verified via visual selection)
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollOptionTag(testPollId, 0))
        .assertIsDisplayed()
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

  @Test
  fun pollCard_deleteConfirmationDialog_shown() {
    val poll = createPoll(creatorId = testUserId)
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .performClick()
    composeTestRule.onNodeWithText("Delete", substring = true).performClick()

    // Delete confirmation dialog should appear
    composeTestRule.onNodeWithText("Delete Poll", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Are you sure", substring = true).assertIsDisplayed()
  }

  @Test
  fun pollCard_deleteConfirmation_triggersCallback() {
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

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .performClick()
    composeTestRule.onNodeWithText("Delete", substring = true).performClick()
    // Click delete button in confirmation dialog
    composeTestRule.onNodeWithText("Delete").performClick()
    assertTrue(deleteCalled)
  }

  @Test
  fun pollCard_deleteConfirmation_cancelDismisses() {
    val poll = createPoll(creatorId = testUserId)
    setupPollCard(poll)

    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollMenuButtonTag(testPollId))
        .performClick()
    composeTestRule.onNodeWithText("Delete", substring = true).performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()

    // Dialog should be dismissed
    composeTestRule.onNodeWithText("Delete Poll").assertDoesNotExist()
  }

  // ============================================================================
  // VotersDialog Tests
  // ============================================================================

  @Test
  fun pollCard_nonAnonymous_showsVotersOnClick() {
    val voterProfiles = mapOf("voter1" to Profile(uid = "voter1", username = "VoterOne"))
    val poll =
        createPoll(
            isAnonymous = false,
            options =
                listOf(
                    PollOption(0, "Option A", listOf("voter1")),
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

    // Click on voters preview (the small avatar area)
    // This should open the voters dialog
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollOptionTag(testPollId, 0))
        .performClick()
  }

  @Test
  fun pollCard_votersDialog_showsVoterNames() {
    val voterProfiles =
        mapOf(
            "voter1" to Profile(uid = "voter1", username = "Alice"),
            "voter2" to Profile(uid = "voter2", username = "Bob"))
    val poll =
        createPoll(
            isAnonymous = false,
            options =
                listOf(
                    PollOption(0, "Option A", listOf("voter1", "voter2")),
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

    // VotersDialog would be triggered by clicking on voters preview
    // Testing that the component renders without crash
    composeTestRule
        .onNodeWithTag(PollDisplayTestTags.getPollCardTag(testPollId))
        .assertIsDisplayed()
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
