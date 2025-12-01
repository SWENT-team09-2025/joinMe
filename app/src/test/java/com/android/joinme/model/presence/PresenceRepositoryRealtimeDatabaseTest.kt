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

        // When
        repository.setUserOffline(testUserId)

        // Then
        verify { mockUserContextsUserRef.get() } // Verify it reads from userContexts index
        verify { mockOnDisconnect.cancel() }
        verify { mockUserRef.updateChildren(any()) }
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

  // ==================== CLEANUP STALE PRESENCE TESTS ====================

  @Test
  fun `cleanupStalePresence marks stale online users as offline`() = runTest {
    // Given
    val staleThreshold = 5 * 60 * 1000L // 5 minutes
    val currentTime = System.currentTimeMillis()
    val staleTime = currentTime - staleThreshold - 1000 // Older than threshold

    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockContextSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUserSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockLastSeenChild = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild = mockk<DataSnapshot>(relaxed = true)
    val mockContextRef = mockk<DatabaseReference>(relaxed = true)
    val mockUserRef = mockk<DatabaseReference>(relaxed = true)
    val mockOnlineRef = mockk<DatabaseReference>(relaxed = true)

    every { mockPresenceRef.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.children } returns listOf(mockContextSnapshot)
    every { mockContextSnapshot.key } returns testContextId
    every { mockContextSnapshot.children } returns listOf(mockUserSnapshot)
    every { mockUserSnapshot.key } returns "staleUser"
    every { mockUserSnapshot.child("lastSeen") } returns mockLastSeenChild
    every { mockLastSeenChild.getValue(Long::class.java) } returns staleTime
    every { mockUserSnapshot.child("online") } returns mockOnlineChild
    every { mockOnlineChild.getValue(Boolean::class.java) } returns true

    every { mockPresenceRef.child(testContextId) } returns mockContextRef
    every { mockContextRef.child("staleUser") } returns mockUserRef
    every { mockUserRef.child("online") } returns mockOnlineRef
    every { mockOnlineRef.setValue(false) } returns Tasks.forResult(null)

    // When
    repository.cleanupStalePresence(staleThreshold)

    // Then
    verify { mockOnlineRef.setValue(false) }
  }

  @Test
  fun `cleanupStalePresence does not affect recent users`() = runTest {
    // Given
    val staleThreshold = 5 * 60 * 1000L // 5 minutes
    val currentTime = System.currentTimeMillis()
    val recentTime = currentTime - 1000 // Very recent

    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockContextSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUserSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockLastSeenChild = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild = mockk<DataSnapshot>(relaxed = true)

    every { mockPresenceRef.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.children } returns listOf(mockContextSnapshot)
    every { mockContextSnapshot.key } returns testContextId
    every { mockContextSnapshot.children } returns listOf(mockUserSnapshot)
    every { mockUserSnapshot.key } returns "recentUser"
    every { mockUserSnapshot.child("lastSeen") } returns mockLastSeenChild
    every { mockLastSeenChild.getValue(Long::class.java) } returns recentTime
    every { mockUserSnapshot.child("online") } returns mockOnlineChild
    every { mockOnlineChild.getValue(Boolean::class.java) } returns true

    // When
    repository.cleanupStalePresence(staleThreshold)

    // Then
    verify(exactly = 0) {
      mockPresenceRef.child(testContextId).child("recentUser").child("online").setValue(false)
    }
  }

  @Test
  fun `cleanupStalePresence ignores already offline users`() = runTest {
    // Given
    val staleThreshold = 5 * 60 * 1000L
    val currentTime = System.currentTimeMillis()
    val staleTime = currentTime - staleThreshold - 1000

    val mockSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockContextSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockUserSnapshot = mockk<DataSnapshot>(relaxed = true)
    val mockLastSeenChild = mockk<DataSnapshot>(relaxed = true)
    val mockOnlineChild = mockk<DataSnapshot>(relaxed = true)

    every { mockPresenceRef.get() } returns Tasks.forResult(mockSnapshot)
    every { mockSnapshot.children } returns listOf(mockContextSnapshot)
    every { mockContextSnapshot.key } returns testContextId
    every { mockContextSnapshot.children } returns listOf(mockUserSnapshot)
    every { mockUserSnapshot.key } returns "offlineUser"
    every { mockUserSnapshot.child("lastSeen") } returns mockLastSeenChild
    every { mockLastSeenChild.getValue(Long::class.java) } returns staleTime
    every { mockUserSnapshot.child("online") } returns mockOnlineChild
    every { mockOnlineChild.getValue(Boolean::class.java) } returns false // Already offline

    // When
    repository.cleanupStalePresence(staleThreshold)

    // Then
    verify(exactly = 0) {
      mockPresenceRef.child(any()).child(any()).child("online").setValue(any())
    }
  }

  @Test
  fun `cleanupStalePresence handles database error gracefully`() = runTest {
    // Given
    every { mockPresenceRef.get() } returns Tasks.forException(Exception("Database error"))

    // When - should not throw
    repository.cleanupStalePresence(5 * 60 * 1000L)

    // Then - no exception thrown, method completes
  }
}
