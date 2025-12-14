package com.android.joinme.model.chat

import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test suite for ConversationCleanupService.
 *
 * Tests the cleanup functionality for conversations and their associated data (messages, polls,
 * images) when entities like events, groups, series, or user profiles are deleted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationCleanupServiceTest {

  private lateinit var mockDatabase: FirebaseDatabase
  private lateinit var mockStorage: FirebaseStorage

  private val testConversationId = "test-conversation-123"
  private val testUserId1 = "user1"
  private val testUserId2 = "user2"

  @Before
  fun setup() {
    // Mock Firebase instances
    mockkStatic(FirebaseDatabase::class)
    mockkStatic(FirebaseStorage::class)

    mockDatabase = mockk(relaxed = true)
    mockStorage = mockk(relaxed = true)

    // Setup Firebase instance returns
    every { FirebaseDatabase.getInstance() } returns mockDatabase
    every { FirebaseStorage.getInstance() } returns mockStorage
  }

  @After
  fun teardown() {
    unmockkAll()
    clearAllMocks()
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  /**
   * Sets up mock conversation reference chain for a given conversation ID.
   *
   * @return Triple of (conversationsRef, conversationRef, messagesRef)
   */
  private fun setupConversationReferences(
      conversationId: String
  ): Triple<DatabaseReference, DatabaseReference, DatabaseReference> {
    val mockConversationsRef = mockk<DatabaseReference>(relaxed = true)
    val mockConversationRef = mockk<DatabaseReference>(relaxed = true)
    val mockMessagesRef = mockk<DatabaseReference>(relaxed = true)

    every { mockDatabase.getReference("conversations") } returns mockConversationsRef
    every { mockConversationsRef.child(conversationId) } returns mockConversationRef
    every { mockConversationRef.child("messages") } returns mockMessagesRef

    return Triple(mockConversationsRef, mockConversationRef, mockMessagesRef)
  }

  /** Sets up an empty messages snapshot that returns no children. */
  private fun setupEmptyMessagesSnapshot(messagesRef: DatabaseReference): DataSnapshot {
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    every { messagesRef.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.children } returns emptyList()
    return mockSnapshot
  }

  /** Sets up mock storage reference. */
  private fun setupStorageReference(): StorageReference {
    val mockStorageRef = mockk<StorageReference>(relaxed = true)
    every { mockStorage.reference } returns mockStorageRef
    return mockStorageRef
  }

  /**
   * Sets up cleanup for a DM conversation by mocking the necessary references.
   *
   * @param conversationsRef The conversations reference to setup child on
   * @param conversationId The DM conversation ID
   * @return The mock conversation reference
   */
  private fun setupDMConversationCleanup(
      conversationsRef: DatabaseReference,
      conversationId: String
  ): DatabaseReference {
    val mockDmConvRef = mockk<DatabaseReference>(relaxed = true)
    val mockDmMessagesRef = mockk<DatabaseReference>(relaxed = true)
    val mockEmptySnapshot = mockk<DataSnapshot>(relaxed = true)

    every { conversationsRef.child(conversationId) } returns mockDmConvRef
    every { mockDmConvRef.child("messages") } returns mockDmMessagesRef
    every { mockDmMessagesRef.get() } returns Tasks.forResult(mockEmptySnapshot)
    every { mockEmptySnapshot.children } returns emptyList()
    every { mockDmConvRef.removeValue() } returns Tasks.forResult(null)

    return mockDmConvRef
  }

  // ============================================================================
  // cleanupConversation Tests
  // ============================================================================

  @Test
  fun cleanupConversation_deletesMessagesAndConversation() = runTest {
    // Given: A conversation with messages
    val (_, mockConversationRef, mockMessagesRef) = setupConversationReferences(testConversationId)
    setupEmptyMessagesSnapshot(mockMessagesRef)
    setupStorageReference()
    every { mockConversationRef.removeValue() } returns Tasks.forResult(null)

    // When
    ConversationCleanupService.cleanupConversation(testConversationId)

    // Then
    verify { mockMessagesRef.get() }
    verify { mockConversationRef.removeValue() }
  }

  @Test
  fun cleanupConversation_deletesImageMessages() = runTest {
    // Given: A conversation with an image message
    val (_, mockConversationRef, mockMessagesRef) = setupConversationReferences(testConversationId)

    val mockMessagesSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockMessageSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockTypeSnapshot = mockk<DataSnapshot>(relaxed = true)

    every { mockMessagesRef.get() } returns Tasks.forResult(mockMessagesSnapshot)
    every { mockMessagesSnapshot.children } returns listOf(mockMessageSnapshot)
    every { mockMessageSnapshot.key } returns "imageMessage1"
    every { mockMessageSnapshot.child("type") } returns mockTypeSnapshot
    every { mockTypeSnapshot.getValue(String::class.java) } returns MessageType.IMAGE.name

    // Setup storage chain
    val mockStorageRef = mockk<StorageReference>(relaxed = true)
    val mockConvStorageRef = mockk<StorageReference>(relaxed = true)
    val mockConvIdStorageRef = mockk<StorageReference>(relaxed = true)
    val mockImagesRef = mockk<StorageReference>(relaxed = true)
    val mockImageFileRef = mockk<StorageReference>(relaxed = true)

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child("conversations") } returns mockConvStorageRef
    every { mockConvStorageRef.child(testConversationId) } returns mockConvIdStorageRef
    every { mockConvIdStorageRef.child("images") } returns mockImagesRef
    every { mockImagesRef.child("imageMessage1.jpg") } returns mockImageFileRef
    every { mockImageFileRef.delete() } returns Tasks.forResult(null)
    every { mockConversationRef.removeValue() } returns Tasks.forResult(null)

    // When
    ConversationCleanupService.cleanupConversation(testConversationId)

    // Then
    verify { mockImageFileRef.delete() }
    verify { mockConversationRef.removeValue() }
  }

  @Test
  fun cleanupConversation_handlesErrorGracefully() = runTest {
    // Given: Database operation that fails
    val (_, mockConversationRef, mockMessagesRef) = setupConversationReferences(testConversationId)
    setupEmptyMessagesSnapshot(mockMessagesRef)
    setupStorageReference()
    every { mockConversationRef.removeValue() } returns
        Tasks.forException(Exception("Delete failed"))

    // When & Then
    try {
      ConversationCleanupService.cleanupConversation(testConversationId)
      fail("Expected exception to be thrown")
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("Failed to cleanup conversation"))
    }
  }

  // ============================================================================
  // cleanupUserConversations Tests
  // ============================================================================

  @Test
  fun cleanupUserConversations_findsDMConversations() = runTest {
    // Given: User has DM conversations
    val mockConversationsRef = mockk<DatabaseReference>(relaxed = true)
    val mockAllConversationsSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockDmConvSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockEventConvSnapshot = mockk<DataSnapshot>(relaxed = true)

    every { mockDatabase.getReference("conversations") } returns mockConversationsRef
    every { mockConversationsRef.get() } returns Tasks.forResult(mockAllConversationsSnapshot)
    every { mockAllConversationsSnapshot.children } returns
        listOf(mockDmConvSnapshot, mockEventConvSnapshot)
    every { mockDmConvSnapshot.key } returns "dm_${testUserId1}_${testUserId2}"
    every { mockEventConvSnapshot.key } returns "event-123"

    val mockDmConvRef =
        setupDMConversationCleanup(mockConversationsRef, "dm_${testUserId1}_${testUserId2}")
    setupStorageReference()

    // When
    ConversationCleanupService.cleanupUserConversations(testUserId1)

    // Then
    verify { mockDmConvRef.removeValue() }
  }

  @Test
  fun cleanupUserConversations_ignoresNonDMConversations() = runTest {
    // Given: Only non-DM conversations exist
    val mockConversationsRef = mockk<DatabaseReference>(relaxed = true)
    val mockAllConversationsSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockEventConvSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockGroupConvSnapshot = mockk<DataSnapshot>(relaxed = true)

    every { mockDatabase.getReference("conversations") } returns mockConversationsRef
    every { mockConversationsRef.get() } returns Tasks.forResult(mockAllConversationsSnapshot)
    every { mockAllConversationsSnapshot.children } returns
        listOf(mockEventConvSnapshot, mockGroupConvSnapshot)
    every { mockEventConvSnapshot.key } returns "event-123"
    every { mockGroupConvSnapshot.key } returns "group-456"

    val mockStorageRef = mockk<StorageReference>(relaxed = true)
    every { mockStorage.reference } returns mockStorageRef

    // When
    ConversationCleanupService.cleanupUserConversations(testUserId1)

    // Then: Should complete successfully and only query for conversations
    verify(exactly = 1) { mockConversationsRef.get() }
    // Verify that no child references were created for DM conversations
    verify(exactly = 0) {
      mockConversationsRef.child(match { it.startsWith("dm_") && it.contains(testUserId1) })
    }
  }

  @Test
  fun cleanupUserConversations_handlesBothUserIdPositions() = runTest {
    // Given: User appears in both positions of DM ID
    val mockConversationsRef = mockk<DatabaseReference>(relaxed = true)
    val mockAllConversationsSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockDm1Snapshot = mockk<DataSnapshot>(relaxed = true)
    val mockDm2Snapshot = mockk<DataSnapshot>(relaxed = true)

    every { mockDatabase.getReference("conversations") } returns mockConversationsRef
    every { mockConversationsRef.get() } returns Tasks.forResult(mockAllConversationsSnapshot)
    every { mockAllConversationsSnapshot.children } returns listOf(mockDm1Snapshot, mockDm2Snapshot)
    every { mockDm1Snapshot.key } returns "dm_${testUserId1}_${testUserId2}"
    every { mockDm2Snapshot.key } returns "dm_${testUserId2}_${testUserId1}"

    val mockDm1Ref =
        setupDMConversationCleanup(mockConversationsRef, "dm_${testUserId1}_${testUserId2}")
    val mockDm2Ref =
        setupDMConversationCleanup(mockConversationsRef, "dm_${testUserId2}_${testUserId1}")
    setupStorageReference()

    // When
    ConversationCleanupService.cleanupUserConversations(testUserId1)

    // Then
    verify { mockDm1Ref.removeValue() }
    verify { mockDm2Ref.removeValue() }
  }

  @Test
  fun cleanupUserConversations_throwsExceptionOnDatabaseError() = runTest {
    // Given: Database query fails
    val mockConversationsRef = mockk<DatabaseReference>(relaxed = true)

    every { mockDatabase.getReference("conversations") } returns mockConversationsRef
    every { mockConversationsRef.get() } returns Tasks.forException(Exception("Database error"))

    // When & Then
    try {
      ConversationCleanupService.cleanupUserConversations(testUserId1)
      fail("Expected exception to be thrown")
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("Failed to cleanup conversations for user"))
      assertTrue(e.message!!.contains(testUserId1))
    }
  }
}
