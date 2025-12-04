package com.android.joinme.model.presence

// Implemented with help of Claude AI

import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PresenceRepositoryRealtimeDatabase.
 *
 * Tests the Firebase Realtime Database implementation of presence tracking including setting users
 * online/offline, observing online users, and cleaning up stale presence data.
 */
class PresenceRepositoryRealtimeDatabaseTest {

  private lateinit var mockDatabase: FirebaseDatabase
  private lateinit var mockPresenceRef: DatabaseReference
  private lateinit var mockUserContextsRef: DatabaseReference
  private lateinit var repository: PresenceRepositoryRealtimeDatabase

  private val testUserId = "user123"
  private val testContextId = "context456"

  @Before
  fun setup() {
    mockDatabase = mockk(relaxed = true)
    mockPresenceRef = mockk(relaxed = true)
    mockUserContextsRef = mockk(relaxed = true)

    every { mockDatabase.getReference("presence") } returns mockPresenceRef
    every { mockDatabase.getReference("userContexts") } returns mockUserContextsRef

    repository = PresenceRepositoryRealtimeDatabase(mockDatabase)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  // ==================== SET USER ONLINE TESTS ====================

  @Test
  fun `setUserOnline sets up onDisconnect handlers and presence data`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnlineRef = mockk<DatabaseReference>(relaxed = true)
    val mockLastSeenRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnDisconnect = mockk<com.google.firebase.database.OnDisconnect>(relaxed = true)
    val mockUserContextsUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserContextsContextRef = mockk<DatabaseReference>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef
    every { mockContextRef.child(testUserId) } returns mockUserRef
    every { mockUserRef.child("online") } returns mockOnlineRef
    every { mockUserRef.child("lastSeen") } returns mockLastSeenRef
    every { mockOnlineRef.onDisconnect() } returns mockOnDisconnect
    every { mockLastSeenRef.onDisconnect() } returns mockOnDisconnect
    every { mockOnDisconnect.setValue(any()) } returns Tasks.forResult(null)
    every { mockUserRef.setValue(any()) } returns Tasks.forResult(null)

    // Mock userContexts index
    every { mockUserContextsRef.child(testUserId) } returns mockUserContextsUserRef
    every { mockUserContextsUserRef.child(testContextId) } returns mockUserContextsContextRef
    every { mockUserContextsContextRef.setValue(true) } returns Tasks.forResult(null)

    // When
    repository.setUserOnline(testUserId, listOf(testContextId))

    // Then
    verify { mockOnlineRef.onDisconnect() }
    verify { mockLastSeenRef.onDisconnect() }
    verify { mockUserRef.setValue(any()) }
  }

  @Test
  fun `setUserOnline handles multiple contexts`() = runTest {
    // Given
    val contextIds = listOf("context1", "context2", "context3")
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnlineRef = mockk<DatabaseReference>(relaxed = true)
    val mockLastSeenRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnDisconnect = mockk<com.google.firebase.database.OnDisconnect>(relaxed = true)
    val mockUserContextsUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserContextsContextRef = mockk<DatabaseReference>(relaxed = true)

    contextIds.forEach { contextId ->
      every { mockPresenceRef.child(contextId) } returns mockContextRef
    }
    every { mockContextRef.child(testUserId) } returns mockUserRef
    every { mockUserRef.child("online") } returns mockOnlineRef
    every { mockUserRef.child("lastSeen") } returns mockLastSeenRef
    every { mockOnlineRef.onDisconnect() } returns mockOnDisconnect
    every { mockLastSeenRef.onDisconnect() } returns mockOnDisconnect
    every { mockOnDisconnect.setValue(any()) } returns Tasks.forResult(null)
    every { mockUserRef.setValue(any()) } returns Tasks.forResult(null)

    // Mock userContexts index for all contexts
    every { mockUserContextsRef.child(testUserId) } returns mockUserContextsUserRef
    contextIds.forEach { contextId ->
      every { mockUserContextsUserRef.child(contextId) } returns mockUserContextsContextRef
    }
    every { mockUserContextsContextRef.setValue(true) } returns Tasks.forResult(null)

    // When
    repository.setUserOnline(testUserId, contextIds)

    // Then
    verify(exactly = 3) { mockUserRef.setValue(any()) }
    verify(exactly = 3) { mockUserContextsContextRef.setValue(true) }
  }

