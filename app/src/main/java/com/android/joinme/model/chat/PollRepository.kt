package com.android.joinme.model.chat

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing polls in chat conversations.
 *
 * Polls are stored separately from messages but linked to conversations. Each poll is uniquely
 * identified by its ID and belongs to a specific conversation.
 */
interface PollRepository {

  /**
   * Generates and returns a new unique identifier for a Poll.
   *
   * @return A new unique Poll identifier.
   */
  fun getNewPollId(): String

  /**
   * Observes all polls for a specific conversation and returns them as a Flow.
   *
   * This method provides real-time updates whenever polls are created, updated, or deleted. Polls
   * are ordered by creation timestamp in ascending order.
   *
   * @param conversationId The unique identifier of the conversation to observe polls for.
   * @return A Flow of Poll lists that updates in real-time.
   */
  fun observePollsForConversation(conversationId: String): Flow<List<Poll>>

  /**
   * Gets a single poll by ID.
   *
   * @param conversationId The unique identifier of the conversation containing the poll.
   * @param pollId The unique identifier of the poll.
   * @return The Poll if found, null otherwise.
   */
  suspend fun getPoll(conversationId: String, pollId: String): Poll?

  /**
   * Creates a new poll in a conversation.
   *
   * @param poll The Poll object to create. Must have a valid ID and conversationId.
   * @throws Exception if the poll creation fails.
   */
  suspend fun createPoll(poll: Poll)

  /**
   * Casts a vote for a poll option.
   *
   * For single-answer polls, this will remove the user's previous vote (if any) before adding the
   * new vote. For multiple-answer polls, this adds the vote without affecting other votes.
   *
   * @param conversationId The unique identifier of the conversation containing the poll.
   * @param pollId The unique identifier of the poll.
   * @param optionId The ID of the option to vote for.
   * @param userId The ID of the user casting the vote.
   * @throws Exception if the poll is closed or not found.
   */
  suspend fun vote(conversationId: String, pollId: String, optionId: Int, userId: String)

  /**
   * Removes a vote from a poll option.
   *
   * @param conversationId The unique identifier of the conversation containing the poll.
   * @param pollId The unique identifier of the poll.
   * @param optionId The ID of the option to remove the vote from.
   * @param userId The ID of the user removing their vote.
   * @throws Exception if the poll is closed or not found.
   */
  suspend fun removeVote(conversationId: String, pollId: String, optionId: Int, userId: String)

  /**
   * Closes a poll, preventing further voting.
   *
   * Only the poll creator should be able to close a poll.
   *
   * @param conversationId The unique identifier of the conversation containing the poll.
   * @param pollId The unique identifier of the poll to close.
   * @throws Exception if the poll is not found.
   */
  suspend fun closePoll(conversationId: String, pollId: String)

  /**
   * Reopens a closed poll, allowing voting again.
   *
   * Only the poll creator should be able to reopen a poll.
   *
   * @param conversationId The unique identifier of the conversation containing the poll.
   * @param pollId The unique identifier of the poll to reopen.
   * @throws Exception if the poll is not found.
   */
  suspend fun reopenPoll(conversationId: String, pollId: String)

  /**
   * Deletes a poll from a conversation.
   *
   * Only the poll creator should be able to delete a poll.
   *
   * @param conversationId The unique identifier of the conversation containing the poll.
   * @param pollId The unique identifier of the poll to delete.
   * @throws Exception if the poll is not found.
   */
  suspend fun deletePoll(conversationId: String, pollId: String)
}
