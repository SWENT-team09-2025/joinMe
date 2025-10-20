package com.android.joinme.model.group

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Firestore collection path for storing group documents. */
const val GROUPS_COLLECTION_PATH = "groups"

/** Firestore collection path for storing user-group membership relationships. */
const val MEMBERSHIPS_COLLECTION_PATH = "memberships"

/**
 * Firestore implementation of [GroupRepository].
 *
 * This repository uses Firebase Firestore as the backend data source for group-related operations.
 * It manages group data retrieval, membership operations, and handles batched queries for
 * efficiency when fetching multiple groups.
 *
 * The repository follows a two-collection design:
 * - `groups`: Stores group documents with details like name, description, owner, etc.
 * - `memberships`: Stores user-group relationships for efficient membership queries.
 *
 * @property db The Firestore instance used for database operations.
 */
class GroupRepositoryFirestore(private val db: FirebaseFirestore) : GroupRepository {

  override suspend fun userGroups(): List<Group> {
    val uid =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("GroupRepositoryFirestore: User not logged in")

    val membershipSnap =
        db.collection(MEMBERSHIPS_COLLECTION_PATH).whereEqualTo("userId", uid).get().await()

    val groupIds = membershipSnap.documents.mapNotNull { it.getString("groupId") }
    if (groupIds.isEmpty()) return emptyList()

    val result = mutableListOf<Group>()
    groupIds.chunked(10).forEach { batch ->
      val groupsSnap =
          db.collection(GROUPS_COLLECTION_PATH).whereIn(FieldPath.documentId(), batch).get().await()
      result += groupsSnap.mapNotNull { documentToGroup(it) }
    }
    return result
  }

  override suspend fun leaveGroup(id: String) {
    val uid =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("GroupRepositoryFirestore: User not logged in")

    val membershipQuery =
        db.collection(MEMBERSHIPS_COLLECTION_PATH)
            .whereEqualTo("userId", uid)
            .whereEqualTo("groupId", id)
            .get()
            .await()

    membershipQuery.documents.forEach { it.reference.delete().await() }
  }

  override suspend fun getGroup(id: String): Group? {
    val doc = db.collection(GROUPS_COLLECTION_PATH).document(id).get().await()
    return documentToGroup(doc)
  }

  /**
   * Converts a Firestore document snapshot to a [Group] object.
   *
   * This helper method safely extracts group data from a Firestore document, handling missing or
   * malformed fields gracefully. The group's name and ownerId are required - if missing, null is
   * returned. Optional fields (description, memberIds, eventIds) default to empty values.
   *
   * @param document The Firestore document snapshot to convert.
   * @return A [Group] object if conversion succeeds and required fields are present, null
   *   otherwise.
   */
  private fun documentToGroup(document: DocumentSnapshot): Group? {
    return try {
      val id = document.id
      val name = document.getString("name") ?: return null
      val description = document.getString("description") ?: ""
      val ownerId = document.getString("ownerId") ?: return null
      val memberIds = document.get("memberIds") as? List<*>
      val eventIds = document.get("eventIds") as? List<*>

      Group(
          id = id,
          name = name,
          description = description,
          ownerId = ownerId,
          memberIds = memberIds?.filterIsInstance<String>() ?: emptyList(),
          eventIds = eventIds?.filterIsInstance<String>() ?: emptyList())
    } catch (e: Exception) {
      null
    }
  }
}