  @Test
  fun `setUserOnline with blank userId returns early`() = runTest {
    // When
    repository.setUserOnline("", emptyList())
    repository.setUserOnline("   ", listOf("context1"))

    // Then - no presence operations attempted for blank userId
    verify(exactly = 0) { mockPresenceRef.child(any()) }
  }

  @Test
  fun `setUserOnline skips blank contextIds`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnlineRef = mockk<DatabaseReference>(relaxed = true)
    val mockLastSeenRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnDisconnect = mockk<com.google.firebase.database.OnDisconnect>(relaxed = true)
    val mockUserContextsUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserContextsContextRef = mockk<DatabaseReference>(relaxed = true)

    every { mockPresenceRef.child("validContext") } returns mockContextRef
    every { mockContextRef.child(testUserId) } returns mockUserRef
    every { mockUserRef.child("online") } returns mockOnlineRef
    every { mockUserRef.child("lastSeen") } returns mockLastSeenRef
    every { mockOnlineRef.onDisconnect() } returns mockOnDisconnect
    every { mockLastSeenRef.onDisconnect() } returns mockOnDisconnect
    every { mockOnDisconnect.setValue(any()) } returns Tasks.forResult(null)
    every { mockUserRef.setValue(any()) } returns Tasks.forResult(null)
    every { mockUserContextsRef.child(testUserId) } returns mockUserContextsUserRef
    every { mockUserContextsUserRef.child("validContext") } returns mockUserContextsContextRef
    every { mockUserContextsContextRef.setValue(true) } returns Tasks.forResult(null)

    // When - include blank context IDs which should be skipped
    repository.setUserOnline(testUserId, listOf("", "   ", "validContext"))

