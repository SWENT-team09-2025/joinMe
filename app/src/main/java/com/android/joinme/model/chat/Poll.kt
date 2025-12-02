package com.android.joinme.model.chat

// Implemented with help of Claude AI

/**
 * Represents a poll option with text content and votes.
 *
 * @property id Unique identifier for this option within the poll (0-indexed)
 * @property text The text content of this option
 * @property voterIds List of user IDs who have voted for this option
 */
data class PollOption(
    val id: Int = 0,
    val text: String = "",
    val voterIds: List<String> = emptyList()
)

/**
 * Represents a poll in a chat conversation.
 *
 * Polls are stored in Firebase Realtime Database at:
 * `conversations/{conversationId}/polls/{pollId}`
 *
 * @property id Unique identifier for the poll
 * @property conversationId ID of the conversation this poll belongs to
 * @property creatorId ID of the user who created the poll
 * @property creatorName Display name of the poll creator
 * @property question The poll question
 * @property options List of poll options (min 2, max 10)
 * @property isAnonymous When true, voters cannot see who voted for what
 * @property allowMultipleAnswers When true, users can vote for multiple options
 * @property isClosed When true, no more votes can be cast
 * @property createdAt Unix timestamp (milliseconds) when the poll was created
 * @property closedAt Unix timestamp (milliseconds) when the poll was closed, null if still open
 */
data class Poll(
    val id: String = "",
    val conversationId: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val question: String = "",
    val options: List<PollOption> = emptyList(),
    val isAnonymous: Boolean = false,
    val allowMultipleAnswers: Boolean = false,
    val isClosed: Boolean = false,
    val createdAt: Long = 0L,
    val closedAt: Long? = null
) {
  companion object {
    /** Minimum number of options required for a poll */
    const val MIN_OPTIONS = 2

    /** Maximum number of options allowed for a poll */
    const val MAX_OPTIONS = 10

    /** Maximum character length for poll question */
    const val MAX_QUESTION_LENGTH = 300

    /** Maximum character length for each poll option */
    const val MAX_OPTION_LENGTH = 100
  }

  /**
   * Returns the total number of votes cast on this poll. Counts unique voters across all options.
   */
  fun getTotalVotes(): Int {
    return if (allowMultipleAnswers) {
      // Count total votes across all options
      options.sumOf { it.voterIds.size }
    } else {
      // Count unique voters (each user can only vote once)
      options.flatMap { it.voterIds }.distinct().size
    }
  }

  /** Returns the number of unique voters on this poll. */
  fun getUniqueVoterCount(): Int {
    return options.flatMap { it.voterIds }.distinct().size
  }

  /**
   * Returns the vote count for a specific option.
   *
   * @param optionId The ID of the option to check
   * @return The number of votes for the option, or 0 if option not found
   */
  fun getVoteCount(optionId: Int): Int {
    return options.find { it.id == optionId }?.voterIds?.size ?: 0
  }

  /**
   * Returns the percentage of votes for a specific option.
   *
   * @param optionId The ID of the option to check
   * @return The percentage (0-100) of votes for the option, or 0 if no votes
   */
  fun getVotePercentage(optionId: Int): Float {
    val totalVotes = getTotalVotes()
    if (totalVotes == 0) return 0f
    val optionVotes = getVoteCount(optionId)
    return (optionVotes.toFloat() / totalVotes) * 100f
  }

  /**
   * Returns the option IDs that a specific user has voted for.
   *
   * @param userId The ID of the user to check
   * @return List of option IDs the user has voted for
   */
  fun getUserVotes(userId: String): List<Int> {
    return options.filter { userId in it.voterIds }.map { it.id }
  }

  /**
   * Checks if a user has voted for a specific option.
   *
   * @param userId The ID of the user to check
   * @param optionId The ID of the option to check
   * @return True if the user has voted for the option
   */
  fun hasUserVotedForOption(userId: String, optionId: Int): Boolean {
    return options.find { it.id == optionId }?.voterIds?.contains(userId) == true
  }

  /**
   * Checks if a user has voted on this poll at all.
   *
   * @param userId The ID of the user to check
   * @return True if the user has voted for any option
   */
  fun hasUserVoted(userId: String): Boolean {
    return options.any { userId in it.voterIds }
  }

  /**
   * Returns the voter IDs for a specific option. In anonymous polls, this should not be shown to
   * users (except the poll creator).
   *
   * @param optionId The ID of the option
   * @return List of user IDs who voted for the option
   */
  fun getVotersForOption(optionId: Int): List<String> {
    return options.find { it.id == optionId }?.voterIds ?: emptyList()
  }
}

/**
 * Represents the result of a poll validation.
 *
 * @property isValid Whether the poll configuration is valid
 * @property errorMessage Error message if invalid, null if valid
 */
data class PollValidationResult(val isValid: Boolean, val errorMessage: String? = null)

/**
 * Validates poll creation data.
 *
 * @param question The poll question
 * @param options The list of option texts
 * @return Validation result indicating if the poll is valid
 */
fun validatePollCreation(question: String, options: List<String>): PollValidationResult {
  // Validate question
  if (question.isBlank()) {
    return PollValidationResult(false, "Poll question cannot be empty")
  }
  if (question.length > Poll.MAX_QUESTION_LENGTH) {
    return PollValidationResult(
        false, "Poll question cannot exceed ${Poll.MAX_QUESTION_LENGTH} characters")
  }

  // Validate option count
  if (options.size < Poll.MIN_OPTIONS) {
    return PollValidationResult(false, "Poll must have at least ${Poll.MIN_OPTIONS} options")
  }
  if (options.size > Poll.MAX_OPTIONS) {
    return PollValidationResult(false, "Poll cannot have more than ${Poll.MAX_OPTIONS} options")
  }

  // Validate each option
  val nonEmptyOptions = options.filter { it.isNotBlank() }
  if (nonEmptyOptions.size < Poll.MIN_OPTIONS) {
    return PollValidationResult(false, "All options must have content")
  }

  // Check for option text length
  options.forEachIndexed { index, option ->
    if (option.length > Poll.MAX_OPTION_LENGTH) {
      return PollValidationResult(
          false, "Option ${index + 1} cannot exceed ${Poll.MAX_OPTION_LENGTH} characters")
    }
  }

  // Check for duplicate options
  val uniqueOptions = options.map { it.trim().lowercase() }.distinct()
  if (uniqueOptions.size != options.size) {
    return PollValidationResult(false, "Duplicate options are not allowed")
  }

  return PollValidationResult(true)
}
