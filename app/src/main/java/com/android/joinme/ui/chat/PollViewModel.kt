package com.android.joinme.ui.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.joinme.R
import com.android.joinme.model.chat.ChatRepository
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import com.android.joinme.model.chat.Poll
import com.android.joinme.model.chat.PollOption
import com.android.joinme.model.chat.PollRepository
import com.android.joinme.model.chat.validatePollCreation
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Sealed class representing poll-related errors with string resource IDs. This allows the ViewModel
 * to remain Context-free while still supporting localization.
 */
sealed class PollError {
  /** Resolves the error message using the provided context. */
  abstract fun getMessage(context: Context): String

  data class LoadFailed(val details: String?) : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_load_failed, details ?: "")
  }

  object ChatNotInitialized : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_chat_not_initialized)
  }

  object InvalidPoll : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_invalid)
  }

  data class CreateFailed(val details: String?) : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_create_failed, details ?: "")
  }

  object VoteOnClosedPoll : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_vote_closed)
  }

  data class VoteFailed(val details: String?) : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_vote_failed, details ?: "")
  }

  object CloseNotCreator : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_close_not_creator)
  }

  data class CloseFailed(val details: String?) : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_close_failed, details ?: "")
  }

  object ReopenNotCreator : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_reopen_not_creator)
  }

  data class ReopenFailed(val details: String?) : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_reopen_failed, details ?: "")
  }

  object DeleteNotCreator : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_delete_not_creator)
  }

  data class DeleteFailed(val details: String?) : PollError() {
    override fun getMessage(context: Context): String =
        context.getString(R.string.poll_error_delete_failed, details ?: "")
  }

  /** Custom validation error from poll validation. */
  data class ValidationError(val message: String) : PollError() {
    override fun getMessage(context: Context): String = message
  }
}

/**
 * Represents the UI state for poll creation.
 *
 * @property question The poll question being composed
 * @property options The list of option texts being composed
 * @property isAnonymous Whether the poll should be anonymous
 * @property allowMultipleAnswers Whether multiple answers are allowed
 * @property isCreating Whether a poll is currently being created
 * @property error Error to display, null if no error
 * @property validationError Validation error, null if valid
 */
data class PollCreationState(
    val question: String = "",
    val options: List<String> = listOf("", ""),
    val isAnonymous: Boolean = false,
    val allowMultipleAnswers: Boolean = false,
    val isCreating: Boolean = false,
    val error: PollError? = null,
    val validationError: PollError? = null
) {
  /** Returns the number of additional options that can be added. */
  fun getRemainingOptionsCount(): Int {
    return Poll.MAX_OPTIONS - options.size
  }

  /** Checks if a new option can be added. */
  fun canAddOption(): Boolean {
    return options.size < Poll.MAX_OPTIONS
  }

  /** Checks if an option can be removed (must keep at least MIN_OPTIONS). */
  fun canRemoveOption(): Boolean {
    return options.size > Poll.MIN_OPTIONS
  }

  /** Checks if the poll is valid and can be created. */
  fun isValid(): Boolean {
    val nonEmptyOptions = options.filter { it.isNotBlank() }
    // Check for duplicates only among non-empty options
    val uniqueNonEmptyOptions = nonEmptyOptions.map { it.trim().lowercase() }.distinct()
    return question.isNotBlank() &&
        question.length <= Poll.MAX_QUESTION_LENGTH &&
        nonEmptyOptions.size >= Poll.MIN_OPTIONS &&
        options.all { it.length <= Poll.MAX_OPTION_LENGTH } &&
        uniqueNonEmptyOptions.size == nonEmptyOptions.size
  }
}

/**
 * Represents the UI state for viewing and interacting with polls.
 *
 * @property polls The list of polls in the current conversation
 * @property isLoading Whether polls are currently loading
 * @property isLoadingVoterProfiles Whether voter profiles are currently being fetched
 * @property error Error to display, null if no error
 * @property currentUserId The ID of the current user
 * @property voterProfiles Map of user IDs to their profiles (for showing who voted)
 */