    // Then - only validContext should be processed
    verify(exactly = 1) { mockPresenceRef.child("validContext") }
    verify(exactly = 0) { mockPresenceRef.child("") }
    verify(exactly = 0) { mockPresenceRef.child("   ") }
  }

  @Test
  fun `setUserOnline handles Firebase exception gracefully`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnlineRef = mockk<DatabaseReference>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef
    every { mockContextRef.child(testUserId) } returns mockUserRef
    every { mockUserRef.child("online") } returns mockOnlineRef
    every { mockOnlineRef.onDisconnect() } throws RuntimeException("Firebase error")

    // When - should not throw
    repository.setUserOnline(testUserId, listOf(testContextId))

    // Then - exception is handled gracefully (logged)
  }

  @Test
  fun `setUserOnline also updates userContexts index`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnlineRef = mockk<DatabaseReference>(relaxed = true)
    val mockLastSeenRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnDisconnect = mockk<com.google.firebase.database.OnDisconnect>(relaxed = true)
    val mockUserContextsUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserContextsContextRef = mockk<DatabaseReference>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef
    every { mockContextRef.child(testUserId) } returns mockUserRef
    every { mockUserRef.child("online") } returns mockOnlineRef
    every { mockUserRef.child("lastSeen") } returns mockLastSeenRef
    every { mockOnlineRef.onDisconnect() } returns mockOnDisconnect
    every { mockLastSeenRef.onDisconnect() } returns mockOnDisconnect
    every { mockOnDisconnect.setValue(any()) } returns Tasks.forResult(null)
    every { mockUserRef.setValue(any()) } returns Tasks.forResult(null)

    // Mock userContexts index
    every { mockUserContextsRef.child(testUserId) } returns mockUserContextsUserRef
    every { mockUserContextsUserRef.child(testContextId) } returns mockUserContextsContextRef
    every { mockUserContextsContextRef.setValue(true) } returns Tasks.forResult(null)

    // When
    repository.setUserOnline(testUserId, listOf(testContextId))

    // Then
    verify { mockUserContextsContextRef.setValue(true) }
  }

  // ==================== SET USER OFFLINE TESTS ====================

  @Test
  fun `setUserOffline updates presence to offline for all contexts using userContexts index`() =
      runTest {
        // Given
        val mockUserContextsSnapshot = mockk<DataSnapshot>(relaxed = true)
        val mockContextSnapshot = mockk<DataSnapshot>(relaxed = true)
        val mockUserContextsUserRef = mockk<DatabaseReference>(relaxed = true)
        val mockUserContextsContextRef = mockk<DatabaseReference>(relaxed = true)
        val mockContextRef = mockk<DatabaseReference>(relaxed = true)
        val mockUserRef = mockk<DatabaseReference>(relaxed = true)
        val mockOnDisconnect = mockk<com.google.firebase.database.OnDisconnect>(relaxed = true)

        // Mock userContexts index lookup (optimized path)
        every { mockUserContextsRef.child(testUserId) } returns mockUserContextsUserRef
        every { mockUserContextsUserRef.get() } returns Tasks.forResult(mockUserContextsSnapshot)
        every { mockUserContextsSnapshot.children } returns listOf(mockContextSnapshot)
        every { mockContextSnapshot.key } returns testContextId

        // Mock presence update
        every { mockPresenceRef.child(testContextId) } returns mockContextRef
        every { mockContextRef.child(testUserId) } returns mockUserRef
        every { mockUserRef.onDisconnect() } returns mockOnDisconnect
        every { mockOnDisconnect.cancel() } returns Tasks.forResult(null)
        every { mockUserRef.updateChildren(any()) } returns Tasks.forResult(null)

        // Mock userContexts entry removal
        every { mockUserContextsUserRef.child(testContextId) } returns mockUserContextsContextRef
        every { mockUserContextsContextRef.removeValue() } returns Tasks.forResult(null)

        // When
        repository.setUserOffline(testUserId)

        // Then
        verify { mockUserContextsUserRef.get() } // Verify it reads from userContexts index
        verify { mockOnDisconnect.cancel() }
        verify { mockUserRef.updateChildren(any()) }
        verify { mockUserContextsContextRef.removeValue() } // Verify entry is removed
  }

  @Test
  fun `setUserOffline with no contexts in index does nothing`() = runTest {
    // Given
    val mockUserContextsSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUserContextsUserRef = mockk<DatabaseReference>(relaxed = true)

    every { mockUserContextsRef.child(testUserId) } returns mockUserContextsUserRef
    every { mockUserContextsUserRef.get() } returns Tasks.forResult(mockUserContextsSnapshot)
    every { mockUserContextsSnapshot.children } returns emptyList()

    // When
    repository.setUserOffline(testUserId)

    // Then - no presence updates attempted
    verify(exactly = 0) { mockPresenceRef.child(any()).child(any()) }
  }

  @Test
  fun `setUserOffline handles database error gracefully`() = runTest {
    // Given
    val mockUserContextsUserRef = mockk<DatabaseReference>(relaxed = true)

    every { mockUserContextsRef.child(testUserId) } returns mockUserContextsUserRef
    every { mockUserContextsUserRef.get() } returns Tasks.forException(Exception("Database error"))

    // When - should not throw
    repository.setUserOffline(testUserId)

    // Then - no exception thrown, method completes (error is logged)
  }

  @Test
  fun `setUserOffline with blank userId returns early`() = runTest {
    // When
    repository.setUserOffline("")
    repository.setUserOffline("   ")

    // Then - no database operations attempted
    verify(exactly = 0) { mockUserContextsRef.child(any()) }
  }

  @Test
  fun `setUserOffline skips contexts with null key`() = runTest {
    // Given
    val mockUserContextsSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockContextSnapshot1 = mockk<DataSnapshot>(relaxed = true)
    val mockContextSnapshot2 = mockk<DataSnapshot>(relaxed = true)
    val mockUserContextsUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserContextsContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnDisconnect = mockk<com.google.firebase.database.OnDisconnect>(relaxed = true)

    every { mockUserContextsRef.child(testUserId) } returns mockUserContextsUserRef
    every { mockUserContextsUserRef.get() } returns Tasks.forResult(mockUserContextsSnapshot)
    every { mockUserContextsSnapshot.children } returns
        listOf(mockContextSnapshot1, mockContextSnapshot2)
    every { mockContextSnapshot1.key } returns null // null key should be skipped
    every { mockContextSnapshot2.key } returns testContextId

    every { mockPresenceRef.child(testContextId) } returns mockContextRef
    every { mockContextRef.child(testUserId) } returns mockUserRef
    every { mockUserRef.onDisconnect() } returns mockOnDisconnect
    every { mockOnDisconnect.cancel() } returns Tasks.forResult(null)
    every { mockUserRef.updateChildren(any()) } returns Tasks.forResult(null)

    // Mock userContexts entry removal
    every { mockUserContextsUserRef.child(testContextId) } returns mockUserContextsContextRef
    every { mockUserContextsContextRef.removeValue() } returns Tasks.forResult(null)

    // When
    repository.setUserOffline(testUserId)

    // Then - only valid context should be processed
    verify(exactly = 1) { mockPresenceRef.child(testContextId) }
  }

  @Test
  fun `setUserOffline handles per-context error gracefully`() = runTest {
    // Given
    val mockUserContextsSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockContextSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUserContextsUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserRef = mockk<DatabaseReference>(relaxed = true)

    every { mockUserContextsRef.child(testUserId) } returns mockUserContextsUserRef
    every { mockUserContextsUserRef.get() } returns Tasks.forResult(mockUserContextsSnapshot)
    every { mockUserContextsSnapshot.children } returns listOf(mockContextSnapshot)
    every { mockContextSnapshot.key } returns testContextId

    every { mockPresenceRef.child(testContextId) } returns mockContextRef
    every { mockContextRef.child(testUserId) } returns mockUserRef
    every { mockUserRef.onDisconnect() } throws RuntimeException("Per-context error")

    // When - should not throw
    repository.setUserOffline(testUserId)

    // Then - exception is handled gracefully (logged)
  }

  // ==================== OBSERVE ONLINE USERS COUNT TESTS ====================

  @Test
  fun `observeOnlineUsersCount returns count of online users excluding current user`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUser1Snapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUser2Snapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUser3Snapshot = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild1 = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild2 = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild3 = mockk<DataSnapshot>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef

    // Setup three users: user1 (online), user2 (online, current user), user3 (offline)
    every { mockUser1Snapshot.key } returns "user1"
    every { mockUser1Snapshot.child("online") } returns mockOnlineChild1
    every { mockOnlineChild1.getValue(Boolean::class.java) } returns true

    every { mockUser2Snapshot.key } returns testUserId // Current user
    every { mockUser2Snapshot.child("online") } returns mockOnlineChild2
    every { mockOnlineChild2.getValue(Boolean::class.java) } returns true

    every { mockUser3Snapshot.key } returns "user3"
    every { mockUser3Snapshot.child("online") } returns mockOnlineChild3
    every { mockOnlineChild3.getValue(Boolean::class.java) } returns false

    every { mockSnapshot.children } returns
        listOf(mockUser1Snapshot, mockUser2Snapshot, mockUser3Snapshot)

    // Capture the listener and trigger it
    val listenerSlot = slot<ValueEventListener>()
    every { mockContextRef.addValueEventListener(capture(listenerSlot)) } answers
        {
          listenerSlot.captured.onDataChange(mockSnapshot)
          mockk()
        }

    // When
    val count = repository.observeOnlineUsersCount(testContextId, testUserId).first()

    // Then
    assertEquals(1, count) // Only user1 is online and not current user
  }

  @Test
  fun `observeOnlineUsersCount returns zero for empty context`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef
    every { mockSnapshot.children } returns emptyList()

    val listenerSlot = slot<ValueEventListener>()
    every { mockContextRef.addValueEventListener(capture(listenerSlot)) } answers
        {
          listenerSlot.captured.onDataChange(mockSnapshot)
          mockk()
        }

    // When
    val count = repository.observeOnlineUsersCount(testContextId, testUserId).first()

    // Then
    assertEquals(0, count)
  }

  @Test
  fun `observeOnlineUsersCount returns zero for blank contextId`() = runTest {
    // When
    val count = repository.observeOnlineUsersCount("", testUserId).first()

    // Then
    assertEquals(0, count)
    verify(exactly = 0) { mockPresenceRef.child(any()) }
  }

  @Test
  fun `observeOnlineUsersCount returns zero for blank currentUserId`() = runTest {
    // When
    val count = repository.observeOnlineUsersCount(testContextId, "").first()

    // Then
    assertEquals(0, count)
    verify(exactly = 0) { mockPresenceRef.child(any()) }
  }

  @Test
  fun `observeOnlineUsersCount skips users with null key`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUser1Snapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUser2Snapshot = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild1 = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild2 = mockk<DataSnapshot>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef

    // user1 has null key (should be skipped)
    every { mockUser1Snapshot.key } returns null
    every { mockUser1Snapshot.child("online") } returns mockOnlineChild1
    every { mockOnlineChild1.getValue(Boolean::class.java) } returns true

    // user2 is online
    every { mockUser2Snapshot.key } returns "user2"
    every { mockUser2Snapshot.child("online") } returns mockOnlineChild2
    every { mockOnlineChild2.getValue(Boolean::class.java) } returns true

    every { mockSnapshot.children } returns listOf(mockUser1Snapshot, mockUser2Snapshot)

    val listenerSlot = slot<ValueEventListener>()
    every { mockContextRef.addValueEventListener(capture(listenerSlot)) } answers
        {
          listenerSlot.captured.onDataChange(mockSnapshot)
          mockk()
        }

    // When
    val count = repository.observeOnlineUsersCount(testContextId, testUserId).first()

    // Then - only user2 counted (user1 skipped due to null key)
    assertEquals(1, count)
  }

  @Test
  fun `observeOnlineUsersCount handles onCancelled by returning zero`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockDatabaseError = mockk<com.google.firebase.database.DatabaseError>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef
    every { mockDatabaseError.toException() } returns
        mockk<com.google.firebase.database.DatabaseException>(relaxed = true)

    val listenerSlot = slot<ValueEventListener>()
    every { mockContextRef.addValueEventListener(capture(listenerSlot)) } answers
        {
          listenerSlot.captured.onCancelled(mockDatabaseError)
          mockk()
        }

    // When
    val count = repository.observeOnlineUsersCount(testContextId, testUserId).first()

    // Then
    assertEquals(0, count)
  }

  // ==================== OBSERVE ONLINE USER IDS TESTS ====================

  @Test
  fun `observeOnlineUserIds returns list of online user IDs excluding current user`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUser1Snapshot = mockk<DataSnapshot>(relaxed = true)
    val mockCurrentUserSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild1 = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild2 = mockk<DataSnapshot>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef

    // user1 is online (should be included)
    every { mockUser1Snapshot.key } returns "user1"
    every { mockUser1Snapshot.child("online") } returns mockOnlineChild1
    every { mockOnlineChild1.getValue(Boolean::class.java) } returns true

    // current user is online (should be excluded)
    every { mockCurrentUserSnapshot.key } returns testUserId
    every { mockCurrentUserSnapshot.child("online") } returns mockOnlineChild2
    every { mockOnlineChild2.getValue(Boolean::class.java) } returns true

    every { mockSnapshot.children } returns listOf(mockUser1Snapshot, mockCurrentUserSnapshot)

    val listenerSlot = slot<ValueEventListener>()
    every { mockContextRef.addValueEventListener(capture(listenerSlot)) } answers
        {
          listenerSlot.captured.onDataChange(mockSnapshot)
          mockk()
        }

    // When
    val userIds = repository.observeOnlineUserIds(testContextId, testUserId).first()

    // Then - only user1 should be included, current user excluded
    assertEquals(1, userIds.size)
    assertTrue(userIds.contains("user1"))
    assertFalse(userIds.contains(testUserId))
  }

  @Test
  fun `observeOnlineUserIds returns empty list when no users online`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef
    every { mockSnapshot.children } returns emptyList()

    val listenerSlot = slot<ValueEventListener>()
    every { mockContextRef.addValueEventListener(capture(listenerSlot)) } answers
        {
          listenerSlot.captured.onDataChange(mockSnapshot)
          mockk()
        }

    // When
    val userIds = repository.observeOnlineUserIds(testContextId, testUserId).first()

    // Then
    assertTrue(userIds.isEmpty())
  }

  @Test
  fun `observeOnlineUserIds returns empty list for blank contextId`() = runTest {
    // When
    val userIds = repository.observeOnlineUserIds("", testUserId).first()

    // Then
    assertTrue(userIds.isEmpty())
    verify(exactly = 0) { mockPresenceRef.child(any()) }
  }

  @Test
  fun `observeOnlineUserIds returns empty list for blank currentUserId`() = runTest {
    // When
    val userIds = repository.observeOnlineUserIds(testContextId, "").first()

    // Then
    assertTrue(userIds.isEmpty())
    verify(exactly = 0) { mockPresenceRef.child(any()) }
  }

  @Test
  fun `observeOnlineUserIds handles onCancelled by returning empty list`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockDatabaseError = mockk<com.google.firebase.database.DatabaseError>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef
    every { mockDatabaseError.toException() } returns
        mockk<com.google.firebase.database.DatabaseException>(relaxed = true)

    val listenerSlot = slot<ValueEventListener>()
    every { mockContextRef.addValueEventListener(capture(listenerSlot)) } answers
        {
          listenerSlot.captured.onCancelled(mockDatabaseError)
          mockk()
        }

    // When
    val userIds = repository.observeOnlineUserIds(testContextId, testUserId).first()

    // Then
    assertTrue(userIds.isEmpty())
  }

  @Test
  fun `observeOnlineUserIds skips users with null key`() = runTest {
    // Given
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUser1Snapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUser2Snapshot = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild1 = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild2 = mockk<DataSnapshot>(relaxed = true)

    every { mockPresenceRef.child(testContextId) } returns mockContextRef

    // user1 has null key (should be skipped)
    every { mockUser1Snapshot.key } returns null
    every { mockUser1Snapshot.child("online") } returns mockOnlineChild1
    every { mockOnlineChild1.getValue(Boolean::class.java) } returns true

    // user2 is online
    every { mockUser2Snapshot.key } returns "user2"
    every { mockUser2Snapshot.child("online") } returns mockOnlineChild2
    every { mockOnlineChild2.getValue(Boolean::class.java) } returns true

    every { mockSnapshot.children } returns listOf(mockUser1Snapshot, mockUser2Snapshot)

    val listenerSlot = slot<ValueEventListener>()
    every { mockContextRef.addValueEventListener(capture(listenerSlot)) } answers
        {
          listenerSlot.captured.onDataChange(mockSnapshot)
          mockk()
        }

    // When
    val userIds = repository.observeOnlineUserIds(testContextId, testUserId).first()

    // Then - only user2 included (user1 skipped due to null key)
    assertEquals(1, userIds.size)
    assertTrue(userIds.contains("user2"))
  }
}
