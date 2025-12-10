package com.android.joinme.model.chat

// Implemented with help of Claude AI

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Tests for PollRepositoryLocal.
 *
 * Tests all CRUD operations, voting logic, poll lifecycle, and multi-conversation isolation.
 */
class PollRepositoryLocalTest {

  private lateinit var repo: PollRepositoryLocal
  private lateinit var samplePoll: Poll
  private val testConversationId = "conversation1"

  @Before
  fun setup() {
    repo = PollRepositoryLocal()
    samplePoll =
        Poll(
            id = "poll1",
            conversationId = testConversationId,
            creatorId = "creator1",
            creatorName = "Creator",
            question = "What is your favorite color?",
            options =
                listOf(
                    PollOption(id = 0, text = "Red", voterIds = emptyList()),
                    PollOption(id = 1, text = "Blue", voterIds = emptyList()),
                    PollOption(id = 2, text = "Green", voterIds = emptyList())),
            isAnonymous = false,
            allowMultipleAnswers = false,
            isClosed = false,
            createdAt = 1000L)
  }

  // ============ ID Generation ============

  @Test
  fun getNewPollId_generatesUniquePrefixedIds() {
    val id1 = repo.getNewPollId()
    val id2 = repo.getNewPollId()

    Assert.assertTrue(id1.startsWith("poll_"))
    Assert.assertTrue(id2.startsWith("poll_"))
    Assert.assertNotEquals(id1, id2)
  }

  // ============ Create & Get Poll ============

  @Test
  fun createPoll_appearsInFlowPreservesPropertiesAndCanBeRetrieved() = runTest {
    val fullPoll =
        Poll(
            id = "poll1",
            conversationId = testConversationId,
            creatorId = "creator1",
            creatorName = "Creator Name",
            question = "Test question?",
            options = listOf(PollOption(id = 0, text = "A"), PollOption(id = 1, text = "B")),
            isAnonymous = true,
            allowMultipleAnswers = true,
            isClosed = false,
            createdAt = 5000L)
    repo.createPoll(fullPoll)

    // Check via observe and verify all properties preserved
    val polls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertEquals(1, polls.size)
    val retrieved = polls[0]
    Assert.assertEquals(fullPoll.id, retrieved.id)
    Assert.assertEquals(fullPoll.creatorId, retrieved.creatorId)
    Assert.assertEquals(fullPoll.creatorName, retrieved.creatorName)
    Assert.assertEquals(fullPoll.question, retrieved.question)
    Assert.assertEquals(fullPoll.isAnonymous, retrieved.isAnonymous)
    Assert.assertEquals(fullPoll.allowMultipleAnswers, retrieved.allowMultipleAnswers)
    Assert.assertEquals(fullPoll.createdAt, retrieved.createdAt)

    // Check via getPoll
    val poll = repo.getPoll(testConversationId, "poll1")
    Assert.assertNotNull(poll)
    Assert.assertEquals("poll1", poll!!.id)
  }

  @Test
  fun getPoll_returnsNullForNonExistentOrWrongConversation() = runTest {
    repo.createPoll(samplePoll)

    Assert.assertNull(repo.getPoll(testConversationId, "nonexistent"))
    Assert.assertNull(repo.getPoll("wrongConversation", "poll1"))
  }

  // ============ Observe Polls ============

  @Test
  fun observePolls_filtersAndSortsByConversation() = runTest {
    val poll1 = samplePoll.copy(id = "p1", conversationId = "conv1", createdAt = 3000L)
    val poll2 = samplePoll.copy(id = "p2", conversationId = "conv2", createdAt = 1000L)
    val poll3 = samplePoll.copy(id = "p3", conversationId = "conv1", createdAt = 1000L)

    repo.createPoll(poll1)
    repo.createPoll(poll2)
    repo.createPoll(poll3)

    val conv1Polls = repo.observePollsForConversation("conv1").first()
    Assert.assertEquals(2, conv1Polls.size)
    Assert.assertEquals("p3", conv1Polls[0].id) // Earlier createdAt first
    Assert.assertEquals("p1", conv1Polls[1].id)

    val conv2Polls = repo.observePollsForConversation("conv2").first()
    Assert.assertEquals(1, conv2Polls.size)
  }

