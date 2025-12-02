package com.android.joinme.model.chat

// Implemented with help of Claude AI

import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test suite for PollRepositoryRealtimeDatabase.
 *
 * Tests the Firebase Realtime Database implementation of the PollRepository interface, including
 * poll CRUD operations, voting, and real-time poll observation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PollRepositoryRealtimeDatabaseTest {

  private lateinit var repository: PollRepositoryRealtimeDatabase
  private lateinit var mockDatabase: FirebaseDatabase
  private lateinit var mockConversationsRef: DatabaseReference
  private lateinit var mockConversationRef: DatabaseReference
  private lateinit var mockPollsRef: DatabaseReference
  private lateinit var mockPollRef: DatabaseReference

  private val testConversationId = "test-conversation-123"
  private val testPollId = "test-poll-456"

  @Before
  fun setup() {
    // Mock Firebase Realtime Database components
    mockDatabase = mockk(relaxed = true)
    mockConversationsRef = mockk(relaxed = true)
    mockConversationRef = mockk(relaxed = true)
    mockPollsRef = mockk(relaxed = true)
    mockPollRef = mockk(relaxed = true)

    // Setup reference chain
    every { mockDatabase.getReference("conversations") } returns mockConversationsRef
    every { mockConversationsRef.child(any()) } returns mockConversationRef
    every { mockConversationRef.child("polls") } returns mockPollsRef
    every { mockPollsRef.child(any()) } returns mockPollRef
    every { mockPollsRef.orderByChild("createdAt") } returns mockk<Query>(relaxed = true)

    repository = PollRepositoryRealtimeDatabase(mockDatabase)
  }

  @After
  fun teardown() {
    clearAllMocks()
  }

  // ============================================================================
  // getNewPollId Tests
  // ============================================================================

  @Test
  fun getNewPollId_generatesUniqueId() {
    // Given
    val mockPushRef = mockk<DatabaseReference>(relaxed = true)
    every { mockConversationsRef.push() } returns mockPushRef
    every { mockPushRef.key } returns "generated-poll-id-123"

    // When
    val pollId = repository.getNewPollId()

    // Then
    assertNotNull(pollId)
    assertEquals("generated-poll-id-123", pollId)
  }

  @Test(expected = IllegalStateException::class)
  fun getNewPollId_throwsExceptionWhenPushReturnsNull() {
    // Given
    val mockPushRef = mockk<DatabaseReference>(relaxed = true)
    every { mockConversationsRef.push() } returns mockPushRef
    every { mockPushRef.key } returns null

    // When - should throw IllegalStateException
    repository.getNewPollId()
  }

  // ============================================================================
  // observePollsForConversation Tests
  // ============================================================================

  @Test
  fun observePollsForConversation_returnsFlowWithPolls() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockPollSnapshot1 =
        createMockPollSnapshot(
            pollId = "poll1",
            question = "What is your favorite color?",
            creatorId = "user1",
            creatorName = "Alice",
            options =
                listOf(
                    mapOf("id" to 0, "text" to "Red", "voterIds" to listOf("user2")),
                    mapOf("id" to 1, "text" to "Blue", "voterIds" to emptyList<String>())))
    val mockPollSnapshot2 =
        createMockPollSnapshot(
            pollId = "poll2",
            question = "Best programming language?",
            creatorId = "user2",
            creatorName = "Bob",
            options =
                listOf(
                    mapOf("id" to 0, "text" to "Kotlin", "voterIds" to emptyList<String>()),
                    mapOf("id" to 1, "text" to "Java", "voterIds" to emptyList<String>())))

    every { mockSnapshot.children } returns listOf(mockPollSnapshot1, mockPollSnapshot2)
    every { mockPollsRef.orderByChild("createdAt") } returns mockQuery
    every { mockQuery.ref } returns mockPollsRef
    every { mockPollsRef.path } returns mockk(relaxed = true)
    every { mockQuery.addValueEventListener(any()) } answers
        {
          val listener = firstArg<ValueEventListener>()
          listener.onDataChange(mockSnapshot)
          mockk(relaxed = true)
        }

    // When
    val flow = repository.observePollsForConversation(testConversationId)
    val polls = withTimeout(1000) { flow.first() }

    // Then
    assertNotNull(polls)
    assertEquals(2, polls.size)
    assertEquals("poll1", polls[0].id)
    assertEquals("What is your favorite color?", polls[0].question)
    assertEquals("poll2", polls[1].id)
    assertEquals("Best programming language?", polls[1].question)
  }

  @Test
  fun observePollsForConversation_returnsEmptyListWhenNoPolls() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)

    every { mockSnapshot.children } returns emptyList()
    every { mockPollsRef.orderByChild("createdAt") } returns mockQuery
    every { mockQuery.ref } returns mockPollsRef
    every { mockPollsRef.path } returns mockk(relaxed = true)
    every { mockQuery.addValueEventListener(any()) } answers
        {
          val listener = firstArg<ValueEventListener>()
          listener.onDataChange(mockSnapshot)
          mockk(relaxed = true)
        }

    // When
    val flow = repository.observePollsForConversation(testConversationId)
    val polls = withTimeout(1000) { flow.first() }

    // Then
    assertNotNull(polls)
    assertEquals(0, polls.size)
  }

  @Test
  fun observePollsForConversation_handlesError() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockError = mockk<DatabaseError>(relaxed = true)

    every { mockPollsRef.orderByChild("createdAt") } returns mockQuery
    every { mockQuery.ref } returns mockPollsRef
    every { mockPollsRef.path } returns mockk(relaxed = true)
    every { mockError.toException() } returns DatabaseException("Database error")
    every { mockQuery.addValueEventListener(any()) } answers
        {
          val listener = firstArg<ValueEventListener>()
          listener.onCancelled(mockError)
          mockk(relaxed = true)
        }

    // When
    val flow = repository.observePollsForConversation(testConversationId)
    val polls = withTimeout(1000) { flow.first() }

    // Then - should return empty list on error
    assertNotNull(polls)
    assertEquals(0, polls.size)
  }

  @Test
  fun observePollsForConversation_filtersOutInvalidPolls() = runTest {
    // Given
    val mockQuery = mockk<Query>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockValidPoll =
        createMockPollSnapshot(
            pollId = "poll1",
            question = "Valid poll?",
            creatorId = "user1",
            creatorName = "Alice",
            options =
                listOf(
                    mapOf("id" to 0, "text" to "Yes", "voterIds" to emptyList<String>()),
                    mapOf("id" to 1, "text" to "No", "voterIds" to emptyList<String>())))
    val mockInvalidPoll = mockk<DataSnapshot>(relaxed = true)

    // Invalid poll (missing question)
    every { mockInvalidPoll.key } returns "poll2"
    every { mockInvalidPoll.child("creatorId").getValue(String::class.java) } returns "user2"
    every { mockInvalidPoll.child("creatorName").getValue(String::class.java) } returns "Bob"
    every { mockInvalidPoll.child("question").getValue(String::class.java) } returns null
    every { mockInvalidPoll.child("createdAt").getValue(Long::class.java) } returns 2000L

    every { mockSnapshot.children } returns listOf(mockValidPoll, mockInvalidPoll)
    every { mockPollsRef.orderByChild("createdAt") } returns mockQuery
    every { mockQuery.ref } returns mockPollsRef
    every { mockPollsRef.path } returns mockk(relaxed = true)
    every { mockQuery.addValueEventListener(any()) } answers
        {
          val listener = firstArg<ValueEventListener>()
          listener.onDataChange(mockSnapshot)
          mockk(relaxed = true)
        }

    // When
    val flow = repository.observePollsForConversation(testConversationId)
    val polls = withTimeout(1000) { flow.first() }

    // Then - should only return valid poll
    assertNotNull(polls)
    assertEquals(1, polls.size)
    assertEquals("poll1", polls[0].id)
  }

  // ============================================================================
  // getPoll Tests
  // ============================================================================

  @Test
  fun getPoll_returnsExistingPoll() = runTest {
    // Given
    val mockSnapshot =
        createMockPollSnapshot(
            pollId = testPollId,
            question = "Test question?",
            creatorId = "user1",
            creatorName = "Alice",
            options =
                listOf(
                    mapOf("id" to 0, "text" to "Option A", "voterIds" to emptyList<String>()),
                    mapOf("id" to 1, "text" to "Option B", "voterIds" to emptyList<String>())))
    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockPollRef.get() } returns mockTask

    // When
    val poll = repository.getPoll(testConversationId, testPollId)

    // Then
    assertNotNull(poll)
    assertEquals(testPollId, poll?.id)
    assertEquals("Test question?", poll?.question)
  }

  @Test
  fun getPoll_returnsNullForNonExistent() = runTest {
    // Given
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    every { mockSnapshot.key } returns null
    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockPollRef.get() } returns mockTask

    // When
    val poll = repository.getPoll(testConversationId, "nonexistent")

    // Then
    assertNull(poll)
  }

  // ============================================================================
  // createPoll Tests
  // ============================================================================

  @Test
  fun createPoll_successfullySavesPoll() = runTest {
    // Given
    val poll =
        Poll(
            id = testPollId,
            conversationId = testConversationId,
            creatorId = "user1",
            creatorName = "Alice",
            question = "Test question?",
            options =
                listOf(
                    PollOption(id = 0, text = "Option A"), PollOption(id = 1, text = "Option B")),
            isAnonymous = false,
            allowMultipleAnswers = false,
            isClosed = false,
            createdAt = System.currentTimeMillis())

    val mockTask = Tasks.forResult<Void>(null)
    every { mockPollRef.setValue(any()) } returns mockTask
    every { mockPollRef.path } returns mockk(relaxed = true)

    // When
    repository.createPoll(poll)

    // Then
    verify { mockPollRef.setValue(any()) }
  }

  // ============================================================================
  // vote Tests
  // ============================================================================

  @Test
  fun vote_addsVoteToOption() = runTest {
    // Given
    val mockSnapshot =
        createMockPollSnapshot(
            pollId = testPollId,
            question = "Test question?",
            creatorId = "user1",
            creatorName = "Alice",
            options =
                listOf(
                    mapOf("id" to 0, "text" to "Option A", "voterIds" to emptyList<String>()),
                    mapOf("id" to 1, "text" to "Option B", "voterIds" to emptyList<String>())),
            isClosed = false)
    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockSetTask = Tasks.forResult<Void>(null)
    val mockOptionsRef = mockk<DatabaseReference>(relaxed = true)

    every { mockPollRef.get() } returns mockGetTask
    every { mockPollRef.child("options") } returns mockOptionsRef
    every { mockOptionsRef.setValue(any()) } returns mockSetTask

    // When
    repository.vote(testConversationId, testPollId, 0, "user2")

    // Then
    verify { mockOptionsRef.setValue(any()) }
  }

  @Test(expected = Exception::class)
  fun vote_throwsExceptionWhenPollNotFound() = runTest {
    // Given
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    every { mockSnapshot.key } returns null
    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockPollRef.get() } returns mockTask

    // When - should throw Exception
    repository.vote(testConversationId, testPollId, 0, "user1")
  }

  @Test(expected = Exception::class)
  fun vote_throwsExceptionWhenPollIsClosed() = runTest {
    // Given
    val mockSnapshot =
        createMockPollSnapshot(
            pollId = testPollId,
            question = "Test question?",
            creatorId = "user1",
            creatorName = "Alice",
            options =
                listOf(mapOf("id" to 0, "text" to "Option A", "voterIds" to emptyList<String>())),
            isClosed = true)
    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockPollRef.get() } returns mockTask

    // When - should throw Exception
    repository.vote(testConversationId, testPollId, 0, "user2")
  }

  // ============================================================================
  // removeVote Tests
  // ============================================================================

  @Test
  fun removeVote_removesVoteFromOption() = runTest {
    // Given
    val mockSnapshot =
        createMockPollSnapshot(
            pollId = testPollId,
            question = "Test question?",
            creatorId = "user1",
            creatorName = "Alice",
            options =
                listOf(
                    mapOf("id" to 0, "text" to "Option A", "voterIds" to listOf("user2")),
                    mapOf("id" to 1, "text" to "Option B", "voterIds" to emptyList<String>())),
            isClosed = false)
    val mockGetTask = Tasks.forResult(mockSnapshot)
    val mockSetTask = Tasks.forResult<Void>(null)
    val mockOptionsRef = mockk<DatabaseReference>(relaxed = true)

    every { mockPollRef.get() } returns mockGetTask
    every { mockPollRef.child("options") } returns mockOptionsRef
    every { mockOptionsRef.setValue(any()) } returns mockSetTask

    // When
    repository.removeVote(testConversationId, testPollId, 0, "user2")

    // Then
    verify { mockOptionsRef.setValue(any()) }
  }

  @Test(expected = Exception::class)
  fun removeVote_throwsExceptionWhenPollIsClosed() = runTest {
    // Given
    val mockSnapshot =
        createMockPollSnapshot(
            pollId = testPollId,
            question = "Test question?",
            creatorId = "user1",
            creatorName = "Alice",
            options = listOf(mapOf("id" to 0, "text" to "Option A", "voterIds" to listOf("user2"))),
            isClosed = true)
    val mockTask = Tasks.forResult(mockSnapshot)
    every { mockPollRef.get() } returns mockTask

    // When - should throw Exception
    repository.removeVote(testConversationId, testPollId, 0, "user2")
  }

  // ============================================================================
  // closePoll Tests
  // ============================================================================

  @Test
  fun closePoll_updatesClosedStatus() = runTest {
    // Given
    val mockTask = Tasks.forResult<Void>(null)
    every { mockPollRef.updateChildren(any()) } returns mockTask

    // When
    repository.closePoll(testConversationId, testPollId)

    // Then
    verify {
      mockPollRef.updateChildren(
          match { updates -> updates["isClosed"] == true && updates.containsKey("closedAt") })
    }
  }

  // ============================================================================
  // reopenPoll Tests
  // ============================================================================

  @Test
  fun reopenPoll_clearsClosedStatus() = runTest {
    // Given
    val mockTask = Tasks.forResult<Void>(null)
    every { mockPollRef.updateChildren(any()) } returns mockTask

    // When
    repository.reopenPoll(testConversationId, testPollId)

    // Then
    verify {
      mockPollRef.updateChildren(
          match { updates -> updates["isClosed"] == false && updates["closedAt"] == null })
    }
  }

  // ============================================================================
  // deletePoll Tests
  // ============================================================================

  @Test
  fun deletePoll_removesFromDatabase() = runTest {
    // Given
    val mockTask = Tasks.forResult<Void>(null)
    every { mockPollRef.removeValue() } returns mockTask

    // When
    repository.deletePoll(testConversationId, testPollId)

    // Then
    verify { mockPollRef.removeValue() }
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  private fun createMockPollSnapshot(
      pollId: String,
      question: String,
      creatorId: String,
      creatorName: String,
      options: List<Map<String, Any>>,
      isAnonymous: Boolean = false,
      allowMultipleAnswers: Boolean = false,
      isClosed: Boolean = false,
      createdAt: Long = 1000L,
      closedAt: Long? = null
  ): DataSnapshot {
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)

    every { mockSnapshot.key } returns pollId
    every { mockSnapshot.child("creatorId").getValue(String::class.java) } returns creatorId
    every { mockSnapshot.child("creatorName").getValue(String::class.java) } returns creatorName
    every { mockSnapshot.child("question").getValue(String::class.java) } returns question
    every { mockSnapshot.child("createdAt").getValue(Long::class.java) } returns createdAt
    every { mockSnapshot.child("isAnonymous").getValue(Boolean::class.java) } returns isAnonymous
    every { mockSnapshot.child("allowMultipleAnswers").getValue(Boolean::class.java) } returns
        allowMultipleAnswers
    every { mockSnapshot.child("isClosed").getValue(Boolean::class.java) } returns isClosed
    every { mockSnapshot.child("closedAt").getValue(Long::class.java) } returns closedAt

    // Mock options
    val optionSnapshots =
        options.mapIndexed { index, optionMap ->
          val optionSnapshot = mockk<DataSnapshot>(relaxed = true)
          every { optionSnapshot.key } returns index.toString()
          every { optionSnapshot.child("id").getValue(Int::class.java) } returns
              optionMap["id"] as Int
          every { optionSnapshot.child("text").getValue(String::class.java) } returns
              optionMap["text"] as String
          @Suppress("UNCHECKED_CAST")
          every {
            optionSnapshot.child("voterIds").getValue(any<GenericTypeIndicator<List<String>>>())
          } returns (optionMap["voterIds"] as List<String>)
          optionSnapshot
        }
    every { mockSnapshot.child("options").children } returns optionSnapshots

    return mockSnapshot
  }
}
