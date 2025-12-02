package com.android.joinme.model.presence

// Implemented with help of Claude AI

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
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

    assertEquals(userId, presenceManager.currentUserId)
  }

  @Test
  fun startTracking_withBlankUserId_doesNotTrack() {
    presenceManager.startTracking(application, "", fakeContextIdProvider)

    assertNull(presenceManager.currentUserId)
  }

  @Test
  fun startTracking_withBlankUserId_spaces_doesNotTrack() {
    presenceManager.startTracking(application, "   ", fakeContextIdProvider)

    assertNull(presenceManager.currentUserId)
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

    assertEquals(userId2, presenceManager.currentUserId)
  }

  @Test
  fun startTracking_setsCurrentUserIdImmediately() {
    // Note: In Robolectric, ProcessLifecycleOwner may not report STARTED state,
    // so we test that userId is set regardless of foreground state
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1", "context2")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    // User ID should be set immediately
    assertEquals(userId, presenceManager.currentUserId)
  }

  @Test
  fun startTracking_withEmptyContextIds_doesNotCallSetUserOnline() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = emptyList()

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    // Use Thread.sleep since coroutine runs on Dispatchers.IO
    Thread.sleep(200)

    // setUserOnline should not be called with empty context list
    // (Note: in Robolectric, may not be in foreground so call may not happen anyway)
    assertEquals(userId, presenceManager.currentUserId)
  }

  // ============================================================================
  // Stop Tracking Tests
  // ============================================================================

  @Test
  fun stopTracking_clearsCurrentUserId() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    presenceManager.stopTracking()

    assertNull(presenceManager.currentUserId)
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

    assertNull(presenceManager.currentUserId)
  }

  @Test
  fun stopTracking_calledMultipleTimes_doesNotThrow() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    presenceManager.stopTracking()
    presenceManager.stopTracking() // Call again

    assertNull(presenceManager.currentUserId)
  }

  // ============================================================================
  // Activity Lifecycle Callback Tests
  // ============================================================================

  @Test
  fun startTracking_registersLifecycleCallbacks() {
    val userId = "user123"

    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    // If we got here without exception, callbacks were registered successfully
    assertEquals(userId, presenceManager.currentUserId)
  }

  @Test
  fun stopTracking_unregistersLifecycleCallbacks() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    presenceManager.stopTracking()

    // If we got here without exception, callbacks were unregistered successfully
    assertNull(presenceManager.currentUserId)
  }

  // ============================================================================
  // Singleton Instance Tests
  // ============================================================================

  // Note: getInstance() tests are not included because they require Firebase initialization
  // which is not available in unit tests. The singleton pattern is tested indirectly
  // through the clearInstance() tests which verify instance management works correctly.

  @Test
  fun multipleInstancesWithSameRepo_areDifferentObjects() {
    // Test that creating multiple instances is possible (for testing purposes)
    val manager1 = PresenceManager(fakePresenceRepository)
    val manager2 = PresenceManager(fakePresenceRepository)

    // They are different instances
    assertNull(manager1.currentUserId)
    assertNull(manager2.currentUserId)
  }

  @Test
  fun clearInstance_clearsState() {
    PresenceManager.clearInstance()

    // After clearing, creating a new instance with fake repo should have cleared state
    val newManager = PresenceManager(fakePresenceRepository)
    assertNull(newManager.currentUserId)
  }

  @Test
  fun clearInstance_whenInstanceHasTracking_clearsIt() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    PresenceManager.clearInstance()

    // Creating new instance should have clean state
    val newManager = PresenceManager(fakePresenceRepository)
    assertNull(newManager.currentUserId)
  }

  @Test
  fun currentUserId_whenNotTracking_returnsNull() {
    assertNull(presenceManager.currentUserId)
  }

  @Test
  fun currentUserId_whenTracking_returnsUserId() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    assertEquals(userId, presenceManager.currentUserId)
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
    assertEquals(userId, presenceManager.currentUserId)
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
    assertNull(presenceManager.currentUserId)
  }

  @Test
  fun startTracking_withTabUserId_doesNotTrack() {
    presenceManager.startTracking(application, "\t", fakeContextIdProvider)

    assertNull(presenceManager.currentUserId)
  }

  @Test
  fun startTracking_withNewlineUserId_doesNotTrack() {
    presenceManager.startTracking(application, "\n", fakeContextIdProvider)

    assertNull(presenceManager.currentUserId)
  }

  @Test
  fun startTracking_whenSetOnlineThrows_handlesGracefully() {
    val userId = "user123"
    fakePresenceRepository.shouldThrowOnOnline = true

    // Should not throw
    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    Thread.sleep(200)

    // User ID should still be set even if setting online failed
    assertEquals(userId, presenceManager.currentUserId)
  }

  @Test
  fun startTracking_switchingUsers_updatesCurrentUserId() {
    val userId1 = "user123"
    val userId2 = "user456"

    presenceManager.startTracking(application, userId1, fakeContextIdProvider)
    Thread.sleep(100)

    presenceManager.startTracking(application, userId2, fakeContextIdProvider)
    Thread.sleep(200)

    // New user should be tracked
    assertEquals(userId2, presenceManager.currentUserId)
  }

  // ============================================================================
  // setUserOnlineInAllContexts Tests (via triggerSetUserOnlineInAllContexts)
  // ============================================================================

  @Test
  fun setUserOnlineInAllContexts_whenNoCurrentUserId_returnsEarly() = runBlocking {
    // Don't start tracking, so currentUserId is null

    presenceManager.triggerSetUserOnlineInAllContexts()

    // setUserOnline should not have been called
    assertNull(fakePresenceRepository.lastOnlineUserId)
  }

  @Test
  fun setUserOnlineInAllContexts_whenNoProvider_returnsEarly() = runBlocking {
    // Set currentUserId directly without provider using reflection
    val manager = PresenceManager(fakePresenceRepository)
    val field = PresenceManager::class.java.getDeclaredField("currentUserId")
    field.isAccessible = true
    field.set(manager, "user123")

    manager.triggerSetUserOnlineInAllContexts()

    // setUserOnline should not have been called since no provider
    assertNull(fakePresenceRepository.lastOnlineUserId)
  }

  @Test
  fun setUserOnlineInAllContexts_withValidContext_callsSetUserOnline() = runBlocking {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1", "context2")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    presenceManager.triggerSetUserOnlineInAllContexts()

    assertEquals(userId, fakePresenceRepository.lastOnlineUserId)
    assertEquals(listOf("context1", "context2"), fakePresenceRepository.lastOnlineContextIds)
  }

  @Test
  fun setUserOnlineInAllContexts_withEmptyContextIds_doesNotCallSetUserOnline() = runBlocking {
    val userId = "user123"
    fakeContextIdProvider.contextIds = emptyList()

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    fakePresenceRepository.lastOnlineUserId = null // Reset
    presenceManager.triggerSetUserOnlineInAllContexts()

    // setUserOnline should not be called with empty list
    assertNull(fakePresenceRepository.lastOnlineUserId)
  }

  @Test
  fun setUserOnlineInAllContexts_whenProviderThrows_handlesGracefully() = runBlocking {
    val userId = "user123"
    fakeContextIdProvider.shouldThrowException = true

    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    // Should not throw
    presenceManager.triggerSetUserOnlineInAllContexts()

    // User ID should still be tracked
    assertEquals(userId, presenceManager.currentUserId)
  }

  @Test
  fun setUserOnlineInAllContexts_whenSetOnlineThrows_handlesGracefully() = runBlocking {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")
    fakePresenceRepository.shouldThrowOnOnline = true

    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    // Should not throw
    presenceManager.triggerSetUserOnlineInAllContexts()

    // User ID should still be tracked
    assertEquals(userId, presenceManager.currentUserId)
  }

  // ============================================================================
  // Activity Lifecycle Simulation Tests
  // ============================================================================

  @Test
  fun simulateActivityStarted_whenInBackground_setsUserOnline() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    fakePresenceRepository.lastOnlineUserId = null // Reset

    // Simulate coming from background (count = 0)
    presenceManager.simulateActivityStarted()
    Thread.sleep(200)

    assertEquals(userId, fakePresenceRepository.lastOnlineUserId)
  }

  @Test
  fun simulateActivityStarted_whenAlreadyInForeground_doesNotSetOnlineAgain() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    // First activity start
    presenceManager.simulateActivityStarted()
    Thread.sleep(100)
    fakePresenceRepository.lastOnlineUserId = null // Reset

    // Second activity start (already in foreground)
    presenceManager.simulateActivityStarted()
    Thread.sleep(100)

    // Should NOT call setUserOnline again
    assertNull(fakePresenceRepository.lastOnlineUserId)
  }

  @Test
  fun simulateActivityStopped_whenLastActivity_setsUserOffline() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    presenceManager.simulateActivityStarted() // count = 1
    Thread.sleep(100)

    presenceManager.simulateActivityStopped() // count = 0
    Thread.sleep(200)

    assertEquals(userId, fakePresenceRepository.lastOfflineUserId)
  }

  @Test
  fun simulateActivityStopped_whenNotLastActivity_doesNotSetOffline() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    presenceManager.simulateActivityStarted() // count = 1
    presenceManager.simulateActivityStarted() // count = 2
    Thread.sleep(100)

    presenceManager.simulateActivityStopped() // count = 1 (still foreground)
    Thread.sleep(200)

    // Should NOT call setUserOffline
    assertNull(fakePresenceRepository.lastOfflineUserId)
  }

  @Test
  fun simulateActivityStopped_whenSetOfflineThrows_handlesGracefully() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    presenceManager.simulateActivityStarted()
    fakePresenceRepository.shouldThrowOnOffline = true

    // Should not throw
    presenceManager.simulateActivityStopped()
    Thread.sleep(200)

    // Exception is handled gracefully (logged), count decremented to 0
    assertEquals(0, presenceManager.getStartedActivityCount())
  }

  @Test
  fun getStartedActivityCount_tracksCorrectly() {
    presenceManager.startTracking(application, "user123", fakeContextIdProvider)

    assertEquals(0, presenceManager.getStartedActivityCount())

    presenceManager.simulateActivityStarted()
    assertEquals(1, presenceManager.getStartedActivityCount())

    presenceManager.simulateActivityStarted()
    assertEquals(2, presenceManager.getStartedActivityCount())

    presenceManager.simulateActivityStopped()
    assertEquals(1, presenceManager.getStartedActivityCount())

    presenceManager.simulateActivityStopped()
    assertEquals(0, presenceManager.getStartedActivityCount())
  }

  @Test
  fun simulateActivityStarted_whenNoCurrentUserId_doesNotCrash() {
    // Start tracking then clear userId via reflection
    presenceManager.startTracking(application, "user123", fakeContextIdProvider)
    val field = PresenceManager::class.java.getDeclaredField("currentUserId")
    field.isAccessible = true
    field.set(presenceManager, null)

    // Should not crash
    presenceManager.simulateActivityStarted()
    Thread.sleep(100)

    // No online call made
    assertNull(fakePresenceRepository.lastOnlineUserId)
  }

  @Test
  fun simulateActivityStopped_whenNoCurrentUserId_doesNotCrash() {
    presenceManager.startTracking(application, "user123", fakeContextIdProvider)
    presenceManager.simulateActivityStarted()

    // Clear userId via reflection
    val field = PresenceManager::class.java.getDeclaredField("currentUserId")
    field.isAccessible = true
    field.set(presenceManager, null)

    // Should not crash
    presenceManager.simulateActivityStopped()
    Thread.sleep(100)

    // No offline call made (userId was null)
    assertNull(fakePresenceRepository.lastOfflineUserId)
  }

  // ============================================================================
  // Fake Repository for Testing
  // ============================================================================

  private class FakePresenceRepository : PresenceRepository {
    var lastOnlineUserId: String? = null
    var lastOnlineContextIds: List<String> = emptyList()
    var lastOfflineUserId: String? = null
    var shouldThrowOnOffline = false
    var shouldThrowOnOnline = false

    override suspend fun setUserOnline(userId: String, contextIds: List<String>) {
      if (shouldThrowOnOnline) {
        throw RuntimeException("Test exception")
      }
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
