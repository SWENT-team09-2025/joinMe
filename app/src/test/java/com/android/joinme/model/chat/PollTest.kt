package com.android.joinme.model.chat

import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for Poll model and validation logic.
 *
 * Tests poll data model methods, vote calculations, and poll validation.
 */
class PollTest {

  private lateinit var samplePoll: Poll
  private lateinit var optionA: PollOption
  private lateinit var optionB: PollOption
  private lateinit var optionC: PollOption

  @Before
  fun setup() {
    optionA = PollOption(id = 0, text = "Option A", voterIds = listOf("user1", "user2"))
    optionB = PollOption(id = 1, text = "Option B", voterIds = listOf("user3"))
    optionC = PollOption(id = 2, text = "Option C", voterIds = emptyList())

    samplePoll =
        Poll(
            id = "poll1",
            conversationId = "conv1",
            creatorId = "creator1",
            creatorName = "Creator",
            question = "What is your favorite color?",
            options = listOf(optionA, optionB, optionC),
            isAnonymous = false,
            allowMultipleAnswers = false,
            isClosed = false,
            createdAt = 1000L)
  }

  // ============ Poll Option Tests ============

  @Test
  fun pollOption_customValues() {
    val option = PollOption(id = 5, text = "Custom option", voterIds = listOf("a", "b", "c"))
    Assert.assertEquals(5, option.id)
    Assert.assertEquals("Custom option", option.text)
    Assert.assertEquals(3, option.voterIds.size)
  }

  // ============ Vote Counting Tests ============

  @Test
  fun getTotalVotes() {
    // Both single and multiple answer modes count total votes the same way
    Assert.assertEquals(3, samplePoll.getTotalVotes())
    Assert.assertEquals(3, samplePoll.copy(allowMultipleAnswers = true).getTotalVotes())

    // Empty poll returns 0
    val emptyPoll =
        samplePoll.copy(
            options =
                listOf(
                    PollOption(id = 0, text = "A", voterIds = emptyList()),
                    PollOption(id = 1, text = "B", voterIds = emptyList())))
    Assert.assertEquals(0, emptyPoll.getTotalVotes())
  }

  @Test
  fun getUniqueVoterCount() {
    // Basic count
    Assert.assertEquals(3, samplePoll.getUniqueVoterCount())

    // Same user voted for multiple options - should still count as 1 unique voter
    val poll =
        samplePoll.copy(
            allowMultipleAnswers = true,
            options =
                listOf(
                    PollOption(id = 0, text = "A", voterIds = listOf("user1", "user2")),
                    PollOption(id = 1, text = "B", voterIds = listOf("user1", "user3"))))
    Assert.assertEquals(3, poll.getUniqueVoterCount())
  }

  @Test
  fun getVoteCount() {
    Assert.assertEquals(2, samplePoll.getVoteCount(0))
    Assert.assertEquals(1, samplePoll.getVoteCount(1))
    Assert.assertEquals(0, samplePoll.getVoteCount(2))
  }

  @Test
  fun getVoteCount_nonExistentOption() {
    Assert.assertEquals(0, samplePoll.getVoteCount(999))
  }

  // ============ Vote Percentage Tests ============

  @Test
  fun getVotePercentage() {
    // Total votes = 3, option A has 2 votes = 66.67%
    val percentA = samplePoll.getVotePercentage(0)
    Assert.assertTrue(percentA > 66.0f && percentA < 67.0f)

    // option B has 1 vote = 33.33%
    val percentB = samplePoll.getVotePercentage(1)
    Assert.assertTrue(percentB > 33.0f && percentB < 34.0f)

    // option C has 0 votes = 0%
    Assert.assertEquals(0f, samplePoll.getVotePercentage(2), 0.01f)

    // Non-existent option returns 0%
    Assert.assertEquals(0f, samplePoll.getVotePercentage(999), 0.01f)

    // Empty poll returns 0%
    val emptyPoll =
        samplePoll.copy(
            options =
                listOf(
                    PollOption(id = 0, text = "A", voterIds = emptyList()),
                    PollOption(id = 1, text = "B", voterIds = emptyList())))
    Assert.assertEquals(0f, emptyPoll.getVotePercentage(0), 0.01f)
  }

  // ============ User Vote Tracking Tests ============

  @Test
  fun getUserVotes() {
    Assert.assertEquals(listOf(0), samplePoll.getUserVotes("user1"))
    Assert.assertEquals(listOf(0), samplePoll.getUserVotes("user2"))
    Assert.assertEquals(listOf(1), samplePoll.getUserVotes("user3"))
    Assert.assertTrue(samplePoll.getUserVotes("user4").isEmpty())
  }

  @Test
  fun getUserVotes_multipleAnswers() {
    val poll =
        samplePoll.copy(
            allowMultipleAnswers = true,
            options =
                listOf(
                    PollOption(id = 0, text = "A", voterIds = listOf("user1")),
                    PollOption(id = 1, text = "B", voterIds = listOf("user1")),
                    PollOption(id = 2, text = "C", voterIds = emptyList())))
    val votes = poll.getUserVotes("user1")
    Assert.assertEquals(2, votes.size)
    Assert.assertTrue(votes.contains(0))
    Assert.assertTrue(votes.contains(1))
  }

  @Test
  fun hasUserVotedForOption() {
    Assert.assertTrue(samplePoll.hasUserVotedForOption("user1", 0))
    Assert.assertFalse(samplePoll.hasUserVotedForOption("user1", 1))
    Assert.assertTrue(samplePoll.hasUserVotedForOption("user3", 1))
    Assert.assertFalse(samplePoll.hasUserVotedForOption("user4", 0))
  }

