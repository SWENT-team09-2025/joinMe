package com.android.joinme.model.invitation

import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InvitationRepositoryTest {

  private lateinit var repository: InvitationsRepository

  @Before
  fun setup() {
    repository =
        object : InvitationsRepository {
          private val invitations = mutableMapOf<String, Invitation>()

          override suspend fun createInvitation(
              type: InvitationType,
              targetId: String,
              createdBy: String,
              expiresInDays: Int?
          ): Result<String> {
            val token = "test-token-${invitations.size}"
            val expiresAt =
                expiresInDays?.let {
                  Timestamp(Date(System.currentTimeMillis() + (it * 24 * 60 * 60 * 1000L)))
                }
            invitations[token] =
                Invitation(
                    token = token,
                    type = type,
                    targetId = targetId,
                    createdBy = createdBy,
                    expiresAt = expiresAt)
            return Result.success(token)
          }

          override suspend fun resolveInvitation(token: String): Result<Invitation?> {
            val invitation = invitations[token]
            return if (invitation != null && invitation.isValid()) {
              Result.success(invitation)
            } else {
              Result.success(null)
            }
          }

          override suspend fun revokeInvitation(token: String): Result<Unit> {
            invitations.remove(token)
            return Result.success(Unit)
          }

          override suspend fun getInvitationsByUser(userId: String): Result<List<Invitation>> {
            val userInvitations = invitations.values.filter { it.createdBy == userId }
            return Result.success(userInvitations)
          }
        }
  }

  @Test
  fun createInvitation_returnsToken() = runTest {
    val result =
        repository.createInvitation(
            type = InvitationType.GROUP,
            targetId = "group123",
            createdBy = "user456",
            expiresInDays = 7)

    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull())
  }

  @Test
  fun resolveInvitation_afterCreation_returnsInvitation() = runTest {
    val createResult =
        repository.createInvitation(
            type = InvitationType.EVENT,
            targetId = "event789",
            createdBy = "user456",
            expiresInDays = null)
    val token = createResult.getOrNull()!!

    val resolveResult = repository.resolveInvitation(token)

    assertTrue(resolveResult.isSuccess)
    val invitation = resolveResult.getOrNull()
    assertNotNull(invitation)
    assertEquals("event789", invitation?.targetId)
    assertEquals(InvitationType.EVENT, invitation?.type)
  }

  @Test
  fun revokeInvitation_removesInvitation() = runTest {
    val createResult =
        repository.createInvitation(
            type = InvitationType.GROUP, targetId = "group123", createdBy = "user456")
    val token = createResult.getOrNull()!!

    val revokeResult = repository.revokeInvitation(token)
    val resolveResult = repository.resolveInvitation(token)

    assertTrue(revokeResult.isSuccess)
    assertTrue(resolveResult.getOrNull() == null)
  }

  @Test
  fun getInvitationsByUser_returnsUserInvitations() = runTest {
    val userId = "user456"
    repository.createInvitation(
        type = InvitationType.GROUP, targetId = "group1", createdBy = userId)
    repository.createInvitation(
        type = InvitationType.EVENT, targetId = "event1", createdBy = userId)
    repository.createInvitation(
        type = InvitationType.SERIE, targetId = "serie1", createdBy = "otherUser")

    val result = repository.getInvitationsByUser(userId)

    assertTrue(result.isSuccess)
    val invitations = result.getOrNull()
    assertEquals(2, invitations?.size)
    assertTrue(invitations?.all { it.createdBy == userId } == true)
  }
}
