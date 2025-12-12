package com.android.joinme.model.invitation

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.tasks.await

private const val INVITATIONS_COLLECTION_PATH = "invitations"
private const val DAY_TO_MILLIS = 24 * 60 * 60 * 1000
private const val TARGET_ID = "targetId"
private const val TYPE = "type"
private const val CREATED_BY = "createdBy"
private const val CREATED_AT = "createdAt"
private const val EXPIRES_AT = "expiresAt"
private const val TOKEN = "token"
private const val EMPTY_STRING = ""

/** Represents an invitation to join a group, event, or serie. */
class InvitationRepositoryFirestore(db: FirebaseFirestore = FirebaseFirestore.getInstance()) :
    InvitationsRepository {
  private val invitationsCollection = db.collection(INVITATIONS_COLLECTION_PATH)

  override suspend fun createInvitation(
      type: InvitationType,
      targetId: String,
      createdBy: String,
      expiresInDays: Double?
  ): Result<String> {
    return try {
      val token = UUID.randomUUID().toString()
      val expiresAt =
          expiresInDays?.let {
            val millis = (it * DAY_TO_MILLIS).toLong()
            Timestamp(java.util.Date(System.currentTimeMillis() + millis))
          }
      val invitation =
          hashMapOf(
              TOKEN to token,
              TYPE to type.name,
              TARGET_ID to targetId,
              CREATED_BY to createdBy,
              CREATED_AT to Timestamp.now(),
              EXPIRES_AT to expiresAt)
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
          InvitationType.fromString(doc.getString(TYPE) ?: EMPTY_STRING)
              ?: return Result.success(null)

      val invitation = docToInvitation(doc, type)

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
      val snapshot = invitationsCollection.whereEqualTo(CREATED_BY, userId).get().await()

      val invitations =
          snapshot.documents.mapNotNull { doc ->
            val type =
                InvitationType.fromString(doc.getString(TYPE) ?: EMPTY_STRING)
                    ?: return@mapNotNull null
            docToInvitation(doc, type)
          }
      Result.success(invitations)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun docToInvitation(doc: DocumentSnapshot, type: InvitationType): Invitation {
    return Invitation(
        token = doc.getString(TOKEN) ?: EMPTY_STRING,
        type = type,
        targetId = doc.getString(TARGET_ID) ?: EMPTY_STRING,
        createdBy = doc.getString(CREATED_BY) ?: EMPTY_STRING,
        createdAt = doc.getTimestamp(CREATED_AT) ?: Timestamp.now(),
        expiresAt = doc.getTimestamp(EXPIRES_AT))
  }
}
