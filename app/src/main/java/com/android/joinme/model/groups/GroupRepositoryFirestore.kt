package com.android.joinme.model.groups

import android.util.Log
import com.android.joinme.model.event.EventType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val GROUPS_COLLECTION_PATH = "groups"

/**
 * Firestore-backed implementation of [GroupRepository]. Manages CRUD operations for [Group]
 * objects.
 */
class GroupRepositoryFirestore(private val db: FirebaseFirestore) : GroupRepository {
  private val ownerAttributeName = "ownerId"

  override fun getNewGroupId(): String {
    return db.collection(GROUPS_COLLECTION_PATH).document().id
  }

  override suspend fun getAllGroups(): List<Group> {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("GroupRepositoryFirestore: User not logged in.")

    val snapshot =
        db.collection(GROUPS_COLLECTION_PATH)
            .whereEqualTo(ownerAttributeName, ownerId)
            .get()
            .await()

    return snapshot.mapNotNull { documentToGroup(it) }
  }

  override suspend fun getGroup(groupId: String): Group {
    val document = db.collection(GROUPS_COLLECTION_PATH).document(groupId).get().await()
    return documentToGroup(document)
        ?: throw Exception("GroupRepositoryFirestore: Group not found ($groupId)")
  }

  override suspend fun addGroup(group: Group) {
    db.collection(GROUPS_COLLECTION_PATH).document(group.id).set(group).await()
  }

  override suspend fun editGroup(groupId: String, newValue: Group) {
    db.collection(GROUPS_COLLECTION_PATH).document(groupId).set(newValue).await()
  }

  override suspend fun deleteGroup(groupId: String) {
    val currentUserId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("GroupRepositoryFirestore: User not logged in.")

    val group = getGroup(groupId)

    if (group.ownerId != currentUserId) {
      throw Exception("GroupRepositoryFirestore: Only the group owner can delete this group")
    }

    db.collection(GROUPS_COLLECTION_PATH).document(groupId).delete().await()
  }

  override suspend fun leaveGroup(groupId: String, userId: String) {
    val group = getGroup(groupId)
    val updatedMemberIds = group.memberIds.filter { it != userId }

    if (updatedMemberIds.size == group.memberIds.size) {
      throw Exception("GroupRepositoryFirestore: User is not a member of this group")
    }

    val updatedGroup = group.copy(memberIds = updatedMemberIds)
    editGroup(groupId, updatedGroup)
  }

  /**
   * Converts a Firestore document to a [Group] object.
   *
   * @param document The Firestore document to convert.
   * @return The [Group] object, or null if conversion fails.
   */
  private fun documentToGroup(document: DocumentSnapshot): Group? {
    return try {
      val id = document.id
      val name = document.getString("name") ?: return null
      val categoryString = document.getString("category") ?: "ACTIVITY"
      val category =
          try {
            EventType.valueOf(categoryString)
          } catch (_: IllegalArgumentException) {
            EventType.ACTIVITY // Default to ACTIVITY if invalid category
          }
      val description = document.getString("description") ?: ""
      val ownerId = document.getString("ownerId") ?: return null
      val memberIds = document.get("memberIds") as? List<String> ?: emptyList()
      val eventIds = document.get("eventIds") as? List<String> ?: emptyList()
      val photoUrl = document.getString("photoUrl")

      Group(
          id = id,
          name = name,
          category = category,
          description = description,
          ownerId = ownerId,
          memberIds = memberIds,
          eventIds = eventIds,
          photoUrl = photoUrl)
    } catch (e: Exception) {
      Log.e("GroupRepositoryFirestore", "Error converting document to Group", e)
      null
    }
  }
}