data class PollsUIState(
    val polls: List<Poll> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingVoterProfiles: Boolean = false,
    val error: PollError? = null,
    val currentUserId: String = "",
    val voterProfiles: Map<String, Profile> = emptyMap()
)

/**
 * ViewModel for managing polls in chat conversations.
 *
 * Handles poll creation, voting, and real-time poll updates.
 *
 * @property pollRepository The repository for poll operations
 * @property chatRepository The repository for sending poll messages to chat
 * @property profileRepository The repository for fetching voter profiles
 */
class PollViewModel(
    private val pollRepository: PollRepository,
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

  private val _creationState = MutableStateFlow(PollCreationState())
  val creationState: StateFlow<PollCreationState> = _creationState.asStateFlow()

  private val _pollsState = MutableStateFlow(PollsUIState())
  val pollsState: StateFlow<PollsUIState> = _pollsState.asStateFlow()

  private var currentConversationId: String = ""
  private var pollObservationJob: Job? = null

  companion object {
    private const val TAG = "PollViewModel"
  }

  /**
   * Initializes the poll observation for a conversation.
   *
   * @param conversationId The ID of the conversation
   * @param userId The ID of the current user
   */
  fun initialize(conversationId: String, userId: String) {
    currentConversationId = conversationId
    _pollsState.value = _pollsState.value.copy(currentUserId = userId)
    observePolls()
  }

  /** Observes polls for the current conversation. */
  private fun observePolls() {
    pollObservationJob?.cancel()
    pollObservationJob =
        viewModelScope.launch {
          _pollsState.value = _pollsState.value.copy(isLoading = true)

          pollRepository
              .observePollsForConversation(currentConversationId)
              .catch { e ->
                Log.e(TAG, "Error observing polls", e)
                _pollsState.value =
                    _pollsState.value.copy(
                        isLoading = false, error = PollError.LoadFailed(e.message))
              }
              .collect { pollsList ->
                _pollsState.value = _pollsState.value.copy(polls = pollsList, isLoading = false)

                // Fetch profiles for all voters
                fetchVoterProfiles(pollsList)
              }
        }
  }

  /** Fetches profile information for all voters in the polls. */
  private fun fetchVoterProfiles(polls: List<Poll>) {
    viewModelScope.launch {
      try {
        // Get all unique voter IDs
        val voterIds = polls.flatMap { poll -> poll.options.flatMap { it.voterIds } }.distinct()

        // Skip if no voters to fetch
        if (voterIds.isEmpty()) {
          _pollsState.value = _pollsState.value.copy(isLoadingVoterProfiles = false)
          return@launch
        }

        // Only fetch profiles we don't already have
        val existingProfiles = _pollsState.value.voterProfiles
        val newVoterIds = voterIds.filter { it !in existingProfiles }

        if (newVoterIds.isEmpty()) {
          _pollsState.value = _pollsState.value.copy(isLoadingVoterProfiles = false)
          return@launch
        }

        _pollsState.value = _pollsState.value.copy(isLoadingVoterProfiles = true)

        // Fetch profiles
        val profiles =
            newVoterIds.mapNotNull { voterId ->
              try {
                profileRepository.getProfile(voterId)
              } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch profile for voter: $voterId", e)
                null
              }
            }

        val newProfilesMap = profiles.associateBy { it.uid }
        _pollsState.value =
            _pollsState.value.copy(
                voterProfiles = existingProfiles + newProfilesMap, isLoadingVoterProfiles = false)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch voter profiles", e)
        _pollsState.value = _pollsState.value.copy(isLoadingVoterProfiles = false)
        // Don't show error to user, profiles are optional
      }
    }
  }

  // ========== Poll Creation Methods ==========

  /**
   * Updates the poll question.
   *
   * @param question The new question text
   */
  fun updateQuestion(question: String) {
    if (question.length <= Poll.MAX_QUESTION_LENGTH) {
      _creationState.value = _creationState.value.copy(question = question, validationError = null)
    }
  }

  /**
   * Updates an option at the specified index.
   *
   * @param index The index of the option to update
   * @param text The new text for the option
   */
  fun updateOption(index: Int, text: String) {
    if (text.length <= Poll.MAX_OPTION_LENGTH) {
      val updatedOptions = _creationState.value.options.toMutableList()
      if (index in updatedOptions.indices) {
        updatedOptions[index] = text
        _creationState.value =
            _creationState.value.copy(options = updatedOptions, validationError = null)
      }
    }
  }

  /** Adds a new empty option to the poll. */
  fun addOption() {
    if (_creationState.value.canAddOption()) {
      val updatedOptions = _creationState.value.options + ""
      _creationState.value =
          _creationState.value.copy(options = updatedOptions, validationError = null)
    }
  }

  /**
   * Removes an option at the specified index.
   *
   * @param index The index of the option to remove
   */
  fun removeOption(index: Int) {
    if (_creationState.value.canRemoveOption()) {
      val updatedOptions = _creationState.value.options.toMutableList()
      if (index in updatedOptions.indices) {
        updatedOptions.removeAt(index)
        _creationState.value =
            _creationState.value.copy(options = updatedOptions, validationError = null)
      }
    }
  }

  /** Toggles anonymous voting setting. */
  fun toggleAnonymous() {
    _creationState.value =
        _creationState.value.copy(isAnonymous = !_creationState.value.isAnonymous)
  }

  /** Toggles multiple answers setting. */
  fun toggleMultipleAnswers() {
    _creationState.value =
        _creationState.value.copy(allowMultipleAnswers = !_creationState.value.allowMultipleAnswers)
  }

  /** Resets the poll creation state. */
  fun resetCreationState() {
    _creationState.value = PollCreationState()
  }

  /**
   * Creates a new poll in the current conversation.
   *
   * @param creatorName The name of the poll creator
   * @param onSuccess Callback invoked when the poll is created successfully
   * @param onError Callback invoked if poll creation fails (receives the PollError)
   */
  fun createPoll(
      creatorName: String,
      onSuccess: () -> Unit = {},
      onError: (PollError) -> Unit = {}
  ) {
    val state = _creationState.value

    // Ensure we have a conversation ID
    if (currentConversationId.isBlank()) {
      Log.e(TAG, "Cannot create poll: conversation ID is not set")
      val error = PollError.ChatNotInitialized
      _creationState.value = state.copy(validationError = error)
      onError(error)
      return
    }

    // Validate the poll
    val nonEmptyOptions = state.options.filter { it.isNotBlank() }
    val validation = validatePollCreation(state.question, nonEmptyOptions)

    if (!validation.isValid) {
      val error =
          validation.errorMessage?.let { PollError.ValidationError(it) } ?: PollError.InvalidPoll
      _creationState.value = state.copy(validationError = error)
      onError(error)
      return
    }

    viewModelScope.launch {
      try {
        _creationState.value = state.copy(isCreating = true, validationError = null)

        val pollId = pollRepository.getNewPollId()
        val currentUserId = _pollsState.value.currentUserId

        val poll =
            Poll(
                id = pollId,
                conversationId = currentConversationId,
                creatorId = currentUserId,
                creatorName = creatorName,
                question = state.question.trim(),
                options =
                    nonEmptyOptions.mapIndexed { index, text ->
                      PollOption(id = index, text = text.trim())
                    },
                isAnonymous = state.isAnonymous,
                allowMultipleAnswers = state.allowMultipleAnswers,
                isClosed = false,
                createdAt = System.currentTimeMillis())

        pollRepository.createPoll(poll)

        // Send a message to the chat with the poll ID
        val messageId = chatRepository.getNewMessageId()
        val pollMessage =
            Message(
                id = messageId,
                conversationId = currentConversationId,
                senderId = currentUserId,
                senderName = creatorName,
                content = pollId, // Store poll ID in content
                timestamp = poll.createdAt,
                type = MessageType.POLL)
        chatRepository.addMessage(pollMessage)

        _creationState.value = PollCreationState()
        onSuccess()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to create poll", e)
        val error = PollError.CreateFailed(e.message)
        _creationState.value = _creationState.value.copy(isCreating = false, error = error)
        onError(error)
      }
    }
  }

  // ========== Voting Methods ==========

  /**
   * Casts a vote for a poll option.
   *
   * For single-answer polls, this will toggle the vote (remove if already voted, add otherwise).
   * For multiple-answer polls, this will toggle the specific option.
   *
   * @param pollId The ID of the poll
   * @param optionId The ID of the option to vote for
   */
  fun vote(pollId: String, optionId: Int) {
    val currentUserId = _pollsState.value.currentUserId
    val poll = _pollsState.value.polls.find { it.id == pollId } ?: return

    if (poll.isClosed) {
      _pollsState.value = _pollsState.value.copy(error = PollError.VoteOnClosedPoll)
      return
    }

    viewModelScope.launch {
      try {
        val hasVotedForOption = poll.hasUserVotedForOption(currentUserId, optionId)

        if (hasVotedForOption) {
          // Remove vote (toggle off)
          pollRepository.removeVote(currentConversationId, pollId, optionId, currentUserId)
        } else {
          // Add vote
          pollRepository.vote(currentConversationId, pollId, optionId, currentUserId)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to vote", e)
        _pollsState.value = _pollsState.value.copy(error = PollError.VoteFailed(e.message))
      }
    }
  }

  // ========== Poll Management Methods ==========

  /**
   * Closes a poll, preventing further voting.
   *
   * @param pollId The ID of the poll to close
   */
  fun closePoll(pollId: String) {
    val currentUserId = _pollsState.value.currentUserId
    val poll = _pollsState.value.polls.find { it.id == pollId } ?: return

    if (poll.creatorId != currentUserId) {
      _pollsState.value = _pollsState.value.copy(error = PollError.CloseNotCreator)
      return
    }

    viewModelScope.launch {
      try {
        pollRepository.closePoll(currentConversationId, pollId, currentUserId)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to close poll", e)
        _pollsState.value = _pollsState.value.copy(error = PollError.CloseFailed(e.message))
      }
    }
  }

  /**
   * Reopens a closed poll.
   *
   * @param pollId The ID of the poll to reopen
   */
  fun reopenPoll(pollId: String) {
    val currentUserId = _pollsState.value.currentUserId
    val poll = _pollsState.value.polls.find { it.id == pollId } ?: return

    if (poll.creatorId != currentUserId) {
      _pollsState.value = _pollsState.value.copy(error = PollError.ReopenNotCreator)
      return
    }

    viewModelScope.launch {
      try {
        pollRepository.reopenPoll(currentConversationId, pollId, currentUserId)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to reopen poll", e)
        _pollsState.value = _pollsState.value.copy(error = PollError.ReopenFailed(e.message))
      }
    }
  }

  /**
   * Deletes a poll.
   *
   * @param pollId The ID of the poll to delete
   */
  fun deletePoll(pollId: String) {
    val currentUserId = _pollsState.value.currentUserId
    val poll = _pollsState.value.polls.find { it.id == pollId } ?: return

    if (poll.creatorId != currentUserId) {
      _pollsState.value = _pollsState.value.copy(error = PollError.DeleteNotCreator)
      return
    }

    viewModelScope.launch {
      try {
        pollRepository.deletePoll(currentConversationId, pollId, currentUserId)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to delete poll", e)
        _pollsState.value = _pollsState.value.copy(error = PollError.DeleteFailed(e.message))
      }
    }
  }

  /** Clears the error. */
  fun clearError() {
    _pollsState.value = _pollsState.value.copy(error = null)
    _creationState.value = _creationState.value.copy(error = null)
  }

  /** Clears the validation error. */
  fun clearValidationError() {
    _creationState.value = _creationState.value.copy(validationError = null)
  }
}
