// Implemented with help of Claude AI
package com.android.joinme.model.groups

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.joinme.model.event.EventType
import com.android.joinme.model.utils.ImageProcessor
import com.android.joinme.util.TestEnvironmentDetector
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

const val GROUPS_COLLECTION_PATH = "groups"

private const val F_PHOTO_URL = "photoUrl"

/**
 * Firestore-backed implementation of [GroupRepository]. Manages CRUD operation for [Group]
 * objects.
 */
class GroupRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val imageProcessorFactory: (Context) -> ImageProcessor = { ImageProcessor(it) }
) : GroupRepository {

  companion object {
    private const val TAG = "GroupRepositoryFirestore"
    private const val GROUPS_STORAGE_PATH = "groups"
    private const val GROUP_PHOTO_NAME = "group.jpg"
  }

  override fun getNewGroupId(): String {
    return db.collection(GROUPS_COLLECTION_PATH).document().id
  }

  override suspend fun getAllGroups(): List<Group> {
    // Get user ID with test environment detection
    val firebaseUserId = Firebase.auth.currentUser?.uid
    val userId =
        if (firebaseUserId != null) {
          firebaseUserId
        } else {
          // Return test user ID in test environments only if Firebase auth is not available
          if (TestEnvironmentDetector.shouldUseTestUserId()) TestEnvironmentDetector.getTestUserId()
          else throw Exception("GroupRepositoryFirestore: User not logged in.")
        }

    // Get all groups where user is a member (includes groups where they are owner)
    val snapshot =
        db.collection(GROUPS_COLLECTION_PATH).whereArrayContains("memberIds", userId).get().await()

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

  override suspend fun deleteGroup(groupId: String, userId: String) {
    val group = getGroup(groupId)

    if (group.ownerId != userId) {
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

  override suspend fun joinGroup(groupId: String, userId: String) {
    val group = getGroup(groupId)

    if (group.memberIds.contains(userId)) {
      throw Exception("GroupRepositoryFirestore: User is already a member of this group")
    }

    val updatedMemberIds = group.memberIds + userId
    val updatedGroup = group.copy(memberIds = updatedMemberIds)
    editGroup(groupId, updatedGroup)
  }

  override suspend fun getCommonGroups(userIds: List<String>): List<Group> {
    if (userIds.isEmpty()) return emptyList()
    if (userIds.size == 1) {
      // For a single user, just get all groups they're a member of
      val snapshot =
          db.collection(GROUPS_COLLECTION_PATH)
              .whereArrayContains("memberIds", userIds[0])
              .get()
              .await()
      return snapshot.mapNotNull { documentToGroup(it) }
    }

    // For multiple users, get groups for the first user and filter for others
    // (Firestore doesn't support multiple arrayContains queries)
    val snapshot =
        db.collection(GROUPS_COLLECTION_PATH)
            .whereArrayContains("memberIds", userIds[0])
            .get()
            .await()

    return snapshot
        .mapNotNull { documentToGroup(it) }
        .filter { group ->
          // Check if all specified users are members
          userIds.all { userId -> group.memberIds.contains(userId) }
        }
  }

  /**
   * Uploads a group photo for the given group ID to Firebase Storage, processes the image
   * (compression, orientation), and updates the group document in Firestore with the new photo URL.
   * Returns the download URL of the uploaded photo.
   */
  override suspend fun uploadGroupPhoto(context: Context, groupId: String, imageUri: Uri): String {
    // Step 1: Process the image (compress, fix orientation)
    val processedBytes = imageProcessorFactory(context).processImage(imageUri)

    // Step 2: Upload to Firebase Storage
    val storageRef =
        storage.reference.child(GROUPS_STORAGE_PATH).child(groupId).child(GROUP_PHOTO_NAME)

    try {
      storageRef.putBytes(processedBytes).await()

      // Step 3: Get the download URL
      val downloadUrl = storageRef.downloadUrl.await().toString()

      // Step 4: Update only the photoUrl field in Firestore (atomic update)
      db.collection(GROUPS_COLLECTION_PATH)
          .document(groupId)
          .update(F_PHOTO_URL, downloadUrl)
          .await()

      return downloadUrl
    } catch (e: Exception) {
      // Clean up orphaned file in Storage if Firestore update failed
      try {
        storageRef.delete().await()
      } catch (cleanupError: Exception) {
        Log.w(TAG, "Failed to clean up orphaned photo after upload failure", cleanupError)
      }
      Log.e(TAG, "Error uploading group photo for group $groupId", e)
      throw Exception("Failed to upload group photo: ${e.message}", e)
    }
  }

  /**
   * Deletes the group photo for the given group ID from Firebase Storage and clears the photoUrl
   * field in Firestore.
   */
  override suspend fun deleteGroupPhoto(groupId: String) {
    try {
      // Step 1: Delete from Storage
      val storageRef =
          storage.reference.child(GROUPS_STORAGE_PATH).child(groupId).child(GROUP_PHOTO_NAME)

      try {
        storageRef.delete().await()
      } catch (e: Exception) {
        // File might not exist, log but continue
        Log.w(TAG, "Photo file not found in Storage, continuing to clear Firestore field", e)
      }

      // Step 2: Clear only the photoUrl field in Firestore (atomic update)
      db.collection(GROUPS_COLLECTION_PATH).document(groupId).update(F_PHOTO_URL, null).await()
    } catch (e: Exception) {
      Log.e(TAG, "Error deleting group photo for group $groupId", e)
      throw Exception("Failed to delete group photo: ${e.message}", e)
    }
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
      val serieIds = document.get("serieIds") as? List<String> ?: emptyList()
      val photoUrl = document.getString(F_PHOTO_URL)

      Group(
          id = id,
          name = name,
          category = category,
          description = description,
          ownerId = ownerId,
          memberIds = memberIds,
          eventIds = eventIds,
          serieIds = serieIds,
          photoUrl = photoUrl)
    } catch (e: Exception) {
      Log.e(TAG, "Error converting document to Group", e)
      null
    }
  }
}
