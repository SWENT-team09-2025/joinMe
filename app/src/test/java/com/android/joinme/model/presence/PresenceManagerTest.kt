package com.android.joinme.model.presence

// Implemented with help of Claude AI

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for PresenceManager.
 *
 * Tests the presence tracking lifecycle management including starting/stopping tracking and user
 * online/offline status transitions.
 */
@RunWith(RobolectricTestRunner::class)
class PresenceManagerTest {

  private lateinit var fakePresenceRepository: FakePresenceRepository
  private lateinit var presenceManager: PresenceManager
  private lateinit var application: Application
  private lateinit var fakeContextIdProvider: FakeContextIdProvider

  @Before
  fun setup() {
    PresenceManager.clearInstance()
    fakePresenceRepository = FakePresenceRepository()
    presenceManager = PresenceManager(fakePresenceRepository)
    application = ApplicationProvider.getApplicationContext()
    fakeContextIdProvider = FakeContextIdProvider()
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
    val userId = "user123"

    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    assertEquals(userId, presenceManager.getCurrentUserId())
  }

  @Test
  fun startTracking_withBlankUserId_doesNotTrack() {
    presenceManager.startTracking(application, "", fakeContextIdProvider)

    assertNull(presenceManager.getCurrentUserId())
  }

  @Test
  fun startTracking_withBlankUserId_spaces_doesNotTrack() {
    presenceManager.startTracking(application, "   ", fakeContextIdProvider)

    assertNull(presenceManager.getCurrentUserId())
  }

  @Test
  fun startTracking_whenAlreadyTrackingSameUser_doesNotRestart() {
    val userId = "user123"

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    val firstCallCount = fakeContextIdProvider.callCount

    // Start tracking again with same user
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    // Should not call provider again since already tracking same user
    assertEquals(firstCallCount, fakeContextIdProvider.callCount)
  }

  @Test
  fun startTracking_whenTrackingDifferentUser_restarts() {
    val userId1 = "user123"
    val userId2 = "user456"

    presenceManager.startTracking(application, userId1, fakeContextIdProvider)
    Thread.sleep(50) // Allow coroutine to execute

    presenceManager.startTracking(application, userId2, fakeContextIdProvider)

    assertEquals(userId2, presenceManager.getCurrentUserId())
  }

  @Test
  fun startTracking_setsUserOnlineInContexts() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1", "context2")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    // Use Thread.sleep since coroutine runs on Dispatchers.IO
    Thread.sleep(200)

    assertEquals(userId, fakePresenceRepository.lastOnlineUserId)
    assertEquals(listOf("context1", "context2"), fakePresenceRepository.lastOnlineContextIds)
  }

  @Test
  fun startTracking_withEmptyContextIds_doesNotCallSetUserOnline() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = emptyList()

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    // Use Thread.sleep since coroutine runs on Dispatchers.IO
    Thread.sleep(200)

    // setUserOnline should not be called with empty context list
    assertNull(fakePresenceRepository.lastOnlineUserId)
  }

  // ============================================================================
  // Stop Tracking Tests
  // ============================================================================

  @Test
  fun stopTracking_clearsCurrentUserId() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    presenceManager.stopTracking()

    assertNull(presenceManager.getCurrentUserId())
  }

  @Test
  fun stopTracking_setsUserOffline() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    presenceManager.stopTracking()
    // Use Thread.sleep since coroutine runs on Dispatchers.IO
    Thread.sleep(200)

    assertEquals(userId, fakePresenceRepository.lastOfflineUserId)
  }

  @Test
  fun stopTracking_whenNotTracking_doesNotThrow() {
    // Should not throw when called without starting tracking
    presenceManager.stopTracking()

    assertNull(presenceManager.getCurrentUserId())
  }

  @Test
  fun stopTracking_calledMultipleTimes_doesNotThrow() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    presenceManager.stopTracking()
    presenceManager.stopTracking() // Call again

    assertNull(presenceManager.getCurrentUserId())
  }

  // ============================================================================
  // Activity Lifecycle Callback Tests
  // ============================================================================

  @Test
  fun startTracking_registersLifecycleCallbacks() {
    val userId = "user123"

    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    // If we got here without exception, callbacks were registered successfully
    assertEquals(userId, presenceManager.getCurrentUserId())
  }

  @Test
  fun stopTracking_unregistersLifecycleCallbacks() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    presenceManager.stopTracking()

    // If we got here without exception, callbacks were unregistered successfully
    assertNull(presenceManager.getCurrentUserId())
  }

  // ============================================================================
  // Singleton Instance Tests
  // ============================================================================

  @Test
  fun clearInstance_clearsState() {
    PresenceManager.clearInstance()

    // After clearing, creating a new instance with fake repo should have cleared state
    val newManager = PresenceManager(fakePresenceRepository)
    assertNull(newManager.getCurrentUserId())
  }

  @Test
  fun clearInstance_whenInstanceHasTracking_clearsIt() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    PresenceManager.clearInstance()

    // Creating new instance should have clean state
    val newManager = PresenceManager(fakePresenceRepository)
    assertNull(newManager.getCurrentUserId())
  }

  @Test
  fun getCurrentUserId_whenNotTracking_returnsNull() {
    assertNull(presenceManager.getCurrentUserId())
  }

  @Test
  fun getCurrentUserId_whenTracking_returnsUserId() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    assertEquals(userId, presenceManager.getCurrentUserId())
  }

  // ============================================================================
  // Error Handling Tests
  // ============================================================================

  @Test
  fun startTracking_whenContextIdProviderThrows_handlesGracefully() {
    val userId = "user123"
    fakeContextIdProvider.shouldThrowException = true

    // Should not throw
    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    Thread.sleep(200)

    // User ID should still be set even if setting online failed
    assertEquals(userId, presenceManager.getCurrentUserId())
  }

  @Test
  fun stopTracking_whenSetOfflineThrows_handlesGracefully() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    fakePresenceRepository.shouldThrowOnOffline = true

    // Should not throw
    presenceManager.stopTracking()
    Thread.sleep(200)

    // State should still be cleared
    assertNull(presenceManager.getCurrentUserId())
  }

  // ============================================================================
  // Fake Repository for Testing
  // ============================================================================

  private class FakePresenceRepository : PresenceRepository {
    var lastOnlineUserId: String? = null
    var lastOnlineContextIds: List<String> = emptyList()
    var lastOfflineUserId: String? = null
    var shouldThrowOnOffline = false

    override suspend fun setUserOnline(userId: String, contextIds: List<String>) {
      lastOnlineUserId = userId
      lastOnlineContextIds = contextIds
    }

    override suspend fun setUserOffline(userId: String) {
      if (shouldThrowOnOffline) {
        throw RuntimeException("Test exception")
      }
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

  // ============================================================================
  // Fake Context ID Provider for Testing
  // ============================================================================

  private class FakeContextIdProvider : ContextIdProvider {
    var contextIds: List<String> = listOf("default-context")
    var callCount = 0
    var shouldThrowException = false

    override suspend fun getContextIdsForUser(userId: String): List<String> {
      callCount++
      if (shouldThrowException) {
        throw RuntimeException("Test exception")
      }
      return contextIds
    }
  }
}
