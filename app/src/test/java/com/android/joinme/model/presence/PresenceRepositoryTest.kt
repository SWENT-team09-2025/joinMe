package com.android.joinme.model.presence

// Implemented with help of Claude AI

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for PresenceRepository interface.
 *
 * Tests the companion object constants defined in the interface.
 */
class PresenceRepositoryTest {

  @Test
  fun staleThresholdMs_isFiveMinutes() {
    // 5 minutes in milliseconds = 5 * 60 * 1000 = 300000
    assertEquals(300000L, PresenceRepository.STALE_THRESHOLD_MS)
  }

  @Test
  fun staleThresholdMs_isCorrectValue() {
    val fiveMinutesInMs = 5 * 60 * 1000L
    assertEquals(fiveMinutesInMs, PresenceRepository.STALE_THRESHOLD_MS)
  }
}
