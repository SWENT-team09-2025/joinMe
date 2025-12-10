package com.android.joinme.model.invitation

// Implemented with help of Claude AI

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests for InvitationRepositoryProvider.
 *
 * Tests the singleton provider pattern for InvitationsRepository, including the ability to swap
 * implementations for testing.
 *
 * Note: We cannot test the default lazy initialization of firestoreRepo because it requires
 * Firebase. Instead, we test the ability to swap the repository for testing purposes by setting a
 * fake first.
 */
class InvitationRepositoryProviderTest {

  @Test
  fun repository_canBeReassignedMultipleTimes() {
    val fake1 = FakeInvitationRepository()
    val fake2 = FakeInvitationRepository()

    InvitationRepositoryProvider.repository = fake1
    assertSame(fake1, InvitationRepositoryProvider.repository)

    InvitationRepositoryProvider.repository = fake2
    assertEquals(fake2, InvitationRepositoryProvider.repository)
  }

  // ============================================================================
  // Fake Repository for Testing
  // ============================================================================

  private class FakeInvitationRepository : InvitationsRepository {
    override suspend fun createInvitation(
        type: InvitationType,
        targetId: String,
        createdBy: String,
        expiresInDays: Int?
    ): Result<String> {
      return Result.success("fake-token")
    }

    override suspend fun resolveInvitation(token: String): Result<Invitation?> {
      return Result.success(null)
    }

    override suspend fun revokeInvitation(token: String): Result<Unit> {
      return Result.success(Unit)
    }

    override suspend fun getInvitationsByUser(userId: String): Result<List<Invitation>> {
      return Result.success(emptyList())
    }
  }
}
