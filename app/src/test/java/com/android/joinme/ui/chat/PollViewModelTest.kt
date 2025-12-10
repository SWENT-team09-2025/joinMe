package com.android.joinme.ui.chat

import com.android.joinme.model.chat.ChatRepository
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.Poll
import com.android.joinme.model.chat.PollOption
import com.android.joinme.model.chat.PollRepository
import com.android.joinme.model.profile.Profile
import com.android.joinme.model.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PollViewModel.
 *
 * Tests poll creation, voting, state management, and validation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PollViewModelTest {

  private class FakeProfileRepository : ProfileRepository {
    private val profiles = mutableMapOf<String, Profile>()
    var shouldThrowError = false

    fun addProfile(profile: Profile) {
      profiles[profile.uid] = profile
    }

    override suspend fun getProfile(uid: String): Profile? {
      if (shouldThrowError) throw Exception("Profile fetch error")
      return profiles[uid]
    }

    override suspend fun getProfilesByIds(uids: List<String>): List<Profile>? {
      if (shouldThrowError) throw Exception("Profiles fetch error")
      return uids.mapNotNull { profiles[it] }
    }

    override suspend fun createOrUpdateProfile(profile: Profile) {
      profiles[profile.uid] = profile
    }

    override suspend fun deleteProfile(uid: String) {
      profiles.remove(uid)
    }

    override suspend fun uploadProfilePhoto(
        context: android.content.Context,
        uid: String,
        imageUri: android.net.Uri
    ): String = "https://example.com/photo.jpg"

    override suspend fun deleteProfilePhoto(uid: String) {}

    override suspend fun followUser(followerId: String, followedId: String) {}

    override suspend fun unfollowUser(followerId: String, followedId: String) {}

    override suspend fun isFollowing(followerId: String, followedId: String): Boolean = false

    override suspend fun getFollowing(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getFollowers(userId: String, limit: Int): List<Profile> = emptyList()

    override suspend fun getMutualFollowing(userId1: String, userId2: String): List<Profile> =
        emptyList()
  }

  private class FakePollRepository : PollRepository {
    var shouldThrowError = false
    var errorMessage = "Test error"
    private val pollsByConversation = mutableMapOf<String, MutableList<Poll>>()
    private val pollsFlows = mutableMapOf<String, MutableStateFlow<List<Poll>>>()
    private var counter = 0

    fun setPolls(conversationId: String, polls: List<Poll>) {
      val chatPolls = pollsByConversation.getOrPut(conversationId) { mutableListOf() }
      chatPolls.clear()
      chatPolls.addAll(polls)

      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      flow.value = chatPolls.sortedBy { it.createdAt }
    }

    override fun getNewPollId(): String = "poll_${counter++}"

    override fun observePollsForConversation(conversationId: String): Flow<List<Poll>> {
      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      return flow.map { polls -> polls.filter { it.conversationId == conversationId } }
    }

    override suspend fun getPoll(conversationId: String, pollId: String): Poll? {
      return pollsByConversation[conversationId]?.find { it.id == pollId }
    }

    override suspend fun createPoll(poll: Poll) {
      if (shouldThrowError) throw Exception(errorMessage)
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
      if (shouldThrowError) throw Exception(errorMessage)
      val polls = pollsByConversation[conversationId] ?: throw Exception("Poll not found")
      val index = polls.indexOfFirst { it.id == pollId }
      if (index == -1) throw Exception("Poll not found")

      val poll = polls[index]
      if (poll.isClosed) throw Exception("Poll is closed")

      val updatedOptions =
          poll.options.map { option ->
            if (!poll.allowMultipleAnswers && option.id != optionId && userId in option.voterIds) {
              option.copy(voterIds = option.voterIds.filter { it != userId })
            } else if (option.id == optionId && userId !in option.voterIds) {
              option.copy(voterIds = option.voterIds + userId)
            } else {
              option
            }
          }
      polls[index] = poll.copy(options = updatedOptions)

      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      flow.value = polls.sortedBy { it.createdAt }
    }

    override suspend fun removeVote(
        conversationId: String,
        pollId: String,
        optionId: Int,
        userId: String
    ) {
      if (shouldThrowError) throw Exception(errorMessage)
      val polls = pollsByConversation[conversationId] ?: throw Exception("Poll not found")
      val index = polls.indexOfFirst { it.id == pollId }
      if (index == -1) throw Exception("Poll not found")

      val poll = polls[index]
      if (poll.isClosed) throw Exception("Poll is closed")

      val updatedOptions =
          poll.options.map { option ->
            if (option.id == optionId && userId in option.voterIds) {
              option.copy(voterIds = option.voterIds.filter { it != userId })
            } else {
              option
            }
          }
      polls[index] = poll.copy(options = updatedOptions)

      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      flow.value = polls.sortedBy { it.createdAt }
    }

    override suspend fun closePoll(conversationId: String, pollId: String, userId: String) {
      if (shouldThrowError) throw Exception(errorMessage)
      val polls = pollsByConversation[conversationId] ?: throw Exception("Poll not found")
      val index = polls.indexOfFirst { it.id == pollId }
      if (index == -1) throw Exception("Poll not found")

      val poll = polls[index]
      if (poll.creatorId != userId) throw Exception("Only the poll owner can close this poll")

      polls[index] = poll.copy(isClosed = true, closedAt = System.currentTimeMillis())

      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      flow.value = polls.sortedBy { it.createdAt }
    }

    override suspend fun reopenPoll(conversationId: String, pollId: String, userId: String) {
      if (shouldThrowError) throw Exception(errorMessage)
      val polls = pollsByConversation[conversationId] ?: throw Exception("Poll not found")
      val index = polls.indexOfFirst { it.id == pollId }
      if (index == -1) throw Exception("Poll not found")

      val poll = polls[index]
      if (poll.creatorId != userId) throw Exception("Only the poll owner can reopen this poll")

      polls[index] = poll.copy(isClosed = false, closedAt = null)

      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      flow.value = polls.sortedBy { it.createdAt }
    }

    override suspend fun deletePoll(conversationId: String, pollId: String, userId: String) {
      if (shouldThrowError) throw Exception(errorMessage)
      val polls = pollsByConversation[conversationId] ?: throw Exception("Poll not found")
      val index = polls.indexOfFirst { it.id == pollId }
      if (index == -1) throw Exception("Poll not found")

      val poll = polls[index]
      if (poll.creatorId != userId) throw Exception("Only the poll owner can delete this poll")

      polls.removeAt(index)

      val flow = pollsFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
      flow.value = polls.sortedBy { it.createdAt }
    }
  }

  private class FakeChatRepository : ChatRepository {
    var shouldThrowError = false
    var errorMessage = "Test error"
    private var messageCounter = 0
    val addedMessages = mutableListOf<Message>()

    override fun getNewMessageId(): String = "msg_${messageCounter++}"

    override fun observeMessagesForConversation(conversationId: String): Flow<List<Message>> =
        MutableStateFlow(emptyList())

    override suspend fun addMessage(message: Message) {
      if (shouldThrowError) throw Exception(errorMessage)
      addedMessages.add(message)
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

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var pollRepository: FakePollRepository
  private lateinit var chatRepository: FakeChatRepository
  private lateinit var profileRepository: FakeProfileRepository
  private lateinit var viewModel: PollViewModel

  private val testConversationId = "conv1"
  private val testUserId = "user1"
  private val testUserName = "User 1"

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    pollRepository = FakePollRepository()
    chatRepository = FakeChatRepository()
    profileRepository = FakeProfileRepository()
    viewModel = PollViewModel(pollRepository, chatRepository, profileRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ============ Initialization Tests ============

  @Test
  fun initialize_setsUserIdAndObservesPolls() = runTest {
    val poll = createSamplePoll("poll1")
    pollRepository.setPolls(testConversationId, listOf(poll))

    viewModel.initialize(testConversationId, testUserId)
    advanceUntilIdle()

    assertEquals(testUserId, viewModel.pollsState.value.currentUserId)
    assertEquals(1, viewModel.pollsState.value.polls.size)
    assertEquals("poll1", viewModel.pollsState.value.polls[0].id)
  }

  // ============ Poll Creation State Tests ============

  @Test
  fun updateQuestionAndOption_updatesStateAndRespectsMaxLength() {
    viewModel.updateQuestion("New question?")
    assertEquals("New question?", viewModel.creationState.value.question)

    viewModel.updateOption(0, "Option text")
    assertEquals("Option text", viewModel.creationState.value.options[0])

    // Max length validation
    viewModel.updateQuestion("A".repeat(Poll.MAX_QUESTION_LENGTH + 100))
    assertEquals("New question?", viewModel.creationState.value.question) // Unchanged

    viewModel.updateOption(0, "A".repeat(Poll.MAX_OPTION_LENGTH + 100))
    assertEquals("Option text", viewModel.creationState.value.options[0]) // Unchanged
  }

  @Test
  fun addAndRemoveOption_respectsBoundaries() {
    assertEquals(2, viewModel.creationState.value.options.size)

    // Add option
    viewModel.addOption()
    assertEquals(3, viewModel.creationState.value.options.size)

    // Remove option
    viewModel.removeOption(0)
    assertEquals(2, viewModel.creationState.value.options.size)

    // Can't remove below minimum
    viewModel.removeOption(0)
    assertEquals(2, viewModel.creationState.value.options.size)

    // Add to max
    repeat(Poll.MAX_OPTIONS - 2) { viewModel.addOption() }
    assertEquals(Poll.MAX_OPTIONS, viewModel.creationState.value.options.size)

    // Can't add above maximum
    viewModel.addOption()
    assertEquals(Poll.MAX_OPTIONS, viewModel.creationState.value.options.size)
  }

  @Test
  fun toggleSettingsAndReset_modifiesState() {
    assertFalse(viewModel.creationState.value.isAnonymous)
    assertFalse(viewModel.creationState.value.allowMultipleAnswers)

    viewModel.toggleAnonymous()
    viewModel.toggleMultipleAnswers()
    viewModel.updateQuestion("Question?")
    viewModel.updateOption(0, "Option")

    assertTrue(viewModel.creationState.value.isAnonymous)
    assertTrue(viewModel.creationState.value.allowMultipleAnswers)

    viewModel.resetCreationState()

    assertEquals("", viewModel.creationState.value.question)
    assertEquals("", viewModel.creationState.value.options[0])
    assertFalse(viewModel.creationState.value.isAnonymous)
    assertFalse(viewModel.creationState.value.allowMultipleAnswers)
  }

  // ============ Creation Validation Tests ============

  @Test
  fun creationState_isValid_checksAllConditions() {
    // Invalid: empty question
    viewModel.updateOption(0, "Option A")
    viewModel.updateOption(1, "Option B")
    assertFalse(viewModel.creationState.value.isValid())

    // Invalid: empty options
    viewModel.resetCreationState()
    viewModel.updateQuestion("Question?")
    assertFalse(viewModel.creationState.value.isValid())

    // Invalid: duplicate options
    viewModel.updateOption(0, "Same Option")
    viewModel.updateOption(1, "same option")
    assertFalse(viewModel.creationState.value.isValid())

    // Valid poll
    viewModel.updateOption(1, "Different Option")
    assertTrue(viewModel.creationState.value.isValid())
  }

  @Test
  fun creationState_helperMethods() {
    // getRemainingOptionsCount
    assertEquals(Poll.MAX_OPTIONS - 2, viewModel.creationState.value.getRemainingOptionsCount())

    // canAddOption and canRemoveOption at min
    assertTrue(viewModel.creationState.value.canAddOption())
    assertFalse(viewModel.creationState.value.canRemoveOption())

    viewModel.addOption()
    assertEquals(Poll.MAX_OPTIONS - 3, viewModel.creationState.value.getRemainingOptionsCount())
    assertTrue(viewModel.creationState.value.canRemoveOption())

    // canAddOption at max
    repeat(Poll.MAX_OPTIONS - 3) { viewModel.addOption() }
    assertFalse(viewModel.creationState.value.canAddOption())
  }

  // ============ Poll Creation Tests ============

  @Test
  fun createPoll_successAndFailurePaths() = runTest {
    viewModel.initialize(testConversationId, testUserId)
    advanceUntilIdle()

    // Invalid poll - validation error
    viewModel.updateOption(0, "Option A")
    viewModel.updateOption(1, "Option B")
    var errorCalled = false
    viewModel.createPoll(creatorName = testUserName, onError = { errorCalled = true })
    assertTrue(errorCalled)
    assertNotNull(viewModel.creationState.value.validationError)

    // Valid poll - success
    viewModel.clearValidationError()
    viewModel.updateQuestion("Test question?")
    var successCalled = false
    viewModel.createPoll(creatorName = testUserName, onSuccess = { successCalled = true })
    advanceUntilIdle()
    assertTrue(successCalled)
    assertEquals("", viewModel.creationState.value.question)

    // Repository error
    pollRepository.shouldThrowError = true
    pollRepository.errorMessage = "Network error"
    viewModel.updateQuestion("Test question?")
    viewModel.updateOption(0, "Option A")
    viewModel.updateOption(1, "Option B")
    var networkError: String? = null
    viewModel.createPoll(creatorName = testUserName, onError = { networkError = it })
    advanceUntilIdle()
    assertNotNull(networkError)
    assertTrue(networkError!!.contains("Network error"))
  }

  // ============ Voting Tests ============

  @Test
  fun vote_addsTogglesAndHandlesClosedPoll() = runTest {
    val poll = createSamplePoll("poll1")
    pollRepository.setPolls(testConversationId, listOf(poll))

    viewModel.initialize(testConversationId, testUserId)
    advanceUntilIdle()

    // Add vote
    viewModel.vote("poll1", 0)
    advanceUntilIdle()
    assertTrue(viewModel.pollsState.value.polls[0].hasUserVotedForOption(testUserId, 0))

    // Toggle off
    viewModel.vote("poll1", 0)
    advanceUntilIdle()
    assertFalse(viewModel.pollsState.value.polls[0].hasUserVotedForOption(testUserId, 0))

    // Closed poll error
    val closedPoll = createSamplePoll("poll1").copy(isClosed = true)
    pollRepository.setPolls(testConversationId, listOf(closedPoll))
    advanceUntilIdle()

    viewModel.vote("poll1", 0)
    advanceUntilIdle()
    assertNotNull(viewModel.pollsState.value.errorMessage)
    assertTrue(viewModel.pollsState.value.errorMessage!!.contains("closed"))
  }

  // ============ Poll Management Tests ============

  @Test
  fun pollManagement_asCreator_closesReopensDeletes() = runTest {
    val poll = createSamplePoll("poll1").copy(creatorId = testUserId)
    pollRepository.setPolls(testConversationId, listOf(poll))

    viewModel.initialize(testConversationId, testUserId)
    advanceUntilIdle()

    // Close
    viewModel.closePoll("poll1")
    advanceUntilIdle()
    assertTrue(viewModel.pollsState.value.polls[0].isClosed)

    // Reopen
    viewModel.reopenPoll("poll1")
    advanceUntilIdle()
    assertFalse(viewModel.pollsState.value.polls[0].isClosed)

    // Delete
    viewModel.deletePoll("poll1")
    advanceUntilIdle()
    assertTrue(viewModel.pollsState.value.polls.isEmpty())
  }

  @Test
  fun pollManagement_notCreator_setsError() = runTest {
    val poll = createSamplePoll("poll1").copy(creatorId = "other_user", isClosed = true)
    pollRepository.setPolls(testConversationId, listOf(poll))

    viewModel.initialize(testConversationId, testUserId)
    advanceUntilIdle()

    viewModel.reopenPoll("poll1")
    advanceUntilIdle()
    assertNotNull(viewModel.pollsState.value.errorMessage)
    assertTrue(viewModel.pollsState.value.errorMessage!!.contains("creator"))

    viewModel.clearError()
    viewModel.deletePoll("poll1")
    advanceUntilIdle()
    assertNotNull(viewModel.pollsState.value.errorMessage)
    assertEquals(1, viewModel.pollsState.value.polls.size)
  }

  // ============ Error Handling Tests ============

  @Test
  fun clearError_clearsAllErrors() = runTest {
    viewModel.initialize(testConversationId, testUserId)
    advanceUntilIdle()

    // Trigger polls state error
    val closedPoll = createSamplePoll("poll1").copy(isClosed = true)
    pollRepository.setPolls(testConversationId, listOf(closedPoll))
    advanceUntilIdle()

    viewModel.vote("poll1", 0)
    advanceUntilIdle()
    assertNotNull(viewModel.pollsState.value.errorMessage)

    viewModel.clearError()
    assertNull(viewModel.pollsState.value.errorMessage)

    // Trigger creation state validation error
    viewModel.createPoll(creatorName = testUserName, onError = {})
    assertNotNull(viewModel.creationState.value.validationError)

    viewModel.clearValidationError()
    assertNull(viewModel.creationState.value.validationError)
  }

  // ============ Additional Coverage Tests ============

  @Test
  fun createPoll_withoutInitialization_setsError() = runTest {
    viewModel.updateQuestion("Test question?")
    viewModel.updateOption(0, "Option A")
    viewModel.updateOption(1, "Option B")

    var errorMessage: String? = null
    viewModel.createPoll(creatorName = testUserName, onError = { errorMessage = it })

    assertNotNull(errorMessage)
    assertTrue(errorMessage!!.contains("not initialized"))
  }

  @Test
  fun optionOperations_invalidIndex_noChange() {
    val initialOptions = viewModel.creationState.value.options.toList()
    viewModel.updateOption(99, "Invalid")
    assertEquals(initialOptions, viewModel.creationState.value.options)

    viewModel.addOption()
    val optionsAfterAdd = viewModel.creationState.value.options.toList()
    viewModel.removeOption(99)
    assertEquals(optionsAfterAdd, viewModel.creationState.value.options)
  }

  @Test
  fun pollOperations_edgeCases() = runTest {
    viewModel.initialize(testConversationId, testUserId)
    advanceUntilIdle()

    // Non-existent poll - returns early without error
    viewModel.vote("nonexistent", 0)
    viewModel.closePoll("nonexistent")
    viewModel.reopenPoll("nonexistent")
    viewModel.deletePoll("nonexistent")
    advanceUntilIdle()
    assertNull(viewModel.pollsState.value.errorMessage)

    // Repository error
    val poll = createSamplePoll("poll1").copy(creatorId = testUserId)
    pollRepository.setPolls(testConversationId, listOf(poll))
    advanceUntilIdle()

    pollRepository.shouldThrowError = true
    pollRepository.errorMessage = "Repository error"

    viewModel.vote("poll1", 0)
    advanceUntilIdle()
    assertNotNull(viewModel.pollsState.value.errorMessage)

    viewModel.clearError()
    viewModel.closePoll("poll1")
    advanceUntilIdle()
    assertNotNull(viewModel.pollsState.value.errorMessage)
  }

  @Test
  fun fetchVoterProfiles_fetchesProfilesAndHandlesErrors() = runTest {
    // Test successful fetch
    val voterProfile =
        Profile(
            uid = "voter1",
            username = "Voter One",
            email = "voter1@test.com",
            interests = emptyList())
    profileRepository.addProfile(voterProfile)

    val pollWithVotes =
        createSamplePoll("poll1")
            .copy(
                options =
                    listOf(
                        PollOption(id = 0, text = "Option A", voterIds = listOf("voter1")),
                        PollOption(id = 1, text = "Option B", voterIds = emptyList())))
    pollRepository.setPolls(testConversationId, listOf(pollWithVotes))

    viewModel.initialize(testConversationId, testUserId)
    advanceUntilIdle()

    assertTrue(viewModel.pollsState.value.voterProfiles.containsKey("voter1"))
    assertEquals("Voter One", viewModel.pollsState.value.voterProfiles["voter1"]?.username)
    assertFalse(viewModel.pollsState.value.isLoadingVoterProfiles)

    // Test error handling - reset and test with error
    profileRepository.shouldThrowError = true
    val pollWithNewVoter =
        createSamplePoll("poll2")
            .copy(
                options =
                    listOf(PollOption(id = 0, text = "Option A", voterIds = listOf("voter2"))))
    pollRepository.setPolls(testConversationId, listOf(pollWithNewVoter))
    advanceUntilIdle()

    assertNull(viewModel.pollsState.value.errorMessage) // Error should not be shown to user
    assertFalse(viewModel.pollsState.value.isLoadingVoterProfiles)
  }

  // ============ Helper Methods ============

  private fun createSamplePoll(id: String): Poll {
    return Poll(
        id = id,
        conversationId = testConversationId,
        creatorId = "creator1",
        creatorName = "Creator",
        question = "Test question?",
        options =
            listOf(
                PollOption(id = 0, text = "Option A", voterIds = emptyList()),
                PollOption(id = 1, text = "Option B", voterIds = emptyList())),
        isAnonymous = false,
        allowMultipleAnswers = false,
        isClosed = false,
        createdAt = 1000L)
  }
}
