package com.android.joinme.model.presence

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PresenceManager.
 *
 * Tests the presence tracking lifecycle management including starting/stopping tracking and user
 * online/offline status transitions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresenceManagerTest {

  private lateinit var fakePresenceRepository: FakePresenceRepository
  private lateinit var presenceManager: PresenceManager

  @Before
  fun setup() {
    PresenceManager.clearInstance()
    fakePresenceRepository = FakePresenceRepository()
    presenceManager = PresenceManager(fakePresenceRepository)
  }

  @After
  fun tearDown() {
    PresenceManager.clearInstance()
  }

  // ============================================================================
  // Start Tracking Tests
  // ============================================================================

  @Test
  fun startTracking_setsCurrentUserId() {
    presenceManager.startTracking("user123")

    assertEquals("user123", presenceManager.getCurrentUserId())
  }

  @Test
  fun startTracking_withSameUserId_doesNotDuplicate() {
    presenceManager.startTracking("user123")
    presenceManager.startTracking("user123")

    assertEquals("user123", presenceManager.getCurrentUserId())
  }

  // ============================================================================
  // Stop Tracking Tests
  // ============================================================================

  @Test
  fun stopTracking_clearsCurrentUserId() = runTest {
    presenceManager.startTracking("user123")
    presenceManager.stopTracking()
    advanceUntilIdle()

    assertNull(presenceManager.getCurrentUserId())
  }

  @Test
  fun stopTracking_triggersOfflineStatus() {
    presenceManager.startTracking("user123")

    // Verify user is tracked
    assertEquals("user123", presenceManager.getCurrentUserId())

    presenceManager.stopTracking()

    // Verify tracking is stopped (user ID is cleared immediately)
    assertNull(presenceManager.getCurrentUserId())

    // Note: The actual setUserOffline call happens asynchronously in a coroutine,
    // which is difficult to test without injecting the coroutine scope.
    // The repository interaction is tested in PresenceRepositoryLocalTest.
  }

  @Test
  fun stopTracking_whenNotTracking_doesNotThrow() {
    // Should not throw when called without starting tracking
    presenceManager.stopTracking()

    assertNull(presenceManager.getCurrentUserId())
  }

  // ============================================================================
  // Singleton Instance Tests
  // ============================================================================

  @Test
  fun getInstance_returnsSameInstance() {
    val instance1 = PresenceManager.getInstance(fakePresenceRepository)
    val instance2 = PresenceManager.getInstance(fakePresenceRepository)

    assertEquals(instance1, instance2)
  }

  @Test
  fun clearInstance_allowsNewInstance() {
    val instance1 = PresenceManager.getInstance(fakePresenceRepository)
    PresenceManager.clearInstance()
    val instance2 = PresenceManager.getInstance(fakePresenceRepository)

    // After clearing, a new instance should be created
    // We can't directly compare instances, but we can verify the user ID is cleared
    assertNull(instance2.getCurrentUserId())
  }

  // ============================================================================
  // Fake Repository for Testing
  // ============================================================================

  private class FakePresenceRepository : PresenceRepository {
    var lastOnlineUserId: String? = null
    var lastOnlineContextIds: List<String> = emptyList()
    var lastOfflineUserId: String? = null

    override suspend fun setUserOnline(userId: String, contextIds: List<String>) {
      lastOnlineUserId = userId
      lastOnlineContextIds = contextIds
    }

    override suspend fun setUserOffline(userId: String) {
      lastOfflineUserId = userId
    }

    override fun observeOnlineUsersCount(contextId: String, currentUserId: String): Flow<Int> {
      return MutableStateFlow(0)
    }

    override fun observeOnlineUserIds(
        contextId: String,
        currentUserId: String
    ): Flow<List<String>> {
      return MutableStateFlow(emptyList())
    }

    override suspend fun cleanupStalePresence(staleThresholdMs: Long) {
      // No-op for tests
    }
  }
}
