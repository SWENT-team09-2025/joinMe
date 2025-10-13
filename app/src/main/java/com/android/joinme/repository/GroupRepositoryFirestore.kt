package com.android.joinme.repository

import android.util.Log
import com.android.joinme.model.group.Group
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.String
import kotlinx.coroutines.tasks.await

const val GROUPS_COLLECTION_PATH = "groups"
const val MEMBERSHIPS_COLLECTION_PATH = "memberships"

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

  private fun documentToGroup(document: DocumentSnapshot): Group? {
    return try {
      val id = document.id
      val name = document.getString("name") ?: return null
      val category = document.getString("category") ?: ""
      val description = document.getString("description") ?: ""
      val membersCount = document.getLong("membersCount")?.toInt() ?: 0

      Group(
          id = id,
          name = name,
          category = category,
          description = description,
          membersCount = membersCount)
    } catch (e: Exception) {
      Log.e("GroupRepositoryFirestore", "Error converting document to Group", e)
      null
    }
  }
}
