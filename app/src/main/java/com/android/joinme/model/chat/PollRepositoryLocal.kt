package com.android.joinme.model.chat

// Implemented with help of Claude AI

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Exception message for when a poll is not found in the local repository. */
const val POLL_NOT_FOUND = "PollRepositoryLocal: Poll not found"

/** Exception message for when trying to vote on a closed poll. */
const val POLL_CLOSED = "PollRepositoryLocal: Cannot vote on closed poll"

/** Exception message for when a non-owner tries to modify a poll. */
const val NOT_POLL_OWNER = "PollRepositoryLocal: Only the poll owner can perform this action"

/** Exception message for when an invalid option ID is provided. */
const val INVALID_OPTION = "PollRepositoryLocal: Invalid option ID"

/**
 * In-memory implementation of [PollRepository] for offline mode or testing.
 *
 * This implementation stores polls in a mutable list and provides real-time updates through
 * StateFlow. Useful for testing without Firebase dependencies.
 *
 * Thread-safe: Uses Mutex to prevent race conditions when multiple coroutines access the polls list
 * concurrently.
 */
class PollRepositoryLocal : PollRepository {
  private val polls: MutableList<Poll> = mutableListOf()
  private val pollsFlow = MutableStateFlow<List<Poll>>(emptyList())
  private val mutex = Mutex()
  private var counter = 0

  override fun getNewPollId(): String {
    return "poll_${counter++}"
  }

  override fun observePollsForConversation(conversationId: String): Flow<List<Poll>> {
    return pollsFlow.map { allPolls ->
      allPolls.filter { it.conversationId == conversationId }.sortedBy { it.createdAt }
    }
  }

  override suspend fun getPoll(conversationId: String, pollId: String): Poll? {
    mutex.withLock {
      return polls.find { it.id == pollId && it.conversationId == conversationId }
    }
  }

  override suspend fun createPoll(poll: Poll) {
    mutex.withLock {
      polls.add(poll)
      pollsFlow.value = polls.toList()
    }
  }

  override suspend fun vote(conversationId: String, pollId: String, optionId: Int, userId: String) {
    mutex.withLock {
      val index = polls.indexOfFirst { it.id == pollId && it.conversationId == conversationId }

      if (index == -1) {
        throw Exception(POLL_NOT_FOUND)
      }

      val poll = polls[index]

      if (poll.isClosed) {
        throw Exception(POLL_CLOSED)
      }

      require(!(poll.options.none { it.id == optionId })) { INVALID_OPTION }

      // Build updated options
      val updatedOptions =
          poll.options.map { option ->
            if (!poll.allowMultipleAnswers && option.id != optionId && userId in option.voterIds) {
              // For single-answer polls, remove vote from other options
              option.copy(voterIds = option.voterIds.filter { it != userId })
            } else if (option.id == optionId && userId !in option.voterIds) {
              // Add vote to selected option
              option.copy(voterIds = option.voterIds + userId)
            } else {
              option
            }
          }

      polls[index] = poll.copy(options = updatedOptions)
      pollsFlow.value = polls.toList()
    }
  }

  override suspend fun removeVote(
      conversationId: String,
      pollId: String,
      optionId: Int,
      userId: String
  ) {
    mutex.withLock {
      val index = polls.indexOfFirst { it.id == pollId && it.conversationId == conversationId }

      if (index == -1) {
        throw Exception(POLL_NOT_FOUND)
      }

      val poll = polls[index]

      if (poll.isClosed) {
        throw Exception(POLL_CLOSED)
      }

      require(!(poll.options.none { it.id == optionId })) { INVALID_OPTION }

      // Build updated options
      val updatedOptions =
          poll.options.map { option ->
            if (option.id == optionId && userId in option.voterIds) {
              option.copy(voterIds = option.voterIds.filter { it != userId })
            } else {
              option
            }
          }

      polls[index] = poll.copy(options = updatedOptions)
      pollsFlow.value = polls.toList()
    }
  }

  override suspend fun closePoll(conversationId: String, pollId: String, userId: String) {
    mutex.withLock {
      val index = polls.indexOfFirst { it.id == pollId && it.conversationId == conversationId }

      if (index == -1) {
        throw Exception(POLL_NOT_FOUND)
      }

      val poll = polls[index]
      if (poll.creatorId != userId) {
        throw Exception(NOT_POLL_OWNER)
      }

      polls[index] = poll.copy(isClosed = true, closedAt = System.currentTimeMillis())
      pollsFlow.value = polls.toList()
    }
  }

  override suspend fun reopenPoll(conversationId: String, pollId: String, userId: String) {
    mutex.withLock {
      val index = polls.indexOfFirst { it.id == pollId && it.conversationId == conversationId }

      if (index == -1) {
        throw Exception(POLL_NOT_FOUND)
      }

      val poll = polls[index]
      if (poll.creatorId != userId) {
        throw Exception(NOT_POLL_OWNER)
      }

      polls[index] = poll.copy(isClosed = false, closedAt = null)
      pollsFlow.value = polls.toList()
    }
  }

  override suspend fun deletePoll(conversationId: String, pollId: String, userId: String) {
    mutex.withLock {
      val index = polls.indexOfFirst { it.id == pollId && it.conversationId == conversationId }

      if (index == -1) {
        throw Exception(POLL_NOT_FOUND)
      }

      val poll = polls[index]
      if (poll.creatorId != userId) {
        throw Exception(NOT_POLL_OWNER)
      }

      polls.removeAt(index)
      pollsFlow.value = polls.toList()
    }
  }

  /** Clears all polls from the local repository. Useful for test setup/teardown. */
  suspend fun clear() {
    mutex.withLock {
      polls.clear()
      pollsFlow.value = emptyList()
    }
  }

  /**
   * Gets all polls in the repository. Useful for testing.
   *
   * @return List of all polls
   */
  suspend fun getAllPolls(): List<Poll> {
    mutex.withLock {
      return polls.toList()
    }
  }
}
