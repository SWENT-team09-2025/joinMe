package com.android.joinme.model.invitation

/** Handles invitations operations such as creating, resolving, and revoking invitations. */
interface InvitationsRepository {

  /**
   * Creates an invitation for a target entity.
   *
   * @param type The type of invitation to create.
   * @param targetId The ID of the target entity.
   * @param createdBy The ID of the user who created the invitation.
   * @param expiresInDays The number of days after which the invitation expires.
   * @return A [Result] containing the token of the created invitation on success, or an exception
   *   on failure.
   */
  suspend fun createInvitation(
      type: InvitationType,
      targetId: String,
      createdBy: String,
      expiresInDays: Int? = null
  ): Result<String>

  /**
   * Resolves an invitation by its token.
   *
   * @param token The token of the invitation to resolve.
   */
  suspend fun resolveInvitation(token: String): Result<Invitation?>

  /**
   * Revokes an invitation by its token.
   *
   * @param token The token of the invitation to revoke.
   */
  suspend fun revokeInvitation(token: String): Result<Unit>

  /**
   * Retrieves a list of invitations for a given user.
   *
   * @param userId The ID of the user for whom to retrieve invitations.
   */
  suspend fun getInvitationsByUser(userId: String): Result<List<Invitation>>
}