  // ============ Voting - Single Answer ============

  @Test
  fun vote_singleAnswer_addsAndMovesVote() = runTest {
    repo.createPoll(samplePoll)

    // Vote for option 0
    repo.vote(testConversationId, "poll1", 0, "user1")
    var polls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertTrue(polls[0].hasUserVotedForOption("user1", 0))

    // Change vote to option 1 - should remove from option 0
    repo.vote(testConversationId, "poll1", 1, "user1")
    polls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertFalse(polls[0].hasUserVotedForOption("user1", 0))
    Assert.assertTrue(polls[0].hasUserVotedForOption("user1", 1))

    // Voting same option again should not duplicate
    repo.vote(testConversationId, "poll1", 1, "user1")
    polls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertEquals(1, polls[0].getVoteCount(1))
  }

  // ============ Voting - Multiple Answers ============

  @Test
  fun vote_multipleAnswers_allowsMultipleVotes() = runTest {
    val multiPoll = samplePoll.copy(allowMultipleAnswers = true)
    repo.createPoll(multiPoll)

    repo.vote(testConversationId, "poll1", 0, "user1")
    repo.vote(testConversationId, "poll1", 1, "user1")

    val polls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertTrue(polls[0].hasUserVotedForOption("user1", 0))
    Assert.assertTrue(polls[0].hasUserVotedForOption("user1", 1))
    Assert.assertEquals(2, polls[0].getUserVotes("user1").size)
  }

  // ============ Voting - Multiple Users ============

  @Test
  fun vote_multipleUsers_tracksAllVotes() = runTest {
    repo.createPoll(samplePoll)
    repo.vote(testConversationId, "poll1", 0, "user1")
    repo.vote(testConversationId, "poll1", 0, "user2")
    repo.vote(testConversationId, "poll1", 1, "user3")

    val polls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertEquals(2, polls[0].getVoteCount(0))
    Assert.assertEquals(1, polls[0].getVoteCount(1))
    Assert.assertEquals(3, polls[0].getUniqueVoterCount())
  }

  // ============ Voting - Errors ============

  @Test
  fun vote_nonExistentPollOrWrongConversation_throwsException() = runTest {
    repo.createPoll(samplePoll)

    try {
      repo.vote(testConversationId, "nonexistent", 0, "user1")
      Assert.fail("Expected exception for non-existent poll")
    } catch (e: Exception) {
      Assert.assertTrue(e.message!!.contains("not found"))
    }

    try {
      repo.vote("wrongConversation", "poll1", 0, "user1")
      Assert.fail("Expected exception for wrong conversation")
    } catch (e: Exception) {
      Assert.assertTrue(e.message!!.contains("not found"))
    }
  }

  // ============ Remove Vote ============

  @Test
  fun removeVote_removesUserVote() = runTest {
    repo.createPoll(samplePoll)
    repo.vote(testConversationId, "poll1", 0, "user1")
    repo.removeVote(testConversationId, "poll1", 0, "user1")

    val polls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertFalse(polls[0].hasUserVotedForOption("user1", 0))
  }

  @Test(expected = Exception::class)
  fun removeVote_closedPoll_throwsException() = runTest {
    repo.createPoll(samplePoll)
    repo.vote(testConversationId, "poll1", 0, "user1")
    repo.closePoll(testConversationId, "poll1", "creator1")
    repo.removeVote(testConversationId, "poll1", 0, "user1")
  }

  // ============ Close Poll ============

  @Test
  fun closePoll_setsClosedAndPreventsVoting() = runTest {
    repo.createPoll(samplePoll)
    repo.closePoll(testConversationId, "poll1", "creator1")

    val polls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertTrue(polls[0].isClosed)
    Assert.assertNotNull(polls[0].closedAt)

    try {
      repo.vote(testConversationId, "poll1", 0, "user1")
      Assert.fail("Expected exception for voting on closed poll")
    } catch (e: Exception) {
      Assert.assertTrue(e.message!!.contains("closed"))
    }
  }

