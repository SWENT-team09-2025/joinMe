package com.android.joinme.model.invitation

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.tasks.await

private const val INVITATIONS_COLLECTION_PATH = "invitations"
private const val DAY_TO_MILLIS = 24 * 60 * 60 * 1000

/** Represents an invitation to join a group, event, or serie. */
class InvitationRepositoryFirestore(db: FirebaseFirestore = FirebaseFirestore.getInstance()) :
    InvitationsRepository {
  private val invitationsCollection = db.collection(INVITATIONS_COLLECTION_PATH)

  override suspend fun createInvitation(
      type: InvitationType,
      targetId: String,
      createdBy: String,
      expiresInDays: Int?
  ): Result<String> {
    return try {
      val token = UUID.randomUUID().toString()
      val expiresAt =
          expiresInDays?.let { days ->
            Timestamp(java.util.Date(System.currentTimeMillis() + (days * DAY_TO_MILLIS)))
          }
      val invitation =
          hashMapOf(
              "token" to token,
              "type" to type.name,
              "targetId" to targetId,
              "createdBy" to createdBy,
              "createdAt" to Timestamp.now(),
              "expiresAt" to expiresAt)
      invitationsCollection.document(token).set(invitation).await()
      Result.success(token)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun resolveInvitation(token: String): Result<Invitation?> {
    return try {
      val doc = invitationsCollection.document(token).get().await()

      if (!doc.exists()) {
        return Result.success(null)
      }

      val type =
          InvitationType.fromString(doc.getString("type") ?: "") ?: return Result.success(null)

      val invitation =
          Invitation(
              token = doc.getString("token") ?: "",
              type = type,
              targetId = doc.getString("targetId") ?: "",
              createdBy = doc.getString("createdBy") ?: "",
              createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
              expiresAt = doc.getTimestamp("expiresAt"))

      if (!invitation.isValid()) {
        return Result.success(null)
      }

      Result.success(invitation)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun revokeInvitation(token: String): Result<Unit> {
    return try {
      invitationsCollection.document(token).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun getInvitationsByUser(userId: String): Result<List<Invitation>> {
    return try {
      val snapshot = invitationsCollection.whereEqualTo("createdBy", userId).get().await()

      val invitations =
          snapshot.documents.mapNotNull { doc ->
            val type =
                InvitationType.fromString(doc.getString("type") ?: "") ?: return@mapNotNull null

            Invitation(
                token = doc.getString("token") ?: "",
                type = type,
                targetId = doc.getString("targetId") ?: "",
                createdBy = doc.getString("createdBy") ?: "",
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                expiresAt = doc.getTimestamp("expiresAt"))
          }
      Result.success(invitations)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