  @Test
  fun hasUserVoted() {
    Assert.assertTrue(samplePoll.hasUserVoted("user1"))
    Assert.assertTrue(samplePoll.hasUserVoted("user2"))
    Assert.assertTrue(samplePoll.hasUserVoted("user3"))
    Assert.assertFalse(samplePoll.hasUserVoted("user4"))
  }

  @Test
  fun getVotersForOption() {
    Assert.assertEquals(listOf("user1", "user2"), samplePoll.getVotersForOption(0))
    Assert.assertEquals(listOf("user3"), samplePoll.getVotersForOption(1))
    Assert.assertTrue(samplePoll.getVotersForOption(2).isEmpty())
    Assert.assertTrue(samplePoll.getVotersForOption(999).isEmpty())
  }

  // ============ Poll Validation Tests ============

  @Test
  fun validatePollCreation_validPoll() {
    val result =
        validatePollCreation(
            question = "What is your favorite color?", options = listOf("Red", "Blue", "Green"))
    Assert.assertTrue(result.isValid)
    Assert.assertNull(result.errorMessage)
  }

  @Test
  fun validatePollCreation_emptyQuestion() {
    val result = validatePollCreation(question = "", options = listOf("A", "B"))
    Assert.assertFalse(result.isValid)
    Assert.assertTrue(result.errorMessage!!.contains("empty"))
  }

  @Test
  fun validatePollCreation_blankQuestion() {
    val result = validatePollCreation(question = "   ", options = listOf("A", "B"))
    Assert.assertFalse(result.isValid)
  }

  @Test
  fun validatePollCreation_questionTooLong() {
    val longQuestion = "A".repeat(Poll.MAX_QUESTION_LENGTH + 1)
    val result = validatePollCreation(question = longQuestion, options = listOf("A", "B"))
    Assert.assertFalse(result.isValid)
    Assert.assertTrue(result.errorMessage!!.contains(Poll.MAX_QUESTION_LENGTH.toString()))
  }

  @Test
  fun validatePollCreation_tooFewOptions() {
    val result = validatePollCreation(question = "Question?", options = listOf("Only one"))
    Assert.assertFalse(result.isValid)
    Assert.assertTrue(result.errorMessage!!.contains(Poll.MIN_OPTIONS.toString()))
  }

  @Test
  fun validatePollCreation_tooManyOptions() {
    val tooManyOptions = (1..Poll.MAX_OPTIONS + 1).map { "Option $it" }
    val result = validatePollCreation(question = "Question?", options = tooManyOptions)
    Assert.assertFalse(result.isValid)
    Assert.assertTrue(result.errorMessage!!.contains(Poll.MAX_OPTIONS.toString()))
  }

  @Test
  fun validatePollCreation_emptyOptions() {
    val result = validatePollCreation(question = "Question?", options = listOf("", ""))
    Assert.assertFalse(result.isValid)
    Assert.assertTrue(result.errorMessage!!.contains("content"))
  }

  @Test
  fun validatePollCreation_someEmptyOptions() {
    val result = validatePollCreation(question = "Question?", options = listOf("Valid", ""))
    Assert.assertFalse(result.isValid)
  }

  @Test
  fun validatePollCreation_optionTooLong() {
    val longOption = "A".repeat(Poll.MAX_OPTION_LENGTH + 1)
    val result = validatePollCreation(question = "Question?", options = listOf(longOption, "B"))
    Assert.assertFalse(result.isValid)
    Assert.assertTrue(result.errorMessage!!.contains(Poll.MAX_OPTION_LENGTH.toString()))
  }

  @Test
  fun validatePollCreation_duplicateOptions() {
    val result = validatePollCreation(question = "Question?", options = listOf("Same", "Same"))
    Assert.assertFalse(result.isValid)
    Assert.assertTrue(result.errorMessage!!.contains("Duplicate"))
  }

  @Test
  fun validatePollCreation_duplicateOptionsCaseInsensitive() {
    val result = validatePollCreation(question = "Question?", options = listOf("Same", "SAME"))
    Assert.assertFalse(result.isValid)
  }

  @Test
  fun validatePollCreation_duplicateOptionsWithWhitespace() {
    val result = validatePollCreation(question = "Question?", options = listOf("Same", " Same "))
    Assert.assertFalse(result.isValid)
  }

  @Test
  fun validatePollCreation_exactMinOptions() {
    val result = validatePollCreation(question = "Question?", options = listOf("A", "B"))
    Assert.assertTrue(result.isValid)
  }

  @Test
  fun validatePollCreation_exactMaxOptions() {
    val maxOptions = (1..Poll.MAX_OPTIONS).map { "Option $it" }
    val result = validatePollCreation(question = "Question?", options = maxOptions)
    Assert.assertTrue(result.isValid)
  }

  @Test
  fun validatePollCreation_maxLengthQuestion() {
    val maxQuestion = "A".repeat(Poll.MAX_QUESTION_LENGTH)
    val result = validatePollCreation(question = maxQuestion, options = listOf("A", "B"))
    Assert.assertTrue(result.isValid)
  }

  @Test
  fun validatePollCreation_maxLengthOption() {
    val maxOption = "A".repeat(Poll.MAX_OPTION_LENGTH)
    val result = validatePollCreation(question = "Question?", options = listOf(maxOption, "B"))
    Assert.assertTrue(result.isValid)
  }
}
