package com.android.joinme.model.presence

// Implemented with help of Claude AI

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PresenceRepositoryProvider.
 *
 * Tests the singleton provider pattern for PresenceRepository, including the ability to swap
 * implementations for testing.
 *
 * Note: We cannot test the default lazy initialization of _repository because it requires Firebase.
 * Instead, we test the ability to swap the repository for testing purposes by setting a fake first.
 */
class PresenceRepositoryProviderTest {

  @Test
  fun repository_canBeSetToFake() {
    val fakeRepository = FakePresenceRepository()

    // Set fake first before any access to avoid Firebase initialization
    PresenceRepositoryProvider.repository = fakeRepository

    assertSame(fakeRepository, PresenceRepositoryProvider.repository)
  }

  @Test
  fun repository_swappedInstanceIsUsed() {
    val fakeRepository = FakePresenceRepository()

    PresenceRepositoryProvider.repository = fakeRepository

    val retrievedRepository = PresenceRepositoryProvider.repository
    assertTrue(retrievedRepository is FakePresenceRepository)
  }

  @Test
  fun repository_canBeReassignedMultipleTimes() {
    val fake1 = FakePresenceRepository()
    val fake2 = FakePresenceRepository()

    PresenceRepositoryProvider.repository = fake1
    assertEquals(fake1, PresenceRepositoryProvider.repository)

    PresenceRepositoryProvider.repository = fake2
    assertEquals(fake2, PresenceRepositoryProvider.repository)
  }

  @Test
  fun repository_afterSettingFake_returnsNonNull() {
    val fakeRepository = FakePresenceRepository()

    PresenceRepositoryProvider.repository = fakeRepository

    assertNotNull(PresenceRepositoryProvider.repository)
  }

  @Test
  fun repository_fakeImplementsAllMethods() {
    val fakeRepository = FakePresenceRepository()
    PresenceRepositoryProvider.repository = fakeRepository

    val repo = PresenceRepositoryProvider.repository

    // Verify it's a valid PresenceRepository
    assertNotNull(repo.observeOnlineUsersCount("context", "user"))
    assertNotNull(repo.observeOnlineUserIds("context", "user"))
  }

  // ============================================================================
  // Fake Repository for Testing
  // ============================================================================

  private class FakePresenceRepository : PresenceRepository {
    override suspend fun setUserOnline(userId: String, contextIds: List<String>) {
      // No-op
    }

    override suspend fun setUserOffline(userId: String) {
      // No-op
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
      // No-op
    }
  }
}
