package com.android.joinme.model.presence

// Implemented with help of Claude AI

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for PresenceManager.
 *
 * These tests run on an Android device/emulator where Firebase is properly initialized, allowing us
 * to test functionality that requires Firebase (like getInstance()).
 */
@RunWith(AndroidJUnit4::class)
class PresenceManagerAndroidTest {

  @Before
  fun setup() {
    // Clear any existing singleton instance before each test
    PresenceManager.clearInstance()
  }

  @After
  fun tearDown() {
    // Clean up after each test
    PresenceManager.clearInstance()
  }

  // ============================================================================
  // Singleton getInstance() Tests
  // ============================================================================

  @Test
  fun getInstance_returnsNonNullInstance() {
    val instance = PresenceManager.getInstance()

    assertNotNull(instance)
  }

  @Test
  fun getInstance_returnsSameInstance_whenCalledMultipleTimes() {
    val instance1 = PresenceManager.getInstance()
    val instance2 = PresenceManager.getInstance()
    val instance3 = PresenceManager.getInstance()

    // All calls should return the same singleton instance
    assertSame(instance1, instance2)
    assertSame(instance2, instance3)
  }

  @Test
  fun getInstance_afterClearInstance_returnsNewInstance() {
    val instance1 = PresenceManager.getInstance()

    PresenceManager.clearInstance()

    val instance2 = PresenceManager.getInstance()

    // After clearing, should return a different instance
    assertNotSame(instance1, instance2)
  }

  @Test
  fun getInstance_instanceHasNullCurrentUserId_initially() {
    val instance = PresenceManager.getInstance()

    // New instance should have null currentUserId
    assertEquals(null, instance.currentUserId)
  }

  @Test
  fun clearInstance_canBeCalledMultipleTimes_withoutCrashing() {
    // Should not crash when called multiple times
    PresenceManager.clearInstance()
    PresenceManager.clearInstance()
    PresenceManager.clearInstance()

    // Verify we can still get a valid instance
    val instance = PresenceManager.getInstance()
    assertNotNull(instance)
  }

  @Test
  fun getInstance_isThreadSafe() {
    // Test thread safety by calling getInstance from multiple threads
    val instances = mutableListOf<PresenceManager>()
    val threads =
        (1..10).map {
          Thread {
            val instance = PresenceManager.getInstance()
            synchronized(instances) { instances.add(instance) }
          }
        }

    threads.forEach { it.start() }
    threads.forEach { it.join() }

    // All threads should have received the same instance
    assertEquals(10, instances.size)
    val firstInstance = instances.first()
    instances.forEach { assertSame(firstInstance, it) }
  }

  @Test
  fun getInstance_afterClearAndMultipleGets_returnsSameNewInstance() {
    val originalInstance = PresenceManager.getInstance()

    PresenceManager.clearInstance()

    // Get multiple times after clearing
    val newInstance1 = PresenceManager.getInstance()
    val newInstance2 = PresenceManager.getInstance()

    // Original should be different from new instances
    assertNotSame(originalInstance, newInstance1)

    // New instances should be the same
    assertSame(newInstance1, newInstance2)
  }
}