  // ============ Reopen Poll ============

  @Test
  fun reopenPoll_clearsClosedStateAndAllowsVoting() = runTest {
    repo.createPoll(samplePoll)
    repo.closePoll(testConversationId, "poll1", "creator1")
    repo.reopenPoll(testConversationId, "poll1", "creator1")

    val polls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertFalse(polls[0].isClosed)
    Assert.assertNull(polls[0].closedAt)

    // Should be able to vote again
    repo.vote(testConversationId, "poll1", 0, "user1")
    val updatedPolls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertTrue(updatedPolls[0].hasUserVotedForOption("user1", 0))
  }

  // ============ Delete Poll ============

  @Test
  fun deletePoll_removesSuccessfully() = runTest {
    repo.createPoll(samplePoll)
    repo.deletePoll(testConversationId, "poll1", "creator1")

    val polls = repo.observePollsForConversation(testConversationId).first()
    Assert.assertTrue(polls.isEmpty())
  }

  // ============ Owner-Only Operations - Error Cases ============

  @Test
  fun ownerOnlyOperations_nonOwner_throwsException() = runTest {
    repo.createPoll(samplePoll)
    repo.closePoll(testConversationId, "poll1", "creator1")

    // Close poll - non-owner
    try {
      repo.createPoll(samplePoll.copy(id = "poll2"))
      repo.closePoll(testConversationId, "poll2", "notOwner")
      Assert.fail("Expected exception for non-owner closing poll")
    } catch (e: Exception) {
      Assert.assertTrue(e.message!!.contains("owner"))
    }

    // Reopen poll - non-owner
    try {
      repo.reopenPoll(testConversationId, "poll1", "notOwner")
      Assert.fail("Expected exception for non-owner reopening poll")
    } catch (e: Exception) {
      Assert.assertTrue(e.message!!.contains("owner"))
    }

    // Delete poll - non-owner
    try {
      repo.deletePoll(testConversationId, "poll1", "notOwner")
      Assert.fail("Expected exception for non-owner deleting poll")
    } catch (e: Exception) {
      Assert.assertTrue(e.message!!.contains("owner"))
    }
  }

  @Test
  fun ownerOnlyOperations_nonExistentPoll_throwsException() = runTest {
    // Close poll - non-existent
    try {
      repo.closePoll(testConversationId, "nonexistent", "creator1")
      Assert.fail("Expected exception for closing non-existent poll")
    } catch (e: Exception) {
      Assert.assertTrue(e.message!!.contains("not found"))
    }

    // Reopen poll - non-existent
    try {
      repo.reopenPoll(testConversationId, "nonexistent", "creator1")
      Assert.fail("Expected exception for reopening non-existent poll")
    } catch (e: Exception) {
      Assert.assertTrue(e.message!!.contains("not found"))
    }

    // Delete poll - non-existent
    try {
      repo.deletePoll(testConversationId, "nonexistent", "creator1")
      Assert.fail("Expected exception for deleting non-existent poll")
    } catch (e: Exception) {
      Assert.assertTrue(e.message!!.contains("not found"))
    }
  }

  // ============ Utility Methods ============

  @Test
  fun utilityMethods_clearAndGetAllPolls() = runTest {
    repo.createPoll(samplePoll.copy(id = "p1", conversationId = "conv1"))
    repo.createPoll(samplePoll.copy(id = "p2", conversationId = "conv2"))

    // getAllPolls returns all polls across conversations
    val allPolls = repo.getAllPolls()
    Assert.assertEquals(2, allPolls.size)

    // clear removes all polls
    repo.clear()
    val pollsAfterClear = repo.getAllPolls()
    Assert.assertTrue(pollsAfterClear.isEmpty())
  }
}
