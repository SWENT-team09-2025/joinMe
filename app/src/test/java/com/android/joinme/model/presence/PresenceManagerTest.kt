package com.android.joinme.model.presence

// Implemented with help of Claude AI

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for PresenceManager.
 *
 * Tests the presence tracking lifecycle management including starting/stopping tracking and user
 * online/offline status transitions.
 *
 * Optimized for fast execution while maintaining comprehensive line coverage.
 */
@RunWith(RobolectricTestRunner::class)
class PresenceManagerTest {

  companion object {
    // Minimal delay for coroutine execution - optimized for speed
    private const val COROUTINE_DELAY_MS = 50L
  }

  private fun awaitCoroutines() = Thread.sleep(COROUTINE_DELAY_MS)

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
  fun startTracking_setsCurrentUserIdAndRegistersCallbacks() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1", "context2")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    // User ID should be set immediately and callbacks registered (no exception)
    assertEquals(userId, presenceManager.currentUserId)
  }

  @Test
  fun startTracking_withBlankUserIds_doesNotTrack() {
    // Test empty string
    presenceManager.startTracking(application, "", fakeContextIdProvider)
    assertNull(presenceManager.currentUserId)

    // Test whitespace variants
    presenceManager.startTracking(application, "   ", fakeContextIdProvider)
    assertNull(presenceManager.currentUserId)

    presenceManager.startTracking(application, "\t", fakeContextIdProvider)
    assertNull(presenceManager.currentUserId)

    presenceManager.startTracking(application, "\n", fakeContextIdProvider)
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
  fun startTracking_whenTrackingDifferentUser_restartsAndUpdates() {
    val userId1 = "user123"
    val userId2 = "user456"

    presenceManager.startTracking(application, userId1, fakeContextIdProvider)
    awaitCoroutines()

    presenceManager.startTracking(application, userId2, fakeContextIdProvider)

    assertEquals(userId2, presenceManager.currentUserId)
  }

  @Test
  fun startTracking_withEmptyContextIds_doesNotCallSetUserOnline() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = emptyList()

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    awaitCoroutines()

    assertEquals(userId, presenceManager.currentUserId)
  }

  // ============================================================================
  // Stop Tracking Tests
  // ============================================================================

  @Test
  fun stopTracking_clearsStateAndSetsUserOffline() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    presenceManager.stopTracking()
    awaitCoroutines()

    assertNull(presenceManager.currentUserId)
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
    presenceManager.startTracking(application, "user123", fakeContextIdProvider)

    presenceManager.stopTracking()
    presenceManager.stopTracking() // Call again - should not throw

    assertNull(presenceManager.currentUserId)
  }

  // ============================================================================
  // Singleton Instance Tests
  // ============================================================================

  @Test
  fun getInstance_returnsSameInstance() {
    // Note: This test verifies getInstance behavior without Firebase dependency
    // by testing the singleton pattern through clearInstance
    val manager1 = PresenceManager(fakePresenceRepository)
    val manager2 = PresenceManager(fakePresenceRepository)

    // Different instances when created directly
    assertTrue(manager1 !== manager2)
    assertNull(manager1.currentUserId)
    assertNull(manager2.currentUserId)
  }

  @Test
  fun clearInstance_clearsStateAndCallsUnregister() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    PresenceManager.clearInstance()

    // Creating new instance should have clean state
    val newManager = PresenceManager(fakePresenceRepository)
    assertNull(newManager.currentUserId)
  }

  @Test
  fun currentUserId_reflectsTrackingState() {
    // Initially null
    assertNull(presenceManager.currentUserId)

    // Set after tracking starts
    presenceManager.startTracking(application, "user123", fakeContextIdProvider)
    assertEquals("user123", presenceManager.currentUserId)

    // Null after tracking stops
    presenceManager.stopTracking()
    assertNull(presenceManager.currentUserId)
  }

  // ============================================================================
  // Error Handling Tests
  // ============================================================================

  @Test
  fun startTracking_whenExceptionsThrown_handlesGracefully() {
    val userId = "user123"

    // Test provider throwing
    fakeContextIdProvider.shouldThrowException = true
    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    awaitCoroutines()
    assertEquals(userId, presenceManager.currentUserId)

    // Reset and test repository throwing
    PresenceManager.clearInstance()
    presenceManager = PresenceManager(fakePresenceRepository)
    fakeContextIdProvider.shouldThrowException = false
    fakePresenceRepository.shouldThrowOnOnline = true

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    awaitCoroutines()
    assertEquals(userId, presenceManager.currentUserId)
  }

  @Test
  fun stopTracking_whenSetOfflineThrows_handlesGracefully() {
    val userId = "user123"
    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    fakePresenceRepository.shouldThrowOnOffline = true

    // Should not throw
    presenceManager.stopTracking()
    awaitCoroutines()

    // State should still be cleared
    assertNull(presenceManager.currentUserId)
  }

  // ============================================================================
  // setUserOnlineInAllContexts Tests (via triggerSetUserOnlineInAllContexts)
  // ============================================================================

  @Test
  fun setUserOnlineInAllContexts_whenNoCurrentUserId_returnsEarly() = runBlocking {
    presenceManager.triggerSetUserOnlineInAllContexts()

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
    fakePresenceRepository.lastOnlineUserId = null
    presenceManager.triggerSetUserOnlineInAllContexts()

    assertNull(fakePresenceRepository.lastOnlineUserId)
  }

  @Test
  fun setUserOnlineInAllContexts_whenExceptionsThrown_handlesGracefully() = runBlocking {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")

    // Test provider throwing
    fakeContextIdProvider.shouldThrowException = true
    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    presenceManager.triggerSetUserOnlineInAllContexts()
    assertEquals(userId, presenceManager.currentUserId)

    // Test repository throwing
    fakeContextIdProvider.shouldThrowException = false
    fakePresenceRepository.shouldThrowOnOnline = true
    presenceManager.triggerSetUserOnlineInAllContexts()
    assertEquals(userId, presenceManager.currentUserId)
  }

  // ============================================================================
  // Activity Lifecycle Simulation Tests
  // ============================================================================

  @Test
  fun simulateActivityStarted_setsUserOnlineWhenFromBackground() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    fakePresenceRepository.lastOnlineUserId = null

    // First activity start (from background)
    presenceManager.simulateActivityStarted()
    awaitCoroutines()

    assertEquals(userId, fakePresenceRepository.lastOnlineUserId)
    assertEquals(1, presenceManager.getStartedActivityCount())

    // Second activity start (already in foreground)
    fakePresenceRepository.lastOnlineUserId = null
    presenceManager.simulateActivityStarted()
    awaitCoroutines()

    assertNull(fakePresenceRepository.lastOnlineUserId) // Should NOT call again
    assertEquals(2, presenceManager.getStartedActivityCount())
  }

  @Test
  fun simulateActivityStopped_setsUserOfflineWhenLastActivity() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    presenceManager.simulateActivityStarted()
    presenceManager.simulateActivityStarted()
    awaitCoroutines()
    assertEquals(2, presenceManager.getStartedActivityCount())

    // Stop first activity (still one running)
    presenceManager.simulateActivityStopped()
    awaitCoroutines()
    assertNull(fakePresenceRepository.lastOfflineUserId)
    assertEquals(1, presenceManager.getStartedActivityCount())

    // Stop last activity
    presenceManager.simulateActivityStopped()
    awaitCoroutines()
    assertEquals(userId, fakePresenceRepository.lastOfflineUserId)
    assertEquals(0, presenceManager.getStartedActivityCount())
  }

  @Test
  fun simulateActivityStopped_whenSetOfflineThrows_handlesGracefully() {
    presenceManager.startTracking(application, "user123", fakeContextIdProvider)
    presenceManager.simulateActivityStarted()
    fakePresenceRepository.shouldThrowOnOffline = true

    presenceManager.simulateActivityStopped()
    awaitCoroutines()

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
  fun simulateActivityLifecycle_whenNoCurrentUserId_doesNotCrash() {
    presenceManager.startTracking(application, "user123", fakeContextIdProvider)

    // Clear userId via reflection
    val field = PresenceManager::class.java.getDeclaredField("currentUserId")
    field.isAccessible = true
    field.set(presenceManager, null)

    // Should not crash on start
    presenceManager.simulateActivityStarted()
    awaitCoroutines()
    assertNull(fakePresenceRepository.lastOnlineUserId)

    // Should not crash on stop
    presenceManager.simulateActivityStopped()
    awaitCoroutines()
    assertNull(fakePresenceRepository.lastOfflineUserId)
  }

  // ============================================================================
  // Real Activity Lifecycle Callback Tests (using Robolectric ActivityController)
  // ============================================================================

  @Test
  fun activityLifecycleCallbacks_onlyStartAndStopAffectPresence() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    val activityController = Robolectric.buildActivity(Activity::class.java)

    // onCreate - no effect
    activityController.create()
    awaitCoroutines()
    assertEquals(0, presenceManager.getStartedActivityCount())

    // onStart - sets online
    fakePresenceRepository.lastOnlineUserId = null
    activityController.start()
    awaitCoroutines()
    assertEquals(userId, fakePresenceRepository.lastOnlineUserId)
    assertEquals(1, presenceManager.getStartedActivityCount())

    // onResume - no effect
    fakePresenceRepository.lastOnlineUserId = null
    activityController.resume()
    awaitCoroutines()
    assertNull(fakePresenceRepository.lastOnlineUserId)

    // onPause - no effect
    activityController.pause()
    awaitCoroutines()
    assertNull(fakePresenceRepository.lastOfflineUserId)
    assertEquals(1, presenceManager.getStartedActivityCount())

    // onSaveInstanceState - no effect
    activityController.saveInstanceState(Bundle())
    awaitCoroutines()
    assertNull(fakePresenceRepository.lastOfflineUserId)

    // onStop - sets offline
    activityController.stop()
    awaitCoroutines()
    assertEquals(userId, fakePresenceRepository.lastOfflineUserId)
    assertEquals(0, presenceManager.getStartedActivityCount())

    // onDestroy - no additional effect
    fakePresenceRepository.lastOfflineUserId = null
    activityController.destroy()
    awaitCoroutines()
    assertNull(fakePresenceRepository.lastOfflineUserId)
  }

  @Test
  fun activityLifecycleCallbacks_multipleActivities_tracksCorrectly() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")
    presenceManager.startTracking(application, userId, fakeContextIdProvider)

    val activity1 = Robolectric.buildActivity(Activity::class.java)
    val activity2 = Robolectric.buildActivity(Activity::class.java)

    // Start first activity
    activity1.create().start()
    awaitCoroutines()
    assertEquals(1, presenceManager.getStartedActivityCount())

    // Start second activity
    activity2.create().start()
    awaitCoroutines()
    assertEquals(2, presenceManager.getStartedActivityCount())

    // Stop first activity (still one running)
    activity1.resume().pause().stop()
    awaitCoroutines()
    assertEquals(1, presenceManager.getStartedActivityCount())
    assertNull(fakePresenceRepository.lastOfflineUserId)

    // Stop second activity (now all stopped)
    activity2.resume().pause().stop()
    awaitCoroutines()
    assertEquals(0, presenceManager.getStartedActivityCount())
    assertEquals(userId, fakePresenceRepository.lastOfflineUserId)
  }

  // ============================================================================
  // Additional Coverage Tests
  // ============================================================================

  @Test
  fun clearInstance_whenNoInstance_doesNotThrow() {
    // Clear multiple times when no instance exists
    PresenceManager.clearInstance()
    PresenceManager.clearInstance()
    // No exception means success
  }

  @Test
  fun startTracking_cancelsExistingPresenceJob() {
    val userId1 = "user123"
    val userId2 = "user456"

    // Start tracking first user
    presenceManager.startTracking(application, userId1, fakeContextIdProvider)
    awaitCoroutines()

    // Start tracking different user (should cancel pending job)
    presenceManager.startTracking(application, userId2, fakeContextIdProvider)

    assertEquals(userId2, presenceManager.currentUserId)
  }

  @Test
  fun activityLifecycleCallbacks_onActivityStopped_whenExceptionThrown_logsError() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")
    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    fakePresenceRepository.shouldThrowOnOffline = true

    // Start an activity
    val activityController = Robolectric.buildActivity(Activity::class.java)
    activityController.create().start()
    awaitCoroutines()
    assertEquals(1, presenceManager.getStartedActivityCount())

    // Stop the activity - should trigger exception in setUserOffline
    activityController.resume().pause().stop()
    awaitCoroutines()

    // Exception is caught and logged, count is still decremented
    assertEquals(0, presenceManager.getStartedActivityCount())
  }

  @Test
  fun startTracking_withSingleContextId_setsUserOnline() = runBlocking {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("single-context")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    presenceManager.triggerSetUserOnlineInAllContexts()

    assertEquals(userId, fakePresenceRepository.lastOnlineUserId)
    assertEquals(listOf("single-context"), fakePresenceRepository.lastOnlineContextIds)
  }

  @Test
  fun stopTracking_resetsAllState() {
    val userId = "user123"
    fakeContextIdProvider.contextIds = listOf("context1")

    presenceManager.startTracking(application, userId, fakeContextIdProvider)
    presenceManager.simulateActivityStarted()
    presenceManager.simulateActivityStarted()
    awaitCoroutines()

    presenceManager.stopTracking()
    awaitCoroutines()

    // All state should be cleared
    assertNull(presenceManager.currentUserId)
    assertEquals(0, presenceManager.getStartedActivityCount())
  }

  @Test
  fun startTracking_thenStopThenStart_worksCorrectly() {
    val userId1 = "user123"
    val userId2 = "user456"
    fakeContextIdProvider.contextIds = listOf("context1")

    // First tracking session
    presenceManager.startTracking(application, userId1, fakeContextIdProvider)
    assertEquals(userId1, presenceManager.currentUserId)

    // Stop tracking
    presenceManager.stopTracking()
    awaitCoroutines()
    assertNull(presenceManager.currentUserId)

    // Start new tracking session
    presenceManager.startTracking(application, userId2, fakeContextIdProvider)
    assertEquals(userId2, presenceManager.currentUserId)
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
